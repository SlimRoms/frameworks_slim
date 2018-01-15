/*
 * Copyright (C) 2014-2018 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package slim.utils;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.KeyEvent;

import slim.action.ActionConstants;
import slim.provider.SlimSettings;

public class HwKeyHelper {

    /**
     * Masks for checking presence of hardware keys.
     * Must match values in core/res/res/values/config.xml
     */
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;

    // These need to match the documentation/constant in
    // core/res/res/values/config.xml
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_RECENT_SYSTEM_UI = 1;
    static final int LONG_PRESS_HOME_ASSIST = 2;

    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;

    public static String getDefaultTapActionForKeyCode(Context context, int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return ActionConstants.ACTION_HOME;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return ActionConstants.ACTION_MENU;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            return ActionConstants.ACTION_BACK;
        } else if (keyCode == KeyEvent.KEYCODE_ASSIST) {
            return ActionConstants.ACTION_SEARCH;
        } else if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return ActionConstants.ACTION_RECENTS;
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            return ActionConstants.ACTION_CAMERA;
        }
        return ActionConstants.ACTION_NULL;
    }

    public static String getDefaultLongPressActionForKeyCode(Context context, int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            int longPressOnHome = context.getResources().getInteger(
                    com.android.internal.R.integer.config_longPressOnHomeBehavior);
            if (longPressOnHome == LONG_PRESS_HOME_RECENT_SYSTEM_UI) {
                return ActionConstants.ACTION_RECENTS;
            } else if (longPressOnHome == LONG_PRESS_HOME_ASSIST) {
                return ActionConstants.ACTION_SEARCH;
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return ActionConstants.ACTION_SEARCH;
        } else if (keyCode == KeyEvent.KEYCODE_ASSIST) {
            return ActionConstants.ACTION_VOICE_SEARCH;
        }
        return ActionConstants.ACTION_NULL;
    }

    public static String getDefaultDoubleTapActionForKeyCode(Context context, int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            int doubleTapOnHome = context.getResources().getInteger(
                    com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
            if (doubleTapOnHome == DOUBLE_TAP_HOME_RECENT_SYSTEM_UI) {
                return ActionConstants.ACTION_RECENTS;
            }
        }
        return ActionConstants.ACTION_NULL;
    }
}

