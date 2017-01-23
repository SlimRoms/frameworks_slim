/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2017 SlimRoms Project
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

package org.slim.providers.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;

import java.util.Locale;

import slim.provider.SlimSettings;

public class SlimSettingsHelper {
    private Context mContext;

    private static final ArraySet<String> sBroadcastOnRestore;
    static {
        sBroadcastOnRestore = new ArraySet<>();
    }

    private interface SettingsLookup {
        public String lookup(ContentResolver resolver, String name, int userHandle);
    }

    private static SettingsLookup sSystemLookup = new SettingsLookup() {
        public String lookup(ContentResolver resolver, String name, int userHandle) {
            return SlimSettings.System.getStringForUser(resolver, name, userHandle);
        }
    };

    private static SettingsLookup sSecureLookup = new SettingsLookup() {
        public String lookup(ContentResolver resolver, String name, int userHandle) {
            return SlimSettings.Secure.getStringForUser(resolver, name, userHandle);
        }
    };

    private static SettingsLookup sGlobalLookup = new SettingsLookup() {
        public String lookup(ContentResolver resolver, String name, int userHandle) {
            return SlimSettings.Global.getStringForUser(resolver, name, userHandle);
        }
    };

    public SlimSettingsHelper(Context context) {
        mContext = context;
    }

    /**
     * Sets the property via a call to the appropriate API, if any, and returns
     * whether or not the setting should be saved to the database as well.
     * @param name the name of the setting
     * @param value the string value of the setting
     * @return whether to continue with writing the value to the database. In
     * some cases the data will be written by the call to the appropriate API,
     * and in some cases the property value needs to be modified before setting.
     */
    public void restoreValue(Context context, ContentResolver cr, ContentValues contentValues,
            Uri destination, String name, String value) {
        // Will we need a post-restore broadcast for this element?
        String oldValue = null;
        boolean sendBroadcast = false;
        final SettingsLookup table;

        if (destination.equals(SlimSettings.Secure.CONTENT_URI)) {
            table = sSecureLookup;
        } else if (destination.equals(SlimSettings.System.CONTENT_URI)) {
            table = sSystemLookup;
        } else { /* must be GLOBAL; this was preflighted by the caller */
            table = sGlobalLookup;
        }

        if (sBroadcastOnRestore.contains(name)) {
            // TODO: http://b/22388012
            oldValue = table.lookup(cr, name, UserHandle.USER_SYSTEM);
            sendBroadcast = true;
        }

        try {

            // Default case: write the restored value to settings
            contentValues.clear();
            contentValues.put(Settings.NameValueTable.NAME, name);
            contentValues.put(Settings.NameValueTable.VALUE, value);
            cr.insert(destination, contentValues);
        } catch (Exception e) {
            // If we fail to apply the setting, by definition nothing happened
            sendBroadcast = false;
        } finally {
            // If this was an element of interest, send the "we just restored it"
            // broadcast with the historical value now that the new value has
            // been committed and observers kicked off.
            if (sendBroadcast) {
                Intent intent = new Intent(Intent.ACTION_SETTING_RESTORED)
                        .setPackage("android").addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
                        .putExtra(Intent.EXTRA_SETTING_NAME, name)
                        .putExtra(Intent.EXTRA_SETTING_NEW_VALUE, value)
                        .putExtra(Intent.EXTRA_SETTING_PREVIOUS_VALUE, oldValue);
                context.sendBroadcastAsUser(intent, UserHandle.SYSTEM, null);
            }
        }
    }

    public String onBackupValue(String name, String value) {
        // Return the original value
        return value;
    }

    private void setAutoRestore(boolean enabled) {
        try {
            IBackupManager bm = IBackupManager.Stub.asInterface(
                    ServiceManager.getService(Context.BACKUP_SERVICE));
            if (bm != null) {
                bm.setAutoRestore(enabled);
            }
        } catch (RemoteException e) {}
    }

    byte[] getLocaleData() {
        Configuration conf = mContext.getResources().getConfiguration();
        final Locale loc = conf.locale;
        String localeString = loc.getLanguage();
        String country = loc.getCountry();
        if (!TextUtils.isEmpty(country)) {
            localeString += "-" + country;
        }
        return localeString.getBytes();
    }

    /**
     * Sets the locale specified. Input data is the byte representation of a
     * BCP-47 language tag. For backwards compatibility, strings of the form
     * {@code ll_CC} are also accepted, where {@code ll} is a two letter language
     * code and {@code CC} is a two letter country code.
     *
     * @param data the locale string in bytes.
     */
    void setLocaleData(byte[] data, int size) {
        // Check if locale was set by the user:
        Configuration conf = mContext.getResources().getConfiguration();
        // TODO: The following is not working as intended because the network is forcing a locale
        // change after registering. Need to find some other way to detect if the user manually
        // changed the locale
        if (conf.userSetLocale) return; // Don't change if user set it in the SetupWizard

        final String[] availableLocales = mContext.getAssets().getLocales();
        // Replace "_" with "-" to deal with older backups.
        String localeCode = new String(data, 0, size).replace('_', '-');
        Locale loc = null;
        for (int i = 0; i < availableLocales.length; i++) {
            if (availableLocales[i].equals(localeCode)) {
                loc = Locale.forLanguageTag(localeCode);
                break;
            }
        }
        if (loc == null) return; // Couldn't find the saved locale in this version of the software

        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();
            config.locale = loc;
            // indicate this isn't some passing default - the user wants this remembered
            config.userSetLocale = true;

            am.updateConfiguration(config);
        } catch (RemoteException e) {
            // Intentionally left blank
        }
    }
}
