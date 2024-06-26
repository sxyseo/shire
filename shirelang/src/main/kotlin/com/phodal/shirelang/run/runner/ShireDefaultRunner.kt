package com.phodal.shirelang.run.runner

import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.phodal.shire.llm.LlmProvider
import com.phodal.shirelang.ShireBundle
import com.phodal.shirelang.run.ShireConfiguration
import com.phodal.shirelang.run.flow.ShireConversationService
import com.phodal.shirelang.utils.ShireCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShireDefaultRunner(
    override val myProject: Project,
    override val configuration: ShireConfiguration,
    override val console: ConsoleViewWrapperBase,
    override val processHandler: ProcessHandler,
    override val prompt: String,
    private val isLocalMode: Boolean,
) : ShireRunner(configuration, processHandler, console, myProject, prompt) {
    override fun execute(postFunction: () -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            if (isLocalMode) {
                console.print(ShireBundle.message("shire.run.local.mode"), ConsoleViewContentType.SYSTEM_OUTPUT)
                processHandler.detachProcess()
                return@invokeLater
            }

            ShireCoroutineScope.scope(myProject).launch {
                val llmResult = StringBuilder()
                runBlocking {
                    LlmProvider.provider(myProject)?.stream(prompt, "", false)?.collect {
                        llmResult.append(it)

                        console.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    } ?: console.print(ShireBundle.message("shire.llm.notfound"), ConsoleViewContentType.ERROR_OUTPUT)
                }

                console.print(ShireBundle.message("shire.llm.done"), ConsoleViewContentType.SYSTEM_OUTPUT)

                myProject.getService(ShireConversationService::class.java)
                    .refreshLlmResponseCache(configuration.getScriptPath(), llmResult.toString())

                postFunction()
                processHandler.detachProcess()
            }
        }
    }
}

