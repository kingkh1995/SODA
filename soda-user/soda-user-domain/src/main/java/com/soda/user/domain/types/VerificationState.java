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
 * I（Initialized）已创建，验证码尚未投递
 * P（Pending）已投递，待验证
 * V（Verified）已验证——<b>内存瞬态</b>：消费流 verify→use 同事务完成，永不落库（见 ADR-0026）
 * U（Used）已使用——持久化终态
 * <p>
 * 过期是派生判断，不落状态（见 ADR-0011）。终态以覆写 {@code terminal()} 标记（仅 U）：
 * 终态不占活跃槽、持久化行不可写（基础设施兜底，见 ADR-0023）；V 不落库故不涉槽位释放，
 * 非终态。{@code terminal()} 与 {@code UserState} 同契约（{@link StateEnumType}）。
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