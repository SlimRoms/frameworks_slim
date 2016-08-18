/*
 * Copyright (C) 2014-2016 SlimRoms Project
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

package com.android.settings.slim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.slim.settings.R;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.action.ActionsArray;
import org.slim.action.ActionConstants;
import org.slim.action.ActionHelper;
import org.slim.provider.SlimSettings;
import org.slim.utils.AppHelper;
import org.slim.utils.DeviceUtils;
import org.slim.utils.HwKeyHelper;
import org.slim.utils.ShortcutPickerHelper;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileFilter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HardwareKeysSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "HardwareKeys";

    private static final String CATEGORY_KEYS = "button_keys";
    private static final String CATEGORY_BACK = "button_keys_back";
    private static final String CATEGORY_CAMERA = "button_keys_camera";
    private static final String CATEGORY_HOME = "button_keys_home";
    private static final String CATEGORY_MENU = "button_keys_menu";
    private static final String CATEGORY_ASSIST = "button_keys_assist";
    private static final String CATEGORY_APPSWITCH = "button_keys_appSwitch";

    private static final String KEYS_CATEGORY_BINDINGS = "keys_bindings";
    private static final String KEYS_ENABLE_CUSTOM = "enable_hardware_rebind";
    private static final String KEYS_BACK_PRESS = "keys_back_press";
    private static final String KEYS_BACK_LONG_PRESS = "keys_back_long_press";
    private static final String KEYS_BACK_DOUBLE_TAP = "keys_back_double_tap";
    private static final String KEYS_CAMERA_PRESS = "keys_camera_press";
    private static final String KEYS_CAMERA_LONG_PRESS = "keys_camera_long_press";
    private static final String KEYS_CAMERA_DOUBLE_TAP = "keys_camera_double_tap";
    private static final String KEYS_HOME_PRESS = "keys_home_press";
    private static final String KEYS_HOME_LONG_PRESS = "keys_home_long_press";
    private static final String KEYS_HOME_DOUBLE_TAP = "keys_home_double_tap";
    private static final String KEYS_MENU_PRESS = "keys_menu_press";
    private static final String KEYS_MENU_LONG_PRESS = "keys_menu_long_press";
    private static final String KEYS_MENU_DOUBLE_TAP = "keys_menu_double_tap";
    private static final String KEYS_ASSIST_PRESS = "keys_assist_press";
    private static final String KEYS_ASSIST_LONG_PRESS = "keys_assist_long_press";
    private static final String KEYS_ASSIST_DOUBLE_TAP = "keys_assist_double_tap";
    private static final String KEYS_APP_SWITCH_PRESS = "keys_app_switch_press";
    private static final String KEYS_APP_SWITCH_LONG_PRESS = "keys_app_switch_long_press";
    private static final String KEYS_APP_SWITCH_DOUBLE_TAP = "keys_app_switch_double_tap";

    private static final String KEY_ENABLE_HWKEYS = "enable_hw_keys";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";

    private static final int DLG_SHOW_WARNING_DIALOG = 0;
    private static final int DLG_SHOW_ACTION_DIALOG  = 1;
    private static final int DLG_RESET_TO_DEFAULT    = 2;

    private static final int MENU_RESET = Menu.FIRST;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    private static final int KEY_MASK_HOME       = 0x01;
    private static final int KEY_MASK_BACK       = 0x02;
    private static final int KEY_MASK_MENU       = 0x04;
    private static final int KEY_MASK_ASSIST     = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;
    private static final int KEY_MASK_CAMERA     = 0x20;

    private SwitchPreference mEnableHwKeys;
    private SwitchPreference mEnableCustomBindings;
    private Preference mBackPressAction;
    private Preference mBackLongPressAction;
    private Preference mBackDoubleTapAction;
    private Preference mCameraPressAction;
    private Preference mCameraLongPressAction;
    private Preference mCameraDoubleTapAction;
    private Preference mHomePressAction;
    private Preference mHomeLongPressAction;
    private Preference mHomeDoubleTapAction;
    private Preference mMenuPressAction;
    private Preference mMenuLongPressAction;
    private Preference mMenuDoubleTapAction;
    private Preference mAssistPressAction;
    private Preference mAssistLongPressAction;
    private Preference mAssistDoubleTapAction;
    private Preference mAppSwitchPressAction;
    private Preference mAppSwitchLongPressAction;
    private Preference mAppSwitchDoubleTapAction;

    private boolean mCheckPreferences;
    private Map<String, String> mKeySettings = new HashMap<String, String>();

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private ActionsArray mActionsArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        // Before we start filter out unsupported options on the
        // ListPreference values and entries
        mActionsArray = new ActionsArray(getActivity());

        // Attach final settings screen.
        reloadSettings();

        setHasOptionsMenu(true);
    }

    private PreferenceScreen reloadSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.hardwarekeys_settings);
        prefs = getPreferenceScreen();

        int deviceKeys = getResources().getInteger(
                org.slim.framework.internal.R.integer.config_deviceHardwareKeys);

        boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        boolean hasCameraKey = (deviceKeys & KEY_MASK_CAMERA) != 0;

        PreferenceCategory keysCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_KEYS);
        PreferenceCategory keysBackCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_BACK);
        PreferenceCategory keysCameraCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_CAMERA);
        PreferenceCategory keysHomeCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_HOME);
        PreferenceCategory keysMenuCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_MENU);
        PreferenceCategory keysAssistCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_ASSIST);
        PreferenceCategory keysAppSwitchCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_APPSWITCH);

        ButtonBacklightBrightness backlight = (ButtonBacklightBrightness)
                prefs.findPreference(KEY_BUTTON_BACKLIGHT);

        mEnableHwKeys = (SwitchPreference) prefs.findPreference(
                KEY_ENABLE_HWKEYS);
        mEnableHwKeys.setOnPreferenceChangeListener(this);

        mEnableCustomBindings = (SwitchPreference) prefs.findPreference(
                KEYS_ENABLE_CUSTOM);
        mBackPressAction = (Preference) prefs.findPreference(
                KEYS_BACK_PRESS);
        mBackLongPressAction = (Preference) prefs.findPreference(
                KEYS_BACK_LONG_PRESS);
        mBackDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_BACK_DOUBLE_TAP);
        mCameraPressAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_PRESS);
        mCameraLongPressAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_LONG_PRESS);
        mCameraDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_DOUBLE_TAP);
        mHomePressAction = (Preference) prefs.findPreference(
                KEYS_HOME_PRESS);
        mHomeLongPressAction = (Preference) prefs.findPreference(
                KEYS_HOME_LONG_PRESS);
        mHomeDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_HOME_DOUBLE_TAP);
        mMenuPressAction = (Preference) prefs.findPreference(
                KEYS_MENU_PRESS);
        mMenuLongPressAction = (Preference) prefs.findPreference(
                KEYS_MENU_LONG_PRESS);
        mMenuDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_MENU_DOUBLE_TAP);
        mAssistPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_PRESS);
        mAssistLongPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_LONG_PRESS);
        mAssistDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_DOUBLE_TAP);
        mAppSwitchPressAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_PRESS);
        mAppSwitchLongPressAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_LONG_PRESS);
        mAppSwitchDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_DOUBLE_TAP);

        if (!backlight.isButtonSupported() && !backlight.isKeyboardSupported()) {
                prefs.removePreference(backlight);
        }

        if (hasBackKey) {
            // Back key
            setupOrUpdatePreference(mBackPressAction,
                    HwKeyHelper.getPressOnBackBehavior(getActivity(), false),
                    SlimSettings.System.KEY_BACK_ACTION);

            // Back key longpress
            setupOrUpdatePreference(mBackLongPressAction,
                    HwKeyHelper.getLongPressOnBackBehavior(getActivity(), false),
                    SlimSettings.System.KEY_BACK_LONG_PRESS_ACTION);

            // Back key double tap
            setupOrUpdatePreference(mBackDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnBackBehavior(getActivity(), false),
                    SlimSettings.System.KEY_BACK_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysBackCategory);
        }

        if (hasCameraKey) {
            // Camera key
            setupOrUpdatePreference(mCameraPressAction,
                    HwKeyHelper.getPressOnCameraBehavior(getActivity(), false),
                    SlimSettings.System.KEY_CAMERA_ACTION);

            // Camera key longpress
            setupOrUpdatePreference(mCameraLongPressAction,
                    HwKeyHelper.getLongPressOnCameraBehavior(getActivity(), false),
                    SlimSettings.System.KEY_CAMERA_LONG_PRESS_ACTION);

            // Camera key double tap
            setupOrUpdatePreference(mCameraDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnCameraBehavior(getActivity(), false),
                    SlimSettings.System.KEY_CAMERA_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysCameraCategory);
        }

        if (hasHomeKey) {
            // Home key
            setupOrUpdatePreference(mHomePressAction,
                    HwKeyHelper.getPressOnHomeBehavior(getActivity(), false),
                    SlimSettings.System.KEY_HOME_ACTION);

            // Home key long press
            setupOrUpdatePreference(mHomeLongPressAction,
                    HwKeyHelper.getLongPressOnHomeBehavior(getActivity(), false),
                    SlimSettings.System.KEY_HOME_LONG_PRESS_ACTION);

            // Home key double tap
            setupOrUpdatePreference(mHomeDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnHomeBehavior(getActivity(), false),
                    SlimSettings.System.KEY_HOME_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysHomeCategory);
        }

        if (hasMenuKey) {
            // Menu key
            setupOrUpdatePreference(mMenuPressAction,
                    HwKeyHelper.getPressOnMenuBehavior(getActivity(), false),
                    SlimSettings.System.KEY_MENU_ACTION);

            // Menu key longpress
            setupOrUpdatePreference(mMenuLongPressAction,
                    HwKeyHelper.getLongPressOnMenuBehavior(getActivity(), false, hasAssistKey),
                    SlimSettings.System.KEY_MENU_LONG_PRESS_ACTION);

            // Menu key double tap
            setupOrUpdatePreference(mMenuDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnMenuBehavior(getActivity(), false),
                    SlimSettings.System.KEY_MENU_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysMenuCategory);
        }

        if (hasAssistKey) {
            // Assistant key
            setupOrUpdatePreference(mAssistPressAction,
                    HwKeyHelper.getPressOnAssistBehavior(getActivity(), false),
                    SlimSettings.System.KEY_ASSIST_ACTION);

            // Assistant key longpress
            setupOrUpdatePreference(mAssistLongPressAction,
                    HwKeyHelper.getLongPressOnAssistBehavior(getActivity(), false),
                    SlimSettings.System.KEY_ASSIST_LONG_PRESS_ACTION);

            // Assistant key double tap
            setupOrUpdatePreference(mAssistDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnAssistBehavior(getActivity(), false),
                    SlimSettings.System.KEY_ASSIST_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysAssistCategory);
        }

        if (hasAppSwitchKey) {
            // App switch key
            setupOrUpdatePreference(mAppSwitchPressAction,
                    HwKeyHelper.getPressOnAppSwitchBehavior(getActivity(), false),
                    SlimSettings.System.KEY_APP_SWITCH_ACTION);

            // App switch key longpress
            setupOrUpdatePreference(mAppSwitchLongPressAction,
                    HwKeyHelper.getLongPressOnAppSwitchBehavior(getActivity(), false),
                    SlimSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);

            // App switch key double tap
            setupOrUpdatePreference(mAppSwitchDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnAppSwitchBehavior(getActivity(), false),
                    SlimSettings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysAppSwitchCategory);
        }

        boolean enableHardwareRebind = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.HARDWARE_KEY_REBINDING, 0) == 1;
        mEnableCustomBindings = (SwitchPreference) findPreference(KEYS_ENABLE_CUSTOM);
        mEnableCustomBindings.setChecked(enableHardwareRebind);
        mEnableCustomBindings.setOnPreferenceChangeListener(this);

        // Handle warning dialog.
        SharedPreferences preferences =
                getActivity().getSharedPreferences("hw_key_settings", Activity.MODE_PRIVATE);
        if (hasHomeKey && !hasHomeKey() && !preferences.getBoolean("no_home_action", false)) {
            preferences.edit()
                    .putBoolean("no_home_action", true).commit();
            showDialogInner(DLG_SHOW_WARNING_DIALOG, null, 0);
        } else if (hasHomeKey()) {
            preferences.edit()
                    .putBoolean("no_home_action", false).commit();
        }

        mCheckPreferences = true;
        return prefs;
    }

    private void setupOrUpdatePreference(
            Preference preference, String action, String settingsKey) {
        if (preference == null || action == null) {
            return;
        }

        if (action.startsWith("**")) {
            preference.setSummary(ActionHelper.getActionDescription(getActivity(), action));
        } else {
            preference.setSummary(AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), action));
        }

        preference.setOnPreferenceClickListener(this);
        mKeySettings.put(settingsKey, action);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String settingsKey = null;
        int dialogTitle = 0;
        if (preference == mBackPressAction) {
            settingsKey = SlimSettings.System.KEY_BACK_ACTION;
            dialogTitle = R.string.keys_back_press_title;
        } else if (preference == mBackLongPressAction) {
            settingsKey = SlimSettings.System.KEY_BACK_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_back_long_press_title;
        } else if (preference == mBackDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_BACK_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_back_double_tap_title;
        } else if (preference == mCameraPressAction) {
            settingsKey = SlimSettings.System.KEY_CAMERA_ACTION;
            dialogTitle = R.string.keys_camera_press_title;
        } else if (preference == mCameraLongPressAction) {
            settingsKey = SlimSettings.System.KEY_CAMERA_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_camera_long_press_title;
        } else if (preference == mCameraDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_CAMERA_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_camera_double_tap_title;
        } else if (preference == mHomePressAction) {
            settingsKey = SlimSettings.System.KEY_HOME_ACTION;
            dialogTitle = R.string.keys_home_press_title;
        } else if (preference == mHomeLongPressAction) {
            settingsKey = SlimSettings.System.KEY_HOME_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_home_long_press_title;
        } else if (preference == mHomeDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_HOME_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_home_double_tap_title;
        } else if (preference == mMenuPressAction) {
            settingsKey = SlimSettings.System.KEY_MENU_ACTION;
            dialogTitle = R.string.keys_menu_press_title;
        } else if (preference == mMenuLongPressAction) {
            settingsKey = SlimSettings.System.KEY_MENU_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_menu_long_press_title;
        } else if (preference == mMenuDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_MENU_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_menu_double_tap_title;
        } else if (preference == mAssistPressAction) {
            settingsKey = SlimSettings.System.KEY_ASSIST_ACTION;
            dialogTitle = R.string.keys_assist_press_title;
        } else if (preference == mAssistLongPressAction) {
            settingsKey = SlimSettings.System.KEY_ASSIST_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_assist_long_press_title;
        } else if (preference == mAssistDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_ASSIST_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_assist_double_tap_title;
        } else if (preference == mAppSwitchPressAction) {
            settingsKey = SlimSettings.System.KEY_APP_SWITCH_ACTION;
            dialogTitle = R.string.keys_app_switch_press_title;
        } else if (preference == mAppSwitchLongPressAction) {
            settingsKey = SlimSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_app_switch_long_press_title;
        } else if (preference == mAppSwitchDoubleTapAction) {
            settingsKey = SlimSettings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_app_switch_double_tap_title;
        }

        if (settingsKey != null) {
            showDialogInner(DLG_SHOW_ACTION_DIALOG, settingsKey, dialogTitle);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }

        if (preference == mEnableCustomBindings) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.HARDWARE_KEY_REBINDING, value ? 1 : 0);
            return true;
        } else if (preference == mEnableHwKeys) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DISABLE_HW_KEYS, value ? 0 : 1);
            if (!value) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.BUTTON_BRIGHTNESS, 0);
            } else {
                int defBright = getResources().getInteger(
                        com.android.internal.R.integer.config_buttonBrightnessSettingDefault);
                int oldBright = PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getInt(ButtonBacklightBrightness.KEY_BUTTON_BACKLIGHT, defBright);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.BUTTON_BRIGHTNESS, oldBright);
            }
            return true;
        } else if (preference == mEnableHwKeys) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DISABLE_HW_KEYS, value ? 0 : 1);
            return true;
        }
        return false;
    }

    private boolean hasHomeKey() {
        Iterator<String> nextAction = mKeySettings.values().iterator();
        while (nextAction.hasNext()){
            String action = nextAction.next();
            if (action != null && action.equals(ActionConstants.ACTION_HOME)) {
                return true;
            }
        }
        return false;
    }

    private void resetToDefault() {
        for (String settingsKey : mKeySettings.keySet()) {
            if (settingsKey != null) {
                SlimSettings.System.putString(getActivity().getContentResolver(),
                settingsKey, null);
            }
        }
        SlimSettings.System.putInt(getContentResolver(),
                SlimSettings.System.DISABLE_HW_KEYS, 0);
        SlimSettings.System.putInt(getContentResolver(),
                SlimSettings.System.HARDWARE_KEY_REBINDING, 1);
        reloadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap b, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        SlimSettings.System.putString(getContentResolver(), mPendingSettingsKey, action);
        reloadSettings();
        mPendingSettingsKey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingSettingsKey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, org.slim.framework.internal.R.string.reset)
                // Use the reset icon
                .setIcon(org.slim.framework.internal.R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String settingsKey, int dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, int dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putInt("dialogTitle", dialogTitle);
            frag.setArguments(args);
            return frag;
        }

        HardwareKeysSettings getOwner() {
            return (HardwareKeysSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String settingsKey = getArguments().getString("settingsKey");
            int dialogTitle = getArguments().getInt("dialogTitle");
            switch (id) {
                case DLG_SHOW_WARNING_DIALOG:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(org.slim.framework.internal.R.string.attention)
                    .setMessage(org.slim.framework.internal.R.string.no_home_key)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
                case DLG_SHOW_ACTION_DIALOG:
                    if (getOwner().mActionsArray == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(dialogTitle)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setItems(getOwner().mActionsArray.getEntries(),
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (getOwner().mActionsArray.getValue(item)
                                    .equals(ActionConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingSettingsKey = settingsKey;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                SlimSettings.System.putString(getActivity().getContentResolver(),
                                        settingsKey,
                                        getOwner().mActionsArray.getValue(item));
                                getOwner().reloadSettings();
                            }
                        }
                    })
                    .create();
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(org.slim.framework.internal.R.string.shortcut_action_reset)
                    .setMessage(org.slim.framework.internal.R.string.reset_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.HARDWAREKEYS_SETTINGS;
    }
}
