/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import org.junit.jupiter.api.Test;

import java.util.*;

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

    @Test
    void copyIntoSiblingCopiesCommonFields() {
        Dog dog = new Dog();
        dog.name = "rex";
        dog.legs = 4;
        Cat cat = new Cat();

        Copier copier = CopierBuilder.newCopier();
        assertSame(cat, copier.shallowCopy(dog, cat));
        assertEquals("rex", cat.name);

        Cat other = new Cat();
        assertSame(other, copier.deepCopy(dog, other));
        assertEquals("rex", other.name);
    }

    @Test
    void copyIntoSuperclassInstanceCopiesCommonFields() {
        Dog dog = new Dog();
        dog.name = "rex";
        Base base = new Base();

        assertSame(base, CopierBuilder.newCopier().shallowCopy(dog, base));
        assertEquals("rex", base.name);
    }

    static class NamedList extends ArrayList<String> {
        String name;
    }

    static class SpecialList extends NamedList {
    }

    @Test
    void superclassClonerIsUsedForCollectionSubclasses() {
        Copier copier = CopierBuilder.newInstance()
                .withCloner(NamedList.class, new TypeCloner<>() {
                    @Override
                    protected void deepCopy(NamedList src, NamedList dest, CopyContext context) {
                        dest.name = src.name;
                        dest.addAll(src);
                    }
                })
                .build();

        SpecialList list = new SpecialList();
        list.name = "special";
        list.add("x");

        SpecialList copy = copier.deepCopy(list);
        assertEquals("special", copy.name);
        assertEquals(List.of("x"), copy);
    }

    @Test
    void copyIntoTargetFillsTargetAlthoughSourceWasAlreadyCopied() {
        Copier copier = CopierBuilder.newCopier();
        Person a = new Person();
        a.name = "a";

        try (CopySession session = copier.createSession()) {
            Person first = copier.deepCopy(a, session);
            Person dest = new Person();

            assertSame(dest, copier.deepCopy(a, dest, session));
            assertEquals("a", dest.name);
            assertSame(first, session.lookupClone(a));

            Person other = new Person();
            assertSame(other, copier.shallowCopy(a, other, session));
            assertEquals("a", other.name);
        }
    }

    @Test
    void excludedSourceIsNotCopiedIntoTarget() {
        Copier copier = CopierBuilder.newCopier();
        Person a = new Person();
        a.name = "a";

        try (CopySession session = copier.createSession()) {
            CopyContext context = copier.createContext(session);
            context.exclude(a);
            Person dest = new Person();

            assertSame(a, copier.deepCopy(a, dest, session));
            assertNull(dest.name);
        }
    }

    static class Failing extends Person implements CopyCallback {
        static boolean fail;

        @Override
        public void postCopy(CopyMode mode, CopyContext context) {
            if (fail) {
                throw new IllegalStateException("failed");
            }
        }
    }

    @Test
    void failedCopyIsNotKeptInSession() {
        Copier copier = CopierBuilder.newCopier();
        Failing a = new Failing();
        a.name = "a";

        try (CopySession session = copier.createSession()) {
            Failing.fail = true;
            assertThrows(CopyException.class, () -> copier.deepCopy(a, session));
            assertFalse(session.hasClone(a));

            Failing.fail = false;
            Failing copy = copier.deepCopy(a, session);
            assertEquals("a", copy.name);
        } finally {
            Failing.fail = false;
        }
    }

    @Test
    void copyKeepsOrderOfSequencedSetsWithoutNoArgConstructor() {
        SequencedSet<String> set = Collections.unmodifiableSequencedSet(
                new LinkedHashSet<>(List.of("c", "a", "b", "e", "d", "f", "h", "g")));

        Set<String> copy = CopierBuilder.newCopier().deepCopy(set);
        assertEquals(List.copyOf(set), List.copyOf(copy));
    }
}
