/*
 * Copyright (C) 2012-2016 SlimRoms Project
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

import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.R;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.provider.SlimSettings;
import org.slim.utils.DeviceUtils;

public class NavbarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBar";
    private static final String PREF_MENU_LOCATION = "pref_navbar_menu_location";
    private static final String PREF_NAVBAR_MENU_DISPLAY = "pref_navbar_menu_display";
    private static final String ENABLE_NAVIGATION_BAR = "enable_nav_bar";
    private static final String PREF_BUTTON = "navbar_button_settings";
    private static final String PREF_BUTTON_STYLE = "nav_bar_button_style";
    private static final String PREF_STYLE_DIMEN = "navbar_style_dimen_settings";

    private int mNavBarMenuDisplayValue;

    ListPreference mMenuDisplayLocation;
    ListPreference mNavBarMenuDisplay;
    PreferenceScreen mButtonPreference;
    PreferenceScreen mButtonStylePreference;
    PreferenceScreen mStyleDimenPreference;

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.NAV_BAR_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navbar_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mNavBarMenuDisplayValue = SlimSettings.System.getIntForUser(getActivity()
                .getContentResolver(), SlimSettings.System.MENU_VISIBILITY,
                2, UserHandle.USER_CURRENT);

        mMenuDisplayLocation = (ListPreference) findPreference(PREF_MENU_LOCATION);
        mMenuDisplayLocation.setValue(SlimSettings.System.getInt(getActivity()
                .getContentResolver(), SlimSettings.System.MENU_LOCATION,
                0) + "");
        mMenuDisplayLocation.setOnPreferenceChangeListener(this);
        mMenuDisplayLocation.setEnabled(mNavBarMenuDisplayValue != 1);

        mNavBarMenuDisplay = (ListPreference) findPreference(PREF_NAVBAR_MENU_DISPLAY);
        mNavBarMenuDisplay.setValue(mNavBarMenuDisplayValue + "");
        mNavBarMenuDisplay.setOnPreferenceChangeListener(this);

        mButtonPreference = (PreferenceScreen) findPreference(PREF_BUTTON);
        mButtonStylePreference = (PreferenceScreen) findPreference(PREF_BUTTON_STYLE);
        mStyleDimenPreference = (PreferenceScreen) findPreference(PREF_STYLE_DIMEN);

        final int showByDefault = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar) ? 1 : 0;
        // disable switch until we have other navigation options
        if (showByDefault == 1) {
            prefs.removePreference(findPreference(ENABLE_NAVIGATION_BAR));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mMenuDisplayLocation) {
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                    SlimSettings.System.MENU_LOCATION, Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mNavBarMenuDisplay) {
            mNavBarMenuDisplayValue = Integer.parseInt((String) newValue);
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                    SlimSettings.System.MENU_VISIBILITY, mNavBarMenuDisplayValue);
            mMenuDisplayLocation.setEnabled(mNavBarMenuDisplayValue != 1);
            return true;
        }
        return false;
    }
}
