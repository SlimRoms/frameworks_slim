/*
 * Copyright (C) 2016-2017 SlimRoms Project
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

package com.slim.settings;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.MenuItem;

import com.slim.settings.Utils;
import com.android.settingslib.drawer.SettingsDrawerActivity;

public class SettingsActivity extends SettingsDrawerActivity implements
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        PreferenceFragment.OnPreferenceStartScreenCallback {

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.slim.settings.FRAGMENT_CLASS";
    public static final String META_DATA_KEY_PREFRERENCE_XML =
            "com.slim.settings.PREFERENCE_XML";

    private String mFragmentClass;
    private String mPreferenceXML;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getMetaData();

        Fragment fragment = getFragment();
        if (mFragmentClass != null) {
            fragment = Fragment.instantiate(this, mFragmentClass);
        }
        if (fragment != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (getFragmentBundle() != null) {
                fragment.setArguments(getFragmentBundle());
            }
            transaction.replace(R.id.content_frame, fragment);
            transaction.commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
            mPreferenceXML = ai.metaData.getString(META_DATA_KEY_PREFRERENCE_XML);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        // Override the fragment title for Wallpaper settings
        startPreferencePanel(pref.getFragment(), pref.getExtras(), pref.getTitle(),
                null, 0, false);
        return true;
    }

    public void startPreferencePanel(String fragmentClass, Bundle args,
            CharSequence title, Fragment resultTo, int resultRequestCode, boolean showMenu) {
        Utils.startWithFragment(this, fragmentClass, args, resultTo,
                resultRequestCode, title, showMenu);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        return startPreferenceScreen(caller, pref, true);
    }

    public boolean startPreferenceScreen(PreferenceFragment caller, PreferenceScreen pref,
                                         boolean addToBackStack) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        SubSettingsFragment fragment = new SubSettingsFragment();
        final Bundle b = new Bundle(1);
        b.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(b);
        fragment.setTargetFragment(caller, 0);
        transaction.replace(R.id.content_frame, fragment);
        if (addToBackStack) {
            transaction.addToBackStack("PreferenceFragment");
        }
        transaction.commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static class SubSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferenceScreen((PreferenceScreen) ((PreferenceFragment) getTargetFragment())
                    .getPreferenceScreen().findPreference(rootKey));
        }
    }

    public Fragment getFragment() {
        return null;
    }

    public Bundle getFragmentBundle() {
        if (!TextUtils.isEmpty(mPreferenceXML)) {
            Bundle b = new Bundle();
            b.putString("preference_xml", mPreferenceXML);
            return b;
        }
        return null;
    }
}
