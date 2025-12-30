package com.github.hank9999.kirokt.kiro

import com.github.hank9999.kirokt.config.AppConfig
import com.github.hank9999.kirokt.config.ConfigLoader
import com.github.hank9999.kirokt.config.KiroCredentials
import com.github.hank9999.kirokt.kiro.model.RefreshRequest
import com.github.hank9999.kirokt.kiro.model.RefreshResponse
import com.github.hank9999.kirokt.utils.MachineIdGenerator
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Token 管理器
 *
 * 负责管理凭据和 Token 的自动刷新
 */
class TokenManager(
    /** 应用配置 */
    val config: AppConfig,
    private val credentialsPath: String,
    private val httpClient: HttpClient
) {
    private val logger = LoggerFactory.getLogger(TokenManager::class.java)
    private val mutex = Mutex()

    /** 当前凭据（线程安全） */
    @Volatile
    var credentials: KiroCredentials = ConfigLoader.loadCredentials(credentialsPath)
        private set

    /**
     * 确保获取有效的访问 Token
     *
     * 如果 Token 过期或即将过期，会自动刷新
     */
    suspend fun ensureValidToken(): String {
        return mutex.withLock {
            if (isTokenExpired() || isTokenExpiringSoon()) {
                credentials = refreshToken()

                // 刷新后再次检查 token 时间有效性
                if (isTokenExpired()) {
                    throw TokenException("刷新后的 Token 仍然无效或已过期")
                }

                // 持久化到文件
                persistCredentials()
            }

            credentials.accessToken
                ?: throw TokenException("没有可用的 accessToken")
        }
    }

    /**
     * 检查 Token 是否在指定时间内过期
     */
    private fun isTokenExpiringWithin(minutes: Long): Boolean? {
        val expiresAt = credentials.expiresAt ?: return null

        return try {
            val expires = ZonedDateTime.parse(expiresAt, DateTimeFormatter.ISO_DATE_TIME)
            val threshold = Instant.now().plus(minutes, ChronoUnit.MINUTES)
            !expires.toInstant().isAfter(threshold)
        } catch (e: Exception) {
            logger.warn("无法解析过期时间: $expiresAt", e)
            null
        }
    }

    /**
     * 检查 Token 是否已过期（提前 5 分钟判断）
     */
    private fun isTokenExpired(): Boolean {
        return isTokenExpiringWithin(5) ?: true
    }

    /**
     * 检查 Token 是否即将过期（10分钟内）
     */
    private fun isTokenExpiringSoon(): Boolean {
        return isTokenExpiringWithin(10) ?: false
    }

    /**
     * 验证 refreshToken 的基本有效性
     */
    private fun validateRefreshToken() {
        val refreshToken = credentials.refreshToken

        if (refreshToken.isBlank()) {
            throw TokenException("refreshToken 为空")
        }

        if (refreshToken.length < 100 ||
            refreshToken.endsWith("...") ||
            refreshToken.contains("...")
        ) {
            throw TokenException(
                "refreshToken 已被截断（长度: ${refreshToken.length} 字符）。\n" +
                        "这通常是 Kiro IDE 为了防止凭证被第三方工具使用而故意截断的。"
            )
        }
    }

    /**
     * 刷新 Token
     */
    private suspend fun refreshToken(): KiroCredentials {
        validateRefreshToken()

        logger.info("正在刷新 Token...")

        val refreshToken = credentials.refreshToken
        val region = config.region

        val refreshUrl = "https://prod.$region.auth.desktop.kiro.dev/refreshToken"
        val refreshDomain = "prod.$region.auth.desktop.kiro.dev"
        val machineId = MachineIdGenerator.generateFromCredentials(credentials, config)
            ?: throw TokenException("无法生成 machineId")
        val kiroVersion = config.kiroVersion

        val response = httpClient.post(refreshUrl) {
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.UserAgent, "KiroIDE-$kiroVersion-$machineId")
            header(HttpHeaders.AcceptEncoding, "gzip, compress, deflate, br")
            header(HttpHeaders.Host, refreshDomain)
            header(HttpHeaders.Connection, "close")
            setBody(RefreshRequest(refreshToken))
        }

        if (!response.status.isSuccess()) {
            val bodyText = try {
                response.body<String>()
            } catch (e: Exception) {
                ""
            }

            val errorMsg = when (response.status.value) {
                401 -> "OAuth 凭证已过期或无效，需要重新认证"
                403 -> "权限不足，无法刷新 Token"
                429 -> "请求过于频繁，已被限流"
                in 500..599 -> "服务器错误，AWS OAuth 服务暂时不可用"
                else -> "Token 刷新失败"
            }
            throw TokenException("$errorMsg: ${response.status} $bodyText")
        }

        val data: RefreshResponse = response.body()

        val newCredentials = credentials.copy(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken ?: credentials.refreshToken,
            profileArn = data.profileArn ?: credentials.profileArn,
            expiresAt = data.expiresIn?.let {
                ZonedDateTime.now(ZoneOffset.UTC)
                    .plusSeconds(it)
                    .format(DateTimeFormatter.ISO_DATE_TIME)
            } ?: credentials.expiresAt
        )

        logger.info("Token 刷新成功")
        return newCredentials
    }

    /**
     * 持久化凭据到文件
     */
    private fun persistCredentials() {
        try {
            ConfigLoader.saveCredentials(credentialsPath, credentials)
            logger.info("凭据已保存到: $credentialsPath")
        } catch (e: Exception) {
            logger.error("保存凭据失败: ${e.message}", e)
        }
    }
}

/**
 * Token 相关异常
 */
class TokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
