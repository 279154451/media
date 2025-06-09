package com.qt.media.encode.program;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import com.qt.media.encode.program.core.FUDrawable2d;
import com.qt.media.encode.program.core.FUProgram;
import com.qt.media.encode.program.utils.FUGLUtils;

import java.util.concurrent.LinkedBlockingQueue;

public class FUProgramBitmapWithBrightness extends FUProgram {
    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "attribute float brightness;\n" +
                    "varying float uBrightness;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    uBrightness = brightness;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "varying float uBrightness;\n" +
                    "void main() {\n" +
                    "    vec4 color = texture2D(sTexture, vTextureCoord);\n" +
                    "    vec3 gammaColor = pow(color.rgb,  vec3(2.2));\n"+
                    "    vec3 brightened = gammaColor * uBrightness;\n"+
                    "    vec3 adjusted  = pow(brightened, vec3(1.0/2.2));\n"+
                    "    gl_FragColor = vec4(adjusted, color.a);\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int brightness;

    public FUProgramBitmapWithBrightness() {
        super(VERTEX_SHADER, FRAGMENT_SHADER_2D);
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
        brightness = GLES20.glGetAttribLocation(mProgramHandle, "brightness");
        FUGLUtils.checkLocation(brightness, "brightness");
    }
    private LinkedBlockingQueue<Integer> textureIdQueue = new LinkedBlockingQueue<>();
    private void initTextureIdQueue(){
        if(textureIdQueue.isEmpty()){
            for (int i = 0; i < 2; i++) {//两张纹理交替使用
                int id = FUGLUtils.createTextureObject(GLES20.GL_TEXTURE_2D);
                textureIdQueue.offer(id);
            }
        }
    }
    private int pollTextureId(){
        initTextureIdQueue();
        Integer id = textureIdQueue.poll();
        if(id == null){
            id = FUGLUtils.createTextureObject(GLES20.GL_TEXTURE_2D);
            textureIdQueue.offer(id);
        }
        return id;
    }
    private void offerTextureId(int id){
        boolean result = textureIdQueue.offer(id);
        if(!result){
            Log.e("FUProgramBitmap2d","offerTextureId error:"+id);
        }
    }
    private void releaseTextureIds(){
        while (!textureIdQueue.isEmpty()){
            Integer id = textureIdQueue.poll();
            if(id!= null){
                int[] textures = new int[1];
                textures[0] = id;
                FUGLUtils.deleteTextures(textures);
            }
        }
    }

    /**
     *
     * @param bitmap
     * @param texMatrix
     * @param mvpMatrix
     * @param uBrightness 亮度控制参数 默认1.0（建议范围0.5-3.0）
     */
    public void drawFrame(Bitmap bitmap, float[] texMatrix, float[] mvpMatrix,float uBrightness) {
        FUGLUtils.checkGlError("draw start");
        int bitmapTextureId = pollTextureId();
        //开启混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        FUGLUtils.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureId);
        //set image
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        //create and use mipmap
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        FUGLUtils.checkGlError("glUniformMatrix4fv");

        GLES20.glVertexAttrib1f(brightness, uBrightness);
        FUGLUtils.checkGlError("brightness");

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
        GLES20.glDisable(GLES20.GL_BLEND);//关闭混合
        offerTextureId(bitmapTextureId);
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

    @Override
    public void release() {
        super.release();
        releaseTextureIds();
    }
}
