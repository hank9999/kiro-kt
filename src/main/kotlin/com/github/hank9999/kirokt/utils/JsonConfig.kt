package com.github.hank9999.kirokt.utils

import kotlinx.serialization.json.Json

/**
 * 共享的 JSON 序列化配置
 */
object JsonConfig {
    val default: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}
