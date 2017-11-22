/*
* Copyright (C) 2016-2017 SlimRoms Project
* Copyright (C) 2013-14 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.systemui.statusbar.slim;

import android.animation.ArgbEvaluator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.content.ContentResolver;

import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.DemoMode;
import com.android.systemui.statusbar.policy.BatteryController;

import slim.provider.SlimSettings;

public class SlimBatteryMeterView extends View implements DemoMode,
        BatteryController.BatteryStateChangeCallback {
    public static final String TAG = SlimBatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    public static final boolean ENABLE_PERCENT = true;
    private static final boolean SINGLE_DIGIT_PERCENT = false;

    private static final int FULL = 96;
    private static final int ADD_LEVEL = 10;
    private static final int ANIM_DURATION = 500;
    private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction

    private final int[] mColors;

    protected boolean mShowPercent;
    private float mButtonHeightFraction;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private int mAnimOffset;

    public static enum BatteryMeterMode {
        BATTERY_METER_GONE,
        BATTERY_METER_ICON_PORTRAIT,
        BATTERY_METER_ICON_LANDSCAPE,
        BATTERY_METER_CIRCLE,
        BATTERY_METER_DOTTED_CIRCLE,
        BATTERY_METER_TEXT
    }

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private final int mCriticalLevel;
    private final int mFrameColor;

    private final Path mShapePath = new Path();
    private final Path mClipPath = new Path();
    private final Path mTextPath = new Path();

    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    private int mForegroundColor;
    private int mBackgroundColor;

    private final Handler mHandler;

    protected BatteryMeterMode mMeterMode = null;

    protected boolean mAttached;

    private int mLevel;
    private boolean mCharging;

    private boolean mDemoMode;
    private BatteryMeterDrawable mBatteryMeterDrawable;
    private final Object mLock = new Object();

    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            invalidateIfVisible();
        }
    };

    public void updateBatteryIconSettings() {
        loadShowBatterySetting();
        postInvalidate();
    };

    private void loadShowBatterySetting() {
        ContentResolver resolver = mContext.getContentResolver();

        boolean showInsidePercent = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_PERCENT, 0) == 1;

        int batteryStyle = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_STYLE, 0);
        BatteryMeterMode meterMode = BatteryMeterMode.BATTERY_METER_GONE;
        switch (batteryStyle) {
            case 0:
                meterMode = BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT;
                break;

            case 2:
                meterMode = BatteryMeterMode.BATTERY_METER_CIRCLE;
                break;

            case 3:
                meterMode = BatteryMeterMode.BATTERY_METER_DOTTED_CIRCLE;
                break;

            case 4:
                meterMode = BatteryMeterMode.BATTERY_METER_GONE;
                showInsidePercent = false;
                break;

            case 5:
                meterMode = BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE;
                break;

            case 6:
                meterMode = BatteryMeterMode.BATTERY_METER_TEXT;
                showInsidePercent = false;
                break;

            default:
                break;
        }

        setMode(meterMode);
        setShowPercent(showInsidePercent);
    }

    public SlimBatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public SlimBatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlimBatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();
        final Resources res = context.getResources();
        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        mFrameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                res.getColor(R.color.meter_background_color));
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        final int N = levels.length();
        mColors = new int[2 * N];
        for (int i=0; i < N; i++) {
            mColors[2 * i] = levels.getInt(i, 0);
            if (colors.getType(i) == TypedValue.TYPE_ATTRIBUTE) {
                mColors[2 * i + 1] = Utils.getColorAttr(context, colors.getThemeAttributeId(i, 0));
            } else {
                mColors[2 * i + 1] = colors.getColor(i, 0);
            }
        }
        levels.recycle();
        colors.recycle();
        atts.recycle();
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        mButtonHeightFraction = context.getResources().getFraction(
                R.fraction.battery_button_height_fraction, 1, 1);
        mSubpixelSmoothingLeft = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_left, 1, 1);
        mSubpixelSmoothingRight = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_right, 1, 1);

        loadShowBatterySetting();
        mBatteryMeterDrawable = createBatteryMeterDrawable(mMeterMode);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBatteryController != null) {
            mBatteryController.addCallback(this);
        }
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached = false;
        if (mBatteryController != null) {
            mBatteryController.removeCallback(this);
        }
    }

    protected BatteryMeterDrawable createBatteryMeterDrawable(BatteryMeterMode mode) {
        Resources res = mContext.getResources();
        switch (mode) {
            case BATTERY_METER_DOTTED_CIRCLE:
            case BATTERY_METER_CIRCLE:
                return new CircleBatteryMeterDrawable(res);

            case BATTERY_METER_ICON_LANDSCAPE:
                return new NormalBatteryMeterDrawable(res, true);

            case BATTERY_METER_ICON_PORTRAIT:
                return new NormalBatteryMeterDrawable(res, false);

            default:
                return null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mMeterMode == BatteryMeterMode.BATTERY_METER_CIRCLE ||
                mMeterMode == BatteryMeterMode.BATTERY_METER_DOTTED_CIRCLE) {
            height += (CircleBatteryMeterDrawable.STROKE_WITH / 3);
            width = height;
        } else if (mMeterMode == BatteryMeterMode.BATTERY_METER_TEXT) {
            onSizeChanged(width, height, 0, 0); // Force a size changed event
        } else if (mMeterMode.compareTo(BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE) == 0) {
            width = (int)(height * 1.2f);
        }

        setMeasuredDimension(width, height);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        if (mAttached) {
            mBatteryController.addCallback(this);
        }
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        if (mDemoMode) return;
        mCharging = pluggedIn || charging;
        mLevel = level;
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                setVisibility(View.VISIBLE);
                invalidateIfVisible();
            }
        }
    }

    @Override
    public void onPowerSaveChanged(boolean enabled) {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                mBatteryMeterDrawable.onSizeChanged(w, h, oldw, oldh);
            }
        }
    }

    protected void invalidateIfVisible() {
        if (getVisibility() == View.VISIBLE && mAttached) {
            if (mAttached) {
                postInvalidate();
            } else {
                invalidate();
            }
        }
    }

    public void setShowPercent(boolean show) {
        if (ENABLE_PERCENT) {
            mShowPercent = show;
            invalidateIfVisible();
        }
    }

    public void setMode(BatteryMeterMode mode) {
        if (mMeterMode == mode) {
            return;
        }

        mMeterMode = mode;
        if (mode == BatteryMeterMode.BATTERY_METER_GONE ||
            mode == BatteryMeterMode.BATTERY_METER_TEXT   ) {
            setVisibility(View.GONE);
            synchronized (mLock) {
                mBatteryMeterDrawable = null;
            }
        } else {
            synchronized (mLock) {
                if (mBatteryMeterDrawable != null) {
                    mBatteryMeterDrawable.onDispose();
                }
                mBatteryMeterDrawable = createBatteryMeterDrawable(mode);
            }
            if (mMeterMode == BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT ||
                    mMeterMode == BatteryMeterMode.BATTERY_METER_ICON_LANDSCAPE) {
                ((NormalBatteryMeterDrawable)mBatteryMeterDrawable).loadBoltPoints(
                        mContext.getResources());
            }

            setVisibility(View.VISIBLE);
            postInvalidate();
            requestLayout();
        }
    }

    public int getColorForLevel(int percent) {

        // If we are in power save mode, always use the normal color.
        if (mPowerSaveEnabled) {
            return mColors[mColors.length-1];
        }
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) {

                // Respect tinting for "normal" level
                if (i == mColors.length-2) {
                    return mForegroundColor;
                } else {
                    return color;
                }
            }
        }
        return color;
    }

    public void setColors(int foreground, int background) {
        mForegroundColor = foreground;
        mBackgroundColor = background;
        invalidate();
    }

    private int updateChargingAnimLevel() {
        int curLevel = mLevel;

        if (mCharging) {
            mAnimOffset = 0;
            mHandler.removeCallbacks(mInvalidate);
        } else {
            curLevel += mAnimOffset;
            if (curLevel >= FULL) {
                curLevel = 100;
                mAnimOffset = 0;
            } else {
                mAnimOffset += ADD_LEVEL;
            }

            mHandler.removeCallbacks(mInvalidate);
            mHandler.postDelayed(mInvalidate, ANIM_DURATION);
        }
        return curLevel;
    }

    @Override
    public void draw(Canvas c) {
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                mBatteryMeterDrawable.onDraw(c);
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (getVisibility() == View.VISIBLE) {
            if (!mDemoMode && command.equals(COMMAND_ENTER)) {
                mDemoMode = true;
            } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
                mDemoMode = false;
                postInvalidate();
            } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
               String level = args.getString("level");
               String plugged = args.getString("plugged");
               if (level != null) {
                   mLevel = Math.min(Math.max(Integer.parseInt(level), 0), 100);
               }
               if (plugged != null) {
                   mCharging = Boolean.parseBoolean(plugged);
               }
               postInvalidate();
            }
        }
    }

    protected interface BatteryMeterDrawable {
        void onDraw(Canvas c);
        void onSizeChanged(int w, int h, int oldw, int oldh);
        void onDispose();
    }

    protected class NormalBatteryMeterDrawable implements BatteryMeterDrawable {
        public static final boolean SINGLE_DIGIT_PERCENT = false;
        public static final boolean SHOW_100_PERCENT = false;

        private boolean mDisposed;

        private boolean mShowBolt;      // stores charge-animation state (text or bolt)
        private boolean mIsAnimating;   // stores charge-animation status to reliably
                                        // remove callbacks

        protected final boolean mHorizontal;

        private final Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
        private float mTextHeight, mWarningTextHeight;

        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mBoltFrame = new RectF();

        public NormalBatteryMeterDrawable(Resources res, boolean horizontal) {
            super();
            mHorizontal = horizontal;
            mDisposed = false;

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setColor(mFrameColor);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBatteryPaint.setDither(true);
            mBatteryPaint.setStrokeWidth(0);
            mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWarningTextPaint.setColor(mColors[1]);
            font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBoltPaint.setColor(res.getColor(R.color.batterymeter_bolt_color));
            mBoltPoints = loadBoltPoints(res);
        }

        @Override
        public void onDraw(Canvas c) {
            if (mDisposed) return;

            final int level = mLevel;

            updateChargeAnim();

            float drawFrac = (float) level / 100f;
            final int pt = getPaddingTop();
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            final int pb = getPaddingBottom();
            final int height = mHeight - pt - pb;
            final int width = mWidth - pl - pr;

            final int buttonHeight = (int) ((mHorizontal ? width : height) * mButtonHeightFraction);

            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

            if (mHorizontal) {
                mButtonFrame.set(
                        /*cover frame border of intersecting area*/
                //set(float left, float top, float right, float bottom)
                        width - buttonHeight - mFrame.left,
                        mFrame.top + Math.round(height * 0.25f),
                        mFrame.right,
                        mFrame.bottom - Math.round(height * 0.25f));
                        //mFrame.bottom);

                mButtonFrame.top += mSubpixelSmoothingLeft;
                mButtonFrame.bottom -= mSubpixelSmoothingRight;
                mButtonFrame.right -= mSubpixelSmoothingRight;
            } else {
                // button-frame: area above the battery body
                mButtonFrame.set(
                        mFrame.left + Math.round(width * 0.25f),
                        mFrame.top,
                        mFrame.right - Math.round(width * 0.25f),
                        mFrame.top + buttonHeight);

                mButtonFrame.top += mSubpixelSmoothingLeft;
                mButtonFrame.left += mSubpixelSmoothingLeft;
                mButtonFrame.right -= mSubpixelSmoothingRight;
            }

            // frame: battery body area

            if (mHorizontal) {
                mFrame.right -= buttonHeight;
            } else {
                mFrame.top += buttonHeight;
            }
            mFrame.left += mSubpixelSmoothingLeft;
            mFrame.top += mSubpixelSmoothingLeft;
            mFrame.right -= mSubpixelSmoothingRight;
            mFrame.bottom -= mSubpixelSmoothingRight;

            // set the battery charging color
            mBatteryPaint.setColor(mCharging ? mForegroundColor : getColorForLevel(level));

            if (level >= FULL) {
                drawFrac = 1f;
            } else if (level <= mCriticalLevel) {
                drawFrac = 0f;
            }

            final float levelTop;

            if (drawFrac == 1f) {
                if (mHorizontal) {
                    levelTop = mButtonFrame.right;
                } else {
                    levelTop = mButtonFrame.top;
                }
            } else {
                if (mHorizontal) {
                    levelTop = (mFrame.right - (mFrame.width() * (1f - drawFrac)));
                } else {
                    levelTop = (mFrame.top + (mFrame.height() * (1f - drawFrac)));
                }
            }

            // define the battery shape
            mShapePath.reset();
            mShapePath.moveTo(mButtonFrame.left, mButtonFrame.top);
            if (mHorizontal) {
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.bottom);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.bottom);
                mShapePath.lineTo(mFrame.right, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);
            } else {
                mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
                mShapePath.lineTo(mButtonFrame.right, mFrame.top);
                mShapePath.lineTo(mFrame.right, mFrame.top);
                mShapePath.lineTo(mFrame.right, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.bottom);
                mShapePath.lineTo(mFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mFrame.top);
                mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);
            }

            if (mCharging && mShowBolt) {
                // define the bolt shape
                final float bl = mFrame.left + mFrame.width() / (mHorizontal ? 9f : 4.5f);
                final float bt = mFrame.top + mFrame.height() / (mHorizontal ? 4.5f : 6f);
                final float br = mFrame.right - mFrame.width() / (mHorizontal ? 6f : 7f);
                final float bb = mFrame.bottom - mFrame.height() / (mHorizontal ? 7f : 10f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }

                mBoltPaint.setColor(mForegroundColor);
                if (level <= 25) { // valid for portrai and landscape
                    // draw the bolt if opaque
                    c.drawPath(mBoltPath, mBoltPaint);
                } else {
                    // otherwise cut the bolt out of the overall shape
                    mShapePath.op(mBoltPath, Path.Op.DIFFERENCE);
                }
            }

            // compute percentage text
            boolean pctOpaque = false;
            float pctX = 0, pctY = 0;
            String pctText = null;
            if ((!mCharging || (mCharging && !mShowBolt)) && level > mCriticalLevel &&
                     (mShowPercent && !(mLevel == 100 && !SHOW_100_PERCENT))) {
                mTextPaint.setColor(getColorForLevel(level));
                final float full = mHorizontal ? 0.60f : 0.45f;
                final float nofull = mHorizontal ? 0.75f : 0.6f;
                final float single = mHorizontal ? 0.86f : 0.75f;
                mTextPaint.setTextSize(height *
                        (SINGLE_DIGIT_PERCENT ? single
                                : (mLevel == 100 ? full : nofull)));
                mTextHeight = -mTextPaint.getFontMetrics().ascent;
                pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
                pctX = mWidth * 0.5f;
                pctY = (mHeight + mTextHeight) * 0.47f;
                if (mHorizontal) {
                    pctOpaque = level <= 25; // Switch at 25% for landscape icon
                } else {
                    pctOpaque = levelTop > pctY;
                }
                if (!pctOpaque) {
                    mTextPath.reset();
                    mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                    // cut the percentage text out of the overall shape
                    mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
                }
            }

            mFramePaint.setColor(mBackgroundColor);
            // draw the battery shape background
            c.drawPath(mShapePath, mFramePaint);

            // draw the battery shape, clipped to charging level
            if (mHorizontal) {
                mFrame.right = levelTop;
            } else {
                mFrame.top = levelTop;
            }
            mClipPath.reset();
            mClipPath.addRect(mFrame, Path.Direction.CCW);
            mShapePath.op(mClipPath, Path.Op.INTERSECT);
            c.drawPath(mShapePath, mBatteryPaint);

            if (!mCharging) {
                if (level <= mCriticalLevel) {
                    // draw the warning text
                    final float x = mWidth * 0.5f;
                    final float y = (mHeight + mWarningTextHeight) * 0.48f;
                    c.drawText(mWarningString, x, y, mWarningTextPaint);
                }
            }

            if (pctOpaque && (!mCharging || (mCharging && !mShowBolt)) &&
                     level > mCriticalLevel &&
                     (mShowPercent && !(mLevel == 100 && !SHOW_100_PERCENT))) {
                // draw the percentage text
                c.drawText(pctText, pctX, pctY, mTextPaint);
            }
        }

        @Override
        public void onDispose() {
            mHandler.removeCallbacks(mInvalidate);
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mHeight = h;
            mWidth = w;
            mWarningTextPaint.setTextSize(h * 0.75f);
            mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
        }

        private float[] loadBoltPoints(Resources res) {
            final int[] pts = res.getIntArray((mHorizontal
                                                ? R.array.batterymeter_inverted_bolt_points
                                                : R.array.batterymeter_bolt_points));
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }

        /**
         * updates the animation
         * cares for timed callbacks to continue animation cycles
         * uses mInvalidate for delayed invalidate() callbacks
         */
        private void updateChargeAnim() {
            // Stop animation when battery is full
            if (!mCharging || mLevel == 100) {
                if (mIsAnimating) {
                    mIsAnimating = false;
                    mShowBolt = false; // reset to not show bolt
                    mHandler.removeCallbacks(mInvalidate);
                }
                return;
            }

            mIsAnimating = true;

            mShowBolt = !mShowBolt; // toggle bolt to/from text

            mHandler.removeCallbacks(mInvalidate);
            mHandler.postDelayed(mInvalidate, 1000);
        }

    }

    protected class CircleBatteryMeterDrawable implements BatteryMeterDrawable {
        public static final boolean SINGLE_DIGIT_PERCENT = false;
        public static final boolean SHOW_100_PERCENT = false;
        public static final boolean ENABLE_PERCENT = true;

        private static final int FULL = 96;

        public static final float STROKE_WITH = 6.5f;

        private boolean mDisposed;

        private DashPathEffect mPathEffect;

        private int     mAnimOffset;
        private boolean mShowBolt;      // stores charge-animation state (text or bolt)
        private boolean mIsAnimating;   // stores charge-animation status to reliably
                                        //remove callbacks

        private int     mCircleSize;    // draw size of circle
        private RectF   mRectLeft;      // contains the precalculated rect used in drawArc(),
                                        // derived from mCircleSize
        private float   mTextX, mTextY; // precalculated position for drawText() to appear centered

        private Paint   mTextPaint;
        private Paint   mFrontPaint;
        private Paint   mBackPaint;
        private Paint   mBoltPaint;
        private Paint   mWarningTextPaint;

        private final RectF mBoltFrame = new RectF();

        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        public CircleBatteryMeterDrawable(Resources res) {
            super();
            mDisposed = false;

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mFrontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFrontPaint.setStrokeCap(Paint.Cap.BUTT);
            mFrontPaint.setDither(true);
            mFrontPaint.setStrokeWidth(0);
            mFrontPaint.setStyle(Paint.Style.STROKE);

            mBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            //mBackPaint.setColor(res.getColor(R.color.batterymeter_frame_color));
            mBackPaint.setStrokeCap(Paint.Cap.BUTT);
            mBackPaint.setDither(true);
            mBackPaint.setStrokeWidth(0);
            mBackPaint.setStyle(Paint.Style.STROKE);
            mBackPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWarningTextPaint.setColor(mColors[1]);
            font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            //mChargeColor = getResources().getColor(R.color.batterymeter_charge_color);

            mPathEffect = new DashPathEffect(new float[]{3,2},0);

            mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBoltPaint.setColor(res.getColor(R.color.batterymeter_bolt_color));
            mBoltPoints = loadBoltPoints(res);
        }

        @Override
        public void onDraw(Canvas c) {
            if (mDisposed) return;

            if (mRectLeft == null) {
                initSizeBasedStuff();
            }

            updateChargeAnim();
            drawCircle(c, mTextX, mRectLeft);
        }

        @Override
        public void onDispose() {
            mHandler.removeCallbacks(mInvalidate);
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            initSizeBasedStuff();
        }

        private float[] loadBoltPoints(Resources res) {
            final int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }

        private void drawCircle(Canvas canvas, float textX, RectF drawRect) {
            int level = mLevel;
            Paint paint;

            paint = mFrontPaint;
            paint.setColor(getColorForLevel(level));

            if (mMeterMode == BatteryMeterMode.BATTERY_METER_DOTTED_CIRCLE) {
                paint.setPathEffect(mPathEffect);
            } else {
                paint.setPathEffect(null);
            }

            mBackPaint.setColor(mBackgroundColor);
            // draw thin gray ring first
            canvas.drawArc(drawRect, 270, 360, false, mBackPaint);
            // draw colored arc representing charge level
            canvas.drawArc(drawRect, 270 + mAnimOffset, 3.6f * level, false, paint);
            // if chosen by options, draw percentage text in the middle
            // always skip percentage when 100, so layout doesnt break
            if (mCharging && mShowBolt) {
                // draw the bolt
                final float bl = (int)(drawRect.left + drawRect.width() / 3.2f);
                final float bt = (int)(drawRect.top + drawRect.height() / 4f);
                final float br = (int)(drawRect.right - drawRect.width() / 5.2f);
                final float bb = (int)(drawRect.bottom - drawRect.height() / 8f);
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }
                mBoltPaint.setColor(mForegroundColor);
                canvas.drawPath(mBoltPath, mBoltPaint);

            } else {
                if (level > mCriticalLevel
                        && (mShowPercent && !(mLevel == 100 && !SHOW_100_PERCENT))) {
                    // draw the percentage text
                    String pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
                    mTextPaint.setColor(getColorForLevel(level));
                    canvas.drawText(pctText, textX, mTextY, mTextPaint);
                } else if (level <= mCriticalLevel) {
                    // draw the warning text
                    canvas.drawText(mWarningString, textX, mTextY, mWarningTextPaint);
                }
            }
        }

        /**
         * updates the animation counter
         * cares for timed callbacks to continue animation cycles
         * uses mInvalidate for delayed invalidate() callbacks
         */
        private void updateChargeAnim() {
            // Stop animation when battery is full or after the meter
            // rotated back to 0 after unplugging.
            if (!mCharging && mAnimOffset == 0 || mLevel == 100) {
                if (mIsAnimating) {
                    mIsAnimating = false;
                    mShowBolt = true; // reset to show bolt
                    mAnimOffset = 0;
                    mHandler.removeCallbacks(mInvalidate);
                }
                return;
            }

            mIsAnimating = true;

            if (mAnimOffset > 360) {
                mAnimOffset = 0;
            } else {
                mAnimOffset += 3;
            }

            // alternate every second (60 degrees per second)
            if (mAnimOffset > 300) {
                mShowBolt = false; // set to not show bolt
            } else if (mAnimOffset > 240) {
                mShowBolt = true;  // set to show bolt
            } else if (mAnimOffset > 180) {
                mShowBolt = false; // set to not show bolt
            } else if (mAnimOffset > 120) {
                mShowBolt = true;  // set to show bolt
            } else if (mAnimOffset > 60) {
                mShowBolt = false; // set to not show bolt
            } else {
                mShowBolt = true;  // set to show bolt
            }

            mHandler.removeCallbacks(mInvalidate);
            mHandler.postDelayed(mInvalidate, 50);
        }

        /**
         * initializes all size dependent variables
         * sets stroke width and text size of all involved paints
         * YES! i think the method name is appropriate
         */
        private void initSizeBasedStuff() {
            mCircleSize = Math.min(getMeasuredWidth(), getMeasuredHeight());
            mTextPaint.setTextSize(mCircleSize / 2f);

            float strokeWidth = mCircleSize / STROKE_WITH;
            mFrontPaint.setStrokeWidth(strokeWidth);
            mBackPaint.setStrokeWidth(strokeWidth);

            // calculate rectangle for drawArc calls
            int pLeft = getPaddingLeft();
            mRectLeft = new RectF(pLeft + strokeWidth / 2.0f, 0 + strokeWidth / 2.0f, mCircleSize
                    - strokeWidth / 2.0f + pLeft, mCircleSize - strokeWidth / 2.0f);

            // calculate Y position for text
            Rect bounds = new Rect();
            mTextPaint.getTextBounds("99", 0, "99".length(), bounds);
            mTextX = mCircleSize / 2.0f + getPaddingLeft();
            // the +1dp at end of formula balances out rounding issues.works out on all resolutions
            mTextY = mCircleSize / 2.0f + (bounds.bottom - bounds.top) / 2.0f
                    - strokeWidth / 2.0f + getResources().getDisplayMetrics().density;
        }
    }
}
