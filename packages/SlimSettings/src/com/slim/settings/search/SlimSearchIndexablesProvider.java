package com.slim.settings.search;

import android.content.ComponentName;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesProvider;
import android.util.ArraySet;
import android.util.Log;

import com.slim.settings.R;
import com.slim.settings.search.Searchable.SearchIndexProvider;
import com.slim.settings.search.SettingsList.SettingInfo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ENTRIES;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEY;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_KEYWORDS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SCREEN_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_TITLE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_USER_ID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

public class SlimSearchIndexablesProvider extends SearchIndexablesProvider {

    private static final String TAG = SlimSearchIndexablesProvider.class.getSimpleName();

    private static final String FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER =
        "SEARCH_INDEX_DATA_PROVIDER";

    public static final String SLIM_SETTINGS_PACKAGE = "com.slim.settings";

    public static final ComponentName SLIM_SETTINGS_ACTIVITY = new ComponentName(
            SLIM_SETTINGS_PACKAGE, SLIM_SETTINGS_PACKAGE + ".SettingsActivity");


    @Override
    public Cursor queryXmlResources(String[] strings) {
        Log.d("TEST", "queryXmlResources - " + strings);
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        final Set<String> keys = SettingsList.get(getContext()).getSettings();

        for (String key : keys) {
            SettingInfo info = SettingsList.get(getContext()).getSetting(key);
            if (info == null || info.xmlResId <= 0) {
                Log.d("TEST", "null continiuning");
                continue;
            }

            Object[] ref = new Object[7];
            ref[COLUMN_INDEX_XML_RES_RANK] = 2;
            ref[COLUMN_INDEX_XML_RES_RESID] = info.xmlResId;
            ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = null;
            ref[COLUMN_INDEX_XML_RES_ICON_RESID] = info.iconResId <= 0 ?
                    R.drawable.ic_settings_interface : info.iconResId;
            ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = info.getAction();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] =
                    SLIM_SETTINGS_ACTIVITY.getPackageName();
            ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = SLIM_SETTINGS_ACTIVITY.getClassName();
            cursor.addRow(ref);
        }
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] strings) {
        Log.d("TEST", "queryRawData - " + strings);
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        final Set<String> keys = SettingsList.get(getContext()).getSettings();

        // we also submit keywords and metadata for all top-level items
        // which don't have an associated XML resource
        for (String key : keys) {
            SettingInfo i = SettingsList.get(getContext()).getSetting(key);
            if (i == null) {
                continue;
            }

            // look for custom keywords
            SearchIndexProvider sip = getSearchIndexProvider(i.fragmentClass);
            if (sip == null) {
                continue;
            }

            // don't create a duplicate entry if no custom keywords are provided
            // and a resource was already indexed
            List<SearchIndexableRaw> rawList = sip.getRawDataToIndex(getContext());
            if (rawList == null || rawList.size() == 0) {
                if (i.xmlResId > 0) {
                    continue;
                }
                rawList = Collections.singletonList(new SearchIndexableRaw(getContext()));
            }

            for (SearchIndexableRaw raw : rawList) {
                Object[] ref = new Object[14];
                ref[COLUMN_INDEX_RAW_RANK] = raw.rank > 0 ?
                        raw.rank : 2;
                ref[COLUMN_INDEX_RAW_TITLE] = raw.title != null ?
                        raw.title : i.title;
                ref[COLUMN_INDEX_RAW_SUMMARY_ON] = i.summary;
                ref[COLUMN_INDEX_RAW_KEYWORDS] = raw.keywords;
                ref[COLUMN_INDEX_RAW_ENTRIES] = raw.entries;
                ref[COLUMN_INDEX_RAW_SCREEN_TITLE] = raw.screenTitle != null ?
                        raw.screenTitle : i.title;
                ref[COLUMN_INDEX_RAW_ICON_RESID] = raw.iconResId > 0 ? raw.iconResId :
                        (i.iconResId > 0 ? i.iconResId : R.drawable.ic_settings_interface);
                ref[COLUMN_INDEX_RAW_INTENT_ACTION] = raw.intentAction != null ?
                        raw.intentAction : i.getAction();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_PACKAGE] = raw.intentTargetPackage != null ?
                        raw.intentTargetPackage : SLIM_SETTINGS_ACTIVITY.getPackageName();
                ref[COLUMN_INDEX_RAW_INTENT_TARGET_CLASS] = raw.intentTargetClass != null ?
                        raw.intentTargetClass : SLIM_SETTINGS_ACTIVITY.getClassName();
                ref[COLUMN_INDEX_RAW_KEY] = raw.key != null ?
                        raw.key : i.key;
                ref[COLUMN_INDEX_RAW_USER_ID] = -1;
                cursor.addRow(ref);
            }
        }
        return cursor;
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] strings) {
        MatrixCursor cursor = new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);

        final Set<String> keys = SettingsList.get(getContext()).getSettings();
        final Set<String> nonIndexables = new ArraySet<>();

        for (String key : keys) {
            SettingInfo i = SettingsList.get(getContext()).getSetting(key);
            if (i == null) {
                continue;
            }

            // look for non-indexable keys
            SearchIndexProvider sip = getSearchIndexProvider(i.fragmentClass);
            if (sip == null) {
                continue;
            }

            Set<String> nik = sip.getNonIndexableKeys(getContext());
            if (nik == null) {
                continue;
            }

            nonIndexables.addAll(nik);
        }

        for (String nik : nonIndexables) {
            Object[] ref = new Object[1];
            ref[COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE] = nik;
            cursor.addRow(ref);
        }
        return cursor;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private SearchIndexProvider getSearchIndexProvider(final String className) {

        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Cannot find class: " + className);
            return null;
        }

        if (clazz == null || !Searchable.class.isAssignableFrom(clazz)) {
            return null;
        }

        try {
            final Field f = clazz.getField(FIELD_NAME_SEARCH_INDEX_DATA_PROVIDER);
            return (SearchIndexProvider) f.get(null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
