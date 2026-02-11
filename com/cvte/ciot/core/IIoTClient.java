package com.cvte.ciot.core;

import com.cvte.ciot.core.callback.IotLogCallback;
import com.cvte.ciot.core.callback.NativeCallback;
import com.cvte.ciot.core.callback.OnCustomTopicCallback;
import com.cvte.ciot.core.callback.OnTslDownCallback;
import com.cvte.ciot.core.model.tsl.TslBasic;
import com.cvte.ciot.core.model.tsl.TslRequest;
import com.cvte.ciot.core.model.tsl.TslResponse;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:02
 * @description IIoTClient
 */
public interface IIoTClient {
    TslResponse callService(TslBasic tslBasic, TslRequest tslRequest);

    TslResponse getProperty(TslBasic tslBasic, TslRequest tslRequest);

    boolean isConnected();

    boolean postEvent(TslBasic tslBasic, TslRequest tslRequest);

    boolean postProperty(TslBasic tslBasic, TslRequest tslRequest);

    boolean publishCustom(String str, String str2, String str3);

    void setLog(boolean z, IotLogCallback iotLogCallback);

    boolean setPropertyGetCallback(OnTslDownCallback onTslDownCallback);

    boolean setPropertySetCallback(OnTslDownCallback onTslDownCallback);

    boolean setServiceCallback(OnTslDownCallback onTslDownCallback);

    boolean subscribeCustom(String str, NativeCallback.OnCustomTopic onCustomTopicCallback);
}