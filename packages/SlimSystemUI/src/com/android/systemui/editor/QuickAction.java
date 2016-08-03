
package com.android.systemui.editor;

import android.content.Context;

import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.PopupWindow.OnDismissListener;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;

import java.util.List;
import java.util.ArrayList;

import com.android.systemui.R;

/**
 * @author Pontus Holmberg (EndLessMind) Email: the_mr_hb@hotmail.com
 **/

public class QuickAction implements OnDismissListener {
    private View mRootView;
    private ImageView mArrowUp;
    private ImageView mArrowDown;
    private LayoutInflater mInflater;
    private ViewGroup mTrack;
    private ScrollView mScroller;
    private OnActionItemClickListener mItemClickListener;
    private OnDismissListener mDismissListener;

    private FrameLayout mContainer;
    private View mHidden;

    private Context mContext;
    private PopupWindow mWindow;
    private WindowManager mWindowManager;

    private List<ActionItem> actionItems = new ArrayList<>();

    private boolean mDidAction;

    private View mAnchor;

    private int mInsertPos;
    private int rootWidth = 0;

    /**
     * Constructor for default vertical layout
     * 
     * @param context Context
     */
    public QuickAction(Context context) {
        mContext = context;
        mWindow = new PopupWindow(context);
        mWindow.setBackgroundDrawable(new BitmapDrawable());
        mWindow.setOutsideTouchable(true);
        mWindow.setTouchInterceptor(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();

                    return true;
                }

                return false;
            }
        });

        mInflater = LayoutInflater.from(context);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        setRootViewId(R.layout.popup_vertical);
    }

    /**
     * Get action item at an index
     * 
     * @param index Index of item (position from callback)
     * @return Action Item at the position
     */
    public ActionItem getActionItem(int index) {
        return actionItems.get(index);
    }

    /**
     * Set root view.
     * 
     * @param id Layout resource id
     */
    public void setRootViewId(int id) {
        // setOutsideTouchable(true);
        mRootView = mInflater.inflate(id, null);
        mTrack = (ViewGroup) mRootView.findViewById(R.id.tracks);
        mArrowDown = (ImageView) mRootView.findViewById(R.id.arrow_down);
        mArrowUp = (ImageView) mRootView.findViewById(R.id.arrow_up);
        mScroller = (ScrollView) mRootView.findViewById(R.id.scroller);

        // This was previously defined on show() method, moved here to prevent force close that
        // occured
        // when tapping fastly on a view to show quickaction dialog.
        // thanks to zammbi (github.com/zammbi)
        mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        setContentView(mRootView);
    }

    /**
     * Set content view.
     *
     * @param root Root view
     */
    public void setContentView(View root) {
        mRootView = root;

        mWindow.setContentView(root);
    }


    /**
     * Set listener for action item clicked.
     * 
     * @param listener Listener
     */
    public void setOnActionItemClickListener(OnActionItemClickListener listener) {
        mItemClickListener = listener;
    }

    /**
     * Add action item
     * 
     * @param action {@link ActionItem}
     */
    public void addActionItem(ActionItem action) {
        actionItems.add(action);

        String title = action.getTitle();
        Drawable icon = action.getIcon();
        View container = mInflater.inflate(R.layout.deep_shortcut, mTrack, false);
        ImageView img = (ImageView) container.findViewById(R.id.deep_shortcut_icon);
        TextView text = (TextView) container.findViewById(R.id.deep_shortcut);

        container.setTag(action);

        if (icon != null) {
            img.setImageDrawable(icon);
        } else {
            img.setVisibility(View.GONE);
        }

        if (title != null) {
            text.setText(title);
        } else {
            text.setVisibility(View.GONE);
        }

        final int pos = actionItems.indexOf(action);
        final int actionId = action.getActionId();

        container.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!getActionItem(pos).isSticky()) {
                    mDidAction = true;
                    dismiss();
                }
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(QuickAction.this, pos, actionId);
                }
            }
        });

        container.setFocusable(true);
        container.setClickable(true);

        mTrack.addView(container, mInsertPos);

        mInsertPos++;
    }

    public View getCurrentAnchor() {
        return mAnchor;
    }

    public void removeAllItems() {
        actionItems.clear();
        mTrack.removeAllViewsInLayout();
        mInsertPos = 0;
    }

    /**
     * Dismiss the popup window.
     */
    public void dismiss() {
        mWindow.dismiss();
        if (mContainer != null) {
            mContainer.setVisibility(View.GONE);
        }
    }

    public void prepare() {
        removeContainer();
        mContainer = new FrameLayout(mContext);
        mHidden = new View(mContext);
        mHidden.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        mHidden.setVisibility(View.INVISIBLE);
        mContainer.addView(mHidden);
        mContainer.setVisibility(View.GONE);
        mWindowManager.addView(mContainer, getLayoutParams());
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp.gravity = Gravity.BOTTOM;
        lp.setTitle("SmartBar Editor");
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        return lp;
    }

    public void removeContainer() {
        dismiss();
        if (mContainer != null && mContainer.isAttachedToWindow()) {
            mContainer.removeAllViews();
            mContainer.setVisibility(View.GONE);
            mWindowManager.removeViewImmediate(mContainer);
        }
    }

    /**
     * On pre show
     */
    private void preShow(View anchor) {
        if (mRootView == null)
            throw new IllegalStateException("setContentView was not called with a view to display.");

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        ViewGroup parent = (ViewGroup) anchor.getParent();
        mContainer.setVisibility(View.VISIBLE);
        mHidden.setTag(anchor.getTag());
        mHidden.getLayoutParams().width = anchor.getWidth();
        mHidden.getLayoutParams().height = anchor.getHeight();
        mHidden.setLayoutParams(mHidden.getLayoutParams());
        mHidden.setX(location[0] - anchor.getLeft());
        mHidden.setY(location[1] - anchor.getTop());

        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        mWindow.setContentView(mRootView);
    }

    /**
     * Show quickaction popup. Popup is automatically positioned, on top or bottom of anchor view.
     */
    public void show(View anchor) {
        mAnchor = anchor;
        preShow(anchor);
        int xPos, yPos, arrowPos;

        mDidAction = false;

        int[] location = new int[2];

        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0] + anchor.getWidth(),
                location[1] + anchor.getHeight());

        mRootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootHeight = mRootView.getMeasuredHeight();

        if (rootWidth == 0) {
            rootWidth = mRootView.getMeasuredWidth();
        }

        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // automatically get X coord of popup (top left)
        if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth - mHidden.getWidth());
            xPos = (xPos < 0) ? 0 : xPos;

            arrowPos = anchorRect.centerX() - xPos;

        } else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth / 2);
            } else {
                xPos = anchorRect.left;
            }

            arrowPos = anchorRect.centerX() - xPos;
        }

        int dyTop = anchorRect.top;
        int dyBottom = screenHeight - anchorRect.bottom;

        if (dyTop > dyBottom) {
            if (rootHeight > dyTop) {
                yPos = 15;
                LayoutParams l = mScroller.getLayoutParams();
                l.height = dyTop - anchor.getHeight();
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;

            if (rootHeight > dyBottom) {
                LayoutParams l = mScroller.getLayoutParams();
                l.height = dyBottom;
            }
        }

        showArrow(((dyTop > dyBottom) ? R.id.arrow_down : R.id.arrow_up), arrowPos);

        setAnimationStyle(screenWidth, anchorRect.centerX());
        mWindow.showAtLocation(mHidden, Gravity.NO_GRAVITY, xPos, yPos);
    }

    /**
     * Set animation style
     * 
     * @param screenWidth screen width
     * @param requestedX distance from left edge
     */
    private void setAnimationStyle(int screenWidth, int requestedX) {
        int arrowPos = requestedX - mArrowUp.getMeasuredWidth() / 2;
        if (arrowPos <= screenWidth / 4) {
            mWindow.setAnimationStyle(R.style.Animations_PopUpMenu_Left);
        } else if (arrowPos > screenWidth / 4 && arrowPos < 3 * (screenWidth / 4)) {
            mWindow.setAnimationStyle(R.style.Animations_PopUpMenu_Center);
        } else {
            mWindow.setAnimationStyle(R.style.Animations_PopUpMenu_Right);
        }
    }

    /**
     * Show arrow
     * 
     * @param whichArrow arrow type resource id
     * @param requestedX distance from left screen
     */
    private void showArrow(int whichArrow, int requestedX) {
        final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp
                : mArrowDown;
        final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown
                : mArrowUp;

        final int arrowWidth = mArrowDown.getMeasuredWidth();

        showArrow.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) showArrow
                .getLayoutParams();

        param.leftMargin = requestedX - arrowWidth / 2;

        hideArrow.setVisibility(View.INVISIBLE);
    }

    /**
     * Set listener for window dismissed. This listener will only be fired if the quicakction dialog
     * is dismissed by clicking outside the dialog or clicking on sticky item.
     */
    public void setOnDismissListener(OnDismissListener listener) {
        mWindow.setOnDismissListener(this);

        mDismissListener = listener;
    }

    @Override
    public void onDismiss() {
        if (!mDidAction && mDismissListener != null) {
            Log.d("Qick", "Dismissed-inside");
            mDismissListener.onDismiss();
        }
    }

    /**
     * Listener for item click
     */
    public interface OnActionItemClickListener {
        public abstract void onItemClick(QuickAction source, int pos, int actionId);
    }

    /**
     * Listener for window dismiss
     */
    public interface OnDismissListener {
        public abstract void onDismiss();
    }
}
