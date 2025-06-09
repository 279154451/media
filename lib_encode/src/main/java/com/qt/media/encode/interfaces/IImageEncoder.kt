package com.qt.media.encode.interfaces

import android.graphics.Bitmap
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.FrameFormat
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.help.EncodeHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 描述：图片编码接口
 *
 * @author hexiang on 2023/8/31
 */
abstract class IImageEncoder(val outputPath: String) {

    /**
     * 纹理处理
     */
    internal val mPhotoHelper = EncodeHelper()

    /**
     * 是否取消
     */
    protected var mCanceled: Boolean = false

    /**
     * 编码前预处理：根据纹理生成bitmap
     */
    internal fun preEncode(textureFrame: TextureFrame): Bitmap {
        mCanceled = false
        val data = EncodeHelper.RecordData(
            textureFrame.textureId,
            textureFrame.texMatrix,
            textureFrame.mvpMatrix,
            textureFrame.width,
            textureFrame.height
        )
        data.isOES = textureFrame.format == FrameFormat.FORMAT_OES
        data.isAlpha = textureFrame.format == FrameFormat.FORMAT_BGRA || textureFrame.format == FrameFormat.FORMAT_RGBA

        return mPhotoHelper.sendRecordingDataSync(data)
    }

    /**
     * 进行编码
     */
    abstract fun encode(textureFrame: TextureFrame)
    open fun encode(imageFrame: ImageFrame){

    }
    open fun encode(rgbaBuffer:ByteArray,width:Int,height:Int){

    }

    /**
     * 结束编码
     */
    open fun finish(): Result<String>{
        return Result.failure(Throwable())
    }

    /**
     * 取消编码
     */
    open fun cancel() {
        mCanceled = true
    }
    /**
     * 将 Bitmap 保存到本地
     * @param targetFilePath String 本地路径
     * @param bitmap Bitmap 图像
     * @param isJpeg Boolean 是否是JPEG格式
     * @return Boolean 是否执行成功
     */
    internal fun addBitmapToExternal(targetFilePath: String, bitmap: Bitmap, isJpeg: Boolean): Boolean {
        val file: File = getExternalFile(targetFilePath, true)
        var fos: FileOutputStream? = null
        return try {
            fos = FileOutputStream(file)
            bitmap.compress(if (isJpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            true
        } catch (e: IOException) {
            FULogger.e("IFUImageEncoder") { "addBitmapToExternal failed, targetPath:$targetFilePath message:${e.message}" }
            false
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                FULogger.e("IFUImageEncoder") { "fos close failed, message:${e.message}" }
            }
        }
    }
    internal fun getExternalFile(filePath: String, needDeleteOriginal: Boolean = false): File {
        val targetFile = File(filePath)
        targetFile.parent?.let {
            createFileDir(it)
        }
        if (needDeleteOriginal && targetFile.exists()) {
            targetFile.delete()
        }
        return targetFile
    }
    internal fun createFileDir(dirPath: String): Boolean {
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else true
    }
}