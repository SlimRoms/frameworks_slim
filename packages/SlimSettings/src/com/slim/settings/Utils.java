/**
 * Copyright (C) 2007 Google Inc.
 * Copyright (C) 2017 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.slim.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;

import java.util.List;

import com.slim.settings.SubSettings;

public final class Utils {

    /**
     * Set the preference's title to the matching activity's label.
     */
    public static final int UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY = 1;

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     * @param flags 0 or one or more of
     *            {@link #UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY}
     *            .
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     */
    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    if ((flags & UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY) != 0) {
                        // Set the preference title to the activity's label
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

     /**
     * Start a new instance of the activity, showing only the given fragment.
     * When launched in this mode, the given preference fragment will be instantiated and fill the
     * entire activity.
     *
     * @param context The context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which
     *                          to report the result.
     * @param title String to display for the title of this set of preferences.
     */
    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, CharSequence title, boolean showMenu) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                title, false /* not a shortcut */, showMenu);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode,
            CharSequence title, boolean isShortcut, boolean showMenu) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args,
                title, isShortcut, showMenu);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    /**
     * Build an Intent to launch a new activity showing the selected fragment.
     * The implementation constructs an Intent that re-launches the current activity with the
     * appropriate arguments to display the fragment.
     *
     *
     * @param context The Context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param title Optional title to show for this item.
     * @param isShortcut  tell if this is a Launcher Shortcut or not
     * @return Returns an Intent that can be launched to display the given
     * fragment.
     */
    public static Intent onBuildStartFragmentIntent(Context context,
            String fragmentName, Bundle args, CharSequence title,
            boolean isShortcut, boolean showMenu) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(context, SubSettings.class);
        intent.putExtra(SubSettings.EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(SubSettings.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(SubSettings.EXTRA_SHOW_FRAGMENT_TITLE, title);
        intent.putExtra(SubSettings.EXTRA_SHOW_FRAGMENT_SHOW_MENU, showMenu);
        return intent;
    }
}
