package com.soda.component.support.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MapTypeCache")
class MapTypeCacheTest {

    @Test
    @DisplayName("命中返回实例")
    void get_hit_returnsInstance() {
        var cache = new MapTypeCache<>(Map.of("a", 1, "b", 2));
        assertThat(cache.get("a")).isEqualTo(1);
        assertThat(cache.get("b")).isEqualTo(2);
    }

    @Test
    @DisplayName("未命中返回 empty")
    void get_miss_returnsEmpty() {
        var cache = new MapTypeCache<>(Map.of("a", 1));
        assertThat(cache.get("missing")).isNull();
    }

    @Test
    @DisplayName("不可变性：构造后修改原 Map 不影响缓存")
    void immutable_originalMapMutation_doesNotAffectCache() {
        var original = new java.util.HashMap<String, Integer>();
        original.put("a", 1);
        var cache = new MapTypeCache<>(original);
        original.put("b", 2);
        assertThat(cache.get("b")).isNull();
    }

    @Test
    @DisplayName("entries 为 null 抛异常")
    void constructor_nullEntries_throws() {
        assertThatThrownBy(() -> new MapTypeCache<>(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("空构造命中返回 empty")
    void get_empty_returnsEmpty() {
        var cache = new MapTypeCache<>(Map.of());
        assertThat(cache.get("anything")).isNull();
    }
}
