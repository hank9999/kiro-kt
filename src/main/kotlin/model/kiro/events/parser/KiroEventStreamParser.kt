package model.kiro.events.parser

import model.kiro.events.Event
import java.io.InputStream

/**
 * Kiro Event Stream 解析器
 *
 * 顶层解析器，整合二进制流解析和事件转换，提供简单易用的 API。
 *
 * 架构：
 * ```
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                     KiroEventStreamParser                        │
 * │                   (顶层解析器 - 协调整体流程)                      │
 * ├─────────────────────────────────────────────────────────────────┤
 * │                                                                  │
 * │  ┌──────────────────────┐    ┌─────────────────────────────┐   │
 * │  │ EventStreamDecoder   │    │ EventParser                 │   │
 * │  │ (二进制流解析)        │───▶│ (Frame → Event 转换)        │   │
 * │  │                      │    │                             │   │
 * │  │  ┌────────────────┐  │    └─────────────────────────────┘   │
 * │  │  │ FrameParser    │  │                                       │
 * │  │  │ (帧解析)        │  │                                       │
 * │  │  │                 │  │                                       │
 * │  │  │ ┌────────────┐ │  │                                       │
 * │  │  │ │HeaderParser│ │  │                                       │
 * │  │  │ │(头部解析)   │ │  │                                       │
 * │  │  │ └────────────┘ │  │                                       │
 * │  │  └────────────────┘  │                                       │
 * │  └──────────────────────┘                                       │
 * └─────────────────────────────────────────────────────────────────┘
 * ```
 *
 * 使用示例：
 * ```kotlin
 * val parser = KiroEventStreamParser()
 *
 * // 方式 1: 解析字节数组
 * val events = parser.parse(byteArray)
 * events.forEach { event ->
 *     when (event) {
 *         is Event.AssistantResponse -> println(event.data.content)
 *         is Event.ToolUse -> println("Tool: ${event.data.name}")
 *         // ...
 *     }
 * }
 *
 * // 方式 2: 流式解析
 * parser.parseStream(inputStream) { event ->
 *     println(event)
 * }
 *
 * // 方式 3: 使用 Sequence
 * parser.parseSequence(inputStream).forEach { event ->
 *     println(event)
 * }
 * ```
 */
class KiroEventStreamParser(
    /** 最大连续错误数 */
    maxErrors: Int = DEFAULT_MAX_ERRORS
) {
    /** 内部解码器 */
    private val decoder = EventStreamDecoder(maxErrors)

    /**
     * 解析字节数组
     *
     * @param data 输入数据
     * @return 解析出的事件列表
     */
    fun parse(data: ByteArray): List<Event> {
        return decoder.decode(data).map { it.toEvent() }
    }

    /**
     * 解析字节数组的指定范围
     *
     * @param data 输入数据
     * @param offset 偏移量
     * @param length 长度
     * @return 解析出的事件列表
     */
    fun parse(data: ByteArray, offset: Int, length: Int): List<Event> {
        return decoder.decode(data, offset, length).map { it.toEvent() }
    }

    /**
     * 从 InputStream 流式解析
     *
     * @param inputStream 输入流
     * @param bufferSize 读取缓冲区大小
     * @param onEvent 每解析出一个事件时的回调
     */
    fun parseStream(
        inputStream: InputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        onEvent: (Event) -> Unit
    ) {
        decoder.decodeStream(inputStream, bufferSize) { frame ->
            onEvent(frame.toEvent())
        }
    }

    /**
     * 返回 Sequence，用于惰性解析
     *
     * @param inputStream 输入流
     * @param bufferSize 读取缓冲区大小
     * @return 事件序列
     */
    fun parseSequence(
        inputStream: InputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): Sequence<Event> {
        return decoder.decodeSequence(inputStream, bufferSize).map { it.toEvent() }
    }

    /**
     * 重置解析器
     */
    fun reset() {
        decoder.reset()
    }

    /**
     * 获取已解析的帧数
     */
    fun framesDecoded(): Int = decoder.framesDecoded()

    /**
     * 获取错误计数
     */
    fun errorCount(): Int = decoder.errorCount()

    /**
     * 获取解码器状态
     */
    fun state(): EventStreamDecoder.State = decoder.state()

    /**
     * 检查是否有未处理的数据
     */
    fun hasPendingData(): Boolean = decoder.hasPendingData()

    companion object {
        /** 默认最大连续错误数 */
        private const val DEFAULT_MAX_ERRORS = 10

        /** 默认读取缓冲区大小 */
        private const val DEFAULT_BUFFER_SIZE = 8192

        /**
         * 便捷方法：直接解析字节数组
         */
        fun parseBytes(data: ByteArray): List<Event> {
            return KiroEventStreamParser().parse(data)
        }

        /**
         * 便捷方法：直接解析 InputStream
         */
        fun parseInputStream(inputStream: InputStream): List<Event> {
            return KiroEventStreamParser().parseSequence(inputStream).toList()
        }
    }
}
