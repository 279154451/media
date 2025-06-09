package com.qt.media.encode.image

import android.graphics.Bitmap
import android.util.Log
import com.faceunity.ImageCodec.ImageCodecAPI
import com.faceunity.ImageCodec.ImageCodecAPI.FuImageCodecErrorCode
import com.faceunity.ImageCodec.ImageEncoderWebp
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.interfaces.IImageEncoder
import java.io.File
import java.nio.ByteBuffer


/**
 * 描述：webp编码
 *
 * @param outputPath 输出路劲
 * @param fps 帧率
 * @param quality 质量 取值范围0~100
 * @param method webp压缩参数，取值范围0~6，值越大压缩速度越慢，文件越小
 */
class WebpImageEncoder(outputPath: String, val fps: Int = 25, val quality:Int = 75, val method:Int = 4) : IImageEncoder(outputPath) {
    private val TAG = "FUWebpImageEncoder"
    private var encoder: ImageEncoderWebp? = null
    private var recordTime = 0L
    private var sysTime = 0L
    /**
     * 获取 Bitmap 的 RGBA 字节数组
     *
     * @param bitmap Bitmap 图像
     * @return  RGBA 字节数组
     */
    private fun readRgbaByteFromBitmap(bitmap: Bitmap): ByteArray? {
        if (bitmap.byteCount == 0) {
            return null
        }
        val bytes = ByteArray(bitmap.byteCount)
        val rgbaBuffer = ByteBuffer.wrap(bytes)
        bitmap.copyPixelsToBuffer(rgbaBuffer)
        return bytes
    }
    override fun encode(textureFrame: TextureFrame) {
        mCanceled = false
        val file = File(outputPath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        val bitmap = preEncode(textureFrame)
        val errorCode = FuImageCodecErrorCode()
        if (encoder == null) {
            val sdkVersion = ImageCodecAPI.SDKVersion()
            val sdkCommitTime = ImageCodecAPI.SDKCommitTime()
            val sdkCommitHash = ImageCodecAPI.SDKCommitHash()
            Log.d(
                TAG,
                "WebpEncoder sdkVersion=$sdkVersion sdkCommitTime=$sdkCommitTime sdkCommitHash=$sdkCommitHash"
            )
            ImageCodecAPI.SetLoggerLevel(ImageCodecAPI.EFuImageCodecLoggerLevel.FU_IMAGE_CODEC_LOG_LEVEL_VERBOSE)
            encoder = ImageEncoderWebp.Create(errorCode)
            checkError("Create", errorCode)
            encoder?.SetImageWidth(bitmap.width, errorCode)
            checkError("SetImageWidth", errorCode)
            encoder?.SetImageHeight(bitmap.height, errorCode)
            checkError("SetImageHeight", errorCode)
            encoder?.SetImageQuality(quality,errorCode)
            checkError("SetImageQuality", errorCode)
            encoder?.SetMethod(method,errorCode)
            checkError("SetMethod", errorCode)
            encoder?.SetImageWritePath(outputPath, errorCode)
        }

        if (sysTime == 0L) {
            sysTime = System.currentTimeMillis()
        }
        val costTime = (System.currentTimeMillis() - sysTime) * 3 / 4
        recordTime += costTime
        sysTime = System.currentTimeMillis()
        var duration_time_ms = (1000.0 / fps).toInt()
        val durations: IntArray? = encoder?.GetImageDurationsFromFps(fps.toFloat(), errorCode)
        if (checkError(
                "GetImageDurationsFromFps",
                errorCode
            ) && durations != null && durations.size > 0
        ) {
            duration_time_ms = durations[0]
        }
        encoder?.EncodeImage(
            readRgbaByteFromBitmap(bitmap),
            bitmap.width,
            bitmap.height,
            duration_time_ms,
            errorCode
        );
        checkError("EncodeImage", errorCode);
    }

    private fun checkError(method: String, errorCode: FuImageCodecErrorCode): Boolean {
        if (errorCode.ec == ImageCodecAPI.EFuImageCodecErrorCode.FU_IMAGE_CODEC_ERROR_SUCCESS) {
            return true
        }
        Log.e(TAG, method + " error " + errorCode.ec)
        return false
    }

    override fun encode(imageFrame: ImageFrame) {
        super.encode(imageFrame)
        val file = File(outputPath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        mCanceled = false
        val bitmap: Bitmap? = imageFrame.getImageBitmap()
        if (bitmap != null) {
            val errorCode = FuImageCodecErrorCode()
            if (encoder == null) {
                val sdkVersion = ImageCodecAPI.SDKVersion()
                val sdkCommitTime = ImageCodecAPI.SDKCommitTime()
                val sdkCommitHash = ImageCodecAPI.SDKCommitHash()
                Log.d(
                    TAG,
                    "WebpEncoder sdkVersion=$sdkVersion sdkCommitTime=$sdkCommitTime sdkCommitHash=$sdkCommitHash"
                )
                ImageCodecAPI.SetLoggerLevel(ImageCodecAPI.EFuImageCodecLoggerLevel.FU_IMAGE_CODEC_LOG_LEVEL_VERBOSE)
                encoder = ImageEncoderWebp.Create(errorCode)
                checkError("Create", errorCode)
                encoder?.SetImageWidth(bitmap.width, errorCode)
                checkError("SetImageWidth", errorCode)
                encoder?.SetImageHeight(bitmap.height, errorCode)
                checkError("SetImageHeight", errorCode)
                encoder?.SetImageQuality(quality,errorCode)
                checkError("SetImageQuality", errorCode)
                encoder?.SetMethod(method,errorCode)
                checkError("SetMethod", errorCode)
//            mWebpRecordPath = getExternalRootWebpFilePath()
                encoder?.SetImageWritePath(outputPath, errorCode)
            }

            if (sysTime == 0L) {
                sysTime = System.currentTimeMillis()
            }
            val costTime = (System.currentTimeMillis() - sysTime) * 3 / 4
            recordTime += costTime
            sysTime = System.currentTimeMillis()
            var duration_time_ms = (1000.0 / fps).toInt()
            val durations: IntArray? = encoder?.GetImageDurationsFromFps(fps.toFloat(), errorCode)
            if (checkError(
                    "GetImageDurationsFromFps",
                    errorCode
                ) && durations != null && durations.size > 0
            ) {
                duration_time_ms = durations[0]
            }
            encoder?.EncodeImage(
                readRgbaByteFromBitmap(bitmap),
                bitmap.width,
                bitmap.height,
                duration_time_ms,
                errorCode
            );
            checkError("EncodeImage", errorCode);
        } else {
            FULogger.e("FUPngCpuEncoder", "encode cpu 2 bitmap error ")
        }
    }

    override fun encode(rgbaBuffer: ByteArray, width: Int, height: Int) {
        super.encode(rgbaBuffer, width, height)
        mCanceled = false
        val file = File(outputPath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        val errorCode = FuImageCodecErrorCode()
        if (encoder == null) {
            val sdkVersion = ImageCodecAPI.SDKVersion()
            val sdkCommitTime = ImageCodecAPI.SDKCommitTime()
            val sdkCommitHash = ImageCodecAPI.SDKCommitHash()
            Log.d(
                TAG,
                "WebpEncoder sdkVersion=$sdkVersion sdkCommitTime=$sdkCommitTime sdkCommitHash=$sdkCommitHash"
            )
            ImageCodecAPI.SetLoggerLevel(ImageCodecAPI.EFuImageCodecLoggerLevel.FU_IMAGE_CODEC_LOG_LEVEL_VERBOSE)
            encoder = ImageEncoderWebp.Create(errorCode)
            checkError("Create", errorCode)
            encoder?.SetImageWidth(width, errorCode)
            checkError("SetImageWidth", errorCode)
            encoder?.SetImageHeight(height, errorCode)
            checkError("SetImageHeight", errorCode)
            encoder?.SetImageQuality(quality,errorCode)
            checkError("SetImageQuality", errorCode)
            encoder?.SetMethod(method,errorCode)
            checkError("SetMethod", errorCode)
//            mWebpRecordPath = getExternalRootWebpFilePath()
            encoder?.SetImageWritePath(outputPath, errorCode)
        }

        if (sysTime == 0L) {
            sysTime = System.currentTimeMillis()
        }
        val costTime = (System.currentTimeMillis() - sysTime) * 3 / 4
        recordTime += costTime
        sysTime = System.currentTimeMillis()
        var duration_time_ms = (1000.0 / fps).toInt()
        val durations: IntArray? = encoder?.GetImageDurationsFromFps(fps.toFloat(), errorCode)
        if (checkError(
                "GetImageDurationsFromFps",
                errorCode
            ) && durations != null && durations.size > 0
        ) {
            duration_time_ms = durations[0]
        }
        encoder?.EncodeImage(
            rgbaBuffer,
            width,
            height,
            duration_time_ms,
            errorCode
        )
        checkError("EncodeImage", errorCode);
    }

    override fun finish(): Result<String> {
        recordTime += System.currentTimeMillis() - sysTime
        val file = File(outputPath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        val errorCode = FuImageCodecErrorCode()
        encoder?.Finish(errorCode)
        checkError("Finish", errorCode)
        encoder?.Destroy(errorCode)
        checkError("Destroy", errorCode)
        encoder = null
        if (File(outputPath).exists()) {
            return Result.success(outputPath)
        }
        return Result.failure(Throwable(""))
    }

}