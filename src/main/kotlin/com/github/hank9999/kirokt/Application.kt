package com.github.hank9999.kirokt

import com.github.hank9999.kirokt.config.AppConfig
import com.github.hank9999.kirokt.config.CliArgs
import com.github.hank9999.kirokt.config.ConfigLoader
import com.github.hank9999.kirokt.config.KiroCredentials
import com.github.hank9999.kirokt.plugins.configureKoin
import com.github.hank9999.kirokt.plugins.configureRouting
import com.github.hank9999.kirokt.plugins.configureSerialization
import com.github.hank9999.kirokt.plugins.configureSSE
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private lateinit var appConfig: AppConfig
private lateinit var credentials: KiroCredentials

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("com.github.hank9999.kirokt.Application")
    logger.info("KIRO KT - Your next generation AI Client Tools.")

    // 解析命令行参数
    val cliArgs = try {
        CliArgs.parse(args)
    } catch (e: IllegalArgumentException) {
        logger.error(e.message)
        kotlin.system.exitProcess(1)
    }

    // 加载配置
    try {
        appConfig = ConfigLoader.loadAppConfig(cliArgs.configPath)
        credentials = ConfigLoader.loadCredentials(cliArgs.credentialsPath)
    } catch (e: Exception) {
        logger.error("配置加载失败: ${e.message}")
        kotlin.system.exitProcess(1)
    }

    embeddedServer(
        Netty,
        port = appConfig.port,
        host = appConfig.host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureKoin(appConfig, credentials)
    configureSerialization()
    configureSSE()
    configureRouting()
}
