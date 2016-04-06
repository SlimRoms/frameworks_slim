LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_PACKAGE_NAME := org.slim.framework-res
LOCAL_CERTIFICATE := platform

LOCAL_MODULE_TAGS := optional

# Install this alongside the libraries.
LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)

# frameworks resource packages don't like the extra subdir layer
LOCAL_IGNORE_SUBDIR := true

# Create package-export.apk, which other packages can use to get
# PRODUCT-agnostic resource data like IDs and type definitions.
LOCAL_EXPORT_PACKAGE_RESOURCES := true

include $(BUILD_PACKAGE)
