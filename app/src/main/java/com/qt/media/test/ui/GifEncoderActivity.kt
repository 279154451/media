package com.qt.media.test.ui

import android.os.Bundle
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import com.qt.camera.log.FULogger
import com.qt.media.encode.entity.FrameFormat
import com.qt.media.encode.entity.ImageFrame
import com.qt.media.encode.gif.GifBufferEncoder
import com.qt.media.encode.help.FileUtils
import com.qt.media.encode.interfaces.IImageEncoder
import com.qt.media.encode.webp.WebpBufferEncoder
import com.qt.media.test.databinding.ActivityGifBinding


/**
 *
 * DESC：
 * Created on 2021/10/26
 *
 */
class GifEncoderActivity : AppCompatActivity() {

    /**
     * 视图
     */
    private lateinit var mBinding: ActivityGifBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityGifBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initData()
        bindListener()
    }

    private fun initData() {
        
    }

    private fun bindListener() {
        mBinding.btnStart.setOnClickListener {
            if (!startEncoder){
                startEncoder()
            }
        }
        mBinding.btnEnd.setOnClickListener {
            stopEncoder()
        }
    }
    private var startEncoder:Boolean = false
    private var workHandler:WorkHandlerThread? = null
    var gifEncoder: IImageEncoder? = null
    private var encoderStartTime:Long = 0
    /**
     * FPS 控制器,如果相机打开失败等异常情况使用这个fps控制器来控制帧率
     */
    private val mLimitFpsHelper: LimitFpsHelper by lazy { LimitFpsHelper() }
    private fun startEncoder(){
        val path = "${FileUtils.getExternalRootFileDir()}/gif/${System.currentTimeMillis()}.gif"
        startEncoder = true
        workHandler?.reset()
        workHandler?.quitSafely()
        workHandler = WorkHandlerThread("WebpEncoder", onLooperPrepared = {
            it.queueEvent(Message.obtain())
        }){
            while (startEncoder){
                val time = System.currentTimeMillis() - encoderStartTime
                if (gifEncoder == null){
                    gifEncoder = GifBufferEncoder(path,30.0).apply {
                        encoderStartTime = System.currentTimeMillis()
                        mBinding.root.post {
                            mBinding.txtStatus.text = "录制中"
                        }
                    }
                }
                if(encoderStartTime>0){
                    BitmapBufferPool.getBitmapBuffer(1080,1920)?.let { buffer ->
                        val imageFrame = ImageFrame(
                            buffer,
                            format = FrameFormat.FORMAT_RGBA,
                            width = 1080,
                            height = 1920
                        )//这里示例代码就采用三原色进行录制验证
                        gifEncoder?.encode(imageFrame)//encode
                    }
                    mBinding.root.post {
                        mBinding.txtStatus.text = "录制时间：$time ms"
                    }
                }
                mLimitFpsHelper.limitFrameRate()
            }
            gifEncoder?.finish()?.onSuccess {
                encoderStartTime = 0
                FULogger.d("WebpEncoder", " finishEncoder outPath:$it")
                mBinding.root.post {
                    mBinding.txtStatus.text = "录制结束"
                }
            }?.onFailure {
                FULogger.d("WebpEncoder", " finishEncoder:$it")
            }
        }
        workHandler?.start()
    }
    private fun  stopEncoder(){
        startEncoder = false
    }




}