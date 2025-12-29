package com.github.hank9999.kirokt.model.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 缓存控制
 */
@Serializable
data class CacheControl(
    val type: String = "ephemeral",
    val ttl: CacheTTL? = null
)

/**
 * 用量统计
 */
@Serializable
data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int,

    @SerialName("output_tokens")
    val outputTokens: Int = 0,

    @SerialName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int? = null,

    @SerialName("cache_read_input_tokens")
    val cacheReadInputTokens: Int? = null,

    @SerialName("cache_creation")
    val cacheCreation: CacheCreationUsage? = null,

    @SerialName("server_tool_use")
    val serverToolUse: ServerToolUseUsage? = null,

    @SerialName("service_tier")
    val serviceTier: String? = null
)

/**
 * 缓存创建用量
 */
@Serializable
data class CacheCreationUsage(
    @SerialName("ephemeral_5m_input_tokens")
    val ephemeral5mInputTokens: Int = 0,

    @SerialName("ephemeral_1h_input_tokens")
    val ephemeral1hInputTokens: Int = 0
)

/**
 * 服务器工具使用用量
 */
@Serializable
data class ServerToolUseUsage(
    @SerialName("web_search_requests")
    val webSearchRequests: Int = 0
)

/**
 * 元数据
 */
@Serializable
data class Metadata(
    @SerialName("user_id")
    val userId: String? = null
)

/**
 * JSON Schema 输入
 */
@Serializable
data class InputSchema(
    val type: String = "object",
    val properties: Map<String, JsonElement>? = null,
    val required: List<String>? = null,
    val additionalProperties: Boolean? = null
)

/**
 * 用户位置 (用于 Web 搜索)
 */
@Serializable
data class UserLocation(
    val type: String = "approximate",
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val timezone: String? = null
)
