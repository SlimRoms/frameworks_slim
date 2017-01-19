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

package slim.action;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import slim.action.ActionConstants;
import slim.utils.DeviceUtils;

public class ActionsArray {

    private String[] values;
    private String[] entries;

    public ActionsArray(Context context) {
        this(context, true, false, null);
    }

    public ActionsArray(Context context, boolean showWake) {
        this(context, true, showWake, null);
    }

    public ActionsArray(Context context, boolean showNone, boolean showWake) {
        this(context, showNone, showWake, null);
    }

    public ActionsArray(Context context, boolean showNone, boolean showWake,
            ArrayList<String> actionsToExclude) {
        String[] initialValues = context.getResources().getStringArray(
                slim.R.array.shortcut_action_values);
        String[] initialEntries = context.getResources().getStringArray(
                slim.R.array.shortcut_action_entries);

        List<String> finalEntries = new ArrayList<>();
        List<String> finalValues = new ArrayList<>();

        for (int i = 0; i < initialValues.length; i++) {
            if (!showNone && ActionConstants.ACTION_NULL.equals(initialValues[i])
                    || !showWake && ActionConstants.ACTION_WAKE_DEVICE.equals(initialValues[i])
                    || !showWake && ActionConstants.ACTION_DOZE_PULSE.equals(initialValues[i])) {
                continue;
            } else if (actionsToExclude != null && actionsToExclude.contains(initialValues[i])) {
                continue;
            } else if (isSupported(context, initialValues[i])) {
                finalEntries.add(initialEntries[i]);
                finalValues.add(initialValues[i]);
            }
        }

        entries = finalEntries.toArray(new String[0]);
        values = finalValues.toArray(new String[0]);
    }

    public String getEntry(int index) {
        return entries[index];
    }

    public String[] getEntries() {
        return entries;
    }

    public String getValue(int index) {
        return values[index];
    }

    public String[] getValues() {
        return values;
    }

    private static boolean isSupported(Context context, String action) {
        if (action.equals(ActionConstants.ACTION_TORCH)
                        && !DeviceUtils.deviceSupportsTorch(context)
                || action.equals(ActionConstants.ACTION_VIB)
                        && !DeviceUtils.deviceSupportsVibrator(context)
                || action.equals(ActionConstants.ACTION_VIB_SILENT)
                        && !DeviceUtils.deviceSupportsVibrator(context)) {
            return false;
        }
        return true;
    }
}
