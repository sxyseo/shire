package com.phodal.shirecore.middleware.builtin

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.phodal.shirecore.middleware.PostProcessorType
import com.phodal.shirecore.middleware.PostProcessorContext
import com.phodal.shirecore.middleware.PostProcessor

class TimeMetricProcessor : PostProcessor {
    private var startTime: Long? = null

    override val processorName: String = PostProcessorType.TimeMetric.handleName

    override fun isApplicable(context: PostProcessorContext): Boolean = true

    override fun setup(context: PostProcessorContext): String {
        startTime = System.currentTimeMillis()
        return startTime.toString()
    }

    override fun execute(project: Project, context: PostProcessorContext, console: ConsoleView?, args: List<Any>): String {
        val endTime = System.currentTimeMillis()
        return (endTime - startTime!!).toString()
    }
}
