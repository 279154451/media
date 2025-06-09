package com.qt.media.encode.interfaces

import android.opengl.EGL14
import android.text.TextUtils
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.AudioTrackEntity
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.entity.TextureFrame
import com.qt.media.encode.video.encoder.MediaVideoNV12Encoder
import com.qt.media.encode.video.encoder.MediaBufferVideoEncoder
import com.qt.media.encode.video.encoder.MediaEncoder
import com.qt.media.encode.video.encoder.MediaMuxerWrapper
import com.qt.media.encode.video.encoder.MediaVideoTextureEncoder
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * 音视频编码接口
 */
abstract class IVideoEncoder {

    companion object {
        const val TAG = "VideoEncoder"
    }

    protected var mVideoEncoder: MediaEncoder? = null

    protected var mMuxerWrapper: MediaMuxerWrapper? = null

    protected var mMediaEncoderListener: MediaEncoderListenerImpl? = null
    protected var config: Map<String, Int>? = null
    fun setMediaFormatConfig(config: Map<String, Int>) {
        this.config = config
    }
    /**
     * @property onPreparedUnit 用于切到opengl线程，做gl相关的初始化
     * @property onStoppedUnit 用于切到opengl线程，做gl相关的初始化
     */
    class MediaEncoderListenerImpl(val outputPath: String, private val onPreparedUnit: (() -> Unit) -> Unit,
                                   private val onStoppedUnit: (() -> Unit) -> Unit,
                                  private val onFrameUnit: ((MediaEncoder?, Any?) -> Unit)? = null) :
        MediaEncoder.MediaEncoderListener {

        private var mCountDownLatch: CountDownLatch? = null

        private var mVideoEncoderListener: IVideoEncoderListener? = null

        fun bindCountDownLatch(countDownLatch: CountDownLatch) {
            mCountDownLatch = countDownLatch
        }

        fun bindVideoEncoderListener(videoEncoderListener: IVideoEncoderListener) {
            mVideoEncoderListener = videoEncoderListener
        }

        override fun onPrepared(encoder: MediaEncoder?) {
            if (encoder is MediaVideoTextureEncoder) {
                onPreparedUnit {
                    encoder.setEglContext(EGL14.eglGetCurrentContext())
                    FULogger.d(TAG, "FUMediaVideoTextureEncoder onPrepared encoder.setEglContext(EGL14.eglGetCurrentContext())")
                }
            }else if(encoder is MediaVideoNV12Encoder){
                onPreparedUnit {
                    FULogger.d(TAG, "FUMediaVideoNV12Encoder onPrepared )")
                }
            }else if (encoder is MediaBufferVideoEncoder){
                onPreparedUnit {
                    encoder.setEglContext(EGL14.eglGetCurrentContext())
                    FULogger.d(TAG, "FUMediaBufferVideoEncoder onPrepared )")
                }
            }
        }

        override fun onStopped(encoder: MediaEncoder?) {
            if (encoder is MediaVideoTextureEncoder) {
                onStoppedUnit {
                    encoder.releaseGL()
                }
            }else if (encoder is MediaVideoNV12Encoder){
                onStoppedUnit {
                    FULogger.d(TAG, "FUMediaVideoNV12Encoder onStopped ")
                }
            }else if (encoder is MediaBufferVideoEncoder){
                onStoppedUnit {
                    encoder.releaseGL()
                    FULogger.d(TAG, "FUMediaBufferVideoEncoder onStopped ")
                }
            }
            mCountDownLatch?.countDown()
            //等多个编码器都结束
            if (mCountDownLatch?.count == 0L) {
                FULogger.d(TAG, "Encoder onFinish:$outputPath")
                mVideoEncoderListener?.onFinish(outputPath)
                mCountDownLatch = null
            }
        }

        override fun onFrameRender(encoder: MediaEncoder?, frame: Any?) {
            onFrameUnit?.invoke(encoder,frame)
        }

        override fun onError(encoder: MediaEncoder?, exception: java.lang.Exception?) {

        }

    }


    /**
     * 开始编码，初始化编码器
     *
     * @param outputPath String mp4输出文件路径
     * @param videoWidth Int 视频宽度
     * @param videoHeight Int 视频高度
     * @param videoTotalDuration Float 视频总时长ms
     * @param videoFps Float 视频帧率
     * @param audioEntities List<FUAudioTrackEntity> 音频文件数组
     * @param glThreadUnit 用于切到opengl线程，做gl相关的初始化
     *
     * @return Result<Boolean>
     */
    abstract fun startVideoEncoder(
        outputPath: String, videoWidth: Int, videoHeight: Int, videoTotalDuration: Float, videoFps: Float = 33f,
        audioEntities: List<AudioTrackEntity>,
        onPreparedUnit: (() -> Unit) -> Unit,
        onStoppedUnit: (() -> Unit) -> Unit,
        onFrameUnit: ((MediaEncoder?, Any?) -> Unit)? = null
    ): Result<Boolean>

    /**
     * 将render渲染的纹理编码到MP4中去
     *
     * @param frame openGL渲染的纹理帧
     * @return Result<Boolean>
     */
    open fun encoderTextureFrame2Video(frame: TextureFrame): Result<Boolean>{
        return Result.failure(Throwable("not support encoderTextureFrame2Video "))
    }

    /**
     * 将render渲染的cpu数据编码到MP4中去
     *
     * @param frame cpu数据
     * @return Result<Boolean>
     */
    open fun encoderBufferFrame2Video(frame: ImageFrame): Result<Boolean>{
        return Result.failure(Throwable("not support encoderBufferFrame2Video "))
    }

    /**
     * 结束编码，释放资源
     * @param listener 编码完成回调 成功返回纹理编码后的视频文件路径
     * @return
     */
    abstract fun finishEncoder(listener: IVideoEncoderListener)

    /**
     * 取消编码，释放资源
     * @return
     */
    abstract fun cancel()
    abstract fun cancelSync()


    /**
     * 创建文件夹
     * @param dirPath String 文件夹路径
     * @return Boolean
     */
    internal fun createFileDir(dirPath: String): Boolean {
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else true
    }
    internal  fun getFileParentPath(filePath: String): String {
        if (TextUtils.isEmpty(filePath)) {
            return filePath
        }
        val filePos = filePath.lastIndexOf(File.separator)
        return if (filePos == -1) "" else filePath.substring(0, filePos)
    }
}