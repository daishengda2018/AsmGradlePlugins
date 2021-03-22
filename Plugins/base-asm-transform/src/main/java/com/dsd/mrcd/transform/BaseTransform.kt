package com.dsd.base.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.dsd.mrcd.transform.RunVariant
import com.dsd.mrcd.transform.asm.BytecodeAdapter
import com.dsd.mrcd.transform.asm.ClassLoaderFactory
import com.dsd.mrcd.transform.concurrent.Schedulers
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File
import java.io.IOException

/**
 * 基础 transform
 *
 * Create by im_dsd 2021/3/19 20:42
 */
abstract class BaseTransform(val project: Project) : Transform() {
    protected var isEmptyRun = false
    protected var mBytecodeWeaver: BytecodeAdapter? = null
    protected val logger = project.logger
    protected var isCleanedDexBuilderFolder = false
    protected var mWorker = Schedulers.newWorker()

    override fun getName(): String {
        return this.javaClass.simpleName
    }

    abstract fun getBytecodeAdapter(): BytecodeAdapter?

    /**
     * 输入的类型。包括 [QualifiedContent.DefaultContentType.CLASSES]
     * 和 [QualifiedContent.DefaultContentType.RESOURCES] 两种，根据具体情况引用
     *
     * 注意： [QualifiedContent.DefaultContentType.CLASSES] 包含了 class 文件 和 jar 文件
     */
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 表示 transform 涉及的范围：
     * 仅处理当前项目(模块)内容：  [QualifiedContent.Scope.PROJECT]
     * 仅处理子项目（其他模块）: [QualifiedContent.Scope.SUB_PROJECTS]
     * 仅外部库:  [QualifiedContent.Scope.EXTERNAL_LIBRARIES]
     *
     * 如果要在 app mould 中处理所有涉及的项目，可以直接使用系统提供的封装
     * 如果是个 lib 或只想处理当前 module 使用 [QualifiedContent.Scope.PROJECT]
     */
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否支持增量编译
     */
    override fun isIncremental(): Boolean {
        return true
    }

    /**
     * 执行 transform 的地方
     */
    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        mBytecodeWeaver = getBytecodeAdapter();
        // 检查编译变体确定运行状态
        if ("debug" == transformInvocation.context.variantName) {
            isEmptyRun = getRunVariant() == RunVariant.RELEASE || getRunVariant() == RunVariant.NEVER
        } else if ("release" == transformInvocation.context.variantName) {
            isEmptyRun = getRunVariant() == RunVariant.DEBUG || getRunVariant() == RunVariant.NEVER
        }
        logger.warn(
            name + " isIncremental = " + isIncremental + ", runVariant = " + getRunVariant()
                    + ", emptyRun = " + isEmptyRun + ", inDuplicatedClassSafeMode = " + inDuplicatedClassSafeMode()
        )

        val startTime = System.currentTimeMillis()
        // 如果不是增量编译就删除原来的所有输出
        if (!transformInvocation.isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }
        val newClassLoader = ClassLoaderFactory.newClassLoader(transformInvocation, project)
        mBytecodeWeaver?.setClassLoader(newClassLoader)
        isCleanedDexBuilderFolder = false
        // 自定义 ClassLoader
        // 处理所有输入内容
        for (input in transformInvocation.inputs) {
            // jar 的输入
            for (jarInput in input.jarInputs) {
                resolveJarInput(jarInput, transformInvocation)
            }
            // 项目输入
            for (directoryInput in input.directoryInputs) {
                resolveDirectoryInput(directoryInput, transformInvocation)
            }
        }

