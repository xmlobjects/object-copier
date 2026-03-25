/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

public interface CopyCallback {
    default void preCopy(CopyContext context, CopyMode mode, boolean isRoot) {
    }

    default void postCopy(CopyContext context, CopyMode mode, boolean isRoot) {
    }
}

