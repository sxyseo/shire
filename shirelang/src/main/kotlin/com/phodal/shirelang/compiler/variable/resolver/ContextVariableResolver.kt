package com.phodal.shirelang.compiler.variable.resolver

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiNameIdentifierOwner
import com.phodal.shirelang.compiler.variable.resolver.base.VariableResolver
import com.phodal.shirelang.compiler.variable.resolver.base.VariableResolverContext
import com.phodal.shirelang.compiler.variable.ContextVariable
import com.phodal.shirelang.compiler.variable.ContextVariable.*

class ContextVariableResolver(
    private val context: VariableResolverContext,
) : VariableResolver {
    fun all(): List<ContextVariable> = entries

    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, String> = ReadAction.compute<Map<String, String>, Throwable> {
        val file = context.element?.containingFile
        val caretModel = context.editor.caretModel

        all().associate {
            it.variableName to when (it) {
                SELECTION -> context.editor.selectionModel.selectedText ?: ""
                BEFORE_CURSOR -> file?.text?.substring(0, caretModel.offset) ?: ""
                AFTER_CURSOR -> file?.text?.substring(caretModel.offset) ?: ""
                FILE_NAME -> file?.name ?: ""
                FILE_PATH -> file?.virtualFile?.path ?: ""
                METHOD_NAME -> when (context.element) {
                    is PsiNameIdentifierOwner -> (context.element as PsiNameIdentifierOwner).nameIdentifier?.text
                        ?: ""

                    else -> ""
                }

                LANGUAGE -> context.element?.language?.displayName ?: ""
                COMMENT_SYMBOL -> when (context.element?.language?.displayName?.lowercase()) {
                    "java", "kotlin" -> "//"
                    "python" -> "#"
                    "javascript" -> "//"
                    "typescript" -> "//"
                    "go" -> "//"
                    "c", "c++", "c#" -> "//"
                    "rust" -> "//"
                    "ruby" -> "#"
                    "shell" -> "#"
                    else -> "-"
                }

                ALL -> file?.text ?: ""
            }
        }
    }
}