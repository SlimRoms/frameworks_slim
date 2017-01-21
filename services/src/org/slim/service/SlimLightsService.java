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

import static android.service.notification.NotificationListenerService.TRIM_FULL;
import static android.service.notification.NotificationListenerService.Ranking.IMPORTANCE_DEFAULT;

import android.app.INotificationManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.INotificationListener;
import android.service.notification.IStatusBarNotificationHolder;
import android.service.notification.StatusBarNotification;
import android.service.notification.NotificationRankingUpdate;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManagerPolicy;

import com.android.server.LocalServices;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.notification.NotificationManagerService;

//import org.slim.framework.internal.lights.ISlimLightsService;
import slim.constants.SlimServiceConstants;
import slim.provider.SlimSettings;
import slim.utils.ColorUtils;
//import slim.utils.ContentObserver;
import android.database.ContentObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlimLightsService extends SlimSystemService {

    private static final String TAG = "SlimLightsSevice";

    private Context mContext;

    private Object mLock = new Object();

    private Light mNotificationLight;
    private Light mBatteryLight;

    private boolean mScreenOn = true;
    private boolean mInCall = false;

    // Disable LED until SettingsObserver can be started
    private boolean mBatteryLightEnabled = false;
    private boolean mLedPulseEnabled;
    private int mBatteryLowARGB;
    private int mBatteryMediumARGB;
    private int mBatteryMediumFastARGB;
    private int mBatteryFullARGB;
    private int mChargingFastThreshold;
    private int mBatteryLedOn;
    private int mBatteryLedOff;
    private boolean mMultiColorLed;
    private int mLowBatteryWarningLevel;

    private boolean mNotificationPulseEnabled;
    private int mDefaultNotificationColor;
    private int mDefaultNotificationLedOn;
    private int mDefaultNotificationLedOff;

    private boolean mAutoGenerateNotificationColor = true;

    private ArrayList<String> mNotificationLights = new ArrayList<>();
    private HashMap<String, NotificationLedValues> mNotificationPulseCustomLedValues;
    private Map<String, String> mPackageNameMappings;
    private final ArrayMap<String, Integer> mGeneratedPackageLedColors = new ArrayMap<>();

    private KeyguardManager mKeyguardManager;
    private INotificationManager mNoMan;
    private SlimNotificationListener mListener;

    private int mBatteryLevel;
    private boolean mCharging;
    private int mMaxChargingCurrent;

    class NotificationLedValues {
        public int color;
        public int onMS;
        public int offMS;
    }

    public SlimLightsService(Context context) {
        super(context);
        mContext = context;

        mBatteryLedOn = context.getResources().getInteger(
                com.android.internal.R.integer.config_notificationsBatteryLedOn);
        mBatteryLedOff = context.getResources().getInteger(
                com.android.internal.R.integer.config_notificationsBatteryLedOff);
        mLowBatteryWarningLevel = context.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mMultiColorLed = context.getResources().getBoolean(
                org.slim.framework.internal.R.bool.config_multiColorBatteryLed);
        mChargingFastThreshold = context.getResources().getInteger(
                org.slim.framework.internal.R.integer.config_chargingFastThreshold);
        mDefaultNotificationColor = context.getResources().getColor(
                com.android.internal.R.color.config_defaultNotificationColor);
        mDefaultNotificationLedOff = context.getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);
        mDefaultNotificationLedOn = context.getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
    }

    @Override
    public void onStart() {
        Log.d("TEST", "start lights service");

        final LightsManager lights = getLocalService(LightsManager.class);
        mNotificationLight = lights.getLight(LightsManager.LIGHT_ID_NOTIFICATIONS);
        mBatteryLight = lights.getLight(LightsManager.LIGHT_ID_BATTERY);

        mNotificationPulseCustomLedValues = new HashMap<>();
        mPackageNameMappings = new HashMap<>();
        final String[] defaultMapping = mContext.getResources().getStringArray(
                org.slim.framework.internal.R.array.notification_light_package_mapping);
        for (String mapping : defaultMapping) {
            String[] map = mapping.split("\\|");
            mPackageNameMappings.put(map[0], map[1]);
        }

        mListener = new SlimNotificationListener();
        INotificationManager noman = getNotificationInterface();
        try {
            noman.registerListener(mListener, new ComponentName(mContext,
                    SlimNotificationListener.class), UserHandle.USER_ALL);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //publishBinderService(SlimServiceConstants.SLIM_LIGHTS_SERVICE, mService);
    }

    private final INotificationManager getNotificationInterface() {
        if (mNoMan == null) {
            mNoMan = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }
        return mNoMan;
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            new SettingsObserver(new Handler()).observe();

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            mContext.registerReceiver(new SlimBatteryListener(), filter);

            // register for various Intents
            filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TEST", "action=" + action);

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                // Keep track of screen on/off state, but do not turn off the notification light
                // until user passes through the lock screen or views the notification.
                mScreenOn = true;
                updateLightsLocked();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mScreenOn = false;
                updateLightsLocked();
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                mInCall = TelephonyManager.EXTRA_STATE_OFFHOOK
                        .equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE));
                updateLightsLocked();
            }
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        protected void observe() {

            ContentResolver resolver = mContext.getContentResolver();

            // Battery light enabled
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.BATTERY_LIGHT_ENABLED), false, this, UserHandle.USER_ALL);

            // Low battery pulse
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.BATTERY_LIGHT_PULSE), false, this, UserHandle.USER_ALL);

            // Light colors
            if (mMultiColorLed) {
                // Register observer if we have a multi color led
                resolver.registerContentObserver(
                        SlimSettings.System.getUriFor(
                        SlimSettings.System.BATTERY_LIGHT_LOW_COLOR),
                        false, this, UserHandle.USER_ALL);
                resolver.registerContentObserver(
                        SlimSettings.System.getUriFor(
                        SlimSettings.System.BATTERY_LIGHT_MEDIUM_COLOR),
                        false, this, UserHandle.USER_ALL);
                resolver.registerContentObserver(
                        SlimSettings.System.getUriFor(
                        SlimSettings.System.BATTERY_LIGHT_MEDIUM_FAST_COLOR),
                        false, this, UserHandle.USER_ALL);
                resolver.registerContentObserver(
                        SlimSettings.System.getUriFor(
                        SlimSettings.System.BATTERY_LIGHT_FULL_COLOR),
                        false, this, UserHandle.USER_ALL);
            }

            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(SlimSettings.System.getUriFor(
                    SlimSettings.System.NOTIFICATION_LIGHT_COLOR_AUTO),
                    false, this, UserHandle.USER_ALL);

            update();
        }
  
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("TEST", "onChange - " + uri.toString());
            update();
        }

        protected void update() {
            ContentResolver resolver = mContext.getContentResolver();
            Resources res = mContext.getResources();

            // Battery light enabled
            mBatteryLightEnabled = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_ENABLED, 1) != 0;

            // Low battery pulse
            mLedPulseEnabled = SlimSettings.System.getInt(resolver,
                        SlimSettings.System.BATTERY_LIGHT_PULSE, 1) != 0;

            // Automatically pick a color for LED if not set
            mAutoGenerateNotificationColor = SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_COLOR_AUTO,
                    1, UserHandle.USER_CURRENT) != 0;
            mGeneratedPackageLedColors.clear();

            // Light colors
            mBatteryLowARGB = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_LOW_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryLowARGB));
            mBatteryMediumARGB = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
            mBatteryMediumFastARGB = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_MEDIUM_FAST_COLOR, res.getInteger(
                    org.slim.framework.internal.R.integer.config_notificationsBatteryMediumFastARGB));
            mBatteryFullARGB = SlimSettings.System.getInt(resolver,
                    SlimSettings.System.BATTERY_LIGHT_FULL_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryFullARGB));

            Log.d("TEST", "update" + " : mBatteryFullARGB=" + mBatteryFullARGB);

            mBatteryLight.turnOff();
            handleBatteryLight();

            // LED enabled
            mNotificationPulseEnabled = SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE, 0, UserHandle.USER_CURRENT) != 0;

            // LED default color
            mDefaultNotificationColor = SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR,
                    mDefaultNotificationColor, UserHandle.USER_CURRENT);

            // LED default on MS
            mDefaultNotificationLedOn = SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON,
                    mDefaultNotificationLedOn, UserHandle.USER_CURRENT);

            // LED default off MS
            mDefaultNotificationLedOff = SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF,
                    mDefaultNotificationLedOff, UserHandle.USER_CURRENT);

            // LED custom notification colors
            mNotificationPulseCustomLedValues.clear();
            if (SlimSettings.System.getIntForUser(resolver,
                    SlimSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE, 0,
                    UserHandle.USER_CURRENT) != 0) {
                parseNotificationPulseCustomValuesString(
                        SlimSettings.System.getStringForUser(resolver,
                        SlimSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                        UserHandle.USER_CURRENT));
            }
        }
    }

    private NotificationLedValues getLedValuesForNotification(StatusBarNotification sbn) {
        return mNotificationPulseCustomLedValues.get(mapPackage(sbn.getPackageName()));
    }

    private String mapPackage(String pkg) {
        if (!mPackageNameMappings.containsKey(pkg)) {
            return pkg;
        }
        return mPackageNameMappings.get(pkg);
    }

    private void parseNotificationPulseCustomValuesString(String customLedValuesString) {
        if (TextUtils.isEmpty(customLedValuesString)) {
            return;
        }

        for (String packageValuesString : customLedValuesString.split("\\|")) {
            String[] packageValues = packageValuesString.split("=");
            if (packageValues.length != 2) {
                Log.e(TAG, "Error parsing custom led values for unknown package");
                continue;
            }
            String packageName = packageValues[0];
            String[] values = packageValues[1].split(";");
            if (values.length != 3) {
                Log.e(TAG, "Error parsing custom led values '"
                        + packageValues[1] + "' for " + packageName);
                continue;
            }
            NotificationLedValues ledValues = new NotificationLedValues();
            try {
                ledValues.color = Integer.parseInt(values[0]);
                ledValues.onMS = Integer.parseInt(values[1]);
                ledValues.offMS = Integer.parseInt(values[2]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing custom led values '"
                        + packageValues[1] + "' for " + packageName);
                continue;
            }
            Log.d("TEST", "parsed ledValues for " + packageName +
                    " : " + ledValues.color + " : " + ledValues.onMS + " : " + ledValues.offMS);
            mNotificationPulseCustomLedValues.put(packageName, ledValues);
        }
    }

    private void handleBatteryLight() {
        Log.d("TEST", "handleBatteryLight");
        synchronized (mLock) {
            if (!mBatteryLightEnabled) {
                mBatteryLight.turnOff();
            } else if (mBatteryLevel < mLowBatteryWarningLevel) {
                if (mCharging) {
                    mBatteryLight.setColor(mBatteryLowARGB);
                } else if (mLedPulseEnabled) {
                    mBatteryLight.setFlashing(mBatteryLowARGB, Light.LIGHT_FLASH_TIMED,
                            mBatteryLedOn, mBatteryLedOff);
                }
            } else if (mCharging) {
                if (mBatteryLevel >= 90) {
                    mBatteryLight.setColor(mBatteryFullARGB);
                } else if (mMaxChargingCurrent > mChargingFastThreshold) {
                    mBatteryLight.setColor(mBatteryMediumFastARGB);
                } else {
                    mBatteryLight.setColor(mBatteryMediumARGB);
                }
            } else {
                mBatteryLight.turnOff();
            }
        }
    }

    private class SlimBatteryListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mBatteryLevel = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                boolean charged = status == BatteryManager.BATTERY_STATUS_FULL;
                mCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

                mMaxChargingCurrent = intent.getIntExtra(
                        BatteryManager.EXTRA_MAX_CHARGING_CURRENT, 0);

                Log.d("TEST", "charging=" + mCharging + " : level=" + mBatteryLevel);

                handleBatteryLight();
            }
        }

    }

    private void handleNotificationPosted(StatusBarNotification sbn,
            NotificationRankingUpdate nru) {
        Log.d("TEST", "posted : pName=" + sbn.getPackageName());
        Notification notif = sbn.getNotification();
        String key = sbn.getKey();
        boolean wasShowLights = mNotificationLights.remove(key);
        boolean aboveThreshold = false;
        for (int i = 0; i < nru.getOrderedKeys().length; i++) {
            if (nru.getOrderedKeys()[i].equals(key)) {
                aboveThreshold = nru.getImportance()[i] > IMPORTANCE_DEFAULT;
                break;
            }
        }
        Log.d("TEST", "aboveThreshold=" + aboveThreshold);
        mNotificationLights.add(key);
        updateLightsLocked();
    }

    private void handleNotificationRemoved(StatusBarNotification sbn) {
        Log.d("TEST", "removed : pName=" + sbn.getPackageName());
        if (mNotificationLights.remove(sbn.getKey())) {
            updateLightsLocked();
        }
    }

    private void updateLightsLocked() {
        Log.d("TEST", "updateLightsLocked();");
        synchronized (mLock) {
            StatusBarNotification sbn = null;
            while (sbn == null && !mNotificationLights.isEmpty()) {
                Log.d("TEST", "mNotificationLights is not empty");
                final String key = mNotificationLights.get(mNotificationLights.size() - 1);
                Log.d("TEST", "key - " + key);
                StatusBarNotification[] notifications = getActiveNotifications(key);
                Log.d("TEST", "notifications size - " + notifications.length);
                if (notifications.length > 0) {
                    sbn = notifications[0];
                }
                Log.d("TEST", "sbn is null ? " + (sbn == null));
                if (sbn == null) {
                    mNotificationLights.remove(key);
                }
            }

            final boolean enableLed;
            if (sbn == null) {
                Log.d("TEST", "sbn is null");
                enableLed = false;
            } else if (mInCall) {
                enableLed = false;
            } else if (mScreenOn) {
                enableLed = false;
            } else {
                enableLed = true;
            }
            Log.d("TEST", "enableLed - " + enableLed);

            if (!enableLed) {
                mNotificationLight.turnOff();
            } else {
                final Notification ledno = sbn.getNotification();
                Log.d("TEST", "ledno - " + ledno.toString());
                final NotificationLedValues ledValues = getLedValuesForNotification(sbn);
                Log.d("TEST", "ledValues");
                int ledARGB;
                int ledOnMS;
                int ledOffMS;
                if (ledValues != null) {
                    Log.d("TEST", "ledValues are not null");
                    ledARGB = ledValues.color != 0 ? ledValues.color : mDefaultNotificationColor;
                    ledOnMS = ledValues.onMS >= 0 ? ledValues.onMS : mDefaultNotificationLedOn;
                    ledOffMS = ledValues.offMS >= 0 ? ledValues.offMS : mDefaultNotificationLedOff;
                } else if ((ledno.defaults & Notification.DEFAULT_LIGHTS) != 0) {
                    Log.d("TEST", "generating Colors for notification");
                    ledARGB = generateLedColorForNotification(sbn);
                    ledOnMS = mDefaultNotificationLedOn;
                    ledOffMS = mDefaultNotificationLedOff;
                } else {
                    Log.d("TEST", "using notification default colors");
                    ledARGB = ledno.ledARGB;
                    ledOnMS = ledno.ledOnMS;
                    ledOffMS = ledno.ledOffMS;
                }

                Log.d("TEST", "pName=" + sbn.getPackageName() + " : color=" + ledARGB +
                        " : on=" + ledOnMS + " : off=" + ledOffMS);
                if (mNotificationPulseEnabled &&
                            (ledARGB > 0 && ledOnMS >= 0 && ledOffMS >= 0)) {
                    mNotificationLight.setFlashing(ledARGB, Light.LIGHT_FLASH_TIMED,
                            ledOnMS, ledOffMS);
                }
            }
        }
    }

    private int generateLedColorForNotification(StatusBarNotification sbn) {
        Log.d("TEST", "generateLedColorForNotification");
        if (!mAutoGenerateNotificationColor) {
            return mDefaultNotificationColor;
        }
        if (!mMultiColorLed) {
            return mDefaultNotificationColor;
        }
        final String packageName = sbn.getPackageName();
        final String mapping = mapPackage(packageName);
        int color = mDefaultNotificationColor;

        Log.d("TEST", "check for a generated color");
        if (mGeneratedPackageLedColors.containsKey(mapping)) {
            return mGeneratedPackageLedColors.get(mapping);
        }

        Log.d("TEST", "get app icon");
        PackageManager pm = mContext.getPackageManager();
        Drawable icon;
        try {
            icon = pm.getApplicationIcon(mapping);
        } catch (NameNotFoundException e) {
            return color;
        }

        Log.d("TEST", "generate color from app icon");
        color = ColorUtils.generateAlertColorFromDrawable(icon);
        Log.d("TEST", "color generated put in cache");
        mGeneratedPackageLedColors.put(mapping, color);
        return color;
    }

    private StatusBarNotification[] getActiveNotifications() {
        return getActiveNotifications(null);
    }

    private StatusBarNotification[] getActiveNotifications(String... keys) {
        Log.d("TEST", "getActiveNotifications(" + keys + ")");
        try {
            ParceledListSlice<StatusBarNotification> parceledList = getNotificationInterface()
                    .getActiveNotificationsFromListener(mListener, keys, TRIM_FULL);
            List<StatusBarNotification> list = parceledList.getList();
            return list.toArray(new StatusBarNotification[list.size()]);
        } catch (android.os.RemoteException ex) {
            Log.v(TAG, "Unable to contact notification manager", ex);
        }
        return null;
    }

    private class SlimNotificationListener extends INotificationListener.Stub {

        // listeners and rankers
        public void onListenerConnected(NotificationRankingUpdate update) {
        }

        public void onNotificationPosted(IStatusBarNotificationHolder notificationHolder,
                NotificationRankingUpdate update) {
            try {
                handleNotificationPosted(notificationHolder.get(), update);
            } catch (RemoteException e) {}
        }

        public void onNotificationRemoved(IStatusBarNotificationHolder notificationHolder,
                NotificationRankingUpdate update) {
            try {
                handleNotificationRemoved(notificationHolder.get());
            } catch (RemoteException e) {}
        }

        public void onNotificationRankingUpdate(NotificationRankingUpdate update) {
        }

        public void onListenerHintsChanged(int hints) {
        }

        public void onInterruptionFilterChanged(int interruptionFilter) {
        }

        // rankers only
        public void onNotificationEnqueued(IStatusBarNotificationHolder notificationHolder,
                int importance, boolean user) {
        }

        public void onNotificationVisibilityChanged(String key, long time, boolean visible) {
        }

        public void onNotificationClick(String key, long time) {
        }

        public void onNotificationActionClick(String key, long time, int actionIndex) {
        }

        public void onNotificationRemovedReason(String key, long time, int reason) {
        }
    }
}
