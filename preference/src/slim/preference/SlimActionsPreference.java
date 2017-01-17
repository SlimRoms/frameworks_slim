package slim.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import slim.action.ActionsArray;
import slim.action.ActionConstants;
import slim.utils.AttributeHelper;
import slim.utils.ShortcutPickerHelper;

import java.util.Arrays;

public class SlimActionsPreference extends ListPreference implements
       ShortcutPickerHelper.OnPickListener  {

    private SlimPreferenceManager mSlimPreferenceManager = SlimPreferenceManager.getInstance();

    private int mSettingType;
    private String mListDependency;
    private String[] mListDependencyValues;

    private ShortcutPickerHelper mPicker;
    private ActionsArray mActionsArray;

    private boolean mPending = false;

    private SlimActionsPreferenceCallback mCallback;

    public interface SlimActionsPreferenceCallback {
        PreferenceFragment getPreferenceFragment();
    }

    public SlimActionsPreference(Context context, AttributeSet attrs) {
       super(context, attrs);

       AttributeHelper a = new AttributeHelper(context, attrs,
               slim.R.styleable.SlimActionsPreference);

        boolean allowWake = a.getBoolean(slim.R.styleable.SlimActionsPreference_showWake, false);
        boolean allowNone = a.getBoolean(slim.R.styleable.SlimActionsPreference_showNone, true);
        String[] removeActions = a.getString(slim.R.styleable.SlimActionsPreference_removeActions,
            "").split("\\|");

        mActionsArray = new ActionsArray(context, allowNone, allowWake, Arrays.asList(removeActions));

        a = new AttributeHelper(context, attrs, slim.R.styleable.SlimPreference);

        mSettingType = SlimPreferenceManager.getSettingType(a);

        String list = a.getString(slim.R.styleable.SlimPreference_listDependency);
        if (!TextUtils.isEmpty(list)) {
            String[] listParts = list.split(":");
            mListDependency = listParts[0];
            mListDependencyValues = listParts[1].split("\\|");
        }

        boolean hidePreference =
                a.getBoolean(slim.R.styleable.SlimPreference_hidePreference, false);
        int hidePreferenceInt = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceInt, -1);
        int intDep = a.getInt(slim.R.styleable.SlimPreference_hidePreferenceIntDependency, 0);
        if (hidePreference || hidePreferenceInt == intDep) {
            setVisible(false);
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        if (mListDependency != null) {
            mSlimPreferenceManager.registerListDependent(
                    this, mListDependency, mListDependencyValues);
        }
        setEntries(mActionsArray.getEntries());
        setEntryValues(mActionsArray.getValues());
    }

    @Override
    public void onDetached() {
        super.onDetached();
        if (mListDependency != null) {
            mSlimPreferenceManager.unregisterListDependent(this, mListDependency);
        }
    }

    public void setCallback(SlimActionsPreferenceCallback callback) {
        mCallback = callback;

        mPicker = new ShortcutPickerHelper(mCallback.getPreferenceFragment().getActivity(), this);
    }

    @Override
    public void shortcutPicked(String action, String description,
            Bitmap bmp, boolean isApplication) {
        mPending = false;
        if (action == null) {
            return;
        }

        SlimPreferenceManager.putStringInSlimSettings(getContext(),
                mSettingType, getKey(), action);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && mPending) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPending = false;
        }
    }

    @Override
    public boolean persistString(String value) {
        if (shouldPersist()) {
            if (TextUtils.equals(value, getPersistedString(null))) {
                return true;
            }
            if (value.equals(ActionConstants.ACTION_APP)) {
                if (mPicker != null) {
                    mPending = true;
                    mPicker.pickShortcut(mCallback.getPreferenceFragment().getId());
                }
            } else {
                SlimPreferenceManager.putStringInSlimSettings(getContext(),
                        mSettingType, getKey(), value);
            }
            return true;
        }
        return false;
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return SlimPreferenceManager.getStringFromSlimSettings(getContext(), mSettingType, getKey(),
                defaultReturnValue);
    }

    @Override
    protected boolean isPersisted() {
        return SlimPreferenceManager.settingExists(getContext(), mSettingType, getKey());
    }
}
