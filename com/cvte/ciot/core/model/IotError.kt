package com.cvte.ciot.core.model

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 12:36
 * @description IotError
 */
enum class IotError(val code: Int) {
    IOT_ERROR_SUCCESS(0),          ///成功
    IOT_ERROR_SERIALIZE_FAIL(1),       ///序列化参数失败
    IOT_ERROR_PUBLISH_FAIL(2),         ///发布失败，连接断开或发布失败
    IOT_ERROR_METHOD_NOT_MATCH(3),     ///方法不匹配，如获取属性填的其他方法等（废弃）
    IOT_ERROR_TIMEOUT(4),              ///超时
    IOT_ERROR_DESERIALIZE_FAIL(5),     ///反序列化失败
    IOT_ERROR_METHOD_NOT_SUPPORT(6),   ///方法不支持
    IOT_ERROR_TOPIC_ERROR(7),          ///TOPIC错误
    IOT_ERROR_IOT_ISNOT_CONNECT(8),    ///发布失败，没有连接上IoT平台
    IOT_ERROR_MQTT_EXCEPT(9),          ///MQTT客户端发生异常
    IOT_ERROR_REGISTER_FAILED(10),      ///动态注册失败
    IOT_ERROR_RECONNECT_TIMEOUT(11),    ///重连时间超过120s
    IOT_ERROR_START(170000);        ///SDK段起始错误码

    /**
     * 获取错误码字符串形式
     */
    fun getCodeStr(): String {
        return code.toString()
    }

    /**
     * 根据错误码获取对应的枚举值
     */
    companion object {
        fun fromCode(code: Int): IotError? {
            return values().find { it.code == code }
        }
    }
}