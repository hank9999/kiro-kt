package model.kiro.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 补充网页链接
 */
@Serializable
data class SupplementaryWebLink(
    val url: String,
    val title: String? = null,
    val snippet: String? = null,
    val score: Double? = null
)

/**
 * 内容范围
 */
@Serializable
data class ContentSpan(
    val start: Int,
    val end: Int
)

/**
 * 代码引用
 */
@Serializable
data class CodeReference(
    val licenseName: String? = null,
    val repository: String? = null,
    val url: String? = null,
    val information: String? = null,
    val recommendationContentSpan: ContentSpan? = null,
    val mostRelevantMissedAlternative: CodeReference? = null
)

/**
 * 后续提示
 */
@Serializable
data class FollowupPrompt(
    val content: String,
    val userIntent: UserIntent? = null
)

/**
 * 编程语言信息
 */
@Serializable
data class ProgrammingLanguage(
    val languageName: String
)

/**
 * 自定义模型配置
 */
@Serializable
data class Customization(
    val arn: String,
    val name: String? = null
)

/**
 * 代码查询信息
 */
@Serializable
data class CodeQuery(
    val codeQueryId: String,
    val programmingLanguage: ProgrammingLanguage? = null,
    val userInputMessageId: String? = null
)

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
