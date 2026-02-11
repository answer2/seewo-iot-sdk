package com.cvte.ciot.core.mqtt;

import com.cvte.ciot.core.IIoTClient;
import com.cvte.ciot.core.callback.IotLogCallback;
import com.cvte.ciot.core.callback.NativeCallback;
import com.cvte.ciot.core.callback.OnCustomTopicCallback;
import com.cvte.ciot.core.callback.OnTslDownCallback;
import com.cvte.ciot.core.model.tsl.TslBasic;
import com.cvte.ciot.core.model.tsl.TslRequest;
import com.cvte.ciot.core.model.tsl.TslResponse;
import java.util.concurrent.locks.Lock;

public class MqttIoTClient implements IIoTClient {
    private final Iot mIot;

    public MqttIoTClient(Iot iot) {
        this.mIot = iot;
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public void setLog(boolean enable, IotLogCallback callback) {
        this.mIot.setLog(enable, callback);
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean isConnected() {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.isInitConnectComplete()) {
                return this.mIot.isMqttConnected();
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean setPropertySetCallback(OnTslDownCallback callback) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.checkMqttPointEnable()) {
                this.mIot.setPropertySetCallback(callback);
                return true;
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean setPropertyGetCallback(OnTslDownCallback callback) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.checkMqttPointEnable()) {
                this.mIot.setPropertyGetCallback(callback);
                return true;
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean setServiceCallback(OnTslDownCallback callback) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.checkMqttPointEnable()) {
                this.mIot.setServiceCallback(callback);
                return true;
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean subscribeCustom(String topic, NativeCallback.OnCustomTopic callback) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.checkMqttPointEnable()) {
                // 需要将 NativeCallback.OnCustomTopic 转换为 OnCustomTopicCallback
                NativeCallback.OnCustomTopic customCallback = null;
                if (callback != null) {
                    customCallback = new NativeCallback.OnCustomTopic() {
                        @Override
                        public void onTopicMessage(String topicName, String payload) {
                            callback.onTopicMessage(topicName, payload);
                        }
                    };
                }
                return this.mIot.subscribeCustom(topic, customCallback);
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean postProperty(TslBasic basic, TslRequest request) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            return this.mIot.isInitConnectComplete() ? this.mIot.propertyPost(basic, request) : false;
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean postEvent(TslBasic basic, TslRequest request) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            return this.mIot.isInitConnectComplete() ? this.mIot.eventPost(basic, request) : false;
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public TslResponse getProperty(TslBasic basic, TslRequest request) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.isInitConnectComplete()) {
                return this.mIot.propertyGet(basic, request);
            } else {
                return new TslResponse(TslResponse.FAILED);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public TslResponse callService(TslBasic basic, TslRequest request) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            if (this.mIot.isInitConnectComplete()) {
                return this.mIot.serviceCall(basic, request);
            } else {
                return new TslResponse(TslResponse.FAILED);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override // com.cvte.ciot.core.IIoTClient
    public boolean publishCustom(String topic, String traceId, String params) {
        Lock readLock = mIot.getReadLock(); // 使用 getReadLock() 方法
        readLock.lock();
        try {
            return this.mIot.isInitConnectComplete() ? this.mIot.publishCustom(topic, traceId, params) : false;
        } finally {
            readLock.unlock();
        }
    }
}