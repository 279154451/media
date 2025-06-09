package com.qt.media.encode.help

import android.os.SystemClock

class LimitFpsHelper {
    private val mDefaultFPS = 30
    private var mFPS = 30
    private var mFrameStartTimeMs: Long = 0
    private var mExpectedFrameTimeMs = (1000 / mDefaultFPS).toLong()

    /**
     * 设置帧率
     *
     * @param fps
     */
    fun setTargetFps(fps: Int) {
        mFPS = fps
        mExpectedFrameTimeMs = if (fps > 0) (1000 / fps).toLong() else 0.toLong()
        mFrameStartTimeMs = 0
    }

    /**
     * 根据帧率做延迟等待
     */
    fun limitFrameRate() {
        val elapsedFrameTimeMs = SystemClock.elapsedRealtime() - mFrameStartTimeMs
        val timeToSleepMs = mExpectedFrameTimeMs - elapsedFrameTimeMs
        if (timeToSleepMs > 0) {
            SystemClock.sleep(timeToSleepMs)
        }
        mFrameStartTimeMs = SystemClock.elapsedRealtime()
    }
    fun getFps():Int{
        return mFPS
    }
}