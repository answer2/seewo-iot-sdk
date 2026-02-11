package dev.answer.seewocampus.com.cvte.ciot.core.mqtt

import com.cvte.ciot.core.model.*
import com.cvte.ciot.core.model.tsl.TslResponse
import com.cvte.ciot.core.utils.*
import com.google.gson.*
import dev.answer.iot.MessageContent
import dev.answer.iot.RequestParams
import dev.answer.seewocampus.iot.common.IotClientDepository



/**
 *
 * @author AnswerDev
 * @date 2025/12/7 10:22
 * @description IoTHttpImpl
 */

class IoTHttpImpl(uri: String) {
    private val _uri: String
    private var _envType: Int
    private val _identifiers: MutableMap<String, List<String>> = mutableMapOf()
    private var _socks5Proxy: String? = null
    private val gson = Gson()

    init {
        _uri = uri

        // 环境类型判断逻辑
        _envType = when {
            uri.contains(".test.seewo.com") || uri.contains(".gz.cvte.cn") -> {
                IotEnvType.IOT_ENV_TEST
            }
            uri.contains("iot-broker.seewo.com") -> {
                IotEnvType.IOT_ENV_PRO
            }
            else -> {
                IotEnvType.IOT_ENV_DEV
            }
        }

        println("IoTHttpImpl initialized with uri: $uri, envType: $_envType")
    }

    fun AddIdentifier(type: String, values: List<String>) {
        _identifiers[type] = values
        println("Added identifier: $type = $values")
    }

    fun SetSocks5Proxy(proxyUri: String) {
        _socks5Proxy = proxyUri
        println("Set socks5 proxy: $proxyUri")
    }

    fun PackContent(productKey: String): String {
        val root = JsonObject()

        // 设置产品密钥
        root.addProperty("productKey", productKey)

        // 设置版本号
        root.addProperty("version", "1.1.3")

        // 构建标识符数组
        val identifiers = JsonArray()

        _identifiers.forEach { (type, values) ->
            val item = JsonObject()
            item.addProperty("type", type)

            // 构建值数组
            val valuesArray = JsonArray()
            values.forEach { value ->
                valuesArray.add(value)
            }
            item.add("values", valuesArray)

            identifiers.add(item)
        }

        root.add("identities", identifiers)

        val content = gson.toJson(root)

        // 记录调试日志
        println("PackContent: $content")

        return content
    }

