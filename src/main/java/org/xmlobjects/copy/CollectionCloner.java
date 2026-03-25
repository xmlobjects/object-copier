/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@SuppressWarnings("rawtypes")
public class CollectionCloner<T extends Collection> extends TypeCloner<T> {

    protected CollectionCloner() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public T newInstance(T src, CopyMode mode, CopyContext context) {
        if (src instanceof SortedSet<?> set) {
            return (T) new TreeSet<>(set.comparator());
        } else if (src instanceof PriorityQueue<?> queue) {
            return (T) new PriorityQueue<>(Math.max(1, queue.size()), queue.comparator());
        } else if (src instanceof PriorityBlockingQueue<?> queue) {
            return (T) new PriorityBlockingQueue<>(Math.max(1, queue.size()), queue.comparator());
        } else if (src instanceof EnumSet set) {
            return (T) EnumSet.complementOf(EnumSet.complementOf(set));
        }

        Exception cause;
        try {
            return super.newInstance(src, mode, context);
        } catch (Exception e) {
            cause = e;
        }

        if (src instanceof List) {
            return (T) new ArrayList<>(src.size());
        } else if (src instanceof Set) {
            return (T) new HashSet<>(src.size());
        } else if (src instanceof Deque) {
            return (T) new ArrayDeque<>(src.size());
        } else {
            throw new CopyException("Failed to create an instance of " + src.getClass() + ".", cause);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void shallowCopy(T src, T dest, CopyContext context) {
        dest.addAll(src);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void deepCopy(T src, T dest, CopyContext context) {
        for (Object value : src) {
            dest.add(context.deepCopy(value));
        }
    }
}
