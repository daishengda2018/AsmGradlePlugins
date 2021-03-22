package com.dsd.plugin.lifecycle2

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 *
 * Create by im_dsd 2021/3/22 16:28
 */
class Lifecycle2Plugin : Plugin<Project> {

    override fun apply(target: Project) {
       // val android = target.extensions.getByType(AppExtension::class.java)
       // android.registerTransform(Lifecycle2Transform(target))
    }
}