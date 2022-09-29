package com.liquid.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ModifyPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        print("ModifyPlugin init=============================================>")
        project.android.registerTransform(new ModifyTransform(project))
    }
}