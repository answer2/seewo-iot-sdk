package dev.answer.seewocampus.com.cvte.ciot.core.mqtt

import com.cvte.ciot.core.callback.NativeCallback.*
import com.cvte.ciot.core.model.DeviceAuth
import com.cvte.ciot.core.model.IoTConfig
import com.cvte.ciot.core.model.IotError
import com.cvte.ciot.core.model.tsl.*
import com.cvte.ciot.core.mqtt.MqttClientAdapter
import com.cvte.ciot.core.utils.JsonHelper
import com.cvte.ciot.core.utils.TopicParser
import com.cvte.ciot.core.utils.Utils
import com.google.gson.Gson

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import org.eclipse.paho.client.mqttv3.MqttCallback


/**
 * @author AnswerDev
 * @date 2025/12/7 10:48
 * @description IoTMqttImpl
 */

class IoTMqttImpl(
    private val config: IoTConfig, // val/var 关键字直接声明为类的属性
) {

    private val poolDown: ExecutorService = Executors.newFixedThreadPool(4)
    private val poolUp: ExecutorService = Executors.newFixedThreadPool(4)
    private val sessionId: String = Utils.GetRandomID()
    private val envType = AtomicInteger(0)

    // --- 回调存储 ---
    private var propertySetCallback: OnTslRequest? = null
    private var propertyGetCallback: OnTslRequest? = null
    private var serviceCallback: OnTslRequest? = null
    private var configCallback: OnTslConfig? = null
    private var upgradeCallback: OnTslUpgrade? = null

    private val customTopicCbs: MutableMap<String, OnCustomTopic> = ConcurrentHashMap()

    // --- 同步调用机制 ---
    private val syncResponseMap: MutableMap<String, CompletableFuture<TslResponse>> = ConcurrentHashMap()
    // 设置同步调用超时时间
    private val SYNC_TIMEOUT_SEC: Long = 10

    // MQTT 客户端 (在 init 块中初始化)
    private val client: MqttClient

    init {
        // 初始化 MQTT 客户端 (使用 Paho 封装)
        var brokerUri = config.host
        if (!brokerUri.contains("://")) {
            brokerUri = "tcp://$brokerUri" // 默认添加协议头
        }
        this.client = MqttClient(brokerUri, config.device.deviceId)

        // 使用可配置实现
        val configurableCallback = ConfigurableMqttCallback(
            onConnectionLost = { cause ->
                println("连接中断，尝试重连...")
                onConnectStateCallback(false)
            },
            onMessageArrived = { topic, message ->
                onConnectStateCallback(true)
                //println("处理消息: $topic")

                onMessageCallback(topic, String(message.payload))
            },
            onDeliveryComplete = { token ->
                println("消息 ${token?.messageId} 已成功发送")
            }
        )


        // 设置客户端回调
        //(this::onConnectStateCallback, this::onMessageCallback
        client.setCallback(configurableCallback)

        // 简单环境判断
        if (config.host.contains(".test.")) {
            envType.set(1)
        } else if (config.host.contains("iot.seewo.com")) {
            envType.set(2)
        }
    }

    fun shutdown() {
        client.disconnect()
        poolDown.shutdownNow()
        poolUp.shutdownNow()
    }

    // --- 连接与状态 ---

    fun isConnected(): Boolean {
        return client.isConnected
    }

    @Throws(MqttException::class)
    fun connect(connectionTimeoutSeconds: Int, maxRetryIntervalSeconds: Int) {
        // 1. 构建认证信息
        val userName = config.device.deviceId + "_" + sessionId
        val password = Utils.HmacMd5(config.device.deviceSecret, config.device.deviceId)

        val options = MqttConnectOptions()
        options.userName = userName
        options.password = password.toCharArray()

        // 2. 开启自动重连
        options.isAutomaticReconnect = true

        // 3. 设置最大重连间隔 (单位: 秒)
        options.maxReconnectDelay = maxRetryIntervalSeconds

        // 4. 设置连接超时时间 (单位: 秒)
        options.connectionTimeout = connectionTimeoutSeconds

        // 5. 设置会话心跳 (KeepAlive)
        options.keepAliveInterval = 30

        // 6. CleanSession 设置
        options.isCleanSession = false

        // 执行连接
        println("Connecting to broker...")
        client.connect(options)
        println("Connected successfully.")
    }

    // --- MQTT 客户端回调处理 ---

    private fun onConnectStateCallback(connected: Boolean) {
        // 通知上层
        config.onConnectState?.onConnectState(connected)

        if (connected) {
            val pk = config.device.productKey
            val did = config.device.deviceId

            // 订阅 TSL 基础 Topic
            client.subscribe("/sys/$pk/$did/rpc/request/+", 1)
            client.subscribe("/sys/$pk/$did/up/response/+", 1)

            // 重新订阅自定义 Topic
            customTopicCbs.keys.forEach { topic -> client.subscribe(topic, 1) }
        }
    }

    private fun onMessageCallback(topic: String, payload: String) {

        // 投递到线程池，避免阻塞 MQTT 线程
        poolDown.submit {
            try {
                when {
                    topic.contains("/rpc/request/") -> processDownRequest(topic, payload)
                    topic.contains("/up/response/") -> processUpResponse(topic, payload)
                    else -> processCustomTopic(topic, payload)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 消息路由逻辑 ---

    private fun processDownRequest(topic: String, payload: String) {
        val basic = TslBasic()
        val req = TslRequest()
        var res = TslResponse()

        res.code = TslDefines.CODE_SUCCESS_STRING
        res.message = "success"

        if (!JsonHelper.deserializeTslReq(payload, basic, req)) {
            res = createErrorResponse(IotError.IOT_ERROR_DESERIALIZE_FAIL, "Request deserialization failed")
        } else {
            basic.deviceId = TopicParser.getDeviceIdFromTopic(topic)

            var handled = false
            when {
                TopicParser.isTheMethod(TslDefines.DOWN_METHOD_PROPERTY_SET, req.method) -> {
                    propertySetCallback?.onRequest( basic, req)
                    handled = true
                }
                TopicParser.isTheMethod(TslDefines.DOWN_METHOD_PROPERTY_GET, req.method) -> {
                    propertyGetCallback?.onRequest(basic, req)
                    handled = true
                }
                TopicParser.isTheMethod(TslDefines.DOWN_SERVICE_CONFIG_PUSH, req.method) -> {
                    handleServiceConfigPush(topic, basic, req, res)
                    handled = true
                }
                TopicParser.isTheMethod(TslDefines.DOWN_SERVICE_UPGRADE, req.method) -> {
                    handleServiceUpgrade(topic, basic, req, res)
                    handled = true
                }
                req.method.startsWith(TslDefines.DOWN_METHOD_SERVICE) && serviceCallback != null -> {
                    serviceCallback!!.onRequest( basic, req)
                    handled = true
                }
            }

            if (!handled && res.code == TslDefines.CODE_SUCCESS_STRING) {
                res = createErrorResponse(IotError.IOT_ERROR_METHOD_NOT_SUPPORT, "Method not supported or callback not set")
            }
        }

        // 发送响应
        val messageId = TopicParser.getMessageIdFromTopic(topic)
        val deviceId = if (basic.deviceId.isNullOrEmpty()) config.device.deviceId else basic.deviceId

        val resTopic = TopicParser.getDownResTopic(config.device.productKey, deviceId, messageId)
        val resPayload = JsonHelper.serializeTslRes(basic, res)
        publishRaw(resTopic, resPayload)
    }

    private fun processUpResponse(topic: String, payload: String) {
        val basic = TslBasic()
        val res = TslResponse()

        // 这里需要先解析出 traceId
        if (!JsonHelper.deserializeTslReq(payload, basic, TslRequest())) {
            return
        }

        val future = syncResponseMap.remove(basic.traceId)
        if (future != null) {
            if (JsonHelper.deserializeTslRes(payload, res)) {
                future.complete(res)
            } else {
                future.complete(createErrorResponse(IotError.IOT_ERROR_DESERIALIZE_FAIL, "Response deserialization failed"))
            }
        }
    }

    private fun processCustomTopic(topic: String, payload: String) {
        val callback = customTopicCbs[topic]
        callback?.onTopicMessage(topic, payload)
    }

    // --- 内部服务处理 ---

    private fun handleServiceConfigPush(topic: String, basic: TslBasic, req: TslRequest, res: TslResponse) {
        if (configCallback != null) {
            val items = ArrayList<TslConfigItem>()
            if (JsonHelper.deserializeTslConfig(req.params, items)) {
                configCallback!!.onConfig(Gson().toJson(items))
                res.code = TslDefines.CODE_SUCCESS_STRING
                res.message = "success"
            } else {
                res.code = IotError.IOT_ERROR_DESERIALIZE_FAIL.getCodeStr()
                res.message = "Config params deserialize failed"
            }
        } else {
            res.code = IotError.IOT_ERROR_METHOD_NOT_SUPPORT.getCodeStr()
            res.message = "Config callback not set"
        }
    }

    private fun handleServiceUpgrade(topic: String, basic: TslBasic, req: TslRequest, res: TslResponse) {
        if (upgradeCallback != null) {
            val update = TslUpdate()
            if (JsonHelper.deserializeTslUpdate(req.params, update)) {
                upgradeCallback!!.onUpgrade(update.versionCode)
                res.code = TslDefines.CODE_SUCCESS_STRING
                res.message = "success"
            } else {
                res.code = IotError.IOT_ERROR_DESERIALIZE_FAIL.getCodeStr()
                res.message = "Upgrade params deserialize failed"
            }
        } else {
            res.code = IotError.IOT_ERROR_METHOD_NOT_SUPPORT.getCodeStr()
            res.message = "Upgrade callback not set"
        }
    }

    // --- 辅助函数 ---

    private fun createErrorResponse(error: IotError, message: String): TslResponse {
        val res = TslResponse()
        res.code = error.getCodeStr()
        res.message = message
        res.data = "null"
        return res
    }

    private fun createTimeoutResponse(basic: TslBasic): TslResponse {
        return createErrorResponse(IotError.IOT_ERROR_TIMEOUT, "Request timeout: ${basic.traceId}")
    }


    /**
     * 发布消息。
     * * 逻辑参照 C++ 实现：
     * 1. 检查连接状态。
     * 2. 检查 QoS 参数是否有效（0, 1, 2）。
     * 3. 使用 try-catch 块处理 Paho MqttException。
     * 4. Paho Java 库通过 MqttMessage.setRetained(boolean) 设置保留标志。
     *
     * @param topic 要发布的主题。
     * @param payload 消息内容。
     * @param qos 质量等级 (0, 1, 2)。
     * @param isRetain 是否保留消息。
     * @return 成功返回 true，失败返回 false。
     */
    fun publishMessage(topic: String, payload: String, qos: Int, isRetain: Boolean): Boolean {
        // 1. 检查连接状态
        if (!client.isConnected) {
            // LogTrace(__FILE__, __LINE__, "PublishMessage", 1, "Not connected, cannot publish");
            System.err.println("Publish failed for topic '$topic': Client is not connected.")
            // 注意：这里我们使用 System.err.println 替代了 C++ 的 LogTrace
            return false
        }

        // 2. 验证 QoS 参数
        if (qos < 0 || qos > 2) {
            // C++: if (_exceptCallback) { _exceptCallback(topic, -9, "Bad QoS value"); }
            System.err.println("Publish failed for topic '$topic': Bad QoS value ($qos). Must be 0, 1, or 2.")

            // 假设有一个内部方法来处理异常回调，以模拟 C++ 的 _exceptCallback
            // handleExceptionCallback(topic, -9, "Bad QoS value")
            return false
        }

        return try {
            // 3. 创建消息
            val message = org.eclipse.paho.client.mqttv3.MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                this.qos = qos
                this.isRetained = isRetain // 设置 Retain 标志
            }

            // 4. 发布消息
            // Paho publish 方法在同步调用时会等待消息发送完成 (QoS > 0)，
            // 失败则抛出 MqttException。
            client.publish(topic, message)

            // 如果没有异常抛出，则发布成功
            true

        } catch (e: org.eclipse.paho.client.mqttv3.MqttException) {
            // 捕获 MQTT 异常 (对应 C++ 的 mqtt::exception)
            // handleMQTTException(e, "PublishMessage");
            System.err.println("MQTT Exception during publish to '$topic'. Reason Code: ${e.reasonCode}, Message: ${e.message}")
            e.printStackTrace()
            false

        } catch (e: Exception) {
            // 捕获其他标准异常 (对应 C++ 的 std::exception)
            // LogTrace(__FILE__, __LINE__, "PublishMessage", 1, "Publish failed: " + std::string(e.what()));
            System.err.println("Unexpected Exception during publish to '$topic': ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 封装 Paho 的 publish 方法，处理异常并返回布尔值。
     */
    private fun publishRaw(topic: String, payload: String): Boolean {
        // 假设 QoS 为 1 (至少一次)
        val qos = 1

        // Paho API 要求 payload 是字节数组
        val message = MqttMessage(payload.toByteArray(Charsets.UTF_8))
        message.qos = qos

        // 如果客户端未连接，Paho publish 也会抛出 MqttException
        if (!client.isConnected) {
            // 可以在这里打印日志或抛出自定义异常
            System.err.println("Error: Client is not connected. Topic: $topic")
            return false
        }

        return try {
            // Paho publish 方法在同步调用时会等待消息发送成功（QoS > 0），
            // 如果失败则抛出 MqttException。
            client.publish(topic, message)

            // 如果执行到这里没有抛出异常，则认为发送成功
            true
        } catch (e: MqttException) {
            // 捕获 MQTT 相关的异常，例如连接丢失、客户端关闭等
            System.err.println("Error publishing to topic $topic. Reason: ${e.reasonCode}, Message: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            // 捕获其他运行时异常
            System.err.println("Unexpected error during publish to $topic: ${e.message}")
            e.printStackTrace()
            false
        }
    }


    private fun publishTslReq(
        basic: TslBasic,
        req: TslRequest,
        methodPrefix: String,
        future: CompletableFuture<TslResponse>?,
    ): Boolean {
        if (!isConnected()) {
            future?.complete(
                createErrorResponse(
                    IotError.IOT_ERROR_IOT_ISNOT_CONNECT,
                    "IoT platform is not connected"
                )
            )
            return false
        }

        if (!req.method.startsWith(methodPrefix)) {
            req.method = methodPrefix + req.method
        }

        if (future != null) {
            syncResponseMap[basic.traceId] = future
        }

        val deviceId = if (basic.deviceId.isNullOrEmpty()) config.device.deviceId else basic.deviceId
        val topic = TopicParser.getUpReqTopic(config.device.productKey, deviceId)

        val payload = JsonHelper.serializeTslUp(basic, req)
        if (payload.isNullOrEmpty()) {
            if (future != null) {
                syncResponseMap.remove(basic.traceId)
                future.complete(createErrorResponse(IotError.IOT_ERROR_SERIALIZE_FAIL, "Serialize fail"))
            }
            return false
        }

        return publishRaw(topic, payload)
    }

    // --- Public API 实现 ---

    fun postDeviceVersion(version: String) {
        val basic = TslBasic().apply {
            traceId = Utils.GetRandomID()
            deviceId = config.device.deviceId
        }
        val req = TslRequest().apply {
            method = "basic.post"
            params = "{\"version\":\"$version\"}"
        }
        publishTslReq(basic, req, TslDefines.UP_METHOD_BASIC_POST, null)
    }

    fun postDeviceName(deviceName: String) {
        val basic = TslBasic().apply {
            traceId = Utils.GetRandomID()
            deviceId = config.device.deviceId
        }
        val req = TslRequest().apply {
            method = "basic.post"
            params = "{\"deviceName\":\"$deviceName\"}"
        }
        publishTslReq(basic, req, TslDefines.UP_METHOD_BASIC_POST, null)
    }

    fun postConfigVersion(configKeys: List<TslConfigKey>) {
        val basic = TslBasic().apply {
            traceId = Utils.GetRandomID()
            deviceId = config.device.deviceId
        }
        val req = TslRequest().apply {
            method = "config.post"
            // 简单序列化 configKeys 为 JSON 字符串 (List -> JsonArray)
            params = Gson().toJson(configKeys)
        }
        publishTslReq(basic, req, TslDefines.UP_METHOD_CONFIG_POST, null)
    }

    fun setConfigCallback(callback: OnTslConfig?) { this.configCallback = callback }
    fun setUpgradeCallback(callback: OnTslUpgrade?) { this.upgradeCallback = callback }
    fun setPropertySetCallback(callback: OnTslRequest?) { this.propertySetCallback = callback }
    fun setPropertyGetCallback(callback: OnTslRequest?) { this.propertyGetCallback = callback }
    fun setServiceCallback(callback: OnTslRequest?) { this.serviceCallback = callback }

    fun subscribeCustom(topic: String, callback: OnCustomTopic): Boolean {
        customTopicCbs[topic] = callback
        return if (isConnected()) {
            client.subscribe(topic, 2)
             false
        } else {
            true
        }
    }

    fun propertyPost(basic: TslBasic, req: TslRequest): Boolean {
        return publishTslReq(basic, req, TslDefines.UP_METHOD_PROPERTY_POST, null)
    }

    fun eventPost(basic: TslBasic, req: TslRequest): Boolean {
        return publishTslReq(basic, req, TslDefines.UP_METHOD_EVENT_POST, null)
    }

    fun publishCustom(topic: String, traceId: String, params: String): Boolean {
        return publishRaw(topic, params)
    }

    // --- 同步调用实现 ---

    fun propertyGet(basic: TslBasic, req: TslRequest, res: TslResponse): Boolean {
        return try {
            poolUp.submit(Callable {
                val future = CompletableFuture<TslResponse>()
                basic.traceId = Utils.GetRandomID()

                if (!publishTslReq(basic, req, TslDefines.UP_METHOD_PROPERTY_GET, future)) {
                    val err = createErrorResponse(IotError.IOT_ERROR_PUBLISH_FAIL, "Failed to publish")
                    res.code = err.code
                    res.message = err.message
                    return@Callable false
                }

                try {
                    val result = future.get(SYNC_TIMEOUT_SEC, TimeUnit.SECONDS)
                    res.code = result.code
                    res.message = result.message
                    res.data = result.data
                    result.code == TslDefines.CODE_SUCCESS_STRING
                } catch (e: TimeoutException) {
                    syncResponseMap.remove(basic.traceId)
                    val err = createTimeoutResponse(basic)
                    res.code = err.code
                    res.message = err.message
                    false
                } catch (e: Exception) {
                    val err = createErrorResponse(IotError.IOT_ERROR_MQTT_EXCEPT, e.message ?: "Unknown MQTT error")
                    res.code = err.code
                    res.message = err.message
                    false
                }
            }).get()
        } catch (e: Exception) {
            false
        }
    }

    fun serviceCall(basic: TslBasic, req: TslRequest, res: TslResponse): Boolean {
        return try {
            poolUp.submit(Callable {
                val future = CompletableFuture<TslResponse>()
                basic.traceId = Utils.GetRandomID()

                if (!publishTslReq(basic, req, TslDefines.UP_METHOD_SERVICE, future)) {
                    val err = createErrorResponse(IotError.IOT_ERROR_PUBLISH_FAIL, "Failed to publish")
                    res.code = err.code
                    res.message = err.message
                    return@Callable false
                }

                try {
                    val result = future.get(SYNC_TIMEOUT_SEC, TimeUnit.SECONDS)
                    res.code = result.code
                    res.message = result.message
                    res.data = result.data
                    result.code == TslDefines.CODE_SUCCESS_STRING
                } catch (e: TimeoutException) {
                    syncResponseMap.remove(basic.traceId)
                    val err = createTimeoutResponse(basic)
                    res.code = err.code
                    res.message = err.message
                    false
                } catch (e: Exception) {
                    val err = createErrorResponse(IotError.IOT_ERROR_MQTT_EXCEPT, e.message ?: "Unknown MQTT error")
                    res.code = err.code
                    res.message = err.message
                    false
                }
            }).get()
        } catch (e: Exception) {
            false
        }
    }

    // --- 网关类设备 ---

    fun onlineSubDevice(basic: TslBasic, dev: DeviceAuth): Boolean {
        val req = TslRequest().apply {
            method = "sub.connect"
            params = Gson().toJson(dev)
        }
        return publishTslReq(basic, req, TslDefines.UP_METHOD_SUB_CONNECT, null)
    }

    fun offlineSubDevice(basic: TslBasic, dev: DeviceAuth): Boolean {
        val req = TslRequest().apply {
            method = "sub.disconnect"
            params = Gson().toJson(dev)
        }
        return publishTslReq(basic, req, TslDefines.UP_METHOD_SUB_DISCONNECT, null)
    }

    fun addSubDevice(basic: TslBasic, dev: DeviceAuth): Boolean {
        val req = TslRequest().apply {
            method = "sub.add"
            params = Gson().toJson(dev)
        }
        return publishTslReq(basic, req, TslDefines.UP_METHOD_SUB_ADD, null)
    }

    fun delSubDevice(basic: TslBasic, dev: DeviceAuth): Boolean {
        val req = TslRequest().apply {
            method = "sub.del"
            params = Gson().toJson(dev)
        }
        return publishTslReq(basic, req, TslDefines.UP_METHOD_SUB_DEL, null)
    }

    fun getSubDevice(basic: TslBasic, devs: List<DeviceAuth>): Boolean {
        // GetSubDevice 是同步调用，这里复用 serviceCall 的逻辑来等待响应
        val req = TslRequest().apply {
            method = "sub.get"
            params = "{}"
        }
        val res = TslResponse()
        if (serviceCall(basic, req, res)) {
            // Java 逻辑中只返回了 true，但注释提到需要从 res.data 中解析 List<DeviceAuth>
            // 在 Kotlin 转换中保持原样，只返回 true
            return true
        }
        return false
    }
}





/**
 * 可配置的 MQTT 回调类，支持自定义处理函数
 */
class ConfigurableMqttCallback(
    private val onConnectionLost: (Throwable?) -> Unit = { cause ->
        println("MQTT 连接丢失: ${cause?.message}")
    },
    private val onMessageArrived: (String, MqttMessage) -> Unit = { topic, message ->
        {
            //println("收到消息 [$topic]: ${message.payload}")
        }
    },
    private val onDeliveryComplete: (IMqttDeliveryToken?) -> Unit = { token ->
        println("消息传递完成: ID=${token?.messageId}")
    }
) : MqttCallback {

    override fun connectionLost(cause: Throwable?) {
        onConnectionLost(cause)
    }

    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {
        onMessageArrived(topic, message)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        onDeliveryComplete(token)
    }
}