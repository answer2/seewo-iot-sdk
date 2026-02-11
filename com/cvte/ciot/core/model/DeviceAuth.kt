package com.cvte.ciot.core.model

/**
 * @author AnswerDev
 * @date 2025/12/7 03:44
 * @description DeviceAuth
 */
data class DeviceAuth(
    var productKey: String = "",
    var deviceId: String = "",
    var deviceSecret: String = ""
) {
    override fun toString(): String {
        return "DeviceAuth{" +
                "productKey='$productKey', " +
                "deviceId='$deviceId', " +
                "deviceSecret='$deviceSecret'" +
                "}"
    }
}
