package com.github.hank9999.kirokt.routes

import com.github.hank9999.kirokt.anthropic.model.*
import com.github.hank9999.kirokt.converter.AnthropicToKiroConverter
import com.github.hank9999.kirokt.converter.KiroToAnthropicConverter
import com.github.hank9999.kirokt.kiro.model.Event as KiroEvent
import com.github.hank9999.kirokt.kiro.model.KiroRequest
import com.github.hank9999.kirokt.kiro.parser.KiroEventStreamParser
import com.github.hank9999.kirokt.kiro.request.KiroRequester
import com.github.hank9999.kirokt.utils.TokenEstimator
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * JSON 序列化器
 */
private val json = Json {
    prettyPrint = false
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

private val logger = LoggerFactory.getLogger("AnthropicRoutes")

/**
 * Anthropic API 路由
 */
fun Route.anthropicRoutes() {
    val kiroRequester: KiroRequester by inject()

    route("/v1") {
        // GET /v1/models - 获取模型列表
        get("/models") {
            call.respond(Claude45Models.getModelsResponse())
        }

        // GET /v1/models/{model_id} - 获取单个模型信息
        get("/models/{model_id}") {
            val modelId = call.parameters["model_id"]
            if (modelId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("缺少模型 ID")
                )
                return@get
            }

            val model = Claude45Models.getModel(modelId)
            if (model == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse.notFound("模型 '$modelId' 未找到")
                )
                return@get
            }

            call.respond(model)
        }

        // POST /v1/messages - 创建消息
        post("/messages") {
            val request = try {
                call.receive<MessagesRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("无效的请求体: ${e.message}")
                )
                return@post
            }

            // 验证模型
            if (!Claude45Models.isValidModel(request.model)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("无效的模型: ${request.model}")
                )
                return@post
            }

            // 验证 max_tokens
            if (request.maxTokens <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("max_tokens 必须大于 0")
                )
                return@post
            }

            // 验证 messages
            if (request.messages.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("messages 数组是必需的且不能为空")
                )
                return@post
            }

            // 判断是否为流式请求
            if (request.stream == true) {
                handleStreamingResponse(call, request, kiroRequester)
            } else {
                handleNonStreamingResponse(call, request, kiroRequester)
            }
        }

        // POST /v1/messages/count_tokens - 计算 token 数量
        post("/messages/count_tokens") {
            val request = try {
                call.receive<CountTokensRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("无效的请求体: ${e.message}")
                )
                return@post
            }

            // 验证模型
            if (!Claude45Models.isValidModel(request.model)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("无效的模型: ${request.model}")
                )
                return@post
            }

            // 验证 messages
            if (request.messages.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("messages 数组是必需的且不能为空")
                )
                return@post
            }

            // 计算 token 数量
            val inputTokens = countTokensForRequest(request)

            val response = CountTokensResponse(
                inputTokens = inputTokens
            )

            call.respond(response)
        }
    }
}

/**
 * 处理非流式响应
 */
private suspend fun handleNonStreamingResponse(
    call: ApplicationCall,
    request: MessagesRequest,
    kiroRequester: KiroRequester
) {
    try {
        // 1. 转换请求
        val kiroRequest = AnthropicToKiroConverter.convert(request)
        val kiroRequestJson = json.encodeToString(KiroRequest.serializer(), kiroRequest)

        logger.debug("Kiro request: {}", kiroRequestJson)

        // 2. 发送请求到 Kiro
        val response = kiroRequester.callApiStream(kiroRequestJson)

        // 3. 解析响应并聚合内容
        val parser = KiroEventStreamParser()
        val contentBuilder = StringBuilder()
        val toolUseBlocks = mutableListOf<ContentBlock>()
        var stopReason: StopReason = StopReason.END_TURN
        var inputTokens = 0
        var outputTokens = 0

        response.bodyAsChannel().let { channel ->
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead <= 0) break

                val events = parser.parse(buffer, 0, bytesRead)
                for (event in events) {
                    when (event) {
                        is KiroEvent.AssistantResponse -> {
                            contentBuilder.append(event.data.content)
                            if (event.data.isCompleted) {
                                stopReason = StopReason.END_TURN
                            }
                        }
                        is KiroEvent.ToolUse -> {
                            if (event.data.isCompleted) {
                                // 构建工具使用块
                                val input = event.data.input ?: kotlinx.serialization.json.JsonObject(emptyMap())
                                toolUseBlocks.add(ToolUseBlock(
                                    id = event.data.toolUseId,
                                    name = event.data.name,
                                    input = input
                                ))
                                stopReason = StopReason.TOOL_USE
                            }
                        }
                        is KiroEvent.ContextUsage -> {
                            // 从上下文使用百分比计算实际的 input_tokens
                            // 公式: percentage * 200000 / 100 = percentage * 2000
                            inputTokens = (event.data.contextUsagePercentage * 200_000 / 100.0).toInt()
                            logger.debug(
                                "收到 contextUsageEvent: {}%, 计算 input_tokens: {}",
                                event.data.contextUsagePercentage,
                                inputTokens
                            )
                        }
                        else -> { /* 忽略其他事件 */ }
                    }
                }
            }
        }

        // 4. 构建响应
        val content = mutableListOf<ContentBlock>()
        if (contentBuilder.isNotEmpty()) {
            content.add(TextBlock(text = contentBuilder.toString()))
        }
        content.addAll(toolUseBlocks)

        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        val messagesResponse = MessagesResponse(
            id = messageId,
            type = "message",
            role = "assistant",
            model = request.model,
            content = content,
            stopReason = stopReason,
            stopSequence = null,
            usage = Usage(
                inputTokens = inputTokens,
                outputTokens = 1
            )
        )

        call.respond(messagesResponse)

    } catch (e: Exception) {
        logger.error("处理非流式请求失败", e)
        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse.apiError("处理请求失败: ${e.message}")
        )
    }
}

