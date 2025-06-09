package com.qt.media.test

import android.app.Application
import android.util.Log


/**
 *
 * DESC：
 * Created on 2021/12/6
 * @author Jason Lu
 *
 */
class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable -> // 将异常信息保存到日志文件中
            Log.e("AppCrash", Log.getStackTraceString(throwable))

            // 在这里执行其他必要的操作，如关闭应用程序或发送错误报告

            // 退出应用程序
            System.exit(1)
        }
    }
}