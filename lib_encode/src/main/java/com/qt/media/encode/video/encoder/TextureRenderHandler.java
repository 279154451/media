package com.qt.media.encode.video.encoder;


import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.qt.media.encode.program.FUProgramTexture2d;
import com.qt.media.encode.program.core.FUEglCore;
import com.qt.media.encode.program.core.FUProgram;
import com.qt.media.encode.program.core.FUWindowSurface;


/**
 * Helper class to draw texture to whole view on private thread
 */
public final class TextureRenderHandler implements Runnable {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "Video_RenderHandler";

    private final Object mSync = new Object();
    private EGLContext mShard_context;
    private Surface mSurface;
    private int mTexId;
    private float[] mTexMatrix = new float[16];
    private float[] mMvpMatrix = new float[16];

    private volatile boolean mRequestSetEglContext;
    private volatile boolean mRequestRelease;
    private volatile int mRequestDraw;

    private FUWindowSurface mInputWindowSurface;
    private FUEglCore mEglCore;
    private FUProgram mProgramTexture2d;

    public static final TextureRenderHandler createHandler(final String name) {
        if (DEBUG) {
            Log.v(TAG, "createHandler:");
        }
        final TextureRenderHandler handler = new TextureRenderHandler();
        synchronized (handler.mSync) {
            new Thread(handler, !TextUtils.isEmpty(name) ? name : TAG).start();
            try {
                handler.mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        return handler;
    }

    public final void setEglContext(final EGLContext shared_context, final Surface surface, final int texId) {
        if (DEBUG) {
            Log.i(TAG, "setEglContext:");
        }
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mShard_context = shared_context;
            mTexId = texId;
            mSurface = surface;
            mRequestSetEglContext = true;
            Matrix.setIdentityM(mTexMatrix, 0);
            Matrix.setIdentityM(mMvpMatrix, 0);
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    public final void draw(final int tex_id, final float[] tex_matrix, final float[] mvp_matrix) {
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mTexId = tex_id;
            if ((tex_matrix != null) && (tex_matrix.length >= 16)) {
                System.arraycopy(tex_matrix, 0, mTexMatrix, 0, 16);
            } else {
                Matrix.setIdentityM(mTexMatrix, 0);
            }
            if ((mvp_matrix != null) && (mvp_matrix.length >= 16)) {
                System.arraycopy(mvp_matrix, 0, mMvpMatrix, 0, 16);
            } else {
                Matrix.setIdentityM(mMvpMatrix, 0);
            }
            mRequestDraw++;
            mSync.notifyAll();
        }
    }

    public boolean isValid() {
        synchronized (mSync) {
            return !(mSurface instanceof Surface) || ((Surface) mSurface).isValid();
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
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    //********************************************************************************
//********************************************************************************

    @Override
    public final void run() {
        if (DEBUG) {
            Log.i(TAG, "RenderHandler thread started:");
        }
        synchronized (mSync) {
            mRequestSetEglContext = mRequestRelease = false;
            mRequestDraw = 0;
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
                localRequestDraw = mRequestDraw > 0;
                if (localRequestDraw) {
                    mRequestDraw--;
                }
            }
            if (localRequestDraw) {
                if ((mEglCore != null) && mTexId >= 0) {
                    mInputWindowSurface.makeCurrent();
                    // clear screen with yellow color so that you can see rendering rectangle
                    GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    mProgramTexture2d.drawFrame(mTexId, mTexMatrix, mMvpMatrix);
                    mInputWindowSurface.swapBuffers();
                }
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
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

    private final void internalPrepare() {
        if (DEBUG) {
            Log.i(TAG, "internalPrepare:");
        }
        internalRelease();
        mEglCore = new FUEglCore(mShard_context, FUEglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new FUWindowSurface(mEglCore, mSurface, true);
        mInputWindowSurface.makeCurrent();
        mProgramTexture2d = new FUProgramTexture2d();
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
        if (mProgramTexture2d != null) {
            mProgramTexture2d.release();
            mProgramTexture2d = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

}
