/*
 * Copyright (C) 2016-2017 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slim.settings.activities;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.PreferenceManager;
import android.os.Bundle;

import com.slim.settings.fragments.SlimPreferenceFragment;
import com.slim.settings.SettingsActivity;

import java.util.List;

public class AdvancedSettingsActivity extends SettingsActivity {

    @Override
    public Fragment getFragment() {
        return new SlimPreferenceFragment();
    }

    @Override
    public Bundle getFragmentBundle() {
        Bundle b = new Bundle();
        b.putString("preference_xml", "slim_advanced_settings");
        return b;
    }

    public static void checkSettings(Context context) {
        boolean checked = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("checked_device_settings", false);

        if (checked) return;

        Intent intent = new Intent("com.cyanogenmod.action.LAUNCH_DEVICE_SETTINGS");

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        int listSize = list.size();

        if (listSize < 1) {
            ComponentName cmp = new ComponentName(context, AdvancedSettingsActivity.class.getName());
            pm.setComponentEnabledSetting(cmp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean("checked_device_settings", true).apply();
        }
    }
}
