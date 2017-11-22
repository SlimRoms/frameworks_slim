/*
 * Copyright (C) 2014-2017 SlimRoms Project
 * Copyright (C) 2017 ABC rom
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.systemui.slimrecent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.util.LruCache;

import java.util.ArrayList;

/**
 * This class is our LRU cache controller. It holds
 * tasks activity infos.
 *
 */
public class InfosCacheController {

    private final static String TAG = "RecentCacheController";

    /**
     * Singleton.
     */
    private static InfosCacheController sInstance;

    /**
     * Memory Cache.
     */
    protected LruCache<String, ActivityInfo> mMemoryCache;

    private Context mContext;

    // Array list of all current keys.
    private final ArrayList<String> mKeys = new ArrayList<String>();

    /**
     * Get the instance.
     */
    public static InfosCacheController getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        } else {
            return sInstance = new InfosCacheController(context);
        }
    }

    /**
     * Constructor.
     * Defines the LRU cache size.
     */
    private InfosCacheController(Context context) {
        mContext = context;

        int cacheSize = 25;

        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, ActivityInfo>(cacheSize);
        }

        // Receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    /**
     * Listen for package change or added broadcast.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                // Get the component name from the intent.
                final String[] components = intent.getStringArrayExtra(
                    Intent.EXTRA_CHANGED_COMPONENT_NAME_LIST);
                if (components == null) {
                    return;
                }
                final ArrayList<String> keysToRemove = new ArrayList<String>();
                for (String key : mKeys) {
                    for (String cn : components) {
                        if (key.toLowerCase().contains(cn.toLowerCase())) {
                            keysToRemove.add(key);
                        }
                    }
                }
                for (String key : keysToRemove) {
                    removeInfosFromMemCache(key);
                }
            }
        }
    };

    /**
     * Add the info to the LRU cache.
     */
    protected void addInfosToMemoryCache(String key, ActivityInfo info) {
        if (key != null && info != null) {
            mKeys.add(key);
            mMemoryCache.put(key, info);
        }
    }

    /**
     * Get the info from the LRU cache.
     */
    protected ActivityInfo getInfosFromMemCache(String key) {
        if (key == null) {
            return null;
        }
        return mMemoryCache.get(key);
    }

    /**
     * Remove a info from the LRU cache.
     */
    protected ActivityInfo removeInfosFromMemCache(String key) {
        if (key == null) {
            return null;
        }
        mKeys.remove(key);
        return mMemoryCache.remove(key);
    }

    /**
     * Wether to clear the whole cache
     */
    public void clearCache() {
        mMemoryCache.evictAll();
    }

    public void removeInfos(String key) {
        removeInfosFromMemCache(key);
    }

    /** Trims the cache to a specific size */
    final void trimToSize(int cacheSize) {
        mMemoryCache.trimToSize(cacheSize);
    }
}
