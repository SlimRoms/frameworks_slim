LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_JAVA_LIBRARIES := telephony-common ims-common

LOCAL_PACKAGE_NAME := SlimSettingsProvider
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

## Slim Framework
include frameworks/opt/slim/common.mk

include $(BUILD_PACKAGE)

########################
include $(call all-makefiles-under,$(LOCAL_PATH))
