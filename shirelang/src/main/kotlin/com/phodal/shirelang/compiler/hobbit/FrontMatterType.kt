package com.phodal.shirelang.compiler.hobbit

import com.intellij.psi.tree.IElementType
import com.phodal.shirelang.psi.ShireTypes

class ShirePatternAction(val pattern: String, val processors: List<PatternFun>)

/**
 * Basic Types: [STRING], [NUMBER], [DATE], [BOOLEAN], [ARRAY], [OBJECT]
 * Pattern: [PATTERN]
 * Expression: [CaseMatch]
 */
sealed class FrontMatterType(val value: Any) {
    class STRING(value: String) : FrontMatterType(value)
    class NUMBER(value: Int) : FrontMatterType(value)
    class DATE(value: String) : FrontMatterType(value)
    class BOOLEAN(value: Boolean) : FrontMatterType(value)
    class ARRAY(value: List<FrontMatterType>) : FrontMatterType(value)
    class OBJECT(value: Map<String, FrontMatterType>) : FrontMatterType(value)

    /**
     * The default pattern action handles for processing
     */
    class PATTERN(value: ShirePatternAction) : FrontMatterType(value)

    /**
     * The case match for the front matter.
     */
    class CaseMatch(value: Map<String, PATTERN>) : FrontMatterType(value)
}
