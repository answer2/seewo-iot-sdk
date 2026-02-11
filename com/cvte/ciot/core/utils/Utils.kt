package com.cvte.ciot.core.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 10:34
 * @description Utils
 */


object Utils {
    private const val HEX_CHARS = "0123456789ABCDEF"
    private val random = SecureRandom()

    /**
     * HMAC-MD5 签名
     * @param key 密钥
     * @param data 要签名的数据
     * @return 十六进制格式的HMAC-MD5签名
     */
    fun HmacMd5(key: String, data: String): String {
        return try {
            return calculateHmacMD5(key, data)
        } catch (e: Exception) {
            // 记录错误日志
            println("HmacMd5 error: ${e.message}")
            ""
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    @Throws(java.lang.Exception::class)
    fun calculateHmacMD5(key: String, message: String): String {
        // 1. 参数校验
        require(!(key == null || key.isEmpty())) { "密钥不能为空" }
        require(!(message == null || message.isEmpty())) { "消息不能为空" }


        // 2. 创建Mac实例
        val algorithm = "HmacMD5"
        val mac = Mac.getInstance(algorithm)


        // 3. 创建密钥（确保使用正确的编码）
        val secretKey = SecretKeySpec(
            key.toByteArray(StandardCharsets.UTF_8),
            algorithm
        )


        // 4. 初始化并计算
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))


        // 5. 转换为十六进制字符串
        return (hmacBytes).toHexString().uppercase()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Throws(java.lang.Exception::class)
    fun calculateHmacMD5ByteA(key: String, message: String): ByteArray {
        // 1. 参数校验
        require(!(key == null || key.isEmpty())) { "密钥不能为空" }
        require(!(message == null || message.isEmpty())) { "消息不能为空" }


        // 2. 创建Mac实例
        val algorithm = "HmacMD5"
        val mac = Mac.getInstance(algorithm)


        // 3. 创建密钥（确保使用正确的编码）
        val secretKey = SecretKeySpec(
            key.toByteArray(StandardCharsets.UTF_8),
            algorithm
        )


        // 4. 初始化并计算
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))


        // 5. 转换为十六进制字符串
        return (hmacBytes)
    }



    /**
     * 获取当前时间戳（毫秒）
     * @return 从1970年1月1日到现在的毫秒数
     */
    fun GetTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * 获取当前时间戳（微秒）
     * @return 从1970年1月1日到现在的微秒数
     */
    fun GetTimestampMicro(): Long {
        return System.nanoTime() / 1000  // 近似值
    }

    /**
     * 获取带有微秒精度的时间戳
     * @return 毫秒数 + 微秒数（1000*秒 + 微秒/1000）
     */
    fun GetTimestampWithMicro(): Long {
        val time = System.currentTimeMillis()
        val micro = System.nanoTime() % 1_000_000 / 1000
        return time * 1000 + micro
    }

    /**
     * 线程睡眠
     * @param ms 毫秒数
     */
    fun TrySleep(ms: Int) {
        try {
            Thread.sleep(ms.toLong())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            println("Sleep interrupted: ${e.message}")
        }
    }

    /**
     * 转换为字符串
     * @param val 整数值
     * @return 字符串表示
     */
    fun ToString(value: Long): String {
        return value.toString()
    }

    fun ToString(value: Int): String {
        return value.toString()
    }

    fun ToString(value: Double): String {
        return value.toString()
    }

