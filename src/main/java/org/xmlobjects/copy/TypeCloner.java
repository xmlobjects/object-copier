/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

public abstract class TypeCloner<T> {

    protected TypeCloner() {
    }

    protected T newInstance(T src, CopyMode mode, CopyContext context) {
        return CopyHelper.newInstance(src, mode, context);
    }

    protected void shallowCopy(T src, T dest, CopyContext context) {
    }

    protected void deepCopy(T src, T dest, CopyContext context) {
    }
}
