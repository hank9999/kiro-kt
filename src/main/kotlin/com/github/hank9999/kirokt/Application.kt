package com.github.hank9999.kirokt

import com.github.hank9999.kirokt.plugins.configureRouting
import com.github.hank9999.kirokt.plugins.configureSerialization
import com.github.hank9999.kirokt.plugins.configureSSE
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("com.github.hank9999.kirokt.Application")
    logger.info("KIRO KT - Your next generation AI Client Tools.")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSSE()
    configureRouting()
}
