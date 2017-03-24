SLIM_SYSTEMUI_PATH := $(call my-dir)

# include SlimSystemUI
LOCAL_JAVA_LIBRARIES += org.slim.framework
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-cardview
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.cardview
LOCAL_FULL_LIBS_MANIFEST_FILES := $(SLIM_SYSTEMUI_PATH)/AndroidManifest.xml
LOCAL_SRC_FILES += $(call all-java-files-under, ../../../slim/packages/SlimSystemUI/src)
LOCAL_RESOURCE_DIR += \
    $(SLIM_SYSTEMUI_PATH)/res \
    frameworks/support/v7/cardview/res
