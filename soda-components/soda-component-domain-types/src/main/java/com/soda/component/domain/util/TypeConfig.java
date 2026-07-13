package com.soda.component.domain.util;


import java.util.ServiceLoader;

/**
 * Type 配置持有者 — 通过 SPI 加载 {@link TypeConfigProvider}，无实现时回退默认值。
 * <p>
 * 参考 kk-ddd 的 {@code Constants.TYPE} 设计。
 */
public final class TypeConfig {

    /**
     * SPI 提供者，仅加载第一个。无注册实现时使用默认回退。
     */
    public static final TypeConfigProvider PROVIDER =
            ServiceLoader.load(TypeConfigProvider.class)
                    .findFirst()
                    .orElseGet(() -> new TypeConfigProvider() {
                    });

    private TypeConfig() {
        throw new UnsupportedOperationException();
    }
}
