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

package com.android.systemui.statusbar.slim;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Display;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.keyguard.KeyguardHostView.OnDismissAction;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.AutoReinflateContainer.InflateListener;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.slimrecent.RecentController;
import com.android.systemui.slimrecent.SlimScreenPinningRequest;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.SlimNotificationGuts;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.SlimNavigationBarFragment;
import com.android.systemui.statusbar.phone.SlimNavigationBarView;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.events.EventBus;

import java.util.List;

import slim.action.SlimActionsManager;
import slim.provider.SlimSettings;

import static com.android.systemui.statusbar.phone.BarTransitions.MODE_LIGHTS_OUT;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_LIGHTS_OUT_TRANSPARENT;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_OPAQUE;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_SEMI_TRANSPARENT;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_TRANSLUCENT;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_TRANSPARENT;
import static com.android.systemui.statusbar.phone.BarTransitions.MODE_WARNING;

public class SlimStatusBar extends StatusBar implements
        SlimCommandQueue.Callbacks {

    private static final String TAG = SlimStatusBar.class.getSimpleName();

    protected static final int MSG_TOGGLE_LAST_APP = 11001;
    protected static final int MSG_TOGGLE_KILL_APP = 11002;
    protected static final int MSG_TOGGLE_SCREENSHOT = 11003;

    private View mSlimNavigationBarView;
    private SlimNavigationBarFragment mSlimNavBar;
    private RecentController mSlimRecents;
    private Display mDisplay;
    private SlimCommandQueue mSlimCommandQueue;
    private Handler mHandler = new H();
    private BatteryController mBatteryController;

    private SlimScreenPinningRequest mSlimScreenPinningRequest;
    private LightBarController mLightBarController;

    private boolean mHasNavigationBar = false;
    private boolean mNavigationBarAttached = false;
    private boolean mDisableHomeLongpress = false;

    private int mDensity;
    private int mStatusBarMode;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.USE_SLIM_RECENTS), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.RECENT_CARD_BG_COLOR), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.RECENT_CARD_TEXT_COLOR), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT_MODE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_GLOW_TINT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_SHOW),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_CONFIG),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_CAN_MOVE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.MENU_LOCATION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.MENU_VISIBILITY),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_TIMEOUT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ALPHA),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_TOUCH_ANYWHERE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.USE_SLIM_RECENTS))) {
                updateRecents();
            } else if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.RECENT_CARD_BG_COLOR))
                    || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.RECENT_CARD_TEXT_COLOR))) {
                rebuildRecentsScreen();
            } else if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT_MODE))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_CONFIG))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_GLOW_TINT))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.MENU_LOCATION))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.MENU_VISIBILITY))) {
                if (mSlimNavigationBarView != null) {
                    mSlimNavBar.recreateNavigationBar();
                    mSlimNavBar.prepareNavigationBarView();
                }
            } else if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_CAN_MOVE))) {
                mSlimNavBar.prepareNavigationBarView();
            } else if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_TIMEOUT))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ALPHA))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION))
                || uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.DIM_NAV_BUTTONS_TOUCH_ANYWHERE))) {
                if (mSlimNavigationBarView != null) {
                    mSlimNavBar.updateNavigationBarSettings();
                    mSlimNavBar.onNavButtonTouched();
                }
            } else if (uri.equals(SlimSettings.System.getUriFor(
                    SlimSettings.System.NAVIGATION_BAR_SHOW))) {
                updateNavigationBarVisibility();
            }
        }
    }

    @Override
    public void start() {
        super.start();

        mDensity = mContext.getResources().getConfiguration().densityDpi;

        mDisplay = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        mBatteryController = Dependency.get(BatteryController.class);
        mLightBarController = new LightBarController();

        updateNavigationBarVisibility();
        updateRecents();

        SlimActionsManager slimActionsManager = SlimActionsManager.getInstance(mContext);

        mSlimCommandQueue = new SlimCommandQueue(this);
        slimActionsManager.registerSlimStatusBar(mSlimCommandQueue);

        SettingsObserver observer = new SettingsObserver(mHandler);
        observer.observe();
    }

    @Override
    protected void makeStatusBarView() {
        super.makeStatusBarView();

        //mStatusBarWindow.findViewById(R.id.battery).setVisibility(View.GONE);

        mSlimScreenPinningRequest = new SlimScreenPinningRequest(mContext);
    }

    /*@Override
    public void showScreenPinningRequest(int taskId, boolean allowCancel) {
        hideRecents(false, false);
        mSlimScreenPinningRequest.showPrompt(taskId, allowCancel);
    }*/

    /*@Override
    protected void createNavigationBarView(Context context) {
        if (mSlimNavigationBarView == null) {
            mSlimNavigationBarView = (SlimNavigationBarView)
                    View.inflate(mContext, R.layout.slim_navigation_bar, null);
        }
        mSlimNavigationBarView.setDisabledFlags(mDisabled1);
        //mSlimNavigationBarView.setBar(this);
        mSlimNavigationBarView.setOnVerticalChangedListener(
                new NavigationBarView.OnVerticalChangedListener() {
            @Override
            public void onVerticalChanged(boolean isVertical) {
                if (mAssistManager != null) {
                    mAssistManager.onConfigurationChanged();
                }
                mNotificationPanel.setQsScrimEnabled(!isVertical);
            }
        });
        mSlimNavigationBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                checkUserAutohide(v, event);
                return false;
            }
        });

        if (mNavigationBarView != mSlimNavigationBarView) {
            mNavigationBarView = mSlimNavigationBarView;
        }
    }*/

    private void updateNavigationBarVisibility() {
        final int showByDefault = mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_showNavigationBar) ? 1 : 0;
        mHasNavigationBar = SlimSettings.System.getIntForUser(mContext.getContentResolver(),
                    SlimSettings.System.NAVIGATION_BAR_SHOW, showByDefault,
                    UserHandle.USER_CURRENT) == 1;

        if (mHasNavigationBar) {
            createNavigationBar();
            //mSlimScreenPinningRequest.setSlimNavigationBarView(mSlimNavigationBarView);
        } else {
            if (mNavigationBarAttached) {
                mNavigationBarAttached = false;
                mWindowManager.removeViewImmediate(mSlimNavigationBarView);
                mSlimScreenPinningRequest.setSlimNavigationBarView(null);
                mSlimNavigationBarView = null;
            }
        }
    }

    @Override
    protected void createNavigationBar() {
        if (mSlimNavigationBarView == null && !mNavigationBarAttached) {
        mSlimNavigationBarView = SlimNavigationBarFragment.create(mContext, (tag, fragment) -> {
            mSlimNavBar = (SlimNavigationBarFragment) fragment;
            if (mLightBarController != null) {
                mSlimNavBar.setLightBarController(mLightBarController);
            }
            mSlimNavBar.setCurrentSysuiVisibility(mSystemUiVisibility);
            mNavigationBarAttached = true;
        });
        }
    }

    /*@Override
    protected void prepareNavigationBarView() {
        mSlimNavigationBarView.reorient();*/

        //View home = mSlimNavigationBarView.getHomeButton().getCurrentView();
        //View recents = mSlimNavigationBarView.getRecentsButton();

        //mSlimNavigationBarView.setPinningCallback(mLongClickCallback);

        /*if (recents != null) {
            recents.setOnClickListener(mRecentsClickListener);
            recents.setOnTouchListener(mRecentsPreloadOnTouchListener);
        }
        if (home != null) {
            home.setOnTouchListener(mHomeActionListener);
        }*/

        //mAssistManager.onConfigurationChanged();
    //}

    /*@Override
    protected void addNavigationBar() {
        if (DEBUG) Log.v(TAG, "addNavigationBar: about to add " + mSlimNavigationBarView);
        if (mSlimNavigationBarView == null) {
            createNavigationBarView(mContext);
        }

        prepareNavigationBarView();

        if (!mNavigationBarAttached) {
            mNavigationBarAttached = true;
            try {
                mWindowManager.addView(mSlimNavigationBarView, getNavigationBarLayoutParams());
            } catch (Exception e) {}
        }
    }*/

    /*@Override
    protected void repositionNavigationBar() {
        if (mSlimNavigationBarView == null
                || !mSlimNavigationBarView.isAttachedToWindow()) return;

        prepareNavigationBarView();

        mWindowManager.updateViewLayout(mSlimNavigationBarView, getNavigationBarLayoutParams());
    }*/

    private WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                    0
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        // this will allow the navbar to run in an overlay on devices that support this
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final int density = newConfig.densityDpi;
        if (density != mDensity) {
            mDensity = density;
            mSlimNavBar.recreateNavigationBar();
            rebuildRecentsScreen();
        }
    }

    private long mLastLockToAppLongPress;
    private SlimKeyButtonView.LongClickCallback mLongClickCallback =
            new SlimKeyButtonView.LongClickCallback() {
        @Override
        public boolean onLongClick(View v) {
            return handleLongPress(v);
        }
    };

    private boolean handleLongPress(View v) {
        try {
            boolean sendBackLongPress = false;
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            if (activityManager.isInLockTaskMode()) {
                activityManager.stopSystemLockTaskMode();
                // When exiting refresh disabled flags.
                //mSlimNavigationBarView.setDisabledFlags(mDisabled1, true);
                return true;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Unable to reach activity manager", e);
        }
        return false;
    }

    /*@Override
    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (mSlimRecents != null) {
            mSlimRecents.hideRecents(triggeredFromHomeKey);
        } else {
            super.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
        }
    }*/

    /*@Override
    protected void toggleSplitScreenMode(int metricsDockAction, int metricsUndockAction) {
        boolean isInLockTaskMode = false;
        try {
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            if (activityManager.isInLockTaskMode()) {
                isInLockTaskMode = true;
            }
        } catch (RemoteException e) {}
        if (mSlimRecents != null) {
            if (!isInLockTaskMode) {
                int dockSide = WindowManagerProxy.getInstance().getDockSide();
                if (dockSide == WindowManager.DOCKED_INVALID) {
                    mSlimRecents.startMultiWindow();
                    if (metricsDockAction != -1) {
                        MetricsLogger.action(mContext, metricsDockAction);
                    }
                } else {
                    EventBus.getDefault().send(new UndockingTaskEvent());
                    if (metricsUndockAction != -1) {
                        MetricsLogger.action(mContext, metricsUndockAction);
                    }
                }
            }
        } else {
            super.toggleSplitScreenMode(metricsDockAction, metricsUndockAction);
        }
    }*/

    /*@Override
    protected void toggleRecents() {
        if (mSlimRecents != null) {
            sendCloseSystemWindows(mContext, SYSTEM_DIALOG_REASON_RECENT_APPS);
            mSlimRecents.toggleRecents(mDisplay, mLayoutDirection, getStatusBarView());
        } else {
            super.toggleRecents();
        }
    }

    @Override
    protected void preloadRecents() {
        if (mSlimRecents != null) {
            mSlimRecents.preloadRecentTasksList();
        } else {
            super.preloadRecents();
        }
    }

    @Override
    protected void cancelPreloadingRecents() {
        if (mSlimRecents != null) {
            mSlimRecents.cancelPreloadingRecentTasksList();
        } else {
            super.cancelPreloadingRecents();
        }
    }*/

    protected void rebuildRecentsScreen() {
        if (mSlimRecents != null) {
            mSlimRecents.rebuildRecentsScreen();
        }
    }

    protected void updateRecents() {
        boolean slimRecents = SlimSettings.System.getIntForUser(mContext.getContentResolver(),
                SlimSettings.System.USE_SLIM_RECENTS, 1, UserHandle.USER_CURRENT) == 1;

        if (slimRecents) {
            mSlimRecents = new RecentController(mContext, mLayoutDirection);
            mRecents = null;
            //mSlimRecents.setCallback(this);
            rebuildRecentsScreen();
        } else {
            mRecents = getComponent(Recents.class);
            mSlimRecents = null;
        }
    }

    private static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // CommandQueue
    public void showCustomIntentAfterKeyguard(Intent intent) {
        startActivityDismissingKeyguard(intent, false, false);
    }

    @Override
    public void toggleLastApp() {
        int msg = MSG_TOGGLE_LAST_APP;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessage(msg);
    }

    @Override
    public void toggleKillApp() {
        int msg = MSG_TOGGLE_KILL_APP;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessage(msg);
    }

    @Override
    public void toggleScreenshot() {
        int msg = MSG_TOGGLE_SCREENSHOT;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessage(msg);
    }

    @Override
    public void toggleRecents() {
        if  (mSlimRecents != null) {
            sendCloseSystemWindows(mContext, SYSTEM_DIALOG_REASON_RECENT_APPS);
            mSlimRecents.toggleRecents(mDisplay, mLayoutDirection, getStatusBarView());
        } else {
            Recents recents = SysUiServiceProvider.getComponent(mContext, Recents.class);
            recents.toggleRecentApps();
        }
    }

    protected class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
             case MSG_TOGGLE_LAST_APP:
                  Slog.d(TAG, "toggle last app");
                  getLastApp();
                  break;
             case MSG_TOGGLE_KILL_APP:
                  Slog.d(TAG, "toggle kill app");
                  mHandler.post(mKillTask);
                  break;
             case MSG_TOGGLE_SCREENSHOT:
                  Slog.d(TAG, "toggle screenshot");
                  takeScreenshot();
                  break;
            }
        }
    }

    Runnable mKillTask = new Runnable() {
        public void run() {
            try {
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                String defaultHomePackage = "com.android.launcher";
                intent.addCategory(Intent.CATEGORY_HOME);
                final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
                if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
                    defaultHomePackage = res.activityInfo.packageName;
                }
                boolean targetKilled = false;
                IActivityManager am = ActivityManagerNative.getDefault();
                List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
                for (RunningAppProcessInfo appInfo : apps) {
                    int uid = appInfo.uid;
                    // Make sure it's a foreground user application (not system,
                    // root, phone, etc.)
                    if (uid >= Process.FIRST_APPLICATION_UID && uid <= Process.LAST_APPLICATION_UID
                            && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if (appInfo.pkgList != null && (appInfo.pkgList.length > 0)) {
                            for (String pkg : appInfo.pkgList) {
                                if (!pkg.equals("com.android.systemui")
                                        && !pkg.equals(defaultHomePackage)) {
                                    am.forceStopPackage(pkg, UserHandle.USER_CURRENT);
                                    targetKilled = true;
                                    break;
                                }
                            }
                        } else {
                            Process.killProcess(appInfo.pid);
                            targetKilled = true;
                        }
                    }
                    if (targetKilled) {
                        Toast.makeText(mContext,
                                R.string.app_killed_message, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            } catch (RemoteException e) {}
        }
    };

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override
        public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    private final Object mScreenshotLock = new Object();
    private ServiceConnection mScreenshotConnection = null;
    private Handler mHDL = new Handler();

    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHDL.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHDL.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        /*
                         * remove for the time being if (mStatusBar != null &&
                         * mStatusBar.isVisibleLw()) msg.arg1 = 1; if
                         * (mNavigationBar != null &&
                         * mNavigationBar.isVisibleLw()) msg.arg2 = 1;
                         */

                        /* wait for the dialog box to close */
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }

                        /* take the screenshot */
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            if (mContext.bindService(intent, conn, mContext.BIND_AUTO_CREATE)) {
                mScreenshotConnection = conn;
                mHDL.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    private void getLastApp() {
        int lastAppId = 0;
        int looper = 1;
        String packageName;
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Activity.ACTIVITY_SERVICE);
        String defaultHomePackage = "com.android.launcher";
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = mContext.getPackageManager().resolveActivity(intent, 0);
        if (res.activityInfo != null && !res.activityInfo.packageName.equals("android")) {
            defaultHomePackage = res.activityInfo.packageName;
        }
        List <ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(5);
        // lets get enough tasks to find something to switch to
        // Note, we'll only get as many as the system currently has - up to 5
        while ((lastAppId == 0) && (looper < tasks.size())) {
            packageName = tasks.get(looper).topActivity.getPackageName();
            if (!packageName.equals(defaultHomePackage)
                    && !packageName.equals("com.android.systemui")) {
                lastAppId = tasks.get(looper).id;
            }
            looper++;
        }
        if (lastAppId != 0) {
            am.moveTaskToFront(lastAppId, am.MOVE_TASK_NO_USER_ACTION);
        }
    }

    /*@Override
    protected void bindGuts(final ExpandableNotificationRow row) {
        row.inflateGuts();
        final StatusBarNotification sbn = row.getStatusBarNotification();
        PackageManager pmUser = getPackageManagerForUser(mContext, sbn.getUser().getIdentifier());
        row.setTag(sbn.getPackageName());
        final SlimNotificationGuts guts = (SlimNotificationGuts) row.getGuts();
        guts.setClosedListener(this);
        final String pkg = sbn.getPackageName();
        String appname = pkg;
        Drawable pkgicon = null;
        int appUid = -1;
        try {
            final ApplicationInfo info = pmUser.getApplicationInfo(pkg,
                    PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS);
            if (info != null) {
                appname = String.valueOf(pmUser.getApplicationLabel(info));
                pkgicon = pmUser.getApplicationIcon(info);
                appUid = info.uid;
            }
        } catch (NameNotFoundException e) {
            // app is gone, just show package name and generic icon
            pkgicon = pmUser.getDefaultActivityIcon();
        }

        ((ImageView) guts.findViewById(R.id.app_icon)).setImageDrawable(pkgicon);
        ((TextView) guts.findViewById(R.id.pkgname)).setText(appname);

        final TextView settingsButton = (TextView) guts.findViewById(R.id.more_settings);
        LinearLayout buttonParent = (LinearLayout) settingsButton.getParent();
        final TextView killButton = (TextView) LayoutInflater.from(
                settingsButton.getContext()).inflate(
                R.layout.kill_button, buttonParent, false);
        if (buttonParent.findViewById(R.id.notification_inspect_kill) == null) { // only add once
            buttonParent.addView(killButton, buttonParent.indexOfChild(settingsButton));
        }
        if (appUid >= 0) {
            final int appUidF = appUid;
            settingsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MetricsLogger.action(mContext, MetricsEvent.ACTION_NOTE_INFO);
                    guts.resetFalsingCheck();
                    startAppNotificationSettingsActivity(pkg, appUidF);
                }
            });
            settingsButton.setText(R.string.notification_more_settings);
            if (isThisASystemPackage(pkg, pmUser)) {
                killButton.setVisibility(View.GONE);
            } else {
                killButton.setVisibility(View.VISIBLE);
                killButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final SystemUIDialog killDialog = new SystemUIDialog(mContext);
                        killDialog.setTitle(mContext.getText(R.string.force_stop_dlg_title));
                        killDialog.setMessage(mContext.getText(R.string.force_stop_dlg_text));
                        killDialog.setPositiveButton(
                                R.string.dlg_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // kill pkg
                                ActivityManager actMan =
                                        (ActivityManager) mContext.getSystemService(
                                        Context.ACTIVITY_SERVICE);
                                actMan.forceStopPackage(pkg);
                            }
                        });
                        killDialog.setNegativeButton(R.string.dlg_cancel, null);
                        killDialog.show();
                    }
                });
                killButton.setText(R.string.kill);
            }
        } else {
            settingsButton.setVisibility(View.GONE);
            killButton.setVisibility(View.GONE);
        }

        guts.bindGutsImportance(pmUser, sbn, mNonBlockablePkgs,
                mNotificationData.getImportance(sbn.getKey()));

        final TextView doneButton = (TextView) guts.findViewById(R.id.done);
        doneButton.setText(R.string.notification_done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the user has security enabled, show challenge if the setting is changed.
                if (guts.hasImportanceChanged() && isLockscreenPublicMode() &&
                        (mState == StatusBarState.KEYGUARD
                        || mState == StatusBarState.SHADE_LOCKED)) {
                    OnDismissAction dismissAction = new OnDismissAction() {
                        @Override
                        public boolean onDismiss() {
                            saveImportanceCloseControls(sbn, row, guts, v);
                            return true;
                        }
                    };
                    onLockedNotificationImportanceChange(dismissAction);
                } else {
                    saveImportanceCloseControls(sbn, row, guts, v);
                }
            }
        });
    }*/

    private boolean isThisASystemPackage(String packageName, PackageManager pm) {
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return (packageInfo != null && packageInfo.signatures != null &&
                    sys.signatures[0].equals(packageInfo.signatures[0]));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
