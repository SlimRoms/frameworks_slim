/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.slim;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SlimNavigationBarView;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.util.ArrayList;

import slim.action.ActionConfig;
import slim.action.ActionConstants;
import slim.action.ActionHelper;
import slim.action.Action;
import slim.action.SlimActionsManager;
import slim.provider.SlimSettings;
import slim.utils.ImageHelper;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;

public class SlimKeyButtonView extends KeyButtonView {

    private int mContentDescriptionRes;
    private long mDownTime;
    String mClickAction = ActionConstants.ACTION_NULL;
    String mLongpressAction = ActionConstants.ACTION_NULL;
    String mDoubleTapAction = ActionConstants.ACTION_NULL;
    private int mTouchSlop;
    boolean mSupportsLongpress = false;
    boolean mIsLongpressed = false;
    boolean mDoubleTapPending = false;
    boolean mDoubleTapConsumed = false;
    private AudioManager mAudioManager;
    private boolean mGestureAborted;
    private SlimKeyButtonRipple mRipple;
    private LongClickCallback mCallback;
    private boolean mEditing;
    private ActionConfig mConfig;
    private int mColorMode;
    private int mButtonColor;
    private boolean mLandscape = false;

    private final Handler mHandler = new Handler();

    private SlimActionsManager mActionsManager;

    private final Runnable mCheckLongPress = new Runnable() {
        public void run() {
            mIsLongpressed = true;
            if (isPressed()) {
                // Log.d("KeyButtonView", "longpressed: " + this);
                if (isLongClickable()) {
                    // Just an old-fashioned ImageView
                    performLongClick();
                } else {
                    sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                }
                setHapticFeedbackEnabled(true);
            }
        }
    };

    private final Runnable mDoubleTapTimeout = new Runnable() {
        @Override
        public void run() {
            mDoubleTapPending = false;
            performClick();
        }
    };

