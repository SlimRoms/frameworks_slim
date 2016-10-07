/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ViewGroup;

import com.slim.settings.R;

/**
 * Stub class for showing sub-settings; we can't use the main Settings class
 * since for our app it is a special singleTask class.
 */
public class SubSettings extends SettingsActivity {

    public static final String EXTRA_SHOW_FRAGMENT = ":slim:settings:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":slim:settings:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":slim:settings:show_fragment_title";
    public static final String EXTRA_SHOW_FRAGMENT_AS_SHORTCUT =
            ":slim:settings:show_fragment_as_shortcut";

    public static final String BACK_STACK_PREFS = ":slim:settings:prefs";

    private ViewGroup mContent;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.prefs);

        mContent = (ViewGroup) findViewById(R.id.main_content);

        final Intent intent = getIntent();

        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        CharSequence initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
        initialTitle = (initialTitle != null) ? initialTitle : getTitle();
        setTitle(initialTitle);

        switchToFragment(initialFragmentName, initialArguments, true, false, initialTitle, false);
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, CharSequence title, boolean withTransition) {
        /*if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }*/
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(mContent);
        }
        if (addToBackStack) {
            transaction.addToBackStack(BACK_STACK_PREFS);
        }
        if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
