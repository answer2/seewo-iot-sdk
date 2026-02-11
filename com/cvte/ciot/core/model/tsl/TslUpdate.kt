package com.cvte.ciot.core.model.tsl

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 04:40
 * @description TslUpdate
 */
data class TslUpdate(
    var versionCode: String = "",  // 版本号
    var policyTag: String = "",    // 策略tag
    var appKey: String = ""        // 萌友appKey
)
