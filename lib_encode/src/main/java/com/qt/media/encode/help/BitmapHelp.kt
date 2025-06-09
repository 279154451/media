package com.qt.media.encode.help

import android.graphics.Bitmap
import com.qt.media.encode.image.NV21ToBitmapHelp
import com.libyuv.util.YuvUtil
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.FrameFormat
import com.qt.media.encode.entity.ImageFrame
import java.nio.ByteBuffer

object BitmapHelp {
    fun getBitmapFromImageFrame(imageFrame: ImageFrame):Bitmap?{
        var bitmap: Bitmap? = null
        when (imageFrame.format) {
            FrameFormat.FORMAT_RGBA -> {
                if (imageFrame.buffer!= null){
                    bitmap = Bitmap.createBitmap(imageFrame.width, imageFrame.height, Bitmap.Config.ARGB_8888)
                    bitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(imageFrame.buffer!!))
                }
            }

            FrameFormat.FORMAT_NV21 -> {
                val nv21Tool = NV21ToBitmapHelp(
                    ReflectApplication.application
                )
                bitmap = nv21Tool.nv21ToBitmap(imageFrame.buffer, imageFrame.width, imageFrame.height)
            }
            FrameFormat.FORMAT_NV12->{
                val argb = ByteArray(imageFrame.width*imageFrame.height*4)
                YuvUtil.NV21ToARGB(imageFrame.buffer,argb,imageFrame.width,imageFrame.height)//因为经过测试发现nv12使用nv21接口转换没问题，使用nv12接口转换会有问题
                bitmap = Bitmap.createBitmap(imageFrame.width, imageFrame.height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argb))
            }

            else -> {
                FULogger.d("BitmapHelp","not support ${imageFrame.format} buffer to Bitmap")
                bitmap = null
            }
        }
        return bitmap
    }
}