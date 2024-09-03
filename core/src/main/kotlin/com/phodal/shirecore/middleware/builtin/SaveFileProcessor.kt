package com.phodal.shirecore.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.phodal.shirecore.middleware.SHIRE_TEMP_OUTPUT
import com.phodal.shirecore.middleware.BuiltinPostHandler
import com.phodal.shirecore.middleware.PostCodeHandleContext
import com.phodal.shirecore.middleware.PostProcessor

class SaveFileProcessor : PostProcessor, Disposable {
    override val processorName: String = BuiltinPostHandler.SaveFile.handleName

    override fun isApplicable(context: PostCodeHandleContext): Boolean = true

    override fun execute(
        project: Project,
        context: PostCodeHandleContext,
        console: ConsoleView?,
        args: List<Any>,
    ): String {
        var fileName: String = ""
        val ext = getFileExt(context)
        if (args.isNotEmpty()) {
            fileName = args[0].toString()
            handleForProjectFile(project, fileName, context, console, ext)
        } else {
            fileName = "${System.currentTimeMillis()}.$ext"
            handleForTempFile(project, fileName, context, console)
        }


        return fileName
    }

    private fun getFileExt(context: PostCodeHandleContext): String {
        val language = context.genTargetLanguage ?: PlainTextLanguage.INSTANCE
        val ext = context.genTargetExtension ?: language?.associatedFileType?.defaultExtension ?: "txt"
        return ext
    }

    private fun handleForTempFile(
        project: Project,
        fileName: String,
        context: PostCodeHandleContext,
        console: ConsoleView?,
    ) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.compute<VirtualFile, Throwable> {
                val outputDir = project.guessProjectDir()?.findChild(SHIRE_TEMP_OUTPUT)
                    ?: project.guessProjectDir()?.createChildDirectory(this, SHIRE_TEMP_OUTPUT)

                val outputFile = outputDir?.createChildData(this, fileName)
                    ?: throw IllegalStateException("Failed to save file")

                val content = getContent(context)
                outputFile.setBinaryContent(content?.toByteArray() ?: ByteArray(0))
                context.pipeData["output"] = outputFile

                project.guessProjectDir()?.refresh(true, true)

                console?.print("Saved to ${outputFile.canonicalPath}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                outputFile
            }
        }
    }

    private fun handleForProjectFile(
        project: Project,
        fileName: String,
        context: PostCodeHandleContext,
        console: ConsoleView?,
        ext: String,
    ) {
        ApplicationManager.getApplication().invokeAndWait {
            WriteAction.compute<VirtualFile, Throwable> {
                val projectDir = project.guessProjectDir()
                // filename may include path, like: `src/main/java/HelloWorld.java`, we need to split it
                // first check if the file is already in the project
                var outputFile = projectDir?.findFileByRelativePath(fileName)
                if (projectDir?.findFileByRelativePath(fileName) == null) {
                    outputFile = createFile(fileName, projectDir)
                }

                val content = getContent(context)
                outputFile!!.setBinaryContent(content?.toByteArray() ?: ByteArray(0))
                context.pipeData["output"] = outputFile

                projectDir?.refresh(true, true)

                console?.print("Saved to ${outputFile.canonicalPath}", ConsoleViewContentType.SYSTEM_OUTPUT)
                outputFile
            }
        }
    }

    private fun getContent(context: PostCodeHandleContext): String? {
        val outputData = context.pipeData["output"]

        if (outputData is String && outputData.isNotEmpty()) {
            return outputData
        }

        if (context.lastTaskOutput?.isNotEmpty() == true) {
            return context.lastTaskOutput
        }

        return context.genText
    }

    private fun createFile(
        fileName: String,
        projectDir: VirtualFile?,
    ): VirtualFile {
        val path = fileName.split("/").dropLast(1)
        val name = fileName.split("/").last()

        var parentDir = projectDir

        // create directories if not exist
        for (dir in path) {
            parentDir = parentDir?.findChild(dir) ?: parentDir?.createChildDirectory(this, dir)
        }

        val outputFile = parentDir?.createChildData(this, name)
            ?: throw IllegalStateException("Failed to save file")

        return outputFile
    }

    override fun dispose() {
        Disposer.dispose(this)
    }
}

