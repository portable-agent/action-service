package dev.portableagent.action.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapUtil {
    public static <K, V> Map<K, V> toMap(Collection<V> values, Function<V, K> key) {
        return values.stream().collect(Collectors.toUnmodifiableMap(key, Function.identity()));
    }
}
