/*
 * Copyright (C) 2017 SlimRoms Project
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

package slim.preference.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.slim.framework.internal.R;

import slim.preference.SlimPreferenceManager;
import slim.utils.AttributeHelper;

public class ColorPickerPreference extends com.enrico.colorpicker.ColorPickerPreference {

    private int mDefaultColor;
    private int mSettingType;

    private SlimPreferenceManager mSlimPreferenceManager = SlimPreferenceManager.getInstance();
    private String mListDependency;
    private String[] mListDependencyValues;

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            AttributeHelper a =
                    new AttributeHelper(context, attrs, slim.R.styleable.ColorPickerPreference);

            mDefaultColor = a.getInt(slim.R.styleable.ColorPickerPreference_defaultColor,
                    Color.WHITE);

            a = new AttributeHelper(context, attrs, slim.R.styleable.SlimPreference);

            mSettingType = SlimPreferenceManager.getSettingType(a);

            String list = a.getString(slim.R.styleable.SlimPreference_listDependency);
            if (!TextUtils.isEmpty(list)) {
                String[] listParts = list.split(":");
                if (listParts.length == 2) {
                    mListDependency = listParts[0];
                    mListDependencyValues = listParts[1].split("\\|");
                }
           }

           boolean hidePreference =
                    a.getBoolean(slim.R.styleable.SlimPreference_hidePreference, false);
            int hidePreferenceInt = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceInt, -1);
            int intDep = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceIntDependency, 0);
            if (hidePreference || hidePreferenceInt == intDep) {
                setVisible(false);
            }
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mListDependency != null) {
            mSlimPreferenceManager.registerListDependent(
                    this, mListDependency, mListDependencyValues);
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mListDependency != null) {
            mSlimPreferenceManager.unregisterListDependent(this, mListDependency);
        }
    }

    public int getDefaultColor() {
        return mDefaultColor;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param color
     * @author Unknown
     */
    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    public static int convertToColorInt(String argb) throws NumberFormatException {

        if (argb.startsWith("#")) {
            argb = argb.replace("#", "");
        }

        int alpha = -1, red = -1, green = -1, blue = -1;

        if (argb.length() == 8) {
            alpha = Integer.parseInt(argb.substring(0, 2), 16);
            red = Integer.parseInt(argb.substring(2, 4), 16);
            green = Integer.parseInt(argb.substring(4, 6), 16);
            blue = Integer.parseInt(argb.substring(6, 8), 16);
        }
        else if (argb.length() == 6) {
            alpha = 255;
            red = Integer.parseInt(argb.substring(0, 2), 16);
            green = Integer.parseInt(argb.substring(2, 4), 16);
            blue = Integer.parseInt(argb.substring(4, 6), 16);
        }

        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected boolean persistInt(int value) {
        if (shouldPersist()) {
            if (value == getPersistedInt(Integer.MIN_VALUE)) {
                return true;
            }
            SlimPreferenceManager.putIntInSlimSettings(getContext(),
                    mSettingType, getKey(), value);
            return true;
        }
        return false;
    }

    @Override
    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return SlimPreferenceManager.getIntFromSlimSettings(getContext(), mSettingType,
                getKey(), defaultReturnValue);
    }

    @Override
    protected boolean isPersisted() {
        return SlimPreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }
}
