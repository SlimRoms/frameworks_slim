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
import android.util.Log;

import slim.provider.SlimSettings;

class SlimPreference {
    static final int SLIM_SYSTEM_SETTING = 0;
    static final int SLIM_GLOBAL_SETTING = 1;
    static final int SLIM_SECURE_SETTING = 2;

    static int getIntFromSlimSettings(Context context, int settingType, String key, int def) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getInt(context.getContentResolver(), key, def);
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
            default:
                return SlimSettings.System.getIntForUser(context.getContentResolver(), key,
                        def, UserHandle.USER_CURRENT);
        }
    }

    static void putIntInSlimSettings(Context context, int settingType, String key, int val) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                SlimSettings.Global.putInt(context.getContentResolver(), key, val);
                break;
            case SLIM_SECURE_SETTING:
                SlimSettings.Secure.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                SlimSettings.System.putIntForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    static String getStringFromSlimSettings(Context context,
            int settingType, String key, String def) {
        if (!settingExists(context, settingType, key)) return def;
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getString(context.getContentResolver(), key);
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
            default:
                return SlimSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT);
        }
    }

    static void putStringInSlimSettings(Context context, int settingType, String key, String val) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                SlimSettings.Global.putString(context.getContentResolver(), key, val);
                break;
            case SLIM_SECURE_SETTING:
                SlimSettings.Secure.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
            default:
                SlimSettings.System.putStringForUser(context.getContentResolver(), key, val,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    static boolean settingExists(Context context, int settingType, String key) {
        switch (settingType) {
            case SLIM_GLOBAL_SETTING:
                return SlimSettings.Global.getString(context.getContentResolver(), key) != null;
            case SLIM_SECURE_SETTING:
                return SlimSettings.Secure.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
            default:
                return SlimSettings.System.getStringForUser(context.getContentResolver(), key,
                        UserHandle.USER_CURRENT) != null;
        }
    }
}
