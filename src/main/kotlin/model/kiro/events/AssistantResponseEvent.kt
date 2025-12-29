package model.kiro.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 助手响应事件 (assistantResponseEvent)
 *
 * 最核心的事件类型，用于传输 AI 助手的响应内容。
 * 支持流式传输，每次事件包含部分响应文本。
 */
@Serializable
data class AssistantResponseEvent(
    /** 会话唯一标识符 (流式传输时可省略) */
    val conversationId: String? = null,

    /** 消息唯一标识符 (流式传输时可省略) */
    val messageId: String? = null,

    /** 响应文本内容 (支持 Markdown) */
    val content: String = "",

    /** 内容 MIME 类型 */
    val contentType: ContentType? = null,

    /** 消息状态 */
    val messageStatus: MessageStatus? = null,

    /** 补充网页链接列表 */
    val supplementaryWebLinks: List<SupplementaryWebLink>? = null,

    /** 引用来源列表 */
    val references: List<CodeReference>? = null,

    /** 代码引用列表 */
    val codeReference: List<CodeReference>? = null,

    /** 后续提示建议 */
    val followupPrompt: FollowupPrompt? = null,

    /** 编程语言信息 */
    val programmingLanguage: ProgrammingLanguage? = null,

    /** 自定义模型配置 */
    val customizations: List<Customization>? = null,

    /** 用户意图分类 */
    val userIntent: UserIntent? = null,

    /** 代码查询信息 */
    val codeQuery: CodeQuery? = null
) {
    /**
     * 检查消息是否已完成
     */
    val isCompleted: Boolean
        get() = messageStatus == MessageStatus.COMPLETED

    /**
     * 检查消息是否在进行中
     */
    val isInProgress: Boolean
        get() = messageStatus == MessageStatus.IN_PROGRESS

    /**
     * 检查是否有错误
     */
    val hasError: Boolean
        get() = messageStatus == MessageStatus.ERROR

    override fun toString(): String = content
}
