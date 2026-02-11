package com.cvte.ciot.core.model.tsl;

/**
 * @author AnswerDev
 * @date 2025/12/7 12:22
 * @description TslDefines
 */
/**
 * TSL协议方法定义
 */
public final class TslDefines {

    /** 私有构造器，防止实例化 */
    private TslDefines() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String CODE_SUCCESS_STRING = "000000";
    public static final  String DEF_TSL_VERSION = "1.0.1";

    /* ========== 设备认证相关标签 ========== */

    /** 产品Key */
    public static final String TAG_PRODUCT_KEY = "productKey";

    /** 产品密钥 */
    public static final String TAG_PRODUCT_SECRET = "productSecret";

    /** 设备ID */
    public static final String TAG_DEVICE_ID = "deviceId";

    /** 设备密钥 */
    public static final String TAG_DEVICE_SECRET = "deviceSecret";

    /* ========== 响应相关标签 ========== */

    /** 响应码 */
    public static final String TAG_CODE = "code";

    /** 响应消息 */
    public static final String TAG_MESSAGE = "message";

    /** 协议版本 */
    public static final String TAG_VERSION = "version";

    /** 跟踪ID */
    public static final String TAG_TRACEID = "traceId";

    /* ========== 请求相关标签 ========== */

    /** 方法名 */
    public static final String TAG_METHOD = "method";

    /** 数据 */
    public static final String TAG_DATA = "data";

    /** 参数 */
    public static final String TAG_PARAMS = "params";

    /* ========== 下行方法 ========== */

    /** 下行属性设置 */
    public static final String DOWN_METHOD_PROPERTY_SET = "thing.property.set";

    /** 下行属性获取 */
    public static final String DOWN_METHOD_PROPERTY_GET = "thing.property.get";

    /** 下行服务调用前缀 */
    public static final String DOWN_METHOD_SERVICE = "thing.service.";

    /** 下行配置推送服务 */
    public static final String DOWN_SERVICE_CONFIG_PUSH = "thing.service.configPush";

    /** 下行升级服务 */
    public static final String DOWN_SERVICE_UPGRADE = "thing.service.upgrade";

    /* ========== 上行方法 ========== */

    /** 上行属性上报 */
    public static final String UP_METHOD_PROPERTY_POST = "thing.property.post";

    /** 上行属性获取 */
    public static final String UP_METHOD_PROPERTY_GET = "thing.property.get";

    /** 上行服务调用前缀 */
    public static final String UP_METHOD_SERVICE = "thing.service.";

    /** 上行事件上报前缀 */
    public static final String UP_METHOD_EVENT_POST = "thing.event.";

    /* ========== 网关类方法 ========== */

    /** 获取网关下挂子设备 */
    public static final String UP_METHOD_SUB_GET = "thing.sub.get";

    /** 添加网关子设备 */
    public static final String UP_METHOD_SUB_ADD = "thing.sub.add";

    /** 删除网关子设备 */
    public static final String UP_METHOD_SUB_DEL = "thing.sub.del";

    /** 子设备上线 */
    public static final String UP_METHOD_SUB_CONNECT = "thing.sub.connect";

    /** 子设备下线 */
    public static final String UP_METHOD_SUB_DISCONNECT = "thing.sub.disconnect";

    /* ========== 标准功能 ========== */

    /** 标准基础信息上报 */
    public static final String UP_METHOD_BASIC_POST = "thing.event.basic.post";

    /** 配置版本上报 */
    public static final String UP_METHOD_CONFIG_POST = "thing.event.config.post";

    /* ========== 工具方法 ========== */

    /**
     * 判断是否为下行方法
     * @param method 方法名
     * @return 是否为下行方法
     */
    public static boolean isDownMethod(String method) {
        if (method == null) return false;
        return method.startsWith(DOWN_METHOD_PROPERTY_SET) ||
                method.startsWith(DOWN_METHOD_PROPERTY_GET) ||
                method.startsWith(DOWN_METHOD_SERVICE);
    }

    /**
     * 判断是否为上行方法
     * @param method 方法名
     * @return 是否为上行方法
     */
    public static boolean isUpMethod(String method) {
        if (method == null) return false;
        return method.startsWith(UP_METHOD_PROPERTY_POST) ||
                method.startsWith(UP_METHOD_PROPERTY_GET) ||
                method.startsWith(UP_METHOD_SERVICE) ||
                method.startsWith(UP_METHOD_SERVICE) ||
                method.startsWith(UP_METHOD_SUB_GET) ||
                method.equals(UP_METHOD_SUB_ADD) ||
                method.equals(UP_METHOD_SUB_DEL) ||
                method.equals(UP_METHOD_SUB_CONNECT) ||
                method.equals(UP_METHOD_SUB_DISCONNECT) ||
                method.equals(UP_METHOD_BASIC_POST) ||
                method.equals(UP_METHOD_CONFIG_POST);
    }

    /**
     * 判断是否为网关相关方法
     * @param method 方法名
     * @return 是否为网关方法
     */
    public static boolean isGatewayMethod(String method) {
        if (method == null) return false;
        return method.equals(UP_METHOD_SUB_GET) ||
                method.equals(UP_METHOD_SUB_ADD) ||
                method.equals(UP_METHOD_SUB_DEL) ||
                method.equals(UP_METHOD_SUB_CONNECT) ||
                method.equals(UP_METHOD_SUB_DISCONNECT);
    }

    /**
     * 判断是否为属性相关方法
     * @param method 方法名
     * @return 是否为属性方法
     */
    public static boolean isPropertyMethod(String method) {
        if (method == null) return false;
        return method.equals(DOWN_METHOD_PROPERTY_SET) ||
                method.equals(DOWN_METHOD_PROPERTY_GET) ||
                method.equals(UP_METHOD_PROPERTY_POST) ||
                method.equals(UP_METHOD_PROPERTY_GET);
    }

    /**
     * 判断是否为服务相关方法
     * @param method 方法名
     * @return 是否为服务方法
     */
    public static boolean isServiceMethod(String method) {
        if (method == null) return false;
        return method.startsWith(DOWN_METHOD_SERVICE) ||
                method.startsWith(UP_METHOD_SERVICE);
    }

    /**
     * 判断是否为事件相关方法
     * @param method 方法名
     * @return 是否为事件方法
     */
    public static boolean isEventMethod(String method) {
        if (method == null) return false;
        return method.startsWith(UP_METHOD_SERVICE);
    }
}