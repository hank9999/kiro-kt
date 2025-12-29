package com.github.hank9999.kirokt.config

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 配置加载器
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * 加载应用配置
     */
    fun loadAppConfig(path: String): AppConfig {
        val file = File(path)
        if (!file.exists()) {
            throw ConfigException("配置文件不存在: $path")
        }
        if (!file.canRead()) {
            throw ConfigException("配置文件无法读取: $path")
        }

        return try {
            val content = file.readText()
            logger.info("加载配置文件: $path")
            json.decodeFromString<AppConfig>(content)
        } catch (e: Exception) {
            throw ConfigException("解析配置文件失败: $path", e)
        }
    }

    /**
     * 加载凭据配置
     */
    fun loadCredentials(path: String): KiroCredentials {
        val file = File(path)
        if (!file.exists()) {
            throw ConfigException("凭据文件不存在: $path")
        }
        if (!file.canRead()) {
            throw ConfigException("凭据文件无法读取: $path")
        }

        return try {
            val content = file.readText()
            logger.info("加载凭据文件: $path")
            json.decodeFromString<KiroCredentials>(content)
        } catch (e: Exception) {
            throw ConfigException("解析凭据文件失败: $path", e)
        }
    }
}

/**
 * 配置加载异常
 */
class ConfigException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
