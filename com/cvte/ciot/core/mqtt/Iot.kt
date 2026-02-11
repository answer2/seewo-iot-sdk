package com.cvte.ciot.core.mqtt

import com.cvte.ciot.core.callback.IotLogCallback
import com.cvte.ciot.core.callback.OnTslDownCallback
import com.cvte.ciot.core.callback.NativeCallback
import com.cvte.ciot.core.callback.OnConnectState
import com.cvte.ciot.core.model.DeviceAuth
import com.cvte.ciot.core.model.IoTConfig
import com.cvte.ciot.core.model.SSLOption
import com.cvte.ciot.core.model.tsl.TslBasic
import com.cvte.ciot.core.model.tsl.TslDefines
import com.cvte.ciot.core.model.tsl.TslRequest
import com.cvte.ciot.core.model.tsl.TslResponse
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

class Iot {
    companion object {
        private const val TAG = "Iot"
        private var logCallback: IotLogCallback? = null
        private var enableLog = true

        private fun logDebug(message: String) {
            if (enableLog) {
                println(TAG + message)
                logCallback?.onPrintLog(1, TAG, message)
            }
        }

        private fun logError(message: String) {
            println(TAG + message)
            logCallback?.onPrintLog(4, TAG, message)
        }
    }

    @Volatile
     var isInitConnectComplete: Boolean = false
         get() = false

    private var mqttClient: IoTMqtt? = null
    private val lock = ReentrantReadWriteLock()
    private val readLock = lock.readLock()
    private val writeLock = lock.writeLock()

    init {
    }

    fun initMqtt(
        host: String?,
        port: String?,
        deviceAuth: DeviceAuth,
        sslOption: SSLOption?,
        connectCallback: OnConnectState?
    ): Boolean {
        writeLock.lock()
        try {
            logDebug("initMqtt called with host: $host, port: $port")

            // 构建IoTConfig
            val config = IoTConfig().apply {
                this.host = "$host:$port"
                this.device = deviceAuth
                this.onConnectState = OnConnectState { isConnected ->
                    connectCallback?.onConnectState(isConnected)
                }
            }

            println(config.device.deviceId)

            // 创建MQTT客户端
            mqttClient = IoTMqtt(config)

            logDebug("initMqtt result is ${if (mqttClient != null) "success" else "failed"}")
            return mqttClient != null
        } catch (e: Exception) {
            logError("initMqtt error: ${e.message}")
            return false
        } finally {
            writeLock.unlock()
        }
    }

    fun checkMqttPointEnable(): Boolean {
        readLock.lock()
        try {
            return mqttClient != null
        } finally {
            readLock.unlock()
        }
    }

