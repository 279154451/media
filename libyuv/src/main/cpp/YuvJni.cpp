#include <jni.h>
#include <string>
#include "libyuv.h"

//分别用来存储1420，1420缩放，I420旋转和镜像的数据
static jbyte *Src_i420_data;
static jbyte *Src_i420_data_scale;
static jbyte *Src_i420_data_rotate;

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    //进行释放
    free(Src_i420_data);
    free(Src_i420_data_scale);
    free(Src_i420_data_rotate);
}

void scaleI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint dst_width,
               jint dst_height, jint mode) {

    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);
    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);
    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::I420Scale((const uint8 *) src_i420_y_data, width,
                      (const uint8 *) src_i420_u_data, width >> 1,
                      (const uint8 *) src_i420_v_data, width >> 1,
                      width, height,
                      (uint8 *) dst_i420_y_data, dst_width,
                      (uint8 *) dst_i420_u_data, dst_width >> 1,
                      (uint8 *) dst_i420_v_data, dst_width >> 1,
                      dst_width, dst_height,
                      (libyuv::FilterMode) mode);
}

void rotateI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data, jint degree) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    //要注意这里的width和height在旋转之后是相反的
    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate270) {
        libyuv::I420Rotate((const uint8 *) src_i420_y_data, width,
                           (const uint8 *) src_i420_u_data, width >> 1,
                           (const uint8 *) src_i420_v_data, width >> 1,
                           (uint8 *) dst_i420_y_data, height,
                           (uint8 *) dst_i420_u_data, height >> 1,
                           (uint8 *) dst_i420_v_data, height >> 1,
                           width, height,
                           (libyuv::RotationMode) degree);
    }
}

void mirrorI420(jbyte *src_i420_data, jint width, jint height, jbyte *dst_i420_data) {
    jint src_i420_y_size = width * height;
    jint src_i420_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_i420_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_i420_y_size + src_i420_u_size;

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + src_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + src_i420_y_size + src_i420_u_size;

    libyuv::I420Mirror((const uint8 *) src_i420_y_data, width,
                       (const uint8 *) src_i420_u_data, width >> 1,
                       (const uint8 *) src_i420_v_data, width >> 1,
                       (uint8 *) dst_i420_y_data, width,
                       (uint8 *) dst_i420_u_data, width >> 1,
                       (uint8 *) dst_i420_v_data, width >> 1,
                       width, height);
}


