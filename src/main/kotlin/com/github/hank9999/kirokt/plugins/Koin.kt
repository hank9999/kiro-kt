package com.github.hank9999.kirokt.plugins

import com.github.hank9999.kirokt.config.AppConfig
import com.github.hank9999.kirokt.config.KiroCredentials
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

/**
 * 创建应用模块
 */
fun appModule(appConfig: AppConfig, credentials: KiroCredentials) = module {
    single { appConfig }
    single { credentials }
}

/**
 * 配置 Koin 依赖注入
 */
fun Application.configureKoin(appConfig: AppConfig, credentials: KiroCredentials) {
    install(Koin) {
        slf4jLogger()
        modules(appModule(appConfig, credentials))
    }
}
