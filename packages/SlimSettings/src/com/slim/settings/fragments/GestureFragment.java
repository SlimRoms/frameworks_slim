package com.slim.settings.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;

import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.activities.ShortcutPickerActivity;

import java.util.LinkedHashMap;

import slim.action.ActionsArray;
import slim.action.ActionConstants;
import slim.preference.SlimPreference;
import slim.preference.SlimPreferenceManager;
import slim.utils.ShortcutPickerHelper;

import org.slim.framework.internal.policy.keyparser.Key;
import org.slim.framework.internal.policy.keyparser.KeyCategory;
import org.slim.framework.internal.policy.keyparser.KeyParser;

public class GestureFragment extends SettingsPreferenceFragment implements
        ShortcutPickerHelper.OnPickListener {

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private LinkedHashMap<String, KeyCategory> mKeys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        mKeys = KeyParser.parseKeys(getActivity());

        setPreferenceScreen(reloadSettings());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected int getMetricsCategory() {
        return 1;
    }

    private PreferenceScreen reloadSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs == null) {
            prefs = getPreferenceManager().createPreferenceScreen(getContext());
        } else {
            prefs.removeAll();
        }

        for (KeyCategory category : mKeys.values()) {
            if (category.keys.size() > 3) {
                SlimPreference pref = new SlimPreference(getContext());
                pref.setFragment("com.slim.settings.fragments.GestureFragment$KeyCategoryFragment");
                pref.getExtras().putString("key", category.key);
                pref.setTitle(category.name);
                pref.setKey(category.key);
                prefs.addPreference(pref);
            } else {
                PreferenceCategory cat = new PreferenceCategory(getContext());
                cat.setTitle(category.name);
                cat.setKey(category.key);
                prefs.addPreference(cat);
                for (Key key : category.keys) {
                    KeyPreference pref = new KeyPreference(getContext(),
                        key.scancode, key.name, key.def);
                    cat.addPreference(pref);
                }
            }
        }
        return prefs;
    }

    @Override
    public void shortcutPicked(String action, String description,
            Bitmap bmp, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        SlimPreferenceManager.putStringInSlimSettings(getContext(), 0, mPendingSettingsKey, action);
        reloadSettings();
        mPendingSettingsKey = null;
    }

    private static class KeyPreference extends ListPreference {
        private final Context mContext;
        private final int mScanCode;

        public KeyPreference(Context context,
                int scanCode, String title, String defaultAction) {
            super(context);
            mContext = context;
            mScanCode = scanCode;
            ActionsArray mActionsArray = new ActionsArray(mContext, true);
            setTitle(title);
            setKey(KeyParser.getPreferenceKey(mScanCode));
            setEntries(mActionsArray.getEntries());
            setEntryValues(mActionsArray.getValues());
            setDefaultValue(defaultAction);
            setSummary("%s");
            setDialogTitle(title);
        }

        @Override
        public boolean callChangeListener(final Object newValue) {
            final String action = String.valueOf(newValue);
            if (action.equals(ActionConstants.ACTION_APP)) {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getStringExtra(ShortcutPickerHelper.EXTRA_ACTION);
                        callChangeListener(action);
                        mContext.unregisterReceiver(this);
                    }
                }, new IntentFilter(ShortcutPickerHelper.ACTION_SHORTCUT_PICKED));
                mContext.startActivity(new Intent(mContext, ShortcutPickerActivity.class));
                return false;
            }
            return super.callChangeListener(newValue);
        }

        @Override
        protected String getPersistedString(String defValue) {
            if (!shouldPersist()) {
                return defValue;
            }
            return SlimPreferenceManager.getStringFromSlimSettings(getContext(), 0, getKey(),
                defValue);
        }

        @Override
        protected boolean persistString(String value) {
            if (shouldPersist()) {
                if (TextUtils.equals(value, getPersistedString(null))) {
                    return true;
                }
                SlimPreferenceManager.putStringInSlimSettings(getContext(),
                        0, getKey(), value);
                return true;
            }
            return false;
        }

        @Override
        protected boolean isPersisted() {
            return SlimPreferenceManager.settingExists(getContext(), 0, getKey());
        }
    }

    public static class KeyCategoryFragment extends SettingsPreferenceFragment {

        KeyCategory mCategory;

        public KeyCategoryFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mCategory = KeyParser.parseKeys(getActivity()).get(getArguments().getString("key"));

            setPreferenceScreen(reloadSettings());
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected int getMetricsCategory() {
            return 1;
        }

        private PreferenceScreen reloadSettings() {
            PreferenceScreen prefs = getPreferenceScreen();
            if (prefs == null) {
                prefs = getPreferenceManager().createPreferenceScreen(getContext());
            } else {
                prefs.removeAll();
            }
            for (Key key : mCategory.keys) {
                KeyPreference pref = new KeyPreference(getContext(),
                        key.scancode, key.name, key.def);
                prefs.addPreference(pref);
            }
            return prefs;
        }
    }
}
