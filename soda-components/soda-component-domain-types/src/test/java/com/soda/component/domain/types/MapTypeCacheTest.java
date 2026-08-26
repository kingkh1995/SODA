package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MapTypeCache Map 实例缓存")
class MapTypeCacheTest {

    @Test
    @DisplayName("命中返回实例")
    void should_returnInstance_when_keyHit() {
        var cache = new MapTypeCache<>(Map.of("a", 1, "b", 2));
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("b")).isEqualTo(2);
    }

    @Test
    @DisplayName("未命中返回 null")
    void should_returnNull_when_keyMisses() {
        var cache = new MapTypeCache<>(Map.of("a", 1));
        assertThat(cache.get("missing")).isNull();
    }

    @Test
    @DisplayName("不可变性：构造后修改原 Map 不影响缓存")
    void should_notAffectCache_when_originalMapMutated() {
        var original = new HashMap<String, Integer>();
        original.put("a", 1);
        var cache = new MapTypeCache<>(original);
        original.put("b", 2);
        assertThat(cache.get("b")).isNull();
    }

    @Test
    @DisplayName("entries 为 null 抛异常")
    void should_throw_when_entriesIsNull() {
        assertThatThrownBy(() -> new MapTypeCache<>(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("空缓存在任意 key 上都未命中")
    void should_returnNull_when_cacheEmpty() {
        var cache = new MapTypeCache<>(Map.of());
        assertThat(cache.get("anything")).isNull();
    }
}
