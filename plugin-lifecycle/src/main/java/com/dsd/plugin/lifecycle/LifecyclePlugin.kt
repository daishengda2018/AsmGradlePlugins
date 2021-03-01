package com.dsd.plugin.lifecycle

import org.gradle.api.Plugin
import org.gradle.api.Project

class LifecyclePlugin : Plugin<Project> {

    override fun apply(p: Project) {
        System.out.println("========================");
        System.out.println("hello gradle plugin!");
        System.out.println("========================");
    }
}