package com.github.hank9999.kirokt.kiro.parser

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * AWS Event Stream 头部解析器
 *
 * 使用状态机解析头部，支持流式解析。
 *
 * 头部格式：
 * [1 byte: name_length]
 * [name_length bytes: name]
 * [1 byte: value_type]
 * [2 bytes: value_length] (仅 STRING/BYTES 类型)
 * [value_length bytes: value]
 */
class HeaderParser {

    /**
     * 解析阶段
     */
    private enum class Phase {
        READ_NAME_LENGTH,   // 读取名称长度 (1 byte)
        READ_NAME,          // 读取名称
        READ_VALUE_TYPE,    // 读取值类型 (1 byte)
        READ_VALUE_LENGTH,  // 读取值长度 (2 bytes, BigEndian)
        READ_VALUE          // 读取值数据
    }

    private var phase = Phase.READ_NAME_LENGTH
    private var nameLength = 0
    private var name = ""
    private var valueType: HeaderValueType? = null
    private var valueLength = 0

    private val headers = mutableListOf<Header>()

    /**
     * 从字节数组解析所有头部
     *
     * @param data 头部数据
     * @return 解析结果
     */
    fun parse(data: ByteArray): Result<Headers> {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            parseBuffer(buffer)
            Result.success(Headers(headers.toList()))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            reset()
        }
    }

    /**
     * 从 ByteBuffer 解析所有头部
     */
    private fun parseBuffer(buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            when (phase) {
                Phase.READ_NAME_LENGTH -> {
                    nameLength = buffer.get().toInt() and 0xFF
                    phase = Phase.READ_NAME
                }

                Phase.READ_NAME -> {
                    if (buffer.remaining() < nameLength) {
                        throw ParseException("Insufficient data for header name")
                    }
                    val nameBytes = ByteArray(nameLength)
                    buffer.get(nameBytes)
                    name = String(nameBytes, Charsets.UTF_8)
                    phase = Phase.READ_VALUE_TYPE
                }

                Phase.READ_VALUE_TYPE -> {
                    val typeCode = buffer.get()
                    valueType = HeaderValueType.fromCode(typeCode)
                        ?: throw ParseException("Unknown header value type: $typeCode")

                    // 根据类型确定是否需要读取值长度
                    when (valueType) {
                        HeaderValueType.STRING, HeaderValueType.BYTES -> {
                            phase = Phase.READ_VALUE_LENGTH
                        }
                        else -> {
                            valueLength = getFixedValueLength(valueType!!)
                            phase = Phase.READ_VALUE
                        }
                    }
                }

                Phase.READ_VALUE_LENGTH -> {
                    if (buffer.remaining() < 2) {
                        throw ParseException("Insufficient data for value length")
                    }
                    valueLength = buffer.short.toInt() and 0xFFFF
                    phase = Phase.READ_VALUE
                }

                Phase.READ_VALUE -> {
                    if (buffer.remaining() < valueLength) {
                        throw ParseException("Insufficient data for header value")
                    }
                    val value = parseValue(buffer, valueType!!, valueLength)
                    headers.add(Header(name, value))

                    // 重置状态，准备解析下一个头部
                    phase = Phase.READ_NAME_LENGTH
                    nameLength = 0
                    name = ""
                    valueType = null
                    valueLength = 0
                }
            }
        }
    }

    /**
     * 获取固定长度值类型的长度
     */
    private fun getFixedValueLength(type: HeaderValueType): Int {
        return when (type) {
            HeaderValueType.BOOL_TRUE, HeaderValueType.BOOL_FALSE -> 0
            HeaderValueType.BYTE -> 1
            HeaderValueType.SHORT -> 2
            HeaderValueType.INT -> 4
            HeaderValueType.LONG, HeaderValueType.TIMESTAMP -> 8
            HeaderValueType.UUID -> 16
            else -> throw ParseException("Not a fixed-length type: $type")
        }
    }

    /**
     * 解析值
     */
    private fun parseValue(buffer: ByteBuffer, type: HeaderValueType, length: Int): HeaderValue {
        return when (type) {
            HeaderValueType.BOOL_TRUE -> HeaderValue.BoolValue(true)
            HeaderValueType.BOOL_FALSE -> HeaderValue.BoolValue(false)
            HeaderValueType.BYTE -> HeaderValue.ByteValue(buffer.get())
            HeaderValueType.SHORT -> HeaderValue.ShortValue(buffer.short)
            HeaderValueType.INT -> HeaderValue.IntValue(buffer.int)
            HeaderValueType.LONG -> HeaderValue.LongValue(buffer.long)
            HeaderValueType.TIMESTAMP -> HeaderValue.TimestampValue(buffer.long)
            HeaderValueType.UUID -> {
                val high = buffer.long
                val low = buffer.long
                HeaderValue.UuidValue(UUID(high, low))
            }
            HeaderValueType.STRING -> {
                val bytes = ByteArray(length)
                buffer.get(bytes)
                HeaderValue.StringValue(String(bytes, Charsets.UTF_8))
            }
            HeaderValueType.BYTES -> {
                val bytes = ByteArray(length)
                buffer.get(bytes)
                HeaderValue.BytesValue(bytes)
            }
        }
    }

    /**
     * 重置解析器状态
     */
    private fun reset() {
        phase = Phase.READ_NAME_LENGTH
        nameLength = 0
        name = ""
        valueType = null
        valueLength = 0
        headers.clear()
    }

    companion object {
        /**
         * 便捷方法：直接解析头部数据
         */
        fun parseHeaders(data: ByteArray): Result<Headers> {
            return HeaderParser().parse(data)
        }
    }
}

/**
 * 解析异常
 */
class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
