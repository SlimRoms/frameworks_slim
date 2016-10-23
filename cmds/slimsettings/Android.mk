# Copyright 2011 The Android Open Source Project
# Copyright 2016 The SlimRoms Project
#
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_MODULE := slimsettings
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := org.slim.framework
include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := slimsettings
LOCAL_SRC_FILES := slimsettings
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE_TAGS := optional
include $(BUILD_PREBUILT)

