package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 秘密值 DP —— 应用内部向 Gateway 传递原始敏感值（密码、API Key、Token）的通用载体，
 * 不绑定任何特定算法或业务场景。
 * <p>
 * 安全姿态（刻意与 {@link com.soda.component.domain.types.SensitiveValue} 相反）：
 * <ul>
 *   <li><b>不实现 {@code StringLiteralType}</b>——无 {@code @JsonValue value()}，无可检测属性，序列化输出空对象</li>
 *   <li><b>构造器标 {@code @JsonCreator(mode = DISABLED)}</b>——隐式 delegating 反序列化被显式关闭，任何 JSON 绑定拒绝并抛 {@code JacksonException}</li>
 *   <li>{@code toString()} 全遮蔽输出 {@code SecretValue[***]}</li>
 *   <li>引用级相等（不声明 equals/hashCode）——秘密值不参与值比较</li>
 * </ul>
 * 生命周期为瞬态输入载体：不写入日志、不保留引用、使用后尽快丢弃。
 *
 * @see com.soda.component.domain.gateway.PasswordHasher
 * @see PasswordHash 哈希后的可安全序列化形态
 */
public final class SecretValue implements Type {

    private final String value;

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public SecretValue(String value) {
        ValidateUtils.hasText(value);
        this.value = value;
    }

    /**
     * 原始秘密值。命名刻意避开 {@code value()}，防止被序列化框架自动发现。
     */
    public String rawValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SecretValue[***]";
    }
}