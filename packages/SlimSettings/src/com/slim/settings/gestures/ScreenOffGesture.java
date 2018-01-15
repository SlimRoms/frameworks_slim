/*
 * Copyright (C) 2014 Slimroms
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

package com.slim.settings.gestures;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import slim.action.ActionsArray;
import slim.action.ActionConstants;
import slim.action.ActionHelper;
import slim.utils.AppHelper;
import slim.utils.ImageHelper;

import java.io.File;

import com.slim.settings.SettingsPreferenceFragment;
import com.slim.settings.util.KernelControl;
import com.slim.settings.R;
import com.slim.settings.gestures.TouchscreenGestureParser.Gesture;
import com.slim.settings.gestures.TouchscreenGestureParser.GesturesArray;

import slim.utils.ShortcutPickerHelper;
import org.slim.framework.internal.logging.SlimMetricsLogger;

public class ScreenOffGesture extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String SLIM_METADATA_NAME = "org.slim.framework";

    public static final String GESTURE_SETTINGS = "screen_off_gesture_settings";

    public static final String PREF_GESTURE_ENABLE = "enable_gestures";

    private static final int DLG_RESET_TO_DEFAULT    = 1;

    private static final int MENU_RESET = Menu.FIRST;

    private SwitchPreference mEnableGestures;

    private SharedPreferences mScreenOffGestureSharedPreferences;

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private ActionsArray mActionsArray;
    private GesturesArray mGestures;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        mScreenOffGestureSharedPreferences = getActivity().getSharedPreferences(
                GESTURE_SETTINGS, Activity.MODE_PRIVATE);

        mActionsArray = new ActionsArray(getActivity(), true);

        mGestures = TouchscreenGestureParser.parseGestures(getActivity());

        // Attach final settings screen.
        reloadSettings();

        setHasOptionsMenu(true);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.SCREEN_OFF_GESTURES;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    private PreferenceScreen reloadSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.screen_off_gesture);
        prefs = getPreferenceScreen();

        mEnableGestures = (SwitchPreference) prefs.findPreference(PREF_GESTURE_ENABLE);

        boolean enableGestures =
                mScreenOffGestureSharedPreferences.getBoolean(PREF_GESTURE_ENABLE, true);
        mEnableGestures.setChecked(enableGestures);
        mEnableGestures.setOnPreferenceChangeListener(this);

        PreferenceCategory gestures = (PreferenceCategory) prefs.findPreference("gestures");
        
        for (Gesture gesture : mGestures) {
            if (new File(gesture.path).exists()) {
                GesturePreference pref = new GesturePreference(getContext(),
                        gesture.scancode, gesture.name, gesture.def);
                gestures.addPreference(pref);
            }
        }

        return prefs;
    }

    private String getDescription(String action) {
        if (mActionsArray == null || action == null) {
            return null;
        }
        int i = 0;
        for (String actionValue : mActionsArray.getValues()) {
            if (action.equals(actionValue)) {
                return mActionsArray.getEntries()[i];
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableGestures) {
            mScreenOffGestureSharedPreferences.edit()
                    .putBoolean(PREF_GESTURE_ENABLE, (Boolean) newValue).commit();
            KernelControl.enableGestures(getActivity(), (Boolean) newValue);
            return true;
        }
        return false;
    }

    // Reset all entries to default.
    private void resetToDefault(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                GESTURE_SETTINGS, Activity.MODE_PRIVATE).edit();
         editor.putBoolean(PREF_GESTURE_ENABLE, true);
        for (Gesture gesture : mGestures) {
            editor.putString(buildPreferenceKey(gesture.scancode), gesture.def);
        }
        editor.apply();
        KernelControl.enableGestures(getActivity(), true);
        reloadSettings();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        mScreenOffGestureSharedPreferences.edit().putString(mPendingSettingsKey, action).commit();
        reloadSettings();
        mPendingSettingsKey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingSettingsKey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(org.slim.framework.internal.R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String settingsKey, int dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, int dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putInt("dialogTitle", dialogTitle);
            frag.setArguments(args);
            return frag;
        }

        ScreenOffGesture getOwner() {
            return (ScreenOffGesture) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String settingsKey = getArguments().getString("settingsKey");
            int dialogTitle = getArguments().getInt("dialogTitle");
            switch (id) {
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault(getOwner().getActivity());
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

    public static String buildPreferenceKey(int id) {
        return "gesture_" + Integer.toString(id);
    }

    private class GesturePreference extends ListPreference {
        private final Context mContext;
        private final int mScanCode;

        public GesturePreference(Context context,
                int scanCode, String title, String defaultAction) {
            super(context);
            mContext = context;
            mScanCode = scanCode;

            setTitle(title);
            setKey(buildPreferenceKey(scanCode));

            Log.d("TEST", "title - " + title);
            String action = getPersistedString(defaultAction);
            Log.d("TEST", "action - " + action);


            setEntries(mActionsArray.getEntries());
            setEntryValues(mActionsArray.getValues());
            setDefaultValue(defaultAction);
            Drawable d = ActionHelper.getActionIconImage(mContext, action, null);
            d = d.getConstantState().newDrawable().mutate();
            if (action.startsWith("**")) {
                setSummary("%s");
                d = ImageHelper.getColoredDrawable(d, 0xff000000);
            } else {
                setSummary(AppHelper.getFriendlyNameForUri(mContext, mContext.getPackageManager(),
                        action));
            }
            setIcon(d);
            setDialogTitle(title);
        }

        @Override
        public boolean callChangeListener(final Object newValue) {
            final String action = String.valueOf(newValue);
            if (action.equals(ActionConstants.ACTION_APP)) {
                mPendingSettingsKey = getKey();
                mPicker.pickShortcut(getId());
                return false;
            }
            return super.callChangeListener(newValue);
        }

        @Override
        protected String getPersistedString(String defValue) {
            if (!shouldPersist()) {
                return defValue;
            }
            return mScreenOffGestureSharedPreferences.getString(getKey(), defValue);
        }

        @Override
        protected boolean shouldPersist() {
            return true;
        }

        @Override
        protected boolean persistString(String value) {
            mScreenOffGestureSharedPreferences.edit()
                    .putString(getKey(), value).apply();
            return true;
        }
      
        @Override
        protected boolean isPersisted() {
            return mScreenOffGestureSharedPreferences.contains(getKey());
        }
    }
}
