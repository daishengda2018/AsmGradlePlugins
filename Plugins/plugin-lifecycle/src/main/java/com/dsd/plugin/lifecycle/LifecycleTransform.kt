package com.dsd.plugin.lifecycle

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.dsd.plugin.lifecycle.asm.LifecycleClassVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry


/**
 *
 * Create by im_dsd 2021/3/1 14:24
 */
class LifecycleTransform : Transform() {

    override fun getName(): String {
        return "lifecycle"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT_WITH_LOCAL_JARS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        println("--------------- LifecyclePlugin visit start --------------- ")
        val startTime = System.currentTimeMillis()
        val inputs = transformInvocation.inputs
        // 删除之前的输出
        val outputProvider = transformInvocation.outputProvider
        outputProvider?.deleteAll()
        // 遍历 input.inputs可以拿到所有的 class 文件
        inputs.forEach { input ->
            // 遍历 directoryInputs. 为文件夹中的 class 文件
            input.directoryInputs.forEach { directoryInput ->
                handleDirectoryInput(directoryInput, outputProvider)
            }
            // 遍历 jarInputs。 为 jar 包中的 class 文件
            input.jarInputs.forEach { jarInput ->
                handleJarInputs(jarInput, outputProvider)
            }
        }
        println("--------------- LifecyclePlugin visit end ---------------")
        println("LifecyclePlugin cost ： ${System.currentTimeMillis() - startTime} ms")
    }

    private fun handleDirectoryInput(
        directoryInput: DirectoryInput?,
        outputProvider: TransformOutputProvider?
    ) {
        val isDirectory = directoryInput?.file?.isDirectory == true
        // 不是目录直接返回掉
        if (!isDirectory) {
            put2Next(outputProvider, directoryInput)
            return
        }
        // 列出目录所有文件（包含子文件夹，子文件夹内文件）
        val fileTree = directoryInput?.file?.walk()
        fileTree?.filter { it.isFile }
            ?.forEach { file ->
                if (!isTargetClazz(file.name)) {
                    return
                }
                val fos =
                    FileOutputStream("${file.parentFile.absoluteFile} + ${File.separator} + $name")
                fos.write(resloveClazzCode(file.readBytes()))
                fos.close()
            }
        // 处理完输入文件之后，要把输出给下一个任务
        put2Next(outputProvider, directoryInput)
    }

    private fun resloveClazzCode(byteArray: ByteArray): ByteArray {
        val classReader = ClassReader(byteArray)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val visitor = LifecycleClassVisitor(classWriter)
        classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    private fun put2Next(
        outputProvider: TransformOutputProvider?,
        directoryInput: DirectoryInput?
    ) {
        val dest = outputProvider?.getContentLocation(
            directoryInput?.name,
            directoryInput?.contentTypes, directoryInput?.scopes,
            Format.DIRECTORY
        )
        FileUtils.copyDirectory(directoryInput?.file, dest)
    }

    private fun isTargetClazz(name: String) = (name.endsWith(".class") && !name.startsWith("R\$")
            && "R.class" != name && "BuildConfig.class" != name
            && "androidx/fragment/app/FragmentActivity.class" == name)

    private fun handleJarInputs(jarInput: JarInput?, outputProvider: TransformOutputProvider?) {
        val isJar = jarInput?.file?.name?.endsWith(".jar") == true
        if (!isJar) {
            return
        }
        // 重命名输出文件，防止名字相同相互覆盖
        val md5Name = DigestUtils.md5Hex(jarInput?.file?.absolutePath ?: "")
        val jarName = if (jarInput?.name?.endsWith(".jar") == true) {
            jarInput.name?.substring(0, (jarInput.name?.length ?: 4 - 4))
        } else {
            jarInput?.name
        }
        val jarFile = JarFile(jarInput?.file)
        val entries = jarFile.entries()
        val tmpFile = File(jarInput?.file?.parent + File.separator + "classes_temp.jar")
        // 避免上次的缓存被重复插入
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
        while (entries.hasMoreElements()) {
            val jarEntry = entries.nextElement()
            val entryName = jarEntry.name
            val zipEntry = ZipEntry(entryName)
            val inputStream = jarFile.getInputStream(jarEntry)
            // 插桩class
            if (isTargetClazz(entryName)) {
                // class文件处理
                println("----------- deal with jar class file $entryName -----------")
                jarOutputStream.putNextEntry(zipEntry)
                val clazzCode = resloveClazzCode(IOUtils.toByteArray(inputStream))
                jarOutputStream.write(clazzCode)
            } else {
                jarOutputStream.putNextEntry(zipEntry)
                jarOutputStream.write(IOUtils.toByteArray(inputStream))
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        val dest = outputProvider?.getContentLocation(
            jarName + md5Name,
            jarInput?.contentTypes, jarInput?.scopes, Format.JAR
        )
        FileUtils.copyFile(tmpFile, dest)
        tmpFile.delete()
    }
}

