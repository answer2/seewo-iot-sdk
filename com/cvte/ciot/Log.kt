package com.cvte.ciot

import java.text.SimpleDateFormat
import java.util.*
/**
 *
 * @author AnswerDev
 * @date 2025/12/7 04:13
 * @description Log
 */

object Log {

    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    @JvmStatic
    var currentLevel = Level.DEBUG

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @JvmStatic
    fun v(tag: String, message: String) {
        log(Level.VERBOSE, tag, message)
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        log(Level.INFO, tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        log(Level.WARN, tag, message)
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        log(Level.ERROR, tag, message, null)
    }

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable?) {
        log(Level.ERROR, tag, message, throwable)
    }

    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        if (level.ordinal < currentLevel.ordinal) return

        val timestamp = dateFormat.format(Date())
        val thread = Thread.currentThread().name
        val levelChar = level.name[0]

        val logMessage = "$timestamp [$thread] $levelChar/$tag: $message"

        when (level) {
            Level.ERROR -> System.err.println(logMessage)
            else -> System.out.println(logMessage)
        }

        throwable?.printStackTrace()
    }
}