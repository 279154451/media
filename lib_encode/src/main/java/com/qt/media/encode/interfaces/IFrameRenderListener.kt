package com.qt.media.encode.interfaces

interface IFrameRenderListener {
    fun renderInputFrameBefore(bitmap: Any?,frameData:Any?)
    fun onInputFrameRender(any: Any?)
}