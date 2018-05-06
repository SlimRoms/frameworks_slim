/*
 * Copyright (C) 2014-2017 SlimRoms Project
 * Author: Lars Greiss - email: kufikugel@googlemail.com
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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.slimrecent.ExpandableCardAdapter.ExpandableCard;
import com.android.systemui.slimrecent.ExpandableCardAdapter.OptionsItem;
import com.android.systemui.stackdivider.WindowManagerProxy;

import java.io.IOException;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import android.util.ArraySet;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import slim.provider.SlimSettings;

import static android.app.ActivityManager.StackId.PINNED_STACK_ID;
import static android.app.ActivityManager.StackId.RECENTS_STACK_ID;

/**
 * Our main view controller which handles and construct most of the view
 * related tasks.
 *
 * Constructing the actual cards, add the listeners, loading or updating the tasks
 * and inform all relevant classes with the listeners is done here.
 *
 * As well the actual click, longpress or swipe action methods are holded here.
 */
public class RecentPanelView {

    private static final String TAG = "RecentPanelView";

    public static final String TASK_PACKAGE_IDENTIFIER = "#ident:";

    private static final int EXPANDED_STATE_UNKNOWN  = 0;
    public static final int EXPANDED_STATE_EXPANDED  = 1;
    public static final int EXPANDED_STATE_COLLAPSED = 2;
    public static final int EXPANDED_STATE_BY_SYSTEM = 4;
    public static final int EXPANDED_STATE_TOPTASK   = 8;

    public static final int EXPANDED_MODE_AUTO    = 0;
    public static final int EXPANDED_MODE_NEVER = 1;
    // mode_always not working well yet, thumbs refresh needs to be improved
    // private static final int EXPANDED_MODE_ALWAYS  = 2;

    //public static final String PLAYSTORE_REFERENCE = "com.android.vending";
    //public static final String AMAZON_REFERENCE    = "com.amazon.venezia";

    //public static final String PLAYSTORE_APP_URI_QUERY = "market://details?id=";
    //public static final String AMAZON_APP_URI_QUERY    = "amzn://apps/android?p=";

    private final Context mContext;
    private final ImageView mEmptyRecentView;

    private final RecyclerView mCardRecyclerView;
    private ExpandableCardAdapter mCardAdapter;

    private final RecentController mController;

    // Our first task which is not displayed but needed for internal references.
    protected TaskDescription mFirstTask;
    // Array list of all expanded states of apps accessed during the session
    private final ArrayList<TaskExpandedStates> mExpandedTaskStates =
            new ArrayList<TaskExpandedStates>();

    private boolean mCancelledByUser;
    private boolean mTasksLoaded;
    private boolean mIsLoading;

    private int mMaxAppsToLoad;
    private float mCornerRadius;
    private float mScaleFactor;
    private int mExpandedMode = EXPANDED_MODE_AUTO;
    private boolean mIsScreenPinningEnabled;
    //private static int mOneHandMode:
    private static int mCardColor = 0x0ffffff;
    private int mFirstExpandedItems = 2;
    private Resources mRes;

    private String mCurrentFavorites = "";
    private Set<String> mCurrentFavoritesSplit = new HashSet<String>();

    private Set<String> mBlacklist = new HashSet<String>();

    private ArraySet<Integer> mCurrentQuietProfiles = new ArraySet<Integer>();

    private PackageManager mPm;
    private ActivityManager mAm;
    private IActivityManager mIam;

    final static BitmapFactory.Options sBitmapOptions;

    static {
        sBitmapOptions = new BitmapFactory.Options();
        sBitmapOptions.inMutable = true;
    }

    private static final int OPTION_INFO = 1001;
    //private static final int OPTION_MARKET = 1002;
    private static final int OPTION_MULTIWINDOW = 1003;
    private static final int OPTION_KILL = 1004;
    private static final int OPTION_CLOSE = 1005;

    private class RecentCard extends ExpandableCard {

        TaskDescription task;

        private RecentCard(TaskDescription task) {
            super(task.getLabel(), null);
            setTask(task);
        }

