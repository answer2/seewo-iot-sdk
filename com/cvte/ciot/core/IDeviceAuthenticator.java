package com.cvte.ciot.core;

import com.cvte.ciot.core.model.DeviceAuth;
import com.cvte.ciot.core.model.RegisterConfig;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:01
 * @description IDeviceAuthenticator
 */
public interface IDeviceAuthenticator {
    DeviceAuth register(RegisterConfig registerConfig);
}
