package com.github.hank9999.kirokt.utils

import com.github.hank9999.kirokt.config.AppConfig
import com.github.hank9999.kirokt.config.KiroCredentials
import java.security.MessageDigest

/**
 * 设备指纹生成器
 *
 * 根据凭证信息生成唯一的 Machine ID
 */
object MachineIdGenerator {

    /**
     * 根据凭证信息生成唯一的 Machine ID
     *
     * 优先使用自定义配置，然后使用 profileArn 生成，否则使用 refreshToken 生成
     */
    fun generateFromCredentials(credentials: KiroCredentials, config: AppConfig): String? {
        // 如果配置了自定义 machineId 且长度为 64，优先使用
        config.machineId?.let { machineId ->
            if (machineId.length == 64) {
                return machineId
            }
        }

        // 如果有有效的 profileArn 则使用 profileArn 固定指纹
        credentials.profileArn?.let { profileArn ->
            if (isValidProfileArn(profileArn)) {
                return sha256Hex("KotlinNativeAPI/$profileArn")
            }
        }

        // 使用 refreshToken 生成
        if (credentials.refreshToken.isNotEmpty()) {
            return sha256Hex("KotlinNativeAPI/${credentials.refreshToken}")
        }

        // 没有有效的凭证
        return null
    }

    /**
     * 验证 profileArn 是否有效
     */
    private fun isValidProfileArn(profileArn: String): Boolean {
        return profileArn.isNotEmpty()
                && profileArn.startsWith("arn:aws")
                && profileArn.contains("profile/")
    }

    /**
     * SHA256 哈希实现（返回十六进制字符串）
     */
    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.toHexString()
    }
}
