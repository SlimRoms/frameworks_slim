/*
 * Copyright (C) 2016 The CyanogenMod Project
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
package slim.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import slim.provider.SlimSettings;

public class SettingsHelper {

    private static final String SETTINGS_GLOBAL = Settings.Global.CONTENT_URI.toString();
    private static final String SETTINGS_SECURE = Settings.Secure.CONTENT_URI.toString();
    private static final String SETTINGS_SYSTEM = Settings.System.CONTENT_URI.toString();

    private static final String SLIMSETTINGS_GLOBAL = SlimSettings.Global.CONTENT_URI.toString();
    private static final String SLIMSETTINGS_SECURE = SlimSettings.Secure.CONTENT_URI.toString();
    private static final String SLIMSETTINGS_SYSTEM = SlimSettings.System.CONTENT_URI.toString();

    private static SettingsHelper sInstance;

    private final Context mContext;
    private final Observatory mObservatory;

    private SettingsHelper(Context context) {
        mContext = context;
        mObservatory = new Observatory(context, new Handler());
    }

    public static synchronized SettingsHelper get(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsHelper(context);
        }
        return sInstance;
    }

    public String getString(Uri settingsUri) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getStringForUser(resolver,
                    settingsUri.getLastPathSegment(), UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getStringForUser(resolver,
                    settingsUri.getLastPathSegment(), UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getString(resolver, settingsUri.getLastPathSegment());
        } else if (uri.startsWith(SLIMSETTINGS_SECURE)) {
            return SlimSettings.Secure.getStringForUser(resolver,
                    settingsUri.getLastPathSegment(), UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_SYSTEM)) {
            return SlimSettings.System.getStringForUser(resolver,
                    settingsUri.getLastPathSegment(), UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_GLOBAL)) {
            return SlimSettings.Global.getString(resolver, settingsUri.getLastPathSegment());
        }
        return null;
    }

    public int getInt(Uri settingsUri, int def) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            return Settings.Secure.getIntForUser(resolver,
                    settingsUri.getLastPathSegment(), def, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            return Settings.System.getIntForUser(resolver,
                    settingsUri.getLastPathSegment(), def, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            return Settings.Global.getInt(resolver, settingsUri.getLastPathSegment(), def);
        } else if (uri.startsWith(SLIMSETTINGS_SECURE)) {
            return SlimSettings.Secure.getIntForUser(resolver,
                    settingsUri.getLastPathSegment(), def, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_SYSTEM)) {
            return SlimSettings.System.getIntForUser(resolver,
                    settingsUri.getLastPathSegment(), def, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_GLOBAL)) {
            return SlimSettings.Global.getInt(resolver, settingsUri.getLastPathSegment(), def);
        }
        return def;
    }

    public boolean getBoolean(Uri settingsUri, boolean def) {
        int value = getInt(settingsUri, def ? 1 : 0);
        return value == 1;
    }

    public void putString(Uri settingsUri, String value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putStringForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putStringForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putString(resolver, settingsUri.getLastPathSegment(), value);
        } else if (uri.startsWith(SLIMSETTINGS_SECURE)) {
            SlimSettings.Secure.putStringForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_SYSTEM)) {
            SlimSettings.System.putStringForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_GLOBAL)) {
            SlimSettings.Global.putString(resolver, settingsUri.getLastPathSegment(), value);
        }
    }

    public void putInt(Uri settingsUri, int value) {
        final String uri = settingsUri.toString();
        final ContentResolver resolver = mContext.getContentResolver();

        if (uri.startsWith(SETTINGS_SECURE)) {
            Settings.Secure.putIntForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_SYSTEM)) {
            Settings.System.putIntForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SETTINGS_GLOBAL)) {
            Settings.Global.putInt(resolver, settingsUri.getLastPathSegment(), value);
        } else if (uri.startsWith(SLIMSETTINGS_SECURE)) {
            SlimSettings.Secure.putIntForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_SYSTEM)) {
            SlimSettings.System.putIntForUser(resolver,
                    settingsUri.getLastPathSegment(), value, UserHandle.USER_CURRENT);
        } else if (uri.startsWith(SLIMSETTINGS_GLOBAL)) {
            SlimSettings.Global.putInt(resolver, settingsUri.getLastPathSegment(), value);
        }
    }

    public void putBoolean(Uri settingsUri, boolean value) {
        putInt(settingsUri, value ? 1 : 0);
    }

    public void startWatching(OnSettingsChangeListener listener, Uri... settingsUris) {
        mObservatory.register(listener, settingsUris);
    }

    public void stopWatching(OnSettingsChangeListener listener) {
        mObservatory.unregister(listener);
    }

    public interface OnSettingsChangeListener {
        public void onSettingsChanged(Uri settingsUri);
    }

    /**
     * A scalable ContentObserver that aggregates all listeners thru a single entrypoint.
     */
    private static class Observatory extends ContentObserver {

        private final Map<OnSettingsChangeListener, Set<Uri>> mTriggers = new ArrayMap<>();
        private final List<Uri> mRefs = new ArrayList<>();

        private final Context mContext;
        private final ContentResolver mResolver;

        public Observatory(Context context, Handler handler) {
            super(handler);
            mContext = context;
            mResolver = mContext.getContentResolver();
        }

        public void register(OnSettingsChangeListener listener, Uri... contentUris) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.get(listener);
                if (uris == null) {
                    uris = new ArraySet<Uri>();
                    mTriggers.put(listener, uris);
                }
                for (Uri contentUri : contentUris) {
                    uris.add(contentUri);
                    if (!mRefs.contains(contentUri)) {
                        mResolver.registerContentObserver(contentUri,
                                false, this, UserHandle.USER_ALL);
                        listener.onSettingsChanged(contentUri);
                    }
                    mRefs.add(contentUri);
                }
            }
        }

        public void unregister(OnSettingsChangeListener listener) {
            synchronized (mRefs) {
                Set<Uri> uris = mTriggers.remove(listener);
                if (uris != null) {
                    for (Uri uri : uris) {
                        mRefs.remove(uri);
                    }
                }
                if (mRefs.size() == 0) {
                    mResolver.unregisterContentObserver(this);
                }
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (mRefs) {
                super.onChange(selfChange, uri);

                final Set<OnSettingsChangeListener> notify = new ArraySet<>();
                for (Map.Entry<OnSettingsChangeListener, Set<Uri>> entry : mTriggers.entrySet()) {
                    if (entry.getValue().contains(uri)) {
                        notify.add(entry.getKey());
                    }
                }

                for (OnSettingsChangeListener listener : notify) {
                    listener.onSettingsChanged(uri);
                }
            }
        }
    }
}
