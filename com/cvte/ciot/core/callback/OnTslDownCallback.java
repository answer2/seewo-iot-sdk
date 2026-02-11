package com.cvte.ciot.core.callback;

import com.cvte.ciot.core.model.tsl.TslResponse;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:00
 * @description OnTslDownCallback
 */
public interface OnTslDownCallback {
    TslResponse onRequest(String params,String method, String deviceId,String isSuccess);
}