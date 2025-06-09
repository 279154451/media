# mediaEnCoder
Android平台MP4,GIF,WEBP编码录制实现

## MP4编码录制

mp4录制采用mediaCodec硬件编码实现，支持buffer和纹理录制

（1）初始化编码器

```kotlin
videoEncoder = VideoBufferEncoder().apply {
    val audioEntities: List<AudioTrackEntity> = emptyList()//音频数据，这里可以传入wav音频，可以实现向视频文件中插入对应时间段的音频。注意：这里的音频只支持采样率是44100，2声道，16Bit位深格式
    startVideoEncoder(videoPath, 1080, 1920, 12000f, 30f, audioEntities, onPreparedUnit = {
        it.invoke()
    }, onStoppedUnit = {
        it.invoke()
    }).onFailure {
        FULogger.e("VideoEncoder", " startVideoEncoder:$it")
    }
}
```

（2）编码录制帧

```kotlin
videoEncoder?.encoderBufferFrame2Video(imageFrame)//encode
```

（3）结束编码写入文件

```kotlin
videoEncoder?.finishEncoder(object : IVideoEncoderListener {
    override fun onFinish(outPath: String) {
        encoderStartTime = 0
        FULogger.d("VideoEncoder", " finishEncoder outPath:$outPath")
    }
})
```

## WEBP编码录制

webp录制采用Google开源的webp库经过简单封装实现

（1）初始化编码器

```kotlin
webpEncoder = WebpBufferEncoder(path,30.0)
```

（2）编码录制帧

```kotlin
webpEncoder?.encode(imageFrame)//encode
```

（3）结束编码写入文件

```kotlin
webpEncoder?.finish()?.onSuccess {
    encoderStartTime = 0
    FULogger.d("WebpEncoder", " finishEncoder outPath:$it")
    mBinding.root.post {
        mBinding.txtStatus.text = "录制结束"
    }
}?.onFailure {
    FULogger.d("WebpEncoder", " finishEncoder:$it")
}
```

## GIF编码录制

gif录制采用lzwEncoder压缩编码

（1）初始化编码器

```kotlin
webpEncoder = GifBufferEncoder(path,30.0)
```

（2）编码录制帧

```kotlin
webpEncoder?.encode(imageFrame)//encode
```

（3）结束编码写入文件

```kotlin
webpEncoder?.finish()?.onSuccess {
    encoderStartTime = 0
    FULogger.d("WebpEncoder", " finishEncoder outPath:$it")
    mBinding.root.post {
        mBinding.txtStatus.text = "录制结束"
    }
}?.onFailure {
    FULogger.d("WebpEncoder", " finishEncoder:$it")
}
```

