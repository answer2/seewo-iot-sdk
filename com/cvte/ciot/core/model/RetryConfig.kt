package com.cvte.ciot.core.model

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 03:48
 * @description RetryConfig
 */

data class RetryConfig(
    val retryCount: Int,
    val delayMaxSecond: Int,
    val delayBaseNum: Int
)  {

    companion object {
        @JvmStatic
        fun defaultRetryConfig(): RetryConfig = RetryConfig(
            retryCount = -1,
            delayMaxSecond = 60,
            delayBaseNum = 2
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        var retryCount: Int = 0
        var delayMaxSecond: Int = 0
        var delayBaseNum: Int = 0

        fun setRetryCount(retryCount: Int): Builder {
            this.retryCount = retryCount
            return this
        }

        fun setDelayMaxSecond(delayMaxSecond: Int): Builder {
            this.delayMaxSecond = delayMaxSecond
            return this
        }

        fun setDelayBaseNum(delayBaseNum: Int): Builder {
            this.delayBaseNum = delayBaseNum
            return this
        }

        fun build(): RetryConfig = RetryConfig(retryCount, delayMaxSecond, delayBaseNum)
    }

    override fun toString(): String {
        return "RetryConfig{" +
                "retryCount=$retryCount, " +
                "delayMaxSecond=$delayMaxSecond, " +
                "delayBaseNum=$delayBaseNum" +
                "}"
    }
}