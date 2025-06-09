package com.qt.media.test.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue


object BitmapBufferPool {
    private var queue:ConcurrentLinkedQueue<Bitmap> = ConcurrentLinkedQueue()

    private fun initQueue(width: Int,height: Int){
        if(queue.isEmpty()){
            for (i in 0..2) {
                if(i ==0){
                    val colorToBitmap = colorToBitmap(width, height, Color.RED)
                    queue.offer(colorToBitmap)
                }else if (i==1){
                    val colorToBitmap = colorToBitmap(width, height, Color.BLUE)
                    queue.offer(colorToBitmap)
                }else {
                    val colorToBitmap = colorToBitmap(width, height, Color.GREEN)
                    queue.offer(colorToBitmap)
                }
            }
        }
    }
    fun colorToBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    fun getBitmapBuffer(width: Int,height: Int):ByteArray?{
        initQueue(width, height)
        val bitmap = queue.poll()
        if(bitmap!= null){
            val bytes = readRgbaByteFromBitmap(bitmap)
            queue.offer(bitmap)
            return bytes
        }else{
            return null
        }
    }
    fun getBitmapBuffer(width: Int,height: Int,buffer:ByteArray):ByteArray?{
        initQueue(width, height)
        val bitmap = queue.poll()
        if(bitmap!= null){
            val bytes = readRgbaByteFromBitmap(bitmap,buffer)
            queue.offer(bitmap)
            return bytes
        }else{
            return null
        }
    }
    private fun readRgbaByteFromBitmap(bitmap: Bitmap,buffer:ByteArray): ByteArray? {
        if (bitmap.byteCount == 0) {
            return null
        }
        val rgbaBuffer = ByteBuffer.wrap(buffer)
        bitmap.copyPixelsToBuffer(rgbaBuffer)
        return buffer
    }
    private fun readRgbaByteFromBitmap(bitmap: Bitmap): ByteArray? {
        if (bitmap.byteCount == 0) {
            return null
        }
        val bytes = ByteArray(bitmap.byteCount)
        val rgbaBuffer = ByteBuffer.wrap(bytes)
        bitmap.copyPixelsToBuffer(rgbaBuffer)
        return bytes
    }
}