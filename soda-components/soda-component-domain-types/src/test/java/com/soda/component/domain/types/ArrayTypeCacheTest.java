package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArrayTypeCache")
class ArrayTypeCacheTest {

    @Test
    @DisplayName("范围内返回缓存实例")
    void get_withinRange_returnsCachedInstance() {
        var cache = new ArrayTypeCache<>(0, 99, String::valueOf);
        assertThat(cache.get(42)).isNotNull().isSameAs(cache.get(42));
    }

    @Test
    @DisplayName("范围外返回 empty")
    void get_beyondRange_returnsEmpty() {
        var cache = new ArrayTypeCache<>(0, 99, String::valueOf);
        assertThat(cache.get(100)).isNull();
        assertThat(cache.get(-1)).isNull();
    }

    @Test
    @DisplayName("偏移起始不为 0 的正确定位")
    void get_withOffset_returnsCorrectInstance() {
        var cache = new ArrayTypeCache<>(10, 20, String::valueOf);
        assertThat(cache.get(10)).isEqualTo("10");
        assertThat(cache.get(15)).isEqualTo("15");
        assertThat(cache.get(20)).isEqualTo("20");
        assertThat(cache.get(9)).isNull();
        assertThat(cache.get(21)).isNull();
    }

    @Test
    @DisplayName("单值范围")
    void get_singleValueRange() {
        var cache = new ArrayTypeCache<>(5, 5, String::valueOf);
        assertThat(cache.get(5)).isEqualTo("5");
        assertThat(cache.get(4)).isNull();
        assertThat(cache.get(6)).isNull();
    }

    @Test
    @DisplayName("factory 为 null 抛异常")
    void constructor_nullFactory_throws() {
        assertThatThrownBy(() -> new ArrayTypeCache<>(0, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("low() 和 high() 返回正确边界")
    void lowHigh_returnsCorrectBounds() {
        var cache = new ArrayTypeCache<>(5, 15, String::valueOf);
        assertThat(cache.low()).isEqualTo(5);
        assertThat(cache.high()).isEqualTo(15);
    }
}
