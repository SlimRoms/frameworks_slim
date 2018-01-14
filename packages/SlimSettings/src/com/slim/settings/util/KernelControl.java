/*
 * Copyright (C) 2014-2018 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slim.settings.util;

import android.content.Context;
import android.util.SparseArray;

import com.slim.settings.gestures.TouchscreenGestureParser;
import com.slim.settings.gestures.TouchscreenGestureParser.Gesture;
import com.slim.settings.gestures.TouchscreenGestureParser.GesturesArray;

import java.io.File;

import slim.utils.FileUtils;

/*
 * Very ugly class which enables or disables for now
 * all gesture controls on kernel level.
 * We need to do it this way for now to do not break 3rd party kernel.
 * Kernel should have a better per gesture control but as long
 * this is not changed by the manufacture we would break gesture control on every
 * 3rd party kernel. Hence we do it this way for now.
 */

public final class KernelControl {

    // Notification slider
    public static final String SLIDER_SWAP_NODE = "/proc/s1302/key_rep";
    public static final String KEYCODE_SLIDER_TOP = "/proc/tri-state-key/keyCode_top";
    public static final String KEYCODE_SLIDER_MIDDLE = "/proc/tri-state-key/keyCode_middle";
    public static final String KEYCODE_SLIDER_BOTTOM = "/proc/tri-state-key/keyCode_bottom";

    private KernelControl() {
        // this class is not supposed to be instantiated
    }

    /**
     * Enable or disable gesture control.
     */
    public static void enableGestures(Context context, boolean enable) {
        GesturesArray gestures = TouchscreenGestureParser.parseGestures(context);
        for (Gesture gesture : gestures) {
            if (new File(gesture.path).exists()) {
                FileUtils.writeLine(gesture.path, enable ? "1" : "0");
            }
        }
    }

    /**
     * Do we have touch control at all?
     */
    public static boolean hasTouchscreenGestures(Context context) {
        GesturesArray gestures = TouchscreenGestureParser.parseGestures(context);
        for (Gesture gesture : gestures) {
            if (new File(gesture.path).exists()) {
                // We only need atleast one
                return true;
            }
        }
        return false;
    }

    public static boolean hasSlider() {
        return new File(KEYCODE_SLIDER_TOP).exists() &&
            new File(KEYCODE_SLIDER_MIDDLE).exists() &&
            new File(KEYCODE_SLIDER_BOTTOM).exists() &&
            new File(SLIDER_SWAP_NODE).exists();
    }

}
