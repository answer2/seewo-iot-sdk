package com.cvte.ciot.core.auth;

import com.cvte.ciot.core.model.DeviceAuth;
import com.cvte.ciot.core.model.RegisterConfig;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:04
 * @description Authenticator
 */
public class Authenticator {
    public static DeviceAuth registerDevice(RegisterConfig registerConfig){
        return new DeviceAuth(registerConfig.getProductKey(), registerConfig.getDeviceId(), registerConfig.getProductSecret());
    };

}
