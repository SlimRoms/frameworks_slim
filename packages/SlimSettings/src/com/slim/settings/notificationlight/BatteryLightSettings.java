/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.slim.settings.notificationlight;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.slim.settings.R;
import com.slim.settings.SettingsPreferenceFragment;

import slim.provider.SlimSettings;

public class BatteryLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "BatteryLightSettings";

    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String MEDIUM_FAST_COLOR_PREF = "medium_fast_color";
    private static final String FULL_COLOR_PREF = "full_color";
    private static final String LIGHT_ENABLED_PREF = "battery_light_enabled";
    private static final String PULSE_ENABLED_PREF = "battery_light_pulse";

    private PreferenceGroup mColorPrefs;
    private ApplicationLightPreference mLowColorPref;
    private ApplicationLightPreference mMediumColorPref;
    private ApplicationLightPreference mMediumFastColorPref;
    private ApplicationLightPreference mFullColorPref;
    private SwitchPreference mLightEnabledPref;
    private SwitchPreference mPulseEnabledPref;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.battery_light_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mLightEnabledPref = (SwitchPreference) prefSet.findPreference(LIGHT_ENABLED_PREF);
        mPulseEnabledPref = (SwitchPreference) prefSet.findPreference(PULSE_ENABLED_PREF);

        // Does the Device support changing battery LED colors?
        if (getResources().getBoolean(
                org.slim.framework.internal.R.bool.config_multiColorBatteryLed)) {
            setHasOptionsMenu(true);

            // Low, Medium and full color preferences
            mLowColorPref = (ApplicationLightPreference) prefSet.findPreference(LOW_COLOR_PREF);
            mLowColorPref.setMultiColorLed(true);
            mLowColorPref.setOnPreferenceChangeListener(this);

            mMediumColorPref = (ApplicationLightPreference)
                    prefSet.findPreference(MEDIUM_COLOR_PREF);
            mMediumColorPref.setMultiColorLed(true);
            mMediumColorPref.setOnPreferenceChangeListener(this);

            mMediumFastColorPref = (ApplicationLightPreference)
                    prefSet.findPreference(MEDIUM_FAST_COLOR_PREF);
            mMediumFastColorPref.setMultiColorLed(true);
            mMediumFastColorPref.setOnPreferenceChangeListener(this);

            mFullColorPref = (ApplicationLightPreference) prefSet.findPreference(FULL_COLOR_PREF);
            mFullColorPref.setMultiColorLed(true);
            mFullColorPref.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(prefSet.findPreference("colors_list"));
            resetColors();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();

        if (mLowColorPref != null) {
            int lowColor = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_LOW_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryLowARGB));
            mLowColorPref.setAllValues(lowColor, 0, 0, false);
        }

        if (mMediumColorPref != null) {
            int mediumColor = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
            mMediumColorPref.setAllValues(mediumColor, 0, 0, false);
        }

        if (mMediumFastColorPref != null) {
            int mediumFastColor = SlimSettings.System.getInt(resolver, 
                    SlimSettings.System.BATTERY_LIGHT_MEDIUM_FAST_COLOR, res.getInteger(
                    org.slim.framework.internal.R.integer.config_notificationsBatteryMediumFastARGB));
            mMediumFastColorPref.setAllValues(mediumFastColor, 0, 0, false);
        }

        if (mFullColorPref != null) {
            int fullColor = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_FULL_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryFullARGB));
            mFullColorPref.setAllValues(fullColor, 0, 0, false);
        }
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param key of the specific setting to update
     * @param color
     */
    protected void updateValues(String key, Integer color) {
        ContentResolver resolver = getContentResolver();

        if (key.equals(LOW_COLOR_PREF)) {
            SlimSettings.System.putInt(resolver,
            SlimSettings.System.BATTERY_LIGHT_LOW_COLOR, color);
        } else if (key.equals(MEDIUM_COLOR_PREF)) {
            SlimSettings.System.putInt(resolver,
            SlimSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, color);
        } else if (key.equals(MEDIUM_FAST_COLOR_PREF)) {
            SlimSettings.System.putInt(resolver,
            SlimSettings.System.BATTERY_LIGHT_MEDIUM_FAST_COLOR, color);
        } else if (key.equals(FULL_COLOR_PREF)) {
            SlimSettings.System.putInt(resolver,
            SlimSettings.System.BATTERY_LIGHT_FULL_COLOR, color);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS
                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefaults();
                return true;
        }
        return false;
    }

    protected void resetColors() {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();

        // Reset to the framework default colors
        SlimSettings.System.putInt(resolver, SlimSettings.System.BATTERY_LIGHT_LOW_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
        SlimSettings.System.putInt(resolver, SlimSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                res.getInteger(
                com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
        SlimSettings.System.putInt(resolver, SlimSettings.System.BATTERY_LIGHT_MEDIUM_FAST_COLOR,
                res.getInteger(
                org.slim.framework.internal.R.integer.config_notificationsBatteryMediumFastARGB));
        SlimSettings.System.putInt(resolver, SlimSettings.System.BATTERY_LIGHT_FULL_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
        refreshDefault();
    }

    protected void resetToDefaults() {
        final Resources res = getResources();
        final boolean batteryLightEnabled = res.getBoolean(R.bool.def_battery_light_enabled);
        final boolean batteryLightPulseEnabled = res.getBoolean(R.bool.def_battery_light_pulse);

        if (mLightEnabledPref != null) mLightEnabledPref.setChecked(batteryLightEnabled);
        if (mPulseEnabledPref != null) mPulseEnabledPref.setChecked(batteryLightPulseEnabled);

        resetColors();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ApplicationLightPreference lightPref = (ApplicationLightPreference) preference;
        updateValues(lightPref.getKey(), lightPref.getColor());

        return true;
    }
}
