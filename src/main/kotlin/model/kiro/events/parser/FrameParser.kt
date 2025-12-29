package model.kiro.events.parser

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AWS Event Stream 帧解析器
 *
 * 使用有限状态机实现流式解析，支持：
 * - 半包处理（数据不完整时保存状态等待更多数据）
 * - 粘包处理（一次性处理多个消息）
 * - CRC32 校验
 *
 * 状态机流程:
 * ```
 * AWAITING_PRELUDE → AWAITING_DATA → COMPLETE
 *        ↑                              │
 *        └──────────────────────────────┘
 * ```
 */
class FrameParser {

    /**
     * 解析状态
     */
    private enum class State {
        /** 等待 Prelude (12 bytes) */
        AWAITING_PRELUDE,
        /** 等待完整消息数据 */
        AWAITING_DATA,
        /** 解析完成 */
        COMPLETE
    }

    /** 内部缓冲区 */
    private var buffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN)

    /** 当前状态 */
    private var state = State.AWAITING_PRELUDE

    /** 当前消息的总长度 */
    private var totalLength = 0

    /** 当前消息的头部长度 */
    private var headerLength = 0

    /** 已解析的帧数 */
    var framesDecoded = 0
        private set

    /** 错误计数 */
    var errorCount = 0
        private set

    /**
     * 向解析器输入数据
     *
     * @param data 输入数据
     * @return 解析出的帧列表
     */
    fun feed(data: ByteArray): List<Frame> {
        ensureCapacity(data.size)
        buffer.put(data)
        return parseFrames()
    }

    /**
     * 向解析器输入数据
     *
     * @param data 输入数据
     * @param offset 偏移量
     * @param length 长度
     * @return 解析出的帧列表
     */
    fun feed(data: ByteArray, offset: Int, length: Int): List<Frame> {
        ensureCapacity(length)
        buffer.put(data, offset, length)
        return parseFrames()
    }

    /**
     * 解析所有可用的帧
     */
    private fun parseFrames(): List<Frame> {
        val frames = mutableListOf<Frame>()

        while (true) {
            val frame = tryParseFrame() ?: break
            frames.add(frame)
            framesDecoded++
        }

        return frames
    }

    /**
     * 尝试解析一个帧
     *
     * @return 解析出的帧，如果数据不完整则返回 null
     */
    private fun tryParseFrame(): Frame? {
        // 切换到读取模式
        buffer.flip()

        try {
            when (state) {
                State.AWAITING_PRELUDE -> {
                    if (buffer.remaining() < Frame.PRELUDE_LENGTH) {
                        return null
                    }

                    // 读取 Prelude
                    val preludeStart = buffer.position()
                    totalLength = buffer.int
                    headerLength = buffer.int
                    val preludeCrc = buffer.int.toLong() and 0xFFFFFFFFL

                    // 验证长度
                    if (totalLength < Frame.MIN_MESSAGE_LENGTH || totalLength > Frame.MAX_MESSAGE_LENGTH) {
                        errorCount++
                        throw ParseException("Invalid message length: $totalLength")
                    }

                    if (headerLength < 0 || headerLength > totalLength - Frame.PRELUDE_LENGTH - Frame.MESSAGE_CRC_LENGTH) {
                        errorCount++
                        throw ParseException("Invalid header length: $headerLength")
                    }

                    // 验证 Prelude CRC
                    val preludeData = ByteArray(8)
                    buffer.position(preludeStart)
                    buffer.get(preludeData)
                    buffer.position(preludeStart + Frame.PRELUDE_LENGTH) // 跳过 CRC

                    val computedPreludeCrc = Crc32.compute(preludeData)
                    if (computedPreludeCrc != preludeCrc) {
                        errorCount++
                        throw ParseException("Prelude CRC mismatch: expected $preludeCrc, got $computedPreludeCrc")
                    }

                    state = State.AWAITING_DATA
                }

                State.AWAITING_DATA -> {
                    // 计算剩余需要的数据量
                    val remainingLength = totalLength - Frame.PRELUDE_LENGTH
                    if (buffer.remaining() < remainingLength) {
                        return null
                    }

                    // 提取消息数据（用于 CRC 校验）
                    val messageStart = buffer.position() - Frame.PRELUDE_LENGTH
                    val messageData = ByteArray(totalLength - Frame.MESSAGE_CRC_LENGTH)

                    // 临时回退以获取完整消息（包括 prelude）
                    val savedPosition = buffer.position()
                    buffer.position(messageStart)
                    buffer.get(messageData)
                    buffer.position(savedPosition)

                    // 读取头部
                    val headersData = ByteArray(headerLength)
                    buffer.get(headersData)

                    // 读取载荷
                    val payloadLength = totalLength - Frame.PRELUDE_LENGTH - headerLength - Frame.MESSAGE_CRC_LENGTH
                    val payload = ByteArray(payloadLength)
                    buffer.get(payload)

                    // 读取并验证 Message CRC
                    val messageCrc = buffer.int.toLong() and 0xFFFFFFFFL
                    val computedMessageCrc = Crc32.compute(messageData)
                    if (computedMessageCrc != messageCrc) {
                        errorCount++
                        throw ParseException("Message CRC mismatch: expected $messageCrc, got $computedMessageCrc")
                    }

                    // 解析头部
                    val headers = HeaderParser.parseHeaders(headersData).getOrElse {
                        errorCount++
                        // 头部解析失败时使用空头部，避免丢失 payload
                        Headers.EMPTY
                    }

                    // 重置状态
                    state = State.AWAITING_PRELUDE
                    totalLength = 0
                    headerLength = 0

                    return Frame(headers, payload)
                }

                State.COMPLETE -> {
                    // 不应该到达这里
                    return null
                }
            }
        } finally {
            // 切换回写入模式，保留未处理的数据
            buffer.compact()
        }

        return null
    }

    /**
     * 确保缓冲区有足够的容量
     */
    private fun ensureCapacity(additionalBytes: Int) {
        val required = buffer.position() + additionalBytes
        if (required > buffer.capacity()) {
            val newCapacity = maxOf(buffer.capacity() * 2, required)
            val newBuffer = ByteBuffer.allocate(newCapacity).order(ByteOrder.BIG_ENDIAN)
            buffer.flip()
            newBuffer.put(buffer)
            buffer = newBuffer
        }
    }

    /**
     * 重置解析器状态
     */
    fun reset() {
        buffer.clear()
        state = State.AWAITING_PRELUDE
        totalLength = 0
        headerLength = 0
        framesDecoded = 0
        errorCount = 0
    }

    /**
     * 获取缓冲区中未处理的数据量
     */
    fun pendingBytes(): Int = buffer.position()

    /**
     * 检查是否有未处理的数据
     */
    fun hasPendingData(): Boolean = buffer.position() > 0

    companion object {
        /** 初始缓冲区大小 */
        private const val INITIAL_BUFFER_SIZE = 8192
    }
}
