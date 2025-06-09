package com.qt.media.encode.entity

import android.graphics.Bitmap
import com.qt.media.encode.help.BitmapHelp
import java.nio.ByteBuffer

/**
 * 创建时间：2023/8/30
 * 创建人：singleCode
 * 功能描述：openGL渲染后的纹理帧
 * @param buffer rgba数据
 * @param format 纹理帧格式
 * @param width 宽
 * @param height 高
 * @param isKeyFrame 是否输出为关键帧
 * @param uBrightness  亮度控制参数 默认null（建议范围0.5-3.0）
 **/
data class ImageFrame(
    var buffer: ByteArray?,
    val format: FrameFormat,
    val width: Int,
    val height: Int,
    val isKeyFrame:Boolean = false,
    val uBrightness:Float? = null,
    var bitmap: Bitmap? = null
) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "ImageFrame(buffer=$buffer, format=$format, width=$width, height=$height)"
    }

    fun getImageBitmap():Bitmap?{
        if (bitmap== null || bitmap!!.isRecycled){
            bitmap = BitmapHelp.getBitmapFromImageFrame(this)
        }
        return bitmap
    }

    fun release(){
        if (bitmap!= null && !bitmap!!.isRecycled){
            bitmap?.recycle()
        }
        bitmap = null
        buffer = null
    }

}
