package com.github.hank9999.kirokt.model.kiro

import kotlinx.serialization.Serializable

/**
 * 上下文使用率事件 (contextUsageEvent)
 *
 * 包含当前上下文窗口的使用百分比，用于监控上下文容量。
 */
@Serializable
data class ContextUsageEvent(
    /** 上下文使用百分比，范围 0-100 */
    val contextUsagePercentage: Double = 0.0
) {
    /**
     * 格式化为百分比字符串
     */
    fun formattedPercentage(): String = "%.2f%%".format(contextUsagePercentage)

    /**
     * 判断是否超过指定阈值
     */
    fun exceedsThreshold(threshold: Double): Boolean = contextUsagePercentage > threshold

    /**
     * 判断是否接近满 (> 80%)
     */
    fun isNearFull(): Boolean = contextUsagePercentage > 80.0

    /**
     * 判断是否已满 (> 95%)
     */
    fun isFull(): Boolean = contextUsagePercentage > 95.0

    override fun toString(): String = formattedPercentage()
}

/**
 * 计费事件 (meteringEvent)
 *
 * 计费相关事件，当前实现中无具体 payload。
 */
@Serializable
class MeteringEvent {
    override fun toString(): String = "MeteringEvent"
}
