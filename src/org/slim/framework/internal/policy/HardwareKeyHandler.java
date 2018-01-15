/*
 * Copyright (C) 2016-2018 SlimRoms Project
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

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.VibrationEffect;
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
import java.util.HashMap;

import slim.action.Action;
import slim.action.ActionConstants;
import slim.action.SlimActionsManager;
import slim.provider.SlimSettings;
import slim.utils.HwKeyHelper;

public class HardwareKeyHandler {

    private static final String TAG = "HardwareKeyHandler";

    private static final int[] SUPPORTED_KEYS = {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_APP_SWITCH
    };

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build();

    private HashMap<Integer, HardwareButton> mButtons = new HashMap<>();

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

    private HwKeySettingsObserver mHwKeySettingsObserver;

    private class HwKeySettingsObserver extends ContentObserver {
        HwKeySettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            for (HardwareButton button : mButtons.values()) {
                button.observe(this, resolver);
                button.updateAssignments();
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            for (HardwareButton button : mButtons.values()) {
                button.updateAssignments();
            }
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

        for (int keyCode : SUPPORTED_KEYS) {
            mButtons.put(keyCode, new HardwareButton(keyCode));
        }

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

        HardwareButton button = mButtons.get(keyCode);
        if (button != null)
            return button.handleKeyEvent(repeatCount, down, canceled, longpress, keyguardOn);

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

    private void finishLockTask() {
        try {
            ActivityManagerNative.getDefault().stopSystemLockTaskMode();
        } catch (Exception e) {
        }
    }

    private boolean isInLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (Exception e) {
        }
        return false;
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
            mVibrator.vibrate(owningUid, owningPackage,
                    VibrationEffect.createOneShot(pattern[0],
                    VibrationEffect.DEFAULT_AMPLITUDE), VIBRATION_ATTRIBUTES);
        } else {
            // Pattern vibration
            mVibrator.vibrate(owningUid, owningPackage,
                    VibrationEffect.createWaveform(pattern, -1),
                    VIBRATION_ATTRIBUTES);
        }
        return true;
    }

    private String getStringFromSettings(String key, String def) {
        String val = SlimSettings.System.getStringForUser(
                mContext.getContentResolver(), key, UserHandle.USER_CURRENT);
        return (val == null) ? def : val;
    }

    private class HardwareButton {

        private String mKey;
        private int mKeyCode;

        private boolean mButtonPressed;
        private boolean mButtonConsumed;
        private boolean mDoubleTapPending;

        private String mTapAction;
        private String mDoubleTapAction;
        private String mLongPressAction;

        private final Runnable mDoubleTapTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDoubleTapPending) {
                    mDoubleTapPending = false;
                    if (!mTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                        cancelPreloadRecentApps();
                    }
                    //mDisableVibration = maybeDisableVibration(mPressOnHomeBehavior);
                    Action.processAction(mContext, mTapAction, false);
                }
            }
        };

        private HardwareButton(int keyCode) {
            mKeyCode = keyCode;
            String key = KeyEvent.keyCodeToString(keyCode);
            mKey = key.replace("KEYCODE_", "key_").toLowerCase();
        }

        private void observe(ContentObserver observer, ContentResolver resolver) {
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    (mKey + "_action")), false, observer,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    (mKey + "_long_press_action")), false, observer,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    (mKey + "_double_tap_action")), false, observer,
                    UserHandle.USER_ALL);
        }

        private void updateAssignments() {
            mTapAction = getStringFromSettings((mKey + "_action"),
                    HwKeyHelper.getDefaultTapActionForKeyCode(mContext, mKeyCode));
            mDoubleTapAction = getStringFromSettings((mKey + "_double_tap_action"),
                    HwKeyHelper.getDefaultDoubleTapActionForKeyCode(mContext, mKeyCode));
            mLongPressAction = getStringFromSettings((mKey + "_long_press_action"),
                    HwKeyHelper.getDefaultLongPressActionForKeyCode(mContext, mKeyCode));
        }

        public boolean handleKeyEvent(int repeatCount, boolean down,
                boolean canceled, boolean longpress, boolean keyguardOn) {
            // If we have released the assistant key, and didn't do anything else
            // while it was pressed, then it is time to process the assistant action!
            if (!down && mButtonPressed) {
                mButtonPressed = false;
                if (mButtonConsumed) {
                    mButtonConsumed = false;
                    return true;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring " + mKey + ", event canceled.");
                    return true;
                }

                // Delay handling assistant if a double-tap is possible.
                if (!mDoubleTapAction.equals(ActionConstants.ACTION_NULL)) {
                    mHandler.removeCallbacks(mDoubleTapTimeoutRunnable); // just in case
                    mDisableVibration = false; // just in case
                    mDoubleTapPending = true;
                    mHandler.postDelayed(mDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return true;
                }

                if (mKeyCode == KeyEvent.KEYCODE_HOME &&
                        mDreamManagerInternal != null && mDreamManagerInternal.isDreaming()) {
                    mDreamManagerInternal.stopDream(false);
                    return true;
                }

                if (!mTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                    cancelPreloadRecentApps();
                }
                //mDisableVibration = maybeDisableVibration(mPressOnCameraBehavior);
                Action.processAction(mContext, mTapAction, false);
                return true;
            }

            // Remember that camera key is pressed and handle special actions.
            if (down) {
                if (!mPreloadedRecentApps &&
                        (mLongPressAction.equals(ActionConstants.ACTION_RECENTS)
                         || mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)
                         || mTapAction.equals(ActionConstants.ACTION_RECENTS))) {
                    preloadRecentApps();
                }
                if (repeatCount == 0) {
                    mButtonPressed = true;
                    if (mDoubleTapPending) {
                        mDoubleTapPending = false;
                        mDisableVibration = false;
                        mButtonConsumed = true;
                        mHandler.removeCallbacks(mDoubleTapTimeoutRunnable);
                        if (!mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        Action.processAction(mContext, mDoubleTapAction, false);
                    }
                } else if (longpress) {
                    if (!keyguardOn
                            && !mLongPressAction.equals(ActionConstants.ACTION_NULL)) {
                        if (!mLongPressAction.equals(ActionConstants.ACTION_RECENTS)) {
                            cancelPreloadRecentApps();
                        }
                        performHapticFeedback(null, HapticFeedbackConstants.LONG_PRESS, false);
                        Action.processAction(mContext, mLongPressAction, false);
                        mButtonConsumed = true;
                    }
                }
            }
            return true;
        }
    }
}
