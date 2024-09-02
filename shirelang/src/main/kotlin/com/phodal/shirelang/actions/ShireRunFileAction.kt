package com.phodal.shirelang.actions

import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.phodal.shirecore.middleware.PostCodeHandleContext
import com.phodal.shirelang.ShireActionStartupActivity
import com.phodal.shirelang.actions.base.DynamicShireActionConfig
import com.phodal.shirelang.psi.ShireFile
import com.phodal.shirelang.run.*
import org.jetbrains.annotations.NonNls
import java.util.concurrent.CompletableFuture

class ShireRunFileAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        e.presentation.isEnabledAndVisible = file is ShireFile

        if (e.presentation.text.isNullOrBlank()) {
            e.presentation.text = "Run Shire file: ${file.name}"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) as? ShireFile ?: return
        val project = file.project
        val config = DynamicShireActionConfig.from(file)

        val existingConfiguration = createRunConfig(e)
        executeShireFile(project, config, existingConfiguration)
    }

    companion object {
        const val ID: @NonNls String = "runShireFileAction"

        fun createRunConfig(e: AnActionEvent): RunnerAndConfigurationSettings? {
            val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
            val configProducer = RunConfigurationProducer.getInstance(ShireRunConfigurationProducer::class.java)

            val existingConfiguration = configProducer.findExistingConfiguration(context)
            return existingConfiguration
        }

        fun executeShireFile(
            project: Project,
            config: DynamicShireActionConfig,
            runSettings: RunnerAndConfigurationSettings?,
            variables: Map<String, String> = mapOf(),
        ) {
            val settings = try {
                runSettings ?: RunManager.getInstance(project)
                    .createConfiguration(config.name, ShireConfigurationType::class.java)
            } catch (e: Exception) {
                logger<ShireRunFileAction>().error("Failed to create configuration", e)
                return
            }

            val runConfiguration = settings.configuration as ShireConfiguration
            runConfiguration.setScriptPath(config.shireFile.virtualFile.path)
            if (variables.isNotEmpty()) {
                runConfiguration.setVariables(variables)
            }

            val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
            val executionEnvironment = ExecutionEnvironmentBuilder
                .createOrNull(executorInstance, runConfiguration)
                ?.build()

            if (executionEnvironment == null) {
                logger<ShireRunFileAction>().error("Failed to create execution environment")
                return
            }

            ExecutionManager.getInstance(project).restartRunProfile(executionEnvironment)
        }

        fun executeFile(
            myProject: Project,
            fileName: String,
            variableNames: Array<String>,
            variableTable: MutableMap<String, Any?>,
        ): Any {
//            var output: String? = null
            val variables: MutableMap<String, String> = mutableMapOf()
            for (i in variableNames.indices) {
                variables[variableNames[i]] = variableTable[variableNames[i]].toString() ?: ""
//                // if include output in variableTable
//                if (variableNames[i] == "output") {
//                    output = variableTable[variableNames[i]].toString()
//                }
            }

            val file = runReadAction {
                ShireActionStartupActivity.obtainShireFiles(myProject).find {
                    it.name == fileName
                }
            }
//
//            val context = PostCodeHandleContext.getData() ?: PostCodeHandleContext(
//                selectedEntry = null,
//                currentLanguage = file?.language,
//                currentFile = file,
//                editor = null,
//                compiledVariables = variableTable,
//                lastTaskOutput = output
//            )
//            PostCodeHandleContext.putData(context)

            if (file == null) {
                throw RuntimeException("File $fileName not found")
            }

            ApplicationManager.getApplication().invokeLater({
                val config = DynamicShireActionConfig.from(file)
                executeShireFile(myProject, config, null, variables)
            }, ModalityState.NON_MODAL)

            return ""
        }

        fun suspendExecuteFile(
            project: Project,
            variableNames: Array<String>,
            variableTable: MutableMap<String, Any?>,
            file: ShireFile,
        ): String? {
            val variables: MutableMap<String, String> = mutableMapOf()
            for (i in variableNames.indices) {
                variables[variableNames[i]] = variableTable[variableNames[i]].toString()
            }

            val config = DynamicShireActionConfig.from(file)

            val settings = try {
                RunManager.getInstance(project)
                    .createConfiguration(config.name, ShireConfigurationType::class.java)
            } catch (e: Exception) {
                logger<ShireRunFileAction>().error("Failed to create configuration", e)
                return null
            }

            val runConfiguration = settings.configuration as ShireConfiguration
            runConfiguration.setScriptPath(config.shireFile.virtualFile.path)
            if (variables.isNotEmpty()) {
                runConfiguration.setVariables(variables)
            }

            val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
            val executionEnvironment = ExecutionEnvironmentBuilder
                .createOrNull(executorInstance, runConfiguration)
                ?.build()

            if (executionEnvironment == null) {
                logger<ShireRunFileAction>().error("Failed to create execution environment")
                return null
            }

            val future = CompletableFuture<String>()

            val hintDisposable = Disposer.newDisposable()
            val connection = ApplicationManager.getApplication().messageBus.connect(hintDisposable)
            connection.subscribe(ShireRunListener.TOPIC, object : ShireRunListener {
                override fun runFinish(allOutput: String, llmOutput: String, event: ProcessEvent, scriptPath: String) {
                    future.complete(llmOutput)
                }
            })

            ExecutionManager.getInstance(project).restartRunProfile(
                project,
                executorInstance,
                executionEnvironment.executionTarget,
                settings,
                null
            )

            return future.get()
        }
    }
}
