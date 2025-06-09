package com.qt.media.test.ui

import android.os.Bundle
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.FrameFormat
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.gif.GifBufferEncoder
import com.qt.media.encode.help.FileUtils
import com.qt.media.encode.image.WebpImageEncoder
import com.qt.media.encode.interfaces.IImageEncoder
import com.qt.media.test.databinding.ActivityImageBinding


/**
 *
 * DESC：
 * Created on 2021/10/26
 *
 */
class ImageEncoderActivity : AppCompatActivity() {

    /**
     * 视图
     */
    private lateinit var mBinding: ActivityImageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initData()
        bindListener()
    }

    private fun initData() {
        
    }

    private fun bindListener() {
        mBinding.btnStart.setOnClickListener {
            startEncoder()
        }
    }
    var imageEncoder: IImageEncoder? = null
    private var encoderStartTime:Long = 0
    private fun startEncoder(){
        val path = "${FileUtils.getExternalRootFileDir()}/image/${System.currentTimeMillis()}.webp"
        imageEncoder = WebpImageEncoder(path, fps = 30).apply {
            encoderStartTime = System.currentTimeMillis()
            mBinding.root.post {
                mBinding.txtStatus.text = "录制中"
            }
        }
        BitmapBufferPool.getBitmapBuffer(1080,1920)?.let { buffer ->
            val imageFrame = ImageFrame(
                buffer,
                format = FrameFormat.FORMAT_RGBA,
                width = 1080,
                height = 1920
            )//这里示例代码就采用三原色进行录制验证
            imageEncoder?.encode(imageFrame)//encode
        }
        mBinding.root.post {
            mBinding.txtStatus.text = "录制中"
        }
        imageEncoder?.finish()?.onSuccess {
            encoderStartTime = 0
            FULogger.d("WebpEncoder", " finishEncoder outPath:$it")
            mBinding.root.post {
                mBinding.txtStatus.text = "录制结束"
            }
        }?.onFailure {
            FULogger.d("WebpEncoder", " finishEncoder:$it")
        }
    }




}