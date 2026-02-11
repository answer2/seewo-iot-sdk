package com.cvte.ciot.core;

import com.cvte.ciot.core.auth.HttpDeviceRegister;
import com.cvte.ciot.core.callback.OnConnectState;
import com.cvte.ciot.core.iotexception.IllegalBehaviorException;
import com.cvte.ciot.core.iotexception.NotSupportProtocolException;
import com.cvte.ciot.core.model.DeviceAuth;
import com.cvte.ciot.core.model.IoTProtocol;
import com.cvte.ciot.core.model.RegisterConfig;
import com.cvte.ciot.core.model.SSLOption;
import com.cvte.ciot.core.mqtt.Iot;
import com.cvte.ciot.core.mqtt.MqttIoTClient;
import com.cvte.ciot.core.utils.SleepUtils;
import com.cvte.ciot.Log;
import com.cvte.ciot.core.model.MqttInitResult;
import kotlin.Pair;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:06
 * @description IoTSdkManager
 */
public class IoTSdkManager {
    public static final String NETWORK_TYPE_DISCONNECT = "disconnect";
    private static final String CONNECT_ERROR_CODE = "170002";
    private static final int INTERVAL = 30;
    private static final String TAG = "IoTSdkManager";
    private static final String TEST_METHOD = "thing.service.androidTestConnect";
    private static final String TEST_PARAMS = "{\"androidTestConnect\":true}";
    private static final int TEST_WAIT_TIME = 100;
    private static volatile IoTSdkManager sInstance;
    private volatile String brokerUrl;
    private volatile String deviceName;
    private IDeviceAuthenticator mAuthenticator;
    private volatile DeviceAuth mDeviceAuth;
    private IIoTClient mIoTClient;
    private final Iot mIot;
    private volatile IoTProtocol protocol;
    private volatile RegisterConfig registerConfig;


    public IoTSdkManager() {
        this.mIot = new Iot();
        System.out.println("初始化");
    }

