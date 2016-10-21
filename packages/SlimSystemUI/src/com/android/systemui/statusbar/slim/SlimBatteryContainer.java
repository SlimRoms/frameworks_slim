/*
* Copyright (C) 2016 SlimRoms Project
* Copyright (C) 2013-14 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.systemui.statusbar.slim;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;

import org.slim.provider.SlimSettings;

public class SlimBatteryContainer extends LinearLayout implements
        BatteryController.BatteryStateChangeCallback {

    private BatteryController mBatteryController;
    private BatterySettingsObserver mBatteryObserver;
    private SlimBatteryMeterView mBattery;
    private View mSpacer;
    private TextView mBatteryLevel;

    private boolean mAttached;
    private boolean mShowBatteryText;
    private boolean mShowBatteryTextCharging;
    private boolean mShowBatteryTextSpacer;
    private boolean mBatteryIsCharging;
    private int mBatteryChargeLevel;

    public SlimBatteryContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBatteryObserver = new BatterySettingsObserver(new Handler());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBattery = (SlimBatteryMeterView) findViewById(R.id.slim_battery);
        mSpacer = findViewById(R.id.battery_batterytext_spacer);
        mBatteryLevel = (TextView) findViewById(R.id.battery_level_text);

        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBatteryController != null) {
            mBatteryController.removeStateChangedCallback(this);
        }
        mBatteryObserver.unObserve();

        mAttached = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mBatteryController != null) {
            mBatteryController.addStateChangedCallback(this);
        }
        mBatteryObserver.observe();
        mAttached = true;
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryIsCharging = charging;
        mBatteryChargeLevel = level;
        updateSettings();
    }

    @Override
    public void onPowerSaveChanged(boolean enabled) {
        // could not care less
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mBattery.setBatteryController(batteryController);
        mBatteryController.addStateChangedCallback(this);
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mShowBatteryText = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_PERCENT, 0) == 2;

        int batteryStyle = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_STYLE, 0);

        switch(batteryStyle) {
            case 4:
                //meterMode = BatteryMeterMode.BATTERY_METER_GONE;
                mShowBatteryText = false;
                mShowBatteryTextCharging = false;
                mShowBatteryTextSpacer = false;
                break;

            case 6:
                //meterMode = BatteryMeterMode.BATTERY_METER_TEXT;
                mShowBatteryText = true;
                mShowBatteryTextCharging = true;
                mShowBatteryTextSpacer = false;
                break;

            default:
                mShowBatteryTextCharging = false;
                mShowBatteryTextSpacer = mShowBatteryText;
                break;
        }

        // update visibilities
        mBatteryLevel.setVisibility(mShowBatteryText ? View.VISIBLE : View.GONE);
        mSpacer.setVisibility(mShowBatteryTextSpacer ? View.VISIBLE : View.GONE);

        mBattery.updateBatteryIconSettings();
        updateBatteryLevelText();
    }

    private void updateBatteryLevelText() {
        if (mBatteryIsCharging & mShowBatteryTextCharging) {
            mBatteryLevel.setText(mContext.getResources().getString(
                    R.string.battery_level_template_charging, mBatteryChargeLevel));
        } else {
            mBatteryLevel.setText(mContext.getResources().getString(
                    R.string.battery_level_template, mBatteryChargeLevel));
        }
    }

    private final class BatterySettingsObserver extends ContentObserver {
        BatterySettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getContext().getContentResolver();

            resolver.registerContentObserver(SlimSettings.Secure.getUriFor(
                    SlimSettings.Secure.STATUS_BAR_BATTERY_PERCENT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.Secure.getUriFor(
                    SlimSettings.Secure.STATUS_BAR_BATTERY_STYLE),
                    false, this, UserHandle.USER_ALL);
        }

        void unObserve() {
            getContext().getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateSettings();
        }
    }

    public void setDarkIntensity(float darkIntensity) {
        mBattery.setDarkIntensity(darkIntensity);
        mBatteryLevel.setTextColor(mBattery.getFillColor(darkIntensity));
    }
}
