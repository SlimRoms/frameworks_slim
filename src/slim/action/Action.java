/*
* Copyright (C) 2016-2018 SlimRoms Project
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

package slim.action;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.statusbar.IStatusBarService;

import java.net.URISyntaxException;

public class Action {

    private static final int MSG_INJECT_KEY_DOWN = 1066;
    private static final int MSG_INJECT_KEY_UP = 1067;

    private static Context mContext;

    private static boolean sTorchEnabled = false;

    public static void processAction(Context context, String action, boolean isLongpress) {
        processActionWithOptions(context, action, isLongpress, true);
    }

    public static void processActionWithOptions(Context context,
            String action, boolean isLongpress, boolean collapseShade) {

            mContext = context;

            if (action == null || action.equals(ActionConstants.ACTION_NULL)) {
                return;
            }

            boolean isKeyguardShowing = false;
            try {
                isKeyguardShowing =
                        WindowManagerGlobal.getWindowManagerService().isKeyguardLocked();
            } catch (RemoteException e) {
                Log.w("Action", "Error getting window manager service", e);
            }

            IStatusBarService barService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            if (barService == null) {
                return; // ouch
            }

            SlimActionsManager actionsManager = SlimActionsManager.getInstance(context);

            final IWindowManager windowManagerService = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
           if (windowManagerService == null) {
               return; // ouch
           }

            boolean isKeyguardSecure = false;
            try {
                isKeyguardSecure = windowManagerService.isKeyguardSecure();
            } catch (RemoteException e) {
            }

            PowerManager pm = context.getSystemService(PowerManager.class);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            Intent intent;

            // process the actions
            switch (action) {
                case ActionConstants.ACTION_HOME:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_HOME, isLongpress);
                    return;
                case ActionConstants.ACTION_BACK:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_BACK, isLongpress);
                    return;
                case ActionConstants.ACTION_SEARCH:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_SEARCH, isLongpress);
                    return;
                case ActionConstants.ACTION_NOTIFICATIONS:
                    if (isKeyguardShowing && isKeyguardSecure) {
                        return;
                    }
                    try {
                        barService.expandNotificationsPanel();
                    } catch (RemoteException e) {}
                    return;
                case ActionConstants.ACTION_SETTINGS_PANEL:
                    if (isKeyguardShowing && isKeyguardSecure) {
                        return;
                    }
                    try {
                        barService.expandSettingsPanel(null);
                    } catch (RemoteException e) {}
                    return;
                case ActionConstants.ACTION_NOWONTAP:
                    actionsManager.startAssist(new Bundle());
                    return;
                case ActionConstants.ACTION_TORCH:
                    try {
                        CameraManager cameraManager = (CameraManager)
                                context.getSystemService(Context.CAMERA_SERVICE);
                        for (final String cameraId : cameraManager.getCameraIdList()) {
                            CameraCharacteristics characteristics =
                                cameraManager.getCameraCharacteristics(cameraId);
                            Boolean f =
                                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                            int orient = characteristics.get(CameraCharacteristics.LENS_FACING);
                            if (f && orient == CameraCharacteristics.LENS_FACING_BACK) {
                                cameraManager.setTorchMode(cameraId, !sTorchEnabled);
                                sTorchEnabled = !sTorchEnabled;
                                break;
                            }
                        }
                    } catch (CameraAccessException e) {}
                    return;
                case ActionConstants.ACTION_POWER_MENU:
                    actionsManager.toggleGlobalMenu();
                    return;
                case ActionConstants.ACTION_MENU:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_MENU, isLongpress);
                    return;
                case ActionConstants.ACTION_IME_NAVIGATION_LEFT:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_LEFT, isLongpress);
                    return;
                case ActionConstants.ACTION_IME_NAVIGATION_RIGHT:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_RIGHT, isLongpress);
                    return;
                case ActionConstants.ACTION_IME_NAVIGATION_UP:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_UP, isLongpress);
                    return;
                case ActionConstants.ACTION_IME_NAVIGATION_DOWN:
                    triggerVirtualKeypress(KeyEvent.KEYCODE_DPAD_DOWN, isLongpress);
                    return;
                case ActionConstants.ACTION_POWER:
                    pm.goToSleep(SystemClock.uptimeMillis());
                    return;
                case ActionConstants.ACTION_TOGGLE_SCREEN:
                    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                    for (Display display : dm.getDisplays()) {
                        if (display.getState() == Display.STATE_OFF) {
                            pm.wakeUp(SystemClock.uptimeMillis());
                        } else {
                            pm.goToSleep(SystemClock.uptimeMillis());
                        }
                    }
                    return;
                case ActionConstants.ACTION_IME:
                    if (isKeyguardShowing) {
                        return;
                    }
                    context.sendBroadcastAsUser(
                            new Intent("android.settings.SHOW_INPUT_METHOD_PICKER"),
                            new UserHandle(UserHandle.USER_CURRENT));
                    return;
                case ActionConstants.ACTION_KILL:
                    if (isKeyguardShowing) {
                        return;
                    }
                    actionsManager.toggleKillApp();
                    return;
                case ActionConstants.ACTION_LAST_APP:
                    if (isKeyguardShowing) {
                        return;
                    }
                    actionsManager.toggleLastApp();
                    return;
                case ActionConstants.ACTION_SCREENSHOT:
                    actionsManager.toggleScreenshot();
                    return;
                case ActionConstants.ACTION_SPLIT_SCREEN:
                    actionsManager.toggleSplitScreen();
                    return;
                case ActionConstants.ACTION_RECENTS:
                    if (isKeyguardShowing) {
                        return;
                    }
                    actionsManager.toggleRecentApps();
                    return;
                case ActionConstants.ACTION_VOICE_SEARCH:
                    // launch the search activity
                    intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        // TODO: This only stops the factory-installed search manager.
                        // Need to formalize an API to handle others
                        SearchManager searchManager =
                                (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
                        if (searchManager != null) {
                            searchManager.stopSearch();
                        }
                        startActivity(context, intent, actionsManager, isKeyguardShowing, pm);
                    } catch (ActivityNotFoundException e) {
                        Log.e("Action:", "No activity to handle assist long press action.", e);
                    }
                    return;
                case ActionConstants.ACTION_VIB:
                    if (am != null && ActivityManagerNative.isSystemReady()) {
                        if (am.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
                            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                            Vibrator vib =
                                    (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                            if (vib != null) {
                                vib.vibrate(50);
                            }
                        } else {
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            ToneGenerator tg = new ToneGenerator(
                                    AudioManager.STREAM_NOTIFICATION,
                                    (int) (ToneGenerator.MAX_VOLUME * 0.85));
                            if (tg != null) {
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }
                    }
                    return;
                case ActionConstants.ACTION_SILENT:
                    if (am != null && ActivityManagerNative.isSystemReady()) {
                        if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        } else {
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            ToneGenerator tg = new ToneGenerator(
                                    AudioManager.STREAM_NOTIFICATION,
                                    (int) (ToneGenerator.MAX_VOLUME * 0.85));
                            if (tg != null) {
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }
                    }
                    return;
                case ActionConstants.ACTION_VIB_SILENT:
                    if (am != null && ActivityManagerNative.isSystemReady()) {
                        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                            Vibrator vib =
                                    (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                            if (vib != null) {
                                vib.vibrate(50);
                            }
                        } else if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                        } else {
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                            ToneGenerator tg = new ToneGenerator(
                                    AudioManager.STREAM_NOTIFICATION,
                                    (int) (ToneGenerator.MAX_VOLUME * 0.85));
                            if (tg != null) {
                                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                            }
                        }
                    }
                    return;
                case ActionConstants.ACTION_CAMERA:
                    if (isKeyguardSecure) {
                        intent = new Intent(
                                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE, null);
                    } else {
                        intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, null);
                    }
                    startActivity(context, intent, actionsManager, false, pm);
                    return;
                case ActionConstants.ACTION_MEDIA_PREVIOUS:
                    dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_PREVIOUS, context);
                    return;
                case ActionConstants.ACTION_MEDIA_NEXT:
                    dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_NEXT, context);
                    return;
                case ActionConstants.ACTION_MEDIA_PLAY_PAUSE:
                    dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, context);
                    return;
                case ActionConstants.ACTION_WAKE_DEVICE:
                    if (!pm.isInteractive()) {
                        pm.wakeUp(SystemClock.uptimeMillis());
                    }
                    return;
                case ActionConstants.ACTION_DOZE_PULSE:
                    if (!pm.isScreenOn()) {
                        context.sendBroadcast(new Intent("com.android.systemui.doze.pulse"));
                    }
                    return;
                default:
                    // we must have a custom uri
                    try {
                        intent = Intent.parseUri(action, 0);
                    } catch (URISyntaxException e) {
                        Log.e("Action:", "URISyntaxException: [" + action + "]");
                        return;
                    }
                    startActivity(context, intent, actionsManager, isKeyguardShowing, pm);
                    return;
            }
    }

    public static boolean isActionKeyEvent(String action) {
        if (action.equals(ActionConstants.ACTION_HOME)
                || action.equals(ActionConstants.ACTION_BACK)
                || action.equals(ActionConstants.ACTION_SEARCH)
                || action.equals(ActionConstants.ACTION_MENU)
                || action.equals(ActionConstants.ACTION_NULL)) {
            return true;
        }
        return false;
    }

    private static void startActivity(Context context, Intent intent,
            SlimActionsManager actionsManager, boolean isKeyguardShowing, PowerManager pm) {
        if (intent == null) {
            return;
        }

        if (pm != null && !pm.isInteractive()) {
            pm.wakeUp(SystemClock.uptimeMillis());
        }

        if (isKeyguardShowing) {
            // Have keyguard show the bouncer and launch the activity if the user succeeds.
            actionsManager.showCustomIntentAfterKeyguard(intent);
        } else {
            // otherwise let us do it here
            try {
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard(null);
            } catch (RemoteException e) {
                Log.w("Action", "Error dismissing keyguard", e);
            }
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivityAsUser(intent,
                    new UserHandle(UserHandle.USER_CURRENT));
        }
    }

    private static void dispatchMediaKeyWithWakeLock(int keycode, Context context) {
        if (ActivityManagerNative.isSystemReady()) {
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
            MediaSessionLegacyHelper.getHelper(context).sendMediaButtonEvent(event, true);
            event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
            MediaSessionLegacyHelper.getHelper(context).sendMediaButtonEvent(event, true);
        }
    }

    public static void triggerVirtualKeypress(final int keyCode, boolean longpress) {
        InputManager im = InputManager.getInstance();
        long now = SystemClock.uptimeMillis();
        int downflags = 0;
        int upflags = 0;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            || keyCode == KeyEvent.KEYCODE_DPAD_UP
            || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            downflags = upflags = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
        } else {
            downflags = upflags = KeyEvent.FLAG_FROM_SYSTEM;
        }
        if (longpress) {
            downflags |= KeyEvent.FLAG_LONG_PRESS;
        }

        final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                downflags,
                InputDevice.SOURCE_KEYBOARD);
        im.injectInputEvent(downEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

        final KeyEvent upEvent = new KeyEvent(now, now, KeyEvent.ACTION_UP,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                upflags,
                InputDevice.SOURCE_KEYBOARD);
        im.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }
}
