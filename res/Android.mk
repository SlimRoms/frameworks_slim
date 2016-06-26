LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_PACKAGE_NAME := org.slim.framework-res
LOCAL_CERTIFICATE := platform
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, res)

# 0x3f/one less than app id
LOCAL_AAPT_FLAGS += -x 55

LOCAL_MODULE_TAGS := optional

# frameworks resource packages don't like the extra subdir layer
LOCAL_IGNORE_SUBDIR := true

# Install this alongside the libraries.
LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)

# Create package-export.apk, which other packages can use to get
# PRODUCT-agnostic resource data like IDs and type definitions.
LOCAL_EXPORT_PACKAGE_RESOURCES := true

include $(BUILD_PACKAGE)
