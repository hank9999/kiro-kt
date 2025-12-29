package config

import kotlinx.serialization.Serializable

@Serializable
data class KiroCredentials(
    /// 访问令牌
    val accessToken: String? = null,
    /// 刷新令牌
    val refreshToken: String,
    /// Profile ARN
    val profileArn: String? = null,
    /// 过期时间 (RFC3339 格式)
    val expiresAt: String? = null,
    /// 认证方式
    val authMethod: String? = null,
    /// 凭证提供者标识
    val provider: String? = null
)