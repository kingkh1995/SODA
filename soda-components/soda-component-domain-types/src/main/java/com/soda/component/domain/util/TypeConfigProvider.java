package com.soda.component.domain.util;

/**
 * Type 配置 SPI 提供者 — 通过 {@link java.util.ServiceLoader} 发现。
 * <p>
 * 不提供 SPI 实现时使用接口的默认方法回退值。
 * 业务模块可通过 {@code META-INF/services/com.soda.component.domain.util.TypeConfigProvider}
 * 注册自定义实现。
 * <p>
 * 参考 kk-ddd 的 {@code TypeConstantsProvider} 设计。
 */
public interface TypeConfigProvider {

    /**
     * 版本号缓存上限（含）。默认 99，至少 99。
     */
    default int versionCacheHigh() {
        return 99;
    }

    /**
     * 短信内容最大长度。默认 70，至少 70。
     */
    default int smsContentMaxLength() {
        return 70;
    }

    /**
     * 邮件主题最大长度。默认 255，至少 255。
     */
    default int emailSubjectMaxLength() {
        return 255;
    }

    /**
     * 凭证哈希值最大长度。默认 255，至少 128。
     */
    default int credentialHashMaxLength() {
        return 255;
    }

    /**
     * WanYuan 小数位数（万元后保留几位）。取值范围 0-4，默认 2。
     */
    default int wanYuanScale() {
        return 2;
    }

    /**
     * 百分比小数位数（小数点后保留几位）。取值范围 0-4，默认 2。
     */
    default int percentageScale() {
        return 2;
    }

    /**
     * 正整数缓存上限（含）。默认 100，至少 100。
     */
    default int positiveIntCacheHigh() {
        return 100;
    }
}
