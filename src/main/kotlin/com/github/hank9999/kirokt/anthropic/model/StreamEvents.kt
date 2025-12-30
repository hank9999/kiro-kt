@file:OptIn(ExperimentalSerializationApi::class)

package com.github.hank9999.kirokt.anthropic.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// ============ SSE 流式事件 ============

/**
 * 流式事件类型
 */
enum class StreamEventType {
    MESSAGE_START,
    CONTENT_BLOCK_START,
    CONTENT_BLOCK_DELTA,
    CONTENT_BLOCK_STOP,
    MESSAGE_DELTA,
    MESSAGE_STOP,
    PING,
    ERROR
}

/**
 * 流式事件 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface StreamEvent {
    val type: String
}

/**
 * 消息开始事件
 */
@Serializable
@SerialName("message_start")
data class MessageStartEvent(
    override val type: String = "message_start",
    val message: MessagesResponse
) : StreamEvent

/**
 * 内容块开始事件
 */
@Serializable
@SerialName("content_block_start")
data class ContentBlockStartEvent(
    override val type: String = "content_block_start",
    val index: Int,
    @SerialName("content_block")
    val contentBlock: ContentBlock
) : StreamEvent

/**
 * 内容块增量事件
 */
@Serializable
@SerialName("content_block_delta")
data class ContentBlockDeltaEvent(
    override val type: String = "content_block_delta",
    val index: Int,
    val delta: Delta
) : StreamEvent

/**
 * 内容块停止事件
 */
@Serializable
@SerialName("content_block_stop")
data class ContentBlockStopEvent(
    override val type: String = "content_block_stop",
    val index: Int
) : StreamEvent

/**
 * 消息增量数据
 */
@Serializable
data class MessageDeltaData(
    @SerialName("stop_reason")
    val stopReason: StopReason? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null
)

/**
 * 消息增量事件
 */
@Serializable
@SerialName("message_delta")
data class MessageDeltaEvent(
    override val type: String = "message_delta",
    val delta: MessageDeltaData,
    val usage: Usage? = null
) : StreamEvent

/**
 * 消息停止事件
 */
@Serializable
@SerialName("message_stop")
data class MessageStopEvent(
    override val type: String = "message_stop"
) : StreamEvent

/**
 * Ping 事件
 */
@Serializable
@SerialName("ping")
data class PingEvent(
    override val type: String = "ping"
) : StreamEvent

/**
 * 错误事件
 */
@Serializable
@SerialName("error")
data class StreamErrorEvent(
    override val type: String = "error",
    val error: ApiError
) : StreamEvent

// ============ Delta 类型 ============

/**
 * Delta 增量 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface Delta {
    val type: String
}

/**
 * 文本增量
 */
@Serializable
@SerialName("text_delta")
data class TextDelta(
    override val type: String = "text_delta",
    val text: String
) : Delta

/**
 * 工具输入 JSON 增量
 */
@Serializable
@SerialName("input_json_delta")
data class InputJsonDelta(
    override val type: String = "input_json_delta",
    @SerialName("partial_json")
    val partialJson: String
) : Delta

/**
 * 思考增量
 */
@Serializable
@SerialName("thinking_delta")
data class ThinkingDelta(
    override val type: String = "thinking_delta",
    val thinking: String
) : Delta

/**
 * 签名增量
 */
@Serializable
@SerialName("signature_delta")
data class SignatureDelta(
    override val type: String = "signature_delta",
    val signature: String
) : Delta
