package dev.answer.seewocampus.iot.common

import com.cvte.ciot.core.IIoTClient
import com.cvte.ciot.core.IoTSdkManager
import com.cvte.ciot.core.callback.IotLogCallback
import com.cvte.ciot.core.callback.OnConnectSSLStateCallback
import com.cvte.ciot.core.callback.OnConnectState
import com.cvte.ciot.core.callback.OnTslDownCallback
import com.cvte.ciot.core.model.*
import com.cvte.ciot.core.model.tsl.TslResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.Closeable
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * IoT 客户端管理仓库
 * 优化重点：Builder 模式、自定义 DeviceId、StateFlow 状态观察
 */
object IotClientDepository : Closeable {
    private const val TAG = "IotClientDepository"
    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val mutex = Mutex()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 内部状态
    private var iotOptions: IotOptions? = null
    private var ioTSdkManager: IoTSdkManager? = null
    private var iotClient: IIoTClient? = null
    private var eventListener: IotConnectEventListener? = null

    // 状态流
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionStateFlow: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * 配置选项类
     */
    data class IotOptions(
        val serialCode: String,
        val productKey: String,
        val productSecret: String,
        val deviceId: String?, // 自定义 DeviceId
        val brokerUrl: String,
        val registerUrl: String
    )

    // ==================== Builder 模式 ====================

    class Builder {
        private var serialCode: String = ""
        private var productKey: String = ""
        private var productSecret: String = ""
        private var customDeviceId: String? = null
        private var brokerUrl: String = ""
        private var registerUrl: String = ""  //自己輸入
        private var listener: IotConnectEventListener? = null

        fun serialCode(sn: String) = apply { this.serialCode = sn }
        fun product(key: String, secret: String) = apply {
            this.productKey = key
            this.productSecret = secret
        }
        fun customDeviceId(id: String?) = apply { this.customDeviceId = id }
        fun brokerUrl(url: String) = apply { this.brokerUrl = url }
        fun registerUrl(url: String) = apply { this.registerUrl = url }
        fun listener(l: IotConnectEventListener) = apply { this.listener = l }

        /**
         * 构建并自动启动连接
         */
        suspend fun buildAndConnect(): IIoTClient? {
            if (serialCode.isEmpty() || productKey.isEmpty()) {
                error("SerialCode and ProductKey must be provided.")
            }
            val options = IotOptions(serialCode, productKey, productSecret, customDeviceId, brokerUrl, registerUrl)
            return instance.initialize(options, listener)
        }

        private val instance = IotClientDepository
    }

    // ==================== 核心逻辑 ====================

    private suspend fun initialize(options: IotOptions, listener: IotConnectEventListener?): IIoTClient? = mutex.withLock {
        this.iotOptions = options
        this.eventListener = listener

        if (iotClient?.isConnected() == true) return iotClient

        try {
            _connectionState.value = ConnectionState.Connecting

            // 1. 组装标识符 (包含自定义 DeviceId)
            val identifiers = hashMapOf("uid" to arrayListOf(options.serialCode))
            options.deviceId?.let { identifiers["deviceId"] = arrayListOf(it) }

            // 2. 初始化 SDK Manager
            val deviceName = "${options.serialCode}_terminal"
            ioTSdkManager = IoTSdkManager.Singleton()
                .setBrokerUrl(options.brokerUrl)
                .setDeviceName(deviceName)
                .setProtocols(IoTProtocol.MQTT)
                .setRegisterConfig(RegisterConfig(options.registerUrl, options.productKey, options.productSecret,
                    options.deviceId!!, identifiers))
                .single()

            // 3. 注册设备
            if (ioTSdkManager?.register() != true) {
                updateError(-1, "Device registration failed")
                return null
            }

            // 4. 连接 MQTT
            return performConnect()
        } catch (e: Exception) {
            updateError(-2, e.message ?: "Unknown init error")
            return null
        }
    }

    private suspend fun performConnect(): IIoTClient? {
        val deferred = CompletableDeferred<Int>()

        // 连接回调
        val connectCallback = OnConnectState { isConnected->
            logInfo("Connect Callback: success=$isConnected")
            if (!deferred.isCompleted) deferred.complete(0)
        }

        // 初始化 MQTT
        val initResult = ioTSdkManager?.initMqtt("") { connected ->
            logDebug("MQTT Connection Status Changed: $connected")
        } ?: return null

        // 配置 TSL 回调
        setupTslCallbacks(initResult)
        iotClient = initResult.client

        // 执行异步连接并等待
        ioTSdkManager?.connectAsync()
        val result = deferred.await()

        return if (result == IotConnectError.kSuccess) {
            val auth = ioTSdkManager?.getDeviceAuth()
            auth?.let {
                _connectionState.value = ConnectionState.Connected(it)
                eventListener?.onConnectSuccess(it)
                iotClient?.subscribeCustom("thing.event.basic.post", { topic, payload ->
                    println("$topic  $payload")
                })
            }
            iotClient
        } else {
            updateError(result, "MQTT connection failed")
            null
        }
    }

    private fun setupTslCallbacks(result: MqttInitResult) {
        val client = result.client ?: return

        // 日志代理
        client.setLog(false) { tag, level, msg ->
            if (level.toInt() >= 3) logError("[$tag] $msg") else logInfo("[$tag] $msg")
        }

        // 指令下发统一处理
        val dispatcher = OnTslDownCallback { params, method, topic, traceId ->

            eventListener?.handleCommand(params, method, topic, traceId)
                ?: defaultResponse(traceId)
        }

        client.setServiceCallback(dispatcher)
        client.setPropertySetCallback(dispatcher)
        client.setPropertyGetCallback(dispatcher)
    }

    private fun defaultResponse(traceId: String): TslResponse {
        val json = JsonObject().apply { addProperty("traceId", traceId) }
        return TslResponse(TslResponse.SUCCEED, "ack", gson.toJson(json))
    }

    private fun updateError(code: Int, msg: String) {
        logError("Error $code: $msg")
        _connectionState.value = ConnectionState.Error(code, msg)
        eventListener?.onConnectFail(code)
    }

    // ==================== 外部 API ====================

    // ==================== 外部 API ====================

    /**
     * 同步获取客户端实例
     */
    @JvmStatic
    fun getClient(): IIoTClient? = if (isConnected()) iotClient else null

    /**
     * 同步检查连接状态
     */
    @JvmStatic
    fun isConnected(): Boolean = iotClient?.isConnected() ?: false

    /**
     * 断开连接（同步包装）
     */
    @JvmStatic
    fun disconnectSync() = runBlocking {
        disconnect()
    }

    // ... 原有的 disconnect 和 close 方法 ...

    fun disconnect() {
        ioScope.launch {
            mutex.withLock {
                ioTSdkManager?.disconnectAllClient()
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    override fun close() {
        disconnect()
        ioScope.cancel()
    }

    private fun logInfo(msg: String) = println("[INFO][$TAG] $msg")
    private fun logDebug(msg: String) = println("[DEBUG][$TAG] $msg")
    private fun logError(msg: String) = System.err.println("[ERROR][$TAG] $msg")

    // ==================== 辅助类 ====================

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceAuth: DeviceAuth) : ConnectionState()
        data class Error(val code: Int, val message: String) : ConnectionState()
    }

    interface IotConnectEventListener {
        fun onConnectSuccess(deviceAuth: DeviceAuth)
        fun onConnectFail(errorCode: Int)
        fun handleCommand(topic: String, method: String, params: String, traceId: String): TslResponse?
    }
}