package com.cvte.ciot.core.callback;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:00
 * @description OnCustomTopicCallback
 */
public abstract class OnCustomTopicCallback {


    public abstract void onTopicMessage(String topicName, String payload);
}
