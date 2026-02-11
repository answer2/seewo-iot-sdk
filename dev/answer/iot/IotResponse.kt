package dev.answer.iot
import com.google.gson.annotations.SerializedName
/**
 *
 * @author AnswerDev
 * @date 2026/1/1 14:36
 * @description IotResponse
 */


    data class ApiRequest(
        @SerializedName("traceId")
        val traceId: String,

        @SerializedName("requestId")
        val requestId: String,

        @SerializedName("method")
        val method: String,

        @SerializedName("params")
        val params: RequestParams,

        @SerializedName("needToCache")
        val needToCache: Boolean = true
    )

    data class RequestParams(
        @SerializedName("msg_content")
        val msgContent: String
    )

    // 或者如果你想要直接解析 msg_content 中的 JSON 数组，可以这样：
    data class ApiRequestWithParsedContent(
        @SerializedName("traceId")
        val traceId: String,

        @SerializedName("requestId")
        val requestId: String,

        @SerializedName("method")
        val method: String,

        @SerializedName("params")
        val params: ParsedRequestParams,

        @SerializedName("needToCache")
        val needToCache: Boolean = true
    )

    data class ParsedRequestParams(
        @SerializedName("msg_content")
        val msgContent: List<MessageContent>
    )

    data class MessageContent(
        @SerializedName("callBack")
        val callBack: Boolean,

        @SerializedName("createTime")
        val createTime: Long,

        @SerializedName("eventId")
        val eventId: String,

        @SerializedName("extraData")
        val extraData: ExtraData,

        @SerializedName("platform")
        val platform: String,

        @SerializedName("priority")
        val priority: Int,

        @SerializedName("product")
        val product: String,

        @SerializedName("resourceName")
        val resourceName: String,

        @SerializedName("retryCount")
        val retryCount: Int = 0
    )

    data class ExtraData(
        @SerializedName("noteId")
        val noteId: String,

        @SerializedName("userUid")
        val userUid: String = ""
    )

    // 你也可以使用可空类型来处理可能的 null 值
    data class MessageContentNullable(
        @SerializedName("callBack")
        val callBack: Boolean? = null,

        @SerializedName("createTime")
        val createTime: Long? = null,

        @SerializedName("eventId")
        val eventId: String? = null,

        @SerializedName("extraData")
        val extraData: ExtraDataNullable? = null,

        @SerializedName("platform")
        val platform: String? = null,

        @SerializedName("priority")
        val priority: Int? = null,

        @SerializedName("product")
        val product: String? = null,

        @SerializedName("resourceName")
        val resourceName: String? = null,

        @SerializedName("retryCount")
        val retryCount: Int? = 0
    )

    data class ExtraDataNullable(
        @SerializedName("noteId")
        val noteId: String? = null,

        @SerializedName("userUid")
        val userUid: String? = null
    )
