package com.github.hank9999.kirokt.kiro.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AWS Event Stream 消息类型
 */
@Serializable
enum class MessageType(val value: String) {
    @SerialName("event") EVENT("event"),
    @SerialName("error") ERROR("error"),
    @SerialName("exception") EXCEPTION("exception")
}

/**
 * 事件类型枚举
 */
@Serializable
enum class EventType(val value: String) {
    @SerialName("assistantResponseEvent") ASSISTANT_RESPONSE("assistantResponseEvent"),
    @SerialName("toolUseEvent") TOOL_USE("toolUseEvent"),
    @SerialName("meteringEvent") METERING("meteringEvent"),
    @SerialName("contextUsageEvent") CONTEXT_USAGE("contextUsageEvent"),
    @SerialName("tool_call_request") TOOL_CALL_REQUEST("tool_call_request"),
    @SerialName("tool_call_error") TOOL_CALL_ERROR("tool_call_error"),
    @SerialName("session_start") SESSION_START("session_start"),
    @SerialName("session_end") SESSION_END("session_end"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): EventType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 内容 MIME 类型
 */
@Serializable
enum class ContentType(val value: String) {
    @SerialName("text/markdown") TEXT_MARKDOWN("text/markdown"),
    @SerialName("text/plain") TEXT_PLAIN("text/plain"),
    @SerialName("application/json") APPLICATION_JSON("application/json")
}

/**
 * 消息状态
 */
@Serializable
enum class MessageStatus {
    @SerialName("COMPLETED") COMPLETED,
    @SerialName("IN_PROGRESS") IN_PROGRESS,
    @SerialName("ERROR") ERROR
}
