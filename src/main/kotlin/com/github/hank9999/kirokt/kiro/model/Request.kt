package com.github.hank9999.kirokt.kiro.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ============ Kiro 请求数据结构 ============

/**
 * Kiro API 请求
 */
@Serializable
data class KiroRequest(
    @SerialName("conversationState")
    val conversationState: ConversationState
)

/**
 * 对话状态
 */
@Serializable
data class ConversationState(
    /** 对话唯一标识符 */
    @SerialName("conversationId")
    val conversationId: String,

    /** 代理延续标识符 */
    @SerialName("agentContinuationId")
    val agentContinuationId: String,

    /** 代理任务类型 (固定为 "vibe") */
    @SerialName("agentTaskType")
    val agentTaskType: String = "vibe",

    /** 聊天触发类型 (固定为 "MANUAL") */
    @SerialName("chatTriggerType")
    val chatTriggerType: String = "MANUAL",

    /** 当前消息 */
    @SerialName("currentMessage")
    val currentMessage: CurrentMessage,

    /** 对话历史 */
    @SerialName("history")
    val history: List<HistoryEntry> = emptyList()
)

/**
 * 当前消息
 */
@Serializable
data class CurrentMessage(
    @SerialName("userInputMessage")
    val userInputMessage: UserInputMessage
)

/**
 * 用户输入消息
 */
@Serializable
data class UserInputMessage(
    /** 消息内容 */
    @SerialName("content")
    val content: String,

    /** 模型 ID */
    @SerialName("modelId")
    val modelId: String,

    /** 来源 (固定为 "AI_EDITOR") */
    @SerialName("origin")
    val origin: String = "AI_EDITOR",

    /** 图片列表 */
    @SerialName("images")
    val images: List<KiroImage>? = null,

    /** 用户输入消息上下文 */
    @SerialName("userInputMessageContext")
    val userInputMessageContext: UserInputMessageContext? = null
)

/**
 * 用户输入消息上下文
 */
@Serializable
data class UserInputMessageContext(
    /** 工具定义列表 */
    @SerialName("tools")
    val tools: List<KiroToolWrapper>? = null,

    /** 工具执行结果列表 */
    @SerialName("toolResults")
    val toolResults: List<KiroToolResult>? = null
)

// ============ 图片相关 ============

/**
 * Kiro 图片
 */
@Serializable
data class KiroImage(
    /** 图片格式 ("jpeg", "png", "gif", "webp") */
    @SerialName("format")
    val format: String,

    /** 图片数据源 */
    @SerialName("source")
    val source: KiroImageSource
) {
    companion object {
        /**
         * 从 base64 数据创建图片
         */
        fun fromBase64(format: String, data: String): KiroImage {
            return KiroImage(
                format = format,
                source = KiroImageSource(bytes = data)
            )
        }
    }
}

/**
 * Kiro 图片数据源
 */
@Serializable
data class KiroImageSource(
    /** base64 编码的图片数据 */
    @SerialName("bytes")
    val bytes: String
)

// ============ 工具相关 ============

/**
 * Kiro 工具包装器
 */
@Serializable
data class KiroToolWrapper(
    @SerialName("toolSpecification")
    val toolSpecification: KiroToolSpecification
)

/**
 * Kiro 工具规格
 */
@Serializable
data class KiroToolSpecification(
    /** 工具名称 */
    @SerialName("name")
    val name: String,

    /** 工具描述 */
    @SerialName("description")
    val description: String = "",

    /** 输入参数 Schema */
    @SerialName("inputSchema")
    val inputSchema: KiroInputSchema
)

/**
 * Kiro 输入参数 Schema
 */
@Serializable
data class KiroInputSchema(
    /** JSON Schema */
    @SerialName("json")
    val json: JsonElement
)

/**
 * Kiro 工具执行结果
 */
@Serializable
data class KiroToolResult(
    /** 工具使用 ID */
    @SerialName("toolUseId")
    val toolUseId: String,

    /** 结果内容 */
    @SerialName("content")
    val content: List<KiroToolResultContent>,

    /** 执行状态 ("success" 或 "error") */
    @SerialName("status")
    val status: String? = null,

    /** 是否为错误 */
    @SerialName("isError")
    val isError: Boolean = false
) {
    companion object {
        /**
         * 创建成功的工具结果
         */
        fun success(toolUseId: String, text: String): KiroToolResult {
            return KiroToolResult(
                toolUseId = toolUseId,
                content = listOf(KiroToolResultContent(text = text)),
                status = "success",
                isError = false
            )
        }

        /**
         * 创建错误的工具结果
         */
        fun error(toolUseId: String, errorMessage: String): KiroToolResult {
            return KiroToolResult(
                toolUseId = toolUseId,
                content = listOf(KiroToolResultContent(text = errorMessage)),
                status = "error",
                isError = true
            )
        }
    }
}

/**
 * Kiro 工具结果内容
 */
@Serializable
data class KiroToolResultContent(
    @SerialName("text")
    val text: String
)

/**
 * Kiro 工具使用条目 (用于历史消息中记录工具调用)
 */
@Serializable
data class KiroToolUseEntry(
    /** 工具使用 ID */
    @SerialName("toolUseId")
    val toolUseId: String,

    /** 工具名称 */
    @SerialName("name")
    val name: String,

    /** 工具输入参数 */
    @SerialName("input")
    val input: JsonElement
)

// ============ 历史消息相关 ============

/**
 * 历史条目 - 用户消息或助手消息
 */
@Serializable
data class HistoryEntry(
    /** 用户输入消息 */
    @SerialName("userInputMessage")
    val userInputMessage: HistoryUserInputMessage? = null,

    /** 助手响应消息 */
    @SerialName("assistantResponseMessage")
    val assistantResponseMessage: HistoryAssistantResponseMessage? = null
)

/**
 * 历史用户输入消息
 */
@Serializable
data class HistoryUserInputMessage(
    /** 消息内容 */
    @SerialName("content")
    val content: String,

    /** 模型 ID */
    @SerialName("modelId")
    val modelId: String,

    /** 来源 */
    @SerialName("origin")
    val origin: String = "AI_EDITOR",

    /** 图片列表 */
    @SerialName("images")
    val images: List<KiroImage>? = null,

    /** 用户输入消息上下文 (包含工具结果) */
    @SerialName("userInputMessageContext")
    val userInputMessageContext: UserInputMessageContext? = null
)

/**
 * 历史助手响应消息
 */
@Serializable
data class HistoryAssistantResponseMessage(
    /** 响应内容 */
    @SerialName("content")
    val content: String,

    /** 工具使用列表 */
    @SerialName("toolUses")
    val toolUses: List<KiroToolUseEntry>? = null
)
