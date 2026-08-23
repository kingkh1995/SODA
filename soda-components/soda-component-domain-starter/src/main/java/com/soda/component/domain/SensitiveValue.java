package com.soda.component.domain;

import org.springframework.util.Assert;
import lombok.EqualsAndHashCode;

/**
 * 敏感值抽象基类 —— 所有敏感数据 DP 的统一父类，编译期强制 toString 安全。
 * <p>
 * {@code value()} 与 {@code toString()} 为手写 final——脱敏输出不可被子类绕过；
 * equals/hashCode 由 Lombok 生成，子类仅需实现 {@link #maskedValue()} 返回日志安全表示。
 * <b>任何敏感 DP 必须继承本类</b>——toString 脱敏由类型系统保证，不依赖约定或测试兜底。
 * <p>
 * 相等性为 class-aware：基类 canEqual 经子类 {@code @EqualsAndHashCode(callSuper = true)}
 * 逐层收窄，不同具体子类型即使存储值相同也不相等——子类标注为强制契约，
 * 由 {@code SensitiveValueContractTest} 校验（漏标将静默继承基类相等语义，跨类误等）。
 *
 * @see StringLiteralType
 * @see com.soda.component.domain.types.Ciphertext
 * @see com.soda.component.domain.types.PasswordHash
 * @see com.soda.component.domain.types.MaskedMobile
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class SensitiveValue implements StringLiteralType {

    @EqualsAndHashCode.Include
    private final String value;

    protected SensitiveValue(String value) {
        // 本类位于 domain-starter，模块依赖为 types → starter 单向，无法引用 domain.util.ValidateUtils；
        // 直接使用其底层 Spring Assert，消息与 ValidateUtils.hasText 逐字一致。
        Assert.hasText(value, "must not be blank");
        this.value = value;
    }

    /**
     * 规范存储值：JSON 序列化与数据库存储使用。
     */
    @Override
    public final String value() {
        return value;
    }

    /**
     * 日志安全表示 —— 子类实现须为基于 value() 的脱敏纯函数（无缓存，可能被多次调用）；
     * 输出格式由 {@code toString()} 统一为 {@code 类名[masked=...]}。
     */
    public abstract String maskedValue();

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[masked=" + maskedValue() + "]";
    }

}