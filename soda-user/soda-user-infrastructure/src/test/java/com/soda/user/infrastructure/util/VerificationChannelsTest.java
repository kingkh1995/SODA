package com.soda.user.infrastructure.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.VerificationChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 锁定基础设施推导与领域判别值的一致（ADR-0016 决策 7）：
 * class → {@link VerificationChannel} → {@code @JsonTypeName} 三方一致。
 * <p>
 * 新增 {@link Verification} 子类型时，本测试与 domain 的
 * {@code VerificationSubtypesMatchesPermitsTest} 共同强制四处同步更新：
 * permits 子句、{@code @JsonTypeName}、枚举常量、infra 推导。
 */
class VerificationChannelsTest {

    @SuppressWarnings("unchecked")
    private static Class<? extends Verification<?>> asVerificationType(Class<?> cls) {
        return (Class<? extends Verification<?>>) cls;
    }

    private static void assertConsistent(Class<? extends Verification<?>> cls,
                                         VerificationChannel channel,
                                         String jsonTypeName) {
        assertEquals(channel, VerificationChannels.of(cls), "class→enum 推导");
        assertEquals(jsonTypeName, cls.getAnnotation(JsonTypeName.class).value(), "@JsonTypeName 值");
        assertEquals(channel.name(), jsonTypeName, "enum name 与 JSON 判别串");
    }

    @Test
    void derivationMatchesEnumAndJsonTypeName() {
        assertConsistent(SmsVerification.class, VerificationChannel.S, "S");
        assertConsistent(EmailVerification.class, VerificationChannel.E, "E");
    }

    @Test
    void derivationCoversEveryPermittedSubclass() {
        for (var cls : Verification.class.getPermittedSubclasses()) {
            VerificationChannels.of(asVerificationType(cls)); // 未知类型会抛异常——permits 子类必须可推导
        }
    }

    @Test
    void unknownTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> VerificationChannels.of(asVerificationType(Verification.class)));
    }
}
