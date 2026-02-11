package com.cvte.ciot.core.utils;

/**
 * @author AnswerDev
 * @date 2025/12/7 12:45
 * @description TopicParser
 */

public class TopicParser {

    /**
     * @brief 简化版 Topic 匹配，用于区分消息类型
     */
    public static boolean match(String topicFilter, String topic) {
        // 简化实现: 仅检查前缀和通配符 "+" 的位置
        if (topicFilter.contains("+") || topicFilter.contains("#")) {
            // 实际应实现完整的 MQTT topic 匹配逻辑
            if (topicFilter.startsWith("/sys/+/+/rpc/request/") && topic.contains("/rpc/request/")) return true;
            if (topicFilter.startsWith("/sys/+/+/up/response/") && topic.contains("/up/response/")) return true;
        }
        return topicFilter.equals(topic);
    }

    /**
     * @brief 检查方法是否匹配全名或简称
     */
    public static boolean isTheMethod(String fullMethodPrefix, String method) {
        // 匹配全名
        if (fullMethodPrefix.endsWith(".") && method.startsWith(fullMethodPrefix)) return true;
        if (fullMethodPrefix.equals(method)) return true;

        // 匹配标识符名 (thing.service.xxx -> xxx)
        int lastDot = fullMethodPrefix.lastIndexOf('.');
        if (lastDot != -1 && lastDot < fullMethodPrefix.length() - 1) {
            String identifier = fullMethodPrefix.substring(lastDot + 1);
            return identifier.equals(method);
        }
        return false;
    }

    /**
     * @brief 从 Topic 中提取 deviceId: /sys/{pk}/{did}/...
     */
    public static String getDeviceIdFromTopic(String topic) {
        String[] parts = topic.split("/");
        if (parts.length > 3) {
            return parts[3]; // parts[0]="", parts[1]="sys", parts[2]="pk", parts[3]="did"
        }
        return "";
    }

    /**
     * @brief 从 Topic 中提取 messageId: .../request/{messageId}
     */
    public static String getMessageIdFromTopic(String topic) {
        int lastSlash = topic.lastIndexOf('/');
        if (lastSlash != -1) {
            return topic.substring(lastSlash + 1);
        }
        return "";
    }

    /**
     * @brief 生成下行响应 Topic: /sys/{productKey}/{deviceId}/rpc/response/{messageId}
     */
    public static String getDownResTopic(String productKey, String deviceId, String messageId) {
        return "/sys/" + productKey + "/" + deviceId + "/rpc/response/" + messageId;
    }

    /**
     * @brief 生成上行请求 Topic: /sys/{productKey}/{deviceId}/up/request
     */
    public static String getUpReqTopic(String productKey, String deviceId) {
        return "/sys/" + productKey + "/" + deviceId + "/up/request";
    }
}