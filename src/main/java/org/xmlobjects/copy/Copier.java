/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Copier {
    private static final TypeCloner<?> IDENTITY_CLONER = new TypeCloner<>() {
    };
    private static final TypeCloner<?> NULL_CLONER = new TypeCloner<>() {
    };

    private final Map<Class<?>, TypeCloner<?>> registeredCloners;
    private final Map<Class<?>, TypeCloner<?>> cloners = new ConcurrentHashMap<>();
    private final TypeCloner<?> copyableCloner = new CopyableCloner<>();
    private final TypeCloner<?> collectionCloner = new CollectionCloner<>();
    private final TypeCloner<?> mapCloner = new MapCloner<>();
    private final TypeCloner<?> arrayCloner = new ArrayCloner();
    private final TypeCloner<?> optionalCloner = new OptionalCloner();

    Copier(Map<Class<?>, TypeCloner<?>> registeredCloners, Set<Class<?>> selfCopyTypes, Set<Class<?>> nullCopyTypes) {
        this.registeredCloners = Map.copyOf(registeredCloners);

        initializeKnownCloners();
        cloners.putAll(registeredCloners);
        selfCopyTypes.forEach(type -> cloners.put(type, IDENTITY_CLONER));
        nullCopyTypes.forEach(type -> cloners.put(type, NULL_CLONER));
    }

    public <S> S shallowCopy(S src) {
        return shallowCopy(src, createSession());
    }

    public <S> S shallowCopy(S src, CopySession session) {
        return copy(src, null, null, CopyMode.SHALLOW, createContext(session));
    }

    public <S extends T, D extends T, T> D shallowCopy(S src, D dest) {
        return shallowCopy(src, dest, null, createSession());
    }

    public <S extends T, D extends T, T> D shallowCopy(S src, D dest, CopySession session) {
        return shallowCopy(src, dest, null, session != null ? session : createSession());
    }

    public <S extends T, D extends T, T> D shallowCopy(S src, D dest, Class<T> template) {
        return shallowCopy(src, dest, template, createSession());
    }

    @SuppressWarnings("unchecked")
    public <S extends T, D extends T, T> D shallowCopy(S src, D dest, Class<T> template, CopySession session) {
        Objects.requireNonNull(dest, "The target object must not be null.");
        return (D) copy(src, dest, template, CopyMode.SHALLOW, createContext(session));
    }

    public <S> S deepCopy(S src) {
        return deepCopy(src, createSession());
    }

    public <S> S deepCopy(S src, CopySession session) {
        return copy(src, null, null, CopyMode.DEEP, createContext(session));
    }

    public <S extends T, D extends T, T> D deepCopy(S src, D dest) {
        return deepCopy(src, dest, null, createSession());
    }

    public <S extends T, D extends T, T> D deepCopy(S src, D dest, CopySession session) {
        return deepCopy(src, dest, null, session != null ? session : createSession());
    }

    public <S extends T, D extends T, T> D deepCopy(S src, D dest, Class<T> template) {
        return deepCopy(src, dest, template, createSession());
    }

    @SuppressWarnings("unchecked")
    public <S extends T, D extends T, T> D deepCopy(S src, D dest, Class<T> template, CopySession session) {
        Objects.requireNonNull(dest, "The target object must not be null.");
        return (D) copy(src, dest, template, CopyMode.DEEP, createContext(session));
    }

    public CopySession createSession() {
        return new CopySession();
    }

    CopyContext createContext(CopySession session) {
        return new CopyContext(session != null ? session : createSession(), this);
    }

    @SuppressWarnings("unchecked")
    <T> T copy(T src, T dest, Class<T> template, CopyMode mode, CopyContext context) {
        if (src == null || src == dest || template == Object.class) {
            return dest;
        }

        CopySession session = context.getSession();
        if (session.isClosed()) {
            throw new CopyException("The copy session has already been closed.");
        }

        if (template == null || src.getClass().isArray()) {
            template = (Class<T>) src.getClass();
        }

        TypeCloner<T> cloner = (TypeCloner<T>) findCloner(template);
        if (cloner == IDENTITY_CLONER) {
            return src;
        } else if (cloner == NULL_CLONER) {
            return null;
        }

        boolean isRoot = session.getAndSetRoot(false);

        T clone = (T) session.getClone(src);
        try {
            if (clone == null) {
                if (src instanceof CopyCallback callback) {
                    callback.preCopy(context, mode, isRoot);
                }

                clone = dest == null ? cloner.newInstance(src, mode, context) : dest;
                session.addClone(src, clone);

                if (clone != null) {
                    if (mode == CopyMode.SHALLOW) {
                        cloner.shallowCopy(src, clone, context);
                    } else {
                        cloner.deepCopy(src, clone, context);
                    }
                }

                if (clone instanceof CopyCallback callback) {
                    callback.postCopy(context, mode, isRoot);
                }
            } else if (session.isNullClone(clone)) {
                clone = null;
            }

            return clone;
        } catch (Exception e) {
            throw e instanceof CopyException copyException ?
                    copyException :
                    new CopyException("Failed to copy " + src + ".", e);
        } finally {
            if (isRoot) {
                session.getAndSetRoot(true);
            }
        }
    }

    <T> void shallowCopyFields(T src, T dest, Class<T> template) {
        if (src != null && dest != null) {
            try {
                for (Field field : CopyHelper.fields(template)) {
                    field.set(dest, field.get(src));
                }
            } catch (Exception e) {
                throw new CopyException("Failed to shallow copy fields of " + src.getClass().getName() + ".", e);
            }
        }
    }

    <T> void deepCopyFields(T src, T dest, Class<T> template, CopyContext context) {
        if (src != null && dest != null) {
            try {
                for (Field field : CopyHelper.fields(template)) {
                    field.set(dest, copy(field.get(src), null, null, CopyMode.DEEP, context));
                }
            } catch (Exception e) {
                throw new CopyException("Failed to deep copy fields of " + src.getClass().getName() + ".", e);
            }
        }
    }

    private TypeCloner<?> findCloner(Class<?> type) {
        return cloners.computeIfAbsent(type, this::createCloner);
    }

    private TypeCloner<?> createCloner(Class<?> type) {
        if (Copyable.class.isAssignableFrom(type)) {
            return copyableCloner;
        } else if (Enum.class.isAssignableFrom(type)) {
            return IDENTITY_CLONER;
        } else if (type.isRecord()) {
            return IDENTITY_CLONER;
        } else if (Collection.class.isAssignableFrom(type)) {
            return collectionCloner;
        } else if (Map.class.isAssignableFrom(type)) {
            return mapCloner;
        } else if (type.isArray()) {
            return arrayCloner;
        } else if (!registeredCloners.isEmpty()) {
            TypeCloner<?> cloner = findSuperclassCloner(type);
            if (cloner != null) {
                return cloner;
            }
        }

        return new ObjectCloner<>(type);
    }

    private TypeCloner<?> findSuperclassCloner(Class<?> type) {
        for (Class<?> current = type.getSuperclass(); current != null; current = current.getSuperclass()) {
            TypeCloner<?> cloner = registeredCloners.get(current);
            if (cloner != null) {
                return cloner;
            }
        }

        return null;
    }

    private void initializeKnownCloners() {
        cloners.put(String.class, IDENTITY_CLONER);
        cloners.put(Integer.class, IDENTITY_CLONER);
        cloners.put(Long.class, IDENTITY_CLONER);
        cloners.put(Boolean.class, IDENTITY_CLONER);
        cloners.put(Class.class, IDENTITY_CLONER);
        cloners.put(Float.class, IDENTITY_CLONER);
        cloners.put(Double.class, IDENTITY_CLONER);
        cloners.put(Character.class, IDENTITY_CLONER);
        cloners.put(Byte.class, IDENTITY_CLONER);
        cloners.put(Short.class, IDENTITY_CLONER);
        cloners.put(Void.class, IDENTITY_CLONER);
        cloners.put(BigDecimal.class, IDENTITY_CLONER);
        cloners.put(BigInteger.class, IDENTITY_CLONER);
        cloners.put(URI.class, IDENTITY_CLONER);
        cloners.put(URL.class, IDENTITY_CLONER);
        cloners.put(UUID.class, IDENTITY_CLONER);
        cloners.put(Pattern.class, IDENTITY_CLONER);
        cloners.put(Clock.class, IDENTITY_CLONER);
        cloners.put(Duration.class, IDENTITY_CLONER);
        cloners.put(Instant.class, IDENTITY_CLONER);
        cloners.put(LocalDate.class, IDENTITY_CLONER);
        cloners.put(LocalDateTime.class, IDENTITY_CLONER);
        cloners.put(LocalTime.class, IDENTITY_CLONER);
        cloners.put(MonthDay.class, IDENTITY_CLONER);
        cloners.put(OffsetDateTime.class, IDENTITY_CLONER);
        cloners.put(OffsetTime.class, IDENTITY_CLONER);
        cloners.put(Period.class, IDENTITY_CLONER);
        cloners.put(Year.class, IDENTITY_CLONER);
        cloners.put(YearMonth.class, IDENTITY_CLONER);
        cloners.put(ZonedDateTime.class, IDENTITY_CLONER);
        cloners.put(Collections.EMPTY_LIST.getClass(), IDENTITY_CLONER);
        cloners.put(Collections.EMPTY_MAP.getClass(), IDENTITY_CLONER);
        cloners.put(Collections.EMPTY_SET.getClass(), IDENTITY_CLONER);
        cloners.put(OptionalInt.class, IDENTITY_CLONER);
        cloners.put(OptionalLong.class, IDENTITY_CLONER);
        cloners.put(OptionalDouble.class, IDENTITY_CLONER);
        cloners.put(Optional.class, optionalCloner);
        cloners.put(ZoneId.class, IDENTITY_CLONER);
        cloners.put(ZoneOffset.class, IDENTITY_CLONER);
        cloners.put(Locale.class, IDENTITY_CLONER);
        cloners.put(Currency.class, IDENTITY_CLONER);
        cloners.put(Charset.class, IDENTITY_CLONER);
    }
}
