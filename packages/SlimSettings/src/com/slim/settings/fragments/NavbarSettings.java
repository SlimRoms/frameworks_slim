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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.R;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.preference.SlimSeekBarPreference;
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
    private static final String DIM_NAV_BUTTONS_TIMEOUT = "dim_nav_buttons_timeout";
    private static final String DIM_NAV_BUTTONS_ALPHA = "dim_nav_buttons_alpha";
    private static final String DIM_NAV_BUTTONS_ANIMATE_DURATION = "dim_nav_buttons_animate_duration";

    private int mNavBarMenuDisplayValue;

    ListPreference mMenuDisplayLocation;
    ListPreference mNavBarMenuDisplay;
    PreferenceScreen mButtonPreference;
    PreferenceScreen mButtonStylePreference;
    PreferenceScreen mStyleDimenPreference;
    SlimSeekBarPreference mDimNavButtonsTimeout;
    SlimSeekBarPreference mDimNavButtonsAlpha;
    SlimSeekBarPreference mDimNavButtonsAnimateDuration;

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

        mDimNavButtonsTimeout = (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_TIMEOUT);
        mDimNavButtonsTimeout.setDefault(3000);
        mDimNavButtonsTimeout.isMilliseconds(true);
        mDimNavButtonsTimeout.setInterval(1);
        mDimNavButtonsTimeout.minimumValue(100);
        mDimNavButtonsTimeout.multiplyValue(100);
        mDimNavButtonsTimeout.setOnPreferenceChangeListener(this);

        mDimNavButtonsAlpha = (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_ALPHA);
        mDimNavButtonsAlpha.setDefault(50);
        mDimNavButtonsAlpha.setInterval(1);
        mDimNavButtonsAlpha.setOnPreferenceChangeListener(this);

        mDimNavButtonsAnimateDuration =
                (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_ANIMATE_DURATION);
        mDimNavButtonsAnimateDuration.setDefault(2000);
        mDimNavButtonsAnimateDuration.isMilliseconds(true);
        mDimNavButtonsAnimateDuration.setInterval(1);
        mDimNavButtonsAnimateDuration.minimumValue(100);
        mDimNavButtonsAnimateDuration.multiplyValue(100);
        mDimNavButtonsAnimateDuration.setOnPreferenceChangeListener(this);

        if (mDimNavButtonsTimeout != null) {
            final int dimTimeout = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DIM_NAV_BUTTONS_TIMEOUT, 3000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDimNavButtonsTimeout.setInitValue((dimTimeout / 100) - 1);
        }

        if (mDimNavButtonsAlpha != null) {
            int alphaScale = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DIM_NAV_BUTTONS_ALPHA, 50);
            mDimNavButtonsAlpha.setInitValue(alphaScale);
        }

        if (mDimNavButtonsAnimateDuration != null) {
            final int animateDuration = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION, 2000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDimNavButtonsAnimateDuration.setInitValue((animateDuration / 100) - 1);
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
        } else if (preference == mDimNavButtonsTimeout) {
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                SlimSettings.System.DIM_NAV_BUTTONS_TIMEOUT, Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mDimNavButtonsAlpha) {
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                SlimSettings.System.DIM_NAV_BUTTONS_ALPHA, Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mDimNavButtonsAnimateDuration) {
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION,
                Integer.parseInt((String) newValue));
            return true;
        }
        return false;
    }
}
