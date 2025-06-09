package com.qt.media.encode.entity


/**
 * 创建时间：2023/8/30
 * 创建人：singleCode
 * 功能描述：
 * 帧数据的格式
 **/
enum class FrameFormat {
    FORMAT_OES,
    FORMAT_RGBA,
    FORMAT_BGRA,
    FORMAT_YV12,
    FORMAT_YU12,
    FORMAT_NV12,
    FORMAT_NV21
}