/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.*;

@SuppressWarnings("rawtypes")
public class MapCloner<T extends Map> extends TypeCloner<T> {

    protected MapCloner() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public T newInstance(T src, CopyMode mode, CopyContext context) {
        if (src instanceof SortedMap map) {
            return (T) new TreeMap<>(map.comparator());
        } else if (src instanceof EnumMap<?, ?> map) {
            EnumMap<?, ?> result = new EnumMap<>(map);
            result.clear();
            return (T) result;
        }

        try {
            return super.newInstance(src, mode, context);
        } catch (Exception e) {
            return (T) new LinkedHashMap<>(Math.max(1, src.size()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void shallowCopy(T src, T dest, CopyContext context) {
        dest.putAll(src);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deepCopy(T src, T dest, CopyContext context) {
        for (Object object : src.entrySet()) {
            Map.Entry entry = (Map.Entry) object;
            Object key = entry.getKey();
            Object value = entry.getValue();
            dest.put(context.deepCopy(key), context.deepCopy(value));
        }
    }
}
