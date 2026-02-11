package com.cvte.ciot.core.utils

import com.cvte.ciot.core.model.IotError
import com.cvte.ciot.core.model.tsl.*
import com.google.gson.*
import com.google.gson.stream.JsonToken

/**
 * @author AnswerDev
 * @date 2025/12/7 12:29
 * @description JsonHelper
 */
object JsonHelper {
    private val gson = GsonBuilder().disableHtmlEscaping().create()
    private val parser = JsonParser()


    fun serializeTslRes(basic: TslBasic, res: TslResponse): String {
        val root = JsonObject()
        root.addProperty(TslDefines.TAG_VERSION, basic.version ?: "1.0")
        root.addProperty(TslDefines.TAG_TRACEID, basic.traceId)
        root.addProperty(TslDefines.TAG_CODE, res.code)
        root.addProperty(TslDefines.TAG_MESSAGE, res.message)

        // data 字段如果不是有效的 JSON 格式，则作为普通字符串处理
        if (res.data != null && res.data != "null") {
            try {
                root.add(TslDefines.TAG_DATA, parser.parse(res.data))
            } catch (e: JsonSyntaxException) {
                root.addProperty(TslDefines.TAG_DATA, res.data)
            }
        }
        return gson.toJson(root)
    }

    fun serializeTslUp(basic: TslBasic, req: TslRequest): String {
        val root = JsonObject()
        root.addProperty(TslDefines.TAG_VERSION, basic.version ?: "1.0")
        root.addProperty(TslDefines.TAG_TRACEID, basic.traceId)
        root.addProperty(TslDefines.TAG_METHOD, req.method)

        // params 字段必须是 JSON Object
        if (req.params != null && req.params.isNotEmpty()) {
            try {
                root.add(TslDefines.TAG_PARAMS, parser.parse(req.params))
            } catch (e: JsonSyntaxException) {
                // 如果 params 无法解析为 JSON，则不添加或记录错误
                return ""
            }
        } else {
            root.add(TslDefines.TAG_PARAMS, JsonObject())
        }
        return gson.toJson(root)
    }

    // --- 反序列化 ---

    fun deserializeTslReq(payload: String?, basic: TslBasic, req: TslRequest): Boolean {
        if (payload.isNullOrEmpty()) return false
        try {
            val root = parser.parse(payload).asJsonObject

            // 解析基础信息
            if (root.has(TslDefines.TAG_TRACEID))
                basic.traceId = root.get(TslDefines.TAG_TRACEID).asString
            if (root.has(TslDefines.TAG_VERSION))
                basic.version = root.get(TslDefines.TAG_VERSION).asString

            // 解析请求信息
            if (root.has(TslDefines.TAG_METHOD))
                req.method = root.get(TslDefines.TAG_METHOD).asString

            // 特殊处理 params：如果是对象，转为 JSON 字符串；如果是字符串，直接取
            if (root.has(TslDefines.TAG_PARAMS)) {
                val paramsElem = root.get(TslDefines.TAG_PARAMS)
                req.params = if (paramsElem.isJsonObject || paramsElem.isJsonArray) {
                    paramsElem.toString()
                } else {
                    paramsElem.asString
                }
            } else {
                req.params = "{}"
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun deserializeTslRes(payload: String?, res: TslResponse): Boolean {
        if (payload.isNullOrEmpty()) return false
        try {
            val root = parser.parse(payload).asJsonObject

            res.code = if (root.has(TslDefines.TAG_CODE))
                root.get(TslDefines.TAG_CODE).asString
            else
                IotError.IOT_ERROR_DESERIALIZE_FAIL.getCodeStr()

            if (root.has(TslDefines.TAG_MESSAGE))
                res.message = root.get(TslDefines.TAG_MESSAGE).asString

            if (root.has(TslDefines.TAG_DATA)) {
                val dataElem = root.get(TslDefines.TAG_DATA)
                res.data = if (dataElem.isJsonObject || dataElem.isJsonArray) {
                    dataElem.toString()
                } else {
                    dataElem.asString
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun deserializeTslUpdate(params: String?, update: TslUpdate): Boolean {
        if (params.isNullOrEmpty()) return false
        try {
            val obj = gson.fromJson(params, TslUpdate::class.java)
            if (obj != null) {
                update.versionCode = obj.versionCode
                update.policyTag = obj.policyTag
                update.appKey = obj.appKey
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun deserializeTslConfig(params: String?, items: MutableList<TslConfigItem>): Boolean {
        if (params.isNullOrEmpty()) return false
        try {
            val array = parser.parse(params).asJsonArray
            for (elem in array) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val item = TslConfigItem()
                    item.key = TslConfigKey()

                    if (obj.has("key")) item.key?.key = obj.get("key").asString
                    if (obj.has("version")) item.key?.version = obj.get("version").asInt

                    item.values = if (obj.has("values")) {
                        obj.get("values").toString()
                    } else {
                        "{}"
                    }
                    items.add(item)
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}