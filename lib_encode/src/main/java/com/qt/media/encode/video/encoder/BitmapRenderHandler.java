package com.qt.media.encode.video.encoder;


import android.graphics.Bitmap;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.qt.media.encode.entity.ImageFrame;
import com.qt.media.encode.help.LimitFpsHelper;
import com.qt.media.encode.interfaces.IFrameRenderListener;
import com.qt.media.encode.program.FUProgramBitmap2d;
import com.qt.media.encode.program.FUProgramBitmapWithBrightness;
import com.qt.media.encode.program.core.FUEglCore;
import com.qt.media.encode.program.core.FUWindowSurface;
import com.qt.media.encode.program.utils.FUGLUtils;
import com.qt.program.utils.FUDecimalUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Helper class to draw texture to whole view on private thread
 */
public final class BitmapRenderHandler implements Runnable {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "InputFrameEncoder";

    private final Object mSync = new Object();
    private EGLContext mShard_context;
    private Surface mSurface;
    private float[] mTexMatrix = new float[16];
    private float[] mMvpMatrix = new float[16];

    private volatile boolean mRequestSetEglContext;
    private volatile boolean mRequestRelease;
    private AtomicInteger mRequestDraw = new AtomicInteger(0);

    private FUWindowSurface mInputWindowSurface;
    private FUEglCore mEglCore;
    private FUProgramBitmapWithBrightness mProgramBrightness;
    private FUProgramBitmap2d mProgram2d;
    private IFrameRenderListener frameRenderListener;

    public static final BitmapRenderHandler createHandler(final String name) {
        if (DEBUG) {
            Log.v(TAG, "createHandler:");
        }
        final BitmapRenderHandler handler = new BitmapRenderHandler();
        synchronized (handler.mSync) {
            new Thread(handler, !TextUtils.isEmpty(name) ? name : TAG).start();
            try {
                handler.mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        return handler;
    }

    public void setFrameRenderListener(IFrameRenderListener listener){
        this.frameRenderListener = listener;
    }

    public final void setEglContext(final EGLContext shared_context, final Surface surface) {
        if (DEBUG) {
            Log.i(TAG, "setEglContext:");
        }
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mShard_context = shared_context;
            mSurface = surface;
            mRequestSetEglContext = true;
            mMvpMatrix = FUDecimalUtils.copyArray(FUGLUtils.IDENTITY_MATRIX);
            // 由于底层给的纹理上下镜像，导致我们这边需要通过矩阵做一下
            //上下镜像
            Matrix.scaleM(mMvpMatrix, 0, 1f, -1f, 1f);
            mTexMatrix  = FUGLUtils.IDENTITY_MATRIX;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                if (DEBUG) {
                    Log.e(TAG, "setEglContext error:"+e);
                }
            }
        }
    }
    private ConcurrentLinkedQueue<ImageFrame> bufferQueue = new ConcurrentLinkedQueue();
    private AtomicInteger  offerQueueCount  = new AtomicInteger(0);
    private AtomicInteger renderFrameCount = new AtomicInteger(0);
    public boolean offerImageFrame(final ImageFrame imageFrame) {
        synchronized (mSync) {
            if (mRequestRelease) {
                return false;
            }
            imageFrame.getImageBitmap();
            boolean offer = bufferQueue.offer(imageFrame);
            if(offer){
                offerQueueCount.incrementAndGet();
            }else {
                Log.e(TAG, "offerImageFrame error:"+offer);
            }
            Log.d(TAG, "renderFinishCount:"+renderFrameCount.get()+" inputQueueCount:"+offerQueueCount.get());
            mRequestDraw.incrementAndGet();
            mSync.notifyAll();
            return true;
        }
    }


    public final void release() {
        if (DEBUG) {
            Log.i(TAG, "release:");
        }
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mRequestRelease = true;
            frameRenderListener = null;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }
    private LimitFpsHelper limitFpsHelper = new LimitFpsHelper();

    //********************************************************************************
//********************************************************************************

