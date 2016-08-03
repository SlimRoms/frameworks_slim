package com.slim.settings.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import slim.utils.ImageHelper;

import com.slim.settings.R;
import com.slim.settings.widget.RecyclerViewFastScroller;

public class IconPickerActivity extends AppCompatActivity {

    public static final String SELECTED_RESOURCE_EXTRA = "selected_resource";
    public static final String SELECTED_BITMAP_EXTRA = "bitmap";

    private int mIconSize;

    private Snackbar mLoadingSnackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String pkgName = getIntent().getStringExtra("package");

        if (TextUtils.isEmpty(pkgName)) {
            setResult(Activity.RESULT_CANCELED, null);
            finish();
        }

        setContentView(R.layout.icon_picker_activity);

        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mIconSize = activityManager.getLauncherLargeIconSize();
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        float dpWidth = metrics.widthPixels / metrics.density;
        int columns = Math.round(dpWidth / dpiFromPx(mIconSize, metrics));
        columns -= 2;

        CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.layout);
        layout.setBackgroundColor(getColor(R.color.material_grey_800));
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        //recyclerView.setBackgroundColor(0x77000000);
//        layout.addView(recyclerView);

        RecyclerViewFastScroller scroller = (RecyclerViewFastScroller) findViewById(R.id.fast_scroller);
  //      layout.addView(scroller);
        scroller.setRecyclerView(recyclerView);
        scroller.setViewsToUse(R.layout.fast_scrollbar,
                R.id.fastscroller_bubble, R.id.fastscroller_handle);

        final GridLayoutManager layoutManager = new GridLayoutManager(this, columns);
        recyclerView.setLayoutManager(layoutManager);

        final ImageAdapter adapter = new ImageAdapter(this, pkgName);
        recyclerView.setAdapter(adapter);

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? layoutManager.getSpanCount() : 1;
            }
        });

        PackageManager pm = getPackageManager();
        ApplicationInfo ai = null;
        try {
            ai = pm.getApplicationInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
            // ignore
        }
        setTitle(ai != null ? pm.getApplicationLabel(ai) : getTitle());

        mLoadingSnackbar = Snackbar.make(layout,
                R.string.loading, Snackbar.LENGTH_INDEFINITE);
        mLoadingSnackbar.show();
    }

    public static float dpiFromPx(int size, DisplayMetrics metrics) {
        float densityRatio = (float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (size / densityRatio);
    }

    public static class Item {
        String title;
        boolean isHeader = false;
        boolean isIcon = false;
        WeakReference<Drawable> drawable;
        int resource_id;
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ImageViewHolder(View view) {
            super(view);
            imageView = (ImageView) view;
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public HeaderViewHolder(View view) {
            super(view);
            textView = (TextView) view;
        }
    }

    public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
            RecyclerViewFastScroller.BubbleTextGetter {
        public final int ITEM_VIEW_TYPE_HEADER = 0;
        public final int ITEM_VIEW_TYPE_ITEM = 1;
        private Context mContext;
        private Resources mResources;
        private ArrayList<Item> mItems = new ArrayList<>();
        private String mIconPackageName;

        public ImageAdapter(Context c, String pkgName) {
            mContext = c;
            mIconPackageName = pkgName;
            final ArrayList<Item> tempItems = IconPackHelper.getCustomIconPackResources(c, pkgName);
            if (tempItems != null && tempItems.size() > 0) {
                new AsyncTask<Void, Item, Void>() {
                    @Override
                    protected Void doInBackground(Void... v) {
                        try {
                            mResources = c.getPackageManager().getResourcesForApplication(pkgName);
                            for (Item i : tempItems) {
                                int id = mResources.getIdentifier(i.title, "drawable", pkgName);
                                if (id != 0) {
                                    i.resource_id = id;
                                    publishProgress(i);
                                } else {
                                    if (i.isHeader) {
                                        publishProgress(i);
                                    }
                                }
                            }
                        } catch (NameNotFoundException e) {
                            // ignore
                        }
                        return null;
                    }

                    @Override
                    public void onProgressUpdate(Item... item) {
                        mItems.add(item[0]);
                        notifyItemInserted(mItems.indexOf(item[0]));
                    }

                    @Override
                    public void onPostExecute(Void v) {
                        mLoadingSnackbar.dismiss();
                    }
                }.execute();
            }
        }

        @Override
        public String getTextToShowInBubble(int pos) {
            android.util.Log.d("TEST", "pos=" + pos);
            if (pos >= mItems.size()) return "";
            for (int i = pos; i > 0; i--) {
                android.util.Log.d("TEST", "i=" + i);
                Item item = mItems.get(i);
                if (item.isHeader) {
                    android.util.Log.d("TEST", "title=" + item.title);
                    return item.title;
                }
            }
            return "";
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_VIEW_TYPE_ITEM) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new GridLayoutManager.LayoutParams(mIconSize, mIconSize));
                imageView.setPadding(10, 10, 10, 10);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                return new ImageViewHolder(imageView);
            } else if (viewType == ITEM_VIEW_TYPE_HEADER) {
                TextView textView = (TextView) View.inflate(mContext, R.layout.header_view, null);
                return new HeaderViewHolder(textView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

            if (mItems.get(position).isHeader) {
                HeaderViewHolder vHolder = (HeaderViewHolder) holder;
                vHolder.textView.setText(mItems.get(position).title);
            } else {
                ImageViewHolder vHolder = (ImageViewHolder) holder;

                FetchDrawable req = new FetchDrawable(vHolder.imageView);
                vHolder.imageView.setTag(req);
                req.execute(position);

                vHolder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent in = new Intent();
                        Item i = mItems.get(position);
                        in.putExtra(SELECTED_RESOURCE_EXTRA,
                                mIconPackageName + "|" + i.title);
                        in.putExtra(SELECTED_BITMAP_EXTRA,
                                ImageHelper.drawableToBitmap(i.drawable.get()));
                        setResult(Activity.RESULT_OK, in);
                        finish();
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        public boolean isHeader(int position) {
            return mItems.get(position).isHeader;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public class FetchDrawable extends AsyncTask<Integer, Void, Drawable> {
            WeakReference<ImageView> mImageView;

            FetchDrawable(ImageView imgView) {
                mImageView = new WeakReference<>(imgView);
            }

            @Override
            protected Drawable doInBackground(Integer... position) {
                Item info = mItems.get(position[0]);
                int itemId = info.resource_id;
                Drawable d = mResources.getDrawable(itemId);
                info.drawable = new WeakReference<>(d);
                return d;
            }

            @Override
            public void onPostExecute(Drawable result) {
                if (mImageView.get() != null) {
                    mImageView.get().setImageDrawable(result);
                }
            }
        }
    }
}
