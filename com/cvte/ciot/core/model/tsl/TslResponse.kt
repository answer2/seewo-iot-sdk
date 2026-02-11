package com.cvte.ciot.core.model.tsl

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 03:56
 * @description TslResponse
 */

class TslResponse  {
    var code: String = ""
    var message: String = ""
    var data: String = ""

    constructor()

    constructor(code: String) : this(code, "null", "null")

    constructor(code: String, message: String, data: String) {
        this.code = code
        this.message = message
        this.data = data
    }

    companion object {
        const val FAILED = "999999"
        const val SUCCEED = "000000"
        @JvmStatic
        fun success(data: String = "", message: String = ""): TslResponse {
            return TslResponse(SUCCEED, message, data)
        }

        @JvmStatic
        fun failure(code: String = FAILED, message: String = "", data: String = ""): TslResponse {
            return TslResponse(code, message, data)
        }
    }

    override fun toString(): String {
        return "TslResponse{" +
                "code='$code', " +
                "message='$message', " +
                "data='$data'" +
                "}"
    }
}