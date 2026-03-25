/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

public class CopyableCloner<T extends Copyable<T>> extends TypeCloner<T> {

    CopyableCloner() {
    }

    @Override
    protected T newInstance(T src, CopyMode mode, CopyContext context) {
        return src.newInstance(mode, context);
    }

    @Override
    protected void shallowCopy(T src, T dest, CopyContext context) {
        src.shallowCopyTo(dest, context);
    }

    @Override
    protected void deepCopy(T src, T dest, CopyContext context) {
        src.deepCopyTo(dest, context);
    }
}
