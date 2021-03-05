package com.dsd.plugin.lifecycle

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

class LifecyclePlugin : Plugin<Project> {

    override fun apply(project: Project) {
//        val android = project.extensions.getByType(AppExtension::class.java)
//        android.registerTransform(LifecycleTransform(project))

        val appExtension = project.properties["android"] as AppExtension?
        appExtension?.registerTransform(LifecycleTransform(project), Collections.EMPTY_LIST)
    }
}