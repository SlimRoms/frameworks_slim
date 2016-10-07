/*
 * Copyright (C) 2013-2016 SlimRoms Project
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

import android.content.Context;
import android.os.Bundle;
import android.os.ServiceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.IWindowManager;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.provider.SlimSettings;

import com.slim.settings.R;
import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.Utils;

public class NavigationSettings extends SettingsPreferenceFragment {

    private static final String KEY_HARDWARE_KEYS = "hardwarekeys_settings";
    private static final String KEY_SCREEN_OFF_GESTURE_SETTINGS = "screen_off_gesture_settings";

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.NAVIGATION_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.slim_navigation_settings);

        // Hide Hardware Keys menu if device doesn't have any
        PreferenceScreen hardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        int deviceKeys = getResources().getInteger(
                org.slim.framework.internal.R.integer.config_deviceHardwareKeys);
        if (deviceKeys == 0 && hardwareKeys != null) {
            getPreferenceScreen().removePreference(hardwareKeys);
        }

        Utils.updatePreferenceToSpecificActivityOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_OFF_GESTURE_SETTINGS,0);
    }

}
