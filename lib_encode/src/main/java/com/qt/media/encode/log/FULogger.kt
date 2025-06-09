package com.qt.camera.log

import android.util.Log

object FULogger {
    /**
     * 日志前缀
     */
    private const val PREFIX = "FU-"

    /**
     * 当前日志等级
     */
    private var mLogLevel = LogLevel.DEBUG

    /**
     * 日志拦截器
     */
    private var mFuLogInterceptor: ILogInterceptor? = null


    /**
     * 设置日志等级
     *
     * @param level
     */
    @JvmStatic
    fun setLogLevel(level: LogLevel) {
        mLogLevel = level
    }

    /**
     * 设置日志拦截器
     * @param interceptor IFULogInterceptor?
     */
    @JvmStatic
    fun setLogInterceptor(interceptor: ILogInterceptor?) {
        mFuLogInterceptor = interceptor
    }


    /**
     * VERBOSE 输出日志
     * @param tag String TAG标识
     * @param msg String 日志内容
     */
    @JvmStatic
    fun v(tag: String, msg: String) {
        logMsg(LogLevel.VERBOSE, tag, msg)
    }

    /**
     * VERBOSE 输出日志
     * 这种方式输入可以优化大量日志存在 String 对象的构造
     * @param tag String TAG标识
     * @param unit () -> Any? 函数
     */
    @JvmStatic
    fun v(tag: String, unit: () -> Any?) {
        logMsg(LogLevel.VERBOSE, tag, unit)
    }

    /**
     * DEBUG 输出日志
     * @param tag String TAG标识
     * @param msg String 日志内容
     */
    @JvmStatic
    fun d(tag: String, msg: String) {
        logMsg(LogLevel.DEBUG, tag, msg)
    }

    /**
     * DEBUG 输出日志
     * 这种方式输入可以优化大量日志存在 String 对象的构造
     * @param tag String TAG标识
     * @param unit () -> Any? 函数
     */
    @JvmStatic
    fun d(tag: String, unit: () -> Any?) {
        logMsg(LogLevel.DEBUG, tag, unit)
    }

    /**
     * INFO 输出日志
     * @param tag String TAG标识
     * @param msg String 日志内容
     */
    @JvmStatic
    fun i(tag: String, msg: String) {
        logMsg(LogLevel.INFO, tag, msg)
    }

    /**
     * INFO 输出日志
     * 这种方式输入可以优化大量日志存在 String 对象的构造
     * @param tag String TAG标识
     * @param unit () -> Any? 函数
     */
    @JvmStatic
    fun i(tag: String, unit: () -> Any?) {
        logMsg(LogLevel.INFO, tag, unit)
    }

    /**
     * WARN 输出日志
     * @param tag String TAG标识
     * @param msg String 日志内容
     */
    @JvmStatic
    fun w(tag: String, msg: String) {
        logMsg(LogLevel.WARN, tag, msg)
    }

    /**
     * WARN 输出日志
     * 这种方式输入可以优化大量日志存在 String 对象的构造
     * @param tag String TAG标识
     * @param unit () -> Any? 函数
     */
    @JvmStatic
    fun w(tag: String, unit: () -> Any?) {
        logMsg(LogLevel.WARN, tag, unit)
    }

    /**
     * ERROR 输出日志
     * @param tag String TAG标识
     * @param msg String 日志内容
     */
    @JvmStatic
    fun e(tag: String, msg: String) {
        logMsg(LogLevel.ERROR, tag, msg)
    }

    /**
     * ERROR 输出日志
     * 这种方式输入可以优化大量日志存在 String 对象的构造
     * @param tag String TAG标识
     * @param unit () -> Any? 函数
     */
    @JvmStatic
    fun e(tag: String, unit: () -> Any?) {
        logMsg(LogLevel.ERROR, tag, unit)
    }


    /**
     * 打印调用堆栈
     * @param tag String
     * @param msg String
     */
    @JvmStatic
    fun debugPrintStack(tag: String, msg: String) {
        logMsg(LogLevel.INFO, tag, msg)
        logMsg(LogLevel.INFO, tag, Log.getStackTraceString(Throwable()))
    }

    @JvmStatic
    private fun logMsg(level: LogLevel, tag: String, msg: String) {
        if (level.level < mLogLevel.level) {
            return
        }
        when (level) {
            LogLevel.VERBOSE -> Log.v(PREFIX + tag, msg)
            LogLevel.DEBUG -> Log.d(PREFIX + tag, msg)
            LogLevel.INFO -> Log.i(PREFIX + tag, msg)
            LogLevel.WARN -> Log.w(PREFIX + tag, msg)
            LogLevel.ERROR -> Log.e(PREFIX + tag, msg)
            else -> {}
        }
    }

    @JvmStatic
    private fun logMsg(level: LogLevel, tag: String, unit: () -> Any?) {
        if (level.level < mLogLevel.level) {
            return
        }
        if (mFuLogInterceptor?.interceptor(level, PREFIX + tag, unit().toString()) == true) return
        when (level) {
            LogLevel.VERBOSE -> Log.v(PREFIX + tag, unit().toString())
            LogLevel.DEBUG -> Log.d(PREFIX + tag, unit().toString())
            LogLevel.INFO -> Log.i(PREFIX + tag, unit().toString())
            LogLevel.WARN -> Log.w(PREFIX + tag, unit().toString())
            LogLevel.ERROR -> Log.e(PREFIX + tag, unit().toString())
            else -> {}
        }
    }

    /**
     * 日志等级
     * @property level Int
     * @constructor
     */
    enum class LogLevel(internal val level: Int) {
        VERBOSE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), OFF(5);
    }
}