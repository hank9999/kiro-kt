@file:OptIn(ExperimentalSerializationApi::class)

package com.github.hank9999.kirokt.anthropic.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

// ============ Source 类型 ============

/**
 * Base64 图像源
 */
@Serializable
@SerialName("base64")
data class Base64ImageSource(
    @SerialName("media_type")
    val mediaType: String,
    val data: String
)

/**
 * URL 图像源
 */
@Serializable
@SerialName("url")
data class UrlImageSource(
    val url: String
)

/**
 * 图像源 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface ImageSource

@Serializable
@SerialName("base64")
data class ImageSourceBase64(
    @SerialName("media_type")
    val mediaType: String,
    val data: String
) : ImageSource

@Serializable
@SerialName("url")
data class ImageSourceUrl(
    val url: String
) : ImageSource

/**
 * 文档源 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface DocumentSource

@Serializable
@SerialName("base64")
data class DocumentSourceBase64(
    @SerialName("media_type")
    val mediaType: String,
    val data: String
) : DocumentSource

@Serializable
@SerialName("url")
data class DocumentSourceUrl(
    val url: String
) : DocumentSource

@Serializable
@SerialName("text")
data class DocumentSourceText(
    @SerialName("media_type")
    val mediaType: String,
    val data: String
) : DocumentSource

// ============ Citation 类型 ============

/**
 * 引用配置
 */
@Serializable
data class CitationsConfig(
    val enabled: Boolean = true
)

/**
 * 引用类型 - 多态
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface Citation

@Serializable
@SerialName("char_location")
data class CitationCharLocation(
    @SerialName("cited_text")
    val citedText: String,
    @SerialName("document_index")
    val documentIndex: Int,
    @SerialName("document_title")
    val documentTitle: String? = null,
    @SerialName("start_char_index")
    val startCharIndex: Int,
    @SerialName("end_char_index")
    val endCharIndex: Int
) : Citation

@Serializable
@SerialName("page_location")
data class CitationPageLocation(
    @SerialName("cited_text")
    val citedText: String,
    @SerialName("document_index")
    val documentIndex: Int,
    @SerialName("document_title")
    val documentTitle: String? = null,
    @SerialName("start_page_number")
    val startPageNumber: Int,
    @SerialName("end_page_number")
    val endPageNumber: Int
) : Citation

@Serializable
@SerialName("content_block_location")
data class CitationContentBlockLocation(
    @SerialName("cited_text")
    val citedText: String,
    @SerialName("document_index")
    val documentIndex: Int,
    @SerialName("document_title")
    val documentTitle: String? = null,
    @SerialName("start_block_index")
    val startBlockIndex: Int,
    @SerialName("end_block_index")
    val endBlockIndex: Int
) : Citation

@Serializable
@SerialName("web_search_result_location")
data class CitationWebSearchResultLocation(
    @SerialName("cited_text")
    val citedText: String,
    @SerialName("encrypted_index")
    val encryptedIndex: String,
    val title: String? = null,
    val url: String? = null
) : Citation

@Serializable
@SerialName("search_result_location")
data class CitationSearchResultLocation(
    @SerialName("cited_text")
    val citedText: String,
    @SerialName("search_result_index")
    val searchResultIndex: Int,
    val source: String? = null,
    val title: String? = null,
    @SerialName("start_block_index")
    val startBlockIndex: Int? = null,
    @SerialName("end_block_index")
    val endBlockIndex: Int? = null
) : Citation

// ============ Content Block 类型 ============

/**
 * 内容块 - 多态类型
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface ContentBlock

/**
 * 文本块
 */
@Serializable
@SerialName("text")
data class TextBlock(
    val text: String,
    @SerialName("cache_control")
    val cacheControl: CacheControl? = null,
    val citations: List<Citation>? = null
) : ContentBlock

/**
 * 图像块
 */
@Serializable
@SerialName("image")
data class ImageBlock(
    val source: ImageSource,
    @SerialName("cache_control")
    val cacheControl: CacheControl? = null
) : ContentBlock

/**
 * 文档块
 */
@Serializable
@SerialName("document")
data class DocumentBlock(
    val source: DocumentSource,
    val title: String? = null,
    val context: String? = null,
    val citations: CitationsConfig? = null,
    @SerialName("cache_control")
    val cacheControl: CacheControl? = null
) : ContentBlock

/**
 * 工具使用块
 */
@Serializable
@SerialName("tool_use")
data class ToolUseBlock(
    val id: String,
    val name: String,
    val input: JsonElement
) : ContentBlock

/**
 * 工具结果块
 */
@Serializable
@SerialName("tool_result")
data class ToolResultBlock(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: ToolResultContent? = null,
    @SerialName("is_error")
    val isError: Boolean? = null,
    @SerialName("cache_control")
    val cacheControl: CacheControl? = null
) : ContentBlock

/**
 * 工具结果内容 - 可以是字符串或内容块列表
 */
@Serializable
@JsonClassDiscriminator("type")
sealed interface ToolResultContent

@Serializable
@SerialName("text")
data class ToolResultText(
    val text: String
) : ToolResultContent

@Serializable
@SerialName("blocks")
data class ToolResultBlocks(
    val blocks: List<ContentBlock>
) : ToolResultContent

/**
 * 思考块
 */
@Serializable
@SerialName("thinking")
data class ThinkingBlock(
    val thinking: String,
    val signature: String? = null
) : ContentBlock

/**
 * 隐藏思考块
 */
@Serializable
@SerialName("redacted_thinking")
data class RedactedThinkingBlock(
    val data: String
) : ContentBlock

/**
 * 服务器工具使用块
 */
@Serializable
@SerialName("server_tool_use")
data class ServerToolUseBlock(
    val id: String,
    val name: String,
    val input: JsonElement
) : ContentBlock

/**
 * Web 搜索结果项
 */
@Serializable
data class WebSearchResultItem(
    val type: String = "web_search_result",
    val title: String,
    val url: String,
    @SerialName("encrypted_content")
    val encryptedContent: String? = null,
    @SerialName("page_age")
    val pageAge: String? = null
)

/**
 * Web 搜索工具结果块
 */
@Serializable
@SerialName("web_search_tool_result")
data class WebSearchToolResultBlock(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: List<WebSearchResultItem>
) : ContentBlock

/**
 * 搜索结果块
 */
@Serializable
@SerialName("search_result")
data class SearchResultBlock(
    val source: String,
    val title: String,
    val content: List<ContentBlock>,
    val citations: CitationsConfig? = null
) : ContentBlock
