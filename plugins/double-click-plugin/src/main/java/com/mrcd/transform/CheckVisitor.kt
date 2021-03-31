package com.mrcd.transform

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class CheckVisitor(
    mv: MethodVisitor?,
    private val owner: String?
) : MethodVisitor(Opcodes.ASM5, mv) {

    override fun visitCode() {
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(
            Opcodes.GETFIELD,
            owner,
            "mDoubleClickHelper",
            "Lcom.dsd.asm.helper.DoubleClickCheck;"
        )
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "com/dsd/asm/helper/DoubleClickCheck",
            "isNotDoubleTap",
            "()Z",
            false
        )
        val label0 = Label()
        mv.visitJumpInsn(Opcodes.IFNE, label0)
        mv.visitInsn(Opcodes.RETURN)
        mv.visitLabel(label0)
        super.visitCode()
    }
}