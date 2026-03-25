/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.*;

public class CopierBuilder {
    private final Map<Class<?>, TypeCloner<?>> cloners = new HashMap<>();
    private final Set<Class<?>> selfCopyTypes = new HashSet<>();
    private final Set<Class<?>> nullCopyTypes = new HashSet<>();

    private CopierBuilder() {
    }

    public static Copier newCopier() {
        return new CopierBuilder().build();
    }

    public static CopierBuilder newInstance() {
        return new CopierBuilder();
    }

    public <T> CopierBuilder withCloner(Class<T> type, TypeCloner<T> cloner) {
        Objects.requireNonNull(type, "The type must not be null.");
        if (type.isInterface()) {
            throw new IllegalArgumentException("Cloners cannot be registered for interfaces.");
        }

        cloners.put(type, cloner);
        return this;
    }

    public CopierBuilder withSelfCopy(Class<?>... types) {
        Objects.requireNonNull(types, "The types must not be null.");
        for (Class<?> type : types) {
            if (type.isInterface()) {
                throw new IllegalArgumentException("Self copies cannot be registered for interfaces.");
            }

            selfCopyTypes.add(type);
        }

        return this;
    }

    public CopierBuilder withNullCopy(Class<?>... types) {
        Objects.requireNonNull(types, "The types must not be null.");
        for (Class<?> type : types) {
            if (type.isInterface()) {
                throw new IllegalArgumentException("Null copies cannot be registered for interfaces.");
            }

            nullCopyTypes.add(type);
        }

        return this;
    }

    public Copier build() {
        return new Copier(cloners, selfCopyTypes, nullCopyTypes);
    }
}
