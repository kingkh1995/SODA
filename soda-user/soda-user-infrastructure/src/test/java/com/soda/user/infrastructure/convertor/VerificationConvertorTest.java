package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link VerificationConvertor} 转换单测（2026-08-16 source/recipient 双概念重写，见 ADR-0026）。
 * <p>
 * {@code toPersistence} 全量构造（创建路径 INSERT 的数据来源，ADR-0024）——重点验证：
 * subject 裸键、channel/target 双列（2026-08-16 修订，见 ADR-0026 注记）、active_key 恒设
 * source.compositeKey()（终态清 NULL 收敛进 gateway.save）、restore 双参工厂。
 */
@DisplayName("VerificationConvertor 转换")
class VerificationConvertorTest {

    private static final VerificationSource UCC_SOURCE = VerificationSource.of("UCC", "1");
    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final Email EMAIL = new Email("user@test.com");
    private static final Instant EXPIRE_AT = Instant.parse("2026-08-15T12:00:00Z");

    private static Verification uccVerification(VerificationState state, Instant expireAt) {
        return Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(state)
                .code(new VerificationCode("123456", expireAt))
                .recipient(new SmsRecipient(MOBILE))
                .build();
    }

    @Test
    @DisplayName("VerificationSource.compositeKey：scene + subject 裸键（active_key 单一事实源）")
    void should_composeKey() {
        assertThat(UCC_SOURCE.compositeKey()).isEqualTo("UCC:1");
        assertThat(VerificationSource.of("URG", MOBILE.value()).compositeKey())
                .isEqualTo("URG:" + MOBILE.value());
    }

    @Test
    @DisplayName("toPersistence：active_key 恒设 source.compositeKey()（终态清 NULL 收敛进 gateway.save，见 ADR-0026 注记）")
    void should_alwaysSetActiveKey() {
        var initialized = uccVerification(VerificationState.I, EXPIRE_AT);
        var pending = uccVerification(VerificationState.P, EXPIRE_AT);
        var verified = uccVerification(VerificationState.V, EXPIRE_AT);
        var used = uccVerification(VerificationState.U, EXPIRE_AT);

        assertThat(VerificationConvertor.toPersistence(initialized).getActiveKey())
                .isEqualTo("UCC:1");
        assertThat(VerificationConvertor.toPersistence(pending).getActiveKey())
                .isEqualTo("UCC:1");
        assertThat(VerificationConvertor.toPersistence(verified).getActiveKey())
                .isEqualTo("UCC:1");
        assertThat(VerificationConvertor.toPersistence(used).getActiveKey())
                .isEqualTo("UCC:1");
    }

    @Test
    @DisplayName("toPersistence → toDomain 往返（SmsRecipient）：全字段同步，channel/target 双列还原")
    void should_roundTrip() {
        var verification = uccVerification(VerificationState.I, EXPIRE_AT);

        var po = VerificationConvertor.toPersistence(verification);
        assertThat(po.getId()).isEqualTo(verification.getId().value());
        assertThat(po.getSubject()).isEqualTo("1");
        assertThat(po.getScene()).isEqualTo("UCC");
        assertThat(po.getState()).isEqualTo("I");
        assertThat(po.getChannel()).isEqualTo("S");
        assertThat(po.getTarget()).isEqualTo(MOBILE.value());
        assertThat(po.getCode()).isEqualTo("123456");
        assertThat(po.getExpireAt()).isEqualTo(EXPIRE_AT);

        Verification restored = VerificationConvertor.toDomain(po);
        assertThat(restored.getId()).isEqualTo(verification.getId());
        assertThat(restored.getSource()).isEqualTo(UCC_SOURCE);
        assertThat(restored.getState()).isEqualTo(VerificationState.I);
        assertThat(restored.getCode().code()).isEqualTo("123456");
        assertThat(restored.getCode().expireAt()).isEqualTo(EXPIRE_AT);
        assertThat(restored.getRecipient()).isEqualTo(new SmsRecipient(MOBILE));
    }

    @Test
    @DisplayName("toPersistence → toDomain 往返（EmailRecipient）：channel/target 双列还原 Email")
    void should_roundTripEmailRecipient() {
        var verification = Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(VerificationState.P)
                .code(new VerificationCode("ABCDEF12", EXPIRE_AT))
                .recipient(new EmailRecipient(EMAIL))
                .build();

        var po = VerificationConvertor.toPersistence(verification);
        assertThat(po.getChannel()).isEqualTo("E");
        assertThat(po.getTarget()).isEqualTo(EMAIL.value());

        Verification restored = VerificationConvertor.toDomain(po);
        assertThat(restored.getRecipient()).isEqualTo(new EmailRecipient(EMAIL));
        assertThat(restored.getSource()).isEqualTo(UCC_SOURCE);
    }

    @Test
    @DisplayName("restore 未知 channel：IAE 快速失败（脏数据）")
    void should_rejectUnknownChannel() {
        var po = VerificationConvertor.toPersistence(uccVerification(VerificationState.I, EXPIRE_AT));
        po.setChannel("X");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> VerificationConvertor.toDomain(po));
    }
}
