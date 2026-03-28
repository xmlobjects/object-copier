/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CopySession implements AutoCloseable {
    static final Object NULL_CLONE = new Object();
    static final Object EXCLUDE_CLONE = new Object();

    private final Map<Object, Object> clones = new IdentityHashMap<>();
    private boolean closed = false;
    private boolean root = true;

    CopySession() {
    }

    public boolean hasClone(Object src) {
        return lookupClone(src) != null;
    }

    public Object lookupClone(Object src) {
        Object clone = clones.get(src);
        return clone == NULL_CLONE || clone == EXCLUDE_CLONE ? null : clone;
    }

    public <T> T lookupClone(Object src, Class<T> type) {
        Object clone = lookupClone(src);
        return type.isInstance(clone) ? type.cast(clone) : null;
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
        addCloneIfAbsent(src, EXCLUDE_CLONE);
    }

    void include(Object src) {
        if (src != null) {
            clones.computeIfPresent(src, (k, v) -> v == EXCLUDE_CLONE ? null : v);
        }
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
    }
}
