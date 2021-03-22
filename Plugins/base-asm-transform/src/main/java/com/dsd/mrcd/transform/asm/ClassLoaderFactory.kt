package com.dsd.mrcd.transform.asm

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import org.gradle.api.Project
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

/**
 * 自定义 ClassLoader
 *
 * ClassWriter其中的一个逻辑，寻找两个类的共同父类。可以看看 [org.objectweb.asm.ClassWriter.getCommonSuperClass] 方法
 * 而编译期间使用的 classloader 并没有加载 android.jar 中的代码，所以我们需要一个自定义的 ClassLoader，
 * 自定义的 ClassLoader 要将 Transform 中接收到的所有 class、 jar、android.jar 都添加到自定义 ClassLoader 中。
 *
 * Create by im_dsd 2021/3/19 20:10
 */
object ClassLoaderFactory {

    /**
     * 获取 ClassLoader
     */
    @Throws(MalformedURLException::class)
    fun newClassLoader(
        transformInvocation: TransformInvocation?,
        project: Project
    ): URLClassLoader {
        val urls = ImmutableList.Builder<URL>()
        val androidJarPath = getAndroidJarPath(project)
        val file = File(androidJarPath)
        val androidJarURL = file.toURI().toURL()
        urls.add(androidJarURL)
        // 将两个可迭代对象合并为一个可迭代对象。
        for (input in Iterables.concat(
            transformInvocation?.inputs,
            transformInvocation?.referencedInputs
        )) {
            for (directoryInput in input.directoryInputs) {
                if (directoryInput.file.isDirectory) {
                    urls.add(directoryInput.file.toURI().toURL())
                }
            }
            for (jarInput in input.jarInputs) {
                if (jarInput.file.isFile) {
                    urls.add(jarInput.file.toURI().toURL())
                }
            }
        }
        val allUrls = urls.build()
        val classLoaderUrls = allUrls.toTypedArray()
        return URLClassLoader(classLoaderUrls)
    }

    /**
     * 通过 gradle.properties 中配置的 sdk 路径获取 android.jar path
     */
    private fun getAndroidJarPath(project: Project): String {
        val appExtension = project.properties["android"] as AppExtension
        var sdkDirectory = appExtension.sdkDirectory.absolutePath
        val compileSdkVersion = appExtension.compileSdkVersion
        sdkDirectory = sdkDirectory + File.separator + "platforms" + File.separator
        return sdkDirectory + compileSdkVersion + File.separator + "android.jar"
    }
}