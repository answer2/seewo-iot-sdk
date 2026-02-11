package com.cvte.ciot.core.auth;

import com.cvte.ciot.core.IDeviceAuthenticator;
import com.cvte.ciot.core.model.DeviceAuth;
import com.cvte.ciot.core.model.RegisterConfig;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:03
 * @description HttpDeviceRegister
 */
public class HttpDeviceRegister implements IDeviceAuthenticator {
    @Override // com.cvte.ciot.core.IDeviceAuthenticator
    public DeviceAuth register(RegisterConfig registerConfig) {
        System.out.println("IoTSdkManager" +"HttpDeviceRegister register");
        return Authenticator.registerDevice(registerConfig);
    }
}