    @Override
    public final void run() {
        if (DEBUG) {
            Log.i(TAG, "RenderHandler thread started:");
        }
        synchronized (mSync) {
            mRequestSetEglContext = mRequestRelease = false;
            mRequestDraw.set(0);
            mSync.notifyAll();
        }
        boolean localRequestDraw;
        for (; ; ) {
            synchronized (mSync) {
                if (mRequestRelease) {
                    break;
                }
                if (mRequestSetEglContext) {
                    mRequestSetEglContext = false;
                    internalPrepare();
                }
                localRequestDraw = mRequestDraw.get() > 0;
                if (localRequestDraw) {
                    mRequestDraw.getAndDecrement();//减一
                }
            }
            if (localRequestDraw) {
                ImageFrame imageFrame = bufferQueue.poll();
                    Bitmap bitmap = null;
                    if(imageFrame!= null){
                        bitmap = imageFrame.getImageBitmap();
                    }
                if ((mEglCore != null) && bitmap!=null && !bitmap.isRecycled()) {
                    if(frameRenderListener!= null){
                        frameRenderListener.renderInputFrameBefore(bitmap,imageFrame);
                    }
                    int frameIndex =  renderFrameCount.incrementAndGet();//帧数加一
                    if (DEBUG) {
                        Log.i(TAG, "drawFrame frameIndex:"+frameIndex);
                    }
                    mInputWindowSurface.makeCurrent();
                    // clear screen with yellow color so that you can see rendering rectangle
                    GLES20.glClearColor(0f, 0f, 0f, 0f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    if (imageFrame.getUBrightness()!=null){
                        mProgramBrightness.drawFrame(bitmap, mTexMatrix, mMvpMatrix, imageFrame.getUBrightness());
                    }else {
                        mProgram2d.drawFrame(bitmap, mTexMatrix, mMvpMatrix);
                    }
                    long nanoPts = computePresentationTime(frameIndex) * 1000; // us
                    mInputWindowSurface.setPresentationTime(nanoPts);
                    mInputWindowSurface.swapBuffers();
                    if (DEBUG) {
                        Log.i(TAG, "setPresentationTime:"+nanoPts+" frameIndex:"+frameIndex);
                    }
                    if(frameRenderListener!= null){
                        frameRenderListener.onInputFrameRender(bitmap);
                    }
                    if (imageFrame!= null){
                        imageFrame.release();
                    }
                }
//                limitFpsHelper.limitFrameRate();
            } else {
                synchronized (mSync) {
                    try {
                        if (DEBUG) {
                            Log.i(TAG, "drawFrame Thread wait newInput frame");
                        }
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        if (DEBUG) {
                            Log.e(TAG, "drawFrame error:"+e);
                        }
                        break;
                    }
                }
            }
        }
        synchronized (mSync) {
            mRequestRelease = true;
            internalRelease();
            mSync.notifyAll();
        }
        if (DEBUG) {
            Log.i(TAG, "RenderHandler thread finished:");
        }
    }
    private Long computePresentationTime(int frameIndex) {
        return (long) (frameIndex * 1000000.0 /limitFpsHelper.getFps());
    }
    public boolean isDrawing(){
        boolean isDrawing =  mRequestDraw.get()>0;
        Log.e("czf", "isDrawing:"+isDrawing+" mRequestDraw:"+mRequestDraw.get());
        return isDrawing;
    }

    private final void internalPrepare() {
        if (DEBUG) {
            Log.i(TAG, "internalPrepare:");
        }
        internalRelease();
        mEglCore = new FUEglCore(mShard_context, FUEglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new FUWindowSurface(mEglCore, mSurface, true);
        mInputWindowSurface.makeCurrent();
        mInputWindowSurface.swapInterval(1);//开垂直同步
        mProgramBrightness = new FUProgramBitmapWithBrightness();
        mProgram2d = new FUProgramBitmap2d();
        mSurface = null;
        mSync.notifyAll();
    }
    private final void internalRelease() {
        if (DEBUG) {
            Log.i(TAG, "internalRelease:");
        }
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mProgramBrightness != null) {
            mProgramBrightness.release();
            mProgramBrightness = null;
        }
        if (mProgram2d != null){
            mProgram2d.release();
            mProgram2d = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        while (!bufferQueue.isEmpty()){
            ImageFrame frame = bufferQueue.poll();
            if (frame!= null){
                frame.release();
            }
        }
        bufferQueue.clear();
    }

    public void setFps(float fps) {
        limitFpsHelper.setTargetFps((int) fps);
    }
}
