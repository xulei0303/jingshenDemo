#include <jni.h>
#define LOG_TAG "libjni_arcsoft_real_bokeh"
#include "arcrealbokeh.h"
#include <string.h>
#include <time.h>
#include <fcntl.h>
#include <unistd.h>
#include "arcsoft_dualcam_image_refocus.h"
#include "amcomdef.h"
#include "ammem.h"
#include <stdlib.h>

#include <android/log.h>
#include <malloc.h>

#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO  , LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__)


/*
 * typedef enum android_LogPriority {
    ANDROID_LOG_UNKNOWN = 0,
    ANDROID_LOG_DEFAULT,
    ANDROID_LOG_VERBOSE,
    ANDROID_LOG_DEBUG,
    ANDROID_LOG_INFO,
    ANDROID_LOG_WARN,
    ANDROID_LOG_ERROR,
    ANDROID_LOG_FATAL,
    ANDROID_LOG_SILENT,
} android_LogPriority;
*/

struct fields_t {
    jfieldID    context;
};

static fields_t fields;

static void* getHandle(JNIEnv* env, jobject thiz){
    return (void*)env->GetLongField(thiz, fields.context);
}

static void setContext(JNIEnv* env, jobject thiz, void* handle) {
    ALOGD("JNI setContext start ");
    env->SetLongField(thiz, fields.context, (jlong)handle);
    ALOGD("JNI setContext end ");
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv *env;
    ALOGI("JNI_OnLoad!");
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed");
        return -1;
    }
    return JNI_VERSION_1_4;
}

MHandle mHandle = MNull;
MInt32 lFocusMode = ARC_DCIR_POST_REFOCUS_MODE;

JNIEXPORT jint JNICALL Java_com_sprd_gallery3d_refocusimage_arcrealtime_ArcRealBokeh_init
  (JNIEnv *env, jobject obj){
    jclass clazz;
    clazz = env->GetObjectClass(obj);
    if (clazz == NULL) {
        ALOGD("ArcSoftRefocus_init,can't access class.");
        return -1;
    }
    fields.context = env->GetFieldID(clazz, "mNativeContext", "J");
    if (fields.context == NULL) {
        ALOGD("ArcSoftRefocus_init,can't get context.");
        return -1;
    }
    const MPBASE_Version * versionInfo = ARC_DCIR_GetVersion();
    ALOGD("arcsoft refocus lib version = [%s]", versionInfo->Version);

    MRESULT result = ARC_DCIR_Init(&mHandle, lFocusMode);
    ALOGD("ARC_DCIR_Init result  = %ld",result);
    if(MOK == result){
        ALOGD("ARC_DCIR_Init success, mHandle = [%p]",mHandle);
        setContext(env,obj,mHandle);
    }else{
        ALOGD("ARC_DCIR_Init fail!");
    }
    return (int)result;
  }

JNIEXPORT jint JNICALL Java_com_sprd_gallery3d_refocusimage_arcrealtime_ArcRealBokeh_unInit
  (JNIEnv *env, jobject obj){
    void* uninit_handle = getHandle(env, obj);
    ALOGD("ARC_DCIR_Uninit, uninit_handle = [%p]", uninit_handle);
    MRESULT result = ARC_DCIR_Uninit(&uninit_handle);
    ALOGD("ARC_DCIR_Uninit result = %ld",result);
    if(MOK == result){
        ALOGD("ARC_DCIR_Uninit success!");
    }else{
        ALOGD("ARC_DCIR_Uninit fail!");
    }
    return (int)result;
  }

JNIEXPORT jbyteArray JNICALL Java_com_sprd_gallery3d_refocusimage_arcrealtime_ArcRealBokeh_refocus
  (JNIEnv *env, jobject obj, jint w, jint h, jint disparitySize, jbyteArray in_array,
   jbyteArray out_array, jbyteArray depth, jint x, jint y, jint blurIntensity){

    ALOGD("ArcSoftRefocus refocus start ");
    ARC_DCIR_REFOCUS_PARAM rfParam;
    MPOINT focus;
    focus.x = (MInt32)x;
    focus.y = (MInt32)y;
    rfParam.ptFocus = focus;
    rfParam.i32BlurIntensity = (MInt32)blurIntensity;
    ALOGD("arc refocus point (%d,%d), blurIntensity = %d", x, y, blurIntensity);
    ALOGD("arc refocus yuv width: %d, height :%d, depth size: %d ", w, h, disparitySize);

    jbyte *depth_data = env->GetByteArrayElements(depth, NULL);
    jbyte *in_array_data = env->GetByteArrayElements(in_array, NULL);
    jbyte *out_array_data = env->GetByteArrayElements(out_array, NULL);

    ASVLOFFSCREEN leftImg;
    ASVLOFFSCREEN dstImg;
    MLong lWidth = (MInt32)w;
    MLong lHeight = (MInt32)h;

    MByte * pLeftImgDataY = (MByte *)in_array_data;
    MByte * pLeftImgDataUV = pLeftImgDataY + lWidth * lHeight;
    MByte * pDstDataY = (MByte *)out_array_data;
    MByte * pDstDataUV = pDstDataY + lWidth * lHeight;

    memset(&leftImg, 0, sizeof(ASVLOFFSCREEN));
    leftImg.u32PixelArrayFormat = ASVL_PAF_NV21;
    leftImg.i32Width = lWidth;
    leftImg.i32Height = lHeight;
    leftImg.pi32Pitch[0] = lWidth;
    leftImg.ppu8Plane[0] = pLeftImgDataY;
    leftImg.pi32Pitch[1] = lWidth / 2 * 2;
    leftImg.ppu8Plane[1] = pLeftImgDataUV;

    memset(&dstImg, 0, sizeof(ASVLOFFSCREEN));
    dstImg.u32PixelArrayFormat = ASVL_PAF_NV21;
    dstImg.i32Width = lWidth;
    dstImg.i32Height = lHeight;
    dstImg.pi32Pitch[0] = lWidth;
    dstImg.ppu8Plane[0] = pDstDataY;
    dstImg.pi32Pitch[1] = lWidth / 2 * 2;
    dstImg.ppu8Plane[1] = pDstDataUV;


    ALOGD("ArcSoftRefocus ARC_DCIR_Process start ");
    void* refocus_handle = getHandle(env, obj);
    ALOGD("ArcSoftRefocus ARC_DCIR_Process refocus_handle = [%p] ", refocus_handle);
    MRESULT result = ARC_DCIR_Process(refocus_handle, (MVoid *)depth_data, (MInt32)disparitySize, &leftImg, &rfParam, &dstImg);
    ALOGD("ARC_DCIR_Process result = %ld",result);
    ALOGD("ArcSoftRefocus ARC_DCIR_Process end ");

    env->ReleaseByteArrayElements(depth, depth_data, JNI_ABORT);
    env->ReleaseByteArrayElements(in_array, in_array_data, JNI_ABORT);
    env->ReleaseByteArrayElements(out_array, out_array_data, JNI_ABORT);

    if(MOK == result){
        ALOGD("ARC_DCIR_Process sucess !");
        return out_array;
    }else{
        ALOGD("ARC_DCIR_Process fail !");
       return in_array;
    }

  }

