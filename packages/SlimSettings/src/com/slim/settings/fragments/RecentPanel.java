/*
 * Copyright (C) 2015-2017 SlimRoms Project
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

import org.slim.framework.internal.logging.SlimMetricsLogger;
import slim.preference.colorpicker.ColorPickerPreference;
import slim.provider.SlimSettings;
import slim.R;
import slim.utils.DeviceUtils;

public class RecentPanel extends SlimPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "RecentPanelSettings";

    // Preferences
    private static final String RECENT_PANEL_LEFTY_MODE =
            "recent_panel_lefty_mode";

    private SwitchPreference mRecentPanelLeftyMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeAllPreferences();
    }

    @Override
    public int getMetricsCategory() {
        return SlimMetricsLogger.RECENT_PANEL_SETTINGS;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecentPanelLeftyMode) {
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRecentPanelPreferences();
    }

    private void updateRecentPanelPreferences() {
        final boolean recentLeftyMode = SlimSettings.System.getInt(getContentResolver(),
                SlimSettings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);
    }

    private void initializeAllPreferences() {
        mRecentPanelLeftyMode =
                (SwitchPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);
    }
}
