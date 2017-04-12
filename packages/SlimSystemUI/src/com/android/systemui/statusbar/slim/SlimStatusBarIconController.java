/*
 * Copyright (C) 2016 SlimRoms Project
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

package com.android.systemui.statusbar.slim;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;

public class SlimStatusBarIconController {

    private Context mContext;

    private SlimStatusBar mSlimStatusBar;
    private SlimBatteryContainer mSlimBattery;

    private int mIconTint = Color.WHITE;
    private float mDarkIntensity;

    private ValueAnimator mTintAnimator;
    private Interpolator mFastOutSlowIn;
    private int mDarkModeIconColorSingleTone;
    private int mLightModeIconColorSingleTone;

    public SlimStatusBarIconController(Context context, View statusBar,
            SlimStatusBar slimStatusBar) {
        mContext = context;
        mSlimStatusBar = slimStatusBar;

        mSlimBattery = (SlimBatteryContainer) statusBar.findViewById(R.id.slim_battery_container);

        mFastOutSlowIn = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.fast_out_slow_in);
        mDarkModeIconColorSingleTone = context.getColor(R.color.dark_mode_icon_color_single_tone);
        mLightModeIconColorSingleTone = context.getColor(R.color.light_mode_icon_color_single_tone);
    }

    public void setIconsDark(boolean dark, boolean animate) {
        if (!animate) {
            setIconTintInternal(dark ? 1.0f : 0.0f);
        } else {
            animateIconTint(dark ? 1.0f : 0.0f, 0,
                    StatusBarIconController.DEFAULT_TINT_ANIMATION_DURATION);
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay, long duration) {
        if (mTintAnimator != null) {
            mTintAnimator.cancel();
        }

        if (mDarkIntensity == targetDarkIntensity) {
            return;
        }

        mTintAnimator = ValueAnimator.ofFloat(mDarkIntensity, targetDarkIntensity);
        mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setIconTintInternal((Float) animation.getAnimatedValue());
            }
        });

        mTintAnimator.setDuration(duration);
        mTintAnimator.setStartDelay(delay);
        mTintAnimator.setInterpolator(mFastOutSlowIn);
        mTintAnimator.start();
    }

    private void setIconTintInternal(float darkIntensity) {
        mDarkIntensity = darkIntensity;
        mIconTint = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                mLightModeIconColorSingleTone, mDarkModeIconColorSingleTone);
        applyIconTint();
    }

    private void applyIconTint() {
        mSlimBattery.setDarkIntensity(mDarkIntensity);
    }
    
    public void hideSystemIconArea(boolean animate) {
    }
  
    public void showSystemIconArea(boolean animate) {
    }
    
    public void hideNotificationIconArea(boolean animate) {
    }
  
    public void showNotificationIconArea(boolean animate) {
    }
    
    public void setClockVisibility(boolean visible) {
    }
}
