package com.holberton.realestate.checks.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationContext;

/**
 * Looks at your classes without using them: the checks load them by name, so a class that
 * does not exist yet makes one check fail instead of breaking the compilation of all of them.
 */
public final class Reflect {

    public static final String PACKAGE = "com.holberton.realestate";

    private Reflect() {
    }

    public static Optional<Class<?>> find(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | LinkageError error) {
            return Optional.empty();
        }
    }

    /** The class, or a failure naming the file that is expected. */
    public static Class<?> require(String className) {
        return find(className).orElseThrow(() -> new AssertionError(
                "expected the class " + className + " (file src/main/java/"
                        + className.replace('.', '/') + ".java) but it does not exist"));
    }

    public static boolean hasAnnotation(AnnotatedElement element, String annotationName) {
        return annotation(element, annotationName).isPresent();
    }

    public static Optional<Annotation> annotation(AnnotatedElement element, String annotationName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return Optional.of(annotation);
            }
            // @RestController is itself annotated with @Controller: look one level up too.
            for (Annotation meta : annotation.annotationType().getAnnotations()) {
                if (meta.annotationType().getName().equals(annotationName)) {
                    return Optional.of(meta);
                }
            }
        }
        return Optional.empty();
    }

    public static void requireAnnotation(Class<?> type, String annotationName) {
        if (!hasAnnotation(type, annotationName)) {
            Expect.fail("expected " + type.getSimpleName() + " to be annotated with @"
                    + simpleName(annotationName) + " but its annotations are " + annotationNames(type));
        }
    }

    /** The value of one attribute of an annotation, as text. */
    public static Optional<String> attribute(AnnotatedElement element, String annotationName, String attribute) {
        return annotation(element, annotationName).map(annotation -> {
            try {
                Method accessor = annotation.annotationType().getMethod(attribute);
                Object value = accessor.invoke(annotation);
                return String.valueOf(value);
            } catch (ReflectiveOperationException exception) {
                return "";
            }
        });
    }

    public static List<String> annotationNames(AnnotatedElement element) {
        List<String> names = new ArrayList<>();
        for (Annotation annotation : element.getAnnotations()) {
            names.add("@" + annotation.annotationType().getSimpleName());
        }
        return names;
    }

    public static boolean hasMethod(Class<?> type, String name) {
        return method(type, name).isPresent();
    }

    public static Optional<Method> method(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return Optional.of(method);
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    public static List<String> methodNames(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (!method.getDeclaringClass().equals(Object.class)) {
                names.add(method.getName());
            }
        }
        return names;
    }

    /** Every method declared by the class or inherited from one of its own interfaces. */
    public static Set<Method> ownMethods(Class<?> type) {
        Set<Method> methods = new LinkedHashSet<>(Arrays.asList(type.getDeclaredMethods()));
        if (type.isInterface()) {
            methods.addAll(Arrays.asList(type.getMethods()));
        }
        return methods;
    }

    public static boolean hasProtectedNoArgsConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0 && Modifier.isProtected(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /** True when one constructor of the class takes a parameter of this type. */
    public static boolean constructorTakes(Class<?> type, String parameterClassName) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            for (Class<?> parameter : constructor.getParameterTypes()) {
                if (matches(parameter, parameterClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasFieldOfType(Class<?> type, String fieldClassName) {
        for (Field field : type.getDeclaredFields()) {
            if (matches(field.getType(), fieldClassName)) {
                return true;
            }
        }
        return false;
    }

    public static List<Field> fieldsAnnotatedWith(Class<?> type, String annotationName) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (hasAnnotation(field, annotationName)) {
                fields.add(field);
            }
        }
        return fields;
    }

    /** The classes of your @RestController beans, as the application really loaded them. */
    public static List<Class<?>> restControllers(ApplicationContext context) {
        List<Class<?>> controllers = new ArrayList<>();
        Class<?> annotation = find("org.springframework.web.bind.annotation.RestController").orElse(null);
        if (annotation == null) {
            return controllers;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> restController = (Class<? extends Annotation>) annotation;
        for (Object bean : context.getBeansWithAnnotation(restController).values()) {
            Class<?> type = bean.getClass();
            while (type.getName().contains("$$")) {
                type = type.getSuperclass();
            }
            controllers.add(type);
        }
        return controllers;
    }

    /** True when the generic type of a method mentions this class, for example List&lt;Property&gt;. */
    public static boolean mentions(java.lang.reflect.Type type, String className) {
        for (String token : type.getTypeName().split("[^A-Za-z0-9_.$]+")) {
            if (token.equals(className)) {
                return true;
            }
        }
        return false;
    }

    /** True when one annotation of the element has this simple name, whatever its package. */
    public static boolean hasAnnotationNamed(AnnotatedElement element, String simpleName) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    /** True when one field of the class carries an annotation of this simple name. */
    public static boolean hasFieldAnnotatedNamed(Class<?> type, String simpleName) {
        for (Field field : type.getDeclaredFields()) {
            if (hasAnnotationNamed(field, simpleName)) {
                return true;
            }
        }
        return false;
    }

    /** The number of methods annotated with @Test in a test class. */
    public static int testMethods(Class<?> type) {
        int count = 0;
        for (Method method : type.getDeclaredMethods()) {
            if (hasAnnotationNamed(method, "Test") || hasAnnotationNamed(method, "ParameterizedTest")) {
                count++;
            }
        }
        return count;
    }

    private static boolean matches(Class<?> type, String className) {
        return type.getName().equals(className) || type.getSimpleName().equals(simpleName(className));
    }

    public static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }
}
