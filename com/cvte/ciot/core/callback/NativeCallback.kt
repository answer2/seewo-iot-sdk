package com.cvte.ciot.core.callback

import com.cvte.ciot.core.model.tsl.TslBasic
import com.cvte.ciot.core.model.tsl.TslRequest

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 10:18
 * @description NativeCallback
 */
class NativeCallback {
    fun interface MLogCallBack {
        fun onLog(message: String)
    }

    fun interface OnTslConfig {
        fun onConfig(config: String)
    }

    fun interface OnTslUpgrade {
        fun onUpgrade(upgradeInfo: String)
    }

    fun interface OnTslRequest {
        fun onRequest(basic: TslBasic, req: TslRequest)
    }

    fun interface OnCustomTopic {
        fun onTopicMessage(topic: String, message: String)
    }
}