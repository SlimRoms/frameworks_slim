/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2014-2017 SlimRoms Project
 * Copyright (C) 2017 ABC rom
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

package com.android.systemui.slimrecent;

import android.content.Intent;
import android.content.pm.ActivityInfo;

public final class TaskDescription {
    final ActivityInfo info;
    final int taskId; // application task id for curating apps
    final int persistentTaskId; // persistent id
    final Intent intent; // launch intent for application
    final String packageName; // used to override animations (see onClick())
    final String identifier;
    final CharSequence description;
    int cardColor = 0;
    final String componentName;

    private String mLabel; // application package label
    private int mExpandedState;
    private boolean mIsFavorite;

    public TaskDescription(int _taskId, int _persistentTaskId,
            ActivityInfo _info, Intent _intent,
            String _packageName, String _componentName,
            String _identifier, CharSequence _description,
            boolean isFavorite, int expandedState, int activityColor) {
        info = _info;
        intent = _intent;
        taskId = _taskId;
        persistentTaskId = _persistentTaskId;

        description = _description;
        packageName = _packageName;
        componentName = _componentName;
        identifier = _identifier;

        mExpandedState = expandedState;
        mIsFavorite = isFavorite;
        cardColor = activityColor;
    }

    public TaskDescription() {
        info = null;
        intent = null;
        taskId = -1;
        persistentTaskId = -1;

        description = null;
        packageName = null;
        componentName = null;
        identifier = null;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public int getExpandedState() {
        return mExpandedState;
    }

    public void setExpandedState(int expandedState) {
        mExpandedState = expandedState;
    }

    public boolean getIsFavorite() {
        return mIsFavorite;
    }

    public void setIsFavorite(boolean isFavorite) {
        mIsFavorite = isFavorite;
    }

}
