/*
 * Copyright (C) 2013-2018 SlimRoms Project
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.internal.hardware.AmbientDisplayConfiguration;

import com.slim.settings.R;
import com.slim.settings.SettingsPreferenceFragment;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import slim.preference.SlimSeekBarPreferencev2;
import slim.provider.SlimSettings;

public class DozeSettingsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_DOZE_FADE_IN_PICKUP         = "doze_fade_in_pickup";
    private static final String KEY_DOZE_FADE_IN_DOUBLETAP      = "doze_fade_in_doubletap";
    private static final String KEY_DOZE_TIMEOUT                = "doze_timeout";
    private static final String KEY_DOZE_FADE_OUT               = "doze_fade_out";
    private static final String KEY_DOZE_BRIGHTNESS             = "doze_brightness";
    private static final String KEY_DOZE_WAKEUP_DOUBLETAP       = "doze_wakeup_doubletap";
    private static final String KEY_DOZE_TRIGGER_PICKUP         = "doze_trigger_pickup";
    private static final String KEY_DOZE_TRIGGER_TILT           = "doze_trigger_tilt";
    private static final String KEY_DOZE_TRIGGER_SIGMOTION      = "doze_trigger_sigmotion";
    private static final String KEY_DOZE_TRIGGER_NOTIFICATION   = "doze_trigger_notification";
//    private static final String KEY_DOZE_TRIGGER_DOUBLETAP    = "doze_trigger_doubletap";
    private static final String KEY_DOZE_TRIGGER_HAND_WAVE      = "doze_trigger_hand_wave";
    private static final String KEY_DOZE_TRIGGER_POCKET         = "doze_trigger_pocket";

    private static final String SYSTEMUI_METADATA_NAME          = "com.android.systemui";

    private SlimSeekBarPreferencev2 mDozeFadeInPickup;
    private SlimSeekBarPreferencev2 mDozeFadeInDoubleTap;
    private SlimSeekBarPreferencev2 mDozeTimeout;
    private SlimSeekBarPreferencev2 mDozeFadeOut;
    private SlimSeekBarPreferencev2 mDozeBrightness;
    private SwitchPreference mDozeWakeupDoubleTap;
    private SwitchPreference mDozeTriggerPickup;
    private SwitchPreference mDozeTriggerTilt;
    private SwitchPreference mDozeTriggerSigmotion;
    private SwitchPreference mDozeTriggerNotification;
//    private SwitchPreference mDozeTriggerDoubleTap;
    private SwitchPreference mDozeTriggerHandWave;
    private SwitchPreference mDozeTriggerPocket;

    private AmbientDisplayConfiguration mConfig;

    private float mBrightnessScale;
    private float mDefaultBrightnessScale;

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.DOZE_SETTINGS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mConfig = new AmbientDisplayConfiguration(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.doze_settings;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        PreferenceScreen prefSet = getPreferenceScreen();
        Resources res = getResources();

        // Doze fade in seekbar for pickup
        mDozeFadeInPickup = (SlimSeekBarPreferencev2) findPreference(KEY_DOZE_FADE_IN_PICKUP);
        mDozeFadeInPickup.setOnPreferenceChangeListener(this);

        // Doze fade in seekbar for doubletap
        if (isDoubleTapSensorUsedByDefault(mConfig) || isTapToWakeAvailable(res)) {
            mDozeFadeInDoubleTap =
                    (SlimSeekBarPreferencev2) findPreference(KEY_DOZE_FADE_IN_DOUBLETAP);
            mDozeFadeInDoubleTap.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_FADE_IN_DOUBLETAP);
        }            

        // Doze timeout seekbar
        mDozeTimeout = (SlimSeekBarPreferencev2) findPreference(KEY_DOZE_TIMEOUT);
        mDozeTimeout.setOnPreferenceChangeListener(this);

        // Doze fade out seekbar
        mDozeFadeOut = (SlimSeekBarPreferencev2) findPreference(KEY_DOZE_FADE_OUT);
        mDozeFadeOut.setOnPreferenceChangeListener(this);

        // Doze brightness
        mDefaultBrightnessScale =
                (float) res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessDoze) / res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMaximum);
        mDozeBrightness = (SlimSeekBarPreferencev2) findPreference(KEY_DOZE_BRIGHTNESS);
        mDozeBrightness.setOnPreferenceChangeListener(this);

        // Double-tap to wake from doze
        mDozeWakeupDoubleTap = (SwitchPreference) findPreference(KEY_DOZE_WAKEUP_DOUBLETAP);
        mDozeWakeupDoubleTap.setOnPreferenceChangeListener(this);

        // Doze triggers
        if (isPickupSensorUsedByDefault(mConfig)) {
            mDozeTriggerPickup = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_PICKUP);
            mDozeTriggerPickup.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_PICKUP);
        }
        if (isTiltSensorUsedByDefault(mConfig)) {
            mDozeTriggerTilt = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_TILT);
            mDozeTriggerTilt.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_TILT);
        }
        if (isSigmotionSensorUsedByDefault(getActivity())) {
            mDozeTriggerSigmotion = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_SIGMOTION);
            mDozeTriggerSigmotion.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_SIGMOTION);
        }
        if (isProximitySensorUsedByDefault(mConfig)) {
            mDozeTriggerHandWave = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_HAND_WAVE);
            mDozeTriggerHandWave.setOnPreferenceChangeListener(this);
            mDozeTriggerPocket = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_POCKET);
            mDozeTriggerPocket.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_HAND_WAVE);
            removePreference(KEY_DOZE_TRIGGER_POCKET);
        }
        mDozeTriggerNotification = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_NOTIFICATION);
        mDozeTriggerNotification.setOnPreferenceChangeListener(this);
//        if (isDoubleTapSensorUsedByDefault(mConfig) || isTapToWakeAvailable(res)) {
//            mDozeTriggerDoubleTap = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_DOUBLETAP);
//            mDozeTriggerDoubleTap.setOnPreferenceChangeListener(this);
//            if (!isTapToWakeEnabled() && !isDoubleTapSensorUsedByDefault(mConfig)) {
//                mDozeTriggerDoubleTap.setEnabled(false);
//            }
//        } else {
//            removePreference(KEY_DOZE_TRIGGER_DOUBLETAP);
//        }

        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDozeFadeInPickup) {
            int dozeFadeInPickup = (Integer) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_IN_PICKUP, dozeFadeInPickup);
        } else if (preference == mDozeFadeInDoubleTap) {
            int dozeFadeInDoubleTap = (Integer) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_IN_DOUBLETAP, dozeFadeInDoubleTap);
        } else if (preference == mDozeTimeout) {
            int dozeTimeout = (Integer) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TIMEOUT, dozeTimeout);
        } else if (preference == mDozeFadeOut) {
            int dozeFadeOut = (Integer) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_OUT, dozeFadeOut);
        } else if (preference == mDozeBrightness) {
            float valNav = (float) ((Integer) newValue);
            SlimSettings.System.putFloat(getContentResolver(),
                    SlimSettings.System.DOZE_BRIGHTNESS, valNav / 100);
        } else if (preference == mDozeWakeupDoubleTap) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_WAKEUP_DOUBLETAP, value ? 1 : 0);
        } else if (preference == mDozeTriggerPickup) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_PICKUP, value ? 1 : 0);
        } else if (preference == mDozeTriggerTilt) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_TILT, value ? 1 : 0);
        } else if (preference == mDozeTriggerSigmotion) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_SIGMOTION, value ? 1 : 0);
        } else if (preference == mDozeTriggerHandWave) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_HAND_WAVE, value ? 1 : 0);
        } else if (preference == mDozeTriggerPocket) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_POCKET, value ? 1 : 0);
        } else if (preference == mDozeTriggerNotification) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_NOTIFICATION, value ? 1 : 0);
//        } else if (preference == mDozeTriggerDoubleTap) {
//            boolean value = (Boolean) newValue;
//            SlimSettings.System.putInt(getContentResolver(),
//                    SlimSettings.System.DOZE_TRIGGER_DOUBLETAP, value ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        final Activity activity = getActivity();

        // Update doze preferences
        if (mDozeFadeInPickup != null) {
            final int statusDozeFadeInPickup = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_IN_PICKUP, dozeFadeInDefault(activity, true));
            mDozeFadeInPickup.setValue(statusDozeFadeInPickup);
        }
        if (mDozeFadeInDoubleTap != null) {
            final int statusDozeFadeInDoubleTap = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_IN_DOUBLETAP, dozeFadeInDefault(activity, false));
            mDozeFadeInDoubleTap.setValue(statusDozeFadeInDoubleTap);
        }
        if (mDozeTimeout != null) {
            final int statusDozeTimeout = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TIMEOUT, dozeTimeoutDefault(activity));
            mDozeTimeout.setValue(statusDozeTimeout);
        }
        if (mDozeFadeOut != null) {
            final int statusDozeFadeOut = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_FADE_OUT, dozeFadeOutDefault(activity));
            mDozeFadeOut.setValue(statusDozeFadeOut);
        }
        if (mDozeBrightness != null) {
            mBrightnessScale = SlimSettings.System.getFloat(getContentResolver(),
                    SlimSettings.System.DOZE_BRIGHTNESS, mDefaultBrightnessScale);
            mDozeBrightness.setValue((int) (mBrightnessScale * 100));
        }
        if (mDozeWakeupDoubleTap != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_WAKEUP_DOUBLETAP, 0);
            mDozeWakeupDoubleTap.setChecked(value != 0);
        }
        if (mDozeTriggerPickup != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_PICKUP, 1);
            mDozeTriggerPickup.setChecked(value != 0);
        }
        if (mDozeTriggerTilt != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_TILT, 1);
            mDozeTriggerTilt.setChecked(value != 0);
        }
        if (mDozeTriggerSigmotion != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_SIGMOTION, 1);
            mDozeTriggerSigmotion.setChecked(value != 0);
        }
        if (mDozeTriggerHandWave != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_HAND_WAVE, 1);
            mDozeTriggerHandWave.setChecked(value != 0);
        }
        if (mDozeTriggerPocket != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_POCKET, 1);
            mDozeTriggerPocket.setChecked(value != 0);
        }
        if (mDozeTriggerNotification != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_NOTIFICATION, 1);
            mDozeTriggerNotification.setChecked(value != 0);
        }
//        if (mDozeTriggerDoubleTap != null) {
//            int value = SlimSettings.System.getInt(getContentResolver(),
//                    SlimSettings.System.DOZE_TRIGGER_DOUBLETAP, 0);
//            mDozeTriggerDoubleTap.setChecked(value != 0);
//        }
    }

    private boolean isTapToWakeEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_WAKE, 0) == 1;
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isPickupSensorUsedByDefault(AmbientDisplayConfiguration config) {
        return config.pulseOnPickupAvailable();
    }

    private static boolean isTiltSensorUsedByDefault(AmbientDisplayConfiguration config) {
        return false;// config.pulseOnTiltAvailable();
    }

    private static boolean isProximitySensorUsedByDefault(AmbientDisplayConfiguration config) {
        return false;//config.pulseOnProximityAvailable();
    }

    private static boolean isSigmotionSensorUsedByDefault(Context context) {
        return getConfigBoolean(context, "doze_pulse_on_significant_motion");
    }

    private static boolean isDoubleTapSensorUsedByDefault(AmbientDisplayConfiguration config) {
        return config.pulseOnDoubleTapAvailable();
    }

    private static int dozeFadeInDefault(Context context, boolean pickupOrDoubleTap) {
        return pickupOrDoubleTap
                ? getConfigInteger(context, "doze_pulse_duration_in_pickup")
                : getConfigInteger(context, "doze_pulse_duration_in");
    }

    private static int dozeTimeoutDefault(Context context) {
        return getConfigInteger(context, "doze_pulse_duration_visible");
    }

    private static int dozeFadeOutDefault(Context context) {
        return getConfigInteger(context, "doze_pulse_duration_out");
    }

    private static Boolean getConfigBoolean(Context context, String configBooleanName) {
        int resId = -1;
        Boolean b = true;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":bool/" + configBooleanName, null, null);
        if (resId > 0) {
            b = systemUiResources.getBoolean(resId);
        }
        return b;
    }

    private static Integer getConfigInteger(Context context, String configIntegerName) {
        int resId = -1;
        Integer i = 1;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":integer/" + configIntegerName, null, null);
        if (resId > 0) {
            i = systemUiResources.getInteger(resId);
        }
        return i;
    }
}
