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
 * 终态以覆写 {@code terminal()} 标记：仅 U 覆写返回 true——
 * 终态不占活跃槽、持久化行不可写（基础设施兜底，ADR-0023）。V 为<b>内存瞬态</b>
 * （verify→use 同事务完成，永不落库）——不落库故不涉槽位释放，非终态
 * （见 ADR-0026 修订注记）。
 * {@code terminal()} 与 {@code UserState} 同契约（{@link StateEnumType}）。
 *
 * @see StateEnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum VerificationState implements StateEnumType {

    I("initialized"),
    P("pending"),
    V("verified"),
    U("used") {
        @Override
        public boolean terminal() {
            return true;
        }
    };

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationState of(String name) {
        return ParseUtils.parseEnum(VerificationState.class, name);
    }
}