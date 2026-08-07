package com.soda.user.infrastructure;

import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.VerificationChannel;

/**
 * {@code class → VerificationChannel} 推导 — 基础设施侧映射（ADR-0016 决策 3/7）。
 * <p>
 * domain 枚举不持 Class 引用；gateway 实现按 {@code Class<T>} 参数经本类推导判别值
 * 用于持久化过滤。一致性由 {@link VerificationChannelsTest} 锁定
 * （class → 枚举 → {@code @JsonTypeName} 三方一致）。
 */
public final class VerificationChannels {

    private VerificationChannels() {
    }

    /**
     * 推导验证类型对应的渠道。
     *
     * @param type 验证实体类型（须为 {@link Verification} 的 permits 子类）
     * @return 对应渠道
     * @throws IllegalArgumentException 未知类型时（非 permits 子类或新增后未登记）
     */
    public static VerificationChannel of(Class<? extends Verification<?>> type) {
        if (type == SmsVerification.class) {
            return VerificationChannel.S;
        }
        if (type == EmailVerification.class) {
            return VerificationChannel.E;
        }
        throw new IllegalArgumentException("Unsupported verification type: " + type.getName());
    }
}
