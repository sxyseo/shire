package com.phodal.shirelang.kotlin.codemodel

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.phodal.shirecore.codemodel.FileStructureProvider
import com.phodal.shirecore.codemodel.model.FileStructure
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective

class KotlinFileStructureProvider : FileStructureProvider {
    override fun build(psiFile: PsiFile): FileStructure? {
        val name = psiFile.name
        val path = psiFile.virtualFile?.path ?: ""

        val packageDirective = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtPackageDirective::class.java).firstOrNull()
        val packageName = packageDirective?.text ?: ""

        val importList = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtImportList::class.java)
        val imports = importList.flatMap { it.imports }

        val classOrObjects = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtClassOrObject::class.java)
        val namedFunctions = PsiTreeUtil.getChildrenOfTypeAsList(psiFile, KtNamedFunction::class.java)

        return FileStructure(psiFile, name, path, packageName, imports, classOrObjects, namedFunctions)
    }
}