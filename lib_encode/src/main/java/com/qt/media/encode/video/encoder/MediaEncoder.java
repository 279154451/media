package com.qt.media.encode.video.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import androidx.annotation.Nullable;

/**
 * see https://github.com/saki4510t/AudioVideoRecordingSample
 */
public abstract class MediaEncoder implements Runnable {
    private static final String TAG = MediaEncoder.class.getSimpleName();
    private static final boolean DEBUG = true;

    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]

    public interface MediaEncoderListener {
        public void onPrepared(MediaEncoder encoder);

        public void onStopped(MediaEncoder encoder);

        public void onFrameRender(MediaEncoder encoder, @Nullable Object frame);

        public void onError(MediaEncoder encoder, Exception exception);
    }

    protected final Object mLock = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private volatile int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    protected MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)

    protected  MediaEncoderListener mListener;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaDecoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaExtractorWrapper is null");
        mWeakMuxer = new WeakReference<MediaMuxerWrapper>(muxer);
        muxer.addEncoder(this);
        mListener = listener;
        synchronized (mLock) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            new Thread(this, getClass().getSimpleName()).start();
            try {
                mLock.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    public String getOutputPath() {
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
        if (DEBUG) Log.d(TAG, "frameAvailableSoon");
        synchronized (mLock) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mLock.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (mLock) {
            mRequestStop = false;
            mRequestDrain = 0;
            mLock.notify();
        }
        boolean localRequestStop;
        boolean localRequestDrain;
        while (true) {
            synchronized (mLock) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain)
                    mRequestDrain--;
            }
            if (DEBUG) Log.d(TAG, "localRequestStop: " + localRequestStop +" localRequestDrain:"+localRequestDrain);
            if (localRequestStop) {
                drain();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
                drain();
            } else {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        if (DEBUG) Log.e(TAG, "Encoder thread exiting");
        synchronized (mLock) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    /*
     * prepareing method for each sub class
     * this method should be implemented in sub class, so set this as abstract method
     * @throws IOException
     */
    /*package*/
    abstract void prepare() throws IOException;

    /*package*/ void startRecording() {
        if (DEBUG) Log.e(TAG, "startRecording:"+this);
        synchronized (mLock) {
            mIsCapturing = true;
            mRequestStop = false;
            mLock.notifyAll();
        }
    }

    /**
     * the method to request stop encoding
     */
    /*package*/ void stopRecording() {
        if (DEBUG) Log.e(TAG, "stopRecording:"+this);
        synchronized (mLock) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
            mLock.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

//********************************************************************************
//********************************************************************************

    /**
     * Release all releated objects
     */
    public void release() {
        if (DEBUG) Log.e(TAG, "release:"+this);
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (final Exception e) {
                    Log.e(TAG, "failed stopping muxer", e);
                }
            }
        }
        mBufferInfo = null;
        if (mListener != null) {
            try {
                mListener.onStopped(this);
                mListener = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed onStopped", e);
            }
        }
        if (DEBUG) Log.e(TAG, "release: end");
    }

    protected void signalEndOfInputStream() {
        if (DEBUG) Log.e(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//		mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing || mMediaCodec == null) return;
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (DEBUG) Log.e(TAG, "inputBufferIndex: " + inputBufferIndex);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
                if (DEBUG) Log.e(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    if (DEBUG) Log.e(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    private boolean dequeueOutputBuffer = false;
    protected void drain() {
        try {
            if (mMediaCodec == null) return;
            int encoderStatus, count = 0;
            final MediaMuxerWrapper muxer = mWeakMuxer.get();
            if (muxer == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
                if (DEBUG) Log.e(TAG, "muxer is unexpectedly null");
                return;
            }
            if (DEBUG) Log.d(TAG, "mIsCapturing: " + mIsCapturing +" mIsEOS:"+mIsEOS);
            LOOP:
            while (mIsCapturing) {
                dequeueOutputBuffer = true;
                // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                dequeueOutputBufferStatus(encoderStatus);
                if (DEBUG) Log.d(TAG, "encoderStatus: " + encoderStatus+" mBufferInfo.flags:"+mBufferInfo.flags);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                    if (!mIsEOS) {
                        if (++count > 5)
                            break LOOP;        // out of while
                    }else if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || ++count>500){ //TIMEOUT_USEC x 500 = 5s
                        if (DEBUG) Log.e(TAG, "mIsEOS==true but 5s no outPut frame");//收到结束信号，或循环500次都没有新的帧就直接结束了，对最坏情况的兜底，避免出现一直接收不到新的帧和结束信号导致mediaCodec资源无法释放。（有遇到过客户机器MediaCodec底层不工作时会出现这种情况）
                        break ;
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (DEBUG) Log.e(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    // this shoud not come when encoding
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (DEBUG) Log.e(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                    // this status indicate the output format of codec is changed
                    // this should come only once before actual encoded data
                    // but this status never come on Android4.3 or less
                    // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                    if (mMuxerStarted) {    // second time request is error
                        throw new RuntimeException("format changed twice");
                    }
                    // get output format from codec and pass them to muxer
                    // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                    final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                    mTrackIndex = muxer.addTrack(format);
                    mMuxerStarted = true;
                    if (!muxer.start()) {
                        // we should wait until muxer is ready
                        synchronized (muxer) {
                            while (!muxer.isStarted())
                                try {
                                    muxer.wait(100);
                                } catch (final InterruptedException e) {
                                    break LOOP;
                                }
                        }
                    }
                } else if (encoderStatus < 0) {
                    // unexpected status
                    if (DEBUG)
                        Log.e(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
                } else {
                    final ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                        // this never should come...may be a MediaCodec internal error
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    boolean keyFrame = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                    if (DEBUG) Log.w(TAG, "keyFrame:"+keyFrame +" mBufferInfo.flags="+mBufferInfo.flags);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // You shoud set output format to muxer here when you target Android4.3 or less
                        // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                        // therefor we should expand and prepare output format from buffer data.
                        // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                        if (DEBUG) Log.e(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        // encoded data is ready, clear waiting counter
                        count = 0;
                        if (!mMuxerStarted) {
                            // muxer is not ready...this will prrograming failure.
                            throw new RuntimeException("drain:muxer hasn't started");
                        }
                        onWriteSampleData(encodedData);
                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                    // return buffer to encoder
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // when EOS come.
                        if (DEBUG) Log.w(TAG, "EOS come,mIsCapturing==false");
                        mIsCapturing = false;
                        break;      // out of while
                    }
                }
            }
            dequeueOutputBuffer = false;
            dequeueOutputBufferLoopEnd();
        } catch (Exception e) {
            Log.e(TAG,"drain",e);
            onError(e);
        }
    }

    /**
     * 手动触发输出关键帧
     */
    protected void requestKeyFrame(){
        if (Build.VERSION.SDK_INT >= 23 && mIsCapturing && mMediaCodec!= null) {
            if (DEBUG) Log.d(TAG, "requestKeyFrame" );
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            mMediaCodec.setParameters(params);
        }
    }
    protected void dequeueOutputBufferLoopEnd(){
    }
    protected void dequeueOutputBufferStatus(int outIndex){
        if (DEBUG)
            Log.d(TAG, "dequeueOutputBufferStatus: " + outIndex+" dequeueOutputBuffer:"+dequeueOutputBuffer);
    }
    protected void onWriteSampleData(Object object){

    }
    protected boolean isDequeueOutputBuffer(){
        return dequeueOutputBuffer;
    }

    /**
     * previous presentationTimeUs for writing
     */
    protected long prevOutputPTSUs = 0;

    /**
     * 每帧的时间间隔：视频渲染的时候需要
     * 单位：毫秒
     */
    protected long intervalTime = 33333;

    /**
     * 每帧的时间间隔：视频渲染的时候需要
     * @param intervalTime 单位微秒
     */
    public void setIntervalTime(long intervalTime) {
        this.intervalTime = intervalTime;
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        return prevOutputPTSUs += intervalTime;
    }

    /**
     * 结束编码
     * @param muxerWrapper
     */
    public void finishEncoder(MediaMuxerWrapper muxerWrapper, boolean force){
        if (muxerWrapper!= null){
            muxerWrapper.stopRecording();
        }
    }

    /**
     * 取消编码
     */
    public void cancel(MediaMuxerWrapper muxerWrapper){
        if (muxerWrapper!= null){
            if (DEBUG) Log.d(TAG, "cancel encoder "+this);
            muxerWrapper.stopRecording();
        }
    }
    protected void onError(Exception e) {
        String tip = "您的手机不支持该分辨率视频的导出";
        if (e != null && e.getMessage() != null) {
            tip = "导出异常：" + e.getMessage();
        }
//        FUARVideoApp.getGLContextThread().onExportError(tip);
    }
}
