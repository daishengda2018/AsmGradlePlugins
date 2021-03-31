package com.mrcd.transform

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


/**
 *
 * Create by im_dsd 2021/3/31 19:42
 */
class InitBlockVisitor(
    mv: MethodVisitor,
    private val owner: String?
) : MethodVisitor(Opcodes.ASM7, mv) {

    override fun visitInsn(opcode: Int) {
        val isReturn = opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN
        if (isReturn || opcode == Opcodes.ATHROW) {
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitTypeInsn(Opcodes.NEW, "com/dsd/asm/helper/DoubleClickCheck")
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/dsd/asm/helper/DoubleClickCheck",
                "<init>",
                "()V",
                false
            )
            mv.visitFieldInsn(
                Opcodes.PUTFIELD,
                owner,
                "mDoubleClickHelper",
                "Lcom/dsd/asm/helper/DoubleClickCheck;"
            )
        }
        super.visitInsn(opcode)
    }

    /**
     * 堆栈信息在编译期就会确认好，如果添加变量需要重新申请
     */
    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        // The values 3 and 0 come from the fact that our instance
        // creation uses 3 stack slots to construct the instances
        // above and 0 local variables.
        val ourMaxStack = 3
        val ourMaxLocals = 0

        // now, instead of just passing original or our own
        // visitMaxs numbers to super, we instead calculate
        // the maximum values for both.
        super.visitMaxs(Math.max(ourMaxStack, maxStack), Math.max(ourMaxLocals, maxLocals))
    }
}