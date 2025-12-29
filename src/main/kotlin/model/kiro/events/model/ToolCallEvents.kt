package model.kiro.events.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 工具调用请求事件 (tool_call_request)
 *
 * 标准 AWS 格式的工具调用请求。
 */
@Serializable
data class ToolCallRequestEvent(
    /** 工具调用 ID */
    val toolCallId: String,

    /** 工具名称 */
    val toolName: String,

    /** 工具输入参数 */
    val input: JsonObject? = null
)

/**
 * 工具调用错误事件 (tool_call_error)
 *
 * 工具执行失败时的错误事件。
 */
@Serializable
data class ToolCallErrorEvent(
    /** 工具调用 ID */
    @SerialName("tool_call_id")
    val toolCallId: String,

    /** 错误信息描述 */
    val error: String
)
