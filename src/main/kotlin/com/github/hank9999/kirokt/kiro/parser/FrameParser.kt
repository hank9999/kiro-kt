package com.github.hank9999.kirokt.kiro.parser

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

    private fun resetToAwaitingPrelude() {
        state = State.AWAITING_PRELUDE
        totalLength = 0
        headerLength = 0
    }

    /**
     * 尝试解析一个帧
     *
     * @return 解析出的帧，如果数据不完整则返回 null
     */
    private fun tryParseFrame(): Frame? {
        // 切换缓冲区到读模式：position → 0, limit → 原 position
        buffer.flip()

        // 若在解析 prelude 阶段发现帧边界可能错位，建议把 position 调到这里做重同步
        var resyncPosition: Int? = null

        try {
            // 状态机主循环：持续解析直到数据不足或完成
            while (true) {
                when (state) {
                    State.AWAITING_PRELUDE -> {
                        // 数据不足 12 字节，无法解析 Prelude，等待更多数据
                        if (buffer.remaining() < Frame.PRELUDE_LENGTH) return null

                        val preludeStart = buffer.position()
                        // 设置重同步点：若 Prelude 校验失败，跳过 1 字节后重试
                        resyncPosition = preludeStart + 1

                        // 读取 Prelude 的三个字段（各 4 字节，大端序）
                        totalLength = buffer.int    // 整个消息的总长度
                        headerLength = buffer.int   // Headers 部分的长度
                        val preludeCrc = buffer.int.toLong() and 0xFFFFFFFFL  // Prelude 的 CRC32

                        // 校验总长度合法性
                        if (totalLength < Frame.MIN_MESSAGE_LENGTH || totalLength > Frame.MAX_MESSAGE_LENGTH) {
                            errorCount++
                            throw ParseException("Invalid message length: $totalLength")
                        }
                        // 校验头部长度合法性：不能为负，且不能超过可用空间
                        if (headerLength < 0 || headerLength > totalLength - Frame.PRELUDE_LENGTH - Frame.MESSAGE_CRC_LENGTH) {
                            errorCount++
                            throw ParseException("Invalid header length: $headerLength")
                        }

                        // 回读 Prelude 前 8 字节用于 CRC 校验（不含 CRC 字段本身）
                        val preludeData = ByteArray(8)
                        val savedPos = buffer.position()
                        buffer.position(preludeStart)
                        buffer.get(preludeData)
                        buffer.position(savedPos)

                        // 验证 Prelude CRC
                        val computedPreludeCrc = Crc32.compute(preludeData)
                        if (computedPreludeCrc != preludeCrc) {
                            errorCount++
                            throw ParseException("Prelude CRC mismatch: expected $preludeCrc, got $computedPreludeCrc")
                        }

                        // Prelude 校验通过，清除重同步点，进入数据读取阶段
                        resyncPosition = null
                        state = State.AWAITING_DATA
                        continue
                    }

                    State.AWAITING_DATA -> {
                        // 计算除 Prelude 外剩余需要的字节数
                        val remainingLength = totalLength - Frame.PRELUDE_LENGTH
                        // 数据不足，等待更多数据
                        if (buffer.remaining() < remainingLength) return null

                        // 计算消息起始位置（当前位置回退 12 字节到 Prelude 开头）
                        val messageStart = buffer.position() - Frame.PRELUDE_LENGTH
                        // 防御性检查：避免状态错乱导致越界
                        if (messageStart < 0) {
                            errorCount++
                            throw ParseException(
                                "Parser state desync: messageStart=$messageStart, pos=${buffer.position()}, totalLength=$totalLength"
                            )
                        }

                        // DATA 阶段出错通常说明这一整帧坏了：默认丢掉已读过的这段（保持 resyncPosition = null）
                        resyncPosition = null

                        // 读取整条消息（不含尾部 CRC）用于校验
                        val messageData = ByteArray(totalLength - Frame.MESSAGE_CRC_LENGTH)
                        val savedPosition = buffer.position()
                        buffer.position(messageStart)
                        buffer.get(messageData)
                        buffer.position(savedPosition)

                        // 读取 Headers 数据
                        val headersData = ByteArray(headerLength)
                        buffer.get(headersData)

                        // 读取 Payload 数据
                        val payloadLength = totalLength - Frame.PRELUDE_LENGTH - headerLength - Frame.MESSAGE_CRC_LENGTH
                        val payload = ByteArray(payloadLength)
                        buffer.get(payload)

                        // 读取并验证消息 CRC
                        val messageCrc = buffer.int.toLong() and 0xFFFFFFFFL
                        val computedMessageCrc = Crc32.compute(messageData)
                        if (computedMessageCrc != messageCrc) {
                            errorCount++
                            throw ParseException("Message CRC mismatch: expected $messageCrc, got $computedMessageCrc")
                        }

                        // 解析 Headers，失败时使用空 Headers
                        val headers = HeaderParser.parseHeaders(headersData).getOrElse {
                            errorCount++
                            Headers.EMPTY
                        }

                        // 解析成功，重置状态机准备解析下一帧
                        resetToAwaitingPrelude()
                        return Frame(headers, payload)
                    }

                    // 终态，不应到达
                    State.COMPLETE -> return null
                }
            }
        } catch (e: ParseException) {
            // 异常恢复：重置状态机，避免残留的长度字段污染后续解析
            resetToAwaitingPrelude()

            // 若在 Prelude 阶段失败且设置了重同步点，则跳过 1 字节后重试
            resyncPosition?.let { pos ->
                val safePos = pos.coerceIn(0, buffer.limit())
                buffer.position(safePos)
            }

            throw e
        } finally {
            // 压缩缓冲区：将未读数据移到开头，切换回写模式
            buffer.compact()
        }
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
