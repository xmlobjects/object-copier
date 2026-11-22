/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CopierTest {

    static class Base {
        String name;
    }

    static class Dog extends Base {
        int legs;
    }

    static class Cat extends Base {
        boolean indoor;
    }

    static class Person {
        String name;
        List<String> tags = new ArrayList<>();
        Person friend;
    }

    @Test
    void deepCopyCopiesNestedObjectsAndCycles() {
        Person a = new Person();
        a.name = "a";
        a.tags.add("x");
        a.friend = a;

        Person copy = CopierBuilder.newCopier().deepCopy(a);
        assertNotSame(a, copy);
        assertEquals("a", copy.name);
        assertNotSame(a.tags, copy.tags);
        assertEquals(List.of("x"), copy.tags);
        assertSame(copy, copy.friend);
    }

    @Test
    void shallowCopySharesReferences() {
        Person a = new Person();
        a.tags.add("x");

        Person copy = CopierBuilder.newCopier().shallowCopy(a);
        assertNotSame(a, copy);
        assertSame(a.tags, copy.tags);
    }

}
