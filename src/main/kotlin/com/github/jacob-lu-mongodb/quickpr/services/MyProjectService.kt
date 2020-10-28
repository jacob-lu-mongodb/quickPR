package com.github.jacob-lu-mongodb.quickpr.services

import com.intellij.openapi.project.Project
import com.github.jacob-lu-mongodb.quickpr.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
