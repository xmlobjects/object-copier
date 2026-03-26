/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CopySession implements AutoCloseable {
    private static final Object NULL_CLONE = new Object();
    private final Map<Object, Object> clones = new IdentityHashMap<>();
    private final Set<Object> excludes = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closed = false;
    private boolean root = true;

    CopySession() {
    }

    public <T> T lookupClone(Object src, Class<T> type) {
        Object clone = clones.get(src);
        return !isNullClone(clone) && type.isInstance(clone) ?
                type.cast(clone) :
                null;
    }

    Object getClone(Object src) {
        return clones.get(src);
    }

    void addClone(Object src, Object dest) {
        if (src != null) {
            clones.put(src, dest != null ? dest : NULL_CLONE);
        }
    }

    void addCloneIfAbsent(Object src, Object dest) {
        if (src != null) {
            clones.putIfAbsent(src, dest != null ? dest : NULL_CLONE);
        }
    }

    <T> void addCloneIfAbsent(T src, Supplier<T> supplier) {
        if (src != null && supplier != null) {
            clones.computeIfAbsent(src, k -> {
                T dest = supplier.get();
                return dest != null ? dest : NULL_CLONE;
            });
        }
    }

    void exclude(Object src) {
        if (src != null) {
            excludes.add(src);
        }
    }

    void include(Object src) {
        if (src != null) {
            excludes.remove(src);
        }
    }

    boolean isExcluded(Object src) {
        return excludes.contains(src);
    }

    boolean isNullClone(Object clone) {
        return clone == null || clone == NULL_CLONE;
    }

    boolean getAndSetRoot(boolean root) {
        boolean result = this.root;
        this.root = root;
        return result;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
        root = true;
        clones.clear();
        excludes.clear();
    }
}
