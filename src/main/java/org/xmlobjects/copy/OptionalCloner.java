/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.Optional;

public class OptionalCloner extends TypeCloner<Optional<?>> {

    OptionalCloner() {
    }

    @Override
    protected Optional<?> newInstance(Optional<?> src, CopyMode mode, CopyContext context) {
        return mode == CopyMode.SHALLOW ? src : src.map(context::deepCopy);
    }
}