        val costTime = System.currentTimeMillis() - startTime
        logger.warn(name + " 耗时： " + costTime + "ms")
    }

    protected open fun resolveJarInput(
        jarInput: JarInput,
        transformInvocation: TransformInvocation
    ) {
        val status = jarInput.status
        val destFile: File = getOutputLocation(transformInvocation, jarInput)
        // 判断是否增量编译
        if (isIncremental && !isEmptyRun) {
            when (status) {
                Status.ADDED, Status.CHANGED -> {
                    transformJar(jarInput.file, destFile, status)
                }
                Status.REMOVED -> {
                    if (destFile.exists()) {
                        FileUtils.forceDelete(destFile)
                    }
                }
                else -> {
                }
            }
        } else {
            // Forgive me!, Some project will store 3rd-party aar for several copies in dexbuilder folder,unknown issue.
            if (inDuplicatedClassSafeMode() && !isIncremental && !isCleanedDexBuilderFolder) {
                cleanDexBuilderFolder(destFile)
                isCleanedDexBuilderFolder = true
            }
            transformJar(jarInput.file, destFile, status)
        }
    }

    protected open fun resolveDirectoryInput(
        directoryInput: DirectoryInput,
        transformInvocation: TransformInvocation
    ) {
        val dest: File = transformInvocation.outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY
        )
        FileUtils.forceMkdir(dest)
        if (!isIncremental || isEmptyRun) {
            transformDir(directoryInput.file, dest)
            return
        }

        val srcDirPath = directoryInput.file.absolutePath
        val destDirPath = dest.absolutePath
        val fileStatusMap = directoryInput.changedFiles
        for ((inputFile, status) in fileStatusMap) {
            val destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
            val destFile = File(destFilePath)
            when (status) {
                Status.REMOVED -> {
                    destFile.deleteOnExit()
                }
                Status.ADDED, Status.CHANGED -> {
                    try {
                        FileUtils.touch(destFile)
                    } catch (e: IOException) {
                        // mkdir 会因为一起莫名的原因失败，需要重试
                        FileUtils.forceMkdirParent(destFile)
                    }
                    transformSingleFile(inputFile, destFile, srcDirPath)
                }
                else -> {
                }
            }
        }
    }

    protected fun getOutputLocation(
        transformInvocation: TransformInvocation,
        jarInput: JarInput
    ): File {
        val outputProvider = transformInvocation.outputProvider
        return outputProvider.getContentLocation(
            jarInput.file.absolutePath,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
    }

    protected open fun transformSingleFile(inputFile: File, outputFile: File, srcBaseDir: String) {
        mWorker.execute {
            mBytecodeWeaver?.transformClazz2File(inputFile, outputFile, srcBaseDir)
        }
    }

    @Throws(IOException::class)
    protected open fun transformDir(inputDir: File, outputDir: File) {
        if (isEmptyRun) {
            FileUtils.copyDirectory(inputDir, outputDir)
            return
        }
        val inputDirPath = inputDir.absolutePath
        val outputDirPath = outputDir.absolutePath
        if (inputDir.isDirectory) {
            for (file in com.android.utils.FileUtils.getAllFiles(inputDir)) {
                mWorker.execute {
                    val filePath = file.absolutePath
                    val outputFile = File(filePath.replace(inputDirPath, outputDirPath))
                    mBytecodeWeaver?.transformClazz2File(file, outputFile, inputDirPath)
                }
            }
        }
    }

    protected open fun transformJar(srcJar: File, destJar: File, status: Status) {
        mWorker.execute {
            if (isEmptyRun) {
                FileUtils.copyFile(srcJar, destJar)
                return@execute
            }
            mBytecodeWeaver?.transformJar(srcJar, destJar)
        }
    }

    protected open fun cleanDexBuilderFolder(dest: File) {
        mWorker.execute {
            try {
                val dexBuilderDir = replaceLastPart(dest.absolutePath, name, "dexBuilder")
                // intermediates/transforms/dexBuilder/debug
                val file = File(dexBuilderDir).parentFile
                project.logger.warn("clean dexBuilder folder = " + file.absolutePath)
                if (file.exists() && file.isDirectory) {
                    com.android.utils.FileUtils.deleteDirectoryContents(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected open fun replaceLastPart(
        originString: String,
        replacement: String,
        toreplace: String
    ): String {
        val start = originString.lastIndexOf(replacement)
        val builder = StringBuilder()
        builder.append(originString, 0, start)
        builder.append(toreplace)
        builder.append(originString.substring(start + replacement.length))
        return builder.toString()
    }

    override fun isCacheable(): Boolean {
        return true
    }

    open fun getRunVariant(): RunVariant? {
        return RunVariant.ALWAYS
    }

    fun inDuplicatedClassSafeMode(): Boolean {
        return false
    }
}