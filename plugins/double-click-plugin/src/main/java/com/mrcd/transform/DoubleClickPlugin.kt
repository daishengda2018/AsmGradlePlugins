package com.mrcd.transform

import com.android.build.gradle.AppExtension
import com.mrcd.transform.asm.AbsBytecodeWeaver
import com.mrcd.transform.asm.ExtendClassWriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor

class DoubleClickPlugin : Plugin<Project> {

    override fun apply(target: Project) {
       val extension = target.extensions.getByType(AppExtension::class.java)
        extension.registerTransform(BaseTransform(target, object: AbsBytecodeWeaver() {
            override fun getClassVisitor(classWriter: ExtendClassWriter): ClassVisitor {
                return DoubleClickClassVisitor(classWriter);
            }
        }))
    }
    // TODO: 2021/3/31 需要为 Transform 添加一个名字
}
