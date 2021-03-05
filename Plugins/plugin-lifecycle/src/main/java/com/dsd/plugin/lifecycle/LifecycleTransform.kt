package com.dsd.plugin.lifecycle

import com.dsd.plugin.lifecycle.asm.LifecycleWeaver
import com.quinn.hunter.transform.HunterTransform
import org.gradle.api.Project

/**
 *
 * Create by im_dsd 2021/3/5 09:59
 */
class LifecycleTransform(project: Project) : HunterTransform(project) {
    init {
        bytecodeWeaver = LifecycleWeaver()
    }
}