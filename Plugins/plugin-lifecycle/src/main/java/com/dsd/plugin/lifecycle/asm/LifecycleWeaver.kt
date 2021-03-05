package com.dsd.plugin.lifecycle.asm

import com.quinn.hunter.transform.asm.BaseWeaver
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 *
 * Create by im_dsd 2021/3/5 10:07
 */
class LifecycleWeaver : BaseWeaver() {

    override fun wrapClassWriter(classWriter: ClassWriter): ClassVisitor {
        return LifecycleClassAdapter(classWriter)
    }
}