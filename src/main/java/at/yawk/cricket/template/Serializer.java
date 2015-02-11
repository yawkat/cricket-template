/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import com.google.common.primitives.Primitives;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yawkat
 */
public class Serializer {
    private final Map<Class<?>, CustomSerializer<?>> specials = new HashMap<>();
    private final Map<Class<?>, CustomSerializer<?>> cache = new ConcurrentHashMap<>();

    private static final CustomSerializer<Number> NUMBER = (n, s) -> {
        if (n.longValue() == n.doubleValue()) {
            return n.longValue();
        } else {
            return n;
        }
    };

    @SuppressWarnings({ "unchecked", "Convert2MethodRef" })
    public Serializer() {
        special(String.class, CustomSerializer.IDENTITY);
        Primitives.allWrapperTypes().forEach(pr -> {
            if (Number.class.isAssignableFrom(pr)) {
                special(pr, (CustomSerializer) NUMBER);
            } else {
                special(pr, CustomSerializer.IDENTITY);
            }
        });

        special(URL.class, CustomSerializer.TO_STRING);
        special(UUID.class, CustomSerializer.TO_STRING);

        special(Collection.class,
                (col, ser) -> {
                    Stream serialized = col.stream().map((Object o) -> ser.serialize(o));
                    return serialized.collect((Collector) Collectors.toList());
                });
        special(Map.class,
                (map, ser) -> {
                    Map<Object, Object> serialized = new HashMap<>();
                    map.forEach((k, v) -> serialized.put(ser.serialize(k), ser.serialize(v)));
                    return serialized;
                });
        special(Optional.class, (opt, ser) -> opt.map(ser::serialize).orElse(null));
    }

    public <T> Serializer special(Class<T> on, CustomSerializer<? super T> serializer) {
        specials.put(on, serializer);
        cache.clear();
        return this;
    }

    @SuppressWarnings({ "unchecked", "Convert2MethodRef" })
    public <T> Object serialize(T other) {
        if (other == null) {
            return null;
        }

        Class<T> type = (Class<T>) other.getClass();

        CustomSerializer<? super T> serializer =
                (CustomSerializer<? super T>) cache.computeIfAbsent(type, c -> findSerializer(c));
        return serializer.serialize(other, this);
    }

    private <T> CustomSerializer<? super T> findSerializer(Class<T> of) {
        CustomSerializer<? super T> special = findSpecial(of);
        return special == null ? TypeSerializer.forClass(of) : special;
    }

    @SuppressWarnings("unchecked")
    private <T> CustomSerializer<? super T> findSpecial(Class<T> of) {
        if (of == null) {
            return null;
        }
        CustomSerializer<T> special = (CustomSerializer<T>) specials.get(of);
        if (special != null) {
            return special;
        }
        CustomSerializer<? super T> fromSuper = findSpecial((Class) of.getSuperclass());
        if (fromSuper != null) {
            return fromSuper;
        }
        for (Class<?> intf : of.getInterfaces()) {
            CustomSerializer<? super T> fromInterface = findSpecial((Class) intf);
            if (fromInterface != null) {
                return fromInterface;
            }
        }
        return null;
    }

    public static interface CustomSerializer<F> {
        public static final CustomSerializer<Object> IDENTITY = (o, s) -> o;
        public static final CustomSerializer<Object> TO_STRING = (o, s) -> String.valueOf(o);

        Object serialize(F from, Serializer serializer);
    }
}