    fun ParseContent(content: String, device: DeviceAuth): Boolean {
        return try {
            val root = gson.fromJson(content, JsonObject::class.java)

            // 检查必需字段
            if (root == null ||
                !root.has("code") ||
                !root.has("data") ||
                !root.has("message")) {
                println("ParseContent: check value failed, json: $content")
                return false
            }

            val code = root.get("code").asString
            val message = root.get("message").asString

            // 检查返回码
            if (code != "000000") {
                println("ParseContent: check code failed, message: $message | code: $code")
                return false
            }

            val data = root.getAsJsonObject("data")

            // 检查数据字段
            if (data == null ||
                !data.has("productKey") ||
                !data.has("deviceId") ||
                !data.has("deviceSecret")) {
                println("ParseContent: check data failed, json: $content")
                return false
            }

            // 提取设备认证信息
            device.deviceId = data.get("deviceId").asString
            device.deviceSecret = data.get("deviceSecret").asString

            println("ParseContent success: deviceId=${device.deviceId}")
            true
        } catch (e: JsonSyntaxException) {
            println("ParseContent: JSON parse failed: ${e.message}")
            false
        } catch (e: Exception) {
            println("ParseContent: Unexpected error: ${e.message}")
            false
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun DeviceRegister(product: ProductAuth, device: DeviceAuth): Boolean {
        // 检查标识符是否为空
        if (_identifiers.isEmpty()) {
            println("DeviceRegister: identifiers is empty")
            return false
        }

        // 检查产品密钥是否为空
        if (product.productKey.isEmpty()) {
            println("DeviceRegister: productKey is empty")
            return false
        }

        // 生成跟踪ID
        val traceID = Utils.GetRandomID()

        // 准备HTTP请求
        val req = HttpReq(_uri)
        // 打包请求内容
        val content = PackContent(product.productKey)
        req.content =content
        // 设置请求头
        req.headers["Content-Type"] = "application/json;charset=UTF-8"

        // 生成签名
        val signature = Utils.calculateHmacMD5(product.productSecret, content)
        println(product.productSecret)
        req.headers["x-auth-sign"] = signature.uppercase();

        // 设置跟踪ID
        req.headers["x-auth-traceID"] = traceID

        // 设置时间戳
        val timestamp = Utils.GetTimestamp()
        req.headers["x-auth-ts"] = timestamp.toString()

        println(req.headers)

        // 准备日志
        val grail = GrailLog(
            traceId = traceID,
            content = content,
            productKey = product.productKey,
            method = "")

        // 发送HTTP请求
        var response = HttpRequest.PostEx(req);
        val httpSuccess = response.isSuccess

        val result: Boolean

        if (httpSuccess) {
            // 解析响应
            result = ParseContent(response.body, device)

            println(response.body)

            if (result) {
                grail.message = "register success, uri: $_uri"
                grail.deviceId = device.deviceId
            } else {
                grail.message = "ParseContent failed, uri: $_uri | resBody: ${response.body}"
            }
        } else {
            grail.message = "http.Post failed, uri: $_uri | resBody: ${response.body}"
            result = false
        }

        // 设置日志其他字段
        grail.code = if (result) "000000" else "170010"
        grail.method = "iot.register"
        grail.logType = IotLogType.IOT_LOG_UP

        // 记录调试日志
        val debugMsg = """
            DeviceRegister result: $result
            | traceID: ${grail.traceId}
            | deviceId: ${device.deviceId}
            | message: ${grail.message}
            | req.content: ${grail.content}
        """.trimIndent()

        println(debugMsg)

        return result
    }
}



// 使用示例
    suspend fun main() {
        // 创建IoTHttpImpl实例
        val iotHttp = IoTHttpImpl("")

        // 添加标识符
        iotHttp.AddIdentifier("uid", listOf(""))

        // 设置SOCKS5代理（可选）
//    iotHttp.SetSocks5Proxy("socks5://proxy.example.com:1080")

        // 产品认证信息
        val productAuth = ProductAuth(
            productKey = "",
            productSecret = ""
        )

        val deviceAuth = DeviceAuth()

        // 注册设备
        val success = iotHttp.DeviceRegister(productAuth, deviceAuth)

        if (success) {
            println("设备注册成功！")
            println(deviceAuth.productKey)
            println("设备ID: ${deviceAuth.deviceId}")
            println("设备密钥: ${deviceAuth.deviceSecret}")


            val myIotClient = IotClientDepository.Builder()
                .serialCode("")
                .product("", deviceAuth.deviceSecret)
                .customDeviceId(deviceAuth.deviceId)
                .listener(object : IotClientDepository.IotConnectEventListener {
                    override fun onConnectSuccess(deviceAuth: DeviceAuth) {
                        println("连接成功，云端 ID: ${deviceAuth.deviceId}")
                    }

                    override fun onConnectFail(errorCode: Int) {
                        println("连接失败: $errorCode")
                    }

                    override fun handleCommand(
                        params: String,
                        method: String,
                        topic: String,
                        traceId: String
                    ): TslResponse? {
                        val gson = Gson()
                        val apiRequest = gson.fromJson(params, RequestParams::class.java)
                        val messageContents =
                            gson.fromJson(apiRequest.msgContent, Array<MessageContent>::class.java).toList()
                        messageContents.forEach { content ->
                            val platform = content.platform;
                            val resourcesName = content.resourceName;
                            val extraData = content.extraData;

                            println("Platform : ${content.platform}, ResourcesName : ${content.resourceName}")
                        }


                        println("收到指令: $method")
                        return null
                    }
                })
                .buildAndConnect()

        } else {
            println("设备注册失败！")
        }
}
