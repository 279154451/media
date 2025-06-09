package com.qt.media.encode.entity

/**
 * 创建时间：2023/8/30
 * 创建人：singleCode
 * 功能描述：openGL渲染后的纹理帧
 * @param textureId openGL渲染后的纹理
 * @param texMatrix 纹理矩阵
 * @param mvpMatrix mvp矩阵
 * @param format 纹理帧格式
 * @param width 纹理宽
 * @param height 纹理高
 **/
data class TextureFrame(
    val textureId: Int,
    val texMatrix: FloatArray,
    val mvpMatrix: FloatArray,
    val format: FrameFormat,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "TextureFrame(textureId=$textureId, texMatrix=${texMatrix.contentToString()}, mvpMatrix=${mvpMatrix.contentToString()}, format=$format, width=$width, height=$height)"
    }


}
