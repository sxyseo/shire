package com.phodal.shirelang.compiler.variable.resolver

import com.phodal.shirelang.compiler.execute.PatternActionProcessor
import com.phodal.shirelang.compiler.variable.resolver.base.VariableResolver
import com.phodal.shirelang.compiler.variable.resolver.base.VariableResolverContext

class UserCustomVariableResolver(
    private val context: VariableResolverContext
) : VariableResolver {
    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, String> {
        var vars: MutableMap<String, Any?> = initVariables.toMutableMap()
        return context.hole?.variables?.mapValues {
            PatternActionProcessor(context.myProject, context.hole, vars).execute(it.value)
        } ?: emptyMap()
    }
}