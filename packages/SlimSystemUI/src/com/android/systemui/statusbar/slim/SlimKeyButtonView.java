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

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SlimNavigationBarView;
import com.android.systemui.statusbar.policy.KeyButtonView;

import java.util.ArrayList;

import slim.action.ActionConstants;
import slim.action.Action;
import slim.action.SlimActionsManager;

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

    public void setRippleColor(int color) {
        mRipple.setColor(color);
    }

    public boolean onTouchEvent(MotionEvent ev) {
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

    public void setDarkIntensity(float darkIntensity) {
        Drawable drawable = getDrawable();
        if (drawable != null && (drawable instanceof SlimKeyButtonDrawable)) {
            ((SlimKeyButtonDrawable) getDrawable()).setDarkIntensity(darkIntensity);

            // Since we reuse the same drawable for multiple views, we need to invalidate the view
            // manually.
            invalidate();
        }
        //mRipple.setDarkIntensity(darkIntensity);
    }

    public interface LongClickCallback {
        public boolean onLongClick(View v);
    }
}
