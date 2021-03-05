package com.dsd.plugin.lifecycle.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 *
 * Create by im_dsd 2021/3/5 10:09
 */
class LifecycleClassAdapter(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM7, cv) {
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
        if ("androidx/fragment/app/FragmentActivity" == this.mClazzName && name == "onCreate") {
            println("LifecycleClassVisitor : change method ----> $name")
            return if (mv == null) null else LifecycleMethodAdapter(mv, access, descriptor)
        }
        return mv
    }
}