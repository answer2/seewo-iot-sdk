package com.cvte.ciot.core.model;

/**
 * @author AnswerDev
 * @date 2025/12/7 03:45
 * @description IotConnectError
 */
public class IotConnectError {
    public static int kAuthenticationFailed = 5;
    public static int kIdentifierRejected = 2;
    public static int kProtocolNotAccept = 1;
    public static int kServiceNotAvailable = 3;
    public static int kSuccess = 0;
    public static int kTcpError = -1;
    public static int kTlsError = -2;
    public static int kUserNameOrPasswordError = 4;
    public static int mqttPrepareFailed = 100232;
    public static int outTime = 100233;
    public static int registerFailed = 100231;
    public static int unknown = 100999;
}