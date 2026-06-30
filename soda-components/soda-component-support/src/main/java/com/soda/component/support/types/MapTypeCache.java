package com.soda.component.support.types;

import com.soda.component.support.util.ValidateUtils;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * DP 实例缓存 — HashMap O(1) 查找，不可变。
 * <p>
 * 用于非 int 键映射的 DP（如枚举键场景），
 * 在构造时从 {@link Map} 加载全部实例。
 *
 * @param <K> key 类型
 * @param <V> DP 类型
 */
public final class MapTypeCache<K, V> {

    private final Map<K, V> cache;

    /**
     * 从已有映射构造缓存。
     *
     * @param entries 全部实例的键值映射
     */
    public MapTypeCache(Map<K, V> entries) {
        ValidateUtils.notNull(entries);
        this.cache = Map.copyOf(entries);
    }

    /**
     * 按 key 查找缓存实例。
     *
     * @param key 键
     * @return 命中返回缓存实例，未命中返回 {@code null}
     */
    public @Nullable V get(K key) {
        return cache.get(key);
    }
}
