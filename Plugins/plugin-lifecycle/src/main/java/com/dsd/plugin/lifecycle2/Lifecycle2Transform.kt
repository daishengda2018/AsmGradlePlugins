package com.dsd.plugin.lifecycle2

import com.dsd.base.transform.BaseTransform
import com.dsd.mrcd.transform.asm.BytecodeAdapter
import com.dsd.plugin.lifecycle.asm.LifecycleMethodAdapter
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 *
 * Create by im_dsd 2021/3/22 16:30
 */
class Lifecycle2Transform(project: Project) : BaseTransform(project) {

    override fun getBytecodeAdapter(): BytecodeAdapter {
        return object : BytecodeAdapter() {
            override fun wrapClassWriter(classVisitor: ClassWriter?): ClassVisitor {
                return MyClassWriter(classVisitor)
            }
        }
    }

    class MyClassWriter(classVisitor: ClassWriter?) : ClassVisitor(Opcodes.ASM7, classVisitor) {
        private var mClazzName: String? = ""

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            mClazzName = name
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
            //匹配FragmentActivity
            if ("com/dsd/asm/MainActivity" == this.mClazzName && name == "onCreate") {
                println("LifecycleClassVisitor : change method ----> $name")
                return if (mv == null) null else LifecycleMethodAdapter(mv, access, descriptor)
            }
            return mv
        }
    }
}