package com.qt.media.encode.video.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音视频混合编码器
 */
public class MediaMuxerWrapper {
    private static final String TAG = MediaMuxerWrapper.class.getSimpleName();
    private static final boolean DEBUG = true;

    private String mOutputPath;
    private final MediaMuxer mMediaMuxer;    // API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder, mAudioExportEncoder;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public MediaMuxerWrapper(String filePath) throws IOException {
        mOutputPath = filePath;
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public void prepare() throws IOException, MediaCodec.CodecException, IllegalArgumentException {
        if (mVideoEncoder != null) {
            mVideoEncoder.prepare();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
        if (mAudioExportEncoder != null) {
            mAudioExportEncoder.prepare();
        }
    }

    public void startRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.startRecording();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.startRecording();
        }
        if (mAudioExportEncoder != null) {
            mAudioExportEncoder.startRecording();
        }
    }

    public void stopRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stopRecording();
        }
        mVideoEncoder = null;
        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
        }
        mAudioEncoder = null;
        if (mAudioExportEncoder != null) {
            mAudioExportEncoder.stopRecording();
        }
        mAudioExportEncoder = null;
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    /**
     * assign encoder to this calss. this is called from encoder.
     *
     * @param encoder instance of MediaVideoEncoder or MediaAudioRecordEncoder
     */
    /*package*/ void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoTextureEncoder) {
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        }else if(encoder instanceof MediaVideoNV12Encoder){
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        } else if(encoder instanceof MediaBufferVideoEncoder){
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioRecordEncoder) {
            if (mAudioEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mAudioEncoder = encoder;
        } else if (encoder instanceof MediaAudioExportEncoder) {
            if (mAudioExportEncoder != null) {
                throw new IllegalArgumentException("Video file encoder already added.");
            }
            mAudioExportEncoder = encoder;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0) + (mAudioExportEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    /*package*/
    synchronized boolean start() {
        if (DEBUG) {
            Log.i(TAG, "start:");
        }
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            if (DEBUG) {
                Log.i(TAG, "MediaMuxer started:");
            }
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    /*package*/
    synchronized void stop() {
        if (DEBUG) {
            Log.i(TAG, "stop:mStatredCount=" + mStatredCount);
        }
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
            if (DEBUG) {
                Log.i(TAG, "MediaMuxer stopped:");
            }
        }
    }

    /**
     * assign encoder to muxer
     *
     * @param format
     * @return minus value indicate error
     */
    /*package*/
    synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        final int trackIx = mMediaMuxer.addTrack(format);
        if (DEBUG) {
            Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
        }
        return trackIx;
    }

    /**
     * write encoded data to muxer
     *
     * @param trackIndex
     * @param byteBuf
     * @param bufferInfo
     */
    /*package*/
    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStatredCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }

    public int getEncoderCount() {
        return mEncoderCount;
    }
}