    public SlimKeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlimKeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView,
                defStyle, 0);

        mSupportsLongpress = a.getBoolean(R.styleable.KeyButtonView_keyRepeat, true);

        TypedValue value = new TypedValue();
        if (a.getValue(R.styleable.KeyButtonView_android_contentDescription, value)) {
            mContentDescriptionRes = value.resourceId;
        }

        a.recycle();

        mActionsManager = SlimActionsManager.getInstance(context);

        setClickable(true);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        setBackground(mRipple = new SlimKeyButtonRipple(context, this));
        updateButton();
    }

    public void updateButton() {
        ContentResolver resolver = mContext.getContentResolver();
        mButtonColor = SlimSettings.System.getIntForUser(resolver,
                SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT, -2, UserHandle.USER_CURRENT);

        if (mButtonColor == -2) {
            mButtonColor = mContext.getResources()
                    .getColor(R.color.navigationbar_button_default_color);
        }

        mColorMode = SlimSettings.System.getIntForUser(resolver,
                SlimSettings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0, UserHandle.USER_CURRENT);

        setButtonIcon(mLandscape);
        setLayoutParams(getLayoutParams(mLandscape));
    }

    public void setLandscape(boolean landscape) {
        mLandscape = landscape;
    }

    public void setEditing(boolean edit) {
        mEditing = edit;
    }

    public void setConfig(ActionConfig config) {
        mConfig = config;
        updateFromConfig();
    }

    public ActionConfig getConfig() {
        return mConfig;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mContentDescriptionRes != 0) {
            setContentDescription(mContext.getString(mContentDescriptionRes));
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            jumpDrawablesToCurrentState();
        }
    }

    public void setClickAction(String action) {
        mClickAction = action;
        setOnClickListener(mClickListener);
    }

    public void setLongpressAction(String action) {
        mLongpressAction = action;
        if (!action.equals(ActionConstants.ACTION_NULL)) {
            mSupportsLongpress = true;
            setOnLongClickListener(mLongPressListener);
        }
    }

    public void setDoubleTapAction(String action) {
        mDoubleTapAction = action;
    }

    public void updateFromConfig() {
        if (mConfig == null) return;
        if (mConfig.getClickAction().equals(ActionConstants.ACTION_SPACE)) {
            setClickable(false);
            setClickAction(ActionConstants.ACTION_NULL);
            setLongpressAction(ActionConstants.ACTION_NULL);
            setDoubleTapAction(ActionConstants.ACTION_NULL);
        } else {
            setClickable(true);
            setClickAction(mConfig.getClickAction());
            setLongpressAction(mConfig.getLongpressAction());
            setDoubleTapAction(mConfig.getDoubleTapAction());
            if (mConfig.getClickAction().startsWith("**")) {
                setScaleType(ScaleType.CENTER_INSIDE);
            } else {
                setScaleType(ScaleType.CENTER);
            }
            setButtonIcon(mLandscape);
        }
        setLayoutParams(getLayoutParams(mLandscape));
    }

    private LayoutParams getLayoutParams(boolean landscape) {
        int dp = mContext.getResources().getDimensionPixelSize(R.dimen.navigation_key_width);
        float px = dp * getResources().getDisplayMetrics().density;
        return landscape ?
                new LayoutParams(LayoutParams.MATCH_PARENT, dp, 1f) :
                new LayoutParams(dp, LayoutParams.MATCH_PARENT, 1f);
    }

    public void setColorMode(int mode) {
        mColorMode = mode;
        setButtonIcon(mLandscape);
    }

    public void setButtonColor(int color) {
        mButtonColor = color;
        setButtonIcon(mLandscape);
    }

    public void setButtonIcon(boolean landscape) {
        if (mConfig == null) return;
        if (mConfig.getClickAction().equals(ActionConstants.ACTION_SPACE)) return;
        boolean colorize = true;
        String iconUri = mConfig.getIcon();
        String clickAction = mConfig.getClickAction();
        if (iconUri != null && !iconUri.equals(ActionConstants.ICON_EMPTY)
                && !iconUri.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)
                && mColorMode == 1) {
            colorize = false;
        } else if (!clickAction.startsWith("**")) {
            final int[] appIconPadding = getAppIconPadding();
            if (landscape) {
                setPaddingRelative(appIconPadding[1], appIconPadding[0],
                        appIconPadding[3], appIconPadding[2]);
            } else {
                setPaddingRelative(appIconPadding[0], appIconPadding[1],
                        appIconPadding[2], appIconPadding[3]);
            }
            if (mColorMode != 0) {
                colorize = false;
            }
        }

        Drawable d = null;
        Log.d("TEST", "clickAction=" + clickAction);
        Log.d("TEST", "iconUri=" + iconUri);
        if (clickAction.startsWith("**") && (iconUri == null ||
                iconUri.equals(ActionConstants.ICON_EMPTY))) {
            if (clickAction.equals(ActionConstants.ACTION_HOME)) {
                d = getResources().getDrawable(R.drawable.ic_sysbar_home);
            } else if (clickAction.equals(ActionConstants.ACTION_BACK)) {
                d = getResources().getDrawable(R.drawable.ic_sysbar_back);
            } else if (clickAction.equals(ActionConstants.ACTION_RECENTS)) {
                d = getResources().getDrawable(R.drawable.ic_sysbar_recent);
            }
        }
        if (d == null) {
            d = ActionHelper.getActionIconImage(mContext, clickAction, iconUri);
        }

        if (d != null) {
            d.mutate();
            if (colorize && mColorMode != 3) {
                d = ImageHelper.getColoredDrawable(d, mButtonColor);
            }
            setImageBitmap(ImageHelper.drawableToBitmap(d));
        }
    }

    private int[] getAppIconPadding() {
        int[] padding = new int[4];
        // left
        padding[0] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
                .getDisplayMetrics());
        // top
        padding[1] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        // right
        padding[2] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources()
                .getDisplayMetrics());
        // bottom
        padding[3] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                getResources()
                        .getDisplayMetrics());
        return padding;
    }

    public void setRippleColor(int color) {
        mRipple.setColor(color);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (mEditing) return false;
        final int action = ev.getAction();
        int x, y;
        if (action == MotionEvent.ACTION_DOWN) {
            mGestureAborted = false;
        }
        if (mGestureAborted) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = SystemClock.uptimeMillis();
                mIsLongpressed = false;
                setPressed(true);
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (mClickAction.equals(ActionConstants.ACTION_RECENTS)
                        || mLongpressAction.equals(ActionConstants.ACTION_RECENTS)
                        || mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                    mActionsManager.preloadRecentApps();
                }
                if (mDoubleTapPending) {
                    mDoubleTapPending = false;
                    removeCallbacks(mDoubleTapTimeout);
                    doubleTap();
                    mDoubleTapConsumed = true;
                } else {
                    removeCallbacks(mCheckLongPress);
                    postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x = (int)ev.getX();
                y = (int)ev.getY();
                setPressed(x >= -mTouchSlop
                        && x < getWidth() + mTouchSlop
                        && y >= -mTouchSlop
                        && y < getHeight() + mTouchSlop);
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                if (mClickAction.equals(ActionConstants.ACTION_RECENTS)
                        || mLongpressAction.equals(ActionConstants.ACTION_RECENTS)
                        || mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                    mActionsManager.cancelPreloadRecentApps();
                }
                // hack to fix ripple getting stuck. exitHardware() starts an animation,
                // but sometimes does not finish it.
                mRipple.exitSoftware();
                removeCallbacks(mCheckLongPress);
                break;
            case MotionEvent.ACTION_UP:
                final boolean doIt = isPressed();
                setPressed(false);
                if (!doIt && mClickAction.equals(ActionConstants.ACTION_RECENTS)
                        || mLongpressAction.equals(ActionConstants.ACTION_RECENTS)
                        || mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
                    mActionsManager.cancelPreloadRecentApps();
                }
                if (!mIsLongpressed) {
                    if (hasDoubleTapAction()) {
                        if (mDoubleTapConsumed) {
                            mDoubleTapConsumed = false;
                        } else {
                            mDoubleTapPending = true;
                            postDelayed(mDoubleTapTimeout,
                                    ViewConfiguration.getDoubleTapTimeout() - 100);
                        }
                    }
                    if (doIt && !hasDoubleTapAction()) {
                        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                        performClick();
                    }
                }
                removeCallbacks(mCheckLongPress);
                break;
        }

        mHandler.post(mNavButtonDimActivator);

        return true;
    }

    private final Runnable mNavButtonDimActivator = new Runnable() {
        @Override
        public void run() {
            ViewParent parent = getParent();
            while (parent != null && !(parent instanceof SlimNavigationBarView)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                ((SlimNavigationBarView) parent).onNavButtonTouched();
            }
        }
    };

    private boolean hasDoubleTapAction() {
        return !mDoubleTapAction.equals(ActionConstants.ACTION_NULL);
    }

    public void playSoundEffect(int soundConstant) {
        mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mClickAction.equals(ActionConstants.ACTION_RECENTS)) {
                mActionsManager.cancelPreloadRecentApps();
            }
            Action.processAction(mContext, mClickAction, false);
            return;
        }
    };

    public void setLongClickCallback(LongClickCallback c) {
        mCallback = c;
        setLongClickable(true);
        setOnLongClickListener(mLongPressListener);
    }

    private OnLongClickListener mLongPressListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            boolean b = true;
            if (mCallback != null) {
                if (mCallback.onLongClick(v)) {
                    b = false;
                }
            }
            if (b) {
                if (!mLongpressAction.equals(ActionConstants.ACTION_RECENTS)) {
                    mActionsManager.cancelPreloadRecentApps();
                }
                Action.processAction(mContext, mLongpressAction, true);
            }
            return true;
        }
    };

    public void abortCurrentGesture() {
        setPressed(false);
        mGestureAborted = true;
    }

    private void doubleTap() {
        if (!mDoubleTapAction.equals(ActionConstants.ACTION_RECENTS)) {
            mActionsManager.cancelPreloadRecentApps();
        }
        Action.processAction(mContext, mDoubleTapAction, true);
    }

    public interface LongClickCallback {
        public boolean onLongClick(View v);
    }
}
