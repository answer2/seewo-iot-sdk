package com.cvte.ciot.core.iotexception;

/**
 * @author AnswerDev
 * @date 2025/12/7 03:58
 * @description NotSupportProtocolException
 */
public class NotSupportProtocolException extends Exception {
    public NotSupportProtocolException() {
        super("Protocol type not support yet");
    }
}
