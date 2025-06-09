package com.qt.media.encode.help

import android.media.AudioFormat
import android.media.AudioRecord

/**
 * 描述：
 *
 * @author hexiang on 2023/9/4
 */
object AudioHelp {

    /**
     * 采样率
     */
    const val DEFAULT_SAMPLE_RATE = 44100

    /**
     * 声道数量
     */
    const val DEFAULT_CHANNEL_COUNT = 2

    /**
     * 声道
     * Channels (声道): 声道配置为 "stereo"，表示有两个声道，即左声道和右声道；momo为单声道
     */
    const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO

    /**
     * 比特率
     * 表示音频数据的传输速率。这告诉您在每秒钟传输的比特数
     */
    const val DEFAULT_BIT_RATE = 128000

    /**
     * 编解码器一次接受的最大输入数据大小
     */
    private var DEFAULT_MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, AudioFormat.ENCODING_PCM_16BIT)


    /**
     * 编解码器一次接受的最大输入数据大小
     */
    @JvmStatic
    fun getDefaultMinBufferSize(): Int = DEFAULT_MIN_BUFFER_SIZE

    private fun mixPcm(sample: ShortArray, vols: FloatArray): Int {
        var temp = 0
        for (i in sample.indices) {
            temp += (sample[i] * vols[i]).toInt()
        }
        if (temp > 32767) {
            temp = 32767
        } else if (temp < -32768) {
            temp = -32768
        }
        return temp
    }

    @JvmStatic
    fun mixPcm(sample: Array<ByteArray?>, vols: FloatArray, trackCount: Int, sample_size: Int): ByteArray? {
        val res = ByteArray(sample_size)
        val tempShort = ShortArray(trackCount)
        var temp: Int
        var i = 0
        while (i < sample_size) {
            for (j in 0 until trackCount) {
                sample[j]?.let {
                    tempShort[j] = (it[i].toInt() and 0xff or (it[i + 1].toInt() and 0xff shl 8)).toShort()
                }
            }
            temp = mixPcm(tempShort, vols)
            res[i] = (temp and 0xFF).toByte()
            res[i + 1] = (temp ushr 8 and 0xFF).toByte()
            i += 2
        }
        return res
    }
}