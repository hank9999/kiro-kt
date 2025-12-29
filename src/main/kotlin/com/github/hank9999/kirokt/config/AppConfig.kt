package com.github.hank9999.kirokt.config

import kotlinx.serialization.Serializable

val defaultSystemVersions = listOf("darwin#24.6.0", "win32#10.0.22631")

@Serializable
data class AppConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8991,
    val apiKey: String,
    val region: String = "us-east-1",
    val kiroVersion: String = "0.8.0",
    val machineId: String? = null,
    val systemVersion: String = defaultSystemVersions.random(),
    val nodeVersion: String = "22.21.1"
)