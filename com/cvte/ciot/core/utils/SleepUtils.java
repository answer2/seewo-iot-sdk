package com.cvte.ciot.core.utils;

/**
 * @author AnswerDev
 * @date 2025/12/7 04:21
 * @description SleepUtils
 */
public class SleepUtils {

    /**
     * 安全的睡眠方法，自动处理中断
     *
     * @param millis 毫秒
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 恢复中断标志
            Thread.currentThread().interrupt();
            // 可以选择抛出运行时异常或记录日志
            throw new RuntimeException("Sleep interrupted", e);
        }
    }

    /**
     * 带有忽略中断选项的睡眠方法
     *
     * @param millis          毫秒
     * @param ignoreInterrupt 是否忽略中断（继续睡眠直到完成）
     */
    public static void sleep(long millis, boolean ignoreInterrupt) {
        long remaining = millis;
        long start = System.currentTimeMillis();

        while (remaining > 0) {
            try {
                Thread.sleep(remaining);
                break;  // 正常完成睡眠
            } catch (InterruptedException e) {
                if (!ignoreInterrupt) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Sleep interrupted", e);
                }
                // 计算剩余时间继续睡眠
                remaining = millis - (System.currentTimeMillis() - start);
            }
        }
    }
}