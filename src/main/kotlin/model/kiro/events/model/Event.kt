package model.kiro.events.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 统一事件封装（密封类）
 *
 * 封装所有 Kiro 事件类型，支持类型安全的事件处理。
 *
 * 事件解析流程:
 * ```
 * Frame
 *   ├─ :message-type = "event"    → parseEvent()
 *   │     ├─ :event-type = "assistantResponseEvent" → AssistantResponse
 *   │     ├─ :event-type = "toolUseEvent"           → ToolUse
 *   │     ├─ :event-type = "meteringEvent"          → Metering
 *   │     ├─ :event-type = "contextUsageEvent"      → ContextUsage
 *   │     ├─ :event-type = "tool_call_request"      → ToolCallRequest
 *   │     ├─ :event-type = "tool_call_error"        → ToolCallError
 *   │     ├─ :event-type = "session_start"          → SessionStart
 *   │     ├─ :event-type = "session_end"            → SessionEnd
 *   │     └─ 其他                                    → Unknown
 *   ├─ :message-type = "error"    → parseError()    → Error
 *   └─ :message-type = "exception"→ parseException()→ Exception
 * ```
 */
@Serializable
sealed class Event {
    /** 获取事件类型 */
    abstract val eventType: EventType

    /**
     * 助手响应事件
     */
    @Serializable
    data class AssistantResponse(val data: AssistantResponseEvent) : Event() {
        override val eventType: EventType = EventType.ASSISTANT_RESPONSE
    }

    /**
     * 工具使用事件
     */
    @Serializable
    data class ToolUse(val data: ToolUseEvent) : Event() {
        override val eventType: EventType = EventType.TOOL_USE
    }

    /**
     * 计费事件
     */
    @Serializable
    data class Metering(val data: MeteringEvent = MeteringEvent()) : Event() {
        override val eventType: EventType = EventType.METERING
    }

    /**
     * 上下文使用率事件
     */
    @Serializable
    data class ContextUsage(val data: ContextUsageEvent) : Event() {
        override val eventType: EventType = EventType.CONTEXT_USAGE
    }

    /**
     * 工具调用请求事件
     */
    @Serializable
    data class ToolCallRequest(val data: ToolCallRequestEvent) : Event() {
        override val eventType: EventType = EventType.TOOL_CALL_REQUEST
    }

    /**
     * 工具调用错误事件
     */
    @Serializable
    data class ToolCallError(val data: ToolCallErrorEvent) : Event() {
        override val eventType: EventType = EventType.TOOL_CALL_ERROR
    }

    /**
     * 会话开始事件
     */
    @Serializable
    data class SessionStart(val data: SessionStartEvent) : Event() {
        override val eventType: EventType = EventType.SESSION_START
    }

    /**
     * 会话结束事件
     */
    @Serializable
    data class SessionEnd(val data: SessionEndEvent) : Event() {
        override val eventType: EventType = EventType.SESSION_END
    }

    /**
     * 未知事件
     */
    @Serializable
    data class Unknown(
        val rawEventType: String? = null,
        val rawPayload: JsonObject? = null
    ) : Event() {
        override val eventType: EventType = EventType.UNKNOWN
    }

    /**
     * 服务端错误
     *
     * 消息类型: :message-type = "error"
     */
    @Serializable
    data class Error(
        /** 错误代码（从 Headers :error-code 获取）*/
        val errorCode: String,
        /** 错误消息详情（从 Payload 获取）*/
        val errorMessage: String
    ) : Event() {
        override val eventType: EventType = EventType.UNKNOWN
    }

    /**
     * 服务端异常
     *
     * 消息类型: :message-type = "exception"
     */
    @Serializable
    data class Exception(
        /** 异常类型（从 Headers :exception-type 获取）*/
        val exceptionType: String,
        /** 异常消息详情（从 Payload 获取）*/
        val message: String
    ) : Event() {
        override val eventType: EventType = EventType.UNKNOWN
    }
}
