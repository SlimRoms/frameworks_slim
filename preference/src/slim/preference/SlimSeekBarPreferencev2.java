/*
 * Copyright (C) 2016 The Dirty Unicorns Project
 * Copyright (C) 2015-2017 SlimRoms Project
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
 * limitations under the License
 */

package slim.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewParent;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.v7.preference.*;

import slim.R;
import slim.utils.AttributeHelper;

public class SlimSeekBarPreferencev2 extends Preference
        implements SeekBar.OnSeekBarChangeListener {

    private final String TAG = getClass().getName();
    private static final int DEFAULT_VALUE = 50;

    private int mMin = 0;
    private int mInterval = 1;
    private int mCurrentValue;
    private int mDefaultValue = -1;
    private int mMax = 100;
    private String mUnits = "%";
    private SeekBar mSeekBar;
    private TextView mTitle;
    private TextView mStatusText;

    public SlimSeekBarPreferencev2(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(org.slim.framework.internal.R.layout.slim_seek_bar_preference_v2);

        AttributeHelper a = new AttributeHelper(context, attrs,
                R.styleable.SlimSeekBarPreferencev2);

        mMax = a.getInt(R.styleable.SlimSeekBarPreferencev2_maxValue, mMax);
        mMin = a.getInt(R.styleable.SlimSeekBarPreferencev2_minValue, mMin);
        mDefaultValue = a.getInt(R.styleable.SlimSeekBarPreferencev2_defValue, mDefaultValue);
        mUnits = a.getString(R.styleable.SlimSeekBarPreferencev2_units);
        mInterval = a.getInt(R.styleable.SlimSeekBarPreferencev2_interval, mInterval);

        mSeekBar = new SeekBar(context, attrs);
        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public SlimSeekBarPreferencev2(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SlimSeekBarPreferencev2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlimSeekBarPreferencev2(Context context) {
        this(context, null);
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        this.setShouldDisableView(true);
        if (mTitle != null)
            mTitle.setEnabled(!disableDependent);
        if (mSeekBar != null)
            mSeekBar.setEnabled(!disableDependent);
        if (mStatusText != null)
            mStatusText.setEnabled(!disableDependent);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        try
        {
            // move our seekbar to the new view we've been given
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);

            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error binding view: " + ex.toString());
        }
        mStatusText = (TextView) view.findViewById(R.id.seekBarPrefValue);
        mStatusText.setText(String.valueOf(mCurrentValue) + mUnits);
        mStatusText.setMinimumWidth(30);
        mSeekBar.setProgress(mCurrentValue - mMin);
        mTitle = (TextView) view.findViewById(android.R.id.title);
    }

    public void setMax(int max) {
        mMax = max;
    }

    public void setMin(int min) {
        mMin = min;
    }

    public void setIntervalValue(int value) {
        mInterval = value;
    }

    public void setValue(int value) {
        mCurrentValue = value;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMin;
        if (newValue > mMax)
            newValue = mMax;
        else if (newValue < mMin)
            newValue = mMin;
        else if (mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(((float) newValue) / mInterval) * mInterval;

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.setProgress(mCurrentValue - mMin);
            return;
        }
        // change accepted, store it
        mCurrentValue = newValue;
        if (mStatusText != null) {
            mStatusText.setText(String.valueOf(newValue) + mUnits);
        }
        persistInt(newValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        }
        else {
            int temp = 0;
            try {
                temp = (Integer) defaultValue;
            } catch (Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }
            persistInt(temp);
            mCurrentValue = temp;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mSeekBar != null && mStatusText != null && mTitle != null) {
            mSeekBar.setEnabled(enabled);
            mStatusText.setEnabled(enabled);
            mTitle.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }
}
