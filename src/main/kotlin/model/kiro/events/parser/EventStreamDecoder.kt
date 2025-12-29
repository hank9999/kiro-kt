package model.kiro.events.parser

import java.io.InputStream

/**
 * AWS Event Stream 流式解码器
 *
 * 顶层解码器，提供便捷的流式解析接口：
 * - 支持 InputStream 读取
 * - 支持迭代器模式
 * - 支持协程 Flow（通过扩展）
 * - 容错恢复机制
 *
 * 使用示例:
 * ```kotlin
 * val decoder = EventStreamDecoder()
 *
 * // 方式 1: 直接 feed 数据
 * val frames = decoder.decode(byteArray)
 *
 * // 方式 2: 从 InputStream 读取
 * decoder.decodeStream(inputStream) { frame ->
 *     println(frame)
 * }
 *
 * // 方式 3: 迭代器模式
 * for (frame in decoder.decodeIterator(byteArray)) {
 *     println(frame)
 * }
 * ```
 */
class EventStreamDecoder(
    /** 最大连续错误数，超过后停止解析 */
    private val maxErrors: Int = DEFAULT_MAX_ERRORS
) {
    /** 内部帧解析器 */
    private val frameParser = FrameParser()

    /** 解码器状态 */
    private var state = State.READY

    /**
     * 解码器状态
     */
    enum class State {
        /** 就绪，可以接收数据 */
        READY,
        /** 正在解析 */
        PARSING,
        /** 恢复中（跳过损坏数据）*/
        RECOVERING,
        /** 已停止（错误过多）*/
        STOPPED
    }

    /**
     * 解码字节数组
     *
     * @param data 输入数据
     * @return 解析出的帧列表
     */
    fun decode(data: ByteArray): List<Frame> {
        if (state == State.STOPPED) {
            return emptyList()
        }

        state = State.PARSING
        return try {
            val frames = frameParser.feed(data)
            checkErrorThreshold()
            frames
        } catch (e: ParseException) {
            state = State.RECOVERING
            recover()
            emptyList()
        }
    }

    /**
     * 解码字节数组的指定范围
     *
     * @param data 输入数据
     * @param offset 偏移量
     * @param length 长度
     * @return 解析出的帧列表
     */
    fun decode(data: ByteArray, offset: Int, length: Int): List<Frame> {
        if (state == State.STOPPED) {
            return emptyList()
        }

        state = State.PARSING
        return try {
            val frames = frameParser.feed(data, offset, length)
            checkErrorThreshold()
            frames
        } catch (e: ParseException) {
            state = State.RECOVERING
            recover()
            emptyList()
        }
    }

    /**
     * 从 InputStream 解码
     *
     * @param inputStream 输入流
     * @param bufferSize 读取缓冲区大小
     * @param onFrame 每解析出一个帧时的回调
     */
    fun decodeStream(
        inputStream: InputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onFrame: (Frame) -> Unit
    ) {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (state == State.STOPPED) {
                break
            }

            val frames = decode(buffer, 0, bytesRead)
            frames.forEach(onFrame)
        }
    }

    /**
     * 返回迭代器，用于遍历解码后的帧
     *
     * @param data 输入数据
     * @return 帧迭代器
     */
    fun decodeIterator(data: ByteArray): Iterator<Frame> {
        return decode(data).iterator()
    }

    /**
     * 返回 Sequence，用于惰性解码
     *
     * @param inputStream 输入流
     * @param bufferSize 读取缓冲区大小
     * @return 帧序列
     */
    fun decodeSequence(
        inputStream: InputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): Sequence<Frame> = sequence {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (state == State.STOPPED) {
                break
            }

            val frames = decode(buffer, 0, bytesRead)
            yieldAll(frames)
        }
    }

    /**
     * 检查错误阈值
     */
    private fun checkErrorThreshold() {
        if (frameParser.errorCount >= maxErrors) {
            state = State.STOPPED
        }
    }

    /**
     * 恢复解析（跳过损坏的数据）
     */
    private fun recover() {
        // 简单的恢复策略：重置解析器
        frameParser.reset()
        state = State.READY
    }

    /**
     * 重置解码器
     */
    fun reset() {
        frameParser.reset()
        state = State.READY
    }

    /**
     * 获取已解码的帧数
     */
    fun framesDecoded(): Int = frameParser.framesDecoded

    /**
     * 获取错误计数
     */
    fun errorCount(): Int = frameParser.errorCount

    /**
     * 获取当前状态
     */
    fun state(): State = state

    /**
     * 检查是否有未处理的数据
     */
    fun hasPendingData(): Boolean = frameParser.hasPendingData()

    companion object {
        /** 默认最大连续错误数 */
        private const val DEFAULT_MAX_ERRORS = 10

        /** 默认读取缓冲区大小 */
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
