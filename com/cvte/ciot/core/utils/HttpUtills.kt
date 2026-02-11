package com.cvte.ciot.core.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 10:45
 * @description HttpUtils
 */

data class HttpReq(
    var uri: String,
    var content: String = "",
    var headers: MutableMap<String, String> = mutableMapOf(),
    var timeout: Long = 30000L, // 30秒超时
    var contentType: String = "application/json;charset=UTF-8"
)

/**
 * HTTP响应数据类
 */
data class HttpResponse(
    val isSuccess: Boolean,
    val statusCode: Int,
    val body: String,
    val headers: Map<String, List<String>>,
    val error: String? = null
)

/**
 * HTTP客户端实现
 */
object HttpRequest {
    // 默认OkHttpClient，可配置连接池、超时等
    private val defaultClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
//            .addInterceptor(LoggingInterceptor())
            .build()
    }

    // 支持SOCKS5代理的客户端
    private val socks5Clients = mutableMapOf<String, OkHttpClient>()

    /**
     * 发送POST请求
     * @param req HTTP请求参数
     * @param resBody 响应体字符串构建器（用于兼容原接口）
     * @return 是否成功
     */
    @Deprecated("使用新版本HttpResponse", ReplaceWith("PostEx(req).isSuccess"))
    fun Post(req: HttpReq, resBody: StringBuilder): Boolean {
        return try {
            val response = PostEx(req)
            if (response.isSuccess) {
                resBody.append(response.body)
                true
            } else {
                resBody.append(response.error ?: "Request failed")
                false
            }
        } catch (e: Exception) {
            resBody.append(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 发送POST请求（新版）
     * @param req HTTP请求参数
     * @return HTTP响应
     */
    fun PostEx(req: HttpReq): HttpResponse {
        return try {
            // 创建请求体
            val mediaType = req.contentType.toMediaType()
            val requestBody = req.content.toRequestBody(mediaType)

            // 构建请求
            val requestBuilder = Request.Builder()
                .url(req.uri)
                .post(requestBody)

            // 添加HttpReq中的请求头
            addHeadersToBuilder(requestBuilder, req.headers)

            // 确保有Content-Type头
            if (!req.headers.containsKey("Content-Type")) {
                requestBuilder.addHeader("Content-Type", req.contentType)
            }

            // 创建请求
            val request = requestBuilder.build()

            // 获取HTTP客户端（根据是否需要SOCKS5代理）
            val client = if (req.headers.containsKey("x-socks5-proxy")) {
                getSocks5Client(req.headers["x-socks5-proxy"] ?: "")
            } else {
                defaultClient
            }

            // 执行请求
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: IOException) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "Request error: ${e.message}"
            )
        }
    }

    /**
     * 发送POST请求（带表单数据）
     * @param url 请求URL
     * @param formData 表单数据
     * @param headers 请求头
     * @return HTTP响应
     */
    fun PostForm(url: String, formData: Map<String, String>, headers: Map<String, String> = emptyMap()): HttpResponse {
        return try {
            // 构建表单数据
            val formBodyBuilder = FormBody.Builder()
            formData.forEach { (key, value) ->
                formBodyBuilder.add(key, value)
            }
            val formBody = formBodyBuilder.build()

            // 构建请求
            val requestBuilder = Request.Builder()
                .url(url)
                .post(formBody)

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "Form POST error: ${e.message}"
            )
        }
    }

    /**
     * 发送文件上传请求（Multipart）
     * @param url 请求URL
     * @param fileFieldName 文件字段名
     * @param file 要上传的文件
     * @param extraParams 额外参数
     * @param headers 请求头
     * @return HTTP响应
     */
    fun UploadFile(
        url: String,
        fileFieldName: String,
        file: File,
        extraParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        return try {
            // 创建Multipart请求体
            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    fileFieldName,
                    file.name,
                    file.asRequestBody("application/octet-stream".toMediaType())
                )

            // 添加额外参数
            extraParams.forEach { (key, value) ->
                requestBodyBuilder.addFormDataPart(key, value)
            }

            val requestBody = requestBodyBuilder.build()

            // 构建请求
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "File upload error: ${e.message}"
            )
        }
    }

    /**
     * 获取SOCKS5代理客户端
     */
    private fun getSocks5Client(proxyUri: String): OkHttpClient {
        return socks5Clients.getOrPut(proxyUri) {
            try {
                val url = URI(proxyUri)
                val proxy = java.net.Proxy(
                    java.net.Proxy.Type.SOCKS,
                    java.net.InetSocketAddress(url.host, url.port)
                )

                OkHttpClient.Builder()
                    .proxy(proxy)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            } catch (e: Exception) {
                println("Failed to create SOCKS5 client: ${e.message}")
                defaultClient
            }
        }
    }

    /**
     * 发送GET请求
     */
    fun Get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "GET error: ${e.message}"
            )
        }
    }

    /**
     * 发送GET请求下载文件
     * @param url 文件URL
     * @param savePath 保存路径
     * @param headers 请求头
     * @return 是否成功
     */
    fun DownloadFile(url: String, savePath: String, headers: Map<String, String> = emptyMap()): Boolean {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        File(savePath).outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            println("Download error: ${e.message}")
            false
        }
    }

    /**
     * 发送PUT请求
     */
    fun Put(req: HttpReq): HttpResponse {
        return try {
            val mediaType = req.contentType.toMediaType()
            val requestBody = req.content.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(req.uri)
                .put(requestBody)

            addHeadersToBuilder(requestBuilder, req.headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "PUT error: ${e.message}"
            )
        }
    }

    /**
     * 发送DELETE请求
     */
    fun Delete(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .delete()

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "DELETE error: ${e.message}"
            )
        }
    }

    /**
     * 发送PATCH请求
     */
    fun Patch(req: HttpReq): HttpResponse {
        return try {
            val mediaType = req.contentType.toMediaType()
            val requestBody = req.content.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(req.uri)
                .patch(requestBody)

            addHeadersToBuilder(requestBuilder, req.headers)

            val request = requestBuilder.build()

            defaultClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "PATCH error: ${e.message}"
            )
        }
    }

    /**
     * 发送带超时的GET请求
     */
    fun GetWithTimeout(url: String, timeoutSeconds: Long, headers: Map<String, String> = emptyMap()): HttpResponse {
        return try {
            // 创建自定义超时的客户端
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            addHeadersToBuilder(requestBuilder, headers)

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "GET with timeout error: ${e.message}"
            )
        }
    }

    /**
     * 发送带超时的POST请求
     */
    fun PostWithTimeout(req: HttpReq, timeoutSeconds: Long): HttpResponse {
        return try {
            // 创建自定义超时的客户端
            val client = OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build()

            val mediaType = req.contentType.toMediaType()
            val requestBody = req.content.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(req.uri)
                .post(requestBody)

            addHeadersToBuilder(requestBuilder, req.headers)

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                HttpResponse(
                    isSuccess = response.isSuccessful,
                    statusCode = response.code,
                    body = body,
                    headers = response.headers.toMultimap()
                )
            }
        } catch (e: Exception) {
            HttpResponse(
                isSuccess = false,
                statusCode = -1,
                body = "",
                headers = emptyMap(),
                error = "POST with timeout error: ${e.message}"
            )
        }
    }

    /**
     * 添加请求头到Request.Builder的辅助方法
     */
    private fun addHeadersToBuilder(builder: Request.Builder, headers: Map<String, String>) {
        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
    }

    /**
     * 清理所有客户端
     */
    fun cleanup() {
        defaultClient.dispatcher.executorService.shutdown()
        socks5Clients.values.forEach { client ->
            client.dispatcher.executorService.shutdown()
        }
        socks5Clients.clear()
    }
}

