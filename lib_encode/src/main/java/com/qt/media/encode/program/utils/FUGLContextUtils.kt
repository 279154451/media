package com.qt.program.utils

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import com.qt.camera.log.FULogger

/**
 *
 * DESC：opengl GLContext 工具类
 * Created on 2022/12/16
 * @author Jason Lu
 *
 */
object FUGLContextUtils {

    /**
     * GL 数据封装
     * @property display EGLDisplay
     * @property context EGLContext
     * @constructor
     */
    data class EGLContextData(
        val display: EGLDisplay,
        val context: EGLContext,
        val eglConfig: EGLConfig?,
    )

    private const val TAG = "FUGLContextUtils"

    /**
     * 创建 GL 上下文
     * @param version Int
     * @return EGLContextData
     */
    @JvmStatic
    fun createGLContext(version: Int = 3): EGLContextData {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val versionArray = IntArray(2)
        EGL14.eglInitialize(display, versionArray, 0, versionArray, 1)
        val configAttrib = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, 4,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        EGL14.eglChooseConfig(display, configAttrib, 0, configs, 0, configs.size, numConfig, 0)
        EGL14.eglBindAPI(EGL14.EGL_OPENGL_API)
        val attribList = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE)
        val eglConfig = configs[0]
        val context = EGL14.eglCreateContext(display, eglConfig, EGL14.EGL_NO_CONTEXT, attribList, 0)
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, context)
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            FULogger.e(TAG, "createGLContext failed, error:$error")
        }
        return EGLContextData(display, context, eglConfig)
    }

    /**
     * 销毁 EGLContext
     * @param data EGLContextData
     */
    @JvmStatic
    fun destroyGLContext(data: EGLContextData) {
        EGL14.eglMakeCurrent(data.display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (!EGL14.eglDestroyContext(data.display, data.context)) {
            val error = EGL14.eglGetError()
            FULogger.e(TAG, "destroyGLContext failed, error:$error")
        }
    }


    /**
     * 通过其他 EGLContext 创建共享上下文
     * @param context EGLContext
     * @param version Int
     * @param display EGLDisplay
     * @param eglConfig EGLConfig
     * @return EGLContext
     */
    @JvmStatic
    fun sharedEGLContext(context: EGLContext, version: Int, display: EGLDisplay, eglConfig: EGLConfig): EGLContext {
        val attribList = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE)
        val res = EGL14.eglCreateContext(display, eglConfig, context, attribList, 0)
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            FULogger.e(TAG, "sharedEGLContext failed, error:$error")
        }
        return res
    }
}