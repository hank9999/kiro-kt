package com.github.hank9999.kirokt.parser.kiro

/**
 * AWS Event Stream 消息帧
 *
 * 消息格式 (BigEndian):
 * ```
 * ┌──────────────┬──────────────┬──────────────┬──────────┬──────────┬───────────┐
 * │ Total Length │ Header Length│ Prelude CRC  │ Headers  │ Payload  │ Msg CRC   │
 * │   (4 bytes)  │   (4 bytes)  │   (4 bytes)  │ (变长)    │ (变长)    │ (4 bytes) │
 * └──────────────┴──────────────┴──────────────┴──────────┴──────────┴───────────┘
 * ```
 *
 * - Total Length: 整个消息的总长度（包括自身 4 字节）
 * - Header Length: 头部数据的长度
 * - Prelude CRC: 前 8 字节（Total Length + Header Length）的 CRC32 校验
 * - Headers: 头部数据
 * - Payload: 载荷数据（通常是 JSON）
 * - Message CRC: 整个消息（不含 Message CRC 自身）的 CRC32 校验
 */
data class Frame(
    /** 头部集合 */
    val headers: Headers,
    /** 载荷数据 */
    val payload: ByteArray
) {
    /**
     * 获取消息类型
     */
    val messageType: String?
        get() = headers.getMessageType()

    /**
     * 获取事件类型
     */
    val eventType: String?
        get() = headers.getEventType()

    /**
     * 获取内容类型
     */
    val contentType: String?
        get() = headers.getContentType()

    /**
     * 获取错误代码
     */
    val errorCode: String?
        get() = headers.getErrorCode()

    /**
     * 获取异常类型
     */
    val exceptionType: String?
        get() = headers.getExceptionType()

    /**
     * 载荷是否为空
     */
    val hasPayload: Boolean
        get() = payload.isNotEmpty()

    /**
     * 获取载荷字符串（UTF-8 解码）
     */
    fun payloadAsString(): String = String(payload, Charsets.UTF_8)

    /**
     * 检查是否为事件消息
     */
    fun isEvent(): Boolean = messageType == "event"

    /**
     * 检查是否为错误消息
     */
    fun isError(): Boolean = messageType == "error"

    /**
     * 检查是否为异常消息
     */
    fun isException(): Boolean = messageType == "exception"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Frame) return false
        return headers == other.headers && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = headers.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "Frame(messageType=$messageType, eventType=$eventType, payloadSize=${payload.size})"
    }

    companion object {
        /** 消息最小长度：Prelude(12) + Message CRC(4) = 16 */
        const val MIN_MESSAGE_LENGTH = 16

        /** 消息最大长度：16 MB */
        const val MAX_MESSAGE_LENGTH = 16 * 1024 * 1024

        /** Prelude 长度：Total Length(4) + Header Length(4) + Prelude CRC(4) = 12 */
        const val PRELUDE_LENGTH = 12

        /** Message CRC 长度 */
        const val MESSAGE_CRC_LENGTH = 4
    }
}
