package com.qt.media.encode.image

import android.graphics.Bitmap
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.interfaces.IImageEncoder
import java.io.File

/**
 * 描述：PNG编码
 *
 */
class PngEncoder(outputPath: String) : IImageEncoder(outputPath) {
    val TAG:String = "FUPngEncoder"

    override fun encode(textureFrame: TextureFrame) {

        val bitmap = preEncode(textureFrame)

        addBitmapToExternal(outputPath, bitmap, false)
    }
    override fun encode(imageFrame: ImageFrame) {
        super.encode(imageFrame)
        mCanceled = false
        val bitmap: Bitmap? = imageFrame.getImageBitmap()
        if(bitmap!= null){
            addBitmapToExternal(outputPath, bitmap, false)
        }else{
            FULogger.e("FUPngCpuEncoder","encode cpu 2 bitmap error ")
        }
    }
    override fun finish(): Result<String> {

        if (File(outputPath).exists()) {
            return Result.success(outputPath)
        }

        return Result.failure(Throwable(""))
    }

}