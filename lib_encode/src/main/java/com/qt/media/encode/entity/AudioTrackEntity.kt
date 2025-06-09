package com.qt.media.encode.entity

import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * 音频数据类
 *
 * @property path String? 音频文件绝对路径
 * @property startTime Float 对应视频的位置 ms
 * @property audioDuration Float 音频的持续时长 ms
 * @property totalAudioDuration Float 音频总时长 ms
 * @property startAudioTime Float 音频的起始位置 ms
 */
class AudioTrackEntity(
    private val path: String? = null,
    private val startTime: Float = 0f,
    private val audioDuration: Float = 0f,
    private val totalAudioDuration: Float = 0f,
    private val startAudioTime: Float = 0f
) {

    /**
     * 音量
     */
    val volume = 1f

    private var isError = false
    private var tempPath: String? = null
    private var mFileInputStream: FileInputStream? = null
    private var index = wav_head_length
    private var buffer: ByteArray? = null


    @Throws(IOException::class)
    fun init() {
        tempPath = path
        isError = tempPath == null || !File(tempPath).exists()
        if (isError) {
            return
        }
        mFileInputStream = FileInputStream(tempPath).apply {
            val size = channel.size()
            val curIndex = (startAudioTime / totalAudioDuration * (size - wav_head_length) + wav_head_length).toInt()
            index = if (curIndex <= 0) curIndex else curIndex / 10 * 10
            //跳到文件中读取音频数据的所需位置
            skip(index.toLong())
        }

    }

    @Throws(IOException::class)
    fun read(cur: Float, buffer_size: Int): ByteArray? {
        if (isError) {
            return null
        }
        if (cur < startTime || cur > startTime + audioDuration || isError || mFileInputStream == null) {
            return null
        }
        if (buffer == null || buffer!!.size != buffer_size) {
            buffer = ByteArray(buffer_size)
        }
        return if (mFileInputStream!!.read(buffer) != -1) {
            buffer
        } else null
    }

    @Throws(IOException::class)
    fun release() {
        mFileInputStream?.close()
    }

    companion object {
        /**
         * WAV 文件头部大小
         * 在处理 WAV 文件时，通常会在文件的开头有一个音频头部（Header）部分，
         * 它包含了文件的格式信息和其他元数据，占用了文件的前若干字节。
         */
        private const val wav_head_length = 44
    }
}