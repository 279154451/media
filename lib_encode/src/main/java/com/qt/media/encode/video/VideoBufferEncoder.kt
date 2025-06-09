package com.qt.media.encode.video

import android.util.Log
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.AudioTrackEntity
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.help.VideoEncoderHelp
import com.qt.media.encode.interfaces.IVideoEncoder
import com.qt.media.encode.interfaces.IVideoEncoderListener
import com.qt.media.encode.video.encoder.MediaAudioExportEncoder
import com.qt.media.encode.video.encoder.MediaBufferVideoEncoder
import com.qt.media.encode.video.encoder.MediaEncoder
import com.qt.media.encode.video.encoder.MediaMuxerWrapper
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * 描述：音视频编码
 * 录制buffer数据的同时，将音频文件一起编码到视频中去
 * 会将buffer转成bitmap，然后将bitmap渲染到mediaCodec的inputSurface上
 * 目前支持的buffer格式为：RGBA、NV21
 *
 * @author cizongfa on 2024/2/27
 */
class VideoBufferEncoder() : IVideoEncoder() {


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
     *
     * @see AudioTrackEntity
     * @sample audioEntities val audioTrackExport = FUAudioTrackEntity(path, 4000f, 6000f, 15000f, 0f)
     * @sample audioEntities val audioTrackExport2 = FUAudioTrackEntity(path2, 0f, 4000f, 11000f, 0f)
     */
    override fun startVideoEncoder(
        outputPath: String,
        videoWidth: Int,
        videoHeight: Int,
        videoTotalDuration: Float,
        videoFps: Float,
        audioEntities: List<AudioTrackEntity>,
        onPreparedUnit: (() -> Unit) -> Unit,
        onStoppedUnit: (() -> Unit) -> Unit,
        onFrameUnit: ((MediaEncoder?, Any?) -> Unit)?
    ): Result<Boolean> {
        FULogger.d(TAG, "startVideoEncoder start: $outputPath videoTotalDuration:$videoTotalDuration")
        mMediaEncoderListener = MediaEncoderListenerImpl(outputPath, onPreparedUnit,onStoppedUnit,onFrameUnit)
        VideoEncoderHelp.offerEncoderPath(outputPath,this)
        createFileDir(getFileParentPath(outputPath))
        val outFile = File(outputPath)
        try {
            mMuxerWrapper =
                MediaMuxerWrapper(outFile.absolutePath)

            mMuxerWrapper?.let { muxer ->
                mVideoEncoder = MediaBufferVideoEncoder(
                    muxer,
                    mMediaEncoderListener,
                    videoWidth,
                    videoHeight,
                    videoFps,
                ).apply {
                    setIntervalTime(((1.0f / videoFps) * 1000 * 1000).toLong())
                    setMediaFormatConfig(config)
                }

                if (audioEntities.isNotEmpty()) {
                    MediaAudioExportEncoder(muxer, mMediaEncoderListener, audioEntities, videoTotalDuration)
                }

                mMediaEncoderListener?.bindCountDownLatch(CountDownLatch(muxer.encoderCount))

                muxer.prepare()
                muxer.startRecording()
                Log.d(TAG, "startVideoEncoder end")
                return Result.success(true)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            FULogger.d(TAG, "startVideoEncoder end")
            return Result.failure(e)
        }
        FULogger.d(TAG, "startVideoEncoder end")
        return Result.failure(Exception("startVideoEncoder failed"))
    }

    /**
     * 将render渲染的纹理编码到MP4中去
     *
     * @param frame openGL渲染的纹理帧
     * @return Result<Boolean>
     */
    override fun encoderBufferFrame2Video(frame: ImageFrame): Result<Boolean> {
        FULogger.d(TAG, "encoderBufferFrame2Video")
        if(mVideoEncoder!= null && mVideoEncoder is MediaBufferVideoEncoder){
            val flag = (mVideoEncoder as MediaBufferVideoEncoder).frameAvailableSoon(frame)
            return Result.success(flag)
        }
        return Result.failure(Throwable("not support encoderBufferFrame2Video "))
    }

    /**
     * 结束编码，释放资源
     * @param listener 编码完成回调 成功返回纹理编码后的视频文件路径
     * @return
     */
    override fun finishEncoder(listener: IVideoEncoderListener) {
        FULogger.d(TAG, "finishEncoder")
        VideoEncoderHelp.pullEncoderPath(mMediaEncoderListener?.outputPath)
        mMediaEncoderListener?.bindVideoEncoderListener(listener)
        mVideoEncoder?.finishEncoder(mMuxerWrapper,false)
    }

    /**
     * 取消编码
     */
    override fun cancel() {
        FULogger.d(TAG, "cancel")
        VideoEncoderHelp.pullEncoderPath(mMediaEncoderListener?.outputPath)
        mMediaEncoderListener?.bindVideoEncoderListener(object : IVideoEncoderListener {
            override fun onFinish(outPath: String) {
                //删除文件
                val isPathDeleteEnable = VideoEncoderHelp.isPathDeleteEnable(outPath)
                FULogger.d(TAG, "cancel outPath:$outPath isPathDeleteEnable:${isPathDeleteEnable}")
                val file = File(outPath)
                if (file.exists() && isPathDeleteEnable) {
                    FULogger.d(TAG, "cancel file.exists() delete")
                    file.delete()
                }
            }

        })
        mVideoEncoder?.cancel(mMuxerWrapper)
    }

    override fun cancelSync() {
        val count = CountDownLatch(1)
        FULogger.d(TAG, "cancelSync")
        VideoEncoderHelp.pullEncoderPath(mMediaEncoderListener?.outputPath)
        mMediaEncoderListener?.bindVideoEncoderListener(object : IVideoEncoderListener {
            override fun onFinish(outPath: String) {
                //删除文件
                val isPathDeleteEnable = VideoEncoderHelp.isPathDeleteEnable(outPath)
                FULogger.d(TAG, "cancelSync outPath:$outPath isPathDeleteEnable:${isPathDeleteEnable}")
                val file = File(outPath)
                if (file.exists() && isPathDeleteEnable) {
                    FULogger.d(TAG, "cancelSync file.exists() delete")
                    file.delete()
                }
                count.countDown()
            }
        })
        mVideoEncoder?.cancel(mMuxerWrapper)
        count.await(2,TimeUnit.SECONDS)
    }
}