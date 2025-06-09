package com.qt.media.encode.program;

import android.opengl.GLES20;

import com.qt.media.encode.program.core.FUDrawable2d;
import com.qt.media.encode.program.core.FUProgram;
import com.qt.media.encode.program.utils.FUGLUtils;


public class FUProgramTexture2dWithAlphaAnimate extends FUProgram {

    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute float alpha;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying float mAlpha;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    mAlpha=alpha;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "varying float mAlpha;\n" +
                    "void main() {\n" +
                    "    vec4 rgba= texture2D(sTexture, vTextureCoord);\n" +
//                    "    float a= rgba * mAlpha;\n" +
//                    "    rgba.a=a;\n" +
                    "    gl_FragColor = rgba * mAlpha;\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int alpha;

    private float mAlpha;
    private boolean isOpenAnimate;
    /*是否指定绘制特定透明度*/
    private boolean mSpecified;

    public FUProgramTexture2dWithAlphaAnimate() {
        super(VERTEX_SHADER, FRAGMENT_SHADER_2D);
        isOpenAnimate = false;
        mAlpha = 0;
        mSpecified = false;
    }

    public void setOpenAnimate(boolean isOpenAnimate) {
        if (mSpecified) {
            return;
        }
        if (!isOpenAnimate) {
            mAlpha = 1;
        }
        if (this.isOpenAnimate == isOpenAnimate) {
            return;
        }
        this.isOpenAnimate = isOpenAnimate;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
        mSpecified = true;
    }

    @Override
    protected FUDrawable2d getDrawable2d() {
        return new FUDrawable2dFull();
    }

    @Override
    protected void getLocations() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        FUGLUtils.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        FUGLUtils.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        FUGLUtils.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        FUGLUtils.checkLocation(muTexMatrixLoc, "uTexMatrix");
        alpha = GLES20.glGetAttribLocation(mProgramHandle, "alpha");
        FUGLUtils.checkLocation(alpha, "alpha");
    }

    @Override
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix) {
        FUGLUtils.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        FUGLUtils.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        GLES20.glVertexAttrib1f(alpha, mAlpha);
        FUGLUtils.checkGlError("alpha");

        if (!mSpecified && isOpenAnimate) {
            if (mAlpha < 1.0f) {
                mAlpha = mAlpha + 0.1f;
            }
        }

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, FUDrawable2dFull.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, FUDrawable2dFull.VERTEXTURE_STRIDE, mDrawable2d.vertexArray());
        FUGLUtils.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, FUDrawable2dFull.TEXTURE_COORD_STRIDE, mDrawable2d.texCoordArray());
        FUGLUtils.checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.vertexCount());
        FUGLUtils.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    public void release() {
        super.release();
        isOpenAnimate = false;
        mAlpha = 0;
        mSpecified = false;
    }

}
