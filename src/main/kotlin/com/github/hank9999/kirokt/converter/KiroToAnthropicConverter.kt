package com.github.hank9999.kirokt.converter

import com.github.hank9999.kirokt.anthropic.model.*
import com.github.hank9999.kirokt.kiro.model.*
import com.github.hank9999.kirokt.kiro.model.Event as KiroEvent
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Kiro 响应到 Anthropic SSE 的转换器
 *
 * 负责将 Kiro 的事件流转换为 Anthropic 兼容的 SSE 事件
 */
class KiroToAnthropicConverter(
    private val messageId: String = generateMessageId(),
    private val model: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    // 当前内容块索引
    private var currentBlockIndex = 0

    // 是否已发送 message_start
    private var messageStartSent = false

    // 是否有活动的文本块
    private var textBlockActive = false

    // 当前工具调用状态
    private var toolUseBlockActive = false
    private var currentToolUseId: String? = null
    private var currentToolName: String? = null
    private var toolInputBuffer = StringBuilder()

    // 输入/输出 token 计数 (从 contextUsageEvent 获取)
    private var inputTokens = 0
    // output_tokens 固定为 1
    private val outputTokens = 1

    companion object {
        fun generateMessageId(): String {
            return "msg_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        }
    }

    /**
     * 生成 message_start 事件
     */
    fun generateMessageStart(): String {
        messageStartSent = true
        val event = MessageStartEvent(
            message = MessagesResponse(
                id = messageId,
                model = model,
                content = emptyList(),
                stopReason = null,
                usage = Usage(inputTokens = inputTokens, outputTokens = 1)
            )
        )
        return formatSSEEvent("message_start", json.encodeToString(MessageStartEvent.serializer(), event))
    }

    /**
     * 处理 Kiro 事件并生成 Anthropic SSE 事件
     */
    fun processEvent(event: KiroEvent): List<String> {
        val sseEvents = mutableListOf<String>()

        // 确保 message_start 已发送
        if (!messageStartSent) {
            sseEvents.add(generateMessageStart())
        }

        when (event) {
            is KiroEvent.AssistantResponse -> {
                sseEvents.addAll(processAssistantResponse(event.data))
            }
            is KiroEvent.ToolUse -> {
                sseEvents.addAll(processToolUse(event.data))
            }
            is KiroEvent.ContextUsage -> {
                // 从上下文使用百分比计算实际的 input_tokens
                // 公式: percentage * 200000 / 100 = percentage * 2000
                inputTokens = (event.data.contextUsagePercentage * 200_000 / 100.0).toInt()
            }
            is KiroEvent.SessionEnd -> {
                sseEvents.addAll(generateMessageEnd(StopReason.END_TURN))
            }
            is KiroEvent.Error -> {
                sseEvents.add(generateErrorEvent(event.errorCode, event.errorMessage))
            }
            is KiroEvent.Exception -> {
                sseEvents.add(generateErrorEvent(event.exceptionType, event.message))
            }
            else -> {
                // 其他事件类型暂时忽略
            }
        }

        return sseEvents
    }

    /**
     * 处理助手响应事件
     */
    private fun processAssistantResponse(event: AssistantResponseEvent): List<String> {
        val sseEvents = mutableListOf<String>()

        // 如果有工具块正在进行，先关闭它
        if (toolUseBlockActive) {
            sseEvents.addAll(closeToolUseBlock())
        }

        // 如果文本块未激活，先发送 content_block_start
        if (!textBlockActive && event.content.isNotEmpty()) {
            sseEvents.add(generateContentBlockStart(TextBlock(text = "")))
            textBlockActive = true
        }

        // 发送文本增量
        if (event.content.isNotEmpty()) {
            sseEvents.add(generateTextDelta(event.content))
        }

        // 如果消息已完成，关闭文本块并发送结束事件
        if (event.isCompleted) {
            if (textBlockActive) {
                sseEvents.add(generateContentBlockStop())
                textBlockActive = false
            }
            sseEvents.addAll(generateMessageEnd(StopReason.END_TURN))
        }

        return sseEvents
    }

    /**
     * 处理工具使用事件
     */
    private fun processToolUse(event: ToolUseEvent): List<String> {
        val sseEvents = mutableListOf<String>()

        // 如果文本块正在进行，先关闭它
        if (textBlockActive) {
            sseEvents.add(generateContentBlockStop())
            textBlockActive = false
            currentBlockIndex++
        }

        // 处理工具使用事件
        if (!toolUseBlockActive) {
            // 开始新的工具使用块
            currentToolUseId = event.toolUseId
            currentToolName = event.name
            toolInputBuffer.clear()

            val toolUseBlock = buildJsonObject {
                put("type", "tool_use")
                put("id", event.toolUseId)
                put("name", event.name)
                put("input", JsonObject(emptyMap()))
            }

            sseEvents.add(generateContentBlockStartRaw(toolUseBlock))
            toolUseBlockActive = true
        }

        // 累积工具输入
        val inputStr = when (val input = event.input) {
            is JsonPrimitive -> input.content
            is JsonObject -> json.encodeToString(JsonObject.serializer(), input)
            is JsonArray -> json.encodeToString(JsonArray.serializer(), input)
            else -> ""
        }

        if (inputStr.isNotEmpty()) {
            toolInputBuffer.append(inputStr)
            sseEvents.add(generateInputJsonDelta(inputStr))
        }

        // 如果工具调用完成
        if (event.isCompleted) {
            sseEvents.addAll(closeToolUseBlock())
            // 工具调用完成后，需要发送 message_delta 和 message_stop
            sseEvents.addAll(generateMessageEnd(StopReason.TOOL_USE))
        }

        return sseEvents
    }

    /**
     * 关闭工具使用块
     */
    private fun closeToolUseBlock(): List<String> {
        val sseEvents = mutableListOf<String>()

        if (toolUseBlockActive) {
            sseEvents.add(generateContentBlockStop())
            toolUseBlockActive = false
            currentToolUseId = null
            currentToolName = null
            toolInputBuffer.clear()
            currentBlockIndex++
        }

        return sseEvents
    }

    /**
     * 生成 content_block_start 事件
     */
    private fun generateContentBlockStart(block: ContentBlock): String {
        val event = ContentBlockStartEvent(
            index = currentBlockIndex,
            contentBlock = block
        )
        return formatSSEEvent("content_block_start", json.encodeToString(ContentBlockStartEvent.serializer(), event))
    }

    /**
     * 生成 content_block_start 事件 (原始 JSON)
     */
    private fun generateContentBlockStartRaw(block: JsonObject): String {
        val eventJson = buildJsonObject {
            put("type", "content_block_start")
            put("index", currentBlockIndex)
            put("content_block", block)
        }
        return formatSSEEvent("content_block_start", json.encodeToString(JsonObject.serializer(), eventJson))
    }

    /**
     * 生成文本增量事件
     */
    private fun generateTextDelta(text: String): String {
        val event = ContentBlockDeltaEvent(
            index = currentBlockIndex,
            delta = TextDelta(text = text)
        )
        return formatSSEEvent("content_block_delta", json.encodeToString(ContentBlockDeltaEvent.serializer(), event))
    }

    /**
     * 生成工具输入 JSON 增量事件
     */
    private fun generateInputJsonDelta(partialJson: String): String {
        val event = ContentBlockDeltaEvent(
            index = currentBlockIndex,
            delta = InputJsonDelta(partialJson = partialJson)
        )
        return formatSSEEvent("content_block_delta", json.encodeToString(ContentBlockDeltaEvent.serializer(), event))
    }

    /**
     * 生成 content_block_stop 事件
     */
    private fun generateContentBlockStop(): String {
        val event = ContentBlockStopEvent(index = currentBlockIndex)
        return formatSSEEvent("content_block_stop", json.encodeToString(ContentBlockStopEvent.serializer(), event))
    }

    /**
     * 生成消息结束事件
     */
    fun generateMessageEnd(stopReason: StopReason): List<String> {
        val sseEvents = mutableListOf<String>()

        // 如果有活动的文本块，先关闭
        if (textBlockActive) {
            sseEvents.add(generateContentBlockStop())
            textBlockActive = false
        }

        // 如果有活动的工具块，先关闭
        if (toolUseBlockActive) {
            sseEvents.addAll(closeToolUseBlock())
        }

        // message_delta 事件
        val messageDeltaEvent = MessageDeltaEvent(
            delta = MessageDeltaData(stopReason = stopReason),
            usage = Usage(inputTokens = inputTokens, outputTokens = outputTokens)
        )
        sseEvents.add(formatSSEEvent("message_delta", json.encodeToString(MessageDeltaEvent.serializer(), messageDeltaEvent)))

        // message_stop 事件
        sseEvents.add(formatSSEEvent("message_stop", json.encodeToString(MessageStopEvent.serializer(), MessageStopEvent)))

        return sseEvents
    }

    /**
     * 生成错误事件
     */
    private fun generateErrorEvent(errorType: String, errorMessage: String): String {
        val event = StreamErrorEvent(
            error = ApiError(
                type = ErrorType.API_ERROR.value,
                message = "$errorType: $errorMessage"
            )
        )
        return formatSSEEvent("error", json.encodeToString(StreamErrorEvent.serializer(), event))
    }

    /**
     * 格式化 SSE 事件
     */
    private fun formatSSEEvent(event: String, data: String): String {
        return "event: $event\ndata: $data\n\n"
    }
}
