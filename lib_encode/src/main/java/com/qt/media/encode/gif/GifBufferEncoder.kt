package com.qt.media.encode.gif

import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.interfaces.IImageEncoder
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * 描述：GIF 编码
 * TODO 遗留问题：导出的gif图像倒置
 * @param outputPath 输出路径
 * @param outFps 输出fps
 *
 * @author hexiang on 2023/9/1
 */
class GifBufferEncoder(outputPath: String, private val outFps: Double = 20.0) : IImageEncoder(outputPath) {

    private var gifRecordWrapper:GifBufferEncoderWrapper? = null

    private var countDownLatch: CountDownLatch? = null


    private fun initGifRecorder(imageFrame: ImageFrame) {
        getExternalFile(outputPath, true)
        if (gifRecordWrapper == null) {
            gifRecordWrapper = GifBufferEncoderWrapper(
                outputPath,
                imageFrame.width,
                imageFrame.height
            )
            gifRecordWrapper?.setFps(outFps)
            gifRecordWrapper?.setListener { isCancel ->
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
        initGifRecorder(imageFrame)
        gifRecordWrapper?.encodeFrame(imageFrame.buffer, imageFrame.width, imageFrame.height)
    }

    /**
     * 需要在GL线程调用
     */
    override fun finish(): Result<String> {
        countDownLatch = CountDownLatch(1)
        gifRecordWrapper?.release()
        countDownLatch?.await()
        gifRecordWrapper = null
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
        gifRecordWrapper?.cancel()
        countDownLatch?.await()
        gifRecordWrapper = null
    }

}