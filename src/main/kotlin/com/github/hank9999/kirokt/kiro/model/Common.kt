package com.github.hank9999.kirokt.kiro.model

import kotlinx.serialization.Serializable

/**
 * 工具调用函数信息
 */
@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

/**
 * 工具调用
 */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)
