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
         * Disables the hardware key actions
         * @hide
         */
        public static final String DISABLE_HW_KEYS = "disable_hw_keys";

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

        /**
         * Navigation bar button color
         * @hide
         */
        public static final String NAVIGATION_BAR_BUTTON_TINT = "navigation_bar_button_tint";

        /**
         * Option To Colorize Navigation bar buttons in different modes
         * 0 = all, 1 = system icons, 2 = system icons + custom user icons
         * @hide
         */
        public static final String NAVIGATION_BAR_BUTTON_TINT_MODE = "navigation_bar_button_tint_mode";

        /**
         * Navigation bar glow color
         * @hide
         */
        public static final String NAVIGATION_BAR_GLOW_TINT = "navigation_bar_glow_tint";

        /**
         * Wether navigation bar is enabled or not
         * @hide
         */
        public static final String NAVIGATION_BAR_SHOW = "navigation_bar_show";

        /**
         * Wether navigation bar is on landscape on the bottom or on the right
         * @hide
         */
        public static final String NAVIGATION_BAR_CAN_MOVE = "navigation_bar_can_move";

        /**
         * Navigation bar height when it is on protrait
         * @hide
         */
        public static final String NAVIGATION_BAR_HEIGHT = "navigation_bar_height";

        /**
         * Navigation bar height when it is on landscape at the bottom
         * @hide
         */
        public static final String NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";

        /**
         * Navigation bar height when it is on landscape at the right
         * @hide
         */
        public static final String NAVIGATION_BAR_WIDTH = "navigation_bar_width";

        /**
         * Wether the navbar menu button is on the left/right/both
         * @hide
         */
        public static final String MENU_LOCATION = "menu_location";

        /**
         * Wether the navbar menu button should show or not
         * @hide
         */
        public static final String MENU_VISIBILITY = "menu_visibility";

        /**
         * Whether to use slim recents
         * @hide
         */
        public static final String USE_SLIM_RECENTS = "use_slim_recents";

        /**
         * Whether to only show actually running tasks
         * @hide
         */
        public static final String RECENT_SHOW_RUNNING_TASKS = "show_running_tasks";

        /**
         * Amount of apps to show in recents
         * @hide
         */
        public static final String RECENTS_MAX_APPS = "recents_max_apps";

        /**
         * Whether recent panel gravity is left or right (default = Gravity.RIGHT).
         * @hide
         */
        public static final String RECENT_PANEL_GRAVITY = "recent_panel_gravity";

        /**
         * Size of recent panel view in percent (default = 100).
         * @hide
         */
        public static final String RECENT_PANEL_SCALE_FACTOR = "recent_panel_scale_factor";

        /**
         * User favorite tasks for recent panel.
         * @hide
         */
        public static final String RECENT_PANEL_FAVORITES = "recent_panel_favorites";

        /**
         * Recent panel expanded mode (auto = 0, always = 1, never = 2).
         * default = 0.
         *
         * @hide
         */
        public static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";

        /**
         * Recent panel: Show topmost task
         *
         * @hide
         */
        public static final String RECENT_PANEL_SHOW_TOPMOST = "recent_panel_show_topmost";

        /**
         * Recent panel background color
         *
         * @hide
         */
        public static final String RECENT_PANEL_BG_COLOR = "recent_panel_bg_color";

        /**
         * Recent card background color
         *
         * @hide
         */
        public static final String RECENT_CARD_BG_COLOR = "recent_card_bg_color";

        /**
         * Recent card text color
         *
         * @hide
         */
        public static final String RECENT_CARD_TEXT_COLOR = "recent_card_text_color";

        /**
         * Status bar battery %
         * 0: Hide the battery percentage
         * 1: Display the battery percentage inside the icon
         * 2: Display the battery percentage next to the icon
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_PERCENT = "status_bar_battery_percent";

        /**
         * Show or hide clock
         * 0 - hide
         * 1 - show (default)
         * @hide
         */
        public static final String STATUS_BAR_CLOCK = "status_bar_clock";

        /**
         * AM/PM Style for clock options
         * 0 - Normal AM/PM
         * 1 - Small AM/PM
         * 2 - No AM/PM
         * @hide
         */
        public static final String STATUSBAR_CLOCK_AM_PM_STYLE = "statusbar_clock_am_pm_style";

        /**
         * Style of clock
         * 0 - Right Clock
         * 1 - Center Clock
         * 2 - Left Clock
         * @hide
         */
        public static final String STATUSBAR_CLOCK_STYLE = "statusbar_clock_style";

        /**
         * Enable setting clock color
         * 0 - Do not override
         * 1 - Override (force use of custom color at all times)
         * @hide
         */
        public static final String STATUSBAR_CLOCK_COLOR_OVERRIDE =
                "statusbar_clock_color_override";

        /**
         * Setting for clock color
         * @hide
         */
        public static final String STATUSBAR_CLOCK_COLOR = "statusbar_clock_color";

        /**
         * Shows custom date before clock time
         * 0 - No Date
         * 1 - Small Date
         * 2 - Normal Date
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_DISPLAY =
                "statusbar_clock_date_display";

        /**
         * Sets the date string style
         * 0 - Regular style
         * 1 - Lowercase
         * 2 - Uppercase
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_STYLE = "statusbar_clock_date_style";

        /**
         * Stores the java DateFormat string for the date
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_FORMAT = "statusbar_clock_date_format";

        /**
         * Position of date
         * 0 - Left of clock
         * 1 - Right of clock
         * @hide
         */
        public static final String STATUSBAR_CLOCK_DATE_POSITION = "statusbar_clock_date_position";

    }

    public interface Secure {
        /**
         * Whether to include options in power menu for rebooting into recovery and bootloader
         * @hide
         */
        public static final String ADVANCED_REBOOT = "advanced_reboot";

        /**
         * Chamber on / off (custom setting shortcuts)
         * @hide
         */
        public static final String CHAMBER_OF_SECRETS = "chamber_of_secrets";

        /**
         * Display style of the status bar battery information
         * 0: Display the battery an icon in portrait mode
         * 2: Display the battery as a full circle
         * 3: Display the battery as a dotted circle
         * 4: Hide the battery status information
         * 5: Display the battery an icon in landscape mode
         * 6: Display the battery as plain text
         * default: 0
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

        /**
         * Status bar battery %
         * 0: Hide the battery percentage
         * 1: Display the battery percentage inside the icon
         * 2: Display the battery percentage next to the icon
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_PERCENT = "status_bar_battery_percent";

        /**
         * Number of columns to display on the quick settings panel
         * Default is 3
         * @hide
         */
        public static final String QS_NUM_TILE_COLUMNS = "qs_num_tile_columns";

    }

    public interface Global {
    }

}
