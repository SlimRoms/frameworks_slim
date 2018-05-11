package org.slim.framework.internal.policy.keyparser;

import static slim.R.styleable.Key;
import static slim.R.styleable.Key_key;
import static slim.R.styleable.Key_name;
import static slim.R.styleable.Key_path;
import static slim.R.styleable.Key_scancode;
import static slim.R.styleable.Key_defaultAction;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedHashMap;


public class KeyParser {

    private static KeysArray sKeys;

    public static LinkedHashMap<String, KeyCategory> sKeyCategories;

    public static KeyCategory getCategory(Context context, String key) {
        if (sKeyCategories == null) {
            parseKeys(context);
        }
        return sKeyCategories.get(key);
    }

    public static LinkedHashMap<String, KeyCategory> parseKeys(Context context) {
        XmlResourceParser parser;

        if (sKeyCategories != null) {
            return sKeyCategories;
        }

        sKeyCategories = new LinkedHashMap<>();

        try {
            parser = context.getResources().getXml(
                    org.slim.framework.internal.R.xml.available_keys);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            String nodeName = parser.getName();
            if (!"keys".equals(nodeName)) {
                throw new RuntimeException("XML document must start with "
                        + " <touchscreen-gestures tag; found " + nodeName
                        + " at " + parser.getPositionDescription());
            }
            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("key-category".equals(nodeName)) {
                    TypedArray sa = context.getResources().obtainAttributes(attrs, Key);

                    KeyCategory category = new KeyCategory();
                    TypedValue tv = sa.peekValue(Key_name);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            category.name = context.getResources().getString(tv.resourceId);
                        } else {
                            category.name = String.valueOf(tv.string);
                        }
                    }
                    if (category.name == null) {
                        throw new RuntimeException("Attribute 'name' is required");
                    }

                    tv = sa.peekValue(Key_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            category.key = context.getResources().getString(tv.resourceId);
                        } else {
                            category.key = String.valueOf(tv.string);
                        }
                    }
                    if (category.key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    sa.recycle();
                    category.keys = getKeys(context, parser, attrs);

                    sKeyCategories.put(category.key, category);
                    }
            }
        } catch (IOException|XmlPullParserException e) {
            e.printStackTrace();
        }

        return sKeyCategories;
    }

    private static KeysArray getKeys(Context context, XmlPullParser parser, AttributeSet attrs) throws IOException, XmlPullParserException {
        KeysArray gesturesArray = new KeysArray();
        int type;
        String nodeName;
        final int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            nodeName = parser.getName();
            if ("key".equals(nodeName)) {
                TypedArray sa = context.getResources().obtainAttributes(attrs, Key);

                Key k = new Key();
                TypedValue tv = sa.peekValue(Key_name);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.name = context.getResources().getString(tv.resourceId);
                    } else {
                        k.name = String.valueOf(tv.string);
                    }
                }
                if (k.name == null) {
                    throw new RuntimeException("Attribute 'name' is required");
                }

                tv = sa.peekValue(Key_path);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.path = context.getResources().getString(tv.resourceId);
                    } else {
                        k.path = String.valueOf(tv.string);
                    }
                }

                tv = sa.peekValue(Key_defaultAction);
                if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                    if (tv.resourceId != 0) {
                        k.def = context.getResources().getString(tv.resourceId);
                    } else {
                        k.def = String.valueOf(tv.string);
                    }
                }

                tv = sa.peekValue(Key_scancode);
                if (tv != null && tv.type == TypedValue.TYPE_INT_DEC) {
                    if (tv.resourceId != 0) {
                        k.scancode = context.getResources().getInteger(tv.resourceId);
                    } else {
                        k.scancode = tv.data;
                    }
                }

                sa.recycle();

                gesturesArray.put(k.scancode, k);
            }
        }
        return gesturesArray;
    }

    public static String getPreferenceKey(int scanCode) {
        return "key_" + Integer.toString(scanCode);
    }
}
