package com.github.hank9999.kirokt.parser.kiro

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.github.hank9999.kirokt.model.kiro.AssistantResponseEvent
import com.github.hank9999.kirokt.model.kiro.ContextUsageEvent
import com.github.hank9999.kirokt.model.kiro.Event
import com.github.hank9999.kirokt.model.kiro.EventType
import com.github.hank9999.kirokt.model.kiro.SessionEndEvent
import com.github.hank9999.kirokt.model.kiro.SessionStartEvent
import com.github.hank9999.kirokt.model.kiro.ToolCallErrorEvent
import com.github.hank9999.kirokt.model.kiro.ToolCallRequestEvent
import com.github.hank9999.kirokt.model.kiro.ToolUseEvent

/**
 * 事件解析器
 *
 * 将 AWS Event Stream Frame 转换为 Kiro Event。
 *
 * 解析流程：
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
object EventParser {

    /** JSON 解析器，配置为宽松模式 */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 将 Frame 转换为 Event
     *
     * @param frame 消息帧
     * @return 解析出的事件
     */
    fun parse(frame: Frame): Event {
        return when (frame.messageType) {
            "event" -> parseEvent(frame)
            "error" -> parseError(frame)
            "exception" -> parseException(frame)
            else -> Event.Unknown(
                rawEventType = frame.messageType,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析事件消息
     */
    private fun parseEvent(frame: Frame): Event {
        val eventType = frame.eventType ?: return Event.Unknown(
            rawEventType = null,
            rawPayload = tryParseJsonObject(frame.payload)
        )

        return when (eventType) {
            EventType.ASSISTANT_RESPONSE.value -> parseAssistantResponse(frame)
            EventType.TOOL_USE.value -> parseToolUse(frame)
            EventType.METERING.value -> Event.Metering()
            EventType.CONTEXT_USAGE.value -> parseContextUsage(frame)
            EventType.TOOL_CALL_REQUEST.value -> parseToolCallRequest(frame)
            EventType.TOOL_CALL_ERROR.value -> parseToolCallError(frame)
            EventType.SESSION_START.value -> parseSessionStart(frame)
            EventType.SESSION_END.value -> parseSessionEnd(frame)
            else -> Event.Unknown(
                rawEventType = eventType,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析助手响应事件
     */
    private fun parseAssistantResponse(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<AssistantResponseEvent>(frame.payloadAsString())
            Event.AssistantResponse(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.ASSISTANT_RESPONSE.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析工具使用事件
     */
    private fun parseToolUse(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<ToolUseEvent>(frame.payloadAsString())
            Event.ToolUse(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.TOOL_USE.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析上下文使用率事件
     */
    private fun parseContextUsage(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<ContextUsageEvent>(frame.payloadAsString())
            Event.ContextUsage(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.CONTEXT_USAGE.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析工具调用请求事件
     */
    private fun parseToolCallRequest(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<ToolCallRequestEvent>(frame.payloadAsString())
            Event.ToolCallRequest(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.TOOL_CALL_REQUEST.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析工具调用错误事件
     */
    private fun parseToolCallError(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<ToolCallErrorEvent>(frame.payloadAsString())
            Event.ToolCallError(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.TOOL_CALL_ERROR.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析会话开始事件
     */
    private fun parseSessionStart(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<SessionStartEvent>(frame.payloadAsString())
            Event.SessionStart(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.SESSION_START.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析会话结束事件
     */
    private fun parseSessionEnd(frame: Frame): Event {
        return try {
            val data = json.decodeFromString<SessionEndEvent>(frame.payloadAsString())
            Event.SessionEnd(data)
        } catch (e: Exception) {
            Event.Unknown(
                rawEventType = EventType.SESSION_END.value,
                rawPayload = tryParseJsonObject(frame.payload)
            )
        }
    }

    /**
     * 解析错误消息
     */
    private fun parseError(frame: Frame): Event {
        val errorCode = frame.errorCode ?: "UnknownError"
        val errorMessage = if (frame.hasPayload) {
            frame.payloadAsString()
        } else {
            "Unknown error"
        }
        return Event.Error(errorCode, errorMessage)
    }

    /**
     * 解析异常消息
     */
    private fun parseException(frame: Frame): Event {
        val exceptionType = frame.exceptionType ?: "UnknownException"
        val message = if (frame.hasPayload) {
            frame.payloadAsString()
        } else {
            "Unknown exception"
        }
        return Event.Exception(exceptionType, message)
    }

    /**
     * 尝试将 payload 解析为 JsonObject
     */
    private fun tryParseJsonObject(payload: ByteArray): JsonObject? {
        if (payload.isEmpty()) return null
        return try {
            json.parseToJsonElement(String(payload, Charsets.UTF_8)).jsonObject
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Frame 扩展函数：转换为 Event
 */
fun Frame.toEvent(): Event = EventParser.parse(this)
