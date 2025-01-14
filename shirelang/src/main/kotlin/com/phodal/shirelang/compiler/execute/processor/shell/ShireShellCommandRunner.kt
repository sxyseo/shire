package com.phodal.shirelang.compiler.execute.processor.shell

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.psi.search.ProjectScope
import com.phodal.shire.json.ShireEnvReader
import com.phodal.shire.json.ShireEnvVariableFiller
import java.io.File
import java.nio.charset.StandardCharsets

object ShireShellCommandRunner {
    private const val DEFAULT_TIMEOUT: Int = 30000

    fun fill(project: Project, file: VirtualFile, processVariables: Map<String, String>): String {
        return runReadAction {
            val scope = ProjectScope.getContentScope(project)

            val envName = ShireEnvReader.getAllEnvironments(project, scope).firstOrNull() ?: ShireEnvReader.DEFAULT_ENV_NAME
            val envObject = ShireEnvReader.getEnvObject(envName, scope, project)

            val content = file.readText()

            val envVariables: List<Set<String>> = ShireEnvReader.fetchEnvironmentVariables(envName, scope)
            val filledContent = ShireEnvVariableFiller.fillVariables(content, envVariables, envObject, processVariables)

            filledContent
        }
    }

    fun runShellCommand(virtualFile: VirtualFile, myProject: Project, processVariables: Map<String, String>): String {
        val workingDirectory = virtualFile.parent.path

        val fileContent = fill(myProject, virtualFile, processVariables)
        val tempFile = File.createTempFile("tempScript", ".sh");
        tempFile.writeText(fileContent)

        val commandLine: GeneralCommandLine = GeneralCommandLine()
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withWorkDirectory(workingDirectory)
            .withCharset(StandardCharsets.UTF_8)
            .withExePath("sh")
            .withParameters(tempFile.path)

        val processOutput = CapturingProcessHandler(commandLine).runProcess(DEFAULT_TIMEOUT)
        val exitCode = processOutput.exitCode
        if (exitCode != 0) {
            throw RuntimeException("Cannot execute ${commandLine}: exit code $exitCode, error output: ${processOutput.stderr}")
        }

        tempFile.delete()
        logger<ShireShellCommandRunner>().info("delete temp file: ${tempFile.path}")
        return processOutput.stdout
    }
}
