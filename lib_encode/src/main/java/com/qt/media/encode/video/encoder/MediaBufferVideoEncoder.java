package com.qt.media.encode.video.encoder;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.qt.media.encode.entity.ImageFrame;
import com.qt.media.encode.interfaces.IFrameRenderListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

/**
 * 根据纹理数据编码视频
 */
public class MediaBufferVideoEncoder extends MediaEncoder implements IFrameRenderListener {
    private static final boolean DEBUG = true;    // TODO set false on release
    String TAG = "FUMediaBitmapVideoEncoder";

    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    // parameters for recording
    private static final int FRAME_RATE = 25;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private BitmapRenderHandler mRenderHandler;
    private Surface mSurface;

    private Map<String, Integer> config;
    public MediaBufferVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int videoWidth, final int videoHeight, float fps) {
        super(muxer, listener);
        if (DEBUG) {
            Log.i(TAG, "MediaVideoEncoder: ");
        }
        mWidth = videoWidth;
        mHeight = videoHeight;
        mRenderHandler = BitmapRenderHandler.createHandler(TAG);
        mRenderHandler.setFrameRenderListener(this);
        mRenderHandler.setFps(fps);
    }

    public boolean frameAvailableSoon(ImageFrame buffer) {
        if (!glInit) {
            return false;
        }
        boolean result;
        if(buffer.isKeyFrame())requestKeyFrame();//手动请求输出关键帧
        if (result = super.frameAvailableSoon()) {
            mRenderHandler.offerImageFrame(buffer);
        }
        return result;
    }
    public void setMediaFormatConfig(Map<String, Integer> config) {
        this.config = config;
    }
    private Integer getValueOrDefault(String key, int defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        Integer integer = config.get(key);
        if (integer == null) {
            return defaultValue;
        }
        return integer;
    }
    @Override
    protected void prepare() {
        try {
            if (DEBUG) {
                Log.i(TAG, "prepare: ");
            }
            mTrackIndex = -1;
            mMuxerStarted = mIsEOS = false;

            final MediaCodecInfo videoCodecInfo = selectCodec(MIME_TYPE);
            if (videoCodecInfo == null) {
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }else  if(!isSupportFormatSurface(videoCodecInfo,MIME_TYPE)){
                Log.e(TAG, "not support COLOR_FormatSurface codec for " + MIME_TYPE);
                return;
            }
            if (DEBUG) {
                Log.i(TAG, "selected codec: " + videoCodecInfo.getName());
            }

            final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
            format.setInteger(MediaFormat.KEY_BIT_RATE, getValueOrDefault(MediaFormat.KEY_BIT_RATE,calcBitRate()));
            format.setInteger(MediaFormat.KEY_FRAME_RATE, getValueOrDefault(MediaFormat.KEY_FRAME_RATE,FRAME_RATE));
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, getValueOrDefault(MediaFormat.KEY_I_FRAME_INTERVAL,10));
            if (DEBUG) {
                Log.i(TAG, "format: " + format);
            }

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // get Surface for encoder input
            // this method only can call between #configure and #start
            mSurface = mMediaCodec.createInputSurface();    // API >= 18
            mMediaCodec.start();
            if (DEBUG) {
                Log.i(TAG, "prepare finishing");
            }
            if (mListener != null) {
                try {
                    mListener.onPrepared(this);
                } catch (final Exception e) {
                    Log.e(TAG, "prepare:", e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            release();
        }
    }

    /**
     * return mMediaCodec.createInputSurface()
     * @return
     */
    public Surface getSurface(){
        return mSurface;
    }
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equals(mimeType)) {
                    return codecInfo;
                }
            }
        }

        return null;
    }
    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    private Boolean isSupportFormatSurface(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                // 编解码器支持 COLOR_FormatSurface 格式
                Log.d(TAG, "Codec " + codecInfo.getName() + " supports FormatSurface encoding");
                return true;
            }
        }
        return false;
    }
    private boolean glInit = false;
    /**
     * 需要在gl线程调用
     * @param shared_context
     */
    public void setEglContext(final EGLContext shared_context) {
        if (DEBUG) {
            Log.i(TAG, "setEglContext ThreadId:" + Thread.currentThread().getId());
        }
        if (mRenderHandler != null) {
            mRenderHandler.setEglContext(shared_context, mSurface);
        }
        glInit = true;
    }

    @Override
    public void release() {
        if (DEBUG) {
            Log.i(TAG, "release:");
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        glInit = false;
        super.release();
    }

    public void releaseGL() {
        if (DEBUG) {
            Log.i(TAG, "releaseGL ThreadId:" + Thread.currentThread().getId());
        }
        glInit = false;
    }


    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    @Override
    protected void signalEndOfInputStream() {
        if (DEBUG) {
            Log.d(TAG, "sending EOS to encoder");
        }
        waitFinish.set(false);
        if (mMediaCodec != null) {
            try {
                mMediaCodec.signalEndOfInputStream();    // API >= 18
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        mIsEOS = true;
    }

    private AtomicBoolean waitFinish = new AtomicBoolean(false);
    private AtomicInteger waitFinishFrameCount = new AtomicInteger(0);
    @Override
    public void finishEncoder(MediaMuxerWrapper muxerWrapper, boolean force) {
        if (force){
            waitFinish.set(false);
            super.finishEncoder(muxerWrapper,true);
        }else {
            if (inputFrameCount.get()>outFrameCount.get()){
                waitFinish.set(true);
                waitFinishFrameCount.set(inputFrameCount.get()-outFrameCount.get());
                super.frameAvailableSoon();
            }else {
                super.finishEncoder(muxerWrapper,true);
            }
        }
        Log.w(TAG,"finishEncoder inputFrameCount:"+inputFrameCount.get()+" outFrameCount:"+outFrameCount.get()+" waitFinishFrameCount:"+waitFinishFrameCount.get());
    }

    @Override
    public void cancel(MediaMuxerWrapper muxerWrapper) {
        super.cancel(muxerWrapper);
        waitFinish.set(false);
    }

    private AtomicInteger inputFrameCount = new AtomicInteger(0);
    private AtomicInteger outFrameCount = new AtomicInteger(0);
    @Override
    public void renderInputFrameBefore(@Nullable Object bitmap,@Nullable Object frameData) {
        if(frameData!= null && frameData instanceof ImageFrame){
            boolean keyFrame = ((ImageFrame) frameData).isKeyFrame();
            if(keyFrame){
                requestKeyFrame();
            }
        }
    }
    @Override
    public void onInputFrameRender(@Nullable Object any) {
        if(mListener!= null){
            mListener.onFrameRender(this,any);
        }
        int count = inputFrameCount.incrementAndGet();
        if (DEBUG) {
            Log.d(TAG, "inputFrame Count="+count);
        }
    }

    @Override
    protected void onWriteSampleData(Object object) {
        super.onWriteSampleData(object);
        int count = outFrameCount.incrementAndGet();
        if (DEBUG) {
            Log.d(TAG, "outputFrame Count="+count);
        }
        if(waitFinish.get() && mWeakMuxer!= null){
            waitFinishFrameCount.decrementAndGet();
            finishEncoder(mWeakMuxer.get(),false);
        }
    }

    @Override
    protected void dequeueOutputBufferStatus(int outIndex) {
        super.dequeueOutputBufferStatus(outIndex);
    }

    @Override
    protected void dequeueOutputBufferLoopEnd() {
        super.dequeueOutputBufferLoopEnd();
        if (waitFinish.get()){
            int count = waitFinishFrameCount.decrementAndGet();//减一
            if (count<0 && mWeakMuxer!= null){
                finishEncoder(mWeakMuxer.get(),true);
            }
        }
    }
}
