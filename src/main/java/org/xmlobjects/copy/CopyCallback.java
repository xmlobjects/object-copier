/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

public interface CopyCallback {
    default void preCopy(CopyMode mode, CopyContext context) {
    }

    default void postCopy(CopyMode mode, CopyContext context) {
    }
}

