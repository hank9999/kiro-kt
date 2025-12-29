package model.kiro.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 代码补全事件 (completion)
 *
 * 完整的代码补全响应（非流式）。
 */
@Serializable
data class CompletionEvent(
    /** 补全的代码内容 */
    val content: String,

    /** 完成原因 */
    @SerialName("finish_reason")
    val finishReason: String? = null,

    /** 工具调用列表 */
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null
) {
    /**
     * 检查是否有工具调用
     */
    val hasToolCalls: Boolean
        get() = !toolCalls.isNullOrEmpty()

    override fun toString(): String = content
}

/**
 * 流式补全片段事件 (completion_chunk)
 *
 * 代码补全的流式传输片段。
 */
@Serializable
data class CompletionChunkEvent(
    /** 累积的内容 */
    val content: String? = null,

    /** 本次增量内容 */
    val delta: String? = null,

    /** 完成原因（最后一个片段才有）*/
    @SerialName("finish_reason")
    val finishReason: String? = null
) {
    /**
     * 检查是否为最后一个片段
     */
    val isLast: Boolean
        get() = !finishReason.isNullOrEmpty()

    override fun toString(): String = delta ?: content ?: ""
}
