package com.github.hank9999.kirokt.model.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 模型信息
 */
@Serializable
data class Model(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("context_window")
    val contextWindow: Int,
    @SerialName("max_output_tokens")
    val maxOutputTokens: Int,
    @SerialName("input_cost_per_token")
    val inputCostPerToken: Double,
    @SerialName("output_cost_per_token")
    val outputCostPerToken: Double,
    @SerialName("supports_vision")
    val supportsVision: Boolean = true,
    @SerialName("supports_tools")
    val supportsTools: Boolean = true,
    @SerialName("supports_extended_thinking")
    val supportsExtendedThinking: Boolean = true
)

/**
 * 模型列表响应
 */
@Serializable
data class ModelsResponse(
    val data: List<Model>,
    val `object`: String = "list"
)

/**
 * Claude 4.5 模型定义
 */
object Claude45Models {

    val OPUS_4_5 = Model(
        id = "claude-opus-4-5-20251101",
        name = "Claude Opus 4.5",
        description = "Most capable com.github.hank9999.model for complex tasks requiring deep reasoning",
        contextWindow = 200000,
        maxOutputTokens = 64000,
        inputCostPerToken = 0.015,
        outputCostPerToken = 0.075
    )

    val OPUS_4_5_LATEST = OPUS_4_5.copy(id = "claude-opus-4-5")

    val SONNET_4_5 = Model(
        id = "claude-sonnet-4-5-20250929",
        name = "Claude Sonnet 4.5",
        description = "Balanced com.github.hank9999.model offering excellent performance and speed",
        contextWindow = 200000,
        maxOutputTokens = 64000,
        inputCostPerToken = 0.003,
        outputCostPerToken = 0.015
    )

    val SONNET_4_5_LATEST = SONNET_4_5.copy(id = "claude-sonnet-4-5")

    val HAIKU_4_5 = Model(
        id = "claude-haiku-4-5-20251001",
        name = "Claude Haiku 4.5",
        description = "Fastest and most cost-effective com.github.hank9999.model for simple tasks",
        contextWindow = 200000,
        maxOutputTokens = 64000,
        inputCostPerToken = 0.0008,
        outputCostPerToken = 0.004
    )

    val HAIKU_4_5_LATEST = HAIKU_4_5.copy(id = "claude-haiku-4-5")

    /**
     * 所有可用模型列表
     */
    val ALL_MODELS = listOf(
        OPUS_4_5,
        OPUS_4_5_LATEST,
        SONNET_4_5,
        SONNET_4_5_LATEST,
        HAIKU_4_5,
        HAIKU_4_5_LATEST
    )

    /**
     * 所有有效的模型 ID
     */
    val VALID_MODEL_IDS = ALL_MODELS.map { it.id }.toSet()

    /**
     * 根据模型 ID 获取模型信息
     */
    fun getModel(modelId: String): Model? = ALL_MODELS.find { it.id == modelId }

    /**
     * 检查模型 ID 是否有效
     */
    fun isValidModel(modelId: String): Boolean = modelId in VALID_MODEL_IDS

    /**
     * 获取模型列表响应
     */
    fun getModelsResponse(): ModelsResponse =
        ModelsResponse(data = ALL_MODELS)
}
