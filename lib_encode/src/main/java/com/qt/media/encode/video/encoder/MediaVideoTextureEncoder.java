package com.qt.media.encode.video.encoder;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.qt.media.encode.program.FUProgramTexture2d;
import com.qt.media.encode.program.utils.FUGLUtils;

import java.util.Map;

/**
 * 根据纹理数据编码视频
 */
public class MediaVideoTextureEncoder extends MediaEncoder {
    private static final boolean DEBUG = true;    // TODO set false on release
    String TAG = "FUMediaVideoTextureEncoder";

    private static final String MIME_TYPE =MediaFormat.MIMETYPE_VIDEO_AVC;
    // parameters for recording
    private static final int FRAME_RATE = 25;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private TextureRenderHandler mRenderHandler;
    private Surface mSurface;


    /**
     * 纹理绘制的起始位置（左下角为（0，0））
     */
    private int cropX;
    private int cropY;
    /**
     * 纹理的宽高
     */
    private int textureWidth, textureHeight;
    private int mFrameCount;
    private FUProgramTexture2d program;
    private int[] mFboTex;
    private int[] mFboId;
    private int[] mViewPort = new int[4];
    private Map<String, Integer> config;
    public MediaVideoTextureEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int videoWidth, final int videoHeight) {
        this(muxer, listener, videoWidth, videoHeight, 0, 0, videoWidth, videoHeight);
    }

    public MediaVideoTextureEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int videoWidth, final int videoHeight,
                                    int cropX, int cropY, int textureWidth, int textureHeight) {
        super(muxer, listener);
        if (DEBUG) {
            Log.i(TAG, "MediaVideoEncoder: ");
        }
        mWidth = videoWidth;
        mHeight = videoHeight;
        mRenderHandler = TextureRenderHandler.createHandler(TAG);
        this.cropX = cropX;
        this.cropY = cropY;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public boolean frameAvailableSoon(int texId, float[] texMatrix, float[] mvpMatrix,int textureWidth, int textureHeight) {
        if (program == null) {
            return false;
        }
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, mViewPort, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
        GLES20.glViewport(cropX, cropY, textureWidth, textureHeight);
        program.drawFrame(texId, texMatrix, mvpMatrix);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(mViewPort[0], mViewPort[1], mViewPort[2], mViewPort[3]);
        // 先绘制三次，不进行编码，解决黑屏问题
        if (mFrameCount++ < 3) {
            return true;
        }
        boolean result;
        if (result = super.frameAvailableSoon()) {
            mRenderHandler.draw(mFboTex[0], texMatrix, FUGLUtils.IDENTITY_MATRIX);
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
     * 需要在gl线程调用
     * @param shared_context
     */
    public void setEglContext(final EGLContext shared_context) {
        if (DEBUG) {
            Log.i(TAG, "setEglContext ThreadId:" + Thread.currentThread().getId());
        }
        mFboTex = new int[1];
        mFboId = new int[1];
        FUGLUtils.createFrameBuffers(mFboTex, mFboId, mWidth, mHeight);
        program = new FUProgramTexture2d();
        if (mRenderHandler != null) {
            mRenderHandler.setEglContext(shared_context, mSurface, mFboTex[0]);
        }
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

        mFrameCount = 0;
        super.release();
    }

    public void releaseGL() {
        if (DEBUG) {
            Log.i(TAG, "releaseGL ThreadId:" + Thread.currentThread().getId());
        }
        FUGLUtils.deleteFrameBuffers(mFboId);
        if (mFboId != null) {
            mFboId[0] = -1;
        }
        FUGLUtils.deleteTextures(mFboTex);
        if (mFboTex != null) {
            mFboTex[0] = -1;
        }
        if (program != null) {
            program.release();
            program = null;
        }

    }


    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
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

    @Override
    protected void signalEndOfInputStream() {
        if (DEBUG) {
            Log.d(TAG, "sending EOS to encoder");
        }
        if (mMediaCodec != null) {
            try {
                mMediaCodec.signalEndOfInputStream();    // API >= 18
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        mIsEOS = true;
    }
}
