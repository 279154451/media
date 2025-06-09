package com.qt.media.encode.webp

import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.interfaces.IImageEncoder
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * 描述：WEBP 编码
 * TODO 遗留问题：导出的Webp图像倒置
 * @param outputPath 输出路径
 * @param outFps 输出fps
 *
 * @author hexiang on 2023/9/1
 */
class WebpBufferEncoder(outputPath: String, private val outFps: Double = 20.0,val quality:Int = 75, val method:Int = 4) : IImageEncoder(outputPath) {

    private var WebpRecordWrapper: WebpBufferEncoderWrapper? = null

    private var countDownLatch: CountDownLatch? = null


    private fun initWebpRecorder(imageFrame: ImageFrame) {
        getExternalFile(outputPath, true)
        if (WebpRecordWrapper == null) {
            WebpRecordWrapper =
                WebpBufferEncoderWrapper(
                    outputPath,
                    imageFrame.width,
                    imageFrame.height,
                    quality,
                    method
                )
            WebpRecordWrapper?.setFps(outFps)
            WebpRecordWrapper?.setListener { isCancel ->
                if (isCancel) {
                    val file = File(outputPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                countDownLatch?.countDown()
            }
        }
    }

    /**
     * 编码
     */
    override fun encode(textureFrame: TextureFrame) {

    }

    override fun encode(imageFrame: ImageFrame) {
        super.encode(imageFrame)
        initWebpRecorder(imageFrame)
        WebpRecordWrapper?.encodeFrame(imageFrame.buffer, imageFrame.width, imageFrame.height)
    }

    /**
     * 需要在GL线程调用
     */
    override fun finish(): Result<String> {
        countDownLatch = CountDownLatch(1)
        WebpRecordWrapper?.release()
        countDownLatch?.await()
        WebpRecordWrapper = null
        if (File(outputPath).exists()) {
            return Result.success(outputPath)
        }
        return Result.failure(Throwable(""))
    }

    /**
     * 需要在GL线程调用
     */
    override fun cancel() {
        super.cancel()
        WebpRecordWrapper?.cancel()
        countDownLatch?.await()
        WebpRecordWrapper = null
    }

}