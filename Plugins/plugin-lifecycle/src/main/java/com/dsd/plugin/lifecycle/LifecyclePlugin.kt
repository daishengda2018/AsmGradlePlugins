package com.dsd.plugin.lifecycle

import com.android.build.gradle.AppExtension
import com.dsd.plugin.lifecycle2.Lifecycle2Transform
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

class LifecyclePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
        android.registerTransform(Lifecycle2Transform(project))
    }
}