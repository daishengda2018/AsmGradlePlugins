package com.dsd.plugin.lifecycle.asm

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.LocalVariablesSorter

/**
 *
 * Create by im_dsd 2021/3/1 17:15
 */
class LifecycleMethodAdapter(
    methodVisitor: MethodVisitor,
    access: Int,
    descriptor: String?
) : LocalVariablesSorter(ASM7, access, descriptor, methodVisitor), Opcodes  {

    override fun visitCode() {
        super.visitCode()
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;", false);
        mv.visitLdcInsn("onCreate");
        mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(POP);
    }
}