    fun connectMqtt() {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("connectMqtt called")
                it.Connect()
                initConnectComplete()
            }


        } catch (e: Exception) {
            logError("connectMqtt error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    fun connectMqtt(minRetryInterval: Int, maxRetryInterval: Int) {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("connectMqtt called with retry params")
                it.Connect(minRetryInterval, maxRetryInterval)
                initConnectComplete()
            }
        } catch (e: Exception) {
            logError("connectMqtt error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    fun isMqttConnected(): Boolean {
        readLock.lock()
        try {
            return mqttClient?.IsConnected() ?: false
        } finally {
            readLock.unlock()
        }
    }

    fun eventPost(basic: TslBasic, req: TslRequest): Boolean {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("eventPost called")
                return it.EventPost(basic, req)
            }
            return false
        } catch (e: Exception) {
            logError("eventPost error: ${e.message}")
            return false
        } finally {
            readLock.unlock()
        }
    }

    fun propertyGet(basic: TslBasic, req: TslRequest): TslResponse {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("propertyGet called")
                val response = TslResponse()
                val success = it.PropertyGet(basic, req, response)
                if (!success) {
                    response.code = "500"
                    response.message = "Property get failed"
                }
                return response
            }

            return TslResponse().apply {
                code = "500"
                message = "MQTT client not initialized"
            }
        } catch (e: Exception) {
            logError("propertyGet error: ${e.message}")
            return TslResponse().apply {
                code = "500"
                message = "Exception: ${e.message}"
            }
        } finally {
            readLock.unlock()
        }
    }

    fun propertyPost(basic: TslBasic, req: TslRequest): Boolean {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("propertyPost called")
                return it.PropertyPost(basic, req)
            }
            return false
        } catch (e: Exception) {
            logError("propertyPost error: ${e.message}")
            return false
        } finally {
            readLock.unlock()
        }
    }

    fun publishCustom(topic: String, traceId: String, params: String): Boolean {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("publishCustom called for topic: $topic")
                return it.PublishCustom(topic, traceId, params)
            }
            return false
        } catch (e: Exception) {
            logError("publishCustom error: ${e.message}")
            return false
        } finally {
            readLock.unlock()
        }
    }

    fun releaseMqtt() {
        writeLock.lock()
        try {
            logDebug("releaseMqtt called")
            mqttClient?.shutdown()
            mqttClient = null
            releaseMqttPtr()
        } catch (e: Exception) {
            logError("releaseMqtt error: ${e.message}")
        } finally {
            writeLock.unlock()
        }
    }

    fun serviceCall(basic: TslBasic, req: TslRequest): TslResponse {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("serviceCall called")
                val response = TslResponse()
                val success = it.ServiceCall(basic, req, response)
                if (!success) {
                    response.code = "500"
                    response.message = "Service call failed"
                }
                return response
            }

            return TslResponse().apply {
                code = "500"
                message = "MQTT client not initialized"
            }
        } catch (e: Exception) {
            logError("serviceCall error: ${e.message}")
            return TslResponse().apply {
                code = "500"
                message = "Exception: ${e.message}"
            }
        } finally {
            readLock.unlock()
        }
    }

    fun setLog(enable: Boolean, callback: IotLogCallback?) {
        writeLock.lock()
        try {
            enableLog = enable
            logCallback = callback
            logDebug("setLog called with enable: $enable")
        } finally {
            writeLock.unlock()
        }
    }

    fun setPropertyGetCallback(callback: OnTslDownCallback?) {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("setPropertyGetCallback called")
                it.SetPropertyGetCallback(NativeCallback.OnTslRequest { basic, req ->
                    callback?.onRequest(req.params, req.method, basic.deviceId, TslDefines.CODE_SUCCESS_STRING)
                })
            }
        } catch (e: Exception) {
            logError("setPropertyGetCallback error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    fun setPropertySetCallback(callback: OnTslDownCallback?) {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("setPropertySetCallback called")
                it.SetPropertySetCallback(NativeCallback.OnTslRequest { basic, req ->
                    callback?.onRequest(req.params, req.method, basic.deviceId, TslDefines.CODE_SUCCESS_STRING)
                })
            }
        } catch (e: Exception) {
            logError("setPropertySetCallback error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    fun setServiceCallback(callback: OnTslDownCallback?) {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("setServiceCallback called")
                it.SetServiceCallback(NativeCallback.OnTslRequest { basic, req ->
                    callback?.onRequest(req.params, req.method, basic.deviceId, TslDefines.CODE_SUCCESS_STRING)
                })
            }
        } catch (e: Exception) {
            logError("setServiceCallback error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    fun subscribeCustom(topic: String, callback: NativeCallback.OnCustomTopic?): Boolean {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("subscribeCustom called for topic: $topic")
                return it.SubscribeCustom(topic, NativeCallback.OnCustomTopic { topicName, payload ->
                    callback?.onTopicMessage(topicName, payload)
                })
            }
            return false
        } catch (e: Exception) {
            logError("subscribeCustom error: ${e.message}")
            return false
        } finally {
            readLock.unlock()
        }
    }

    // 兼容原有JNI接口的方法
    fun releaseMqttPtr() {
        writeLock.lock()
        try {
            mqttClient = null
        } finally {
            writeLock.unlock()
        }
    }

    fun isRelease(): Boolean {
        readLock.lock()
        try {
            return mqttClient == null
        } finally {
            readLock.unlock()
        }
    }

    fun initConnectComplete() {
        writeLock.lock()
        try {
            isInitConnectComplete = true
        } finally {
            writeLock.unlock()
        }
    }

    fun resetConnectInitState() {
        writeLock.lock()
        try {
            isInitConnectComplete = false
        } finally {
            writeLock.unlock()
        }
    }

    // 新增的便捷方法

    /**
     * 上报设备版本
     */
    fun postDeviceVersion(version: String) {
        readLock.lock()
        try {
            mqttClient?.PostDeviceVersion(version)
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 上报设备名称
     */
    fun postDeviceName(deviceName: String) {
        readLock.lock()
        try {
            mqttClient?.PostDeviceName(deviceName)
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 设置配置回调
     */
    fun setConfigCallback(callback: NativeCallback.OnTslConfig?) {
        readLock.lock()
        try {
            mqttClient?.SetConfigCallback(callback)
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 设置OTA升级回调
     */
    fun setUpgradeCallback(callback: NativeCallback.OnTslUpgrade?) {
        readLock.lock()
        try {
            mqttClient?.SetUpgradeCallback(callback)
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 直接发布MQTT消息
     */
    fun publishMessage(topic: String, payload: String, qos: Int, isRetain: Boolean): Boolean {
        readLock.lock()
        try {
            return mqttClient?.PublishMessage(topic, payload, qos, isRetain) ?: false
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 获取底层MQTT客户端（用于高级操作）
     */
    fun getMqttClient(): IoTMqtt? {
        readLock.lock()
        try {
            return mqttClient
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 优雅关闭所有资源
     */
    fun shutdown() {
        writeLock.lock()
        try {
            logDebug("shutdown called")
            mqttClient?.shutdown()
            mqttClient = null
            isInitConnectComplete = false
            logCallback = null
        } catch (e: Exception) {
            logError("shutdown error: ${e.message}")
        } finally {
            writeLock.unlock()
        }
    }

    /**
     * 重新连接MQTT
     */
    fun reconnect() {
        readLock.lock()
        try {
            mqttClient?.let {
                logDebug("Reconnecting MQTT...")
                // 如果已有连接逻辑，可以在这里实现
            }
        } catch (e: Exception) {
            logError("reconnect error: ${e.message}")
        } finally {
            readLock.unlock()
        }
    }

    /**
     * 检查MQTT客户端状态
     */
    fun getStatus(): Map<String, Any> {
        readLock.lock()
        try {
            return mapOf(
                "initialized" to (mqttClient != null),
                "connected" to (mqttClient?.IsConnected() ?: false),
                "initComplete" to isInitConnectComplete,
                "logEnabled" to enableLog
            )
        } finally {
            readLock.unlock()
        }
    }
    // 添加这个方法供 Java 代码调用
    fun getReadLock(): Lock {
        return readLock
    }
    // 添加这个方法供 Java 代码调用
    fun getWriteLock(): Lock {
        return writeLock
    }

}