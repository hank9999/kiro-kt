package com.github.hank9999.kirokt.kiro.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Token 刷新请求
 */
@Serializable
data class RefreshRequest(
    @SerialName("refreshToken")
    val refreshToken: String
)

/**
 * Token 刷新响应
 */
@Serializable
data class RefreshResponse(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("profileArn")
    val profileArn: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Long? = null
)