/**
 * 日志拦截器（用于调试）
 */
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        println("--> ${request.method} ${request.url}")
        request.headers.toMultimap() .forEach { name, value ->
            println("$name: $value")
        }

        if (request.body != null) {
            println("Request body size: ${request.body?.contentLength()} bytes")
        }

        val response = chain.proceed(request)
        val endTime = System.nanoTime()

        println("<-- ${response.code} ${response.message} ${request.url}")
        println("Time: ${(endTime - startTime) / 1e6} ms")
        response.headers.toMultimap() .forEach { name, value ->
            println("$name: $value")
        }

        return response
    }
}

/**
 * HTTP请求扩展函数
 */

/**
 * 发送POST请求的扩展函数
 */
fun String.postHttp(
    url: String,
    headers: MutableMap<String, String> = mutableMapOf(),
    contentType: String = "application/json;charset=UTF-8"
): HttpResponse {
    val req = HttpReq(
        uri = url,
        content = this,
        headers = headers,
        contentType = contentType
    )
    return HttpRequest.PostEx(req)
}

/**
 * Map转换为POST请求
 */
fun Map<String, Any>.postHttp(
    url: String,
    headers: MutableMap<String, String> = mutableMapOf()
): HttpResponse {
    val json = com.google.gson.Gson().toJson(this)
    return json.postHttp(url, headers)
}

/**
 * 发送表单POST请求的扩展函数
 */
fun Map<String, String>.postFormHttp(
    url: String,
    headers: MutableMap<String, String> = mutableMapOf()
): HttpResponse {
    return HttpRequest.PostForm(url, this, headers)
}