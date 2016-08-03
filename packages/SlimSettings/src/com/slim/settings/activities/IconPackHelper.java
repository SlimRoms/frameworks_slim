package com.slim.settings.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.slim.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class IconPackHelper {

    public static final int REQUEST_PICK_ICON = 13;

    public static final int NUM_PALETTE_COLORS = 32;
    public final static String[] sSupportedActions = new String[]{
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME"
    };
    public static final String[] sSupportedCategories = new String[]{
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME"
    };

    public static Map<String, IconPackInfo> getSupportedPackages(Context context) {
        Intent i = new Intent();
        Map<String, IconPackInfo> packages = new HashMap<>();
        PackageManager packageManager = context.getPackageManager();
        for (String action : sSupportedActions) {
            i.setAction(action);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
        }
        i = new Intent(Intent.ACTION_MAIN);
        for (String category : sSupportedCategories) {
            i.addCategory(category);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                IconPackInfo info = new IconPackInfo(r, packageManager);
                packages.put(r.activityInfo.packageName, info);
            }
            i.removeCategory(category);
        }
        return packages;
    }

    private static void loadApplicationResources(Context context,
                                                 Map<ComponentName, String> iconPackResources, String packageName) {
        Field[] drawableItems;
        try {
            Context appContext = context.createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            drawableItems = Class.forName(packageName + ".R$drawable",
                    true, appContext.getClassLoader()).getFields();
        } catch (Exception e) {
            return;
        }

        ComponentName compName;
        for (Field f : drawableItems) {
            String name = f.getName();

            String icon = name.toLowerCase();
            name = name.replaceAll("_", ".");

            compName = new ComponentName(name, "");
            iconPackResources.put(compName, icon);

            int activityIndex = name.lastIndexOf(".");
            if (activityIndex <= 0 || activityIndex == name.length() - 1) {
                continue;
            }

            String iconPackage = name.substring(0, activityIndex);
            if (TextUtils.isEmpty(iconPackage)) {
                continue;
            }

            String iconActivity = name.substring(activityIndex + 1);
            if (TextUtils.isEmpty(iconActivity)) {
                continue;
            }

            // Store entries as lower case to ensure match
            iconPackage = iconPackage.toLowerCase();
            iconActivity = iconActivity.toLowerCase();

            iconActivity = iconPackage + "." + iconActivity;
            compName = new ComponentName(iconPackage, iconActivity);
            iconPackResources.put(compName, icon);
        }
    }

    public static ArrayList<IconPickerActivity.Item> getCustomIconPackResources(
            Context context, String packageName) {
        Resources res;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        XmlResourceParser parser = null;
        ArrayList<IconPickerActivity.Item> iconPackResources = new ArrayList<>();

        try {
            parser = res.getAssets().openXmlResourceParser("drawable.xml");
        } catch (IOException e) {
            int resId = res.getIdentifier("drawable", "xml", packageName);
            if (resId != 0) {
                parser = res.getXml(resId);
            }
        }

        if (parser != null) {
            try {
                loadCustomResourcesFromXmlParser(parser, iconPackResources);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            } finally {
                parser.close();
            }
        }
        return iconPackResources;
    }

    private static void loadCustomResourcesFromXmlParser(
            XmlPullParser parser, ArrayList<IconPickerActivity.Item> iconPackResources)
            throws XmlPullParserException, IOException {

        int eventType = parser.getEventType();
        do {
            if (eventType != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equalsIgnoreCase("item")) {
                String drawable = parser.getAttributeValue(null, "drawable");
                if (TextUtils.isEmpty(drawable) || drawable.length() == 0) {
                    continue;
                }
                IconPickerActivity.Item item = new IconPickerActivity.Item();
                item.isIcon = true;
                item.title = drawable;
                iconPackResources.add(item);
            } else if (parser.getName().equalsIgnoreCase("category")) {
                String title = parser.getAttributeValue(null, "title");
                if (TextUtils.isEmpty(title) || title.length() == 0) {
                    continue;
                }
                IconPickerActivity.Item item = new IconPickerActivity.Item();
                item.isHeader = true;
                item.title = title;
                iconPackResources.add(item);
            }
        } while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT);

    }

    public static boolean pickIconPack(final Activity activity) {
        final Map<String, IconPackInfo> supportedPackages = getSupportedPackages(activity);
        if (supportedPackages.isEmpty()) {
            Toast.makeText(activity, R.string.no_iconpacks_summary, Toast.LENGTH_SHORT).show();
            return false;
        }

        final IconAdapter adapter;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity,
                android.R.style.Theme_Material_Dialog_MinWidth));
        builder.setTitle(R.string.dialog_pick_iconpack_title);
        adapter = new IconAdapter(activity, supportedPackages, true);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String selectedPackage = adapter.getItem(which);
                Intent i = new Intent();
                i.setClass(activity, IconPickerActivity.class);
                i.putExtra("package", selectedPackage);
                activity.startActivityForResult(i, REQUEST_PICK_ICON);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
        return true;
    }

    static class IconPackInfo {
        String packageName;
        CharSequence label;
        Drawable icon;

        IconPackInfo(ResolveInfo r, PackageManager packageManager) {
            packageName = r.activityInfo.packageName;
            icon = r.loadIcon(packageManager);
            label = r.loadLabel(packageManager);
        }

        public IconPackInfo(String label, Drawable icon, String packageName) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
        }
    }

    private static class IconAdapter extends BaseAdapter {
        ArrayList<IconPackInfo> mSupportedPackages;
        Context mContext;
        String mCurrentIconPack;
        int mCurrentIconPackPosition = -1;
        boolean mPickIcon = false;

        IconAdapter(Context ctx, Map<String, IconPackInfo> supportedPackages) {
            this(ctx, supportedPackages, false);
        }

        IconAdapter(Context ctx, Map<String, IconPackInfo> supportedPackages, boolean pickIcon) {
            mContext = ctx;
            mSupportedPackages = new ArrayList<>(supportedPackages.values());
            Collections.sort(mSupportedPackages, new Comparator<IconPackInfo>() {
                @Override
                public int compare(IconPackInfo lhs, IconPackInfo rhs) {
                    return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
                }
            });

            Resources res = ctx.getResources();

            mPickIcon = pickIcon;
        }

        @Override
        public int getCount() {
            return mSupportedPackages.size();
        }

        @Override
        public String getItem(int position) {
            return mSupportedPackages.get(position).packageName;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public boolean isCurrentIconPack(int position) {
            return mCurrentIconPackPosition == position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.iconpack_chooser, null);
            }
            IconPackInfo info = mSupportedPackages.get(position);
            TextView txtView = (TextView) convertView.findViewById(R.id.title);
            txtView.setText(info.label);
            ImageView imgView = (ImageView) convertView.findViewById(R.id.icon);
            imgView.setImageDrawable(info.icon);
            RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            if (mPickIcon) {
                radioButton.setVisibility(View.GONE);
            } else {
                boolean isCurrentIconPack = info.packageName.equals(mCurrentIconPack);
                radioButton.setChecked(isCurrentIconPack);
                if (isCurrentIconPack) {
                    mCurrentIconPackPosition = position;
                }
            }
            return convertView;
        }
    }
}
