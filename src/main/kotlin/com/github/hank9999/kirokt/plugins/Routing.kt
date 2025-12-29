package com.github.hank9999.kirokt.plugins

import com.github.hank9999.kirokt.routes.anthropicRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * 配置路由
 */
fun Application.configureRouting() {
    routing {
        anthropicRoutes()
    }
}
