package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.time.Duration;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EmailAuthAccount} 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>默认策略和类型</li>
 *   <li>createBuilder / builder 工厂方法</li>
 *   <li>Jackson 序列化 / 反序列化</li>
 * </ul>
 */
class EmailAuthAccountTest {

    private static final Email EMAIL = new Email("test@example.com");
    private static final EmailAuthAccountId ID = EmailAuthAccountId.from(EMAIL);

    @Test
    void constructor_setsId() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertEquals(ID, account.getId());
    }

    @Test
    void getAccountType_returnsE() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertEquals(AuthAccountType.E, account.getAccountType());
    }

    @Test
    void defaultPolicy_isEightDigitsThirtyMinutes() {
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, VerificationCodePolicy.DEFAULT_EMAIL);
    }

    @Test
    void email_returnsFromId() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertEquals(EMAIL, account.getEmail());
    }

    @Test
    void activeTrue_isActive() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertTrue(account.isActive());
    }

    @Test
    void activeFalse_isInactive() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.FALSE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertFalse(account.isActive());
    }

    @Test
    void policy_customViaConstructor() {
        var customPolicy = new VerificationCodePolicy(4, Duration.ofMinutes(1));
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(customPolicy).build();
        assertEquals(customPolicy, account.getVerificationCodePolicy());
    }

    @Test
    void constructor_rejectsNullPolicy() {
        // 恢复 / 反序列化路径策略必传（create 路径才允许 null → 默认策略）；构造器校验与 DP 一致（ValidateUtils → IAE）
        assertThrows(IllegalArgumentException.class,
                () -> EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(null).build());
    }

    // ——— factories ———

    @Test
    void createBuilder_setsDefaults() {
        var account = EmailAuthAccount.createBuilder()
                .email(EMAIL)
                .build();
        assertEquals(EmailAuthAccountId.from(EMAIL), account.getId());
        assertTrue(account.isActive());
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, account.getVerificationCodePolicy());
    }

    @Test
    void builder_restoresAllFields() {
        var policy = new VerificationCodePolicy(4, Duration.ofMinutes(1));
        var account = EmailAuthAccount.builder()
                .id(ID)
                .active(Active.FALSE)
                .verificationCodePolicy(policy)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
        assertEquals(policy, account.getVerificationCodePolicy());
    }

    // ——— JSON ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        var json = MAPPER.writeValueAsString(account);
        var deserialized = MAPPER.readValue(json, EmailAuthAccount.class);
        assertEquals(account, deserialized);
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, deserialized.getVerificationCodePolicy());
    }

    @Test
    void jackson_rejectsMissingId() {
        var json = """
                {"active":true,"verificationCodePolicy":{"codeLength":8,"expiry":"PT30M"}}
                """;
        assertThrows(JacksonException.class, () -> MAPPER.readValue(json, EmailAuthAccount.class));
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        var same = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        var equal = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        var diffEmail = EmailAuthAccountId.from(new Email("other@example.com"));
        var diffId = EmailAuthAccount.builder().id(diffEmail).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = EmailAuthAccount.builder().id(ID).active(Active.TRUE).verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY).build();
        assertTrue(a.toString().contains("EmailAuthAccount@"));
    }
}
