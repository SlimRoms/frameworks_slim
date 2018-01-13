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

package com.slim.settings.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.preference.ButtonBacklightBrightness;
import com.slim.settings.R;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import slim.action.ActionsArray;
import slim.action.ActionConstants;
import slim.action.ActionHelper;
import slim.provider.SlimSettings;
import slim.utils.AppHelper;
import slim.utils.HwKeyHelper;
import slim.utils.ShortcutPickerHelper;

import java.util.HashMap;
import java.util.Iterator;

public class HardwareKeysSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "HardwareKeys";

    private static final String CATEGORY_KEYS = "button_keys";

    private static final String KEY_ENABLE_HWKEYS = "enable_hw_keys";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";

    private static final int DLG_SHOW_WARNING_DIALOG = 0;
    private static final int DLG_SHOW_ACTION_DIALOG  = 1;
    private static final int DLG_RESET_TO_DEFAULT    = 2;

    private static final int MENU_RESET = Menu.FIRST;

    private SwitchPreference mEnableHwKeys;

    private boolean mCheckPreferences;
    private HashMap<String, String> mKeySettings = new HashMap<String, String>();

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

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
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

        boolean hasBackKey = (deviceKeys & HwKeyHelper.KEY_MASK_BACK) != 0;
        boolean hasHomeKey = (deviceKeys & HwKeyHelper.KEY_MASK_HOME) != 0;
        boolean hasMenuKey = (deviceKeys & HwKeyHelper.KEY_MASK_MENU) != 0;
        boolean hasAssistKey = (deviceKeys & HwKeyHelper.KEY_MASK_ASSIST) != 0;
        boolean hasAppSwitchKey = (deviceKeys & HwKeyHelper.KEY_MASK_APP_SWITCH) != 0;
        boolean hasCameraKey = (deviceKeys & HwKeyHelper.KEY_MASK_CAMERA) != 0;

        ButtonBacklightBrightness backlight = (ButtonBacklightBrightness)
                prefs.findPreference(KEY_BUTTON_BACKLIGHT);

        mEnableHwKeys = (SwitchPreference) prefs.findPreference(
                KEY_ENABLE_HWKEYS);
        mEnableHwKeys.setOnPreferenceChangeListener(this);

        if (!backlight.isButtonSupported() && !backlight.isKeyboardSupported()) {
                prefs.removePreference(backlight);
        }

        if (hasBackKey) {
           createPreferenceCategory(R.string.keys_back_title, KeyEvent.KEYCODE_BACK);
        }

        if (hasCameraKey) {
            createPreferenceCategory(R.string.keys_camera_title, KeyEvent.KEYCODE_CAMERA);
        }

        if (hasHomeKey) {
            createPreferenceCategory(R.string.keys_home_title, KeyEvent.KEYCODE_HOME);
        }

        if (hasMenuKey) {
            createPreferenceCategory(R.string.keys_menu_title, KeyEvent.KEYCODE_MENU);
        }

        if (hasAssistKey) {
            createPreferenceCategory(R.string.keys_assist_title, KeyEvent.KEYCODE_ASSIST);
        }

        if (hasAppSwitchKey) {
            createPreferenceCategory(R.string.keys_app_switch_title, KeyEvent.KEYCODE_APP_SWITCH);
        }

        // Handle warning dialog.
        SharedPreferences preferences =
                getActivity().getSharedPreferences("hw_key_settings", Activity.MODE_PRIVATE);
        if (hasHomeKey && !hasHomeKey() && !preferences.getBoolean("no_home_action", false)) {
            preferences.edit()
                    .putBoolean("no_home_action", true).commit();
            showDialogInner(DLG_SHOW_WARNING_DIALOG, null, "");
        } else if (hasHomeKey()) {
            preferences.edit()
                    .putBoolean("no_home_action", false).commit();
        }

        mCheckPreferences = true;
        return prefs;
    }

     private String getStringFromSettings(String key, String def) {
        String val = SlimSettings.System.getStringForUser(
                getActivity().getContentResolver(), key, UserHandle.USER_CURRENT);
        return (val == null) ? def : val;
    }

    private void createPreferenceCategory(int titleResId, int keyCode) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(getActivity().getString(titleResId));
        getPreferenceScreen().addPreference(category);

        String key = KeyEvent.keyCodeToString(keyCode);
        key = key.replace("KEYCODE_", "key_").toLowerCase();

        // Normal Press
        Preference press = new Preference(getActivity());
        press.setTitle(getActivity().getString(R.string.keys_action_normal));
        press.setKey(key + "_action");
        String action = getStringFromSettings(press.getKey(),
                HwKeyHelper.getDefaultTapActionForKeyCode(getActivity(), keyCode));
        setupOrUpdatePreference(press, action);
        category.addPreference(press);

        // Long Press
        Preference longpress = new Preference(getActivity());
        longpress.setTitle(getActivity().getString(R.string.keys_action_long));
        longpress.setKey(key + "_long_press_action");
        action = getStringFromSettings(longpress.getKey(),
                HwKeyHelper.getDefaultLongPressActionForKeyCode(getActivity(), keyCode));
        setupOrUpdatePreference(longpress, action);
        category.addPreference(longpress);

        // Double Tap
        Preference doubletap = new Preference(getActivity());
        doubletap.setTitle(getActivity().getString(R.string.keys_action_double));
        doubletap.setKey(key + "_double_tap_action");
        action = getStringFromSettings(doubletap.getKey(),
                HwKeyHelper.getDefaultDoubleTapActionForKeyCode(getActivity(), keyCode));
        setupOrUpdatePreference(doubletap, action);
        category.addPreference(doubletap);
    }

    private void setupOrUpdatePreference(Preference preference, String action) {
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
        mKeySettings.put(preference.getKey(), action);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String settingsKey = preference.getKey();
        String dialogTitle = (String) preference.getTitle();

        if (!TextUtils.isEmpty(settingsKey) && !TextUtils.isEmpty(dialogTitle)) {
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

        if (preference == mEnableHwKeys) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DISABLE_HW_KEYS, value ? 0 : 1);
            if (!value) {
                SlimSettings.System.putInt(getContentResolver(),
                        SlimSettings.System.BUTTON_BRIGHTNESS, 0);
            } else {
                int defBright = getResources().getInteger(
                        org.slim.framework.internal.R
                            .integer.config_buttonBrightnessSettingDefault);
                int oldBright = PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .getInt(ButtonBacklightBrightness.KEY_BUTTON_BACKLIGHT, defBright);
                SlimSettings.System.putInt(getContentResolver(),
                        SlimSettings.System.BUTTON_BRIGHTNESS, oldBright);
            }
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
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, "");
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

    private void showDialogInner(int id, String settingsKey, String dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, String dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putString("dialogTitle", dialogTitle);
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
            final String dialogTitle = getArguments().getString("dialogTitle");
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
