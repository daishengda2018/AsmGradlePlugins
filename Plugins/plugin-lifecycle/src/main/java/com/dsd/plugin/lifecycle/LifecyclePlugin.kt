package com.dsd.plugin.lifecycle

import com.android.build.gradle.AppExtension
import com.dsd.plugin.lifecycle.asm.LifecycleClassVisitor
import com.mrcd.transform.BaseTransform
import com.mrcd.transform.asm.AbsBytecodeResolver
import com.mrcd.transform.asm.ExtendClassWriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor

class LifecyclePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
        android.registerTransform(BaseTransform(project, object : AbsBytecodeResolver() {
            override fun getClassVisitor(classWriter: ExtendClassWriter): ClassVisitor {
               return LifecycleClassVisitor(classWriter)
            }
        }))
    }
}