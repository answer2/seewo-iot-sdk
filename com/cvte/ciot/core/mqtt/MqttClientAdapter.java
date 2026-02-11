package com.cvte.ciot.core.mqtt;

import com.cvte.ciot.core.callback.NativeCallback;
import com.cvte.ciot.core.callback.OnConnectState;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

public class MqttClientAdapter implements MqttCallbackExtended {

    private MqttAsyncClient client;
    private final String serverURI;
    private final String clientId;

    private OnConnectState stateCallback;
    private NativeCallback.OnCustomTopic messageCallback;

    public MqttClientAdapter(String serverURI, String clientId) {
        this.serverURI = serverURI;
        this.clientId = clientId;
    }

    public void setCallbacks(OnConnectState stateCb,
                             NativeCallback.OnCustomTopic msgCb) {
        this.stateCallback = stateCb;
        this.messageCallback = msgCb;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void connect(String userName,
                        String password,
                        int connectTimeoutSec,
                        int maxReconnectIntervalSec) {

        try {
            if (client == null) {
                client = new MqttAsyncClient(
                        serverURI,
                        clientId,
                        new MemoryPersistence()
                );
                client.setCallback(this);
            }

            MqttConnectOptions opt = new MqttConnectOptions();
            opt.setUserName(userName);
            opt.setPassword(password.toCharArray());
            opt.setCleanSession(false);
            opt.setAutomaticReconnect(true);
            opt.setConnectionTimeout(connectTimeoutSec);
            opt.setKeepAliveInterval(60);
            opt.setMaxReconnectDelay(maxReconnectIntervalSec);

            client.connect(opt, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // connectComplete 会统一回调
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (stateCallback != null) {
                        stateCallback.onConnectState(false);
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
            if (stateCallback != null) {
                stateCallback.onConnectState(false);
            }
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignored) {
        }
    }

    public boolean subscribe(String topic, int qos) {
        if (!isConnected()) return false;
        try {
            client.subscribe(topic, qos);
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean publish(String topic, String payload, int qos) {
        if (!isConnected()) return false;
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(qos);
            client.publish(topic, msg);
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Callback ----------

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (stateCallback != null) {
            stateCallback.onConnectState(true);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        if (stateCallback != null) {
            stateCallback.onConnectState(false);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        if (messageCallback != null) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            messageCallback.onTopicMessage(topic, payload);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // ignore
    }
}
