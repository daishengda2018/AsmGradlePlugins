package com.dsd.plugin.lifecycle.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 *
 * Create by im_dsd 2021/3/1 16:34
 */
class LifecycleClassVisitor(visitor: ClassVisitor) : ClassVisitor(Opcodes.ASM7, visitor), Opcodes {
    var mClazzName: String? = ""

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
        if (cv == null) {
            return null
        }
        val visitMethod = cv.visitMethod(access, name, descriptor, signature, exceptions)
        //匹配FragmentActivity
        if ("android/support/v4/app/FragmentActivity" == this.mClazzName) {
            if ("onCreate" == name) {
                //处理onCreate
                println("LifecycleClassVisitor : change method ----> $name")
                return LifecycleOnCreateMethodVisitor(visitMethod)
            } else if ("onDestroy" == name) {
                //处理onDestroy
                println("LifecycleClassVisitor : change method ----> $name");
                return LifecycleOnCreateMethodVisitor(visitMethod)
            }
        }
        return visitMethod
    }
}