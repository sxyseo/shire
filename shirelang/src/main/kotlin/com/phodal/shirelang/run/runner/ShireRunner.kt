package com.phodal.shirelang.run.runner

import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.project.Project
import com.phodal.shirelang.run.ShireConfiguration

abstract class ShireRunner(
    open val configuration: ShireConfiguration,
    open val processHandler: ProcessHandler,
    open val console: ConsoleViewWrapperBase,
    open val myProject: Project,
    open val prompt: String,
) {
    abstract fun execute()
}