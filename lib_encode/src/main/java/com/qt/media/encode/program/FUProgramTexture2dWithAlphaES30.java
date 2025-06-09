package com.qt.media.encode.program;

import android.opengl.GLES30;

import com.qt.media.encode.program.core.FUDrawable2d;
import com.qt.media.encode.program.core.FUProgram;
import com.qt.media.encode.program.utils.FUGLUtils;


public class FUProgramTexture2dWithAlphaES30 extends FUProgram {
    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER_ES30 ="#version 300 es\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "layout (location = 0) in vec4 aPosition;\n" +
            "layout (location = 1) in vec4 aTextureCoord;\n" +
            "out vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";
    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D_ES30 ="#version 300 es\n" +
            "precision mediump float;\n" +
            "in vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "out vec4 vFragColor;\n" +
            "void main() {\n" +
            "    vFragColor = vec4(texture(sTexture, vTextureCoord).rgb,1.0);\n" +
            "}\n";

    private static final String VERTEX_SHADER_ES20 =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D_ES20 =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = vec4(texture2D(sTexture, vTextureCoord).rgb, 1.0);\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int[] mSamplers;

    public FUProgramTexture2dWithAlphaES30() {
        this(getShader(),getShaderExt());
    }
    public FUProgramTexture2dWithAlphaES30(String shader, String shaderExt) {
        super(shader, shaderExt);
        createSimple();
    }
    private static String getShader() {
        int[] NumExtensions = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_NUM_EXTENSIONS, NumExtensions, 0);

        for (int Ext = 0; Ext < NumExtensions[0]; ++Ext) {
            String ext_str = GLES30.glGetStringi(GLES30.GL_EXTENSIONS, Ext);
            if (ext_str.equals("GL_OES_EGL_image_external_essl3")) {
                return VERTEX_SHADER_ES30;
            }
        }
        return VERTEX_SHADER_ES20;
    }
    private static String getShaderExt() {
        int[] NumExtensions = new int[1];
        GLES30.glGetIntegerv(GLES30.GL_NUM_EXTENSIONS, NumExtensions, 0);

        for (int Ext = 0; Ext < NumExtensions[0]; ++Ext) {
            String ext_str = GLES30.glGetStringi(GLES30.GL_EXTENSIONS, Ext);
            if (ext_str.equals("GL_OES_EGL_image_external_essl3")) {
                return FRAGMENT_SHADER_2D_ES30;
            }
        }
        return FRAGMENT_SHADER_2D_ES20;
    }
    public  void createSimple(){
        mSamplers = new int[1];
        GLES30.glGenSamplers(1, mSamplers, 0);
        GLES30.glSamplerParameteri(mSamplers[0], GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glSamplerParameteri(mSamplers[0], GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glSamplerParameteri(mSamplers[0], GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glSamplerParameteri(mSamplers[0], GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glSamplerParameteri(mSamplers[0], GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE);
    }

    @Override
    protected FUDrawable2d getDrawable2d() {
        return new FUDrawable2dFull();
    }

    @Override
    protected void getLocations() {
        maPositionLoc = GLES30.glGetAttribLocation(mProgramHandle, "aPosition");
        FUGLUtils.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES30.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        FUGLUtils.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES30.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        FUGLUtils.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES30.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        FUGLUtils.checkLocation(muTexMatrixLoc, "uTexMatrix");
    }

    @Override
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix) {
        FUGLUtils.checkGlError("draw start");

        // Select the program.
        GLES30.glUseProgram(mProgramHandle);
        FUGLUtils.checkGlError("glUseProgram");

        // Set the texture.
        int location = GLES30.glGetUniformLocation(mProgramHandle, "sTexture");

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        GLES30.glBindSampler(0, mSamplers[0]);
        GLES30.glUniform1i(location, 0);

        // Copy the model / view / projection matrix over.
        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(maPositionLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(maPositionLoc, FUDrawable2d.COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false, FUDrawable2d.VERTEXTURE_STRIDE, mDrawable2d.vertexArray());
        FUGLUtils.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES30.GL_FLOAT, false, FUDrawable2d.TEXTURE_COORD_STRIDE,mDrawable2d.texCoordArray());
        FUGLUtils.checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mDrawable2d.vertexCount());
        FUGLUtils.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES30.glDisableVertexAttribArray(maPositionLoc);
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glUseProgram(0);
    }
    public void release() {
        // 删除纹理采样器对象
        GLES30.glDeleteSamplers(1, mSamplers, 0);
        //删除program
        GLES30.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }
}
