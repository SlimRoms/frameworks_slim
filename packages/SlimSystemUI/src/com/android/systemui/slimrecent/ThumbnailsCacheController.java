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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.ArrayList;

/**
 * This class is our LRU cache controller. It holds
 * the task screenshots.
 *
 */
public class ThumbnailsCacheController {

    private final static String TAG = "RecentCacheController";

    /**
     * Singleton.
     */
    private static ThumbnailsCacheController sInstance;

    /**
     * Memory Cache.
     */
    protected LruCache<String, Bitmap> mMemoryCache;

    private Context mContext;
    private int mMaxMemory;

    // Array list of all current keys.
    private final ArrayList<String> mKeys = new ArrayList<String>();

    /**
     * Get the instance.
     */
    public static ThumbnailsCacheController getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        } else {
            return sInstance = new ThumbnailsCacheController(context);
        }
    }

    /**
     * Constructor.
     * Defines the LRU cache size.
     */
    private ThumbnailsCacheController(Context context) {
        mContext = context;

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        mMaxMemory = maxMemory;

        int cacheSize = maxMemory / 4;

        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }

                @Override
                protected void entryRemoved(boolean evicted, String key,
                        Bitmap oldBitmap, Bitmap newBitmap) {
                }
            };
        }
    }

    /**
     * Add the bitmap to the LRU cache.
     */
    protected void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (key != null && bitmap != null) {
            if (key.startsWith(RecentPanelView.TASK_PACKAGE_IDENTIFIER)) {
                mKeys.add(key);
            }
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * Get the bitmap from the LRU cache.
     */
    protected Bitmap getBitmapFromMemCache(String key) {
        if (key == null) {
            return null;
        }
        return mMemoryCache.get(key);
    }

    /**
     * Remove a bitmap from the LRU cache.
     */
    protected Bitmap removeBitmapFromMemCache(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith(RecentPanelView.TASK_PACKAGE_IDENTIFIER)) {
            mKeys.remove(key);
        }
        return mMemoryCache.remove(key);
    }

    /**
     * Wether to clear the whole cache
     */
    public void clearCache() {
        mMemoryCache.evictAll();
    }

    public void removeThumb(String key) {
        removeBitmapFromMemCache(key);
    }

    /** Trims the cache to a specific size */
    final void trimToSize(int cacheSize) {
        mMemoryCache.trimToSize(cacheSize);
    }

    public int getMaxMemory() {
        return mMaxMemory;
    }
}
