package com.android.systemui.statusbar.slim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.android.internal.util.ArrayUtils;
import com.android.systemui.editor.ActionItem;
import com.android.systemui.editor.QuickAction;

import com.android.systemui.statusbar.phone.SlimNavigationBarView;

import slim.action.ActionsArray;
import slim.action.ActionConfig;
import slim.action.ActionConstants;
import slim.action.ActionHelper;
import slim.provider.SlimSettings;
import slim.utils.ImageHelper;
import slim.utils.ShortcutPickerHelper;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NavigationBarEditor implements View.OnTouchListener {

    public static final String NAVBAR_EDIT_ACTION = "android.intent.action.NAVBAR_EDIT";

    private static final int MENU_SINGLE_TAP = 1;
    private static final int MENU_DOUBLE_TAP = 2;
    private static final int MENU_LONG_PRESS = 3;
    private static final int MENU_PICK_ICON  = 4;

    private static final int MENU_VISIBILITY_ALWAYS  = 5;
    private static final int MENU_VISIBILITY_REQUEST = 6;
    private static final int MENU_VISIBILITY_NEVER   = 7;

    private static final int IME_VISIBILITY_NEVER = 8;
    private static final int IME_VISIBILITY_REQUEST = 9;

    private static final int MENU_ICON_RESET = 10;
    private static final int MENU_ICON_PACK = 11;
    private static final int MENU_ICON_GALLERY = 12;

    private static final int ADD_BUTTON_ID = View.generateViewId();
    private static final int DELETE_BUTTON_ID = View.generateViewId();

    private Context mContext;
    private SlimNavigationBarView mNavBar;

    private ActionsArray mActionsArray;
    private ArrayList<SlimKeyButtonView> mButtons = new ArrayList<>();
    private int mMaxButtons;

    private SlimKeyButtonView mAddButton;
    private SlimKeyButtonView mDeleteButton;
    private SlimKeyButtonView mButtonToEdit;
    private int mEditAction = -1;

    private static final int EDITING_SINGLE_TAP = 0;
    private static final int EDITING_LONGPRESS = 1;
    private static final int EDITING_DOUBLE_TAP = 2;

    private boolean mEditing;
    private boolean mDeleting = false;
    private boolean mLongPressed;

    private QuickAction mPopup;
    private ArrayList<ActionItem> mButtonItems = new ArrayList<>();
    private ArrayList<ActionItem> mMenuButtonItems = new ArrayList<>();
    private ArrayList<ActionItem> mImeButtonItems = new ArrayList<>();
    private ArrayList<ActionItem> mIconButtonItems = new ArrayList<>();

    private QuickAction.OnActionItemClickListener mQuickClickListener =
            new QuickAction.OnActionItemClickListener() {
        @Override
        public void onItemClick(QuickAction action, int pos, int actionId) {
            switch (actionId) {
                case MENU_SINGLE_TAP:
                    editSingleTap(mButtonToEdit);
                    break;
                case MENU_DOUBLE_TAP:
                    editDoubleTap(mButtonToEdit);
                    break;
                case MENU_LONG_PRESS:
                    editLongpress(mButtonToEdit);
                    break;
                case MENU_PICK_ICON:
                    editAction(mButtonToEdit, true);
                    break;
                case MENU_VISIBILITY_ALWAYS:
                case MENU_VISIBILITY_REQUEST:
                case MENU_VISIBILITY_NEVER:
                    updateMenuButtonVisibility(actionId);
                    break;
                case IME_VISIBILITY_REQUEST:
                case IME_VISIBILITY_NEVER:
                    updateImeButtonVisibility(actionId);
                    break;
                case MENU_ICON_RESET:
                case MENU_ICON_PACK:
                case MENU_ICON_GALLERY:
                    selectIcon(actionId);
                    break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ImageHelper.ACTION_IMAGE_PICKED.equals(action)) {
                int result = intent.getIntExtra("result", Activity.RESULT_CANCELED);
                if (result == Activity.RESULT_OK) {
                    String uri = intent.getStringExtra("uri");
                    imagePicked(uri);
                }
            } else if (ShortcutPickerHelper.ACTION_SHORTCUT_PICKED.equals(action)) {
                String shortcutAction = intent.getStringExtra("action");
                String description = intent.getStringExtra("description");
                if (mButtonToEdit == null) {
                    addButton(shortcutAction, description);
                } else {
                    ActionConfig config = mButtonToEdit.getConfig();
                    if (mEditAction == EDITING_SINGLE_TAP) {
                        config.setClickAction(shortcutAction);
                        config.setClickActionDescription(description);
                    } else if (mEditAction == EDITING_LONGPRESS) {
                        config.setLongpressAction(shortcutAction);
                        config.setLongpressActionDescription(description);
                    } else if (mEditAction == EDITING_DOUBLE_TAP) {
                        config.setDoubleTapAction(shortcutAction);
                        config.setDoubleTapActionDescription(description);
                    }
                    updateKey(mButtonToEdit, config);
                    mButtonToEdit = null;
                    mEditAction = -1;
                }
            }
        }
    };

    public static final int[] SMALL_BUTTON_IDS = { R.id.menu, R.id.menu_left, R.id.ime_switcher,
        ADD_BUTTON_ID, DELETE_BUTTON_ID };

     // start point of the current drag operation
    private float mDragOrigin;

    // just to avoid reallocations
    private static final int[] sLocation = new int[2];

    // Button chooser dialog
    private AlertDialog mDialog;

    private AlertDialog mActionDialog;

    /**
     * Longpress runnable to assign buttons in edit mode
     */
    private Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (mEditing) {
                mLongPressed = true;
                mNavBar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setDeleting(true);
            }
        }
    };

    public NavigationBarEditor(SlimNavigationBarView navBar) {
        mContext = navBar.getContext();
        mNavBar = navBar;
        createAddButton();
        createDeleteButton();
        initActionsArray();

        mPopup = new QuickAction(mContext);

        mMaxButtons = mContext.getResources().getInteger(
                org.slim.framework.internal.R.integer.config_maxNavigationBarButtons);

        IntentFilter filter = new IntentFilter(ImageHelper.ACTION_IMAGE_PICKED);
        filter.addAction(ShortcutPickerHelper.ACTION_SHORTCUT_PICKED);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void createAddButton() {
        mAddButton = new SlimKeyButtonView(mContext, null);
        mAddButton.setId(ADD_BUTTON_ID);
        Drawable d = mContext.getResources().getDrawable(R.drawable.ic_action_add);
        mAddButton.setImageDrawable(d);
        mAddButton.setOnTouchListener(this);
    }

    private void createDeleteButton() {
        mDeleteButton = new SlimKeyButtonView(mContext, null);
        Drawable d = mContext.getResources().getDrawable(R.drawable.ic_action_delete);
        mDeleteButton.setImageDrawable(d);
        mDeleteButton.setRippleColor(0xffff0000);
    }

    private void initActionsArray() {
        mActionsArray = new ActionsArray(mContext);
        mActionsArray.addEntry(ActionConstants.ACTION_SPACE,
                mContext.getString(org.slim.framework.internal.R.string.shortcut_action_space));
    }

    private interface DialogClickListener {
        void onClick(int which);
    }

    private void showActionDialog(final DialogClickListener clickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.navbar_dialog_title))
                .setItems(mActionsArray.getEntries(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clickListener.onClick(which);
                        closeDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeDialog();
                    }
                });
        mDialog = builder.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }

    private void editSingleTap(final SlimKeyButtonView v) {
        showActionDialog(new DialogClickListener() {
            @Override
            public void onClick(int which) {
                String action = mActionsArray.getValue(which);
                String description = mActionsArray.getEntry(which);
                if (action.equals(ActionConstants.ACTION_APP)) {
                    mButtonToEdit = v;
                    mEditAction = EDITING_SINGLE_TAP;
                    ShortcutPickerHelper.pickShortcut(mContext);
                    return;
                }
                ActionConfig config = v.getConfig();
                config.setClickAction(action);
                config.setClickActionDescription(description);
                updateKey(v, config);
            }
        });
    }

    private void editDoubleTap(final SlimKeyButtonView v) {
        showActionDialog(new DialogClickListener() {
            @Override
            public void onClick(int which) {
                String action = mActionsArray.getValue(which);
                String description = mActionsArray.getEntry(which);
                if (action.equals(ActionConstants.ACTION_APP)) {
                    mButtonToEdit = v;
                    mEditAction = EDITING_LONGPRESS;
                    ShortcutPickerHelper.pickShortcut(mContext);
                    return;
                }
                ActionConfig config = v.getConfig();
                config.setDoubleTapAction(action);
                config.setDoubleTapActionDescription(description);
                v.setConfig(config);
            }
        });
    }

    private void editLongpress(final SlimKeyButtonView v) {
        showActionDialog(new DialogClickListener() {
            @Override
            public void onClick(int which) {
                String action = mActionsArray.getValue(which);
                String description = mActionsArray.getEntry(which);
                if (action.equals(ActionConstants.ACTION_APP)) {
                    mButtonToEdit = v;
                    mEditAction = EDITING_LONGPRESS;
                    ShortcutPickerHelper.pickShortcut(mContext);
                    return;
                }
                ActionConfig config = v.getConfig();
                config.setLongpressAction(action);
                config.setLongpressActionDescription(description);
                v.setConfig(config);
            }
        });
    }

    private void selectIcon(int id) {
        switch (id) {
            case MENU_ICON_RESET:
                resetIcon();
                break;
            case MENU_ICON_GALLERY:
                selectIconFromGallery();
                break;
            case MENU_ICON_PACK:
                selectIconFromIconPack();
                break;
        }
    }

    private void resetIcon() {
        ActionConfig config = mButtonToEdit.getConfig();
        config.setIcon(ActionConstants.ICON_EMPTY);
        updateKey(mButtonToEdit, config);
        mButtonToEdit = null;
    }

    private void selectIconFromGallery() {
        Intent intent = new Intent();
        intent.setClassName("com.slim.settings", "com.slim.settings.activities.IconPickerGallery");
        //intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void selectIconFromIconPack() {
        Intent intent = new Intent();
        //intent.setAction(Intent.ACTION_MAIN);
        intent.setClassName("com.slim.settings", "com.slim.settings.activities.IconPackActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void imagePicked(String uri) {
        ActionConfig config = mButtonToEdit.getConfig();
        config.setIcon(uri);
        updateKey(mButtonToEdit, config);
        mButtonToEdit = null;
    }

    private void closeDialog() {
        mDialog.dismiss();
        mDialog = null;
    }

    private void updateKey(SlimKeyButtonView button, ActionConfig config) {
        ActionConfig oldConfig = button.getConfig();
        button.setConfig(config);
    }

    private void updateButton(SlimKeyButtonView v) {
        v.setEditing(mEditing);
        v.setOnTouchListener(mEditing ? this : null);
        v.setOnClickListener(null);
        v.setOnLongClickListener(null);
        v.setVisibility(View.VISIBLE);
        v.updateFromConfig();
    }

    private void updateMenuButtonVisibility(int menuId) {
        String key;
        int vis = 0;
        if (mButtonToEdit.getId() == R.id.menu) {
            key = SlimSettings.System.MENU_VISIBILITY_RIGHT;
        } else {
            key = SlimSettings.System.MENU_VISIBILITY_LEFT;
        }
        switch (menuId) {
            case MENU_VISIBILITY_ALWAYS:
                vis = SlimNavigationBarView.MENU_VISIBILITY_ALWAYS;
                break;
            case MENU_VISIBILITY_REQUEST:
                vis = SlimNavigationBarView.MENU_VISIBILITY_SYSTEM;
                break;
            case MENU_VISIBILITY_NEVER:
                vis = SlimNavigationBarView.MENU_VISIBILITY_NEVER;
                break;
        }
        SlimSettings.System.putIntForUser(mContext.getContentResolver(),
                key, vis, UserHandle.USER_CURRENT);
    }

    private void updateImeButtonVisibility(int menuId) {
        int vis;
        if (menuId == IME_VISIBILITY_NEVER) {
            vis = 1;
        } else {
            vis = 2;
        }
        SlimSettings.System.putIntForUser(mContext.getContentResolver(),
                SlimSettings.System.IME_BUTTON_VISIBILITY, vis, UserHandle.USER_CURRENT);
    }

    private void setDeleting(boolean d) {
        if (mButtons.size() == 1) return;
        if (mDeleting == d) return;
        mDeleting = d;
        if (d) {
            if (mAddButton.getParent() != null) {
                mNavBar.removeButton(mAddButton);
            }
            if (mDeleteButton.getParent() == null) {
                mNavBar.addButton(mDeleteButton);
            }
        } else {
            if (mDeleteButton.getParent() != null) {
                mNavBar.removeButton(mDeleteButton);
            }
            if (mAddButton.getParent() == null) {
                mNavBar.addButton(mAddButton);
            }
        }
    }

    public void setEditing(boolean editing) {
        mEditing = editing;
        if (mEditing) {
            mButtons.clear();
            mButtons.addAll(mNavBar.getButtons());
            loadMenu();
            mPopup.prepare();
            mPopup.setOnActionItemClickListener(mQuickClickListener);
            mNavBar.addButton(mAddButton);
        } else {
            mPopup.removeContainer();
            mNavBar.removeButton(mAddButton);
            save();
            mButtons.clear();
        }
        for (SlimKeyButtonView key : mButtons) {
            updateButton(key);
        }
        if (!editing && mDialog != null && mDialog.isShowing()) {
            closeDialog();
        }
    }

    private View mLastAffected;

    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (!mEditing || (mDialog != null && mDialog.isShowing())) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mPopup != null) {
                mPopup.dismiss();
            }
            view.setPressed(true);
            view.getLocationOnScreen(sLocation);
            mDragOrigin = sLocation[mNavBar.isVertical() ? 1 : 0];
            view.postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            view.setPressed(false);

            if (!mLongPressed || ArrayUtils.contains(SMALL_BUTTON_IDS, view.getId())
                    || view == mAddButton) {
                return false;
            }

            ViewGroup viewParent = (ViewGroup) view.getParent();
            float pos = mNavBar.isVertical() ? event.getRawY() : event.getRawX();
            float buttonSize = mNavBar.isVertical() ? view.getHeight() : view.getWidth();
            float min = mNavBar.isVertical() ? viewParent.getTop()
                    : (viewParent.getLeft() - buttonSize / 2);
            float max = mNavBar.isVertical() ? (viewParent.getTop() + viewParent.getHeight())
                    : (viewParent.getLeft() + viewParent.getWidth());

            // Prevents user from dragging view outside of bounds
            if (pos < min || pos > max) {
                //return false;
            }
            if (true) {
                view.setX(pos - viewParent.getLeft() - buttonSize / 2);
            } else {
                view.setY(pos - viewParent.getTop() - buttonSize / 2);
            }
            View affectedView = findInterceptingView(pos, view);
            if (affectedView != mDeleteButton) {
                 mDeleteButton.setClickable(false);
                 mDeleteButton.setPressed(false);
            }
            if (affectedView == null) {
                return false;
            } else if (affectedView == mDeleteButton) {
                mDeleteButton.setClickable(true);
                mDeleteButton.setPressed(true);
                return true;
            }
            if (mPopup != null) {
                mPopup.dismiss();
            }
            mLastAffected = affectedView;
            moveButton(affectedView, view);
        } else if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            view.setPressed(false);
            view.removeCallbacks(mCheckLongPress);

            float pos = mNavBar.isVertical() ? event.getRawY() : event.getRawX();
            View affectedView = findInterceptingView(pos, view);

            if (!mLongPressed) {
                if (view == mAddButton) {
                    showActionDialog(new DialogClickListener() {
                        @Override
                        public void onClick(int which) {
                            String action = mActionsArray.getValue(which);
                            String description = mActionsArray.getEntry(which);
                            addButton(action, description);
                        }
                    });
                } else {
                    editAction((SlimKeyButtonView) view, false);
                }
            } else {
                if (affectedView == mDeleteButton) {
                    mDeleteButton.setPressed(false);
                    deleteButton(view);
                } else {
                    // Reset the dragged view to its original location
                    ViewGroup parent = (ViewGroup) view.getParent();

                    if (!mNavBar.isVertical()) {
                        view.setX(mDragOrigin - parent.getLeft());
                    } else {
                        view.setY(mDragOrigin - parent.getTop());
                    }
                }
            }
            setDeleting(false);
            mLongPressed = false;
        }
        return true;
    }

    private void moveButton(View targetView, View view) {
        ViewGroup parent = (ViewGroup) view.getParent();

        targetView.getLocationOnScreen(sLocation);
        if (!mNavBar.isVertical()) {
            targetView.setX(mDragOrigin - parent.getLeft());
            mDragOrigin = sLocation[0];
        } else {
            targetView.setY(mDragOrigin - parent.getTop());
            mDragOrigin = sLocation[1];
        }
        Collections.swap(mButtons, mButtons.indexOf(view), mButtons.indexOf(targetView));
    }

    private void deleteButton(View view) {
       SlimKeyButtonView key = (SlimKeyButtonView) view;
       if (key.getConfig().getClickAction().equals(ActionConstants.ACTION_HOME) ||
               key.getConfig().getClickAction().equals(ActionConstants.ACTION_BACK)) {
           if (mContext.getResources().getInteger(
                   org.slim.framework.internal.R.integer.config_deviceHardwareKeys) == 0) {
               // TODO: toast notifying user why button was not deleted
               return;
           }
       }
       mNavBar.removeButton(view);
       mButtons.remove(view);
       ArrayList<ActionConfig> configs = new ArrayList<>();
       for (SlimKeyButtonView k : mButtons) {
           if (k.getConfig() != null) {
               configs.add(k.getConfig());
           }
       }
       mNavBar.makeBar(configs);
       setEditing(true);
    }

    public boolean isEditing() {
        return mEditing;
    }

    /**
     * Find intersecting view in mButtonViews
     * @param pos - pointer location
     * @param v - view being dragged
     * @return intersecting view or null
     */
    private View findInterceptingView(float pos, View v) {
        for (SlimKeyButtonView otherView : mNavBar.getButtons()) {
            if (otherView == v) {
                continue;
            }

            if (ArrayUtils.contains(SMALL_BUTTON_IDS, otherView.getId())
                    || otherView == mAddButton) {
                continue;
            }

            otherView.getLocationOnScreen(sLocation);
            float otherPos = sLocation[mNavBar.isVertical() ? 1 : 0];
            float otherDimension = mNavBar.isVertical() ? v.getHeight() : v.getWidth();

            if (pos > (otherPos + otherDimension / 4) && pos < (otherPos + otherDimension)) {
                return otherView;
            }
        }
        return null;
    }

    public View getAddButton() {
        return mAddButton;
    }

    private void addButton(String action, String description) {
        ActionConfig actionConfig = new ActionConfig(
                action, description,
                ActionConstants.ACTION_NULL,
                mContext.getResources().getString(
                        org.slim.framework.internal.R.string.shortcut_action_none),
                ActionConstants.ACTION_NULL,
                mContext.getResources().getString(
                        org.slim.framework.internal.R.string.shortcut_action_none),
                ActionConstants.ICON_EMPTY);

        SlimKeyButtonView v = new SlimKeyButtonView(mContext, null);
        v.setConfig(actionConfig);

        updateButton(v);

        mNavBar.addButton(v, mNavBar.getNavButtons().indexOfChild(mAddButton));
        mButtons.add(v);
    }

    public void save() {

       ArrayList<ActionConfig> buttons = new ArrayList<>();

        for (View v : mButtons) {
            if (v instanceof SlimKeyButtonView) {
                SlimKeyButtonView key = (SlimKeyButtonView) v;

                if (ArrayUtils.contains(SMALL_BUTTON_IDS, v.getId()) || mAddButton == v
                        || mDeleteButton == v) {
                    continue;
                }


                ActionConfig config = key.getConfig();
                if (config != null) {
                    buttons.add(config);
                }
            }
        }

        ActionHelper.setNavBarConfig(mContext, buttons, false);
    }

    private void editAction(SlimKeyButtonView key, boolean icon) {
        mButtonToEdit = key;
        ArrayList<ActionItem> items;
        if (key.getId() == R.id.menu || key.getId() == R.id.menu_left) {
            items = mMenuButtonItems;
        } else if (key.getId() == R.id.ime_switcher) {
            items = mImeButtonItems;
        } else if (icon) {
            items = mIconButtonItems;
        } else {
            items = mButtonItems;
        }
        mPopup.removeAllItems();
        for (ActionItem item : items) {
            mPopup.addActionItem(item);
        }
        mPopup.show(key);
    }

    private void loadMenu() {
        mButtonItems.clear();
        mButtonItems.add(new ActionItem(MENU_SINGLE_TAP, "Single tap",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_single_tap)));
        mButtonItems.add(new ActionItem(MENU_DOUBLE_TAP, "Double tap",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_double_tap)));
        mButtonItems.add(new ActionItem(MENU_LONG_PRESS, "Longpress",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_long_press)));
        mButtonItems.add(new ActionItem(MENU_PICK_ICON, "Icon",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_icon)));

        mIconButtonItems.clear();
        mIconButtonItems.add(new ActionItem(MENU_ICON_RESET, "Default",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_icon_reset)));
        mIconButtonItems.add(new ActionItem(MENU_ICON_PACK, "Icon Pack",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_icon_pack)));
        mIconButtonItems.add(new ActionItem(MENU_ICON_GALLERY, "Gallery",
                mContext.getDrawable(R.drawable.ic_smartbar_editor_icon_gallery)));

        mMenuButtonItems.clear();
        mMenuButtonItems.add(new ActionItem(MENU_VISIBILITY_ALWAYS, "Always show", null));
        mMenuButtonItems.add(new ActionItem(MENU_VISIBILITY_REQUEST,
                "Show on request (default)", null));
        mMenuButtonItems.add(new ActionItem(MENU_VISIBILITY_NEVER, "Never show", null));

        mImeButtonItems.clear();
        mImeButtonItems.add(new ActionItem(IME_VISIBILITY_REQUEST,
                "Show on request (default)", null));
        mImeButtonItems.add(new ActionItem(IME_VISIBILITY_NEVER, "Never show", null));
    }
}
