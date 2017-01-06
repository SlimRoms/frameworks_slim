/*
 * Copyright (C) 2013-2017 SlimRoms Project
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

package com.slim.settings.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.slim.settings.R;
import com.slim.settings.fragments.SlimPreferenceFragment;
import com.slim.settings.Utils;

import java.util.Date;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import slim.preference.colorpicker.ColorPickerPreference;
import slim.provider.SlimSettings;

public class StatusBarClockStyle extends SlimPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarClockStyle";

    private static final String PREF_AM_PM_STYLE = "statusbar_clock_am_pm_style";
    private static final String PREF_CLOCK_DATE_STYLE = "clock_date_style";
    private static final String PREF_CLOCK_DATE_POSITION = "clock_date_position";
    private static final String PREF_CLOCK_DATE_FORMAT = "clock_date_format";

    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private ListPreference mClockDateStyle;
    private ListPreference mClockDatePosition;
    private ListPreference mClockDateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen prefSet = getPreferenceScreen();

        findPreference(PREF_AM_PM_STYLE).setEnabled(!DateFormat.is24HourFormat(getActivity()));

        mClockDateStyle = (ListPreference) findPreference(PREF_CLOCK_DATE_STYLE);
        mClockDateStyle.setOnPreferenceChangeListener(this);
        mClockDateStyle.setValue(Integer.toString(SlimSettings.System.getInt(getActivity()
                .getContentResolver(), SlimSettings.System.STATUSBAR_CLOCK_DATE_STYLE,
                0)));
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());

        mClockDatePosition = (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        mClockDatePosition.setOnPreferenceChangeListener(this);
        mClockDatePosition.setValue(Integer.toString(SlimSettings.System.getInt(getActivity()
                .getContentResolver(), SlimSettings.System.STATUSBAR_CLOCK_DATE_POSITION,
                0)));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());

        mClockDateFormat = (ListPreference) findPreference(PREF_CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

        boolean mClockDateToggle = SlimSettings.System.getInt(getActivity().getContentResolver(),
                    SlimSettings.System.STATUSBAR_CLOCK_DATE_DISPLAY, 0) != 0;
        if (!mClockDateToggle) {
            mClockDateStyle.setEnabled(false);
            mClockDatePosition.setEnabled(false);
            mClockDateFormat.setEnabled(false);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AlertDialog dialog;

        if (preference == mClockDateStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                    SlimSettings.System.STATUSBAR_CLOCK_DATE_STYLE, val);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDatePosition) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            SlimSettings.System.putInt(getActivity().getContentResolver(),
                    SlimSettings.System.STATUSBAR_CLOCK_DATE_POSITION, val);
            mClockDatePosition.setSummary(mClockDatePosition.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = SlimSettings.System.getString(
                    getActivity().getContentResolver(),
                    SlimSettings.System.STATUSBAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        SlimSettings.System.putString(getActivity().getContentResolver(),
                            SlimSettings.System.STATUSBAR_CLOCK_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    SlimSettings.System.putString(getActivity().getContentResolver(),
                        SlimSettings.System.STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        }
        return false;
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = SlimSettings.System.getInt(getActivity()
                .getContentResolver(), SlimSettings.System.STATUSBAR_CLOCK_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }
}
