package com.phodal.shirelang.java.codeedit

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.phodal.shirecore.provider.codeedit.CodeModifier

open class JavaCodeModifier : CodeModifier {
    private val log = logger<JavaCodeModifier>()

    override fun isApplicable(language: Language) = language is JavaLanguage

    override fun smartInsert(sourceFile: VirtualFile, project: Project, code: String): PsiElement? {
        val isTestFile = sourceFile.name.endsWith("Test.java")
        if (!isTestFile) {
            return insertTestCode(sourceFile, project, code)
        }

        return insertMethod(sourceFile, project, code)
    }

    private fun lookupFile(project: Project, sourceFile: VirtualFile) =
        PsiManager.getInstance(project).findFile(sourceFile) as PsiJavaFile

    /**
     * This function is used to insert test code into a specified source file in a Kotlin project.
     * It takes the source file, project, and the test code as parameters.
     *
     * The function first trims the test code by removing leading and trailing whitespaces, as well as any surrounding triple backticks and "java" prefix.
     * If the trimmed code does not contain the "@Test" annotation, a warning is logged and the method code is inserted into the source file.
     *
     * It then checks if the trimmed code is a full class code (starts with "import" or "package" and contains "class ").
     * If the source file already contains classes, the function inserts the test code into an existing class.
     *
     * If the trimmed code is a full class code, the function inserts a new class into the source file.
     *
     * If none of the above conditions are met, the function inserts the test code as a method into the source file.
     *
     * @param sourceFile The VirtualFile representing the source file where the test code will be inserted.
     * @param project The Project to which the source file belongs.
     * @param code The test code to be inserted into the source file.
     * @return Boolean value indicating whether the test code was successfully inserted.
     */
    override fun insertTestCode(sourceFile: VirtualFile, project: Project, code: String): PsiElement? {
        val trimCode = code.trim().removeSurrounding("```").removePrefix("java").trim()

        val isFullTestCode =
            (trimCode.startsWith("import") || trimCode.startsWith("package")) && trimCode.contains("class ")

        val existTestFileClasses = runReadAction { lookupFile(project, sourceFile).classes }
        val alreadyExtTestFile = existTestFileClasses.isNotEmpty()

        return when {
            alreadyExtTestFile -> return insertToExistClass(existTestFileClasses, project, trimCode)
            isFullTestCode -> return insertClass(sourceFile, project, trimCode)
            trimCode.contains("@Test") -> insertMethod(sourceFile, project, trimCode)
            else -> {
                log.warn("methodCode does not contain @Test annotation: $trimCode")
                insertMethod(sourceFile, project, trimCode)
            }
        }
    }

    private fun insertToExistClass(
        testFileClasses: Array<out PsiClass>,
        project: Project,
        trimCode: String,
    ): PsiElement? {
        // todo: check to naming testFile, but since Java only has One Class under file
        val lastClass = testFileClasses.last()
        val classEndOffset = runReadAction { lastClass.textRange.endOffset }

        val psiFile = try {
            PsiFileFactory.getInstance(project)
                .createFileFromText("Test.java", JavaLanguage.INSTANCE, trimCode)
        } catch (e: Throwable) {
            log.warn("Failed to create file from text: $trimCode", e)
            null
        }

        val newCode = psiFile?.text ?: trimCode
        try {
            val newClassMethods = runReadAction {
                psiFile?.children?.firstOrNull { it is PsiClass }?.children?.filterIsInstance<PsiMethod>()
            }

            WriteCommandAction.runWriteCommandAction(project) {
                newClassMethods?.forEach {
                    lastClass.add(it)
                }
            }

            return lastClass
        } catch (e: Throwable) {
            WriteCommandAction.runWriteCommandAction(project) {
                val document = PsiDocumentManager.getInstance(project).getDocument(lastClass.containingFile)
                document?.insertString(classEndOffset - 1, "\n    $newCode")
            }

            return lastClass
        }
    }

    override fun insertMethod(sourceFile: VirtualFile, project: Project, code: String): PsiElement? {
        val rootElement = runReadAction {
            val psiJavaFile = lookupFile(project, sourceFile)
            val psiClass = psiJavaFile.classes.firstOrNull()
            if (psiClass == null) {
                log.error("Failed to find PsiClass in the source file: $psiJavaFile, code: $code")
                return@runReadAction null
            }

            return@runReadAction psiClass
        } ?: return null

        val newTestMethod = ReadAction.compute<PsiMethod, Throwable> {
            val psiElementFactory = PsiElementFactory.getInstance(project)
            try {
                val methodCode = psiElementFactory.createMethodFromText(code, rootElement)
                if (rootElement.findMethodsByName(methodCode.name, false).isNotEmpty()) {
                    log.error("Method already exists in the class: ${methodCode.name}")
                }

                methodCode
            } catch (e: Throwable) {
                log.error("Failed to create method from text: $code", e)
                return@compute null
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            try {
                rootElement.add(newTestMethod)
            } catch (e: Throwable) {
                val classEndOffset = rootElement.textRange.endOffset
                val document = PsiDocumentManager.getInstance(project).getDocument(rootElement.containingFile)
                document?.insertString(classEndOffset - 1, "\n    ")
                document?.insertString(classEndOffset - 1 + "\n    ".length, newTestMethod.text)
            }
        }

        project.guessProjectDir()?.refresh(true, true)

        return newTestMethod
    }

    override fun insertClass(sourceFile: VirtualFile, project: Project, code: String): PsiElement? {
        return WriteCommandAction.runWriteCommandAction<PsiElement?>(project) {
            val psiFile = lookupFile(project, sourceFile)
            val document = psiFile.viewProvider.document!!
            document.insertString(document.textLength, code)

            psiFile.classes.firstOrNull()
        }
    }
}
