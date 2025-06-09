package com.qt.media.encode.video.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import com.qt.media.encode.entity.ImageFrame
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue


/**
 *nv12数据视频编码
 * 但是在有些机型上有硬件编码适配性问题
 */
class MediaVideoNV12Encoder(
    muxer: MediaMuxerWrapper,
    listener: MediaEncoderListener?,
    private val mWidth: Int = 0,
    private val mHeight: Int = 0,
    private val presentationTimeUsByPtsUs: Boolean = true
) : MediaEncoder(muxer, listener) {
    private var mVideoExportThread: VideoExportThread? = null
    private val bufferQueue: ConcurrentLinkedQueue<ImageFrame> = ConcurrentLinkedQueue()
    private var config: Map<String, Int>? = null
    @Throws(IOException::class)
    override fun prepare() {
        try {
            if (DEBUG) {
                Log.i(TAG, "prepare: ")
            }
            mTrackIndex = -1
            mIsEOS = false
            mMuxerStarted = mIsEOS
            val codecInfo = selectCodec(MIME_TYPE)
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for $MIME_TYPE")
                return
            }
            Log.d(TAG, "found codec: " + codecInfo.name)
            val colorFormat = selectColorFormat(codecInfo, MIME_TYPE)
            Log.d(TAG, "found colorFormat: $colorFormat")
            val format = MediaFormat.createVideoFormat(
                MIME_TYPE,
                mWidth,
                mHeight
            )//注意：这里的宽高要和等会录制的数据大小一致，否则可能会出现花屏和分屏异常效果
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)//编码器输入格式NV12
//            format.setInteger(
//                MediaFormat.KEY_COLOR_FORMAT,
//                CodecCapabilities.COLOR_FormatYUV420Flexible
//            )//编码器输入格式NV12

//            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
//            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
//            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
//            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
            // 设置比特率控制模式为 CBR 或 VBR
//            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR); // 设置为 VBR
//            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR); // 设置为 CBR
            format.setInteger(MediaFormat.KEY_BIT_RATE, getValueOrDefault(MediaFormat.KEY_BIT_RATE,calcBitRate()))
            format.setInteger(MediaFormat.KEY_FRAME_RATE, getValueOrDefault(MediaFormat.KEY_FRAME_RATE,FRAME_RATE))
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getValueOrDefault(MediaFormat.KEY_I_FRAME_INTERVAL,1))
            if (DEBUG) {
                Log.i(TAG, "format: $format")
            }
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec.start()
            if (DEBUG) {
                Log.i(TAG, "prepare finishing")
            }
            if (mListener != null) {
                try {
                    mListener.onPrepared(this)
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "prepare:", e)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            release()
        }
    }
    fun setMediaFormatConfig(config: Map<String, Int>?) {
        this.config = config
    }

    private fun getValueOrDefault(key: String, defaultValue: Int): Int {
        return config?.get(key)?:defaultValue
    }
    /**
     * 编码nv12 buffer数据
     */
    fun frameAvailableSoon(
        buffer: ImageFrame
    ): Boolean {
        if (mVideoExportThread == null) {
            return false
        }
        return bufferQueue.offer(buffer)
    }

    override fun startRecording() {
        super.startRecording()
        // create and execute video capturing thread using internal mic
        if (mVideoExportThread == null) {
            mVideoExportThread = VideoExportThread().apply {
                start()
            }
        }
    }

    override fun release() {
        mVideoExportThread = null
        super.release()
    }

    var exportVideoCurTime = 0L
        private set

    private inner class VideoExportThread : Thread() {
        override fun run() {
            try {
                if (mIsCapturing) {
                    while (mIsCapturing && !mRequestStop && !mIsEOS) {
                        val imageFrame = bufferQueue.poll()
                        val byteArray = imageFrame?.buffer
                        if (byteArray!= null) {
//                            val byteArray = swapYV12toI420(imageFrame.buffer,imageFrame.width,imageFrame.height)
                            val bufferSize = byteArray.size
                            val byteBuffer = ByteBuffer.wrap(byteArray)
                            // set nv12 data to encoder
                            val presentationTimeUs =
                                if (presentationTimeUsByPtsUs || exportVideoCurTime == 0L) {
                                    ptsUs
                                } else {
                                    System.currentTimeMillis() - exportVideoCurTime
                                }
                            encode(byteBuffer, bufferSize, presentationTimeUs)
                            frameAvailableSoon()
                            exportVideoCurTime = System.currentTimeMillis()
                        }
                    }
//                    frameAvailableSoon()
                }
            } catch (e: Exception) {
                Log.e(TAG, "VideoExportThread#run", e)
            } finally {
                bufferQueue.clear()
            }
            if (DEBUG) {
                Log.v(TAG, "VideoExportThread:finished")
            }
        }

        private fun swapYV12toI420(yv12bytes: ByteArray, width: Int, height: Int): ByteArray {
            val i420bytes = ByteArray(yv12bytes.size)
            for (i in 0 until width * height) i420bytes[i] = yv12bytes[i]
            for (i in width * height until width * height + width / 2 * height / 2) i420bytes[i] =
                yv12bytes[i + width / 2 * height / 2]
            for (i in width * height + width / 2 * height / 2 until width * height + 2 * (width / 2 * height / 2)) i420bytes[i] =
                yv12bytes[i - width / 2 * height / 2]
            return i420bytes
        }

        /**
         * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
         * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV Apply NV21 to
         * I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
         */
        private fun nV21toI420SemiPlanar(
            nv21bytes: ByteArray, i420bytes: ByteArray,
            width: Int, height: Int
        ) {
            val totle = width * height //Y数据的长度
            val nLen = totle / 4 //U、V数据的长度
            System.arraycopy(nv21bytes, 0, i420bytes, 0, totle)
            for (i in 0 until nLen) {
                i420bytes[totle + i] = nv21bytes[totle + 2 * i]
                i420bytes[totle + nLen + i] = nv21bytes[totle + 2 * i + 1]
            }
        }
    }

    //    override fun getPTSUs(): Long {