//    /**
//     * 生成随机ID
//     * @return 基于时间戳的随机ID
//     */
//    fun GetRandomID(): String {
////        val timestamp = GetTimestamp()
////        val randomNum = random.nextInt(1000000)
////
////        // 组合时间戳和随机数，并转换为36进制
////        val combined = timestamp * 1000000L + randomNum
////        return combined.toString(36) + random.nextLong().toString(36).take(8)
//        return  getTimeBasedId()
//    }

    // 方法1: 基于时间的ID（类似原C++实现，但更安全）
    @OptIn(ExperimentalStdlibApi::class)
    fun getTimeBasedId(): String {
        val timestamp = System.currentTimeMillis()
        val nanos = System.nanoTime()

        val timestamp_us = nanos * 1000000 + timestamp;

        // 组合时间戳和随机数，转换为16进制
        return timestamp_us.toHexString().uppercase()
    }

    /**
     * 获取基于时间戳的随机ID（毫秒级）
     */
    fun getRandomID(): String {
        // System.currentTimeMillis() 返回毫秒时间戳
        val timestampMs = System.currentTimeMillis()
        return timestampMs.toString()
    }

    /**
     * 获取基于时间戳的随机ID（纳秒级，更高精度）
     */
    fun getHighPrecisionRandomID(): String {
        // System.nanoTime() 返回纳秒级时间，但起始时间不确定
        val timestampNs = System.nanoTime()
        return timestampNs.toString()
    }

    /**
     * 组合毫秒和纳秒获得更高精度
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun GetRandomID(): String {
        val millis = System.currentTimeMillis()
        val nanos = System.nanoTime() * 1000000 // 取纳秒的后6位
        return (millis +nanos).toHexString().uppercase();
    }


    /**
     * 生成指定长度的随机ID
     * @param length ID长度
     * @return 随机ID
     */
    fun GetRandomID(length: Int): String {
        require(length > 0) { "Length must be positive" }

        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * 绝对值
     * @param value 整数值
     * @return 绝对值
     */
    fun abs(value: Long): Long {
        return kotlin.math.abs(value)
    }

    fun abs(value: Int): Int {
        return kotlin.math.abs(value)
    }

    fun abs(value: Double): Double {
        return kotlin.math.abs(value)
    }

    /**
     * MD5哈希
     * @param input 输入字符串
     * @return 十六进制格式的MD5哈希值
     */
    fun Md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            bytesToHex(digest)
        } catch (e: Exception) {
            println("Md5 error: ${e.message}")
            ""
        }
    }

    /**
     * SHA-256哈希
     * @param input 输入字符串
     * @return 十六进制格式的SHA-256哈希值
     */
    fun Sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            bytesToHex(digest)
        } catch (e: Exception) {
            println("Sha256 error: ${e.message}")
            ""
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexBuilder = StringBuilder(bytes.size * 2)

        for (byte in bytes) {
            val hex = (byte and 0xFF.toByte()).toInt()
            hexBuilder.append(HEX_CHARS[hex shr 4])
            hexBuilder.append(HEX_CHARS[hex and 0x0F])
        }

        return hexBuilder.toString()
    }

    /**
     * 十六进制字符串转字节数组
     */
    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }

        val bytes = ByteArray(hex.length / 2)

        for (i in bytes.indices) {
            val high = hexToDigit(hex[i * 2])
            val low = hexToDigit(hex[i * 2 + 1])
            bytes[i] = ((high shl 4) or low).toByte()
        }

        return bytes
    }

    private fun hexToDigit(char: Char): Int {
        return when (char) {
            in '0'..'9' -> char - '0'
            in 'A'..'F' -> char - 'A' + 10
            in 'a'..'f' -> char - 'a' + 10
            else -> throw IllegalArgumentException("Invalid hex character: $char")
        }
    }

    /**
     * 生成Base64编码
     */
    fun Base64Encode(input: String): String {
        return java.util.Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
    }

    fun Base64Encode(bytes: ByteArray): String {
        return java.util.Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * 解码Base64
     */
    fun Base64Decode(input: String): String {
        val bytes = java.util.Base64.getDecoder().decode(input)
        return String(bytes, Charsets.UTF_8)
    }

    fun Base64DecodeToBytes(input: String): ByteArray {
        return java.util.Base64.getDecoder().decode(input)
    }

    /**
     * URL安全的Base64编码
     */
    fun Base64UrlEncode(input: String): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        return encoder.encodeToString(input.toByteArray(Charsets.UTF_8))
    }

    /**
     * URL安全的Base64解码
     */
    fun Base64UrlDecode(input: String): String {
        val bytes = java.util.Base64.getUrlDecoder().decode(input)
        return String(bytes, Charsets.UTF_8)
    }
}

/**
 * 扩展函数
 */

/**
 * 字符串扩展：生成HMAC-MD5签名
 */
fun String.hmacMd5(key: String): String {
    return Utils.HmacMd5(key, this)
}

/**
 * 字符串扩展：MD5哈希
 */
fun String.md5(): String {
    return Utils.Md5(this)
}

/**
 * 字符串扩展：SHA-256哈希
 */
fun String.sha256(): String {
    return Utils.Sha256(this)
}

/**
 * 字符串扩展：Base64编码
 */
fun String.base64Encode(): String {
    return Utils.Base64Encode(this)
}

/**
 * 字符串扩展：URL安全的Base64编码
 */
fun String.base64UrlEncode(): String {
    return Utils.Base64UrlEncode(this)
}

/**
 * Long扩展：转换为特定进制的字符串
 */
fun Long.toString(radix: Int): String {
    return java.lang.Long.toString(this, radix)
}