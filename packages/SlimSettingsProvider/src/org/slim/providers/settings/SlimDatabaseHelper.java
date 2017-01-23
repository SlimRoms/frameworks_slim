/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.media.AudioSystem;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.internal.content.PackageHelper;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import slim.provider.SlimSettings;
import slim.provider.SlimSettings.Global;
import slim.provider.SlimSettings.Secure;

/**
 * Legacy settings database helper class for {@link SettingsProvider}.
 *
 * IMPORTANT: Do not add any more upgrade steps here as the global,
 * secure, and system settings are no longer stored in a database
 * but are kept in memory and persisted to XML.
 *
 * See: SettingsProvider.UpgradeController#onUpgradeLocked
 *
 * @deprecated The implementation is frozen.  Do not add any new code to this class!
 */
@Deprecated
class SlimDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "SlimSettingsProvider";
    private static final String DATABASE_NAME = "slim_settings.db";

    // Please, please please. If you update the database version, check to make sure the
    // database gets upgraded properly. At a minimum, please confirm that 'upgradeVersion'
    // is properly propagated through your change.  Not doing so will result in a loss of user
    // settings.
    private static final int DATABASE_VERSION = 1;

    private Context mContext;
    private int mUserHandle;

    private static final HashSet<String> mValidTables = new HashSet<String>();

    private static final String DATABASE_JOURNAL_SUFFIX = "-journal";
    private static final String DATABASE_BACKUP_SUFFIX = "-backup";

    private static final String TABLE_SYSTEM = "system";
    private static final String TABLE_SECURE = "secure";
    private static final String TABLE_GLOBAL = "global";

    static {
        mValidTables.add(TABLE_SYSTEM);
        mValidTables.add(TABLE_SECURE);
        mValidTables.add(TABLE_GLOBAL);
    }

    static String dbNameForUser(final int userHandle) {
        // The owner gets the unadorned db name;
        if (userHandle == UserHandle.USER_SYSTEM) {
            return DATABASE_NAME;
        } else {
            // Place the database in the user-specific data tree so that it's
            // cleaned up automatically when the user is deleted.
            File databaseFile = new File(
                    Environment.getUserSystemDirectory(userHandle), DATABASE_NAME);
            // If databaseFile doesn't exist, database can be kept in memory. It's safe because the
            // database will be migrated and disposed of immediately after onCreate finishes
            if (!databaseFile.exists()) {
                Log.i(TAG, "No previous database file exists - running in in-memory mode");
                return null;
            }
            return databaseFile.getPath();
        }
    }

    public SlimDatabaseHelper(Context context, int userHandle) {
        super(context, dbNameForUser(userHandle), null, DATABASE_VERSION);
        mContext = context;
        mUserHandle = userHandle;
    }

    public static boolean isValidTable(String name) {
        return mValidTables.contains(name);
    }

    private boolean isInMemory() {
        return getDatabaseName() == null;
    }

    public void dropDatabase() {
        close();
        // No need to remove files if db is in memory
        if (isInMemory()) {
            return;
        }
        File databaseFile = mContext.getDatabasePath(getDatabaseName());
        if (databaseFile.exists()) {
            databaseFile.delete();
        }
        File databaseJournalFile = mContext.getDatabasePath(getDatabaseName()
                + DATABASE_JOURNAL_SUFFIX);
        if (databaseJournalFile.exists()) {
            databaseJournalFile.delete();
        }
    }

    public void backupDatabase() {
        close();
        // No need to backup files if db is in memory
        if (isInMemory()) {
            return;
        }
        File databaseFile = mContext.getDatabasePath(getDatabaseName());
        if (!databaseFile.exists()) {
            return;
        }
        File backupFile = mContext.getDatabasePath(getDatabaseName()
                + DATABASE_BACKUP_SUFFIX);
        if (backupFile.exists()) {
            return;
        }
        databaseFile.renameTo(backupFile);
    }

    private void createSecureTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE secure (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE ON CONFLICT REPLACE," +
                "value TEXT" +
                ");");
        db.execSQL("CREATE INDEX secureIndex1 ON secure (name);");
    }

    private void createGlobalTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE global (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE ON CONFLICT REPLACE," +
                "value TEXT" +
                ");");
        db.execSQL("CREATE INDEX globalIndex1 ON global (name);");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE system (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE ON CONFLICT REPLACE," +
                    "value TEXT" +
                    ");");
        db.execSQL("CREATE INDEX systemIndex1 ON system (name);");

        createSecureTable(db);

        // Only create the global table for the singleton 'owner/system' user
        if (mUserHandle == UserHandle.USER_SYSTEM) {
            createGlobalTable(db);
        }

        // Load inital settings values
        loadSettings(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        Log.w(TAG, "Upgrading settings database from version " + oldVersion + " to "
                + currentVersion);

        int upgradeVersion = oldVersion;

        // Pattern for upgrade blocks:
        //
        //    if (upgradeVersion == [the DATABASE_VERSION you set] - 1) {
        //        .. your upgrade logic..
        //        upgradeVersion = [the DATABASE_VERSION you set]
        //    }

        /*
         * IMPORTANT: Do not add any more upgrade steps here as the global,
         * secure, and system settings are no longer stored in a database
         * but are kept in memory and persisted to XML.
         *
         * See: SettingsProvider.UpgradeController#onUpgradeLocked
         */

        if (upgradeVersion != currentVersion) {
            recreateDatabase(db, oldVersion, upgradeVersion, currentVersion);
        }
    }

    public void recreateDatabase(SQLiteDatabase db, int oldVersion,
            int upgradeVersion, int currentVersion) {
        db.execSQL("DROP TABLE IF EXISTS global");
        db.execSQL("DROP TABLE IF EXISTS globalIndex1");
        db.execSQL("DROP TABLE IF EXISTS system");
        db.execSQL("DROP INDEX IF EXISTS systemIndex1");
        db.execSQL("DROP TABLE IF EXISTS secure");
        db.execSQL("DROP INDEX IF EXISTS secureIndex1");

        onCreate(db);

        // Added for diagnosing settings.db wipes after the fact
        String wipeReason = oldVersion + "/" + upgradeVersion + "/" + currentVersion;
        db.execSQL("INSERT INTO secure(name,value) values('" +
                "wiped_db_reason" + "','" + wipeReason + "');");
    }

    private String[] setToStringArray(Set<String> set) {
        String[] array = new String[set.size()];
        return set.toArray(array);
    }

    private void loadSettings(SQLiteDatabase db) {
        loadSystemSettings(db);
        loadSecureSettings(db);
        // The global table only exists for the 'owner/system' user
        if (mUserHandle == UserHandle.USER_SYSTEM) {
            loadGlobalSettings(db);
        }
    }

    private void loadSystemSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value)"
                    + " VALUES(?,?);");
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadSecureSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO secure(name,value)"
                    + " VALUES(?,?);");
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadGlobalSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO global(name,value)"
                    + " VALUES(?,?);");
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadSetting(SQLiteStatement stmt, String key, Object value) {
        stmt.bindString(1, key);
        stmt.bindString(2, value.toString());
        stmt.execute();
    }

    private void loadStringSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key, mContext.getResources().getString(resid));
    }

    private void loadBooleanSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key,
                mContext.getResources().getBoolean(resid) ? "1" : "0");
    }

    private void loadIntegerSetting(SQLiteStatement stmt, String key, int resid) {
        loadSetting(stmt, key,
                Integer.toString(mContext.getResources().getInteger(resid)));
    }

    private void loadFractionSetting(SQLiteStatement stmt, String key, int resid, int base) {
        loadSetting(stmt, key,
                Float.toString(mContext.getResources().getFraction(resid, base, base)));
    }

    private int getIntValueFromSystem(SQLiteDatabase db, String name, int defaultValue) {
        return getIntValueFromTable(db, TABLE_SYSTEM, name, defaultValue);
    }

    private int getIntValueFromTable(SQLiteDatabase db, String table, String name,
            int defaultValue) {
        String value = getStringValueFromTable(db, table, name, null);
        return (value != null) ? Integer.parseInt(value) : defaultValue;
    }

    private String getStringValueFromTable(SQLiteDatabase db, String table, String name,
            String defaultValue) {
        Cursor c = null;
        try {
            c = db.query(table, new String[] { SlimSettings.System.VALUE }, "name='" + name + "'",
                    null, null, null, null);
            if (c != null && c.moveToFirst()) {
                String val = c.getString(0);
                return val == null ? defaultValue : val;
            }
        } finally {
            if (c != null) c.close();
        }
        return defaultValue;
    }

    private String getOldDefaultDeviceName() {
        return mContext.getResources().getString(R.string.def_device_name,
                Build.MANUFACTURER, Build.MODEL);
    }

    private String getDefaultDeviceName() {
        return mContext.getResources().getString(R.string.def_device_name_simple, Build.MODEL);
    }
}
