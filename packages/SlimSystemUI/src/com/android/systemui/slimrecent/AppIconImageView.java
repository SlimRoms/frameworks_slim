/*
 * Copyright (C) 2018 Android Ice Cold Project
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

import android.content.Context;
import android.util.AttributeSet;

public class AppIconImageView extends RecentImageView {

    public AppIconImageView(Context context) {
        super(context);
    }

    public AppIconImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppIconImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Square view, adapting to given height
        int height = getMeasuredHeight();
        setMeasuredDimension(height, height);
    }

}
