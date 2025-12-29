package com.github.hank9999.kirokt.parser.kiro

import java.util.zip.CRC32

/**
 * AWS Event Stream CRC32 校验工具
 *
 * 使用标准 CRC32 算法（IEEE 802.3 多项式）
 */
object Crc32 {
    /**
     * 计算字节数组的 CRC32 校验值
     */
    fun compute(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }

    /**
     * 计算字节数组指定范围的 CRC32 校验值
     */
    fun compute(data: ByteArray, offset: Int, length: Int): Long {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value
    }

    /**
     * 验证 CRC32 校验值
     */
    fun verify(data: ByteArray, expected: Long): Boolean {
        return compute(data) == expected
    }

    /**
     * 验证字节数组指定范围的 CRC32 校验值
     */
    fun verify(data: ByteArray, offset: Int, length: Int, expected: Long): Boolean {
        return compute(data, offset, length) == expected
    }
}
