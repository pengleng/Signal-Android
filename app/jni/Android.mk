JNI_DIR := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := native-utils
LOCAL_C_INCLUDES := $(JNI_DIR)/utils/
LOCAL_CFLAGS     += -Wall

LOCAL_SRC_FILES := $(JNI_DIR)/utils/asia_coolapp_chat_util_FileUtils.cpp

include $(BUILD_SHARED_LIBRARY)