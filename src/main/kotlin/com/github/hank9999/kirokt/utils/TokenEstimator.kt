package com.github.hank9999.kirokt.utils

/**
 * Token 估算工具
 *
 * 提供文本 token 数量估算功能
 *
 * ## 计算规则
 * - 非西文字符：每个计 4.0 个字符单位
 * - 西文字符：每个计 1.0 个字符单位
 * - 4 个字符单位 = 1 token
 * - 根据 token 数量应用不同的修正系数
 */
object TokenEstimator {

    /**
     * 计算文本的 token 数量
     *
     * @param text 要计算的文本
     * @return 估算的 token 数量
     */
    fun countTokens(text: String): Long {
        val charUnits = text.sumOf { c ->
            if (isNonWesternChar(c)) 4.0 else 1.0
        }

        val tokens = charUnits / 4.0

        // 根据 token 数量应用修正系数
        val adjustedTokens = when {
            tokens < 100.0 -> tokens * 1.5
            tokens < 200.0 -> tokens * 1.3
            tokens < 300.0 -> tokens * 1.25
            tokens < 800.0 -> tokens * 1.2
            else -> tokens
        }

        return adjustedTokens.toLong()
    }

    /**
     * 判断字符是否为非西文字符
     *
     * 西文字符包括：
     * - ASCII 字符 (U+0000..U+007F)
     * - 拉丁字母扩展 (U+0080..U+024F)
     * - 拉丁字母扩展附加 (U+1E00..U+1EFF)
     * - 拉丁字母扩展-C/D/E (U+2C60..U+2C7F, U+A720..U+A7FF, U+AB30..U+AB6F)
     *
     * @param c 要判断的字符
     * @return true 表示该字符是非西文字符（如中文、日文、韩文等）
     */
    private fun isNonWesternChar(c: Char): Boolean {
        val code = c.code
        return !(
            // 基本 ASCII
            code in 0x0000..0x007F ||
            // 拉丁字母扩展-A (Latin Extended-A)
            code in 0x0080..0x00FF ||
            // 拉丁字母扩展-B (Latin Extended-B)
            code in 0x0100..0x024F ||
            // 拉丁字母扩展附加 (Latin Extended Additional)
            code in 0x1E00..0x1EFF ||
            // 拉丁字母扩展-C
            code in 0x2C60..0x2C7F ||
            // 拉丁字母扩展-D
            code in 0xA720..0xA7FF ||
            // 拉丁字母扩展-E
            code in 0xAB30..0xAB6F
        )
    }
}
