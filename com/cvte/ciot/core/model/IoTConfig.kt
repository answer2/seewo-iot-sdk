package com.cvte.ciot.core.model

import com.cvte.ciot.core.callback.OnConnectSSLStateCallback
import com.cvte.ciot.core.callback.OnConnectState

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 04:42
 * @description IoTConfig
 */
data class IoTConfig(
    var host: String = "",          // broker地址
    var version: String = "",       // 本地固件版本
    var deviceName: String = "",    // 设备名称，可为空
    var device: DeviceAuth = DeviceAuth(),         // 设备认证信息
    var onConnectState: OnConnectState = impl()   // 连接状态回调
) {
}


class impl : OnConnectState {
    override fun onConnectState(bool: Boolean) {
        TODO("Not yet implemented")
    }

}