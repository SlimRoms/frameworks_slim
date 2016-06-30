/*
 * Copyright (C) 2016 The SlimRoms Project
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

package org.slim.provider;

public final class SlimSettingsKeys {

    public interface System {

        /**
         * Custom navigation bar intent and action configuration
         * @hide
         */
        public static final String NAVIGATION_BAR_CONFIG = "navigation_bar_config";

         /**
         * Timeout for ambient display notification
         * @hide
         */
        public static final String DOZE_TIMEOUT = "doze_timeout";

        /**
         * Use pick up gesture sensor as doze pulse trigger
         * @hide
         */
        public static final String DOZE_TRIGGER_PICKUP = "doze_trigger_pickup";

        /**
         * Use significant motion sensor as doze pulse trigger
         * @hide
         */
        public static final String DOZE_TRIGGER_SIGMOTION = "doze_trigger_sigmotion";

        /**
         * Use notifications as doze pulse triggers
         * @hide
         */
        public static final String DOZE_TRIGGER_NOTIFICATION = "doze_trigger_notification";

        /**
         * Follow pre-configured doze pulse repeat schedule
         * @hide
         */
        public static final String DOZE_SCHEDULE = "doze_schedule";

        /**
         * Doze pulse screen brightness level
         * @hide
         */
        public static final String DOZE_BRIGHTNESS = "doze_brightness";

        /**
         * Require double tap instead of simple tap to wake from Doze pulse screen
         * @hide
         */
        public static final String DOZE_WAKEUP_DOUBLETAP = "doze_wakeup_doubletap";

        /**
         * Check the proximity sensor during wakeup
         * @hide
         */
        public static final String PROXIMITY_ON_WAKE = "proximity_on_wake";

        /**
         * wake up when plugged or unplugged
         *
         * @hide
         */
        public static final String WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";

        /**
         * Whether the proximity sensor will adjust call to speaker
         * @hide
         */
        public static final String PROXIMITY_AUTO_SPEAKER = "proximity_auto_speaker";

        /**
         * Time delay to activate speaker after proximity sensor triggered
         * @hide
         */
        public static final String PROXIMITY_AUTO_SPEAKER_DELAY = "proximity_auto_speaker_delay";

        /**
         * Whether the proximity sensor will adjust call to speaker,
         * only while in call (not while ringing on outgoing call)
         * @hide
         */
        public static final String PROXIMITY_AUTO_SPEAKER_INCALL_ONLY =
                "proximity_auto_speaker_incall_only";

        /**
         * Whether custom hardware key rebinding is enabled
         * @hide
         */
        public static final String HARDWARE_KEY_REBINDING = "hardware_key_rebinding";

        /**
         * Action to perform when the back key is pressed (default: ACTION_BACK)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_BACK_ACTION = "key_back_action";

        /**
         * Action to perform when the back key is long-pressed. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_BACK_LONG_PRESS_ACTION = "key_back_long_press_action";

        /**
         * Action to perform when the back key is double tapped. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_BACK_DOUBLE_TAP_ACTION = "key_back_double_tap_action";

        /**
         * Action to perform when the home key is pressed. (default: ACTION_HOME)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_HOME_ACTION = "key_home_action";

        /**
         * Action to perform when the home key is long-pressed. (default: ACTION_RECENTS)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_HOME_LONG_PRESS_ACTION = "key_home_long_press_action";

        /**
         * Action to perform when the home key is double taped. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_HOME_DOUBLE_TAP_ACTION = "key_home_double_tap_action";

        /**
         * Action to perform when the menu key is pressed. (default: ACTION_MENU)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_MENU_ACTION = "key_menu_action";

        /**
         * Action to perform when the menu key is long-pressed.
         * (Default is ACTION_NULL on devices with a search key, ACTION_SEARCH on devices without)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_MENU_LONG_PRESS_ACTION = "key_menu_long_press_action";

        /**
         * Action to perform when the menu key is double tapped. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_MENU_DOUBLE_TAP_ACTION = "key_menu_double_tap_action";

        /**
         * Action to perform when the assistant (search) key is pressed. (default: ACTION_SEARCH)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_ACTION = "key_assist_action";

        /**
         * Action to perform when the assistant (search) key is long-pressed.
         * (default: ACTION_VOICE_SEARCH)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_LONG_PRESS_ACTION = "key_assist_long_press_action";

        /**
         * Action to perform when the assistant (search) key is double tapped.
         * (default: ACTION_NULL) (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_DOUBLE_TAP_ACTION = "key_assist_double_tap_action";

        /**
         * Action to perform when the app switch key is pressed. (default: ACTION_RECENTS)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_ACTION = "key_app_switch_action";

        /**
         * Action to perform when the app switch key is long-pressed. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_LONG_PRESS_ACTION =
                "key_app_switch_long_press_action";

        /**
         * Action to perform when the app switch key is double tapped. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_DOUBLE_TAP_ACTION =
                "key_app_switch_double_tap_action";

        /**
         * Action to perform when the camera key is pressed. (default: LAUNCH_CAMERA)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_CAMERA_ACTION = "key_camera_action";

        /**
         * Action to perform when the camera key is long-pressed. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_CAMERA_LONG_PRESS_ACTION =
                "key_camera_long_press_action";

        /**
         * Action to perform when the camera key is double tapped. (default: ACTION_NULL)
         * (See ButtonsConstants.java for valid values)
         * @hide
         */
        public static final String KEY_CAMERA_DOUBLE_TAP_ACTION =
                "key_camera_double_tap_action";
    }

    public interface Secure {
        /**
         * Whether to include options in power menu for rebooting into recovery and bootloader
         * @hide
         */
        public static final String ADVANCED_REBOOT = "advanced_reboot";
    }

    public interface Global {
    }

}
