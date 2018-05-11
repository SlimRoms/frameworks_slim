package org.slim.framework.internal.policy.keyparser;

import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class KeysArray extends LinkedHashMap<Integer, Key>
        implements Iterable<Key> {
    @NonNull
    @Override
    public Iterator<Key> iterator() {
        return new Iterator<Key>() {
            private int mCurrent = 0;
            @Override
            public boolean hasNext() {
                return mCurrent != size();
            }

            @Override
            public Key next() {
                return get(keySet().toArray(new Integer[0])[mCurrent++]);
            }
        };
    }
}
