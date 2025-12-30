package com.github.hank9999.kirokt.kiro.request

import com.github.hank9999.kirokt.kiro.TokenManager
import com.github.hank9999.kirokt.utils.MachineIdGenerator
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("KiroRequester")

/**
 * Kiro API Requester
 *
 * 核心组件，负责与 Kiro API 通信
 */
class KiroRequester(
    private val tokenManager: TokenManager,
    private val httpClient: HttpClient
) {
    /**
     * 获取 API 基础域名
     */
    fun baseDomain(): String {
        return "q.${tokenManager.config.region}.amazonaws.com"
    }

    /**
     * 获取 API 基础 URL
     */
    fun baseUrl(): String {
        return "https://${baseDomain()}/generateAssistantResponse"
    }

    /**
     * 发送流式 API 请求
     *
     * @param requestBody JSON 格式的请求体字符串
     * @return 原始的 HTTP Response，调用方负责处理流式数据
     * @throws KiroRequestException 请求失败时抛出
     */
    suspend fun callApiStream(requestBody: String): HttpResponse {
        val token = tokenManager.ensureValidToken()
        val url = baseUrl()

        logger.debug("发送请求到 Kiro API: {}", url)
        logger.debug("请求体: {}", requestBody)

        val response = httpClient.post(url) {
            buildHeaders(token)
            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val bodyText = try {
                response.bodyAsText()
            } catch (e: Exception) {
                ""
            }
            logger.error("Kiro API 请求失败: {} {}", response.status, bodyText)
            logger.error("失败的请求体: {}", requestBody)
            throw KiroRequestException("API 请求失败: ${response.status} $bodyText")
        }

        return response
    }

    /**
     * 构建请求头
     */
    private fun HttpRequestBuilder.buildHeaders(token: String) {
        val credentials = tokenManager.credentials
        val config = tokenManager.config

        val machineId = MachineIdGenerator.generateFromCredentials(credentials, config)
            ?: throw KiroRequestException("无法生成 machine_id，请检查凭证配置")

        val kiroVersion = config.kiroVersion
        val osName = config.systemVersion
        val nodeVersion = config.nodeVersion

        val xAmzUserAgent = "aws-sdk-js/1.0.27 KiroIDE-$kiroVersion-$machineId"
        val userAgent = "aws-sdk-js/1.0.27 ua/2.1 os/$osName lang/js md/nodejs#$nodeVersion " +
                "api/codewhispererstreaming#1.0.27 m/E KiroIDE-$kiroVersion-$machineId"

        header("content-type", ContentType.Application.Json)
        header("x-amzn-codewhisperer-optout", "true")
        header("x-amzn-kiro-agent-mode", "vibe")
        header("x-amz-user-agent", xAmzUserAgent)
        header("user-agent", userAgent)
        header("host", baseDomain())
        header("amz-sdk-invocation-id", UUID.randomUUID().toString())
        header("amz-sdk-request", "attempt=1; max=3")
        header(HttpHeaders.Authorization, "Bearer $token")
        header(HttpHeaders.Connection, "close")
    }
}

/**
 * Kiro 请求相关异常
 */
class KiroRequestException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
