@file:OptIn(ExperimentalSerializationApi::class)

package com.github.hank9999.kirokt.anthropic.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

// ============ 消息 ============

/**
 * 消息内容 - 可以是字符串或内容块列表
 */
@Serializable
@JsonClassDiscriminator("_content_type")
sealed interface MessageContent

/**
 * 简单文本内容
 */
@Serializable
data class SimpleTextContent(
    val text: String
) : MessageContent

/**
 * 内容块列表
 */
@Serializable
data class BlocksContent(
    val blocks: List<ContentBlock>
) : MessageContent

/**
 * 消息
 */
@Serializable
data class Message(
    val role: Role,
    val content: JsonElement // 可以是 String 或 List<ContentBlock>
)

/**
 * 系统提示 - 可以是字符串或 TextBlock 列表
 */
@Serializable
@JsonClassDiscriminator("_system_type")
sealed interface SystemPrompt

@Serializable
data class SimpleSystemPrompt(
    val text: String
) : SystemPrompt

@Serializable
data class BlocksSystemPrompt(
    val blocks: List<TextBlock>
) : SystemPrompt

// ============ 请求 ============

/**
 * Messages API 请求
 */
@Serializable
data class MessagesRequest(
    val model: String,

    @SerialName("max_tokens")
    val maxTokens: Int,

    val messages: List<Message>,

    val system: JsonElement? = null, // String 或 List<TextBlock>

    val temperature: Double? = null,

    @SerialName("top_p")
    val topP: Double? = null,

    @SerialName("top_k")
    val topK: Int? = null,

    @SerialName("stop_sequences")
    val stopSequences: List<String>? = null,

    val stream: Boolean? = null,

    val tools: List<Tool>? = null,

    @SerialName("tool_choice")
    val toolChoice: ToolChoice? = null,

    val thinking: ThinkingConfig? = null,

    val metadata: Metadata? = null,

    @SerialName("service_tier")
    val serviceTier: String? = null
)

/**
 * Token 计数请求
 */
@Serializable
data class CountTokensRequest(
    val model: String,
    val messages: List<Message>,
    val system: JsonElement? = null,
    val tools: List<Tool>? = null,
    val thinking: ThinkingConfig? = null
)

// ============ 响应 ============

/**
 * Messages API 响应
 */
@Serializable
data class MessagesResponse(
    val id: String,
    val type: String = "message",
    val role: String = "assistant",
    val model: String,
    val content: List<ContentBlock>,
    @SerialName("stop_reason")
    val stopReason: StopReason?,
    @SerialName("stop_sequence")
    val stopSequence: String? = null,
    val usage: Usage
)

/**
 * Token 计数响应
 */
@Serializable
data class CountTokensResponse(
    @SerialName("input_tokens")
    val inputTokens: Int,

    @SerialName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int? = null,

    @SerialName("cache_read_input_tokens")
    val cacheReadInputTokens: Int? = null
)
