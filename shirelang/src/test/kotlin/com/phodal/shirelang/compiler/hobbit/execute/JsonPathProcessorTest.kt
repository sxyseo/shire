package com.phodal.shirelang.compiler.hobbit.execute

import com.phodal.shirelang.compiler.ast.patternaction.PatternActionFunc
import com.phodal.shirelang.compiler.execute.processor.JsonPathProcessor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class JsonPathProcessorTest {

    @Test
    fun `should parse JSON string with valid JSON path`() {
        // given
        val jsonStr = "{\"key\":\"value\"}"
        val action = PatternActionFunc.JsonPath(null, "key")

        // when
        val result = JsonPathProcessor.execute(jsonStr, action)

        // then
        assertEquals("value", result)
    }

    @Test
    fun `should return null when JSON path does not exist in JSON string`() {
        // given
        val jsonStr = "{\"key\":\"value\"}"
        val action = PatternActionFunc.JsonPath(null, "invalidKey")

        // when
        val result = JsonPathProcessor.execute(jsonStr, action)

        // then
        assertNull(result)
    }

    @Test
    fun `should parse SSE result with valid JSON path`() {
        // given
        val sseInput = "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n"
        val jsonPath = "answer"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("The", result)
    }

    @Test
    fun `should return empty string when JSON path does not exist in SSE result`() {
        // given
        val sseInput = "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n"
        val jsonPath = "invalidKey"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("", result)
    }

    @Test
    fun `should parse multiple SSE data lines with valid JSON path`() {
        // given
        val sseInput = "data: {\"event\":\"agent_message\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"answer\":\"The\"}\n" +
                "data: {\"event\":\"message_end\",\"conversation_id\":\"48929266-a58f-46cc-a5eb-33145e6a96ef\",\"message_id\":\"91ad550b-1109-4062-88f8-07be18238e0e\",\"created_at\":1725437154,\"task_id\":\"4f846104-8571-42f1-b04c-f6f034b2fe9e\",\"id\":\"91ad550b-1109-4062-88f8-07be18238e0e\"}\n"
        val jsonPath = "$.event"

        // when
        val result = JsonPathProcessor.parseSSEResult(sseInput, jsonPath)

        // then
        assertEquals("agent_messagemessage_end", result)
    }
}
