package com.mrcd.transform

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Create by im_dsd 2021/3/31 17:13
 */
class DoubleClickClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM7, classVisitor) {
    private var mInterface: Array<String>? = null
    private var isVisitedStaticBlock = false
    private var owner: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        mInterface = interfaces
        if (interfaces == null || interfaces.isEmpty()) {
            return
        }
        for (item: String in interfaces) {
            // 判断当前类是否实现了目标接口
            if (item != CLICK_INTERFACE) {
                continue
            }
            isVisitedStaticBlock = true
            owner = name
            // 插入全局变量
            cv.visitField(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
                "mDoubleClickHelper",
                "Lcom/dsd/asm/helper/DoubleClickCheck;",
                signature,
                null
            )
            cv.visitEnd()
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (mInterface.isNullOrEmpty()) {
            return methodVisitor
        }
        if (isVisitedStaticBlock && name == "<init>") {
            return InitBlockVisitor(methodVisitor, owner)
        }
        // 是目标方法
        if (CLICK_METHOD == (name + descriptor)) {
            return CheckVisitor(methodVisitor, owner)
        }
        return methodVisitor
    }

    companion object {
        private const val CLICK_INTERFACE = "android/view/View\$OnClickListener"
        private const val CLICK_METHOD = "onClick(Landroid/view/View;)V"
    }
}