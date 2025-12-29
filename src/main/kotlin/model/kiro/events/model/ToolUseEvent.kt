package model.kiro.events.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 工具使用事件 (toolUseEvent)
 *
 * 用于 AI 调用外部工具，支持流式传输工具调用参数。
 *
 * 流式传输机制:
 * 1. 首次事件：包含 name、toolUseId，input 可能为空或部分数据，stop=false
 * 2. 增量事件：input 包含 JSON 字符串片段，stop=false
 * 3. 结束事件：stop=true，表示工具调用参数传输完成
 */
@Serializable
data class ToolUseEvent(
    /** 工具名称 */
    val name: String,

    /** 工具调用唯一标识符 */
    val toolUseId: String,

    /** 工具输入参数（对象或 JSON 字符串片段）*/
    val input: JsonElement? = null,

    /** 是否为结束标志（true 表示工具调用完成）*/
    val stop: Boolean = false
) {
    /**
     * 检查工具调用是否已完成
     */
    val isCompleted: Boolean
        get() = stop

    /**
     * 检查工具调用是否在进行中
     */
    val isInProgress: Boolean
        get() = !stop

    override fun toString(): String {
        val status = if (stop) "complete" else "partial"
        return "ToolUse[$name] (id=$toolUseId, $status): $input"
    }
}
