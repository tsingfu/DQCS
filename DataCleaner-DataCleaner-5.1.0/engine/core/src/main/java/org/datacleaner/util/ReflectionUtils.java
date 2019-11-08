/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.util;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;
import org.datacleaner.api.InputColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.gentyref.GenericTypeReflector;

/**
 * Various static methods for reflection related tasks.
 * 
 * 
 */
public final class ReflectionUtils {

    /**
     * A lock used at various points when calling Class.getAnnotation(...) or
     * Field.getAnnotation(...), since it seems there is a deadlock issue in the
     * Sun JVM when calling this method in a multithreaded environment!
     */
    public static final Object ANNOTATION_REFLECTION_LOCK = new Object();

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {
        // Prevent instantiation
    }

    /**
     * @return true if thisType is a valid type ofThatType, either as a single
     *         instance or as an array of ofThatType
     */
    public static boolean is(Type thisType, Class<?> ofThatType) {
        return is(thisType, ofThatType, true);
    }

    public static boolean is(Type thisType, Class<?> ofThatType, boolean includeArray) {
        Class<?> thisClass = null;
        if (thisType instanceof Class<?>) {
            thisClass = (Class<?>) thisType;
            if (includeArray && thisClass.isArray() && !ofThatType.isArray()) {
                if (ofThatType == Object.class) {
                    return true;
                }

                thisClass = thisClass.getComponentType();
            }
        }

        if (thisClass == ofThatType) {
            return true;
        }

        if (thisClass.isPrimitive() != ofThatType.isPrimitive()) {
            if (isByte(thisClass) && isByte(ofThatType)) {
                return true;
            }
            if (isCharacter(thisClass) && isCharacter(ofThatType)) {
                return true;
            }
            if (isBoolean(thisClass) && isBoolean(ofThatType)) {
                return true;
            }
            if (isShort(thisClass) && isShort(ofThatType)) {
                return true;
            }
            if (isInteger(thisClass) && isInteger(ofThatType)) {
                return true;
            }
            if (isLong(thisClass) && isLong(ofThatType)) {
                return true;
            }
            if (isFloat(thisClass) && isFloat(ofThatType)) {
                return true;
            }
            if (isDouble(thisClass) && isDouble(ofThatType)) {
                return true;
            }
        }
        return ofThatType.isAssignableFrom(thisClass);
    }

    public static boolean isCharacter(Type type) {
        return (type == char.class || type == Character.class);
    }

    public static boolean isInputColumn(Class<?> type) {
        return is(type, InputColumn.class);
    }

    public static boolean isColumn(Class<?> type) {
        return is(type, Column.class);
    }

    public static boolean isTable(Class<?> type) {
        return is(type, Table.class);
    }

    public static boolean isSchema(Class<?> type) {
        return is(type, Schema.class);
    }

    public static boolean isCloseable(Class<?> type) {
        return is(type, Closeable.class);
    }

    public static boolean isBoolean(Type type) {
        return (type == Boolean.class || type == boolean.class);
    }

    public static boolean isString(Type type) {
        return is(type, String.class);
    }

    public static boolean isShort(Type type) {
        return (type == Short.class || type == short.class);
    }

    public static boolean isDouble(Type type) {
        return (type == Double.class || type == double.class);
    }

    public static boolean isLong(Type type) {
        return (type == Long.class || type == long.class);
    }

    public static boolean isInteger(Type type) {
        return (type == Integer.class || type == int.class);
    }

    public static boolean isFloat(Type type) {
        return (type == Float.class || type == float.class);
    }

    public static boolean isMap(Type type) {
        return type == Map.class;
    }

    public static boolean isSet(Type type) {
        return type == Set.class;
    }

    public static boolean isList(Type type) {
        return type == List.class;
    }

    public static boolean isDate(Type type) {
        return is(type, Date.class, false);
    }

