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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import slim.utils.AppHelper;
import slim.utils.AttributeHelper;

import java.util.List;

public class SlimPreference extends Preference {

    public SlimPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public SlimPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SlimPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SlimPreference(Context context) {
        this(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        AttributeHelper a = new AttributeHelper(context, attrs,
            slim.R.styleable.SlimPreference);

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
        Intent intent = getIntent();
        if (intent != null) {
            android.util.Log.d("TEST", "intent - " + intent.toString());
            String title = AppHelper.getFriendlyActivityName(getContext(),
                    getContext().getPackageManager(), intent, false);
            android.util.Log.d("TEST", "title - " + title);
            if (!intentExists(getContext(), intent)) {
                setVisible(false);
                setTitle(title);
            } else {
                setTitle(title);
            }
        }
    }

    private boolean intentExists(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
        return (list.size() > 0);
    }
}
