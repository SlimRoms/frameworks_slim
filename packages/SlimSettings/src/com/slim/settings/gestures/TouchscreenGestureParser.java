package com.slim.settings.gestures;

import static org.slim.framework.internal.R.styleable.TouchscreenGesture;
import static org.slim.framework.internal.R.styleable.TouchscreenGesture_name;
import static org.slim.framework.internal.R.styleable.TouchscreenGesture_path;
import static org.slim.framework.internal.R.styleable.TouchscreenGesture_scancode;
import static org.slim.framework.internal.R.styleable.TouchscreenGesture_defaultAction;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.slim.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class TouchscreenGestureParser {

    private static GesturesArray sGestures;

    public static class GesturesArray extends LinkedHashMap<Integer, Gesture>
            implements Iterable<Gesture> {
        @Override
        public Iterator<Gesture> iterator() {
            return new Iterator<Gesture>() {
                private int mCurrent = 0;
                @Override
                public boolean hasNext() {
                    return mCurrent != size();
                }

                @Override
                public Gesture next() {
                    return get(keySet().toArray(new Integer[0])[mCurrent++]);
                }
            };
        }
    }
    public static class Gesture {
        public String path;
        public String name;
        public String def;
        public int scancode;
    }

    public static GesturesArray parseGestures(Context context) {
        XmlResourceParser parser;

        if (sGestures != null) {
            return sGestures;
        }

        GesturesArray gestures = new GesturesArray();

        try {
            parser = context.getResources().getXml(
                    org.slim.framework.internal.R.xml.touchscreen_gestures);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }
            String nodeName = parser.getName();
            if (!"touchscreen-gestures".equals(nodeName)) {
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
                if ("gesture".equals(nodeName)) {
                    TypedArray sa = context.getResources().obtainAttributes(attrs, TouchscreenGesture);

                    Gesture g = new Gesture();
                    TypedValue tv = sa.peekValue(TouchscreenGesture_name);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            g.name = context.getResources().getString(tv.resourceId);
                        } else {
                            g.name = String.valueOf(tv.string);
                        }
                    }
                    if (g.name == null) {
                        throw new RuntimeException("Attribute 'name' is required");
                    }

                    tv = sa.peekValue(TouchscreenGesture_path);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            g.path = context.getResources().getString(tv.resourceId);
                        } else {
                            g.path = String.valueOf(tv.string);
                        }
                    }

                    tv = sa.peekValue(TouchscreenGesture_defaultAction);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            g.def = context.getResources().getString(tv.resourceId);
                        } else {
                            g.def = String.valueOf(tv.string);
                        }
                    }

                    tv = sa.peekValue(TouchscreenGesture_scancode);
                    if (tv != null && tv.type == TypedValue.TYPE_INT_DEC) {
                        if (tv.resourceId != 0) {
                            g.scancode = context.getResources().getInteger(tv.resourceId);
                        } else {
                            g.scancode = tv.data;
                        }
                    }

                    sa.recycle();

                    gestures.put(g.scancode, g);
                }
            }
        } catch (IOException|XmlPullParserException e) {
            e.printStackTrace();
        }

        return gestures;
    }
}
