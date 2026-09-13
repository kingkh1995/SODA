package com.soda.user.domain.types;

import com.soda.component.domain.Identifier;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 认证账户标识符密封基类 — 所有 AuthAccountId 统一为 {@link Identifier}{@code <String>}。
 * <p>
 * 编码格式（前缀 + 业务键，如 {@code "P:42"}）单源在 ADR-0007 与本类型 javadoc——单属性 String 字面量，实现
 * {@link StringLiteralType}（{@code @JsonValue} 继承自家族接口，见 ADR-0028；
 * 自描述编码与字面量契约正交）。基类不声明 creator，反序列化入口下放各子类的 {@code of(String)}，
 * 声明类型为基类的 JSON 边界不存在消费者。
 * <p>
 * <b>规范串是派生值</b>（见 dp-conventions §2.1「派生字段」）：每个子类只持有强类型 payload，
 * 其工厂方法在派生规范串形态后经参数传入，前缀与拼接由基类构造器完成（唯一拼写点）。故同一逻辑账户恒有
 * 同一规范串——线形态入口的入参原文只用于解析，永不直接入值（ADR-0007）。
 *
 * @see PasswordAuthAccountId
 * @see SmsAuthAccountId
 * @see EmailAuthAccountId
 * @see SocialAuthAccountId
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public abstract sealed class AuthAccountId implements Identifier<String>, StringLiteralType
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

    /**
     * 认证账户标识符各部分之间的分隔符。
     */
    protected static final String DELIMITER = ":";

    /**
     * 规范串 —— 由 {@link #accountType()} 与子类传入的 payload 规范串形态派生，相等性唯一依据。
     * <p>
     * 派生发生在厂方法：子类字段在 {@code super(...)} 之前尚未赋值，故 payload 规范串由厂方法算出后经参数传入，
     * 基类只负责前缀与拼接（唯一拼写点）。
     */
    @EqualsAndHashCode.Include
    private final String value;

    /**
     * @param payload payload 的规范串形态（如 {@code String.valueOf(userId.value())}），由子类厂方法派生
     */
    protected AuthAccountId(String payload) {
        ValidateUtils.hasText(payload);
        this.value = prefix(accountType()) + payload;
    }

    /**
     * 判别值的规范串形态 —— 规范串的固定前缀，构造渲染与线形态入口的类型守卫同源（{@code of} 以本方法比对入参前缀，
     * 故以 {@code "S:…"} 构造密码账户会被拒绝）。
     */
    protected static String prefix(AuthAccountType type) {
        return type.name() + DELIMITER;
    }

    /**
     * 认证方式判别值 —— 本族的唯一判别来源，恒为 {@link #value()} 的前缀。
     * <p>
     * 公开的目的是身份自描述（与 {@code VerificationRecipient.channel()} 对称）；实体层的判别值另由各账户子类
     * 自我声明，不经 ID 分发——ID 在创建瞬态尚未分配（{@link com.soda.component.domain.Identifiable#getId()} 抛
     * NPE），实体类型不依赖它。
     * <p>
     * 实现约束：覆写必须返回常量、禁读本类字段——基类构造器在子类字段赋值前调用本方法渲染规范串前缀。
     */
    public abstract AuthAccountType accountType();

    @Override
    public final String identifier() {
        return value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[value=" + value + "]";
    }
}
