/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.lang.reflect.Array;

public class ArrayCloner extends TypeCloner<Object> {

    protected ArrayCloner() {
    }

    @Override
    public Object newInstance(Object src, CopyMode mode, CopyContext context) {
        return Array.newInstance(src.getClass().getComponentType(), Array.getLength(src));
    }

    @Override
    protected void shallowCopy(Object src, Object dest, CopyContext context) {
        checkLength(src, dest);
        System.arraycopy(src, 0, dest, 0, Array.getLength(src));
    }

    @Override
    protected void deepCopy(Object src, Object dest, CopyContext context) {
        checkLength(src, dest);
        if (src instanceof Object[] srcArray && dest instanceof Object[] destArray) {
            for (int i = 0; i < srcArray.length; i++) {
                destArray[i] = context.deepCopy(srcArray[i]);
            }
        } else {
            System.arraycopy(src, 0, dest, 0, Array.getLength(src));
        }
    }

    private void checkLength(Object src, Object dest) {
        int srcLength = Array.getLength(src);
        int destLength = Array.getLength(dest);
        if (destLength < srcLength) {
            throw new CopyException("Destination array length " + destLength +
                    " is less than source array length " + srcLength + ".");
        }
    }
}
