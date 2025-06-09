package com.qt.media.encode.help

import android.graphics.Bitmap
import com.qt.camera.log.FULogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    fun getExternalRootGifFilePath(): String {
        val filePath = "${getExternalRootFileDir()}/image/${System.currentTimeMillis()}.gif"
        val file = getExternalFile(filePath, true)
        return file.absolutePath
    }
    fun getExternalRootWebpFilePath(): String {
        val filePath = "${getExternalRootFileDir()}/image/${System.currentTimeMillis()}.webp"
        val file = getExternalFile(filePath, true)
        return file.absolutePath
    }
    fun copyFile(sourceFile: File, targetFile: File): Boolean {
        targetFile.parent?.let {
            createFileDir(it)
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
        var bis: BufferedInputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            bis = BufferedInputStream(FileInputStream(sourceFile))
            bos = BufferedOutputStream(FileOutputStream(targetFile))
            val bytes = ByteArray(1024 * 10)
            var length: Int
            while (bis.read(bytes).also { length = it } != -1) {
                bos.write(bytes, 0, length)
            }
            bos.flush()
        } catch (e: IOException) {
            FULogger.e("GifFileUtils") { "copyFile failed, sourceFile:${sourceFile.absolutePath} message:${e.message}" }
            return false
        } finally {
            try {
                bis?.close()
            } catch (e: IOException) {
                FULogger.e("GifFileUtils") { "bis close failed, message:${e.message}" }
            }
            try {
                bos?.close()
            } catch (e: IOException) {
                FULogger.e("GifFileUtils") { "bos close failed, message:${e.message}" }
            }
        }
        return true
    }
    //region 文件管理
    /**
     * 获取外部存储目录
     * @return File 外部存储目录
     */
    @JvmStatic
    fun getExternalRootFileDir(): File {
        return ReflectApplication.application.getExternalFilesDir(null) ?: ReflectApplication.application.filesDir
    }
    /**
     * 将 Bitmap 保存到本地
     * @param targetFilePath String 本地路径
     * @param bitmap Bitmap 图像
     * @param isJpeg Boolean 是否是JPEG格式
     * @return Boolean 是否执行成功
     */
    fun addBitmapToExternal(targetFilePath: String, bitmap: Bitmap, isJpeg: Boolean): Boolean {
        val file: File = getExternalFile(targetFilePath, true)
        var fos: FileOutputStream? = null
        return try {
            fos = FileOutputStream(file)
            bitmap.compress(if (isJpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            true
        } catch (e: IOException) {
            FULogger.e("IFUImageEncoder") { "addBitmapToExternal failed, targetPath:$targetFilePath message:${e.message}" }
            false
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                FULogger.e("IFUImageEncoder") { "fos close failed, message:${e.message}" }
            }
        }
    }
    fun getExternalFile(filePath: String, needDeleteOriginal: Boolean = false): File {
        val targetFile = File(filePath)
        targetFile.parent?.let {
            createFileDir(it)
        }
        if (needDeleteOriginal && targetFile.exists()) {
            targetFile.delete()
        }
        return targetFile
    }
    fun createFileDir(dirPath: String): Boolean {
        val dir = File(dirPath)
        return if (!dir.exists()) {
            dir.mkdirs()
        } else true
    }
}