package com.qt.media.encode.help

import com.qt.camera.log.FULogger
import com.qt.media.encode.interfaces.IVideoEncoder
import java.util.concurrent.ConcurrentHashMap

object VideoEncoderHelp {
    private val encoderPathMap: ConcurrentHashMap<String, IVideoEncoder> = ConcurrentHashMap()

    fun isPathDeleteEnable(path: String): Boolean {
        val containsKey = encoderPathMap.containsKey(path)
        FULogger.w("VideoEncoderHelp", "isPathDeleteEnable:${!containsKey}")
        return !containsKey
    }

    fun offerEncoderPath(path: String?, encoder: IVideoEncoder) {
        path?.let { encoderPathMap.put(path, encoder) }
    }

    fun pullEncoderPath(path: String?) {
        path?.let { encoderPathMap.remove(path) }
    }
}