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
import android.os.Bundle;

import com.slim.settings.fragments.SlimPreferenceFragment;
import com.slim.settings.SettingsActivity;

public class GestureSettingsActivity extends SettingsActivity {

    @Override
    public Fragment getFragment() {
        return new SlimPreferenceFragment();
    }

    @Override
    public Bundle getFragmentBundle() {
        Bundle b = new Bundle();
        b.putString("preference_xml", "gesture_settings");
        return b;
    }
}
