package com.qt.media.encode.video.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 录音编码
 */
public class MediaAudioRecordEncoder extends MediaEncoder {
    private static final String TAG = MediaAudioRecordEncoder.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
    public static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25;    // AAC, frame/buffer/sec

    private AudioThread mAudioThread = null;

    public MediaAudioRecordEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        super(muxer, listener);
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) {
            Log.v(TAG, "prepare:");
        }
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "selected codec: " + audioCodecInfo.getName());
        }

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
        if (DEBUG) {
            Log.i(TAG, "format: " + audioFormat);
        }
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
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
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mAudioThread == null) {
            mAudioThread = new AudioThread();
            mAudioThread.start();
        }
    }

    @Override
    public void release() {
        mAudioThread = null;
        super.release();
    }

    private static final int[] AUDIO_SOURCES = new int[] {
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
                int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
                if (buffer_size < min_buffer_size) {
                    buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
                }

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source, SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                            audioRecord = null;
                        }
                    } catch (final Exception e) {
                        audioRecord = null;
                    }
                    if (audioRecord != null) {
                        break;
                    }
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {
                            if (DEBUG) {
                                Log.v(TAG, "AudioThread:start audio recording");
                            }
                            final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        encode(buf, readBytes, getPTSUs());
                                        frameAvailableSoon();
                                    }
                                }
                                frameAvailableSoon();
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    Log.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG, "AudioThread#run", e);
            }
            if (DEBUG) {
                Log.v(TAG, "AudioThread:finished");
            }
        }
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) {
            Log.v(TAG, "selectAudioCodec:");
        }

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG) {
                    Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                }
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

}
