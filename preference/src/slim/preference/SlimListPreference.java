/*
 * Copyright (C) 2016 The CyanogenMod project
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

package slim.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import slim.utils.AttributeHelper;

public class SlimListPreference extends ListPreference {

    private int mSettingType;

    private SlimPreferenceManager mSlimPreferenceManager = SlimPreferenceManager.get();

    private String mListDependency;
    private String[] mListDependencyValues;

    public SlimListPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SlimListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SlimListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        AttributeHelper a = new AttributeHelper(context, attrs,
                slim.R.styleable.SlimPreference);

        mSettingType = SlimPreferenceManager.getSettingType(a);

        String list = a.getString(slim.R.styleable.SlimPreference_listDependency);
        if (!TextUtils.isEmpty(list)) {
            String[] listParts = list.split(":");
            mListDependency = listParts[0];
            mListDependencyValues = listParts[1].split("\\|");
        }

        boolean hidePreference =
                a.getBoolean(slim.R.styleable.SlimPreference_hidePreference, false);
        int hidePreferenceInt = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceInt, -1);
        int intDep = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceIntDependency, 0);
        if (hidePreference || hidePreferenceInt == intDep) {
            setVisible(false);
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

    @Override
    protected boolean persistString(String value) {
        if (shouldPersist()) {
            if (TextUtils.equals(value, getPersistedString(null))) {
                return true;
            }
            SlimPreferenceManager.putStringInSlimSettings(getContext(),
                    mSettingType, getKey(), value);
            mSlimPreferenceManager.updateDependents(this);
            return true;
        }
        return false;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return SlimPreferenceManager.getStringFromSlimSettings(getContext(), mSettingType, getKey(),
                defaultReturnValue);
    }

    @Override
    protected boolean isPersisted() {
        // Using getString instead of getInt so we can simply check for null
        // instead of catching an exception. (All values are stored as strings.)
        return SlimPreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }
}
