package com.cvte.ciot.core.mqtt;

import com.cvte.ciot.core.callback.NativeCallback;
import com.cvte.ciot.core.model.DeviceAuth;
import com.cvte.ciot.core.model.GrailLog;
import com.cvte.ciot.core.model.tsl.TslBasic;
import com.cvte.ciot.core.model.tsl.TslRequest;
import com.cvte.ciot.core.model.tsl.TslResponse;
import com.cvte.ciot.core.model.IoTConfig;
import com.cvte.ciot.core.model.tsl.TslConfigKey;
import dev.answer.seewocampus.com.cvte.ciot.core.mqtt.IoTMqttImpl;

import java.util.List;

/**
 * @author AnswerDev
 * @date 2025/12/7 10:13
 * @description IoTMqtt
 */
public class IoTMqtt {
    private IoTMqttImpl _impl;

    // 构造及析构
    public IoTMqtt(IoTConfig config) {
        // 构造实现
        _impl = new IoTMqttImpl(config);
    }

    /**
     * 关闭MQTT连接并释放资源
     */
    public void shutdown() {
        if (_impl != null) {
            _impl.shutdown();
        }
    }

    // MQTT连接及认证
    public void Connect() {
        Connect(2, 60);
    }

    public void Connect(int minRetryInterval) {
        Connect(minRetryInterval, 60);
    }

    public void Connect(int minRetryInterval, int maxRetryInterval) {
        try {
            // 开始连接，失败会自动重连
            // 第一次重连间隔为min，以后为min*2，直到max
            _impl.connect(minRetryInterval, maxRetryInterval);
        } catch (Exception e) {
            e.printStackTrace();
            // 可以根据需要处理连接异常
        }
    }

    public boolean IsConnected() {
        return _impl.isConnected();
    }

    // 上报日志到grail
    public void Postlog2Grail(GrailLog log) {
        // TODO: 实现日志上报到Grail的功能
        System.out.println("Postlog2Grail called with: " + log);
        // 暂时为空实现，需要根据实际需求实现
    }

    // 上报基础信息
    public void PostDeviceVersion(String version) {
        _impl.postDeviceVersion(version);
    }

    public void PostDeviceName(String deviceName) {
        _impl.postDeviceName(deviceName);
    }

    public void PostConfigVersion(List<TslConfigKey> config) {
        _impl.postConfigVersion(config);
    }

    // 设置日志回调
    public static void SetLogCallback(NativeCallback.MLogCallBack callback) {
        // TODO: 实现全局日志回调设置
        System.out.println("SetLogCallback called");
        // 由于是静态方法，可能需要一个全局的日志管理器
    }

    // 远程配置回调
    public void SetConfigCallback(NativeCallback.OnTslConfig callback) {
        _impl.setConfigCallback(callback);
    }

    // OTA升级回调
    public void SetUpgradeCallback(NativeCallback.OnTslUpgrade callback) {
        _impl.setUpgradeCallback(callback);
    }

    /************************************************************************/
    /* 下行（云端 -> 设备）                                                   */
    /************************************************************************/
    public void SetPropertySetCallback(NativeCallback.OnTslRequest callback) {
        // 设置设置属性回调
        _impl.setPropertySetCallback(callback);
    }

    public void SetPropertyGetCallback(NativeCallback.OnTslRequest callback) {
        // 设置获取属性回调
        _impl.setPropertyGetCallback(callback);
    }

    public void SetServiceCallback(NativeCallback.OnTslRequest callback) {
        // 设置服务调用回调
        _impl.setServiceCallback(callback);
    }

    public boolean SubscribeCustom(String topic, NativeCallback.OnCustomTopic callback) {
        // 订阅自定义主题
        return _impl.subscribeCustom(topic, callback);
    }

    /************************************************************************/
    /* 上行（设备 -> 云端）                                                   */
    /************************************************************************/
    public boolean PropertyPost(TslBasic basic, TslRequest req) {
        // 属性上报
        return _impl.propertyPost(basic, req);
    }

    public boolean EventPost(TslBasic basic, TslRequest req) {
        // 事件上报
        return _impl.eventPost(basic, req);
    }

    public boolean PropertyGet(TslBasic basic, TslRequest req, TslResponse res) {
        // 上行属性获取
        return _impl.propertyGet(basic, req, res);
    }

    public boolean ServiceCall(TslBasic basic, TslRequest req, TslResponse res) {
        // 上行服务调用
        return _impl.serviceCall(basic, req, res);
    }

    public boolean PublishCustom(String topic, String traceId, String params) {
        // 发布自定义主题
        return _impl.publishCustom(topic, traceId, params);
    }

    /************************************************************************/
    /* 网关类设备                                                            */
    /************************************************************************/
    public boolean GetSubDevice(TslBasic basic, List<DeviceAuth> devs) {
        // 获取网关下挂子设备
        return _impl.getSubDevice(basic, devs);
    }

    public boolean AddSubDevice(TslBasic basic, DeviceAuth dev) {
        // 添加网关子设备
        return _impl.addSubDevice(basic, dev);
    }

    public boolean DelSubDevice(TslBasic basic, DeviceAuth dev) {
        // 删除网关子设备
        return _impl.delSubDevice(basic, dev);
    }

    public boolean OnlineSubDevice(TslBasic basic, DeviceAuth dev) {
        // 上线子设备（包括认证）
        return _impl.onlineSubDevice(basic, dev);
    }

    public boolean OfflineSubDevice(TslBasic basic, DeviceAuth dev) {
        // 下线子设备
        return _impl.offlineSubDevice(basic, dev);
    }

    /**
     * 直接发布原始MQTT消息
     * @param topic MQTT主题
     * @param payload 消息内容
     * @param qos 服务质量等级 (0,1,2)
     * @param isRetain 是否保留消息
     * @return 发布是否成功
     */
    public boolean PublishMessage(String topic, String payload, int qos, boolean isRetain) {
        return _impl.publishMessage(topic, payload, qos, isRetain);
    }

    /**
     * 获取底层实现（用于调试或扩展）
     * @return IoTMqttImpl实例
     */
    public IoTMqttImpl getImpl() {
        return _impl;
    }
}