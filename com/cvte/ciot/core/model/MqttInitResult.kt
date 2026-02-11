package com.cvte.ciot.core.model

import com.cvte.ciot.core.IIoTClient

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 04:17
 * @description MqttInitResult
 */
data class MqttInitResult(
    val protocol: IoTProtocol?,
    val auth: DeviceAuth?,
    val client: IIoTClient?
)