    public static boolean isNumber(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            boolean numberClass = is(clazz, Number.class, false);
            if (numberClass) {
                return true;
            }
            return type == byte.class || type == int.class || type == short.class || type == long.class
                    || type == float.class || type == double.class;
        }
        return false;
    }

    public static boolean isByte(Type type) {
        return type == byte.class || type == Byte.class;
    }

    public static boolean isByteArray(Type type) {
        if (type == byte[].class || type == Byte[].class) {
            return true;
        }
        return false;
    }

    public static String explodeCamelCase(String str, boolean excludeGetOrSet) {
      return ApiStringUtils.explodeCamelCase(str, excludeGetOrSet);
    }

    public static int getTypeParameterCount(Field field) {
        Type genericType = field.getGenericType();
        return getTypeParameterCount(genericType);
    }

    public static int getTypeParameterCount(Type genericType) {
        if (genericType instanceof GenericArrayType) {
            GenericArrayType gaType = (GenericArrayType) genericType;
            genericType = gaType.getGenericComponentType();
        }
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type[] typeArguments = pType.getActualTypeArguments();
            return typeArguments.length;
        }
        return 0;
    }

    public static Class<?> getTypeParameter(Class<?> clazz, Class<?> genericInterface, int parameterIndex) {
        Type baseType = GenericTypeReflector.getExactSuperType(clazz, genericInterface);
        ParameterizedType pBaseType = (ParameterizedType) baseType;
        Type typeParameterForBaseInterface = pBaseType.getActualTypeArguments()[parameterIndex];
        return getSafeClassToUse(typeParameterForBaseInterface);
    }

    public static Class<?> getTypeParameter(Type genericType, int parameterIndex) {
        if (genericType instanceof GenericArrayType) {
            GenericArrayType gaType = (GenericArrayType) genericType;
            genericType = gaType.getGenericComponentType();
        }
        if (genericType instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) genericType;
            Type[] typeArguments = ptype.getActualTypeArguments();
            if (typeArguments.length > parameterIndex) {
                Type argument = typeArguments[parameterIndex];
                return getSafeClassToUse(argument);
            } else {
                throw new IllegalArgumentException("Only " + typeArguments.length + " parameters available");
            }
        }
        return null;
    }

    public static Class<?> getTypeParameter(Field field, int parameterIndex) {
        Type genericType = field.getGenericType();
        return getTypeParameter(genericType, parameterIndex);
    }

    public static boolean isWildcard(Type type) {
        return type instanceof WildcardType;
    }

    private static Class<?> getSafeClassToUse(Type someType) {
        if (someType instanceof GenericArrayType) {
            GenericArrayType gaType = (GenericArrayType) someType;
            someType = gaType.getGenericComponentType();
            return Array.newInstance((Class<?>) someType, 0).getClass();
        }

        if (someType instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) someType;

            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds != null && upperBounds.length > 0) {
                return (Class<?>) upperBounds[0];
            }

            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds != null && lowerBounds.length > 0) {
                return (Class<?>) lowerBounds[0];
            }
        } else if (someType instanceof Class) {
            return (Class<?>) someType;
        } else if (someType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) someType;
            return (Class<?>) pType.getRawType();
        }

        throw new UnsupportedOperationException("Parameter type not supported: " + someType);
    }

    public static int getHierarchyDistance(Class<?> subtype, Class<?> supertype) throws IllegalArgumentException {
        assert subtype != null;
        assert supertype != null;

        if (!ReflectionUtils.is(subtype, supertype)) {
            throw new IllegalArgumentException("Not a valid subtype of " + supertype.getName() + ": " + subtype
                    .getName());
        }

        if (supertype.isInterface()) {
            return getInterfaceHierarchyDistance(subtype, supertype);
        } else {
            return getClassHierarchyDistance(subtype, supertype);
        }
    }

    private static int getClassHierarchyDistance(final Class<?> subtype, final Class<?> supertype) {
        if (subtype == supertype) {
            return 0;
        }
        if (subtype == Object.class) {
            return Integer.MAX_VALUE;
        }

        final Class<?> subSuperclass = subtype.getSuperclass();
        final int distance = getClassHierarchyDistance(subSuperclass, supertype);
        if (distance != Integer.MAX_VALUE) {
            return 1 + distance;
        }
        return Integer.MAX_VALUE;
    }

    private static int getInterfaceHierarchyDistance(final Class<?> subtype, final Class<?> supertype) {
        if (subtype == supertype) {
            return 0;
        }

        final Class<?>[] interfaces = subtype.getInterfaces();
        for (Class<?> i : interfaces) {
            if (i == supertype) {
                return 1;
            }
        }

        int bestCandidate = Integer.MAX_VALUE;

        if (!subtype.isInterface()) {
            final Class<?> subSuperclass = subtype.getSuperclass();
            if (subSuperclass != null) {
                final int distance = getInterfaceHierarchyDistance(subSuperclass, supertype);
                if (distance != Integer.MAX_VALUE) {
                    final int candidate = 1 + distance;
                    bestCandidate = Math.min(bestCandidate, candidate);
                }
            }
        }

        for (Class<?> i : interfaces) {
            final Class<?>[] subInterfaces = i.getInterfaces();
            if (subInterfaces != null && subInterfaces.length > 0) {
                for (Class<?> subInterface : subInterfaces) {
                    final int distance = getInterfaceHierarchyDistance(subInterface, supertype);
                    if (distance != Integer.MAX_VALUE) {
                        final int candidate = 1 + distance;
                        bestCandidate = Math.min(bestCandidate, candidate);
                    }
                }
            }
        }

        return bestCandidate;
    }

    public static boolean isArray(Object o) {
        if (o == null) {
            return false;
        }
        return o.getClass().isArray();
    }

    public static Method[] getMethods(Class<?> clazz, Class<? extends Annotation> withAnnotation) {
        List<Method> result = new ArrayList<Method>();

        Method[] methods = getMethods(clazz);
        for (Method method : methods) {
            if (isAnnotationPresent(method, withAnnotation)) {
                result.add(method);
            }
        }

        return result.toArray(new Method[result.size()]);
    }

    public static Field[] getAllFields(Class<?> clazz, Class<? extends Annotation> withAnnotation) {
        List<Field> result = new ArrayList<Field>();

        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            if (isAnnotationPresent(field, withAnnotation)) {
                result.add(field);
            }
        }

        return result.toArray(new Field[result.size()]);
    }

    /**
     * Gets a method of a class by name.
     * 
     * @param clazz
     * @param name
     * @return
     */
    public static Method getMethod(Class<?> clazz, String name) {
        return getMethod(clazz, name, false);
    }

    /**
     * Gets a method of a class by name.
     * 
     * @param clazz
     * @param name
     * @param withParameters
     *            whether or not to include methods with parameters
     * @return
     */
    public static Method getMethod(Class<?> clazz, String name, boolean withParameters) {
        if (clazz == Object.class || clazz == null) {
            return null;
        }

        try {
            // first try without parameters
            Method method = clazz.getDeclaredMethod(name);
            return method;
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            if (withParameters) {
                Method[] methods = getMethods(clazz);
                for (Method method : methods) {
                    if (name.equals(method.getName())) {
                        return method;
                    }
                }
                return null;
            }
            return getMethod(clazz.getSuperclass(), name, withParameters);
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == Object.class || clazz == null) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            return getField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * Tells which approach {@link #getMethods(Class)} is being implemented with
     * 
     * @return
     * 
     * @deprecated since DataCleaner 5.0 we no longer support Java 7 or older,
     *             so there is no longer a "legacy approach". This method always
     *             returns false.
     */
    @Deprecated
    public static boolean isGetMethodsLegacyApproach() {
        return false;
    }

    /**
     * Gets all methods of a class, excluding those from Object.
     * 
     * @param clazz
     * @return
     */
    public static Method[] getMethods(Class<?> clazz) {
        final List<Method> allMethods = new ArrayList<>();
        addMethods(allMethods, clazz);

        return allMethods.toArray(new Method[allMethods.size()]);
    }

    private static void addMethods(final List<Method> allMethods, final Class<?> clazz) {
        if (clazz == Object.class || clazz == null) {
            return;
        }

        final Method[] methods = clazz.getMethods();
        for (final Method method : methods) {
            final Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass != Object.class) {
                allMethods.add(method);
            }
        }
    }

    /**
     * Gets all fields of a class, including private fields in super-classes.
     * 
     * @param clazz
     * @return
     */
    public static Field[] getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();
        addFields(allFields, clazz);
        return allFields.toArray(new Field[allFields.size()]);
    }

    /**
     * Gets non-synthetic fields of a class, including private fields in
     * super-classes.
     *
     * @param clazz
     * @return
     */
    public static Field[] getNonSyntheticFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        addFields(fieldList, clazz, true);

        return fieldList.toArray(new Field[fieldList.size()]);
    }

    private static void addFields(List<Field> allFields, Class<?> clazz) {
        addFields(allFields, clazz, false);
    }

    private static void addFields(List<Field> allFields, Class<?> clazz, boolean excludeSynthetic) {
        if (clazz == Object.class) {
            return;
        }

        Field[] f = clazz.getDeclaredFields();

        for (Field field : f) {
            if (excludeSynthetic && field.isSynthetic()) {
                continue;
            }

            allFields.add(field);
        }

        Class<?> superclass = clazz.getSuperclass();
        addFields(allFields, superclass, excludeSynthetic);
    }

    public static <E> E newInstance(Class<? extends E> clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            logger.warn("Could not instantiate {}: {}", clazz, e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IllegalStateException(e);
        }
    }

    public static <A extends Annotation> A getAnnotation(Enum<?> enumConstant, Class<A> annotationClass) {
        try {
            Field field = enumConstant.getClass().getDeclaredField(enumConstant.name());
            return getAnnotation(field, annotationClass);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotationClass) {
        synchronized (ANNOTATION_REFLECTION_LOCK) {
            final A annotation = element.getAnnotation(annotationClass);
            if (annotation == null && element instanceof Method) {
                // check for annotations on overridden methods. Since Java 8
                // those are not returned by .getAnnotation(...)
                final Method m = (Method) element;
                final Class<?> declaringClass = m.getDeclaringClass();
                final Class<?> superClass = declaringClass.getSuperclass();
                final String methodName = m.getName();
                final Class<?>[] methodParameterTypes = m.getParameterTypes();
                if (superClass != null) {
                    try {
                        final Method overriddenMethod = superClass.getMethod(methodName, methodParameterTypes);
                        return getAnnotation(overriddenMethod, annotationClass);
                    } catch (NoSuchMethodException e) {
                        logger.debug("Failed to get overridden method '{}' from {}", methodName, superClass);
                    }
                }

                // check for annotations on interface methods too.
                final Class<?>[] interfaces = declaringClass.getInterfaces();
                for (Class<?> interfaceClass : interfaces) {
                    try {
                        final Method overriddenMethod = interfaceClass.getMethod(methodName, methodParameterTypes);
                        return getAnnotation(overriddenMethod, annotationClass);
                    } catch (NoSuchMethodException e) {
                        logger.debug("Failed to get overridden method '{}' from {}", methodName, interfaceClass);
                    }
                }
            }
            return annotation;
        }
    }

    public static boolean isAnnotationPresent(Enum<?> enumConstant, Class<? extends Annotation> annotationClass) {
        try {
            Field field = enumConstant.getClass().getDeclaredField(enumConstant.name());
            return isAnnotationPresent(field, annotationClass);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isAnnotationPresent(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        return getAnnotation(element, annotationClass) != null;
    }
}
