/*
 * Copyright (C) 2016-2017 The SlimRoms Project
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

package org.slim.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.WindowManagerPolicy;

import com.android.server.LocalServices;

import slim.action.ISlimActionsService;
import slim.constants.SlimServiceConstants;
import org.slim.framework.internal.statusbar.ISlimStatusBar;

public class SlimActionsService extends SlimSystemService {
    private static final String TAG = "SlimActionsService";

    private final Context mContext;

    private WindowManagerPolicy mPolicy;

    private final IBinder mService = new ISlimActionsService.Stub() {
        private ISlimStatusBar mBar;

        @Override
        public void registerSlimStatusBar(ISlimStatusBar bar) {
            enforceSlimActionsService();
            Slog.i(TAG, "registerSlimStatusBar bar=" + bar);
            mBar = bar;
        }

        /**
         * Ask keyguard to invoke a custom intent after dismissing keyguard
         * @hide
         */
        @Override
        public void showCustomIntentAfterKeyguard(Intent intent) {
            enforceSlimActionsService();
            if (mBar != null) {
                try {
                    mBar.showCustomIntentAfterKeyguard(intent);
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleSplitScreen() {
            if (mBar != null) {
                try {
                    mBar.toggleSplitScreen();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleScreenshot() {
            if (mBar != null) {
                try {
                    mBar.toggleScreenshot();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleLastApp() {
            if (mBar != null) {
                try {
                    mBar.toggleLastApp();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleKillApp() {
            if (mBar != null) {
                try {
                    mBar.toggleKillApp();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleRecentApps() {
            if (mBar != null) {
                try {
                    mBar.toggleRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void preloadRecentApps() {
            if (mBar != null) {
                try {
                    mBar.preloadRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void cancelPreloadRecentApps() {
            if (mBar != null) {
                try {
                    mBar.cancelPreloadRecentApps();
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void startAssist(Bundle args) {
            if (mBar != null) {
                try {
                    mBar.startAssist(args);
                } catch (RemoteException ex) {}
            }
        }

        @Override
        public void toggleGlobalMenu() {
            if (mPolicy != null) {
                mPolicy.showGlobalActions();
            }
        }
    };

    public SlimActionsService(Context context) {
        super(context);
        mContext = context;

        mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    }

    private void enforceSlimActionsService() {
       // mContext.enforceCallingOrSelfPermission(
         //       slim.Manifest.permission.SLIM_ACTIONS_SERVICE,
           //     "SlimActionsService");
    }

    @Override
    public void onStart() {
        publishBinderService(SlimServiceConstants.SLIM_ACTIONS_SERVICE, mService);
    }
}
