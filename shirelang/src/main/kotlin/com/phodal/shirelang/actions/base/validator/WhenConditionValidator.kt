package com.phodal.shirelang.actions.base.validator

import com.intellij.psi.PsiFile
import com.phodal.shirecore.provider.variable.model.ConditionPsiVariable
import com.phodal.shirelang.compiler.hobbit.ast.FrontMatterType
import com.phodal.shirelang.compiler.hobbit.ast.Statement

object WhenConditionValidator {
    private fun buildPsiVariable(file: PsiFile): Map<String, String> {
        return ConditionPsiVariable.values().associate {
            when (it) {
                ConditionPsiVariable.FILE_PATH -> it.variableName to file.virtualFile.path
                ConditionPsiVariable.FILE_NAME -> it.variableName to file.name
                ConditionPsiVariable.FILE_EXTENSION -> it.variableName to (file.virtualFile.extension ?: "")
                ConditionPsiVariable.FILE_CONTENT -> it.variableName to file.text
            }
        }
    }

    fun isAvailable(conditions: FrontMatterType.EXPRESSION, file: PsiFile): Boolean {
        return (conditions.value as? Statement)?.evaluate(buildPsiVariable(file)) == true
    }
}