        private void setTask(TaskDescription task) {
            this.task = task;
            this.appName = task.getLabel();
            updateExpandState();

            this.context = mContext;
            this.identifier = task.identifier;

            this.persistentTaskId = task.persistentTaskId;
            this.packageName = task.packageName;
            this.favorite = task.getIsFavorite();
            this.appIconClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startApplication(task);
                }
            };
            this.appIconLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    favorite = !favorite;
                    handleFavoriteEntry(task);
                    mCardAdapter.notifyItemChanged(index);
                    return true;
                }
            };

            this.expandListener = new ExpandableCardAdapter.ExpandListener() {
                @Override
                public void onExpanded(boolean expanded) {
                    mController.onStartExpandAnimation();
                    final int oldState = task.getExpandedState();
                    int state;
                    if (expanded) {
                        state = EXPANDED_STATE_EXPANDED;
                    } else {
                        state = EXPANDED_STATE_COLLAPSED;
                    }
                    if ((oldState & EXPANDED_STATE_BY_SYSTEM) != 0) {
                        state |= EXPANDED_STATE_BY_SYSTEM;
                    }
                    task.setExpandedState(state);
                }
            };

            this.pinAppListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.pinApp(task.persistentTaskId);
                }
            };

            this.hideOptionsListener = new ExpandableCardAdapter.HideOptionsListener() {
                @Override
                public void onHideOptions(int index) {
                    hideOptions(index);
                }
            };


            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    Intent intent = null;
                    if (id == OPTION_INFO) {
                        intent = getAppInfoIntent();
                    /*} else if (id == OPTION_MARKET) {
                        intent = getStoreIntent();*/
                    } else if (id == OPTION_MULTIWINDOW) {
                        int dockSide = WindowManagerProxy.getInstance()
                                .getDockSide();
                        if (dockSide != WindowManager.DOCKED_INVALID) {
                            try {
                                //resize the docked stack to fullscreen to disable current
                                //multiwindow mode
                                mIam.resizeStack(
                                        ActivityManager.StackId.DOCKED_STACK_ID,
                                        null, true, true, false, -1);
                            } catch (Exception e) {}
                        }
                        mController.startTaskinMultiWindow(task.persistentTaskId);
                        clearOptions();
                        return;
                    } else if (id == OPTION_KILL) {
                        if (RecentController.killAppLongClick(
                                mContext, task.packageName, task.persistentTaskId)) {
                            mCardAdapter.removeCard(index);
                            removeApplication(task);
                        }
                        return;
                    }
                    if (intent != null) {
                        RecentController.sendCloseSystemWindows("close_recents");
                        intent.setComponent(intent.resolveActivity(mPm));
                        TaskStackBuilder.create(mContext)
                                .addNextIntentWithParentStack(intent).startActivities(
                                    RecentController.getAnimation(mContext).toBundle());
                    }
                }
            };

            clearOptions();
            addOption(new OptionsItem(
                    mContext.getDrawable(R.drawable.ic_recent_app_info),
                    OPTION_INFO, listener));
            /*if (checkAppInstaller(task.packageName, AMAZON_REFERENCE)
                    || checkAppInstaller(task.packageName, PLAYSTORE_REFERENCE)) {
                addOption(new OptionsItem(
                        mContext.getDrawable(R.drawable.ic_shop), OPTION_MARKET,
                        listener));
            }*/
            addOption(new OptionsItem(
                    mContext.getDrawable(R.drawable.ic_multiwindow),
                    OPTION_MULTIWINDOW,
                    listener));
            addOption(new OptionsItem(
                    mContext.getDrawable(R.drawable.ic_kill_app),
                    OPTION_KILL, listener));
            addOption(new OptionsItem(
                    mContext.getDrawable(R.drawable.ic_done),
                    OPTION_CLOSE, true));
        }

        private void updateExpandState() {
            // Read flags and set accordingly initial expanded state.
            final boolean isTopTask =
                    (task.getExpandedState() & EXPANDED_STATE_TOPTASK) != 0;

            final boolean isSystemExpanded =
                    (task.getExpandedState() & EXPANDED_STATE_BY_SYSTEM) != 0;

            final boolean isUserExpanded =
                    (task.getExpandedState() & EXPANDED_STATE_EXPANDED) != 0;

            final boolean isUserCollapsed =
                    (task.getExpandedState() & EXPANDED_STATE_COLLAPSED) != 0;

            final boolean isExpanded =
                    ((isSystemExpanded && !isUserCollapsed) || isUserExpanded)
                    && !isTopTask;

            boolean screenPinningEnabled = mIsScreenPinningEnabled;
            expanded = isExpanded;
            expandVisible = !isTopTask;
            /*noIcon = mExpandedMode != EXPANDED_MODE_NEVER
                    && isTopTask && !screenPinningEnabled;*/
            pinAppIcon = isTopTask && screenPinningEnabled;
            custom = mContext.getDrawable(R.drawable.ic_slimrec_pin_app);
        }

        private Intent getAppInfoIntent() {
            return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", task.packageName, null));
        }

        /*private Intent getStoreIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String reference;
            if (checkAppInstaller(task.packageName, AMAZON_REFERENCE)) {
                reference = AMAZON_REFERENCE;
                intent.setData(Uri.parse(AMAZON_APP_URI_QUERY + task.packageName));
            } else {
                reference = PLAYSTORE_REFERENCE;
                intent.setData(Uri.parse(PLAYSTORE_APP_URI_QUERY + task.packageName));
            }
            // Exclude from recents if the store is not in our task list.
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            return intent;
        }*/
    }


    private void hideOptions(int index) {
        ExpandableCardAdapter.ViewHolder vh =
                (ExpandableCardAdapter.ViewHolder) mCardRecyclerView
                .findViewHolderForLayoutPosition(index);
        if (vh != null) {
            vh.hideOptions(true);
        }
    }

    public interface OnExitListener {
        void onExit();
    }
    private OnExitListener mOnExitListener = null;

    public void setOnExitListener(OnExitListener onExitListener) {
        mOnExitListener = onExitListener;
    }

    public interface OnTasksLoadedListener {
        void onTasksLoaded();
    }
    private OnTasksLoadedListener mOnTasksLoadedListener = null;

    public void setOnTasksLoadedListener(
            OnTasksLoadedListener onTasksLoadedListener) {
        mOnTasksLoadedListener = onTasksLoadedListener;
    }

    public RecentPanelView(Context context, RecentController controller,
            RecyclerView recyclerView, ImageView emptyRecentView) {
        mContext = context;
        mCardRecyclerView = recyclerView;
        mEmptyRecentView = emptyRecentView;
        mController = controller;
        mPm = mContext.getPackageManager();
        mAm = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        mIam = ActivityManagerNative.getDefault();
        mRes = context.getResources();
        mFirstExpandedItems =
                mRes.getInteger(R.integer.expanded_items_default);
        
	buildCardListAndAdapter();

        setupItemTouchHelper();
    }

    /**
     * Build card list and arrayadapter we need to fill with tasks
     */
    protected void buildCardListAndAdapter() {
        boolean neverExpand = SlimSettings.System.getIntForUser(
                mContext.getContentResolver(),
                SlimSettings.System.RECENT_PANEL_EXPANDED_MODE,
                EXPANDED_MODE_NEVER,
                UserHandle.USER_CURRENT)
                == EXPANDED_MODE_NEVER;
        mCardAdapter = new ExpandableCardAdapter(mContext, neverExpand);
        if (mCardRecyclerView != null) {
            mCardRecyclerView.setAdapter(mCardAdapter);
        }
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new ItemTouchHelper.Callback() {
            RecentCard card;
            int taskid;
            int initPos;
            int finalPos;
            boolean isSwipe;
            boolean unwantedDrag = true;
            boolean areOptionsHidden = false;

            @Override
            public boolean onMove(RecyclerView recyclerView,
                    ViewHolder viewHolder, ViewHolder target) {
                initPos = viewHolder.getAdapterPosition();
                card = (RecentCard) mCardAdapter.getCard(initPos);
                taskid = card.task.persistentTaskId;
                /*we'll start multiwindow action in the clearView void, when the drag action
                and all animations are completed. Otherwise we'd do a loop action
                till the drag is completed for each onMove (wasting resources and making
                the drag not smooth).*/
                unwantedDrag = false;

                //hide longpress options while moving
                if (!areOptionsHidden) {
                    ExpandableCardAdapter.ViewHolder vh =
                            (ExpandableCardAdapter.ViewHolder) viewHolder;
                    vh.hideOptions(true);
                    areOptionsHidden = true;
                }

                return true;
            }

            @Override
            public void onMoved (RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder,
                int fromPos,
                RecyclerView.ViewHolder target,
                int toPos,
                int x,
                int y) {

                finalPos = toPos;
                isSwipe = false;
            }

            @Override
            public float getMoveThreshold (RecyclerView.ViewHolder viewHolder) {
                //if less than this, we consider it as unwanted drag
                return 0.2f;
            }

            @Override
            public void clearView (RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if (isSwipe) {
                    //don't start multiwindow on swipe
                    return;
                }

                if (unwantedDrag) {
                    /*this means MoveThreshold is less than needed, so onMove
                    has not been considered, so we don't consider the action as
                    wanted drag*/
                    return;
                }

                //restore the checks
                unwantedDrag = true;
                areOptionsHidden = false;

                boolean wasDocked = false;
                int dockSide = WindowManagerProxy.getInstance().getDockSide();
                if (dockSide != WindowManager.DOCKED_INVALID) {
                    try {
                        //resize the docked stack to fullscreen to disable current multiwindow mode
                        mIam.resizeStack(
                                ActivityManager.StackId.DOCKED_STACK_ID,
                                null, true, true, false, -1);
                    } catch (Exception e) {}
                    wasDocked = true;
                }
                ActivityOptions options = RecentController.getAnimation(mContext);
                options.setDockCreateMode(0); //0 means dock app to top, 1 to bottom
                options.setLaunchStackId(ActivityManager.StackId.DOCKED_STACK_ID);
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            card = (RecentCard) mCardAdapter.getCard(finalPos);
                            int newTaskid = card.task.persistentTaskId;
                            mIam.startActivityFromRecents((finalPos > initPos)
                                    ? taskid : newTaskid, options.toBundle());
                            /*after we docked our main app, on the other side of the screen we
                            open the app we dragged the main app over*/
                            try {
                                mIam.startActivityFromRecents(((finalPos > initPos)
                                        ? newTaskid : taskid),
                                        RecentController.getAnimation(mContext).toBundle());
                            } catch (RemoteException e) {}
                            //now no need to keep the panel open, we already chose
                            //both top and bottom apps
                            mController.closeRecents();
                        } catch (Exception e) {}
                    }
                //if we disabled a running multiwindow mode,
                //just wait a little bit before docking the new apps
                }, wasDocked ? 100 : 0);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public void onSwiped(ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                RecentCard card = (RecentCard) mCardAdapter.getCard(pos);
                mCardAdapter.removeCard(pos);
                removeApplication(card.task);
                isSwipe = true;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder) {
                // Set movement flags based on the layout manager
                final int dragFlags = ItemTouchHelper.UP| ItemTouchHelper.DOWN;
                final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }
        });
        touchHelper.attachToRecyclerView(mCardRecyclerView);
    }

    /**
     * Check if the requested app was installed by the reference store.
     */
    /*private boolean checkAppInstaller(String packageName, String reference) {
        if (packageName == null) {
            return false;
        }
        if (!isReferenceInstalled(reference, mPm)) {
            return false;
        }

        String installer = mPm.getInstallerPackageName(packageName);
        if (reference.equals(AMAZON_REFERENCE)) {
            if (reference.equals(installer)) {
                return true;
            } else {
                return false;
            }
        }
        if (!packageName.equals("com.android.settings")
                && !packageName.equals("com.android.systemui")) {
            //we already checked if at least PlayStore is installed with isReferenceInstalled
            //so we can safely return true and search the package in the Playstore, if it's not
            // Settings or SystemUI (it'd be useless for those).
            return true;
        }
        return false;
    }*/

    /**
     * Check is store reference is installed.
     */
    /*private boolean isReferenceInstalled(String packagename, PackageManager pm) {
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }*/

    /**
     * Handle favorite task entry (add or remove) if user longpressed on app icon.
     */
    private void handleFavoriteEntry(TaskDescription td) {
        ContentResolver resolver = mContext.getContentResolver();
        final String favorites = mCurrentFavorites;
        String entryToSave = "";

        if (!td.getIsFavorite()) {
            if (favorites != null && !favorites.isEmpty()) {
                entryToSave += favorites + "|";
            }
            entryToSave += td.identifier;
        } else {
            if (favorites == null) {
                return;
            }
            for (String favorite : favorites.split("\\|")) {
                if (favorite.equals(td.identifier)) {
                    continue;
                }
                entryToSave += favorite + "|";
            }
            if (!entryToSave.isEmpty()) {
                entryToSave = entryToSave.substring(0, entryToSave.length() - 1);
            }
        }

        td.setIsFavorite(!td.getIsFavorite());

        SlimSettings.System.putStringForUser(
                resolver, SlimSettings.System.RECENT_PANEL_FAVORITES,
                entryToSave,
                UserHandle.USER_CURRENT);

         setCurrentFavorites(entryToSave);
    }

    /**
     * Remove requested application.
     */
    private void removeApplication(TaskDescription td) {
        // Kill the actual app and send accessibility event.
        if (mAm != null) {
            mAm.removeTask(td.persistentTaskId);

            // Accessibility feedback
            mCardRecyclerView.setContentDescription(
                    mContext.getString(R.string.accessibility_recents_item_dismissed,
                            td.getLabel()));
            mCardRecyclerView.sendAccessibilityEvent(
                    AccessibilityEvent.TYPE_VIEW_SELECTED);
            mCardRecyclerView.setContentDescription(null);

            // Remove app from task and expanded state list.
            removeExpandedTaskState(td.identifier);

            // Refresh activity info on next app load if we removed the app
            // we can still keep icons
            InfosCacheController.getInstance(mContext).removeInfos(td.componentName);
            /* we could also refresh thumbs but this will slow down panel loading e.g. after a
                clear all apps action, it's not worth it considering that even an old thumb
                is good to recognize the app. A better solution would be to add a "last time"
                thumb load check and refresh thumbs after some time.
            */
            // ThumbnailsCacheController.getInstance(mContext).removeThumb(td.identifier);
        }

        // All apps were removed? Close recents panel.
        if (mCardAdapter.getItemCount() == 0) {
            setVisibility();
            exit();
        }
    }

    /**
     * Remove all applications. Call from controller class
     */
    protected boolean removeAllApplications() {
        boolean hasFavorite = false;
        int size = mCardAdapter.getItemCount() - 1;
        for (int i = size; i >= 0; i--) {
            RecentCard card = (RecentCard) mCardAdapter.getCard(i);
            TaskDescription td = card.task;
            // User favorites are not removed.
            if (td.getIsFavorite()) {
                hasFavorite = true;
                continue;
            }
            // Remove from task stack.
            if (mAm != null) {
                mAm.removeTask(td.persistentTaskId);
            }
            // Remove the card.
            removeRecentCard(card);
            // Remove expanded state.
            removeExpandedTaskState(td.identifier);
        }
        return !hasFavorite;
    }

    private void removeRecentCard(RecentCard card) {
        mCardAdapter.removeCard(card);
    }

    /**
     * Start application or move to forground if still active.
     */
    private void startApplication(TaskDescription td) {
        mController.startApplication(td);
        exit();
    }

    /**
     * Check if the requested store is in the task list to prevent it gets excluded.
     */
    /*private boolean storeIsInTaskList(String uriReference) {
        if (mFirstTask != null && uriReference.equals(
                mFirstTask.packageName)) {
            return true;
        }
        int count = mCardAdapter.getItemCount();
        for (int i = 0;  i < count; i++) {
            RecentCard c = (RecentCard) mCardAdapter.getCard(i);
            if (uriReference.equals(c.task.packageName)) {
                return true;
            }
        }
        return false;
    }*/

    /**
     * Create a TaskDescription, returning null if the title or icon is null.
     */
    private TaskDescription createTaskDescription(int taskId, int persistentTaskId,
            Intent baseIntent, ComponentName origActivity,
            CharSequence description, boolean isFavorite, int expandedState,
            ActivityManager.TaskDescription td) {

        final Intent intent = new Intent(baseIntent);
        if (origActivity != null) {
            intent.setComponent(origActivity);
        }
        intent.setFlags((intent.getFlags()
                &~ Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        String cn = null;
        final ComponentName component = intent.getComponent();
        if (component != null) {
            cn = component.flattenToString();
        }
        ActivityInfo info =
                InfosCacheController.getInstance(mContext)
                .getInfosFromMemCache(cn);
        if (info == null) {
            final ResolveInfo resolveInfo = mPm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                info = resolveInfo.activityInfo;
                if (component != null) {
                    InfosCacheController.getInstance(mContext)
                            .addInfosToMemoryCache(cn, info);
                }
            }
        }
        if (info != null) {
            String title = td.getLabel();
            if (title == null) {
                title = info.loadLabel(mPm).toString();
            }
            String identifier = TASK_PACKAGE_IDENTIFIER;
            if (component != null) {
                identifier += cn;
            } else {
                identifier += info.packageName;
            }

            if (title != null && title.length() > 0) {
                int color = td.getPrimaryColor();

                final TaskDescription item = new TaskDescription(taskId,
                        persistentTaskId, info, baseIntent, info.packageName, cn,
                        identifier, description, isFavorite, expandedState, color);
                item.setLabel(title);
                return item;
            }
        }
        return null;
    }

    /**
     * Load all tasks we want.
     */
    protected void loadTasks() {
        if (isTasksLoaded() || mIsLoading) {
            return;
        }
        mIsLoading = true;
        updateExpandedTaskStates();

        // We have all needed tasks now.
        // Let us load the cards for it in background.
        final CardLoader cardLoader = new CardLoader();
        cardLoader.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     * Set correct visibility states for the listview and the empty recent icon.
     */
    private void setVisibility() {
        mEmptyRecentView.setVisibility((
                mCardAdapter.getItemCount() == 0) ? View.VISIBLE : View.GONE);
        mCardRecyclerView.setVisibility((
                mCardAdapter.getItemCount() == 0) ? View.GONE : View.VISIBLE);
    }

    /**
     * We are holding a list of user expanded state of apps.
     * Update the List for actual apps.
     */
    private void updateExpandedTaskStates() {
        int count = mCardAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            RecentCard card = (RecentCard) mCardAdapter.getCard(i);
            boolean updated = false;
            for (TaskExpandedStates expandedState : mExpandedTaskStates) {
                if (card.task.identifier.equals(expandedState.getIdentifier())) {
                    updated = true;
                    expandedState.setExpandedState(card.task.getExpandedState());
                }
            }
            if (!updated) {
                mExpandedTaskStates.add(
                        new TaskExpandedStates(
                                card.task.identifier, card.task.getExpandedState()));
            }
        }
    }

    /**
     * We are holding a list of user expanded state of apps.
     * Get expanded state of the app.
     */
    private int getExpandedState(TaskDescription item) {
        for (TaskExpandedStates oldTask : mExpandedTaskStates) {
            if (item.identifier.equals(oldTask.getIdentifier())) {
                    return oldTask.getExpandedState();
            }
        }
        return EXPANDED_STATE_UNKNOWN;
    }

    /**
     * We are holding a list of user expanded state of apps.
     * Remove expanded state entry due that app was removed by the user.
     */
    private void removeExpandedTaskState(String identifier) {
        TaskExpandedStates expandedStateToDelete = null;
        for (TaskExpandedStates expandedState : mExpandedTaskStates) {
            if (expandedState.getIdentifier().equals(identifier)) {
                expandedStateToDelete = expandedState;
            }
        }
        if (expandedStateToDelete != null) {
            mExpandedTaskStates.remove(expandedStateToDelete);
        }
    }

    protected void notifyDataSetChanged(boolean forceupdate) {
        if (forceupdate || !mController.isShowing()) {
            mCardAdapter.notifyDataSetChanged();
        }
    }

    protected void setCancelledByUser(boolean cancelled) {
        mCancelledByUser = cancelled;
        if (cancelled) {
            setTasksLoaded(false);
        }
    }

    protected void setTasksLoaded(boolean loaded) {
        mTasksLoaded = loaded;
    }

    protected boolean isCancelledByUser() {
        return mCancelledByUser;
    }

    protected boolean isTasksLoaded() {
        return mTasksLoaded;
    }

    protected void setScaleFactor(float factor) {
        mScaleFactor = factor;
    }

    protected void setExpandedMode(int mode) {
        mExpandedMode = mode;
        boolean fastMode =
                mode == EXPANDED_MODE_NEVER;
        if (fastMode) {
            ThumbnailsCacheController.getInstance(mContext).clearCache();
        }
        mCardAdapter.setFastMode(fastMode);
    }

    protected boolean hasFavorite() {
        int count = mCardAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            RecentCard card = (RecentCard) mCardAdapter.getCard(i);
            if (card.task.getIsFavorite()) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasClearableTasks() {
        int count = mCardAdapter.getItemCount();
        for (int i = 0; i < count; i++) {
            RecentCard card = (RecentCard) mCardAdapter.getCard(i);
            if (!card.task.getIsFavorite()) {
                return true;
            }
        }
        return false;
    }

    /*protected void setOneHandMode(String str) {
        if (str != null && str.contains("left")) {
            mOneHandMode = 1;
        } else if (str != null && str.contains("right")) {
            mOneHandMode = 2;
        } else {
            mOneHandMode = 0;
        }
    }*/

    protected void setCardColor(int color) {
        mCardColor = color;
    }

    protected void setCurrentFavorites(String favorites) {
        mCurrentFavoritesSplit.clear();
        if (favorites != null) {
            mCurrentFavorites = favorites;
            for (String app : favorites.split("\\|")) {
                mCurrentFavoritesSplit.add(app);
            }
        }
    }

    protected void setBlackList(String blacklist) {
        mBlacklist.clear();
        if (blacklist != null) {
            for (String app : blacklist.split("\\|")) {
                mBlacklist.add(app);
            }
        }
    }

    protected void setCornerRadius(float radius) {
        mCornerRadius = radius;
    }

    /**
     * Notify listener that tasks are loaded.
     */
    private void tasksLoaded() {
        if (mOnTasksLoadedListener != null) {
            mIsLoading = false;
            if (!isCancelledByUser()) {
                setTasksLoaded(true);
                mOnTasksLoadedListener.onTasksLoaded();
           }
        }
    }

    /**
     * Notify listener that we exit recents panel now.
     */
    private void exit() {
        setTasksLoaded(false);
        if (mOnExitListener != null) {
            mOnExitListener.onExit();
        }
    }

    protected void scrollToFirst() {
        LinearLayoutManager lm =
                (LinearLayoutManager) mCardRecyclerView.getLayoutManager();
        lm.scrollToPositionWithOffset(0, 0);
    }

    /**
     * AsyncTask cardloader to load all cards in background. Preloading
     * forces as well a card load or update. So if the user cancelled the preload
     * or does not even open the recent panel we want to reduce the system
     * load as much as possible. So we do it in background.
     *
     * Note: App icons as well the app screenshots are loaded in other
     *       async tasks.
     *       See #link:RecentCard, #link:RecentExpandedCard
     *       #link:RecentAppIcon and #link AppIconLoader
     */
    private class CardLoader extends AsyncTask<Void,
            RecentCard, Boolean> {

        private int mCounter;

        public CardLoader() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // be sure to hide cards optionsView in the viewHolder
            // before cleaning up cards
            for (int i = 0; i < mCardAdapter.getItemCount(); i++) {
                hideOptions(i);
            }
            mCardAdapter.clearCards();
            mController.resetTasks();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            mCounter = 0;
            int firstItems = 0;
            final ArrayList<TaskDescription> nonFavoriteTasks = new ArrayList<>();

            SystemServicesProxy ssp = Recents.getSystemServices();
            int currentUserId = ssp.getCurrentUser();
            updateCurrentQuietProfilesCache(currentUserId);
            final List<ActivityManager.RecentTaskInfo> recentTasks =
                    ssp.getRecentTasks(ActivityManager.getMaxRecentTasksStatic(),
                    currentUserId, false/*includeFrontMostExcludedTask*/, mCurrentQuietProfiles);

            final int numTasks = recentTasks.size();

            for (int i = 0; i < numTasks; i++) {

                // If we reach max apps limit set by user, we are done
                if (mCounter >= mMaxAppsToLoad) {
                    break;
                }
                if (isCancelled() || mCancelledByUser) {
                    mIsLoading = false;
                    //return false;
                    break;
                }

                final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);

                final Intent intent = new Intent(recentInfo.baseIntent);
                if (recentInfo.origActivity != null) {
                    intent.setComponent(recentInfo.origActivity);
                }

                boolean topTask = i == 0;
                if (topTask) {
                    ActivityManager.RunningTaskInfo rTask = getRunningTask(mAm);
                    if (rTask != null) {
                        if (!rTask.baseActivity.getPackageName().equals(
                                recentInfo.baseIntent.getComponent().getPackageName())) {
                            topTask = false;
                        }
                    }
                    mController.isTopTaskInForeground(topTask);
                }

                TaskDescription item = createTaskDescription(recentInfo.id,
                        recentInfo.persistentId, recentInfo.baseIntent,
                        recentInfo.origActivity, recentInfo.description,
                        false, EXPANDED_STATE_UNKNOWN, recentInfo.taskDescription);

                if (item == null) {
                    // skip this item and go to next iteration
                    continue;
                }

                if (!topTask && !mBlacklist.isEmpty()
                        && mBlacklist.contains(item.packageName)) {
                    // skip this item and go to next iteration
                    continue;
                }

                if (mCounter < 2) {
                    // we need just the first 2 apps for double tap recents last app action
                    mController.addTasks(item);
                }

                if (!mCurrentFavoritesSplit.isEmpty()
                        && mCurrentFavoritesSplit.contains(item.identifier)) {
                    item.setIsFavorite(true);
                }

                if (topTask) {
                    // User want to see actual running task. Set it here
                    int oldState = getExpandedState(item);
                    if ((oldState & EXPANDED_STATE_TOPTASK) == 0) {
                        oldState |= EXPANDED_STATE_TOPTASK;
                    }
                    item.setExpandedState(oldState);
                    addCard(item, true, false);
                    mFirstTask = item;
                } else {
                    // FirstExpandedItems value forces to show always the app screenshot
                    // if the old state is not known and the user has set expanded mode to auto.
                    // On all other items we check if they were expanded from the user
                    // in last known recent app list and restore the state. This counts as well
                    // if expanded mode is always or never.
                    int oldState = getExpandedState(item);
                    if ((oldState & EXPANDED_STATE_BY_SYSTEM) != 0) {
                        oldState &= ~EXPANDED_STATE_BY_SYSTEM;
                    }
                    if ((oldState & EXPANDED_STATE_TOPTASK) != 0) {
                        oldState &= ~EXPANDED_STATE_TOPTASK;
                    }
                    if (firstItems < mFirstExpandedItems) {
                        //expand only if no expanded_mode_never
                        if (mExpandedMode != EXPANDED_MODE_NEVER) {
                            oldState |= EXPANDED_STATE_BY_SYSTEM;
                        }
                        item.setExpandedState(oldState);
                        // The first tasks are always added to the task list.
                        addCard(item, false, true);
                    } else {
                        /*if (mExpandedMode == EXPANDED_MODE_ALWAYS) {
                            oldState |= EXPANDED_STATE_BY_SYSTEM;
                        }*/
                        item.setExpandedState(oldState);
                        // Favorite tasks are added next. Non favorite
                        // we hold for a short time in an extra list.
                        if (item.getIsFavorite()) {
                            addCard(item, false, false);
                        } else {
                            nonFavoriteTasks.add(item);
                        }
                    }
                    firstItems++;
                }
            }

            // Add now the non favorite tasks to the final task list.
            for (TaskDescription item : nonFavoriteTasks) {
                if (mCounter >= mMaxAppsToLoad) {
                    break;
                }
                if (isCancelled() || mCancelledByUser) {
                    mIsLoading = false;
                    break;
                }
                addCard(item, false, false);
            }

            return true;
        }

        private void addCard(final TaskDescription task, boolean topTask, boolean loadBitmap) {
            final RecentCard card = new RecentCard(task);

            final Drawable appIcon =
                    CacheController.getInstance(mContext, mClearThumbOnEviction)
                    .getBitmapFromMemCache(task.identifier);
            if (appIcon != null) {
                card.appIcon = appIcon;
                postnotifyItemChanged(mCardRecyclerView, card);
            } else {
                AppIconLoader.getInstance(mContext).loadAppIcon(task.info,
                        task.identifier, new AppIconLoader.IconCallback() {
                            @Override
                            public void onDrawableLoaded(Drawable drawable) {
                                card.appIcon = drawable;
                                postnotifyItemChanged(mCardRecyclerView, card);
                            }
                }, mScaleFactor);
            }
            // skip thumbs loading process if fast mode enabled
            if (mExpandedMode != EXPANDED_MODE_NEVER && !topTask && loadBitmap) {
                new BitmapDownloaderTask(mContext,
                        new DownloaderCallback() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap) {
                        card.screenshot = bitmap;
                        postnotifyItemChanged(mCardRecyclerView, card);
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        task.persistentTaskId);
            }
            card.cardClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startApplication(task);
                }
            };

            // Set card color
            card.cardBackgroundColor = getCardBackgroundColor(task);
            //Set corner radius
            card.cornerRadius = mCornerRadius;

            mCounter++;
            publishProgress(card);
        }

        @Override
        protected void onProgressUpdate(RecentCard... card) {
            mCardAdapter.addCard(card[0]);
            if (!isTasksLoaded()) {
                //we have at least one task and card, so can show the panel while we
                //load more tasks and cards
               setVisibility();
               tasksLoaded();
            }
        }

        @Override
        protected void onPostExecute(Boolean loaded) {
            // If cancelled by system, log it and set task size
            // to the only visible tasks we have till now to keep task
            // removing alive. This should never happen. Just in case.
            if (!loaded) {
                Log.v(TAG, "card constructing was cancelled by system or user");
            }

            // Notify arrayadapter that data set has changed
            notifyDataSetChanged(true);
            // Notfiy controller that tasks are completly loaded.
            if (!isTasksLoaded()) {
                setVisibility();
                tasksLoaded();
            }
        }
    }

    private void updateCurrentQuietProfilesCache(int currentUserId) {
        mCurrentQuietProfiles.clear();

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        List<UserInfo> profiles = userManager.getProfiles(currentUserId);
        if (profiles != null) {
            for (int i = 0; i < profiles.size(); i++) {
                UserInfo user  = profiles.get(i);
                if (user.isManagedProfile() && user.isQuietModeEnabled()) {
                    mCurrentQuietProfiles.add(user.id);
                }
            }
        }
    }

    private CacheController.EvictionCallback mClearThumbOnEviction =
            new CacheController.EvictionCallback() {
        @Override
        public void onEntryEvicted(String key) {
            if (key != null && mExpandedMode != EXPANDED_MODE_NEVER) {
                ThumbnailsCacheController.getInstance(mContext).removeThumb(key);
            }
        }
    };

    private void postnotifyItemChanged(final RecyclerView recyclerView,
            RecentCard card) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!recyclerView.isComputingLayout()) {
                    mCardAdapter.notifyItemChanged(card.index);
                } else {
                    postnotifyItemChanged(recyclerView, card);
                }
            }
        });
    }

    private int getCardBackgroundColor(TaskDescription task) {
        if (mCardColor != 0x0ffffff) {
            return mCardColor;
        } else if (task != null && task.cardColor != 0) {
            return task.cardColor;
        } else {
            return mRes.getColor(R.color.recents_task_bar_default_background_color);
        }
    }

    private static ActivityManager.RunningTaskInfo
            getRunningTask(ActivityManager am) {
        // Note: The set of running tasks from the system is ordered by recency
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);
        if (tasks != null && !tasks.isEmpty()) {
            // Find the first task in a valid stack, we ignore everything
            // from the Recents and PiP stacks
            for (int i = 0; i < tasks.size(); i++) {
                ActivityManager.RunningTaskInfo task = tasks.get(i);
                int stackId = task.stackId;
                if (stackId != RECENTS_STACK_ID && stackId != PINNED_STACK_ID) {
                    return task;
                }
            }
        }
        return null;
    }

    protected void isScreenPinningEnabled(boolean enabled) {
        mIsScreenPinningEnabled = enabled;
    }

    protected void setMaxAppsToLoad(int max) {
        mMaxAppsToLoad = max;
    }

    /**
     * We are holding a list of user expanded states of apps.
     * This class describes one expanded state object.
     */
    private static final class TaskExpandedStates {
        private String mIdentifier;
        private int mExpandedState;

        public TaskExpandedStates(String identifier, int expandedState) {
            mIdentifier = identifier;
            mExpandedState = expandedState;
        }

        public String getIdentifier() {
            return mIdentifier;
        }

        public int getExpandedState() {
            return mExpandedState;
        }

        public void setExpandedState(int expandedState) {
            mExpandedState = expandedState;
        }
    }

    // Loads the actual task bitmap.
    public static Bitmap loadThumbnail(int persistentTaskId, Context context) {
        if (context == null) {
            return null;
        }
        return getThumbnail(persistentTaskId, true, context);
    }

    /**
     * Returns a task thumbnail from the activity manager
     */
    public static Bitmap getThumbnailOld(int taskId, Context ctx) {
        final ActivityManager am = (ActivityManager)
                ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.TaskThumbnail taskThumbnail =
                am.getTaskThumbnail(taskId);
        if (taskThumbnail == null) return null;

        Bitmap thumbnail = taskThumbnail.mainThumbnail;
        ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
        if (thumbnail == null && descriptor != null) {
            thumbnail = BitmapFactory.decodeFileDescriptor(
                    descriptor.getFileDescriptor(), null, sBitmapOptions);
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException e) {
            }
        }
        return thumbnail;
    }

    public static Bitmap getThumbnail(int taskId, boolean reducedResolution,
            Context ctx) {
        if (ActivityManager.ENABLE_TASK_SNAPSHOTS) {
            try {
                ActivityManager.TaskSnapshot snapshot = ActivityManager.getService()
                        .getTaskSnapshot(taskId, reducedResolution);
                if (snapshot != null) {
                    return Bitmap.createHardwareBitmap(snapshot.getSnapshot());
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to retrieve snapshot", e);
            }
            return null;
        } else {
            return getThumbnailOld(taskId, ctx);
        }
    }

    interface DownloaderCallback {
        void onBitmapLoaded(Bitmap bitmap);
    }

    // AsyncTask loader for the task bitmap.
    public static class BitmapDownloaderTask
            extends AsyncTask<Integer, Void, Bitmap> {

        private boolean mLoaded;
        private final WeakReference<Context> rContext;

        private DownloaderCallback mCallback;

        public BitmapDownloaderTask(Context context, DownloaderCallback callback) {
            rContext = new WeakReference<Context>(context);
            mCallback = callback;
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            mLoaded = false;
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + 1);
            if (isCancelled() || rContext == null) {
                return null;
            }
            // Load and return bitmap
            return loadThumbnail(params[0], rContext.get());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            final Context context;
            if (rContext != null) {
                context = rContext.get();
            } else {
                context = null;
            }
            // Assign image to the view.
            mLoaded = true;
            if (mCallback != null) {
                mCallback.onBitmapLoaded(bitmap);
            }
        }

        public boolean isLoaded() {
            return mLoaded;
        }
    }
}
