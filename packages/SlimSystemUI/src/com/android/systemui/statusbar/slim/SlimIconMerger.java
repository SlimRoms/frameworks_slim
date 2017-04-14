package com.android.systemui.statusbar.slim;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.IconMerger;

public class SlimIconMerger extends IconMerger {

    private int mIconSize;
    private int mIconHPadding;

    private View mMoreView;

    public SlimIconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        reloadDimens();
    }

    private void reloadDimens() {
        Resources res = getContext().getResources();
        mIconSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadDimens();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
    }

    private int getFullIconWidth() {
        return mIconSize + 2 * mIconHPadding;
    }

    @Override
    public void setOverflowIndicator(View v) {
        mMoreView = v;
    }

    private void checkOverflow(int width) {
        if (mMoreView == null) return;

        final int N = getChildCount();
        int visibleChildren = 0;
        for (int i=0; i<N; i++) {
            if (getChildAt(i).getVisibility() != GONE) visibleChildren++;
        }
        final boolean overflowShown = (mMoreView.getVisibility() == View.VISIBLE);
        // let's assume we have one more slot if the more icon is already showing
        if (overflowShown) visibleChildren --;
        final boolean moreRequired = visibleChildren * getFullIconWidth() > width;
        if (moreRequired != overflowShown) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMoreView.setVisibility(moreRequired ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}
