package com.github.hank9999.kirokt.anthropic.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 消息角色
 */
@Serializable
enum class Role {
    @SerialName("user")
    USER,

    @SerialName("assistant")
    ASSISTANT
}

/**
 * 停止原因
 */
@Serializable
enum class StopReason {
    @SerialName("end_turn")
    END_TURN,

    @SerialName("max_tokens")
    MAX_TOKENS,

    @SerialName("stop_sequence")
    STOP_SEQUENCE,

    @SerialName("tool_use")
    TOOL_USE,

    @SerialName("pause_turn")
    PAUSE_TURN,

    @SerialName("refusal")
    REFUSAL
}

/**
 * 图像媒体类型
 */
@Serializable
enum class ImageMediaType {
    @SerialName("image/jpeg")
    JPEG,

    @SerialName("image/png")
    PNG,

    @SerialName("image/gif")
    GIF,

    @SerialName("image/webp")
    WEBP
}

/**
 * 文档媒体类型
 */
@Serializable
enum class DocumentMediaType {
    @SerialName("application/pdf")
    PDF,

    @SerialName("text/plain")
    PLAIN_TEXT,

    @SerialName("text/html")
    HTML,

    @SerialName("text/markdown")
    MARKDOWN
}

/**
 * 缓存控制 TTL
 */
@Serializable
enum class CacheTTL {
    @SerialName("5m")
    FIVE_MINUTES,

    @SerialName("1h")
    ONE_HOUR
}

/**
 * 服务层级
 */
@Serializable
enum class ServiceTier {
    @SerialName("auto")
    AUTO,

    @SerialName("standard_only")
    STANDARD_ONLY
}
