/*
 * Copyright (C) 2016 SlimRoms Project
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
package org.slim.framework.internal.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.util.Log;
import android.util.Slog;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManagerPolicy.WindowState;

import java.util.Arrays;

import org.slim.action.Action;
import org.slim.action.ActionConstants;
import org.slim.action.SlimActionsManager;
import org.slim.provider.SlimSettings;
import org.slim.utils.HwKeyHelper;

public class HardwareKeyHandler {

    private static final String TAG = "HardwareKeyHandler";

    /**
     * Masks for checking presence of hardware keys.
     * Must match values in core/res/res/values/config.xml
     */
    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_ASSIST = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;
    private static final int KEY_MASK_CAMERA = 0x20;

    private static final int[] SUPPORTED_KEYS = {
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_APP_SWITCH };

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build();

    boolean mHomePressed;
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    boolean mMenuPressed;
    boolean mMenuConsumed;
    boolean mMenuDoubleTapPending;
    boolean mBackPressed;
    boolean mBackConsumed;
    boolean mBackDoubleTapPending;
    boolean mAppSwitchPressed;
    boolean mAppSwitchConsumed;
    boolean mAppSwitchDoubleTapPending;
    boolean mAssistPressed;
    boolean mAssistConsumed;
    boolean mAssistDoubleTapPending;
    boolean mCameraPressed;
    boolean mCameraConsumed;
    boolean mCameraDoubleTapPending;

    // Custom hardware key rebinding
    private int mDeviceHardwareKeys;
    private boolean mKeysDisabled;
    private boolean mDisableVibration;
    private boolean mPreloadedRecentApps;

    private Context mContext;
    private Handler mHandler;
    private DreamManagerInternal mDreamManagerInternal;
    private Vibrator mVibrator;

    private long[] mLongPressVibePattern;
    private long[] mVirtualKeyVibePattern;

    // Tracks user-customisable behavior for certain key events
    private String mPressOnHomeBehavior          = ActionConstants.ACTION_NULL;
    private String mLongPressOnHomeBehavior      = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnHomeBehavior      = ActionConstants.ACTION_NULL;
    private String mPressOnMenuBehavior          = ActionConstants.ACTION_NULL;
    private String mLongPressOnMenuBehavior      = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnMenuBehavior      = ActionConstants.ACTION_NULL;
    private String mPressOnBackBehavior          = ActionConstants.ACTION_NULL;
    private String mLongPressOnBackBehavior      = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnBackBehavior      = ActionConstants.ACTION_NULL;
    private String mPressOnAssistBehavior        = ActionConstants.ACTION_NULL;
    private String mLongPressOnAssistBehavior    = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnAssistBehavior    = ActionConstants.ACTION_NULL;
    private String mPressOnAppSwitchBehavior     = ActionConstants.ACTION_NULL;
    private String mLongPressOnAppSwitchBehavior = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnAppSwitchBehavior = ActionConstants.ACTION_NULL;
    private String mPressOnCameraBehavior        = ActionConstants.ACTION_NULL;
    private String mLongPressOnCameraBehavior    = ActionConstants.ACTION_NULL;
    private String mDoubleTapOnCameraBehavior    = ActionConstants.ACTION_NULL;

    private HwKeySettingsObserver mHwKeySettingsObserver;

    private final Runnable mDoubleTapTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mHomeDoubleTapPending) {
                mHomeDoubleTapPending = false;
                if (!mPressOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnHomeBehavior);
                Action.processAction(mContext, mPressOnHomeBehavior, false);
            }
            if (mMenuDoubleTapPending) {
                mMenuDoubleTapPending = false;
                if (!mPressOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnMenuBehavior);
                Action.processAction(mContext, mPressOnMenuBehavior, false);
            }
            if (mBackDoubleTapPending) {
                mBackDoubleTapPending = false;
                if (!mPressOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnBackBehavior);
                Action.processAction(mContext, mPressOnBackBehavior, false);
            }
            if (mAppSwitchDoubleTapPending) {
                mAppSwitchDoubleTapPending = false;
                if (!mPressOnAppSwitchBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnAppSwitchBehavior);
                Action.processAction(mContext, mPressOnAppSwitchBehavior, false);
            }
            if (mAssistDoubleTapPending) {
                mAssistDoubleTapPending = false;
                if (!mPressOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnAssistBehavior);
                Action.processAction(mContext, mPressOnAssistBehavior, false);
            }
            if (mCameraDoubleTapPending) {
                mCameraDoubleTapPending = false;
                if (!mPressOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnCameraBehavior);
                Action.processAction(mContext, mPressOnCameraBehavior, false);
            }
        }
    };

    private class HwKeySettingsObserver extends ContentObserver {
        HwKeySettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            // Observe all hw key users' changes
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.DISABLE_HW_KEYS), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_HOME_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_HOME_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_HOME_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_MENU_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_MENU_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_MENU_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_ASSIST_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_ASSIST_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_ASSIST_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_APP_SWITCH_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_BACK_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_BACK_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_BACK_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_CAMERA_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_CAMERA_LONG_PRESS_ACTION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.KEY_CAMERA_DOUBLE_TAP_ACTION), false, this,
                    UserHandle.USER_ALL);
            updateKeyAssignments();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateKeyAssignments();
        }
    }

    public HardwareKeyHandler(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        mDeviceHardwareKeys = mContext.getResources().getInteger(
                org.slim.framework.internal.R.integer.config_deviceHardwareKeys);

        mLongPressVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_longPressVibePattern);
        mVirtualKeyVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_virtualKeyVibePattern);

        mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

        mHwKeySettingsObserver = new HwKeySettingsObserver(mHandler);
        mHwKeySettingsObserver.observe();
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i=0; i<ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    private void updateKeyAssignments() {
        final boolean noMenu = (mDeviceHardwareKeys & KEY_MASK_MENU) == 0;
        final boolean noBack = (mDeviceHardwareKeys & KEY_MASK_BACK) == 0;
        final boolean noHome = (mDeviceHardwareKeys & KEY_MASK_HOME) == 0;
        final boolean noAssist = (mDeviceHardwareKeys & KEY_MASK_ASSIST) == 0;
        final boolean noAppSwitch = (mDeviceHardwareKeys & KEY_MASK_APP_SWITCH) == 0;
        final boolean noCamera = (mDeviceHardwareKeys & KEY_MASK_CAMERA) == 0;

        mKeysDisabled = SlimSettings.System.getIntForUser(
                mContext.getContentResolver(),
                SlimSettings.System.DISABLE_HW_KEYS, 0,
                UserHandle.USER_CURRENT) == 1;

        // Home button
        mPressOnHomeBehavior =
                HwKeyHelper.getPressOnHomeBehavior(
                        mContext, noHome);
        mLongPressOnHomeBehavior =
                HwKeyHelper.getLongPressOnHomeBehavior(
                        mContext, noHome);
        mDoubleTapOnHomeBehavior =
                HwKeyHelper.getDoubleTapOnHomeBehavior(
                        mContext, noHome);

        // Menu button
        mPressOnMenuBehavior =
                HwKeyHelper.getPressOnMenuBehavior(
                        mContext, noMenu);
        mLongPressOnMenuBehavior =
                HwKeyHelper.getLongPressOnMenuBehavior(mContext,
                        noMenu, noMenu || !noAssist);
        mDoubleTapOnMenuBehavior =
                HwKeyHelper.getDoubleTapOnMenuBehavior(
                        mContext, noMenu);

        // Back button
        mPressOnBackBehavior =
                HwKeyHelper.getPressOnBackBehavior(
                        mContext, noBack);
        mLongPressOnBackBehavior =
                HwKeyHelper.getLongPressOnBackBehavior(
                        mContext, noBack);
        mDoubleTapOnBackBehavior =
                HwKeyHelper.getDoubleTapOnBackBehavior(
                        mContext, noBack);

        // Assist button
        mPressOnAssistBehavior =
                HwKeyHelper.getPressOnAssistBehavior(
                        mContext, noAssist);
        mLongPressOnAssistBehavior =
                HwKeyHelper.getLongPressOnAssistBehavior(
                        mContext, noAssist);
        mDoubleTapOnAssistBehavior =
                HwKeyHelper.getDoubleTapOnAssistBehavior(
                        mContext, noAssist);

        // App switcher button
        mPressOnAppSwitchBehavior =
                HwKeyHelper.getPressOnAppSwitchBehavior(
                        mContext, noAppSwitch);
        mLongPressOnAppSwitchBehavior =
                HwKeyHelper.getLongPressOnAppSwitchBehavior(
                        mContext, noAppSwitch);
        mDoubleTapOnAppSwitchBehavior =
                HwKeyHelper.getDoubleTapOnAppSwitchBehavior(
                        mContext, noAppSwitch);

        // Camera button
        mPressOnCameraBehavior =
                HwKeyHelper.getPressOnCameraBehavior(
                        mContext, noCamera);
        mLongPressOnCameraBehavior =
                HwKeyHelper.getLongPressOnCameraBehavior(
                        mContext, noCamera);
        mDoubleTapOnCameraBehavior =
                HwKeyHelper.getDoubleTapOnCameraBehavior(
                        mContext, noCamera);
    }

    public boolean isHwKeysDisabled() {
        return mKeysDisabled;
    }

    private boolean isKeyDisabled(int keyCode) {
        if (mKeysDisabled) {
            for (int i = 0; i < SUPPORTED_KEYS.length; i++) {
                if (SUPPORTED_KEYS[i] == keyCode) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleKeyEvent(int keyCode, int repeatCount, boolean down,
            boolean canceled, boolean longpress, boolean keyguardOn) {

        if (isKeyDisabled(keyCode)) {
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (!down && mHomePressed) {
                mHomePressed = false;
                if (mHomeConsumed) {
                    mHomeConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring HOME, event canceled.");
                    return true;
                }

                if (!mDoubleTapOnHomeBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                    mHomeDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (mDreamManagerInternal != null && mDreamManagerInternal.isDreaming()) {
                    mDreamManagerInternal.stopDream(false);
                    return true;
                }

                if (!mPressOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                Action.processAction(mContext, mPressOnHomeBehavior, false);
                return true;
            }

            if (down) {
                if (!mPreloadedRecentApps &&
                        (mLongPressOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)
                        || mDoubleTapOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)
                        || mPressOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mHomePressed = true;
                    if (mHomeDoubleTapPending) {
                        mHomeDoubleTapPending = false;
                        mDisableVibration = false;
                        mHomeConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnHomeBehavior, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnHomeBehavior.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnHomeBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        mHomePressed = true;
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnHomeBehavior, false);
                        mHomeConsumed = true;
                    }
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            // If we have released the menu key, and didn't do anything else
            // while it was pressed, then it is time to process the menu action!
            if (!down && mMenuPressed) {
                mMenuPressed = false;
                if (mMenuConsumed) {
                    mMenuConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring MENU, event canceled.");
                    return true;
                }

                // Delay handling menu if a double-tap is possible.
                if (!mDoubleTapOnMenuBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mMenuDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (!mPressOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnMenuBehavior);
                Action.processAction(mContext, mPressOnMenuBehavior, false);
                return true;
            }

            if (down) {
                // Remember that menu is pressed and handle special actions.
                if (!mPreloadedRecentApps &&
                        (mLongPressOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mPressOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mMenuPressed = true;
                    if (mMenuDoubleTapPending) {
                        mMenuDoubleTapPending = false;
                        mDisableVibration = false;
                        mMenuConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnMenuBehavior, false);
                        return true;
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnMenuBehavior.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnMenuBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnMenuBehavior, false);
                        mMenuConsumed = true;
                        return true;
                    }
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            // If we have released the back key, and didn't do anything else
            // while it was pressed, then it is time to process the back action!
            if (!down && mBackPressed) {
                mBackPressed = false;
                if (mBackConsumed) {
                    mBackConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring BACK, event canceled.");
                    return true;
                }

                // Delay handling back if a double-tap is possible.
                if (!mDoubleTapOnBackBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mBackDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (!mPressOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnBackBehavior);
                Action.processAction(mContext, mPressOnBackBehavior, false);
                return true;
            }

            if (down) {
                // Remember that back is pressed and handle special actions.
                if (!mPreloadedRecentApps &&
                        (mLongPressOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mPressOnBackBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mBackPressed = true;
                    if (mBackDoubleTapPending) {
                        mBackDoubleTapPending = false;
                        mDisableVibration = false;
                        mBackConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnBackBehavior, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnBackBehavior.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnBackBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnBackBehavior, false);
                        mBackConsumed = true;
                    }
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ASSIST) {
            // If we have released the assistant key, and didn't do anything else
            // while it was pressed, then it is time to process the assistant action!
            if (!down && mAssistPressed) {
                mAssistPressed = false;
                if (mAssistConsumed) {
                    mAssistConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring ASSIST, event canceled.");
                    return true;
                }

                // Delay handling assistant if a double-tap is possible.
                if (!mDoubleTapOnAssistBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mAssistDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (!mPressOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnAssistBehavior);
                Action.processAction(mContext, mPressOnAssistBehavior, false);
                return true;
            }

            // Remember that assistant key is pressed and handle special actions.
            if (down) {
                if (!mPreloadedRecentApps &&
                        (mLongPressOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mPressOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mAssistPressed = true;
                    if (mAssistDoubleTapPending) {
                        mAssistDoubleTapPending = false;
                        mDisableVibration = false;
                        mAssistConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnAssistBehavior, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnAssistBehavior.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnAssistBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnAssistBehavior, false);
                        mAssistConsumed = true;
                    }
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // If we have released the assistant key, and didn't do anything else
            // while it was pressed, then it is time to process the assistant action!
            if (!down && mCameraPressed) {
                mCameraPressed = false;
                if (mCameraConsumed) {
                    mCameraConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring CAMERA, event canceled.");
                    return true;
                }

                // Delay handling assistant if a double-tap is possible.
                if (!mDoubleTapOnCameraBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mCameraDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (!mPressOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnCameraBehavior);
                Action.processAction(mContext, mPressOnCameraBehavior, false);
                return true;
            }

            // Remember that camera key is pressed and handle special actions.
            if (down) {
                if (!mPreloadedRecentApps &&
                        (mLongPressOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mPressOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mCameraPressed = true;
                    if (mCameraDoubleTapPending) {
                        mCameraDoubleTapPending = false;
                        mDisableVibration = false;
                        mCameraConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnCameraBehavior, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnCameraBehavior.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnCameraBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnCameraBehavior, false);
                        mCameraConsumed = true;
                    }
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            // If we have released the app switch key, and didn't do anything else
            // while it was pressed, then it is time to process the app switch action!
            if (!down && mAppSwitchPressed) {
                mAppSwitchPressed = false;
                if (mAppSwitchConsumed) {
                    mAppSwitchConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring APPSWITCH, event canceled.");
                    return true;
                }

                // Delay handling AppSwitch if a double-tap is possible.
                if (!mDoubleTapOnAppSwitchBehavior.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mAppSwitchDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (!mPressOnAppSwitchBehavior.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnAppSwitchBehavior);
                Action.processAction(mContext, mPressOnAppSwitchBehavior, false);
                return true;
            }

            // Remember that AppSwitch is pressed and handle special actions.
            if (down) {
                if (!mPreloadedRecentApps &&
                        (mLongPressOnAppSwitchBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapOnAppSwitchBehavior.equals(ActionConstants.ACTION_RECENTS)
                         || mPressOnAppSwitchBehavior.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mAppSwitchPressed = true;
                    if (mAppSwitchDoubleTapPending) {
                        mAppSwitchDoubleTapPending = false;
                        mDisableVibration = false;
                        mAppSwitchConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapOnAppSwitchBehavior.equals(
                                ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapOnAppSwitchBehavior, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressOnAppSwitchBehavior.equals(
                                    ActionConstants.ACTION_NULL)) {
                        if (!mLongPressOnAppSwitchBehavior.equals(
                                ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressOnAppSwitchBehavior, false);
                        mAppSwitchConsumed = true;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void preloadRecentApps() {
        mPreloadedRecentApps = true;
        SlimActionsManager actionsManager = SlimActionsManager.getInstance(mContext);
        if (actionsManager != null) {
            actionsManager.preloadRecentApps();
        }
    }

    private void cancelPreloadRecentApps() {
        if (mPreloadedRecentApps) {
            mPreloadedRecentApps = false;
            SlimActionsManager actionsManager = SlimActionsManager.getInstance(mContext);
            if (actionsManager != null) {
                actionsManager.cancelPreloadRecentApps();
            }
        }
    }

    private boolean performHapticFeedback(WindowState win, int effectId, boolean always) {
       if (mDisableVibration) {
            mDisableVibration = false;
            return false;
        }
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return false;
        }
        final boolean hapticsDisabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0, UserHandle.USER_CURRENT) == 0;
        if (hapticsDisabled && !always) {
            return false;
        }
        long[] pattern = null;
        switch (effectId) {
            case HapticFeedbackConstants.LONG_PRESS:
                pattern = mLongPressVibePattern;
                break;
            case HapticFeedbackConstants.VIRTUAL_KEY:
                pattern = mVirtualKeyVibePattern;
                break;
            default:
                return false;
        }
        int owningUid;
        String owningPackage;
        if (win != null) {
            owningUid = win.getOwningUid();
            owningPackage = win.getOwningPackage();
        } else {
            owningUid = android.os.Process.myUid();
            owningPackage = mContext.getOpPackageName();
        }
        if (pattern.length == 1) {
            // One-shot vibration
            mVibrator.vibrate(owningUid, owningPackage, pattern[0], VIBRATION_ATTRIBUTES);
        } else {
            // Pattern vibration
            mVibrator.vibrate(owningUid, owningPackage, pattern, -1, VIBRATION_ATTRIBUTES);
        }
        return true;
    }
}
