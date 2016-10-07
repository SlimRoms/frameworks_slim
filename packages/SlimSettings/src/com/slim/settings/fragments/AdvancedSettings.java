/*
 * Copyright (C) 2013 SlimRoms Project
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.provider.SlimSettings;

import com.slim.settings.R;
import com.slim.settings.SettingsPreferenceFragment;

import java.util.List;

public class AdvancedSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String PREF_DEVICESETTINGS_APP = "devicesettings_app";

    private PreferenceScreen mDeviceSettingsApp;

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.ADVANCED_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.slim_advanced_settings);

        mDeviceSettingsApp = (PreferenceScreen) findPreference(PREF_DEVICESETTINGS_APP);

        if (!deviceSettingsAppExists()) {
            getPreferenceScreen().removePreference(mDeviceSettingsApp);
        }

    }

    private boolean deviceSettingsAppExists() {
        Intent intent = mDeviceSettingsApp.getIntent();
        if (intent != null) {
            PackageManager pm = getActivity().getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            int listSize = list.size();
            return (listSize > 0) ? true : false;

        }
        return false;

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
