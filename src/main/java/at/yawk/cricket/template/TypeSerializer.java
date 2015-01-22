/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.Value;

/**
 * @author yawkat
 */
class TypeSerializer<T> implements Serializer.CustomSerializer<T> {
    private static final Map<Class<?>, TypeSerializer<?>> CACHE = new ConcurrentHashMap<>();

    private final Map<String, Member> members = new LinkedHashMap<>();

    @SuppressWarnings({ "unchecked", "Convert2MethodRef" })
    public static <T> TypeSerializer<T> forClass(Class<T> clazz) {
        return (TypeSerializer<T>) CACHE.computeIfAbsent(clazz, t -> new TypeSerializer(t));
    }

    private TypeSerializer(Class<T> type) {
        boolean fields;
        boolean methods;
        ParameterClass parameterClass = type.getAnnotation(ParameterClass.class);
        if (parameterClass != null) {
            fields = parameterClass.fields();
            methods = parameterClass.methods();
        } else {
            fields = false;
            methods = false;
        }
        fields(type).forEach(field -> {
            field.setAccessible(true);
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (fields || parameter != null) {
                String name;
                if (parameter != null && !parameter.value().isEmpty()) {
                    name = parameter.value();
                } else {
                    name = field.getName();
                }
                members.put(
                        name,
                        new Member(parameter != null && parameter.flat(), field::get)
                );
            }
        });
        methods(type).forEach(method -> {
            method.setAccessible(true);
            Optional<Parameter> parameter = annotation(Parameter.class, method);
            if (methods || parameter.isPresent()) {
                String name = parameter.map(Parameter::value).filter(s -> !s.isEmpty()).orElse(method.getName());
                members.put(
                        name,
                        new Member(parameter.map(Parameter::flat).orElse(false), method::invoke)
                );
            }
        });
    }

    public Map<String, Object> serialize(T from, Serializer entrySerializer) {
        Map<String, Object> result = new HashMap<>();
        serializeTo(from, entrySerializer, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void serializeTo(T from, Serializer entrySerializer, Map<String, Object> result) {
        try {
            for (Map.Entry<String, Member> entry : members.entrySet()) {
                String name = entry.getKey();
                Member member = entry.getValue();
                Object decoded = member.getAccessor().get(from);
                if (member.flat) {
                    if (decoded != null) {
                        ((TypeSerializer) TypeSerializer.forClass(decoded.getClass()))
                                .serializeTo(decoded, entrySerializer, result);
                    }
                } else {
                    result.put(name, entrySerializer.serialize(decoded));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Method> methods(Class<?> of) {
        return superClasses(of).flatMap(c -> Arrays.stream(c.getDeclaredMethods()));
    }

    private static Stream<Field> fields(Class<?> of) {
        return superClasses(of).flatMap(c -> Arrays.stream(c.getDeclaredFields()));
    }

    public <A extends Annotation> Optional<A> annotation(Class<A> type, Method on) {
        return methods(on.getDeclaringClass())
                .filter(m -> m.getName().equals(on.getName()) &&
                             Arrays.equals(m.getParameterTypes(), on.getParameterTypes()) &&
                             !Modifier.isPrivate(m.getModifiers()))
                .flatMap(m -> {
                    A annotation = m.getAnnotation(type);
                    return annotation == null ? Stream.empty() : Stream.of(annotation);
                })
                .findFirst();
    }

    private static Stream<Class<?>> superClasses(Class<?> of) {
        if (of == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(of),
                Stream.concat(
                        superClasses(of.getSuperclass()),
                        Arrays.stream(of.getInterfaces()).flatMap(TypeSerializer::superClasses)
                )
        ).distinct();
    }

    @Value
    private static class Member {
        boolean flat;
        MemberAccessor accessor;
    }

    private static interface MemberAccessor {
        Object get(Object from) throws ReflectiveOperationException;
    }
}
