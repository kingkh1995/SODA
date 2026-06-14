package com.soda.component.support.spi;

/**
 * Type 配置 SPI 提供者 — 通过 {@link java.util.ServiceLoader} 发现。
 * <p>
 * 不提供 SPI 实现时使用接口的默认方法回退值。
 * 业务模块可通过 {@code META-INF/services/com.soda.component.support.spi.TypeConfigProvider}
 * 注册自定义实现。
 * <p>
 * 参考 kk-ddd 的 {@code TypeConstantsProvider} 设计。
 */
public interface TypeConfigProvider {

    /** 版本号缓存上限（含）。默认 99，至少 99。 */
    default int versionCacheHigh() {
        return 99;
    }
}
