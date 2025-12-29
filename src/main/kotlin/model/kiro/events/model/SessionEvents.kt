package model.kiro.events.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 会话开始事件 (session_start)
 *
 * 标记会话开始。
 */
@Serializable
data class SessionStartEvent(
    /** 会话唯一标识符 */
    @SerialName("session_id")
    val sessionId: String,

    /** 会话 ID（兼容格式）*/
    @SerialName("sessionId")
    val sessionIdAlt: String? = null,

    /** 时间戳 */
    val timestamp: String? = null
) {
    /**
     * 获取有效的会话 ID
     */
    val effectiveSessionId: String
        get() = sessionIdAlt ?: sessionId
}

/**
 * 会话结束事件 (session_end)
 *
 * 标记会话结束。
 */
@Serializable
data class SessionEndEvent(
    /** 会话唯一标识符 */
    @SerialName("session_id")
    val sessionId: String,

    /** 时间戳 */
    val timestamp: String? = null,

    /** 会话持续时间（毫秒）*/
    val duration: Long? = null
)
