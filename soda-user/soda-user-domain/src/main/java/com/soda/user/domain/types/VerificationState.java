package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.StateEnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 验证状态枚举。
 * <p>
 * I(Initialized) 已初始化（创建后尚未发送验证码）
 * P(Pending) 待验证（验证码已发送）
 * V(Verified) 已验证——<b>内存瞬态</b>：消费流 verify→use 同事务完成
 * （{@code CredentialChangeDomainService}），V 永不落库
 * U(Used) 已使用（持久化终态；过期是派生判断，不落状态，见 ADR-0011）
 * <p>
 * 终态标记为构造器注入字段（record 风格）：V/U 均为终态（{@link #terminal()}）——
 * 终态不占活跃槽、持久化行不可写（基础设施兜底，ADR-0023）；U 是唯一可落库的终态。
 * {@code terminal()} 与 {@code UserState} 同契约（{@link StateEnumType}）。
 *
 * @see StateEnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum VerificationState implements StateEnumType {

    I("initialized", false),
    P("pending", false),
    V("verified", true),
    U("used", true);

    private final String desc;
    private final boolean terminal;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationState of(String name) {
        return ParseUtils.parseEnum(VerificationState.class, name);
    }
}