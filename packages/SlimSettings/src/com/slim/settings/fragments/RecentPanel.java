/*
 * Copyright (C) 2015-2016 SlimRoms Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import com.slim.settings.DialogCreatable;
//import com.android.settings.R;
import com.slim.settings.SettingsPreferenceFragment;

import org.slim.framework.R;
import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.utils.DeviceUtils;
import org.slim.preference.SlimSeekBarPreference;
import org.slim.preference.colorpicker.ColorPickerPreference;
import org.slim.provider.SlimSettings;

public class RecentPanel extends SettingsPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentPanelSettings";

    // Preferences
    private static final String RECENTS_MAX_APPS = "max_apps";
    private static final String RECENT_PANEL_LEFTY_MODE =
            "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE =
            "recent_panel_scale";
    private static final String RECENT_PANEL_BG_COLOR =
            "recent_panel_bg_color";
    private static final String RECENT_CARD_BG_COLOR =
            "recent_card_bg_color";
    private static final String RECENT_CARD_TEXT_COLOR =
            "recent_card_text_color";

    private SlimSeekBarPreference mMaxApps;
    private SwitchPreference mRecentPanelLeftyMode;
    private SlimSeekBarPreference mRecentPanelScale;
    private ColorPickerPreference mRecentPanelBgColor;
    private ColorPickerPreference mRecentCardBgColor;
    private ColorPickerPreference mRecentCardTextColor;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(com.slim.settings.R.xml.recent_panel_settings);
        initializeAllPreferences();
    }

    @Override
    public int getMetricsCategory() {
        return SlimMetricsLogger.RECENT_PANEL_SETTINGS;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) newValue);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_PANEL_SCALE_FACTOR, value);
            return true;
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_PANEL_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentCardBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_CARD_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentCardTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.default_string);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_CARD_TEXT_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentPanelLeftyMode) {
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        } else if (preference == mMaxApps) {
            int value = Integer.parseInt((String) newValue);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENTS_MAX_APPS, value);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRecentPanelPreferences();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.reset_message);
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        SlimSettings.System.putInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setSummary(R.string.default_string);
        SlimSettings.System.putInt(getContentResolver(),
                SlimSettings.System.RECENT_CARD_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentCardBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentCardBgColor.setSummary(R.string.default_string);
        SlimSettings.System.putInt(getContentResolver(),
                SlimSettings.System.RECENT_CARD_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentCardTextColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentCardTextColor.setSummary(R.string.default_string);
    }

    private void updateRecentPanelPreferences() {
        final boolean recentLeftyMode = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);

        final int recentScale = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale.setInitValue(recentScale - 60);
    }

    private void initializeAllPreferences() {
        mMaxApps = (SlimSeekBarPreference) findPreference(RECENTS_MAX_APPS);
        mMaxApps.setOnPreferenceChangeListener(this);
        mMaxApps.minimumValue(5);
        mMaxApps.setInitValue(SlimSettings.System.getIntForUser(getContentResolver(),
                SlimSettings.System.RECENTS_MAX_APPS, ActivityManager.getMaxRecentTasksStatic(),
                UserHandle.USER_CURRENT) - 5);
        mMaxApps.disablePercentageValue(true);

        // Recent panel background color
        mRecentPanelBgColor =
                (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
        mRecentPanelBgColor.setOnPreferenceChangeListener(this);
        final int intColor = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_BG_COLOR, 0x00ffffff);
        String hexColor = String.format("#%08x", (0x00ffffff & intColor));
        if (hexColor.equals("#00ffffff")) {
            mRecentPanelBgColor.setSummary(R.string.default_string);
        } else {
            mRecentPanelBgColor.setSummary(hexColor);
        }
        mRecentPanelBgColor.setNewPreviewColor(intColor);

        // Recent card background color
        mRecentCardBgColor =
                (ColorPickerPreference) findPreference(RECENT_CARD_BG_COLOR);
        mRecentCardBgColor.setOnPreferenceChangeListener(this);
        final int intColorCard = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_CARD_BG_COLOR, 0x00ffffff);
        String hexColorCard = String.format("#%08x", (0x00ffffff & intColorCard));
        if (hexColorCard.equals("#00ffffff")) {
            mRecentCardBgColor.setSummary(R.string.default_string);
        } else {
            mRecentCardBgColor.setSummary(hexColorCard);
        }
        mRecentCardBgColor.setNewPreviewColor(intColorCard);

        // Recent card text color
        mRecentCardTextColor =
                (ColorPickerPreference) findPreference(RECENT_CARD_TEXT_COLOR);
        mRecentCardTextColor.setOnPreferenceChangeListener(this);
        final int intColorText = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_CARD_TEXT_COLOR, 0x00ffffff);
        String hexColorText = String.format("#%08x", (0x00ffffff & intColorText));
        if (hexColorText.equals("#00ffffff")) {
            mRecentCardTextColor.setSummary(R.string.default_string);
        } else {
            mRecentCardTextColor.setSummary(hexColorText);
        }
        mRecentCardTextColor.setNewPreviewColor(intColorText);

        // Enable options menu for color reset
        setHasOptionsMenu(true);

        mRecentPanelLeftyMode =
                (SwitchPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        mRecentPanelScale =
                (SlimSeekBarPreference) findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setInterval(5);
        mRecentPanelScale.setDefault(100);
        mRecentPanelScale.minimumValue(60);
        mRecentPanelScale.setOnPreferenceChangeListener(this);
        mRecentPanelScale.setInitValue(SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_SCALE_FACTOR, 100) - 60);
    }
}
