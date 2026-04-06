/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.util.Objects;
import java.util.function.Supplier;

public class CopyContext {
    private final CopySession session;
    private final Copier copier;
    private int level;

    CopyContext(CopySession session, Copier copier) {
        this.session = session;
        this.copier = copier;
    }

    CopySession getSession() {
        return session;
    }

    public boolean hasClone(Object src) {
        return session.hasClone(src);
    }

    public Object lookupClone(Object src) {
        return session.lookupClone(src);
    }

    public <T> T lookupClone(Object src, Class<T> type) {
        return session.lookupClone(src, type);
    }

    public <T> void withClone(T src, T clone) {
        session.addClone(src, clone);
    }

    public <T> void withCloneIfAbsent(T src, T dest) {
        session.addCloneIfAbsent(src, dest);
    }

    public <T> void withCloneIfAbsent(T src, Supplier<T> supplier) {
        session.addCloneIfAbsent(src, supplier);
    }

    public void withSelfCopy(Object src) {
        session.addClone(src, src);
    }

    public void exclude(Object src) {
        session.exclude(src);
    }

    public void include(Object src) {
        session.include(src);
    }

    public <T> T shallowCopy(T src) {
        return copier.copy(src, null, null, CopyMode.SHALLOW, this);
    }

    public <S extends T, D extends T, T> D shallowCopy(S src, D dest) {
        return shallowCopy(src, dest, null);
    }

    @SuppressWarnings("unchecked")
    public <S extends T, D extends T, T> D shallowCopy(S src, D dest, Class<T> template) {
        Objects.requireNonNull(dest, "The target object must not be null.");
        return (D) copier.copy(src, dest, template, CopyMode.SHALLOW, this);
    }

    public <T> T deepCopy(T src) {
        return copier.copy(src, null, null, CopyMode.DEEP, this);
    }

    public <S extends T, D extends T, T> D deepCopy(S src, D dest) {
        return deepCopy(src, dest, null);
    }

    @SuppressWarnings("unchecked")
    public <S extends T, D extends T, T> D deepCopy(S src, D dest, Class<T> template) {
        Objects.requireNonNull(dest, "The target object must not be null.");
        return (D) copier.copy(src, dest, template, CopyMode.DEEP, this);
    }

    @SuppressWarnings("unchecked")
    public <T> void shallowCopyFields(T src, T dest) {
        copier.shallowCopyFields(src, dest, (Class<T>) src.getClass());
    }

    <T> void shallowCopyFields(T src, T dest, Class<T> template) {
        copier.shallowCopyFields(src, dest, template);
    }

    @SuppressWarnings("unchecked")
    public <T> void deepCopyFields(T src, T dest) {
        copier.deepCopyFields(src, dest, (Class<T>) src.getClass(), this);
    }

    <T> void deepCopyFields(T src, T dest, Class<T> template) {
        copier.deepCopyFields(src, dest, template, this);
    }

    public boolean isRoot() {
        return level == 1;
    }

    void enterCopy() {
        level++;
    }

    void exitCopy() {
        level--;
    }
}
