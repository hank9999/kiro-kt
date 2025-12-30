package com.github.hank9999.kirokt.converter

import com.github.hank9999.kirokt.anthropic.model.*
import com.github.hank9999.kirokt.kiro.model.*
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Anthropic 到 Kiro 协议转换器
 */
object AnthropicToKiroConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    // ============ 模型映射 ============

    /**
     * Anthropic 模型名映射到 Kiro 模型名
     */
    fun mapModel(anthropicModel: String): String {
        return when {
            anthropicModel.contains("opus", ignoreCase = true) -> "claude-opus-4.5"
            anthropicModel.contains("sonnet", ignoreCase = true) -> "claude-sonnet-4.5"
            anthropicModel.contains("haiku", ignoreCase = true) -> "claude-haiku-4.5"
            else -> "claude-sonnet-4.5" // 默认使用 sonnet
        }
    }

    // ============ 主转换函数 ============

    /**
     * 转换 Anthropic 请求到 Kiro 请求
     */
    fun convert(request: MessagesRequest): KiroRequest {
        val kiroModelId = mapModel(request.model)
        val conversationId = UUID.randomUUID().toString()
        val agentContinuationId = UUID.randomUUID().toString()

        // 构建历史消息和当前消息
        val (history, currentMessage) = buildHistoryAndCurrentMessage(
            request = request,
            modelId = kiroModelId
        )

        // 构建工具定义
        val tools = request.tools?.let { convertTools(it) }

        // 构建当前消息的上下文
        val currentMessageContext = if (tools != null) {
            UserInputMessageContext(tools = tools)
        } else {
            null
        }

        // 获取当前消息内容和附加信息
        val currentContent = currentMessage.first
        val currentImages = currentMessage.second
        val currentToolResults = currentMessage.third

        // 合并工具结果到上下文
        val finalContext = when {
            currentMessageContext != null && currentToolResults != null -> {
                currentMessageContext.copy(toolResults = currentToolResults)
            }
            currentMessageContext != null -> currentMessageContext
            currentToolResults != null -> UserInputMessageContext(toolResults = currentToolResults)
            else -> null
        }

        return KiroRequest(
            conversationState = ConversationState(
                conversationId = conversationId,
                agentContinuationId = agentContinuationId,
                currentMessage = CurrentMessage(
                    userInputMessage = UserInputMessage(
                        content = currentContent,
                        modelId = kiroModelId,
                        images = currentImages,
                        userInputMessageContext = finalContext
                    )
                ),
                history = history
            )
        )
    }

    // ============ 历史消息构建 ============

    /**
     * 构建历史消息和当前消息
     * 返回: Pair<历史消息列表, Triple<当前内容, 当前图片, 当前工具结果>>
     */
    private fun buildHistoryAndCurrentMessage(
        request: MessagesRequest,
        modelId: String
    ): Pair<List<HistoryEntry>, Triple<String, List<KiroImage>?, List<KiroToolResult>?>> {
        val history = mutableListOf<HistoryEntry>()

        // 1. 处理系统提示词 -> 转换为历史的 user + assistant 配对
        val systemPrompt = extractSystemPrompt(request.system)

        // 注入扩展思考配置到系统提示词
        val finalSystemPrompt = injectThinkingConfig(systemPrompt, request.thinking)

        if (finalSystemPrompt.isNotEmpty()) {
            // 用户消息（系统提示词）
            history.add(HistoryEntry(
                userInputMessage = HistoryUserInputMessage(
                    content = finalSystemPrompt,
                    modelId = modelId
                )
            ))
            // 助手确认消息
            history.add(HistoryEntry(
                assistantResponseMessage = HistoryAssistantResponseMessage(
                    content = "I will follow these instructions."
                )
            ))
        }

        // 2. 处理消息历史
        val messages = request.messages
        if (messages.isEmpty()) {
            return Pair(history, Triple("", null, null))
        }

        // 将消息分为历史消息和当前消息
        // 最后一条用户消息作为当前消息，之前的都是历史
        val lastUserMessageIndex = messages.indexOfLast { it.role == Role.USER }

        if (lastUserMessageIndex == -1) {
            // 没有用户消息，返回空当前消息
            return Pair(history, Triple("", null, null))
        }

        // 处理历史消息（最后一条用户消息之前的所有消息）
        val historyMessages = if (lastUserMessageIndex > 0) {
            messages.subList(0, lastUserMessageIndex)
        } else {
            emptyList()
        }

        // 转换历史消息
        history.addAll(convertMessagesToHistory(historyMessages, modelId))

        // 处理当前消息（最后一条用户消息）
        val currentUserMessage = messages[lastUserMessageIndex]
        val (currentContent, currentImages, currentToolResults) = extractMessageContent(currentUserMessage)

        return Pair(history, Triple(currentContent, currentImages, currentToolResults))
    }

    /**
     * 将 Anthropic 消息列表转换为 Kiro 历史消息
     */
    private fun convertMessagesToHistory(
        messages: List<Message>,
        modelId: String
    ): List<HistoryEntry> {
        val history = mutableListOf<HistoryEntry>()
        var i = 0

        while (i < messages.size) {
            val message = messages[i]

            when (message.role) {
                Role.USER -> {
                    val (content, images, toolResults) = extractMessageContent(message)

                    val userInputContext = if (toolResults != null) {
                        UserInputMessageContext(toolResults = toolResults)
                    } else {
                        null
                    }

                    history.add(HistoryEntry(
                        userInputMessage = HistoryUserInputMessage(
                            content = content,
                            modelId = modelId,
                            images = images,
                            userInputMessageContext = userInputContext
                        )
                    ))

                    // 检查下一条是否是助手消息
                    if (i + 1 < messages.size && messages[i + 1].role == Role.ASSISTANT) {
                        // 下一条是助手消息，稍后处理
                    } else {
                        // 需要补充一个空的助手响应
                        history.add(HistoryEntry(
                            assistantResponseMessage = HistoryAssistantResponseMessage(content = "")
                        ))
                    }
                }

                Role.ASSISTANT -> {
                    val (content, toolUses) = extractAssistantContent(message)

                    history.add(HistoryEntry(
                        assistantResponseMessage = HistoryAssistantResponseMessage(
                            content = content,
                            toolUses = toolUses
                        )
                    ))

                    // 检查前一条是否是用户消息
                    if (history.isEmpty() || history.last().userInputMessage == null) {
                        // 需要在前面补充一个空的用户消息
                        val lastEntry = history.removeLastOrNull()
                        history.add(HistoryEntry(
                            userInputMessage = HistoryUserInputMessage(
                                content = "",
                                modelId = modelId
                            )
                        ))
                        if (lastEntry != null) {
                            history.add(lastEntry)
                        }
                    }
                }
            }
            i++
        }

        return history
    }

    // ============ 内容提取 ============

    /**
     * 提取用户消息内容
     * 返回: Triple<文本内容, 图片列表, 工具结果列表>
     */
    private fun extractMessageContent(message: Message): Triple<String, List<KiroImage>?, List<KiroToolResult>?> {
        val contentElement = message.content
        val textParts = mutableListOf<String>()
        val images = mutableListOf<KiroImage>()
        val toolResults = mutableListOf<KiroToolResult>()

        when {
            // 简单字符串内容
            contentElement is JsonPrimitive && contentElement.isString -> {
                textParts.add(contentElement.content)
            }
            // 内容块数组
            contentElement is JsonArray -> {
                for (block in contentElement) {
                    if (block !is JsonObject) continue

                    val type = block["type"]?.jsonPrimitive?.contentOrNull ?: continue

                    when (type) {
                        "text" -> {
                            val text = block["text"]?.jsonPrimitive?.contentOrNull
                            if (text != null) {
                                textParts.add(text)
                            }
                        }
                        "image" -> {
                            val image = convertImageBlock(block)
                            if (image != null) {
                                images.add(image)
                            }
                        }
                        "tool_result" -> {
                            val toolResult = convertToolResultBlock(block)
                            if (toolResult != null) {
                                toolResults.add(toolResult)
                            }
                        }
                    }
                }
            }
        }

        return Triple(
            textParts.joinToString("\n"),
            images.takeIf { it.isNotEmpty() },
            toolResults.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * 提取助手消息内容
     * 返回: Pair<文本内容, 工具使用列表>
     */
    private fun extractAssistantContent(message: Message): Pair<String, List<KiroToolUseEntry>?> {
        val contentElement = message.content
        val textParts = mutableListOf<String>()
        val toolUses = mutableListOf<KiroToolUseEntry>()

        when {
            // 简单字符串内容
            contentElement is JsonPrimitive && contentElement.isString -> {
                textParts.add(contentElement.content)
            }
            // 内容块数组
            contentElement is JsonArray -> {
                for (block in contentElement) {
                    if (block !is JsonObject) continue

                    val type = block["type"]?.jsonPrimitive?.contentOrNull ?: continue

                    when (type) {
                        "text" -> {
                            val text = block["text"]?.jsonPrimitive?.contentOrNull
                            if (text != null) {
                                textParts.add(text)
                            }
                        }
                        "tool_use" -> {
                            val toolUse = convertToolUseBlock(block)
                            if (toolUse != null) {
                                toolUses.add(toolUse)
                            }
                        }
                    }
                }
            }
        }

        return Pair(
            textParts.joinToString("\n"),
            toolUses.takeIf { it.isNotEmpty() }
        )
    }

    // ============ 系统提示词处理 ============

    /**
     * 提取系统提示词
     */
    private fun extractSystemPrompt(system: JsonElement?): String {
        if (system == null) return ""

        return when {
            // 简单字符串
            system is JsonPrimitive && system.isString -> {
                system.content
            }
            // TextBlock 数组
            system is JsonArray -> {
                system.mapNotNull { block ->
                    if (block is JsonObject) {
                        block["text"]?.jsonPrimitive?.contentOrNull
                    } else null
                }.joinToString("\n")
            }
            else -> ""
        }
    }

    /**
     * 注入扩展思考配置到提示词
     */
    private fun injectThinkingConfig(systemPrompt: String, thinking: ThinkingConfig?): String {
        if (thinking == null || thinking is ThinkingDisabled) {
            return systemPrompt
        }

        val thinkingEnabled = thinking as? ThinkingEnabled ?: return systemPrompt

        val thinkingInstructions = buildString {
            appendLine("<thinking_mode>enabled</thinking_mode>")
            appendLine("<max_thinking_length>${thinkingEnabled.budgetTokens}</max_thinking_length>")
        }

        return if (systemPrompt.isEmpty()) {
            thinkingInstructions
        } else {
            "$systemPrompt\n\n$thinkingInstructions"
        }
    }

    // ============ 图片转换 ============

    /**
     * 转换图片块
     */
    private fun convertImageBlock(block: JsonObject): KiroImage? {
        val source = block["source"] as? JsonObject ?: return null
        val sourceType = source["type"]?.jsonPrimitive?.contentOrNull

        return when (sourceType) {
            "base64" -> {
                val mediaType = source["media_type"]?.jsonPrimitive?.contentOrNull ?: return null
                val data = source["data"]?.jsonPrimitive?.contentOrNull ?: return null

                // 从 media_type 提取格式 (如 "image/jpeg" -> "jpeg")
                val format = mediaType.substringAfter("image/", "jpeg")

                KiroImage.fromBase64(format, data)
            }
            "url" -> {
                // Kiro 不支持 URL 图片，返回 null
                null
            }
            else -> null
        }
    }

    // ============ 工具转换 ============

    /**
     * 转换工具定义列表
     */
    private fun convertTools(tools: List<Tool>): List<KiroToolWrapper> {
        return tools.mapNotNull { tool ->
            when (tool) {
                is CustomTool -> {
                    // 过滤 web_search 工具
                    if (tool.name.equals("web_search", ignoreCase = true) ||
                        tool.name.equals("websearch", ignoreCase = true)) {
                        return@mapNotNull null
                    }

                    KiroToolWrapper(
                        toolSpecification = KiroToolSpecification(
                            name = tool.name,
                            description = tool.description ?: "",
                            inputSchema = KiroInputSchema(
                                json = convertInputSchema(tool.inputSchema)
                            )
                        )
                    )
                }
                // 其他内置工具类型暂不支持
                else -> null
            }
        }
    }

    /**
     * 转换输入 Schema
     */
    private fun convertInputSchema(schema: InputSchema): JsonElement {
        return buildJsonObject {
            put("type", schema.type)
            schema.properties?.let { props ->
                put("properties", JsonObject(props))
            }
            schema.required?.let { req ->
                put("required", JsonArray(req.map { JsonPrimitive(it) }))
            }
            // 不包含 additionalProperties，Kiro 可能不支持
        }
    }

    /**
     * 转换工具使用块
     */
    private fun convertToolUseBlock(block: JsonObject): KiroToolUseEntry? {
        val id = block["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = block["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val input = block["input"] ?: JsonObject(emptyMap())

        return KiroToolUseEntry(
            toolUseId = id,
            name = name,
            input = input
        )
    }

    /**
     * 转换工具结果块
     */
    private fun convertToolResultBlock(block: JsonObject): KiroToolResult? {
        val toolUseId = block["tool_use_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val isError = block["is_error"]?.jsonPrimitive?.booleanOrNull ?: false
        val contentElement = block["content"]

        val content = extractToolResultContent(contentElement)

        return KiroToolResult(
            toolUseId = toolUseId,
            content = content,
            status = if (isError) "error" else "success",
            isError = isError
        )
    }

    /**
     * 提取工具结果内容
     */
    private fun extractToolResultContent(content: JsonElement?): List<KiroToolResultContent> {
        if (content == null) {
            return listOf(KiroToolResultContent(text = ""))
        }

        return when {
            // 简单字符串
            content is JsonPrimitive && content.isString -> {
                listOf(KiroToolResultContent(text = content.content))
            }
            // 内容块数组
            content is JsonArray -> {
                content.mapNotNull { block ->
                    if (block is JsonObject) {
                        val type = block["type"]?.jsonPrimitive?.contentOrNull
                        when (type) {
                            "text" -> {
                                val text = block["text"]?.jsonPrimitive?.contentOrNull ?: ""
                                KiroToolResultContent(text = text)
                            }
                            "image" -> {
                                // 工具结果中的图片，转换为文本描述
                                KiroToolResultContent(text = "[Image content]")
                            }
                            else -> null
                        }
                    } else null
                }.ifEmpty { listOf(KiroToolResultContent(text = "")) }
            }
            else -> listOf(KiroToolResultContent(text = ""))
        }
    }
}