    public boolean register() throws IllegalBehaviorException {
        try {
            this.mIot.getWriteLock().lock();
            if (this.mIot.isInitConnectComplete() || this.mDeviceAuth != null) {
                throw new IllegalBehaviorException("you need disconnect first");
            }
            Log.d(TAG, "register called !!!");
            if (this.mAuthenticator == null) {
                this.mAuthenticator = new HttpDeviceRegister();
            }
            try {
                this.mDeviceAuth = this.mAuthenticator.register(this.registerConfig);
            } catch (Exception e) {
                e.printStackTrace();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("register success = ");
            sb.append(this.mDeviceAuth != null);
            Log.d(TAG, sb.toString());
            return this.mDeviceAuth != null;
        } finally {
            this.mIot.getWriteLock().unlock();
        }
    }

    public MqttInitResult initMqtt(String str, OnConnectState onConnectSSLStateCallback) throws NotSupportProtocolException {
        try {
            this.mIot.getWriteLock().lock();
            Pair<Boolean, SSLOption> option = getOption(str);
            return (((Boolean) option.getFirst()).booleanValue() && selectProtocolInit((SSLOption) option.getSecond(), onConnectSSLStateCallback)) ? new MqttInitResult(this.protocol, this.mDeviceAuth, this.mIoTClient) : null;
        } finally {
            this.mIot.getWriteLock().unlock();
        }
    }

    private Pair<Boolean, SSLOption> getOption(String str) {
        if (str == null) {
            return new Pair<>(true, null);
        }
        Log.d(TAG, String.format(">>> now cacert %s= ", str));
        return new Pair<>(true, new SSLOption(str, "", "", "", "", true));
    }


   private static class C27371 {
        private static final int[] switchMap_IoTProtocol;

        static {
            int[] iArr = new int[IoTProtocol.values().length];
            switchMap_IoTProtocol = iArr;
            try {
                iArr[IoTProtocol.MQTT.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                switchMap_IoTProtocol[IoTProtocol.COAP.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                switchMap_IoTProtocol[IoTProtocol.WEBSOCK.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                switchMap_IoTProtocol[IoTProtocol.HTTP.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    private boolean selectProtocolInit(SSLOption sSLOption, OnConnectState onConnectSSLStateCallback) throws NotSupportProtocolException {
        Log.d(TAG, String.format(">>> init iot protocol:<%s>!!", this.protocol));
        if (C27371.switchMap_IoTProtocol[this.protocol.ordinal()] == 1) {
            return initMqttInNative(this.brokerUrl, this.deviceName, this.mDeviceAuth, sSLOption, onConnectSSLStateCallback);
        }
        throw new NotSupportProtocolException();
    }

    private boolean initMqttInNative(String str, String str2, DeviceAuth deviceAuth, SSLOption sSLOption, OnConnectState onConnectSSLStateCallback) {
        try {
            Log.d(TAG, "init Mqtt !!!");
            return this.mIot.initMqtt(str, "8883", deviceAuth, sSLOption, onConnectSSLStateCallback);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void connectAsync() throws IllegalBehaviorException {
        try {
            this.mIot.getWriteLock().lock();
            if (!this.mIot.isInitConnectComplete()) {
                this.mIot.connectMqtt();
                this.mIot.initConnectComplete();
                return;
            }
            throw new IllegalBehaviorException("you need disconnect first");
        } finally {
            this.mIot.getWriteLock().unlock();
        }
    }

    public void disconnectAllClient() throws IllegalBehaviorException {
        try {
            this.mIot.getWriteLock().lock();
            if (this.mIot.isInitConnectComplete()) {
                Log.d(TAG, NETWORK_TYPE_DISCONNECT);
                SleepUtils.sleep(1000L);
                this.mDeviceAuth = null;
                this.mIot.releaseMqtt();
                while (!this.mIot.checkMqttPointEnable()) {
                    Log.d(TAG, "disconnect check");
                    SleepUtils.sleep(100L);
                }
                this.mIot.releaseMqttPtr();
                this.mIot.resetConnectInitState();
                Log.d(TAG, "disconnect disconnect success");
                return;
            }
            throw new IllegalBehaviorException("you need register and connect first");
        } finally {
            this.mIot.getWriteLock().unlock();
        }
    }

    public void disconnectAllClientSaveRegister() throws IllegalBehaviorException {
        try {
            this.mIot.getWriteLock().lock();
            if (this.mIot.isInitConnectComplete()) {
                Log.d(TAG, NETWORK_TYPE_DISCONNECT);
                SleepUtils.sleep(1000L);
                this.mIot.releaseMqtt();
                while (!this.mIot.checkMqttPointEnable()) {
                    Log.d(TAG, "disconnect check");
                    SleepUtils.sleep(100L);
                }
                this.mIot.releaseMqttPtr();
                this.mIot.resetConnectInitState();
                Log.d(TAG, "disconnect disconnect success");
                return;
            }
            throw new IllegalBehaviorException("you need register and connect first");
        } finally {
            this.mIot.getWriteLock().unlock();
        }
    }

    public DeviceAuth getDeviceAuth() {
        return this.mDeviceAuth;
    }

    public IIoTClient getIoTClient() {
        return this.mIoTClient;
    }

    public static class Singleton {
        private IDeviceAuthenticator authenticator;
        private String brokerUrl;
        private String deviceName;
        private IoTProtocol protocol;
        private RegisterConfig registerConfig;

        public Singleton(){

        }

        public Singleton setBrokerUrl(String str) {
            this.brokerUrl = str;
            return this;
        }

        public Singleton setDeviceName(String str) {
            this.deviceName = str;
            return this;
        }

        public Singleton setProtocols(IoTProtocol ioTProtocol) {
            this.protocol = ioTProtocol;
            return this;
        }

        public Singleton setRegisterConfig(RegisterConfig registerConfig) {
            this.registerConfig = registerConfig;
            return this;
        }

        public Singleton setAuthenticator(IDeviceAuthenticator iDeviceAuthenticator) {
            this.authenticator = iDeviceAuthenticator;
            return this;
        }

        public IoTSdkManager single() throws NotSupportProtocolException {
            if (this.brokerUrl == null) {
                throw new IllegalArgumentException("Must invoke setBrokerUrl argument first!!");
            }
            if (this.deviceName == null) {
                throw new IllegalArgumentException("Must invoke setDeviceName argument first!!");
            }
            if (this.registerConfig == null) {
                throw new IllegalArgumentException("Must invoke setRegisterConfig argument first!!");
            }
            synchronized (IoTSdkManager.class) {
                if (IoTSdkManager.sInstance != null) {
                    IoTSdkManager.sInstance.mIot.getWriteLock().lock();
                    IoTSdkManager.sInstance.brokerUrl = this.brokerUrl;
                    IoTSdkManager.sInstance.deviceName = this.deviceName;
                    IoTSdkManager.sInstance.registerConfig = this.registerConfig;
                    IoTSdkManager.sInstance.protocol = this.protocol;
                    IoTSdkManager.sInstance.mAuthenticator = this.authenticator;
                } else {
                    synchronized (IoTSdkManager.class) {
                        if (IoTSdkManager.sInstance == null) {
                            IoTSdkManager unused = IoTSdkManager.sInstance = new IoTSdkManager();
                            try {
                                IoTSdkManager.sInstance.mIot.getWriteLock().lock();
                                IoTSdkManager.sInstance.mIoTClient = getIotClient(this.protocol, IoTSdkManager.sInstance.mIot);
                                IoTSdkManager.sInstance.mIot.getWriteLock().unlock();
                            } finally {
                            }
                        }
                        try {
                            IoTSdkManager.sInstance.mIot.getWriteLock().lock();
                            IoTSdkManager.sInstance.brokerUrl = this.brokerUrl;
                            IoTSdkManager.sInstance.deviceName = this.deviceName;
                            IoTSdkManager.sInstance.registerConfig = this.registerConfig;
                            IoTSdkManager.sInstance.protocol = this.protocol;
                            IoTSdkManager.sInstance.mAuthenticator = this.authenticator;
                        } finally {
                        }
                    }
                }
            }
            return IoTSdkManager.sInstance;
        }

        private IIoTClient getIotClient(IoTProtocol ioTProtocol, Iot iot) throws NotSupportProtocolException {
            if (C27371.switchMap_IoTProtocol[ioTProtocol.ordinal()] == 1) {
                return new MqttIoTClient(iot);
            }
            throw new NotSupportProtocolException();
        }
    }

}
