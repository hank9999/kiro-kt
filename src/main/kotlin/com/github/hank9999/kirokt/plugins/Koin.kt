package com.github.hank9999.kirokt.plugins

import com.github.hank9999.kirokt.config.AppConfig
import com.github.hank9999.kirokt.kiro.TokenManager
import com.github.hank9999.kirokt.utils.JsonConfig
import io.ktor.client.*
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * 创建应用模块
 */
fun appModule(
    appConfig: AppConfig,
    credentialsPath: String
) = module {
    single { appConfig }
    single {
        HttpClient(Java) {
            install(ContentNegotiation) {
                json(JsonConfig.default)
            }
        }
    }
    single(createdAtStart = true) { TokenManager(appConfig, credentialsPath, get()) }
}

/**
 * 配置 Koin 依赖注入
 */
fun Application.configureKoin(
    appConfig: AppConfig,
    credentialsPath: String
) {
    install(Koin) {
        slf4jLogger()
        modules(appModule(appConfig, credentialsPath))
    }
}
