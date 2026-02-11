package com.cvte.ciot.core.model

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 10:23
 * @description GrailLog
 */

/**
 * 上报日志信息
 */

data class GrailLog(
    /**
     * 时间戳，[default]从1970年1月1日0:0:0:000到现在的微秒数(UTC时间)，不建议重新赋值
     * 默认会生成，没有特殊要求，不需要赋值
     */
    var timestamp: Long = System.currentTimeMillis() * 1000, // 微秒数

    /**
     * 模块名，[default]为空默认为iotSDK
     */
    var module: String = "iotSDK",

    /**
     * 设备信息
     */

    /**
     * 设备ID [default]为空则会默认赋值为网关/主设备ID
     */
    var deviceId: String = "",

    /**
     * 产品Key [default]为空则会默认赋值为网关/主设备产品Key
     */
    var productKey: String = "",

    /**
     * 内容信息
     */

    /**
     * [*必填*] 处理结果状态码，设备端错误码段应该为171000 - 179999段
     */
    var code: String = "171000",

    /**
     * 错误消息
     */
    var message: String = "",

    /**
     * [*必填*] 全链路日志ID
     */
    var traceId: String,

    /**
     * [*必填*] 方法
     */
    var method: String,

    /**
     * 内容，可以为 data、params或自定义
     */
    var content: String = "",

    /**
     * [*必填*] 消息类型（收到为下行，发送为上行）
     */
    var logType: Int = IotLogType.IOT_LOG_UP
) {


    /**
     * 验证必填字段是否已填写
     */
    fun validateRequiredFields(): Boolean {
        return code.isNotEmpty() && traceId.isNotEmpty() && method.isNotEmpty()
    }

    /**
     * 转换为Map，用于序列化或调试
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "timestamp" to timestamp,
            "module" to module,
            "deviceId" to deviceId,
            "productKey" to productKey,
            "code" to code,
            "message" to message,
            "traceId" to traceId,
            "method" to method,
            "content" to content,
            "logType" to logType
        )
    }

    /**
     * 生成摘要信息
     */
    fun getSummary(): String {
        return "GrailLog[method=$method, code=$code, traceId=$traceId, deviceId=$deviceId]"
    }

    companion object {
        /**
         * 创建成功的日志
         */
        fun createSuccessLog(
            traceId: String,
            method: String,
            deviceId: String = "",
            productKey: String = "",
            content: String = "",
            message: String = "success"
        ): GrailLog {
            return GrailLog(
                code = "000000",
                traceId = traceId,
                method = method,
                deviceId = deviceId,
                productKey = productKey,
                content = content,
                message = message
            )
        }

        /**
         * 创建失败的日志
         */
        fun createErrorLog(
            traceId: String,
            method: String,
            code: String,
            deviceId: String = "",
            productKey: String = "",
            content: String = "",
            message: String = "",
            logType: Int = IotLogType.IOT_LOG_UP
        ): GrailLog {
            // 验证错误码范围
            val errorCode = code.toIntOrNull()
            if (errorCode != null) {
                require(errorCode in 171000..179999) {
                    "设备端错误码应该在171000 - 179999范围内，当前为: $code"
                }
            }

            return GrailLog(
                code = code,
                traceId = traceId,
                method = method,
                deviceId = deviceId,
                productKey = productKey,
                content = content,
                message = message,
                logType = logType
            )
        }
    }
}

/**
 * 扩展函数示例
 */

/**
 * 设置设备信息
 */
fun GrailLog.withDeviceInfo(deviceId: String, productKey: String): GrailLog {
    return this.copy(deviceId = deviceId, productKey = productKey)
}
/**
 * 设置内容为JSON对象
 */
fun GrailLog.withJsonContent(json: String): GrailLog {
    return this.copy(content = json)
}