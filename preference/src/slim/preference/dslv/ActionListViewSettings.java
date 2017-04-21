/*
 * Copyright (C) 2016-2017 SlimRoms Project
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

package slim.preference.dslv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import slim.action.ActionsArray;
import slim.action.ActionChecker;
import slim.action.ActionConfig;
import slim.action.ActionConstants;
import slim.action.ActionHelper;
import slim.utils.ImageHelper;
import slim.utils.DeviceUtils;
import slim.utils.ShortcutPickerHelper;

import org.slim.framework.internal.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class ActionListViewSettings extends ListFragment implements
            ShortcutPickerHelper.OnPickListener {

    private static final int DLG_SHOW_ACTION_DIALOG   = 0;
    private static final int DLG_SHOW_ICON_PICKER     = 1;
    private static final int DLG_DELETION_NOT_ALLOWED = 2;
    private static final int DLG_SHOW_HELP_SCREEN     = 3;
    private static final int DLG_RESET_TO_DEFAULT     = 4;
    private static final int DLG_HOME_WARNING_DIALOG  = 5;
    private static final int DLG_BACK_WARNING_DIALOG  = 6;

    private static final int MENU_HELP = Menu.FIRST;
    private static final int MENU_ADD = MENU_HELP + 1;
    private static final int MENU_RESET = MENU_ADD + 1;

    private static final int NAV_BAR               = 0;

    private static final int DEFAULT_MAX_ACTION_NUMBER = 5;

    public static final int REQUEST_PICK_CUSTOM_ICON = 1000;

    private int mActionMode;
    private int mMaxAllowedActions;
    private boolean mUseAppPickerOnly;
    private boolean mUseFullAppsOnly;
    private boolean mDisableLongpress;
    private boolean mDisableDoubleTap;
    private boolean mDisableIconPicker;
    private boolean mDisableDeleteLastEntry;

    private TextView mDisableMessage;

    private ActionConfigsAdapter mActionConfigsAdapter;

    private ArrayList<ActionConfig> mActionConfigs;
    private ActionConfig mActionConfig;

    private boolean mAdditionalFragmentAttached;
    private String mAdditionalFragment;
    private String mFragmentPrefXML;
    private View mDivider;

    private int mPendingIndex = -1;
    private boolean mPendingLongpress;
    private boolean mPendingDoubleTap;
    private boolean mPendingNewAction;

    private String[] mActionDialogValues;
    private String[] mActionDialogEntries;
    private String mActionValuesKey;
    private String mActionEntriesKey;

    private ActionConfig mTempActionConfig;

    private Activity mActivity;
    private ShortcutPickerHelper mPicker;

    private File mImageTmp;

    private DragSortListView.DropListener onDrop =
        new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ActionConfig item = mActionConfigsAdapter.getItem(from);

                mActionConfigsAdapter.remove(item);
                mActionConfigsAdapter.insert(item, to);

                setConfig(mActionConfigs, false);
            }
        };

    private DragSortListView.RemoveListener onRemove =
        new DragSortListView.RemoveListener() {
            @Override
            public void remove(int which) {
                ActionConfig item = mActionConfigsAdapter.getItem(which);
                mActionConfigsAdapter.remove(item);
                if (mDisableDeleteLastEntry && mActionConfigs.size() == 0) {
                    mActionConfigsAdapter.add(item);
                    showDialogInner(DLG_DELETION_NOT_ALLOWED, 0, false, false, false);
                } else if (!ActionChecker.containsAction(
                            mActivity, item, ActionConstants.ACTION_BACK)) {
                    mTempActionConfig = item;
                    showDialogInner(DLG_BACK_WARNING_DIALOG, which, false, false, false);
                } else if (!ActionChecker.containsAction(
                            mActivity, item, ActionConstants.ACTION_HOME)) {
                    mTempActionConfig = item;
                    showDialogInner(DLG_HOME_WARNING_DIALOG, which, false, false, false);
                } else {
                    setConfig(mActionConfigs, false);
                    deleteIconFileIfPresent(item, true);
                    if (mActionConfigs.size() == 0) {
                        showDisableMessage(true);
                    }
                }
            }
        };

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(org.slim.preference.R.layout.action_list_view_main, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Resources res = mActivity.getResources();

        mActionMode = getArguments().getInt("actionMode", NAV_BAR);
        mMaxAllowedActions = getArguments().getInt("maxAllowedActions", DEFAULT_MAX_ACTION_NUMBER);
        mAdditionalFragment = getArguments().getString("fragment", null);
        mFragmentPrefXML = getArguments().getString("fragment_pref_xml", null);
        mActionValuesKey = getArguments().getString("actionValues", "shortcut_action_values");
        mActionEntriesKey = getArguments().getString("actionEntries", "shortcut_action_entries");
        mDisableLongpress = getArguments().getBoolean("disableLongpress", false);
        mDisableDoubleTap = getArguments().getBoolean("disableDoubleTap", false);
        mUseAppPickerOnly = getArguments().getBoolean("useAppPickerOnly", false);
        mUseFullAppsOnly = getArguments().getBoolean("useOnlyFullAppPicker", false);
        mDisableIconPicker = getArguments().getBoolean("disableIconPicker", false);
        mDisableDeleteLastEntry = getArguments().getBoolean("disableDeleteLastEntry", false);

        mDisableMessage = (TextView) view.findViewById(R.id.disable_message);

        ActionsArray actionsArray = new ActionsArray(mActivity);
        mActionDialogValues = actionsArray.getValues();
        mActionDialogEntries = actionsArray.getEntries();

        mPicker = new ShortcutPickerHelper(mActivity, this);

        File folder = new File(Environment.getExternalStorageDirectory() + File.separator +
                ".slim" + File.separator + "icons");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        mImageTmp = new File(folder.toString()
                + File.separator + "shortcut.tmp");

        DragSortListView listView = (DragSortListView) getListView();

        listView.setDropListener(onDrop);
        listView.setRemoveListener(onRemove);

        mActionConfigs = getConfig();

        if (mActionConfigs != null) {
            mActionConfigsAdapter = new ActionConfigsAdapter(getContext(), mActionConfigs);
            setListAdapter(mActionConfigsAdapter);
            showDisableMessage(mActionConfigs.size() == 0);
        }

        listView.setOnItemTouchedCallback(new DragSortController.OnItemTouchedCallback() {
            @Override
            public boolean onDoubleTap(int position) {
                if (mDisableDoubleTap) return false;
                    if (mUseFullAppsOnly) {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = false;
                            mPendingDoubleTap = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId(), true);
                        }
                    } else if (!mUseAppPickerOnly) {
                        showDialogInner(DLG_SHOW_ACTION_DIALOG, position, false, true, false);
                    } else {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = false;
                            mPendingDoubleTap = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId());
                        }
                    }
                return true;
            }

            @Override
            public boolean onSingleClick(int position) {
                if (mUseFullAppsOnly) {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = false;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId(), true);
                        }
                    } else if (!mUseAppPickerOnly) {
                        ActionConfig actionConfig = mActionConfigsAdapter.getItem(position);
                        if (ActionConstants.ACTION_BACK.equals(
                                actionConfig.getClickAction())) {
                            showDialogInner(DLG_BACK_WARNING_DIALOG, position, false, false, true);
                        } else if (ActionConstants.ACTION_HOME.equals(
                                actionConfig.getClickAction())) {
                            showDialogInner(DLG_HOME_WARNING_DIALOG, position, false, false, true);
                        } else {
                            showDialogInner(DLG_SHOW_ACTION_DIALOG, position, false, false, false);
                        }
                    } else {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = false;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId());
                        }
                    }
                return true;
            }

            @Override
            public boolean onLongClick(int position) {
                if (mDisableLongpress) return false;
                    if (mUseFullAppsOnly) {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId(), true);
                        }
                    } else if (!mUseAppPickerOnly) {
                        showDialogInner(DLG_SHOW_ACTION_DIALOG, position, true, false, false);
                    } else {
                        if (mPicker != null) {
                            mPendingIndex = position;
                            mPendingLongpress = true;
                            mPendingNewAction = false;
                            mPicker.pickShortcut(getId());
                        }
                    }
                return true;
            }
        });

        mDivider = (View) view.findViewById(R.id.divider);
        loadAdditionalFragment();

        // get shared preference
        SharedPreferences preferences =
                mActivity.getSharedPreferences("dslv_settings", Activity.MODE_PRIVATE);
        if (!preferences.getBoolean("first_help_shown_mode_" + mActionMode, false)) {
            preferences.edit()
                    .putBoolean("first_help_shown_mode_" + mActionMode, true).commit();
            showDialogInner(DLG_SHOW_HELP_SCREEN, 0, false, false, false);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdditionalFragmentAttached) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (fragment != null && !fragmentManager.isDestroyed()) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }

    private void loadAdditionalFragment() {
        Log.d("TEST", "fragment - " + mAdditionalFragment);
        Log.d("TEST", "pref xml - " + mFragmentPrefXML);
        if (!TextUtils.isEmpty(mAdditionalFragment)) {
            try {
                Fragment fragment = Fragment.instantiate(getContext(), mAdditionalFragment);
                if (!TextUtils.isEmpty(mFragmentPrefXML)) {
                    Bundle b = new Bundle();
                    b.putString("preference_xml", mFragmentPrefXML);
                    fragment.setArguments(b);
                }
                getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();
                if (mDivider != null) {
                    mDivider.setVisibility(View.VISIBLE);
                }
                mAdditionalFragmentAttached = true;
            } catch (Exception e) {
                mAdditionalFragmentAttached = false;
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingIndex == -1) {
            return;
        }
        if (bmp != null && !mPendingLongpress && !mPendingDoubleTap) {
            // Icon is present, save it for future use and add the file path to the action.
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File folder = new File(Environment.getExternalStorageDirectory() + File.separator +
                        ".slim" + File.separator + "icons");
                folder.mkdirs();
                String fileName = folder.toString()
                        + File.separator + "shortcut_" + System.currentTimeMillis() + ".png";
                try {
                    FileOutputStream out = new FileOutputStream(fileName);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    action = action + "?hasExtraIcon=" + fileName;
                    File image = new File(fileName);
                    image.setReadable(true, false);
                }
            }
        }
        if (mPendingNewAction) {
            addNewAction(action, description);
        } else {
            updateAction(action, description, null, mPendingIndex,
                         mPendingLongpress, mPendingDoubleTap);
        }
        mPendingLongpress = false;
        mPendingDoubleTap = false;
        mPendingNewAction = false;
        mPendingIndex = -1;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if (requestCode == REQUEST_PICK_CUSTOM_ICON && mPendingIndex != -1) {
                if (mImageTmp.length() == 0 || !mImageTmp.exists()) {
                    mPendingIndex = -1;
                    Toast.makeText(mActivity, getContext().getResources().getString(
                            R.string.shortcut_image_not_valid), Toast.LENGTH_LONG).show();
                    return;
                }
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File folder = new File(Environment.getExternalStorageDirectory() +
                            File.separator + ".slim" + File.separator + "icons");
                    folder.mkdirs();
                    File image = new File(folder.toString() + File.separator
                            + "shortcut_" + System.currentTimeMillis() + ".png");
                    String path = image.getAbsolutePath();
                    mImageTmp.renameTo(image);
                    image.setReadable(true, false);
                    updateAction(null, null, path, mPendingIndex, false, false);
                    mPendingIndex = -1;
                }
            }
        } else {
            if (mImageTmp.exists()) {
                mImageTmp.delete();
            }
            mPendingLongpress = false;
            mPendingNewAction = false;
            mPendingIndex = -1;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateAction(String action, String description, String icon,
                int which, boolean longpress, boolean doubletap) {

        if (!longpress && !doubletap && checkForDuplicateMainNavActions(action)) {
            return;
        }

        ActionConfig actionConfig = mActionConfigsAdapter.getItem(which);
        mActionConfigsAdapter.remove(actionConfig);

        if (!longpress && !doubletap) {
            deleteIconFileIfPresent(actionConfig, false);
        }

        if (icon != null) {
            actionConfig.setIcon(icon);
        } else {
            if (longpress) {
                actionConfig.setLongpressAction(action);
                actionConfig.setLongpressActionDescription(description);
            } else if (doubletap) {
                actionConfig.setDoubleTapAction(action);
                actionConfig.setDoubleTapActionDescription(description);
            } else {
                deleteIconFileIfPresent(actionConfig, true);
                actionConfig.setClickAction(action);
                actionConfig.setClickActionDescription(description);
                actionConfig.setIcon(ActionConstants.ICON_EMPTY);
            }
        }

        mActionConfigsAdapter.insert(actionConfig, which);
        showDisableMessage(false);
        setConfig(mActionConfigs, false);
    }

    private boolean checkForDuplicateMainNavActions(String action) {
        ActionConfig actionConfig;
        for (int i = 0; i < mActionConfigs.size(); i++) {
            actionConfig = mActionConfigsAdapter.getItem(i);
            if (actionConfig.getClickAction().equals(action)) {
                Toast.makeText(mActivity,
                        getContext().getResources().getString(R.string.shortcut_duplicate_entry),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    private void deleteIconFileIfPresent(ActionConfig action, boolean deleteShortCutIcon) {
        File oldImage = new File(action.getIcon());
        if (oldImage.exists()) {
            oldImage.delete();
        }
        oldImage = new File(action.getClickAction().replaceAll(".*?hasExtraIcon=", ""));
        if (oldImage.exists() && deleteShortCutIcon) {
            oldImage.delete();
        }
    }

    private void showDisableMessage(boolean show) {
        if (mDisableMessage == null || mDisableDeleteLastEntry) {
            return;
        }
        if (show) {
            mDisableMessage.setVisibility(View.VISIBLE);
        } else {
            mDisableMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                if (mActionConfigs.size() == mMaxAllowedActions) {
                    Toast.makeText(mActivity,
                            getContext().getResources().getString(R.string.shortcut_action_max),
                            Toast.LENGTH_LONG).show();
                    break;
                }
                if (mUseFullAppsOnly) {
                    if (mPicker != null) {
                        mPendingIndex = 0;
                        mPendingLongpress = false;
                        mPendingNewAction = true;
                        mPicker.pickShortcut(getId(), true);
                    }
                } else if (!mUseAppPickerOnly) {
                    showDialogInner(DLG_SHOW_ACTION_DIALOG, 0, false, false, true);
                } else {
                    if (mPicker != null) {
                        mPendingIndex = 0;
                        mPendingLongpress = false;
                        mPendingNewAction = true;
                        mPicker.pickShortcut(getId());
                    }
                }
                break;
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, 0, false, false, true);
                break;
            case MENU_HELP:
                    showDialogInner(DLG_SHOW_HELP_SCREEN, 0, false, false, true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_HELP, 0, getContext().getResources().getString(R.string.help))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_RESET, 0,
                getContext().getResources().getString(R.string.shortcut_action_reset))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, MENU_ADD, 0, getContext().getResources().getString(R.string.shortcut_action_add))
                .setIcon(getContext().getResources().getDrawable(R.drawable.ic_menu_add))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    }

    private void addNewAction(String action, String description) {
        if (checkForDuplicateMainNavActions(action)) {
            return;
        }
        ActionConfig actionConfig = new ActionConfig(
            action, description,
            ActionConstants.ACTION_NULL,
            getContext().getResources().getString(R.string.shortcut_action_none),
            ActionConstants.ACTION_NULL,
            getContext().getResources().getString(R.string.shortcut_action_none),
            ActionConstants.ICON_EMPTY);

            mActionConfigsAdapter.add(actionConfig);
            showDisableMessage(false);
            setConfig(mActionConfigs, false);
    }

    private ArrayList<ActionConfig> getConfig() {
        switch (mActionMode) {
            case NAV_BAR:
                return ActionHelper.getNavBarConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
/* Disabled for now till all features are back. Enable it step per step!!!!!!
            case POWER_MENU_SHORTCUT:
                return ActionHelper.getPowerMenuConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case NAV_RING:
                return ActionHelper.getNavRingConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case PIE:
                return ActionHelper.getPieConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case PIE_SECOND:
                return ActionHelper.getPieSecondLayerConfigWithDescription(
                    mActivity, mActionValuesKey, mActionEntriesKey);
            case LOCKSCREEN_SHORTCUT:
                return ActionHelper.getLockscreenShortcutConfig(mActivity);
            case SHAKE_EVENTS_DISABLED:
                return ActionHelper.getDisabledShakeApps(mActivity);
*/
        }
        return null;
    }

    private void setConfig(ArrayList<ActionConfig> actionConfigs, boolean reset) {
        switch (mActionMode) {
            case NAV_BAR:
                ActionHelper.setNavBarConfig(mActivity, actionConfigs, reset);
                break;
/* Disabled for now till all features are back. Enable it step per step!!!!!!
            case POWER_MENU_SHORTCUT:
                ActionHelper.setPowerMenuConfig(mActivity, actionConfigs, reset);
                break;
            case NAV_RING:
                ActionHelper.setNavRingConfig(mActivity, actionConfigs, reset);
                break;
            case PIE:
                ActionHelper.setPieConfig(mActivity, actionConfigs, reset);
                break;
            case PIE_SECOND:
                ActionHelper.setPieSecondLayerConfig(mActivity, actionConfigs, reset);
                break;
            case LOCKSCREEN_SHORTCUT:
                ActionHelper.setLockscreenShortcutConfig(mActivity, actionConfigs, reset);
                break;
            case SHAKE_EVENTS_DISABLED:
                ActionHelper.setDisabledShakeApps(mActivity, actionConfigs, reset);
                break;
*/
        }
    }

    private class ViewHolder {
        public TextView clickActionDescriptionView;
        public TextView longpressActionDescriptionView;
        public TextView doubleTapActionDescriptionView;
        public ImageView iconView;
    }

    private class ActionConfigsAdapter extends ArrayAdapter<ActionConfig> {

        public ActionConfigsAdapter(Context context, List<ActionConfig> clickActionDescriptions) {
            super(context, org.slim.preference.R.layout.action_list_view_item,
                    R.id.click_action_description, clickActionDescriptions);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            if (v != convertView && v != null) {
                ViewHolder holder = new ViewHolder();

                TextView clickActionDescription =
                    (TextView) v.findViewById(R.id.click_action_description);
                TextView longpressActionDecription =
                    (TextView) v.findViewById(R.id.longpress_action_description);
                TextView doubleTapActionDescription =
                    (TextView) v.findViewById(R.id.doubletap_action_description);
                ImageView icon = (ImageView) v.findViewById(R.id.list_item_icon);

                if (mDisableLongpress) {
                    longpressActionDecription.setVisibility(View.GONE);
                } else {
                    holder.longpressActionDescriptionView = longpressActionDecription;
                }

                if (mDisableDoubleTap) {
                    doubleTapActionDescription.setVisibility(View.GONE);
                } else {
                    holder.doubleTapActionDescriptionView = doubleTapActionDescription;
                }

                holder.iconView = icon;
                holder.clickActionDescriptionView = clickActionDescription;

                v.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) v.getTag();

            holder.clickActionDescriptionView.setText(
                    getItem(position).getClickActionDescription());

            if (!mDisableLongpress) {
                holder.longpressActionDescriptionView.setText(
                    getContext().getResources().getString(R.string.shortcut_action_longpress)
                    + " " + getItem(position).getLongpressActionDescription());
            }
            if (!mDisableDoubleTap) {
                holder.doubleTapActionDescriptionView.setText(
                    getContext().getResources().getString(R.string.shortcut_action_doubletap)
                    + " " + getItem(position).getDoubleTapActionDescription());
            }

            Drawable d = null;
            String iconUri = getItem(position).getIcon();
            /*if (mActionMode == POWER_MENU_SHORTCUT) {
                d = ImageHelper.resize(
                        mActivity, ActionHelper.getPowerMenuIconImage(mActivity,
                        getItem(position).getClickAction(),
                        iconUri), 48);
            } else {*/
                d = ImageHelper.resize(
                        mActivity, ActionHelper.getActionIconImage(mActivity,
                        getItem(position).getClickAction(),
                        iconUri), 36);

                if ((iconUri.equals(ActionConstants.ICON_EMPTY) &&
                        getItem(position).getClickAction().startsWith("**")) || (iconUri != null
                        && iconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER))) {
                    if (d != null) {
                        d = ImageHelper.getColoredDrawable(d,
                                getContext().getResources().getColor(R.color.dslv_icon_dark));
                    }
                }
            //}
            if (iconUri != null && iconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)) {
                if (d != null) d.setTint(
                        getContext().getResources().getColor(R.color.dslv_icon_dark));
            }
            holder.iconView.setImageBitmap(ImageHelper.drawableToBitmap(d));

            if (!mDisableIconPicker && holder.iconView.getDrawable() != null) {
                holder.iconView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPendingIndex = position;
                        showDialogInner(DLG_SHOW_ICON_PICKER, 0, false, false, false);
                    }
                });
            }

            return v;
        }
    }

    private void showDialogInner(int id, int which, boolean longpress,
                                 boolean doubletap, boolean newAction) {
        DialogFragment newFragment =
            MyAlertDialogFragment.newInstance(id, which, longpress, doubletap, newAction);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id,
                int which, boolean longpress, boolean doubletap, boolean newAction) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("which", which);
            args.putBoolean("longpress", longpress);
            args.putBoolean("doubletap", doubletap);
            args.putBoolean("newAction", newAction);
            frag.setArguments(args);
            return frag;
        }

        ActionListViewSettings getOwner() {
            return (ActionListViewSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final int which = getArguments().getInt("which");
            final boolean longpress = getArguments().getBoolean("longpress");
            final boolean doubletap = getArguments().getBoolean("doubletap");
            final boolean newAction = getArguments().getBoolean("newAction");
            switch (id) {
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // first delete custom icons in case they exist
                            ArrayList<ActionConfig> actionConfigs = getOwner().getConfig();
                            for (int i = 0; i < actionConfigs.size(); i++) {
                                getOwner().deleteIconFileIfPresent(actionConfigs.get(i), true);
                            }

                            // reset provider values and action adapter to default
                            getOwner().setConfig(null, true);
                            getOwner().mActionConfigsAdapter.clear();

                            // Add the new default objects fetched from @getConfig()
                            actionConfigs = getOwner().getConfig();
                            final int newConfigsSize = actionConfigs.size();
                            for (int i = 0; i < newConfigsSize; i++) {
                                getOwner().mActionConfigsAdapter.add(actionConfigs.get(i));
                            }

                            // dirty helper if actionConfigs list has no entries
                            // to proper update the content. .notifyDatSetChanged()
                            // does not work in this case.
                            if (newConfigsSize == 0) {
                                ActionConfig emptyAction =
                                    new ActionConfig(null, null, null, null, null, null, null);
                                getOwner().mActionConfigsAdapter.add(emptyAction);
                                getOwner().mActionConfigsAdapter.remove(emptyAction);
                            }
                            getOwner().showDisableMessage(newConfigsSize == 0);
                        }
                    })
                    .create();
                case DLG_SHOW_HELP_SCREEN:
                    Resources res = getOwner().getContext().getResources();
                    String finalHelpMessage;
                    String actionMode;
                    String icon = "";
                    switch (getOwner().mActionMode) {
                        case NAV_BAR:
                        default:
                            actionMode = res.getString(R.string.shortcut_action_help_button);
                            break;
                    }
                    if (!getOwner().mDisableIconPicker) {
                        icon = res.getString(R.string.shortcut_action_help_icon);
                    }
                    finalHelpMessage = res.getString(
                        R.string.shortcut_action_help_main, actionMode, icon);
                    if (!getOwner().mDisableDeleteLastEntry) {
                        finalHelpMessage += " " + res.getString(
                                R.string.shortcut_action_help_delete_last_entry, actionMode);
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.help_label)
                    .setMessage(finalHelpMessage)
                    .setNegativeButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_DELETION_NOT_ALLOWED:
                    int message;
                    if (getOwner().mActionConfigs.size() > 1) {
                        message = R.string.shortcut_action_required_warning_message;
                    } else {
                        message = R.string.shortcut_action_warning_message;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_warning)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_SHOW_ACTION_DIALOG:
                    int title;
                    if (longpress) {
                        title = R.string.shortcut_action_select_action_longpress;
                    } else if (doubletap) {
                        title = R.string.shortcut_action_select_action_doubletap;
                    } else if (newAction) {
                        title = R.string.shortcut_action_select_action_newaction;
                    } else {
                        title = R.string.shortcut_action_select_action;
                    }

                    // for normal press action we filter out null value
                    // due it does not make sense to set a null action
                    // on normal press action
                    String[] values = null;
                    String[] entries = null;
                    if (!longpress && !doubletap) {
                        List<String> finalEntriesList = new ArrayList<String>();
                        List<String> finalValuesList = new ArrayList<String>();

                        for (int i = 0; i < getOwner().mActionDialogValues.length; i++) {
                            if (!getOwner().mActionDialogValues[i]
                                    .equals(ActionConstants.ACTION_NULL)) {
                                finalEntriesList.add(getOwner().mActionDialogEntries[i]);
                                finalValuesList.add(getOwner().mActionDialogValues[i]);
                            }
                        }

                        entries = finalEntriesList.toArray(new String[finalEntriesList.size()]);
                        values = finalValuesList.toArray(new String[finalValuesList.size()]);
                    }

                    final String[] finalDialogValues =
                        (longpress || doubletap) ? getOwner().mActionDialogValues : values;
                    final String[] finalDialogEntries =
                        (longpress || doubletap) ? getOwner().mActionDialogEntries : entries;

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(getOwner().getContext().getString(title))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setItems(finalDialogEntries,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (finalDialogValues[item].equals(ActionConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingIndex = which;
                                    getOwner().mPendingLongpress = longpress;
                                    getOwner().mPendingDoubleTap = doubletap;
                                    getOwner().mPendingNewAction = newAction;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                if (newAction) {
                                    getOwner().addNewAction(finalDialogValues[item],
                                            finalDialogEntries[item]);
                                } else {
                                    getOwner().updateAction(finalDialogValues[item],
                                            finalDialogEntries[item],
                                            null, which, longpress, doubletap);
                                }
                            }
                        }
                    })
                    .create();
                case DLG_SHOW_ICON_PICKER:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcuts_icon_picker_type)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setItems(R.array.icon_types,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which) {
                                case 0: // Default
                                    getOwner().updateAction(null, null,
                                        ActionConstants.ICON_EMPTY,
                                        getOwner().mPendingIndex, false, false);
                                    getOwner().mPendingIndex = -1;
                                    break;
                                case 1: // System defaults
                                    ListView list = new ListView(getActivity());
                                    list.setAdapter(new IconAdapter(getOwner().getContext()));
                                    final Dialog holoDialog = new Dialog(getActivity());
                                    holoDialog.setTitle(
                                            R.string.shortcuts_icon_picker_choose_icon_title);
                                    holoDialog.setContentView(list);
                                    list.setOnItemClickListener(new OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                                            IconAdapter adapter = (IconAdapter) parent.getAdapter();
                                            getOwner().updateAction(null, null,
                                                adapter.getItemReference(position),
                                                getOwner().mPendingIndex, false, false);
                                            getOwner().mPendingIndex = -1;
                                            holoDialog.cancel();
                                        }
                                    });
                                    holoDialog.show();
                                    break;
                                case 2: // Custom user icon
                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                                    intent.setType("image/*");
                                    intent.putExtra("crop", "true");
                                    intent.putExtra("scale", true);
                                    intent.putExtra("outputFormat",
                                        Bitmap.CompressFormat.PNG.toString());
                                    intent.putExtra("aspectX", 100);
                                    intent.putExtra("aspectY", 100);
                                    intent.putExtra("outputX", 100);
                                    intent.putExtra("outputY", 100);
                                    try {
                                        getOwner().mImageTmp.createNewFile();
                                        getOwner().mImageTmp.setWritable(true, false);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                            Uri.fromFile(getOwner().mImageTmp));
                                        intent.putExtra("return-data", false);
                                        getOwner().startActivityForResult(
                                            intent, REQUEST_PICK_CUSTOM_ICON);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    })
                    .create();
                case DLG_HOME_WARNING_DIALOG:
                case DLG_BACK_WARNING_DIALOG:
                    int msg;
                    if (id == DLG_HOME_WARNING_DIALOG) {
                        msg = R.string.no_home_key;
                    } else {
                        msg = R.string.no_back_key;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(getOwner().getContext().getString(R.string.attention))
                    .setMessage(getOwner().getContext().getString(msg))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            if (!newAction) {
                                getOwner().mTempActionConfig = null;
                                getOwner().setConfig(getOwner().mActionConfigs, false);
                            }
                            dialog.cancel();
                            if (newAction) {
                                getOwner().showDialogInner(
                                        DLG_SHOW_ACTION_DIALOG, which, false, false, false);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            if (!newAction) {
                                getOwner().mActionConfigsAdapter.insert(
                                        getOwner().mTempActionConfig, which);
                                getOwner().mTempActionConfig = null;
                            }
                            dialog.cancel();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }

        public class IconAdapter extends BaseAdapter {

            TypedArray icons;
            String[] labels;
            int color;

            public IconAdapter(Context context) {
                labels = context.getResources().getStringArray(R.array.shortcut_icon_picker_labels);
                icons = context.getResources().obtainTypedArray(R.array.shortcut_icon_picker_icons);
                color = context.getResources().getColor(R.color.dslv_icon_dark);
            }

            @Override
            public Object getItem(int position) {
                return icons.getDrawable(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public int getCount() {
                return labels.length;
            }

            public String getItemReference(int position) {
                String name = icons.getString(position);
                int separatorIndex = name.lastIndexOf(File.separator);
                int periodIndex = name.lastIndexOf('.');
                return ActionConstants.SYSTEM_ICON_IDENTIFIER
                    + name.substring(separatorIndex + 1, periodIndex);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View iView = convertView;
                if (convertView == null) {
                    iView = View.inflate(getActivity(), android.R.layout.simple_list_item_1, null);
                }
                TextView tt = (TextView) iView.findViewById(android.R.id.text1);
                tt.setText(labels[position]);
                Drawable ic = ((Drawable) getItem(position)).mutate();
                ic.setTint(color);
                tt.setCompoundDrawablePadding(15);
                tt.setCompoundDrawablesWithIntrinsicBounds(ic, null, null, null);
                return iView;
            }
        }
    }
}
