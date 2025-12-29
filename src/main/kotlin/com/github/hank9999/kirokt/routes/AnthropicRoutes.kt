package com.github.hank9999.kirokt.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import com.github.hank9999.kirokt.model.anthropic.*
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

/**
 * Anthropic API 路由
 */
fun Route.anthropicRoutes() {
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
                    ErrorResponse.invalidRequest("model_id is required")
                )
                return@get
            }

            val model = Claude45Models.getModel(modelId)
            if (model == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse.notFound("Model '$modelId' not found")
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
                    ErrorResponse.invalidRequest("Invalid request body: ${e.message}")
                )
                return@post
            }

            // 验证模型
            if (!Claude45Models.isValidModel(request.model)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("Invalid com.github.hank9999.model: ${request.model}")
                )
                return@post
            }

            // 验证 max_tokens
            if (request.maxTokens <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("max_tokens must be greater than 0")
                )
                return@post
            }

            // 验证 messages
            if (request.messages.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("messages array is required and cannot be empty")
                )
                return@post
            }

            // 判断是否为流式请求
            if (request.stream == true) {
                handleStreamingResponse(call, request)
            } else {
                handleNonStreamingResponse(call, request)
            }
        }

        // POST /v1/messages/count_tokens - 计算 token 数量
        post("/messages/count_tokens") {
            val request = try {
                call.receive<CountTokensRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("Invalid request body: ${e.message}")
                )
                return@post
            }

            // 验证模型
            if (!Claude45Models.isValidModel(request.model)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("Invalid com.github.hank9999.model: ${request.model}")
                )
                return@post
            }

            // 验证 messages
            if (request.messages.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse.invalidRequest("messages array is required and cannot be empty")
                )
                return@post
            }

            // TODO: 实际的 token 计数逻辑
            // 这里返回一个占位响应，实际实现需要对接后端服务
            val response = CountTokensResponse(
                inputTokens = 0,
                cacheCreationInputTokens = 0,
                cacheReadInputTokens = 0
            )

            call.respond(response)
        }
    }
}

/**
 * 处理非流式响应
 */
private suspend fun handleNonStreamingResponse(call: ApplicationCall, request: MessagesRequest) {
    val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(24)}"

    // TODO: 实际的消息处理逻辑
    // 这里返回一个占位响应，实际实现需要对接后端服务
    val response = MessagesResponse(
        id = messageId,
        type = "message",
        role = "assistant",
        model = request.model,
        content = listOf(
            TextBlock(text = "This is a placeholder response. Backend integration pending.")
        ),
        stopReason = StopReason.END_TURN,
        stopSequence = null,
        usage = Usage(
            inputTokens = 0,
            outputTokens = 0
        )
    )

    call.respond(response)
}

/**
 * 处理流式响应 (SSE)
 * 使用 respondTextWriter 手动发送 SSE 格式的响应
 */
private suspend fun handleStreamingResponse(call: ApplicationCall, request: MessagesRequest) {
    val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(24)}"

    call.response.header(HttpHeaders.CacheControl, "no-cache")
    call.response.header(HttpHeaders.Connection, "keep-alive")

    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        // 辅助函数：发送 SSE 事件
        suspend fun sendEvent(event: String, data: String) {
            write("event: $event\n")
            write("data: $data\n\n")
            flush()
        }

        // 1. message_start 事件
        val messageStartEvent = MessageStartEvent(
            message = MessagesResponse(
                id = messageId,
                model = request.model,
                content = emptyList(),
                stopReason = null,
                usage = Usage(inputTokens = 0, outputTokens = 1)
            )
        )
        sendEvent("message_start", json.encodeToString(messageStartEvent))

        // 2. content_block_start 事件
        val contentBlockStartEvent = ContentBlockStartEvent(
            index = 0,
            contentBlock = TextBlock(text = "")
        )
        sendEvent("content_block_start", json.encodeToString(contentBlockStartEvent))

        // 3. content_block_delta 事件 - TODO: 实际的流式内容
        val placeholderText = "This is a placeholder streaming response. Backend integration pending."
        for (char in placeholderText) {
            val deltaEvent = ContentBlockDeltaEvent(
                index = 0,
                delta = TextDelta(text = char.toString())
            )
            sendEvent("content_block_delta", json.encodeToString(deltaEvent))
            delay(10) // 模拟流式输出延迟
        }

        // 4. content_block_stop 事件
        val contentBlockStopEvent = ContentBlockStopEvent(index = 0)
        sendEvent("content_block_stop", json.encodeToString(contentBlockStopEvent))

        // 5. message_delta 事件
        val messageDeltaEvent = MessageDeltaEvent(
            delta = MessageDeltaData(stopReason = StopReason.END_TURN),
            usage = Usage(inputTokens = 0, outputTokens = placeholderText.length)
        )
        sendEvent("message_delta", json.encodeToString(messageDeltaEvent))

        // 6. message_stop 事件
        val messageStopEvent = MessageStopEvent()
        sendEvent("message_stop", json.encodeToString(messageStopEvent))
    }
}
