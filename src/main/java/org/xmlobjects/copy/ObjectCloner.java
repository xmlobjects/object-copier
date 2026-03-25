/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.Objects;

public class ObjectCloner<T> extends TypeCloner<T> {
    private final Class<T> type;

    protected ObjectCloner(Class<T> type) {
        this.type = Objects.requireNonNull(type, "The clone type must not be null.");
    }

    @Override
    protected void shallowCopy(T src, T dest, CopyContext context) {
        context.shallowCopyFields(src, dest, type);
    }

    @Override
    protected void deepCopy(T src, T dest, CopyContext context) {
        context.deepCopyFields(src, dest, type);
    }
}
