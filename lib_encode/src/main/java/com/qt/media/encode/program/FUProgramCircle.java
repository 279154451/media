package com.qt.media.encode.program;

import android.opengl.GLES20;

import com.qt.camera.log.FULogger;
import com.qt.media.encode.program.core.FUDrawable2d;
import com.qt.media.encode.program.core.FUProgram;
import com.qt.media.encode.program.utils.FUGLUtils;


/**
 * Created by hyj on 9/1/21.
 */
public class FUProgramCircle extends FUProgram {

    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "attribute float ratio;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec3 vPosition;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "    vPosition = vec3(gl_Position.xy,ratio);\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "varying vec3 vPosition;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    float distance=sqrt(pow((vPosition.x-0.0),2.0)+pow((vPosition.y/vPosition.z-0.0),2.0));\n" +
                    "    if(vPosition.y>=0.0||distance<=1.0){\n" +
                    "       gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "    }else{\n" +
                    "       gl_FragColor = texture2D(sTexture, vTextureCoord).rgba * vec4(0.0, 0.0, 0.0, 0.0);\n" +
                    "    }\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private float whRadio;//宽高比例
    private int ratio;

    public FUProgramCircle(int width, int height) {
        super(VERTEX_SHADER, FRAGMENT_SHADER_2D);
        whRadio = width * 1.0f / (height * 1.0f);
        FULogger.d("ProgramCircle", "width:" + width + " height:" + height + " whRadio:" + whRadio);
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
        ratio = GLES20.glGetAttribLocation(mProgramHandle, "ratio");
        FUGLUtils.checkLocation(ratio, "ratio");
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

        GLES20.glVertexAttrib1f(ratio, whRadio);
        FUGLUtils.checkGlError("radio");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, FUDrawable2d.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, FUDrawable2d.VERTEXTURE_STRIDE, mDrawable2d.vertexArray());
        FUGLUtils.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        FUGLUtils.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, FUDrawable2d.TEXTURE_COORD_STRIDE, mDrawable2d.texCoordArray());
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

}
