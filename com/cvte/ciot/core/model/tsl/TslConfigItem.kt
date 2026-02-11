package com.cvte.ciot.core.model.tsl

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 04:38
 * @description TslConfigItem
 */
data class TslConfigItem(var key : TslConfigKey? = TslConfigKey(),
                         var values : String = ""///配置值
                          )
