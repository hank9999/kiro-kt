package com.github.hank9999.kirokt

import com.github.hank9999.kirokt.plugins.configureRouting
import com.github.hank9999.kirokt.plugins.configureSerialization
import com.github.hank9999.kirokt.plugins.configureSSE
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSSE()
    configureRouting()
}
