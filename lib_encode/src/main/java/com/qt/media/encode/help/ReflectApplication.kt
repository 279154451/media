package com.qt.media.encode.help

import android.annotation.SuppressLint
import android.app.Application

object ReflectApplication {
    @Volatile
    private var mApplication: Application? = null

    /**
     * Application 反射获取
     */
    @JvmStatic
    val application: Application
        get() {
            if (mApplication == null) {
                synchronized(ReflectApplication::javaClass) {
                    if (mApplication == null) {
                        mApplication = reflectionGetApplication()
                    }
                }
            }
            return mApplication!!
        }

    /**
     * 反射获取Application
     * @return Application
     */
    @SuppressLint("PrivateApi")
    private fun reflectionGetApplication(): Application {
        return Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication").invoke(null) as Application
    }
}