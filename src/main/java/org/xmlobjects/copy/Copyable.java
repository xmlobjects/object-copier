/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

public interface Copyable<T extends Copyable<T>> {

    @SuppressWarnings("unchecked")
    default T newInstance(CopyMode mode, CopyContext context) {
        return CopyHelper.newInstance((T) this, mode, context);
    }

    default void shallowCopyTo(T dest, CopyContext context) {
        context.shallowCopyFields(this, dest);
    }

    default void deepCopyTo(T dest, CopyContext context) {
        context.deepCopyFields(this, dest);
    }
}
