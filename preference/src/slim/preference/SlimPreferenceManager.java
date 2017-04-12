/*
* Copyright (C) 2016-2017 SlimRoms Project
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

package slim.preference;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;

import slim.provider.SlimSettings;
import slim.utils.AttributeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SlimPreferenceManager {
    public static final int SLIM_SYSTEM_SETTING = 0;
    public static final int SLIM_GLOBAL_SETTING = 1;
    public static final int SLIM_SECURE_SETTING = 2;
    public static final int AOSP_SYSTEM_SETTING = 3;
    public static final int AOSP_GLOBAL_SETTING = 4;
    public static final int AOSP_SECURE_SETTING = 5;

    private static SlimPreferenceManager INSTANCE;

    private HashMap<String, ArrayList<Dependent>> mDependents = new HashMap<>();

    public class Dependent {
        Preference dependent;
        String dependencyKey;
        String[] values;
    }

    private SlimPreferenceManager() {
    }

    public static SlimPreferenceManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SlimPreferenceManager();
        }
        return INSTANCE;
    }

    public void registerListDependent(Preference dep, String key, String[] values) {
        Dependent dependent = new Dependent();
        dependent.dependent = dep;
        dependent.dependencyKey = key;
        dependent.values = values;
        ArrayList<Dependent> deps = mDependents.get(key);
        if (deps == null) deps = new ArrayList<>();
        deps.add(dependent);
        mDependents.put(key, deps);
        dep.setEnabled(!Arrays.asList(values).contains(((ListPreference) dep.getPreferenceManager()
                .getPreferenceScreen().findPreference(key)).getValue()));
    }

    public void unregisterListDependent(Preference dep, String key) {
        ArrayList<Dependent> deps = mDependents.get(key);
        if (deps == null) return;
        for (Dependent dependent : deps) {
            if (dependent.dependent.getKey().equals(dep.getKey())) {
                deps.remove(dependent);
            }
        }
    }

    public void updateDependents(ListPreference dep) {
        ArrayList<Dependent> deps = mDependents.get(dep.getKey());
        if (deps == null) return;
        for (Dependent dependent : deps) {
            if (Arrays.asList(dependent.values).contains(dep.getValue())) {
                dependent.dependent.setEnabled(false);
            } else {
                dependent.dependent.setEnabled(true);
            }
        }
    }

    public static int getSettingType(AttributeHelper a) {
        int s = a.getInt(slim.R.styleable.SlimPreference_slimSettingType,
                SLIM_SYSTEM_SETTING);
        Log.d("TEST", "settingType=" + s);
        switch (s) {
            case SLIM_GLOBAL_SETTING:
                return SLIM_GLOBAL_SETTING;
            case SLIM_SECURE_SETTING:
                return SLIM_SECURE_SETTING;
            case AOSP_SYSTEM_SETTING:
                return AOSP_SYSTEM_SETTING;
            case AOSP_GLOBAL_SETTING:
                return AOSP_GLOBAL_SETTING;
            case AOSP_SECURE_SETTING:
                return AOSP_SECURE_SETTING;
            default:
                return SLIM_SYSTEM_SETTING;
        }
    }


    public static int getIntFromSlimSettings(
            Context context, int settingType, String key, int def) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getInt(context.getContentResolver(), key, def);
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
            case AOSP_SYSTEM_SETTING:
                return Settings.System.getIntForUser(context.getContentResolver(), key, def,
                        UserHandle.USER_CURRENT);
            case AOSP_GLOBAL_SETTING:
                return Settings.Global.getInt(context.getContentResolver(), key, def);
            case AOSP_SECURE_SETTING:
                return Settings.Secure.getIntForUser(context.getContentResolver(), key, def,
                        UserHandle.USER_CURRENT);
            default:
                return SlimSettings.System.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
        }
    }

    public static void putIntInSlimSettings(Context context, int settingType, String key, int val) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                SlimSettings.Global.putInt(context.getContentResolver(), key, val);
                break;
            case SLIM_SECURE_SETTING:
                SlimSettings.Secure.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            case AOSP_SYSTEM_SETTING:
                Settings.System.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            case AOSP_GLOBAL_SETTING:
                Settings.Global.putInt(context.getContentResolver(), key, val);
                break;
            case AOSP_SECURE_SETTING:
                Settings.Secure.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                SlimSettings.System.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    public static String getStringFromSlimSettings(Context context,
            int settingType, String key, String def) {
        if (!settingExists(context, settingType, key)) return def;
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getString(context.getContentResolver(), key);
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
            case AOSP_SYSTEM_SETTING:
                return Settings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
            case AOSP_GLOBAL_SETTING:
                return Settings.Global.getString(context.getContentResolver(), key);
            case AOSP_SECURE_SETTING:
                return Settings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
            default:
                return SlimSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
        }
    }

    public static void putStringInSlimSettings(
            Context context, int settingType, String key, String val) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                SlimSettings.Global.putString(context.getContentResolver(), key, val);
                break;
            case SLIM_SECURE_SETTING:
                SlimSettings.Secure.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            case AOSP_SYSTEM_SETTING:
                Settings.System.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            case AOSP_GLOBAL_SETTING:
                Settings.Global.putString(context.getContentResolver(), key, val);
                break;
            case AOSP_SECURE_SETTING:
                Settings.Secure.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                SlimSettings.System.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    public static boolean settingExists(Context context, int settingType, String key) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getString(context.getContentResolver(), key) != null;
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
            case AOSP_SYSTEM_SETTING:
                return Settings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
            case AOSP_GLOBAL_SETTING:
                return Settings.Global.getString(context.getContentResolver(), key) != null;
            case AOSP_SECURE_SETTING:
                return Settings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
            default:
                return SlimSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
        }
    }
}
