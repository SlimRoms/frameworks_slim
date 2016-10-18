# Copyright (C) 2016 The SlimRoms Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := org.slim.framework

LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_SRC_FILES += $(call all-Iaidl-files-under,src)

LOCAL_JAVA_LIBRARIES := \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-annotations

slim_platform_res := APPS/org.slim.framework-res_intermediates/src

LOCAL_INTERMEDIATE_SOURCES := \
    $(slim_platform_res)/org/slim/framework/R.java \
    $(slim_platform_res)/org/slim/framework/internal/R.java \
    $(slim_platform_res)/org/slim/framework/Manifest.java

include $(BUILD_JAVA_LIBRARY)
slim_framework_module := $(LOCAL_INSTALLED_MODULE)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
slim_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.slim.framework-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(slim_framework_res_R_stamp)
$(built_dex_intermediate): $(slim_framework_res_R_stamp)

$(slim_framework_module): | $(dir $(slim_framework_module))org.slim.framework-res.apk

slim_framework_built := $(call java-lib-deps, org.slim.framework)

include $(call all-makefiles-under,$(LOCAL_PATH))

