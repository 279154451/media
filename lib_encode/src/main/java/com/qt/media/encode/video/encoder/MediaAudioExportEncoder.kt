package com.qt.media.encode.video.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import com.qt.media.encode.entity.AudioTrackEntity
import com.qt.media.encode.help.AudioHelp
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Arrays

/**
 * 音频编码
 * @param mAudioTrackEntities 音频文件数组 需要固定格式的音频文件：采样率44100，左右双声道，16bit
 * @see AudioHelp
 * @param mVideoTotalDuration 视频的总长度
 */
class MediaAudioExportEncoder(
    muxer: MediaMuxerWrapper,
    listener: MediaEncoderListener?,
    private val mAudioTrackEntities: List<AudioTrackEntity>,
    private val mVideoTotalDuration: Float
) : MediaEncoder(muxer, listener) {

    private var mAudioExportThread: AudioExportThread? = null
    private var step = 0f
    var exportAudioCurTime = 0f
        private set

    @Throws(IOException::class)
    override fun prepare() {
        if (DEBUG) {
            Log.v(TAG, "prepare:")
        }
        mTrackIndex = -1
        mIsEOS = false
        mMuxerStarted = mIsEOS
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        val audioCodecInfo = selectAudioCodec(MediaFormat.MIMETYPE_AUDIO_AAC)
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MediaFormat.MIMETYPE_AUDIO_AAC)
            return
        }
        if (DEBUG) {
            Log.i(TAG, "selected codec: " + audioCodecInfo.name)
        }
        val audioFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AudioHelp.DEFAULT_SAMPLE_RATE, AudioHelp.DEFAULT_CHANNEL_COUNT)
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioHelp.DEFAULT_CHANNEL_CONFIG)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioHelp.DEFAULT_BIT_RATE)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AudioHelp.getDefaultMinBufferSize() / 4 * 2)
        //        audioFormat.setLong(MediaFormat.KEY_DURATION, (long) mDuration);
        if (DEBUG) {
            Log.i(TAG, "format: $audioFormat")
        }
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec.start()
        if (DEBUG) {
            Log.i(TAG, "prepare finishing")
        }
        if (mListener != null) {
            try {
                mListener.onPrepared(this)
            } catch (e: Exception) {
                Log.e(TAG, "prepare:", e)
            }
        }
    }

    override fun startRecording() {
        super.startRecording()
        // create and execute audio capturing thread using internal mic
        if (mAudioExportThread == null) {
            mAudioExportThread = AudioExportThread().apply {
                start()
            }
        }
    }

    override fun release() {
        mAudioExportThread = null
        super.release()
        try {
            for (export in mAudioTrackEntities) {
                export.release()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private inner class AudioExportThread : Thread() {
        override fun run() {
            try {
                val bufferSize = AudioHelp.getDefaultMinBufferSize() / 4 * 2
                if (mIsCapturing) {
                    for (export in mAudioTrackEntities) {
                        export.init()
                    }
                    val mInputBuffer: ByteBuffer = ByteBuffer.allocateDirect(bufferSize + 16)
                    val sampleBytes: Array<ByteArray?> = Array(mAudioTrackEntities.size) { ByteArray(bufferSize) }
                    val sampleBytesEmpty: ByteArray = ByteArray(bufferSize)
                    //bufferSize 对应大小的音频数据 对应的时长 为 step
                    step = 1000f * bufferSize / AudioHelp.DEFAULT_CHANNEL_COUNT / 2 / AudioHelp.DEFAULT_SAMPLE_RATE
                    exportAudioCurTime = 0f
                    while (mIsCapturing && !mRequestStop && !mIsEOS && exportAudioCurTime < mVideoTotalDuration) {
                        var numTrack = 0
                        val vols = FloatArray(mAudioTrackEntities.size)
                        Arrays.fill(vols, 1f)
                        for (i in mAudioTrackEntities.indices) {
                            val export = mAudioTrackEntities[i]
                            sampleBytes[numTrack] = null
                            sampleBytes[numTrack] = export.read(exportAudioCurTime, bufferSize)
                            if (sampleBytes[numTrack] != null) {
                                vols[numTrack] = export.volume
                                numTrack++
                            }
                        }
                        mInputBuffer.position(0)
                        mInputBuffer.clear()
                        if (numTrack == 0) {
                            mInputBuffer.put(sampleBytesEmpty)
                        } else if (numTrack == 1 && vols[0] == 1f) {
                            mInputBuffer.put(sampleBytes[0])
                        } else {
                            //这里一定满足 sampleBytes[i] != null
                            mInputBuffer.put(AudioHelp.mixPcm(sampleBytes, vols, numTrack, bufferSize))
                        }
                        mInputBuffer.position(bufferSize)
                        mInputBuffer.flip()
                        encode(mInputBuffer, bufferSize, (exportAudioCurTime * 1000).toLong())
                        frameAvailableSoon()
                        exportAudioCurTime += step
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioExportThread#run", e)
            }
            if (DEBUG) {
                Log.v(TAG, "AudioExportThread:finished")
            }
        }
    }

    override fun getPTSUs(): Long {
        return mBufferInfo.presentationTimeUs
    }

    companion object {
        private val TAG = MediaAudioRecordEncoder::class.java.simpleName
        private const val DEBUG = true

        /**
         * select the first codec that match a specific MIME type
         *
         * @param mimeType
         * @return
         */
        private fun selectAudioCodec(mimeType: String): MediaCodecInfo? {
            if (DEBUG) {
                Log.v(TAG, "selectAudioCodec:")
            }
            var result: MediaCodecInfo? = null
            // get the list of available codecs
            val numCodecs = MediaCodecList.getCodecCount()
            LOOP@ for (i in 0 until numCodecs) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (!codecInfo.isEncoder) {    // skipp decoder
                    continue
                }
                val types = codecInfo.supportedTypes
                for (j in types.indices) {
                    if (DEBUG) {
                        Log.i(TAG, "supportedType:" + codecInfo.name + ",MIME=" + types[j])
                    }
                    if (types[j].equals(mimeType, ignoreCase = true)) {
                        if (result == null) {
                            result = codecInfo
                            break@LOOP
                        }
                    }
                }
            }
            return result
        }
    }
}