void nv21ToI420(jbyte *src_nv21_data, jint width, jint height, jbyte *src_i420_data) {
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;


    libyuv::NV21ToI420((const uint8 *) src_nv21_y_data, width,
                       (const uint8 *) src_nv21_vu_data, width,
                       (uint8 *) src_i420_y_data, width,
                       (uint8 *) src_i420_u_data, width >> 1,
                       (uint8 *) src_i420_v_data, width >> 1,
                       width, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_init(JNIEnv *env, jclass type, jint width, jint height, jint dst_width,
                                  jint dst_height) {
    Src_i420_data = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
    Src_i420_data_scale = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
    Src_i420_data_rotate = (jbyte *) malloc(sizeof(jbyte) * dst_width * dst_height * 3 / 2);
}

//extern "C"
//JNIEXPORT void JNICALL
//Java_com_libyuv_util_YuvUtil_compressYUV(JNIEnv *env, jclass type,
//                                         jbyteArray src_, jint width,
//                                         jint height, jbyteArray dst_,
//                                         jint dst_width, jint dst_height,
//                                         jint mode, jint degree,
//                                         jboolean isMirror) {
//    jbyte *Src_data = env->GetByteArrayElements(src_, NULL);
//    jbyte *Dst_data = env->GetByteArrayElements(dst_, NULL);
//    //nv21转化为i420
//    nv21ToI420(Src_data, width, height, Src_i420_data);
//    //进行缩放的操作
//    scaleI420(Src_i420_data, width, height, Src_i420_data_scale, dst_width, dst_height, mode);
//    if (isMirror) {
//        //进行旋转的操作
//        rotateI420(Src_i420_data_scale, dst_width, dst_height, Src_i420_data_rotate, degree);
//        //因为旋转的角度都是90和270，那后面的数据width和height是相反的
//        mirrorI420(Src_i420_data_rotate, dst_height, dst_width, Dst_data);
//    } else {
//        rotateI420(Src_i420_data_scale, dst_width, dst_height, Dst_data, degree);
//    }
//    env->ReleaseByteArrayElements(dst_, Dst_data, 0);
//}
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_compressYUV(JNIEnv *env, jclass type,
                                         jbyteArray nv21Src, jint width,
                                         jint height, jbyteArray i420Dst,
                                         jint dst_width, jint dst_height,
                                         jint mode, jint degree,
                                         jboolean isMirror) {

    jbyte *src_nv21_data = env->GetByteArrayElements(nv21Src, NULL);
    jbyte *dst_i420_data = env->GetByteArrayElements(i420Dst, NULL);
    jbyte *tmp_dst_i420_data = NULL;

    // nv21转化为i420
    jbyte *i420_data = (jbyte *) malloc(sizeof(jbyte) * width * height * 3 / 2);
    nv21ToI420(src_nv21_data, width, height, i420_data);
    tmp_dst_i420_data = i420_data;

    // 镜像
    jbyte *i420_mirror_data = NULL;
    if(isMirror){
        i420_mirror_data = (jbyte *)malloc(sizeof(jbyte) * width * height * 3 / 2);
        mirrorI420(tmp_dst_i420_data, width, height, i420_mirror_data);
        tmp_dst_i420_data = i420_mirror_data;
    }

    // 缩放
    jbyte *i420_scale_data = NULL;
    if(width != dst_width || height != dst_height){
        i420_scale_data = (jbyte *)malloc(sizeof(jbyte) * width * height * 3 / 2);
        scaleI420(tmp_dst_i420_data, width, height, i420_scale_data, dst_width, dst_height, mode);
        tmp_dst_i420_data = i420_scale_data;
        width = dst_width;
        height = dst_height;
    }

    // 旋转
    jbyte *i420_rotate_data = NULL;
    if (degree == libyuv::kRotate90 || degree == libyuv::kRotate180 || degree == libyuv::kRotate270){
        i420_rotate_data = (jbyte *)malloc(sizeof(jbyte) * width * height * 3 / 2);
        rotateI420(tmp_dst_i420_data, width, height, i420_rotate_data, degree);
        tmp_dst_i420_data = i420_rotate_data;
    }

    // 同步数据
    // memcpy(dst_i420_data, tmp_dst_i420_data, sizeof(jbyte) * width * height * 3 / 2);
    jint len = env->GetArrayLength(i420Dst);
    memcpy(dst_i420_data, tmp_dst_i420_data, len);
    tmp_dst_i420_data = NULL;
    env->ReleaseByteArrayElements(i420Dst, dst_i420_data, 0);

    // 释放
    if(i420_data != NULL) free(i420_data);
    if(i420_mirror_data != NULL) free(i420_mirror_data);
    if(i420_scale_data != NULL) free(i420_scale_data);
    if(i420_rotate_data != NULL) free(i420_rotate_data);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_cropYUV(JNIEnv *env, jclass type, jbyteArray src_, jint width,
                                     jint height, jbyteArray dst_, jint dst_width, jint dst_height,
                                     jint left, jint top) {
    //裁剪的区域大小不对
    if (left + dst_width > width || top + dst_height > height) {
        return;
    }

    //left和top必须为偶数，否则显示会有问题
    if (left % 2 != 0 || top % 2 != 0) {
        return;
    }

    jint src_length = env->GetArrayLength(src_);
    jbyte *src_i420_data = env->GetByteArrayElements(src_, NULL);
    jbyte *dst_i420_data = env->GetByteArrayElements(dst_, NULL);


    jint dst_i420_y_size = dst_width * dst_height;
    jint dst_i420_u_size = (dst_width >> 1) * (dst_height >> 1);

    jbyte *dst_i420_y_data = dst_i420_data;
    jbyte *dst_i420_u_data = dst_i420_data + dst_i420_y_size;
    jbyte *dst_i420_v_data = dst_i420_data + dst_i420_y_size + dst_i420_u_size;

    libyuv::ConvertToI420((const uint8 *) src_i420_data, src_length,
                          (uint8 *) dst_i420_y_data, dst_width,
                          (uint8 *) dst_i420_u_data, dst_width >> 1,
                          (uint8 *) dst_i420_v_data, dst_width >> 1,
                          left, top,
                          width, height,
                          dst_width, dst_height,
                          libyuv::kRotate0, libyuv::FOURCC_I420);

    env->ReleaseByteArrayElements(dst_, dst_i420_data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_yuvI420ToNV21(JNIEnv *env, jclass type, jbyteArray i420Src,
                                           jbyteArray nv21Src,
                                           jint width, jint height) {

    jbyte *src_i420_data = env->GetByteArrayElements(i420Src, NULL);
    jbyte *src_nv21_data = env->GetByteArrayElements(nv21Src, NULL);

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    jbyte *src_nv21_y_data = src_nv21_data;
    jbyte *src_nv21_vu_data = src_nv21_data + src_y_size;


    libyuv::I420ToNV21(
            (const uint8 *) src_i420_y_data, width,
            (const uint8 *) src_i420_u_data, width >> 1,
            (const uint8 *) src_i420_v_data, width >> 1,
            (uint8 *) src_nv21_y_data, width,
            (uint8 *) src_nv21_vu_data, width,
            width, height);
}

// i420 --> nv12
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_yuvI420ToNV12(JNIEnv *env, jclass type, jbyteArray i420src,
                                           jbyteArray nv12Dst,
                                           jint width, jint height) {


    jbyte *src_i420_data = env->GetByteArrayElements(i420src, NULL);
    jbyte *src_nv12_data = env->GetByteArrayElements(nv12Dst, NULL);

    jint src_y_size = width * height;
    jint src_u_size = src_y_size >> 2;

    jbyte *src_y = src_i420_data;
    jbyte *src_u = src_i420_data + src_y_size;
    jbyte *src_v = src_i420_data + src_y_size + src_u_size;

    jint dst_y_size = width * height;
    jbyte *dst_y = src_nv12_data;
    jbyte *dst_uv = src_nv12_data + dst_y_size;

    libyuv::I420ToNV12(
            (uint8_t *) src_y, width,
            (uint8_t *) src_u, width >> 1,
            (uint8_t *) src_v, width >> 1,
            (uint8_t *) dst_y, width,
            (uint8_t *) dst_uv, width,
            width, height
    );
}


// nv21 --> argb
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_NV21ToARGB(JNIEnv *env, jclass type, jbyteArray nv21src,
                                           jbyteArray argbDst,
                                           jint width, jint height) {

    jbyte *src_nv21_data = env->GetByteArrayElements(nv21src, NULL);
    jbyte *src_argb_data = env->GetByteArrayElements(argbDst, NULL);
    jbyte *src_y = src_nv21_data;
    jbyte *src_vu = src_nv21_data + width * height;
    libyuv::NV21ToARGB(
            (uint8_t *) src_y, width,
            (uint8_t *) src_vu, width,
            (uint8_t *) src_argb_data,
            width*4,
            width, height
    );
}
// nv21 --> argb
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_NV12ToARGB(JNIEnv *env, jclass type, jbyteArray nv12src,
                                        jbyteArray argbDst,
                                        jint width, jint height) {

    jbyte *src_nv12_data = env->GetByteArrayElements(nv12src, NULL);
    jbyte *src_argb_data = env->GetByteArrayElements(argbDst, NULL);
    jbyte *src_y = src_nv12_data;
    jbyte *src_uv = src_nv12_data + width * height;
    libyuv::NV12ToARGB(
            (uint8_t *) src_y, width,
            (uint8_t *) src_uv, width,
            (uint8_t *) src_argb_data,
            width*4,
            width, height
    );
}

// nv21 --> argb
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_BGRAToARGB(JNIEnv *env, jclass type, jbyteArray bgraSrc,
                                        jbyteArray argbDst,
                                        jint width, jint height) {
    jbyte *src_bgra_data = env->GetByteArrayElements(bgraSrc, NULL);
    jbyte *src_argb_data = env->GetByteArrayElements(argbDst, NULL);
    libyuv::BGRAToARGB(
            (uint8_t *) src_bgra_data, width*4,
            (uint8_t *) src_argb_data,
            width*4,
            width, height
    );
}

// nv21 --> argb
extern "C"
JNIEXPORT void JNICALL
Java_com_libyuv_util_YuvUtil_RGBAToARGB(JNIEnv *env, jclass type, jbyteArray rgbaSrc,
                                        jbyteArray argbDst,
                                        jint width, jint height) {
    jbyte *src_rgba_data = env->GetByteArrayElements(rgbaSrc, NULL);
    jbyte *src_argb_data = env->GetByteArrayElements(argbDst, NULL);
    libyuv::RGBAToARGB(
            (uint8_t *) src_rgba_data, width*4,
            (uint8_t *) src_argb_data,
            width*4,
            width, height
    );
}