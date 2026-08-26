package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import lombok.EqualsAndHashCode;

/**
 * 敏感值抽象基类 —— 所有敏感数据 DP 的统一父类，编译期强制 toString 安全。
 * <p>
 * {@code value()} 与 {@code toString()} 为手写 final——脱敏输出不可被子类绕过；
 * equals/hashCode 由 Lombok 生成，子类仅需实现 {@link #maskedValue()} 返回日志安全表示。
 * <b>任何敏感 DP 必须继承本类</b>——toString 脱敏由类型系统保证，不依赖约定或测试兜底。
 * <p>
 * <b>不允许 {@code null}</b> —— 构造器调 {@link ValidateUtils#hasText(String)}，
 * null / 空白字符串均抛 {@link IllegalArgumentException}（DP 规范见
 * {@code docs/dp-conventions.md}「可空性」条）。子类构造器同样禁止透传 null
 * ——基类校验先于子类域校验，自然屏蔽。
 * <p>
 * 相等性为 class-aware：基类 canEqual 经子类 {@code @EqualsAndHashCode(callSuper = true)}
 * 逐层收窄，不同具体子类型即使存储值相同也不相等——子类标注为强制契约，
 * 由 {@code SensitiveValueContractTest} 校验（漏标将静默继承基类相等语义，跨类误等）。
 *
 * @see StringLiteralType
 * @see Ciphertext
 * @see PasswordHash
 * @see MaskedMobile
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class SensitiveValue implements StringLiteralType {

    @EqualsAndHashCode.Include
    private final String value;

    protected SensitiveValue(String value) {
        ValidateUtils.hasText(value);
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