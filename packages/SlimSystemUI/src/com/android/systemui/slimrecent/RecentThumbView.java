/*
 * Copyright (C) 2017 ABC rom
 * Copyright (C) 2014-2016 SlimRoms Project
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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.android.systemui.R;

/**
 * Thanks to a google post from Jorim Jaggy I saw
 * this nice trick to reduce requestLayout calls.
 *
 * https://plus.google.com/+JorimJaggi/posts/iTk4PjgeAWX
 *
 * We handle in SlimRecent always exactly same measured
 * bitmaps and drawables. So we do not need a
 * requestLayout call at all when we use setImageBitmap
 * or setImageDrawable. Due that setImageBitmap directly
 * calls setImageDrawable (#link:ImageView) it is enough
 * to block this behaviour on setImageDrawable.
 */

public class RecentThumbView extends ImageView {

    private boolean mBlockLayout;
    private Bitmap mBitmap;
    private float mThumbnailHeightToWidthRatio;

    public RecentThumbView(Context context) {
        super(context);
        init(context);
    }

    public RecentThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecentThumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mThumbnailHeightToWidthRatio = context.getResources()
                .getInteger(R.integer.recent_thumbnail_height_to_width_ratio_percent) / 100f;
    }


    @Override
    public void requestLayout() {
        if (!mBlockLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mBlockLayout = true;
        super.setImageDrawable(drawable);
        mBlockLayout = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = (int) (width * mThumbnailHeightToWidthRatio);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
            if (bitmap != null && bitmap.isRecycled()) {
                return;
            }
            if (!canvas.isHardwareAccelerated()) {
                return;
            }
            /*canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                            Paint.FILTER_BITMAP_FLAG));*/
            int h = bitmap.getHeight();
            int w = bitmap.getWidth();
            int myWidth = getMeasuredWidth();
            int myHeight = getMeasuredHeight();
            Rect src;
            if ((((float) h) / myHeight) > (((float) w) / myWidth)) {
                src = new Rect(0, 0, w, myHeight*w/myWidth);
            } else {
                src = new Rect(0, 0, myWidth*h/myHeight, h);
            }
            final RectF targetRect = new RectF(0.0f, 0.0f, myWidth, myHeight);
            canvas.drawBitmap(bitmap, src, targetRect, null);
        }
    }
}

