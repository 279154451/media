package com.qt.camera.log


interface ILogInterceptor {
    /**
     * 拦截日志信息
     * @param level LogLevel
     * @param tag String
     * @param message String
     */
    fun interceptor(level: FULogger.LogLevel, tag: String, message: String): Boolean
}