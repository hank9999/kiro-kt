@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.github.hank9999.kirokt.model.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// ============ Tool 定义 ============

/**
 * 工具定义 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface Tool

/**
 * 自定义工具
 */
@Serializable
@SerialName("custom")
data class CustomTool(
    val name: String,
    val description: String? = null,
    @SerialName("input_schema")
    val inputSchema: InputSchema,
    @SerialName("cache_control")
    val cacheControl: CacheControl? = null
) : Tool

/**
 * Bash 工具 (2025-01-24 版本)
 */
@Serializable
@SerialName("bash_20250124")
data class BashTool(
    val name: String = "bash"
) : Tool

/**
 * 文本编辑器工具 (2025-01-24 版本)
 */
@Serializable
@SerialName("text_editor_20250124")
data class TextEditorTool20250124(
    val name: String = "str_replace_editor"
) : Tool

/**
 * 文本编辑器工具 (2025-04-29 版本)
 */
@Serializable
@SerialName("text_editor_20250429")
data class TextEditorTool20250429(
    val name: String = "str_replace_based_edit_tool"
) : Tool

/**
 * 文本编辑器工具 (2025-07-28 版本)
 */
@Serializable
@SerialName("text_editor_20250728")
data class TextEditorTool20250728(
    val name: String = "str_replace_based_edit_tool",
    @SerialName("max_characters")
    val maxCharacters: Int? = null
) : Tool

/**
 * Web 搜索工具
 */
@Serializable
@SerialName("web_search_20250305")
data class WebSearchTool(
    val name: String = "web_search",
    @SerialName("allowed_domains")
    val allowedDomains: List<String>? = null,
    @SerialName("blocked_domains")
    val blockedDomains: List<String>? = null,
    @SerialName("max_uses")
    val maxUses: Int? = null,
    @SerialName("user_location")
    val userLocation: UserLocation? = null
) : Tool

// ============ Tool Choice ============

/**
 * 工具选择 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface ToolChoice

/**
 * 自动选择工具
 */
@Serializable
@SerialName("auto")
data class ToolChoiceAuto(
    @SerialName("disable_parallel_tool_use")
    val disableParallelToolUse: Boolean? = null
) : ToolChoice

/**
 * 使用任意工具
 */
@Serializable
@SerialName("any")
data class ToolChoiceAny(
    @SerialName("disable_parallel_tool_use")
    val disableParallelToolUse: Boolean? = null
) : ToolChoice

/**
 * 强制使用特定工具
 */
@Serializable
@SerialName("tool")
data class ToolChoiceTool(
    val name: String,
    @SerialName("disable_parallel_tool_use")
    val disableParallelToolUse: Boolean? = null
) : ToolChoice

/**
 * 禁止使用工具
 */
@Serializable
@SerialName("none")
data object ToolChoiceNone : ToolChoice

// ============ Thinking 配置 ============

/**
 * 扩展思考配置 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface ThinkingConfig

/**
 * 启用扩展思考
 */
@Serializable
@SerialName("enabled")
data class ThinkingEnabled(
    @SerialName("budget_tokens")
    val budgetTokens: Int
) : ThinkingConfig

/**
 * 禁用扩展思考
 */
@Serializable
@SerialName("disabled")
data object ThinkingDisabled : ThinkingConfig
