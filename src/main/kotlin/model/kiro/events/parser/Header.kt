package model.kiro.events.parser

import java.nio.ByteBuffer
import java.util.UUID

/**
 * AWS Event Stream 头部值类型
 *
 * 支持 AWS Event Stream 协议定义的 10 种值类型
 */
enum class HeaderValueType(val code: Byte) {
    BOOL_TRUE(0),
    BOOL_FALSE(1),
    BYTE(2),
    SHORT(3),
    INT(4),
    LONG(5),
    BYTES(6),
    STRING(7),
    TIMESTAMP(8),
    UUID(9);

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: Byte): HeaderValueType? = codeMap[code]
    }
}

/**
 * AWS Event Stream 头部值
 */
sealed class HeaderValue {
    data class BoolValue(val value: Boolean) : HeaderValue()
    data class ByteValue(val value: Byte) : HeaderValue()
    data class ShortValue(val value: Short) : HeaderValue()
    data class IntValue(val value: Int) : HeaderValue()
    data class LongValue(val value: Long) : HeaderValue()
    data class BytesValue(val value: ByteArray) : HeaderValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BytesValue) return false
            return value.contentEquals(other.value)
        }
        override fun hashCode(): Int = value.contentHashCode()
    }
    data class StringValue(val value: String) : HeaderValue()
    data class TimestampValue(val value: Long) : HeaderValue()
    data class UuidValue(val value: UUID) : HeaderValue()

    /**
     * 获取字符串值（如果是字符串类型）
     */
    fun asString(): String? = when (this) {
        is StringValue -> value
        else -> null
    }

    /**
     * 获取字节数组值（如果是字节数组类型）
     */
    fun asBytes(): ByteArray? = when (this) {
        is BytesValue -> value
        else -> null
    }

    /**
     * 获取布尔值（如果是布尔类型）
     */
    fun asBoolean(): Boolean? = when (this) {
        is BoolValue -> value
        else -> null
    }

    /**
     * 获取长整型值（如果是数值类型）
     */
    fun asLong(): Long? = when (this) {
        is ByteValue -> value.toLong()
        is ShortValue -> value.toLong()
        is IntValue -> value.toLong()
        is LongValue -> value
        is TimestampValue -> value
        else -> null
    }
}

/**
 * AWS Event Stream 头部
 */
data class Header(
    val name: String,
    val value: HeaderValue
) {
    /**
     * 获取字符串值
     */
    fun stringValue(): String? = value.asString()

    /**
     * 获取字节数组值
     */
    fun bytesValue(): ByteArray? = value.asBytes()

    /**
     * 获取布尔值
     */
    fun boolValue(): Boolean? = value.asBoolean()

    /**
     * 获取长整型值
     */
    fun longValue(): Long? = value.asLong()
}

/**
 * 头部集合，提供便捷的查询方法
 */
class Headers(private val headers: List<Header>) : Iterable<Header> {
    /**
     * 根据名称获取头部
     */
    operator fun get(name: String): Header? = headers.find { it.name == name }

    /**
     * 根据名称获取字符串值
     */
    fun getString(name: String): String? = get(name)?.stringValue()

    /**
     * 根据名称获取字节数组值
     */
    fun getBytes(name: String): ByteArray? = get(name)?.bytesValue()

    /**
     * 获取消息类型 (:message-type)
     */
    fun getMessageType(): String? = getString(":message-type")

    /**
     * 获取事件类型 (:event-type)
     */
    fun getEventType(): String? = getString(":event-type")

    /**
     * 获取内容类型 (:content-type)
     */
    fun getContentType(): String? = getString(":content-type")

    /**
     * 获取错误代码 (:error-code)
     */
    fun getErrorCode(): String? = getString(":error-code")

    /**
     * 获取异常类型 (:exception-type)
     */
    fun getExceptionType(): String? = getString(":exception-type")

    /**
     * 头部数量
     */
    val size: Int get() = headers.size

    /**
     * 是否为空
     */
    fun isEmpty(): Boolean = headers.isEmpty()

    override fun iterator(): Iterator<Header> = headers.iterator()

    companion object {
        val EMPTY = Headers(emptyList())
    }
}
