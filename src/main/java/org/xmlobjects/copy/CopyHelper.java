/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Claus Nagel <claus.nagel@gmail.com>
 */

package org.xmlobjects.copy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class CopyHelper {
    private static final ClassValue<ConstructorInfo> CONSTRUCTORS = new ClassValue<>() {
        @Override
        protected ConstructorInfo computeValue(Class<?> type) {
            return new ConstructorInfo(findConstructor(type), findCopyCreator(type));
        }
    };

    private static final ClassValue<List<Field>> FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            return findFields(type);
        }
    };

    private record ConstructorInfo(Constructor<?> constructor, Method copyCreator) {
    }

    private CopyHelper() {
    }

    @SuppressWarnings("unchecked")
    static <T> T newInstance(T src, CopyMode mode, CopyContext context) {
        ConstructorInfo info = CONSTRUCTORS.get(src.getClass());
        if (info.constructor() == null && info.copyCreator() == null) {
            throw new CopyException("No @CopyCreator method and no no-arg constructor found on " +
                    src.getClass().getName() + ".");
        }

        try {
            if (info.copyCreator() != null) {
                Object instance = info.copyCreator().invoke(src, mode, context);
                if (instance == null) {
                    throw new CopyException("@CopyCreator returned null for " + src.getClass().getName() + ".");
                }

                return (T) instance;
            }

            return (T) info.constructor().newInstance();
        } catch (Throwable e) {
            throw new CopyException("Failed to create instance of " + src.getClass().getName() + ".", e);
        }
    }

    static List<Field> fields(Class<?> type) {
        return FIELDS.get(type);
    }

    private static Constructor<?> findConstructor(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new CopyException("Cannot access no-arg constructor of " + type.getName() + ".", e);
        }
    }

    private static Method findCopyCreator(Class<?> type) {
        Method method = null;
        for (Method candidate : type.getDeclaredMethods()) {
            if (candidate.isSynthetic()
                    || Modifier.isStatic(candidate.getModifiers())
                    || !candidate.isAnnotationPresent(CopyCreator.class)) {
                continue;
            }

            if (!candidate.getName().equals("newInstance")
                    || !type.isAssignableFrom(candidate.getReturnType())
                    || candidate.getParameterCount() != 2
                    || candidate.getParameterTypes()[0] != CopyMode.class
                    || candidate.getParameterTypes()[1] != CopyContext.class) {
                throw new CopyException("Malformed @CopyCreator method on " + type.getName() + ". Expected a " +
                        "non-static method 'newInstance(CopyContext)' returning an instance of the declaring class.");
            }

            method = candidate;
            break;
        }

        if (method == null) {
            return null;
        }

        try {
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            throw new CopyException("Cannot access @CopyCreator method '" + method.getName() +
                    "' on " + type.getName(), e);
        }
    }

    private static List<Field> findFields(Class<?> type) {
        Field[] declaredFields = type.getDeclaredFields();
        List<Field> ownFields = new ArrayList<>(declaredFields.length);

        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    || Modifier.isFinal(modifiers)
                    || field.isSynthetic()
                    || field.isAnnotationPresent(CopyIgnore.class)) {
                continue;
            }

            try {
                field.setAccessible(true);
                ownFields.add(field);
            } catch (Exception e) {
                throw new CopyException("Cannot access field '" + field.getName() + "'. Consider implementing " +
                        "Copyable or registering a TypeCloner.");
            }
        }

        Class<?> superclass = type.getSuperclass();
        List<Field> parentFields = superclass != null && superclass != Object.class ?
                FIELDS.get(superclass) :
                List.of();

        if (ownFields.isEmpty()) {
            return parentFields;
        } else if (parentFields.isEmpty()) {
            return List.copyOf(ownFields);
        } else {
            List<Field> combined = new ArrayList<>(ownFields.size() + parentFields.size());
            combined.addAll(parentFields);
            combined.addAll(ownFields);
            return List.copyOf(combined);
        }
    }
}