/**
 * 处理流式响应 (SSE)
 */
private suspend fun handleStreamingResponse(
    call: ApplicationCall,
    request: MessagesRequest,
    kiroRequester: KiroRequester
) {
    try {
        // 1. 转换请求
        val kiroRequest = AnthropicToKiroConverter.convert(request)
        val kiroRequestJson = json.encodeToString(KiroRequest.serializer(), kiroRequest)

        logger.debug("Kiro request (streaming): {}", kiroRequestJson)

        // 2. 发送请求到 Kiro
        val response = kiroRequester.callApiStream(kiroRequestJson)

        // 3. 设置 SSE 响应头
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.response.header(HttpHeaders.Connection, "keep-alive")

        // 4. 创建转换器
        val converter = KiroToAnthropicConverter(model = request.model)

        // 5. 流式处理响应
        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
            val parser = KiroEventStreamParser()

            response.bodyAsChannel().let { channel ->
                val buffer = ByteArray(8192)
                var messageStarted = false

                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) break

                    val events = parser.parse(buffer, 0, bytesRead)

                    for (event in events) {
                        // 确保 message_start 已发送
                        if (!messageStarted) {
                            write(converter.generateMessageStart())
                            flush()
                            messageStarted = true
                        }

                        // 转换并发送事件
                        val sseEvents = converter.processEvent(event)
                        for (sseEvent in sseEvents) {
                            write(sseEvent)
                            flush()
                        }
                    }
                }

                // 如果还没发送过消息开始，发送空响应
                if (!messageStarted) {
                    write(converter.generateMessageStart())
                    flush()
                }

                // 确保消息结束事件已发送
                // (processEvent 应该已经处理了，但作为保险)
            }
        }

    } catch (e: Exception) {
        logger.error("处理流式请求失败", e)

        // 对于流式请求，尝试发送错误事件
        try {
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header(HttpHeaders.Connection, "keep-alive")

            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                val errorEvent = StreamErrorEvent(
                    error = ApiError.apiError("处理请求失败: ${e.message}")
                )
                write("event: error\n")
                write("data: ${json.encodeToString(StreamErrorEvent.serializer(), errorEvent)}\n\n")
                flush()
            }
        } catch (e2: Exception) {
            // 如果已经开始写入响应，可能无法发送错误
            logger.error("无法发送错误事件", e2)
        }
    }
}

/**
 * 计算 CountTokensRequest 的 token 数量
 */
private fun countTokensForRequest(request: CountTokensRequest): Int {
    var total = 0L

    // 1. 系统消息
    request.system?.let { system ->
        when (system) {
            is JsonPrimitive -> {
                if (system.isString) {
                    total += TokenEstimator.countTokens(system.content)
                }
            }
            is JsonArray -> {
                for (block in system.jsonArray) {
                    if (block is JsonObject) {
                        block["text"]?.jsonPrimitive?.content?.let { text ->
                            total += TokenEstimator.countTokens(text)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    // 2. 用户消息
    for (message in request.messages) {
        val content = message.content
        when (content) {
            is JsonPrimitive -> {
                if (content.isString) {
                    total += TokenEstimator.countTokens(content.content)
                }
            }
            is JsonArray -> {
                for (item in content.jsonArray) {
                    if (item is JsonObject) {
                        // 文本内容
                        item["text"]?.jsonPrimitive?.content?.let { text ->
                            total += TokenEstimator.countTokens(text)
                        }
                        // 图片内容 - 计算 Base64 数据长度
                        item["source"]?.jsonObject?.get("data")?.jsonPrimitive?.content?.let { data ->
                            total += TokenEstimator.countTokens(data)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    // 3. 工具定义
    request.tools?.forEach { tool ->
        when (tool) {
            is CustomTool -> {
                total += TokenEstimator.countTokens(tool.name)
                tool.description?.let { desc ->
                    total += TokenEstimator.countTokens(desc)
                }
                val inputSchemaJson = json.encodeToString(InputSchema.serializer(), tool.inputSchema)
                total += TokenEstimator.countTokens(inputSchemaJson)
            }
            is BashTool -> {
                total += TokenEstimator.countTokens(tool.name)
            }
            is TextEditorTool20250124 -> {
                total += TokenEstimator.countTokens(tool.name)
            }
            is TextEditorTool20250429 -> {
                total += TokenEstimator.countTokens(tool.name)
            }
            is TextEditorTool20250728 -> {
                total += TokenEstimator.countTokens(tool.name)
            }
            is WebSearchTool -> {
                total += TokenEstimator.countTokens(tool.name)
            }
        }
    }

    return total.coerceAtLeast(1).toInt()
}
