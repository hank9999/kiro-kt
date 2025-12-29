package com.github.hank9999.kirokt.plugins

import io.ktor.server.application.*
import io.ktor.server.sse.*

/**
 * 配置 Server-Sent Events (SSE)
 */
fun Application.configureSSE() {
    install(SSE)
}
