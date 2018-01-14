/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2018 The SlimRoms Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.slim;

import android.graphics.Color;
import android.annotation.Nullable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;

import slim.utils.ImageHelper;

/**
 * Drawable for {@link KeyButtonView}s which contains an asset for both normal mode and light
 * navigation bar mode.
 */
public class SlimKeyButtonDrawable extends LayerDrawable {

    private final boolean mHasDarkDrawable;

    public static SlimKeyButtonDrawable create(Drawable lightDrawable,
            @Nullable Drawable darkDrawable) {
        Drawable light = lightDrawable.mutate();
        Drawable dark;
        if (darkDrawable != null) {
            dark = darkDrawable.mutate();
        } else {
            dark = light.getConstantState().newDrawable().mutate();
            dark = ImageHelper.getColoredDrawable(dark, Color.BLACK);
        }
        return new SlimKeyButtonDrawable(
                new Drawable[] { light, dark });
    }

    private SlimKeyButtonDrawable(Drawable[] drawables) {
        super(drawables);
        for (int i = 0; i < drawables.length; i++) {
            setLayerGravity(i, Gravity.CENTER);
        }
        mutate();
        mHasDarkDrawable = drawables.length > 1;
        setDarkIntensity(0f);
    }

    public void setDarkIntensity(float intensity) {
        if (!mHasDarkDrawable) {
            return;
        }
        android.util.Log.d("TEST", "intensity - " + intensity);
        getDrawable(0).setAlpha((int) ((1 - intensity) * 255f));
        getDrawable(1).setAlpha((int) (intensity * 255f));
        invalidateSelf();
    }
}