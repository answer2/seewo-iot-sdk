package com.cvte.ciot.core.model;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:35
 * @description IotLogType
 */
public class IotLogType {
    public static final int
            IOT_LOG_DEV = 1, /// 设备行为分析（上下线）
            IOT_LOG_UP = 2, /// 上行消息分析
            IOT_LOG_DOWN = 3;    ///下行消息分析
}
