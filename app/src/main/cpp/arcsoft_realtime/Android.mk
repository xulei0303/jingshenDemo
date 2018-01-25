LOCAL_PATH := $(call my-dir)
ifeq ($(strip $(TARGET_BOARD_ARCSOFT_BOKEH_MODE_SUPPORT)),true)

include $(CLEAR_VARS)
LOCAL_LDLIBS := -lm -llog
LOCAL_SRC_FILES := arcrealbokeh.cpp
LOCAL_MODULE := libjni_arcsoft_real_bokeh
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libutils libcutils libarcsoft_dualcam_refocus libmpbase
include $(BUILD_SHARED_LIBRARY)

endif