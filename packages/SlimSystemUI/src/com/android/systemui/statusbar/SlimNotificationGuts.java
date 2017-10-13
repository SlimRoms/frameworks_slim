/*
 * Copyright (C) 2017 SlimRoms
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;

import com.android.systemui.statusbar.NotificationGuts;

import java.util.Set;

/**
 * The guts of a notification revealed when performing a long press.
 */
public class SlimNotificationGuts extends NotificationGuts {

    public SlimNotificationGuts(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void bindGutsImportance(final PackageManager pm, final StatusBarNotification sbn,
            final Set<String> nonBlockablePkgs, final int importance) {
        //bindImportance(pm, sbn, nonBlockablePkgs, importance);
    }

}
