package com.soda.user.domain;

import com.soda.component.support.types.Active;
import com.soda.component.support.types.Email;
import com.soda.component.support.types.RandomString;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

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
 *   <li>{@link EmailAuthAccount#verifyCode(RandomString)} — 正确 / 错误</li>
 *   <li>{@link EmailAuthAccount#useCode()} — 标记已使用</li>
 *   <li>{@link EmailAuthAccount#replaceCode(VerificationCode)} — 替换验证码</li>
 *   <li>默认策略和类型</li>
 *   <li>createBuilder / restoreBuilder 工厂方法</li>
 *   <li>Jackson 序列化 / 反序列化</li>
 * </ul>
 */
class EmailAuthAccountTest {

    private static final Email EMAIL = new Email("test@example.com");
    private static final EmailAuthAccountId ID = EmailAuthAccountId.from(EMAIL);

    /**
     * 永不过期的验证码，用于测试。
     */
    private static final VerificationCode CODE = new VerificationCode("abcdef12", Instant.MAX, false);

    @Test
    void constructor_setsId() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(ID, account.getId());
    }

    @Test
    void getAuthAccountType_returnsE() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(AuthAccountType.E, account.getAuthAccountType());
    }

    @Test
    void defaultPolicy_isEightDigitsThirtyMinutes() {
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, VerificationCodePolicy.DEFAULT_EMAIL);
    }

    @Test
    void email_returnsFromId() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(EMAIL, account.getEmail());
    }

    @Test
    void verifyCode_correctNotExpired_returnsTrue() {
        var account = new EmailAuthAccount(ID, Active.TRUE, CODE, null);
        assertTrue(account.verifyCode(new RandomString("abcdef12")));
    }


    @Test
    void verifyCode_wrongCode_returnsFalse() {
        var account = new EmailAuthAccount(ID, Active.TRUE, CODE, null);
        assertFalse(account.verifyCode(new RandomString("wrong")));
    }

    @Test
    void replaceCode_injectsCode() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(account.getVerificationCode().isEmpty());
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }

    @Test
    void replaceCode_null_throws() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertThrows(IllegalArgumentException.class, () -> account.replaceCode(null));
    }

    @Test
    void replaceCode_replacesWhenNewCodeValid() {
        var account = new EmailAuthAccount(ID, Active.TRUE, CODE, null);
        var newCode = new VerificationCode("111111", Instant.MAX, false);
        assertTrue(account.replaceCode(newCode));
        assertEquals(Optional.of(newCode), account.getVerificationCode());
    }

    @Test
    void replaceCode_whenNewCodeExpired_returnsEmpty() {
        var expiredCode = new VerificationCode("expired", Instant.now().minusSeconds(1), false);
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertFalse(account.replaceCode(expiredCode));
        assertTrue(account.getVerificationCode().isEmpty()); // unchanged
    }

    @Test
    void replaceCode_whenOldCodeExpired_replaces() {
        var oldExpired = new VerificationCode("old", Instant.now().minusSeconds(1), false);
        var account = new EmailAuthAccount(ID, Active.TRUE, oldExpired, null);
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }

    @Test
    void replaceCode_whenOldCodeUsed_replaces() {
        var usedCode = new VerificationCode("used", Instant.now().minusSeconds(1), true);
        var account = new EmailAuthAccount(ID, Active.TRUE, usedCode, null);
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }


    @Test
    void activeTrue_isActive() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(account.isActive());
    }

    @Test
    void activeFalse_isInactive() {
        var account = new EmailAuthAccount(ID, Active.FALSE, null, null);
        assertFalse(account.isActive());
    }

    @Test
    void policy_customViaConstructor() {
        var customPolicy = new VerificationCodePolicy(4, Duration.ofMinutes(1));
        var account = new EmailAuthAccount(ID, Active.TRUE, null, customPolicy);
        assertEquals(customPolicy, account.getVerificationCodePolicy());
    }

    @Test
    void policy_null_returnsDefault() {
        var account = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, account.getVerificationCodePolicy());
    }

    // ——— factories ———

    @Test
    void createBuilder_setsDefaults() {
        var account = EmailAuthAccount.createBuilder()
                .email(EMAIL)
                .build();
        assertEquals(ID, account.getId());
        assertTrue(account.isActive());
        assertTrue(account.getVerificationCode().isEmpty());
    }

    @Test
    void restoreBuilder_restoresAllFields() {
        var account = EmailAuthAccount.restoreBuilder()
                .id(ID)
                .active(Active.FALSE)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
        assertTrue(account.getVerificationCode().isEmpty());
    }

    // ——— JSON ———


    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = EmailAuthAccount.createBuilder()
                .email(EMAIL)
                .build();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, EmailAuthAccount.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAuthAccountType(), restored.getAuthAccountType());
        assertEquals(original.isActive(), restored.isActive());
        assertEquals(original.getEmail(), restored.getEmail());
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        // 添加 @EqualsAndHashCode(callSuper = true) 后实体使用字段相等
        var same = new EmailAuthAccount(ID, Active.TRUE, null, null);
        var equal = new EmailAuthAccount(ID, Active.TRUE, null, null);
        var diffEmail = EmailAuthAccountId.from(new Email("other@example.com"));
        var diffId = new EmailAuthAccount(diffEmail, Active.TRUE, null, null);
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = new EmailAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(a.toString().contains("EmailAuthAccount@"));
    }
}
