package com.qt.media.encode.gif;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import com.faceunity.ImageCodec.ImageCodecAPI;
import com.faceunity.ImageCodec.ImageEncoderGIF;
import com.qt.media.encode.help.FileUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class GifBufferEncoderWrapper {
    private static final String TAG = GifBufferEncoderWrapper.class.getSimpleName();

    private static boolean DEBUG = false;

    private static final int WHAT_GIF_ENCODER = 0;
    private static final String data_key_img = "data_key_img";
    private static final String data_key_img_w = "data_key_img_w";
    private static final String data_key_img_h = "data_key_img_h";
    private ImageEncoderGIF encoderGIF;
    private HandlerThread mThread;
    private Handler mHandler;
    private String mGifPath;
    private String mGifRecordPath;
    private long lastEncodeTime;
    /**
     * 当前录制的帧数
     */
    private long currentFrameNumber = 1;
    /**
     * 需要丢弃整个GIF的几分之一帧
     */
    private int discardAFraction = -1;
    /**
     * 录制跟丢弃帧的模板，只要由1跟0两种参数，1表示录制、0表示丢弃
     * 数组的第一位最好是1，frameTemplate数组的长度也最好长一点
     */
    private int[] frameTemplate = null;

    private double fps;
    private OnRecordListener listener;
    private AtomicBoolean isCancel;
    private boolean fristFrame = true;

    public GifBufferEncoderWrapper(String path, int gifWidth, int gifHeight) {
        String sdkVersion = ImageCodecAPI.SDKVersion();
        String sdkCommitTime = ImageCodecAPI.SDKCommitTime();
        String sdkCommitHash = ImageCodecAPI.SDKCommitHash();
        Log.d(TAG,"gifEncoder sdkVersion="+sdkVersion+" sdkCommitTime="+sdkCommitTime+" sdkCommitHash="+sdkCommitHash);
        ImageCodecAPI.SetLoggerLevel(ImageCodecAPI.EFuImageCodecLoggerLevel.FU_IMAGE_CODEC_LOG_LEVEL_VERBOSE);
        ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
        encoderGIF =ImageEncoderGIF.Create(errorCode);
        checkError("Create",errorCode);
        encoderGIF.SetImageWidth(gifWidth,errorCode);
        checkError("SetImageWidth",errorCode);
        encoderGIF.SetImageHeight(gifHeight,errorCode);
        checkError("SetImageHeight",errorCode);
        mGifRecordPath = FileUtils.INSTANCE.getExternalRootGifFilePath();
        encoderGIF.SetImageWritePath(mGifRecordPath,errorCode);
        checkError("SetImageWritePath",errorCode);
        mGifPath = path;
        lastEncodeTime = 0;
        isCancel = new AtomicBoolean(false);
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new GifHardHandler(mThread.getLooper());
    }
    private boolean checkError(String method,ImageCodecAPI.FuImageCodecErrorCode errorCode){
        if(errorCode.ec == ImageCodecAPI.EFuImageCodecErrorCode.FU_IMAGE_CODEC_ERROR_SUCCESS){
            return true;
        }
        Log.e(TAG,method+" error "+errorCode.ec);
        return false;
    }

    public void setImageSizeToComputeColorTable(int w, int h) {
        if (encoderGIF != null) {
            ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
            encoderGIF.SetImageSizeToComputeColorTable(w, h,errorCode);
            checkError("SetImageSizeToComputeColorTable",errorCode);
        }
    }

    public void setDiscardAFraction(int discardAFraction) {
        if (discardAFraction <= 0) {
            return;
        }
        this.discardAFraction = discardAFraction;
    }

    public void setGifThreadCount(int threadCount) {
        if (encoderGIF != null) {
            ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
            encoderGIF.SetThreadCount(threadCount,errorCode);
            checkError("SetThreadCount",errorCode);
        }
    }

    /**
     * 设置一个关于当前帧是留是丢的数组
     * 假设数组为[1,0,1,0,0]，表示第一帧留下，第二帧丢掉，第三帧留下，第四、第五帧丢掉
     *
     * @param frameTemplate
     */
    public void setDiscardAFraction(int[] frameTemplate) {
        if (frameTemplate == null || frameTemplate.length <= 1) {
            return;
        }
        boolean haveChange = false;
        int firstElement = -1;
        for (int i : frameTemplate) {
            // 只运行有0跟1两种元素
            if (i != 0 && i != 1) {
                throw new IllegalArgumentException("The array can only contain 0 and 1");
            }
            if (firstElement == -1) {
                firstElement = i;
            }
            if (!haveChange) {
                haveChange = firstElement == i;
            }
        }
        if (!haveChange) {// 数组元素是否为同一种元素
            throw new IllegalArgumentException("The array contains only one type of element, and the array must contain both 0 and 1 elements");
        }
        this.frameTemplate = frameTemplate;
    }

    public void encodeFrame(byte[] rgbaData, int bufferWidth,int bufferHeight) {
        currentFrameNumber++;
        if (encoderGIF == null) return;
        if (needAbandon()) return;
//        if (fristFrame) {
//           //TODO 处理第一帧异常问题
//            fristFrame = false;
//        }
        Message message = mHandler.obtainMessage(WHAT_GIF_ENCODER);
        Bundle data = new Bundle();
        data.putByteArray(data_key_img, rgbaData);
        data.putInt(data_key_img_w,bufferWidth);
        data.putInt(data_key_img_h,bufferHeight);
        message.setData(data);
        mHandler.sendMessage(message);
    }
    private boolean needAbandon() {
        if (frameTemplate == null) {
            if (discardAFraction != -1 && discardAFraction != 0) {
                long remainder = currentFrameNumber % discardAFraction;
                return remainder == (discardAFraction - 1);
            }
        } else {
            // 求余数
            int remainder = (int) (currentFrameNumber % frameTemplate.length);
            // 判断当前帧是否需要丢弃
            return frameTemplate[remainder] == 0;
        }
        return false;
    }

    public void cancel() {
        Log.i(TAG, "cancel");
        isCancel.set(true);
        discardAFraction = -1;
        frameTemplate = null;
        currentFrameNumber = 0;
        if (encoderGIF == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (encoderGIF == null) return;
                Log.i(TAG, "cancel run");
                ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
                encoderGIF.Reset(errorCode);
                checkError("Reset",errorCode);
                encoderGIF.Destroy(errorCode);
                checkError("Destroy",errorCode);
                encoderGIF = null;
                mHandler.removeMessages(WHAT_GIF_ENCODER);
                mThread.quitSafely();
                if (listener != null) {
                    Log.i(TAG, "cancel gifHardEncoder OnRecordEnd");
                    listener.onRecordEnd(isCancel.get());
                }
                listener = null;
            }
        });
    }

    public void release() {
        Log.i(TAG, "release");
        if (isCancel.get()) {
            Log.i(TAG, "release isCancel");
            return;
        }
        discardAFraction = -1;
        frameTemplate = null;
        currentFrameNumber = 0;
        if (encoderGIF == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "release run listener = " + listener);
                if (isCancel.get() || encoderGIF == null) {
                    return;
                }
                long startTime = System.currentTimeMillis();
                ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
                encoderGIF.Finish(errorCode);
                checkError("Finish",errorCode);
                encoderGIF.Destroy(errorCode);
                checkError("Destroy",errorCode);
                encoderGIF = null;
                Log.i(TAG, "release time = "+(System.currentTimeMillis()-startTime));
                mHandler.removeMessages(WHAT_GIF_ENCODER);
                mThread.quitSafely();
                Log.i(TAG, "release gif record end");
                if (isCancel.get()) {
                    File file = new File(mGifRecordPath);
                    if (file.isFile()) {
                        file.delete();
                        Log.i(TAG, "release gif file cancel and delete file");
                    } else {
                        Log.i(TAG, "release gif file is not file");
                    }
                } else {
                    File sourceFile = new File(mGifRecordPath);
                    if (sourceFile.exists() && sourceFile.isFile()) {
                        File tagFile = new File(mGifPath);
                        boolean copySuccess = FileUtils.INSTANCE.copyFile(sourceFile, tagFile);
                        if(copySuccess){
                            sourceFile.delete();
                        }else {
                            Log.e(TAG, "copyFile "+sourceFile.getAbsolutePath() +"to "+tagFile.getAbsolutePath()+" error");
                        }
                    }
                    if (listener != null) {
                        listener.onRecordEnd(isCancel.get());
                    }
                }
                listener = null;
            }
        });
    }

    class GifHardHandler extends Handler {

        public GifHardHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DEBUG) {
                Log.i(TAG, "handleMessage what = " + msg.what);
            }
            if (WHAT_GIF_ENCODER == msg.what && encoderGIF != null) {
                Bundle data = msg.getData();
                byte[] img = data.getByteArray(data_key_img);
                int width = data.getInt(data_key_img_w);
                int height = data.getInt(data_key_img_h);
                if (encoderGIF == null) return;
                ImageCodecAPI.FuImageCodecErrorCode errorCode = new ImageCodecAPI.FuImageCodecErrorCode();
                if (fps > 0) {
                    int duration_time_ms =  (int) (1000.0 / fps);
                    int[] durations = encoderGIF.GetImageDurationsFromFps((float) fps, errorCode);
                    if(checkError("GetImageDurationsFromFps",errorCode) && durations!= null && durations.length>0){
                        duration_time_ms = durations[0];
                    }
                    encoderGIF.EncodeImage(img,width,height, duration_time_ms,errorCode);
                    checkError("EncodeImage",errorCode);
                } else {
                    long encodeTime = System.nanoTime() / 1000000;
                    if (lastEncodeTime > 0) {
                        encoderGIF.EncodeImage(img,width,height,(int) (encodeTime - lastEncodeTime),errorCode);
                        checkError("EncodeImage",errorCode);
                    }
                    lastEncodeTime = encodeTime;
                }
            }
        }
    }
    /**
     * 设置帧率
     */
    public void setFps(double fps) {
        this.fps = fps ;
    }

    public void setListener(OnRecordListener listener) {
        this.listener = listener;
    }

    public static void openDebug(boolean open) {
        DEBUG = open;
    }

    /**
     * 录制监听
     */
    public interface OnRecordListener {
        /**
         * 录制结束
         */
        void onRecordEnd(boolean isCancel);
    }
}