//        return mBufferInfo.presentationTimeUs
//    }
    private fun calcBitRate(): Int {
        val bitrate =
            (BPP * FRAME_RATE * mWidth * mHeight).toInt()
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f))
        return bitrate
    }

    companion object {
        private val TAG = MediaVideoNV12Encoder::class.java.simpleName
        private const val DEBUG = true
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
//        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_VP8

        // parameters for recording
        private const val FRAME_RATE = 25
        private const val BPP = 0.25f

        /**
         * Returns the first codec capable of encoding the specified MIME type, or null if no
         * match was found.
         */
        private fun selectCodec(mimeType: String): MediaCodecInfo? {
            val numCodecs = MediaCodecList.getCodecCount()
            for (i in 0 until numCodecs) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (!codecInfo.isEncoder) {
                    continue
                }
                val types = codecInfo.supportedTypes
                for (j in types.indices) {
                    if (types[j].equals(mimeType, ignoreCase = true)) {
                        return codecInfo
                    }
                }
            }
            return null
        }

        /**
         * Returns a color format that is supported by the codec and by this test code.  If no
         * match is found, this throws a test failure -- the set of formats known to the test
         * should be expanded for new platforms.
         */
        private fun selectColorFormat(codecInfo: MediaCodecInfo, mimeType: String): Int {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            for (i in capabilities.colorFormats.indices) {
                val colorFormat = capabilities.colorFormats[i]
                if (isRecognizedFormat(colorFormat)) {
                    return colorFormat
                }
            }
            return 0 // not reached
        }
        /**
         * Returns a color format that is supported by the codec and by this test code.  If no
         * match is found, this throws a test failure -- the set of formats known to the test
         * should be expanded for new platforms.
         */
        private fun isSupportNV12(codecInfo: MediaCodecInfo, mimeType: String): Boolean {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            for (i in capabilities.colorFormats.indices) {
                val colorFormat = capabilities.colorFormats[i]
                if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                    // 编解码器支持 NV12 格式
                    Log.d(TAG, "Codec " + codecInfo.getName() + " supports NV12 encoding");
                    return true
                }
            }
            return false
        }
        private fun isSupportYV12(codecInfo: MediaCodecInfo, mimeType: String): Boolean {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            for (i in capabilities.colorFormats.indices) {
                val colorFormat = capabilities.colorFormats[i]
                if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                    // 编解码器支持 YV12 格式
                    Log.d(TAG, "Codec " + codecInfo.getName() + " supports YV12 encoding");
                    return true
                }
            }
            return false
        }
        /**
         * Returns true if this is a color format that this test code understands (i.e. we know how
         * to read and generate frames in this format).
         */
        private fun isRecognizedFormat(colorFormat: Int): Boolean {
            return when (colorFormat) {
                CodecCapabilities.COLOR_FormatYUV420Planar,
                CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
                CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
                CodecCapabilities.COLOR_FormatYUV420Flexible,
                CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
                CodecCapabilities.COLOR_FormatSurface->false
                else -> false
            }
        }

        /**
         * Returns true if the specified color format is semi-planar YUV.  Throws an exception
         * if the color format is not recognized (e.g. not YUV).
         */
        private fun isSemiPlanarYUV(colorFormat: Int): Boolean {
            return when (colorFormat) {
                CodecCapabilities.COLOR_FormatYUV420Planar,
                CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> false
                CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                CodecCapabilities.COLOR_FormatYUV420Flexible,
                CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
                CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
                else -> throw RuntimeException("unknown format $colorFormat")
            }
        }
    }
}