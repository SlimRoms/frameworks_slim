package com.slim.settings.search;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import com.slim.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.android.internal.R.styleable.Preference;
import static com.android.internal.R.styleable.Preference_fragment;
import static com.android.internal.R.styleable.Preference_icon;
import static com.android.internal.R.styleable.Preference_key;
import static com.android.internal.R.styleable.Preference_summary;
import static com.android.internal.R.styleable.Preference_title;
import static org.slim.framework.internal.R.styleable.SlimPreference;
import static org.slim.framework.internal.R.styleable.SlimPreference_preferenceXml;

public class SettingsList {

    private static final String SLIM_SETTINGS_TAG = "slim_settings";
    private static final String SETTING_TAG = "setting";

    public static final String SETTINGS_ACTION = "com.slim.settings.action";

    private Context mContext;
    private static SettingsList sInstance;

    private final Map<String, SettingInfo> mSettings = new ArrayMap<>();

    private SettingsList(Context context) {
        mContext = context;
        loadSettings();
    }

    public static SettingsList get(Context context) {
        if (sInstance == null) {
            sInstance = new SettingsList(context);
        }
        return sInstance;
    }

    public Set<String> getSettings() {
        synchronized(mSettings) {
            return mSettings.keySet();
        }
    }

    public SettingInfo getSetting(String key) {
        synchronized(mSettings) {
            return mSettings.get(key);
        }
    }

    public SettingInfo getSettingForClass(String clazz) {
        synchronized(mSettings) {
            for (SettingInfo info : mSettings.values()) {
                if (info.fragmentClass != null && info.fragmentClass.equals(clazz)) {
                    return info;
                }
            }
            return null;
        }
    }

    private void loadSettings() {
        loadSettingsFromResourceLocked(mContext.getResources(), R.xml.slim_settings, mSettings);
    }

    private void loadSettingsFromResourceLocked(Resources res, int resid,
            Map<String, SettingInfo> target) {

        XmlResourceParser parser = null;
        try {
            parser = res.getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }

            String name = parser.getName();
            if (!SLIM_SETTINGS_TAG.equals(name)) {
                // throw exception
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                name = parser.getName();
                if (SETTING_TAG.equals(name)) {
                    TypedArray sa = res.obtainAttributes(attrs, Preference);

                    String key = null;
                    TypedValue tv = sa.peekValue(Preference_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            key = res.getString(tv.resourceId);
                        } else {
                            key = String.valueOf(tv.string);
                        }
                    }
                    if (key == null) {
                        // throw exception
                    }

                    String title = null;
                    tv = sa.peekValue(Preference_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            title = res.getString(tv.resourceId);
                        } else {
                            title = String.valueOf(tv.string);
                        }
                    }

                    String summary = null;
                    tv = sa.peekValue(Preference_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            summary = res.getString(tv.resourceId);
                        } else {
                            summary = String.valueOf(tv.string);
                        }
                    }

                    String fragmentClass = sa.getString(Preference_fragment);
                    int iconResId = sa.getResourceId(Preference_icon, 0);

                   sa = res.obtainAttributes(attrs, SlimPreference);
                   int xmlResId = sa.getResourceId(SlimPreference_preferenceXml, 0);

                   sa.recycle();

                   SettingInfo info = new SettingInfo();
                   info.key = key;
                   info.title = title;
                   info.iconResId = iconResId;
                   info.summary = summary;
                   info.fragmentClass = fragmentClass;
                   info.xmlResId = xmlResId;

                   target.put(key, info);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    public static class SettingInfo {
        public String key;
        public String title;
        public int iconResId;
        public String summary;
        public String fragmentClass;
        public int xmlResId;

        public String getAction() {
            return SETTINGS_ACTION + "." + key;
        }
    }
}
