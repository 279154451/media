package com.qt.media.encode.help

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import com.qt.media.encode.program.FUProgramTexture2d
import com.qt.media.encode.program.FUProgramTexture2dWithAlpha
import com.qt.media.encode.program.FUProgramTextureOES
import com.qt.media.encode.program.utils.FUGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10


/**
 *
 * DESC：拍照合成
 * Created on 2021/11/1
 *
 */
class EncodeHelper {

    /**
     * ProgramTexture 实例
     */
    private val mFUProgramTextureOES by lazy { FUProgramTextureOES() }
    private val mFUProgramTexture2d by lazy { FUProgramTexture2d() }
    private val mFUProgramTexture2dWithAlpha by lazy { FUProgramTexture2dWithAlpha() }

    /**
     * 业务回调
     */
    private var mOnPhotoRecordingListener: OnPhotoRecordingListener? = null

    /**
     * 绑定回调
     * @param listener OnPhotoRecordingListener?
     */
    fun bindListener(listener: OnPhotoRecordingListener?) {
        mOnPhotoRecordingListener = listener
    }

    /**
     * 释放
     */
    fun release() {
        mOnPhotoRecordingListener = null
        mFUProgramTextureOES.release()
        mFUProgramTexture2d.release()
        mFUProgramTexture2dWithAlpha.release()
    }

    /**
     * 发送录制数据模型
     * @param recordData RecordData
     */
    fun sendRecordingData(recordData: RecordData) {
        val texWidth = recordData.texWidth
        val texHeight = recordData.texHeight
        val texId = recordData.texId
        val texMatrix = recordData.texMatrix
        val mvpMatrix = recordData.mvpMatrix
        val isOes = recordData.isOES
        val isNeedAlpha = recordData.isAlpha

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        val frameBuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffers, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        GLES20.glViewport(0, 0, texWidth, texHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        when {
            isOes -> {
                mFUProgramTextureOES.drawFrame(texId, texMatrix, mvpMatrix)
            }
            !isNeedAlpha -> {
                mFUProgramTexture2d.drawFrame(texId, texMatrix, mvpMatrix)
            }
            else -> {
                mFUProgramTexture2dWithAlpha.drawFrame(texId, texMatrix, mvpMatrix)
            }
        }
        val buffer = ByteBuffer.allocateDirect(texWidth * texHeight * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glFinish()
        GLES20.glReadPixels(0, 0, texWidth, texHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer)
        FUGLUtils.checkGlError("glReadPixels")
        buffer.rewind()
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteTextures(1, textures, 0)
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0)
        if (recordData.isSynchronize) {
            getBitmapFromBuffer(recordData, buffer)
        } else {
            Thread{
                getBitmapFromBuffer(recordData, buffer)
            }.start()
        }
    }

    /**
     * 发送录制数据模型,同步返回
     * @param recordData RecordData
     * @return Bitmap
     */
    fun sendRecordingDataSync(recordData: RecordData): Bitmap {
        val texWidth = recordData.texWidth
        val texHeight = recordData.texHeight
        val texId = recordData.texId
        val texMatrix = recordData.texMatrix
        val mvpMatrix = recordData.mvpMatrix
        val isOes = recordData.isOES
        val isNeedAlpha = recordData.isAlpha

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        val frameBuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffers, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texWidth, texHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        GLES20.glViewport(0, 0, texWidth, texHeight)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        when {
            isOes -> {
                mFUProgramTextureOES.drawFrame(texId, texMatrix, mvpMatrix)
            }
            !isNeedAlpha -> {
                mFUProgramTexture2d.drawFrame(texId, texMatrix, mvpMatrix)
            }
            else -> {
                mFUProgramTexture2dWithAlpha.drawFrame(texId, texMatrix, mvpMatrix)
            }
        }
        val buffer = ByteBuffer.allocateDirect(texWidth * texHeight * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        GLES20.glFinish()
        GLES20.glReadPixels(0, 0, texWidth, texHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer)
        FUGLUtils.checkGlError("glReadPixels")
        buffer.rewind()
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteTextures(1, textures, 0)
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0)
        val bmp = Bitmap.createBitmap(recordData.texWidth, recordData.texHeight, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buffer)
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        val finalBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, false)
        bmp.recycle()
        return finalBmp
    }


    /**
     * 获取Bitmap对象
     * @param recordData RecordData
     * @param buffer ByteBuffer
     */
    private fun getBitmapFromBuffer(recordData: RecordData, buffer: ByteBuffer) {
        val bmp = Bitmap.createBitmap(recordData.texWidth, recordData.texHeight, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buffer)
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        val finalBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, false)
        bmp.recycle()
        mOnPhotoRecordingListener?.onRecordSuccess(finalBmp, recordData.tag)
    }


    /**
     * 录制数据模型
     * @property texId Int 纹理
     * @property texMatrix FloatArray? 纹理矩阵
     * @property mvpMatrix FloatArray? 旋转矩阵
     * @property texWidth Int 纹理宽
     * @property texHeight Int 纹理高
     * @property isOES Boolean 是否是相机原始OES数据
     * @property isAlpha Boolean 是否需要透明通道
     * @property isSynchronize Boolean 是否需要同步返回
     * @property tag String 数据标签
     */
    class RecordData private constructor() {
        /**
         * 纹理id
         */
        var texId = 0

        /**
         * Tex矩阵
         */
        var texMatrix: FloatArray? = null

        /**
         * MVP矩阵
         */
        var mvpMatrix: FloatArray? = null

        /**
         * 纹理高
         */
        var texWidth = 0

        /**
         * 纹理高
         */
        var texHeight = 0

        /**
         * 数据源是否OES格式
         */
        var isOES = false

        /**
         * 是否需要透明通道
         */
        var isAlpha = false

        /**
         * 是否需要同步处理
         */
        var isSynchronize = false

        /**
         * 标签
         */
        var tag = System.currentTimeMillis().toString()

        /**
         * 绑定数据
         * @param texId Int
         * @param texMatrix FloatArray
         * @param mvpMatrix FloatArray
         * @param texWidth Int
         * @param texHeight Int
         * @return Builder
         */
        constructor(texId: Int, texMatrix: FloatArray, mvpMatrix: FloatArray, texWidth: Int, texHeight: Int) : this() {
            this.texId = texId
            this.texMatrix = texMatrix
            this.mvpMatrix = mvpMatrix
            this.texWidth = texWidth
            this.texHeight = texHeight
        }


    }


    interface OnPhotoRecordingListener {

        /**
         * 录制完成回调
         */
        fun onRecordSuccess(bitmap: Bitmap?, tag: String)
    }


}