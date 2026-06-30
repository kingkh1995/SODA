package com.soda.user.domain;

import com.soda.component.support.types.Active;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.types.RandomString;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SmsAuthAccount} 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>{@link SmsAuthAccount#verifyCode(RandomString)} — 正确 / 错误</li>
 *   <li>{@link SmsAuthAccount#useCode()} — 标记已使用</li>
 *   <li>{@link SmsAuthAccount#replaceCode(VerificationCode)} — 替换验证码</li>
 *   <li>默认策略和类型</li>
 *   <li>createBuilder / restoreBuilder 工厂方法</li>
 *   <li>Jackson 序列化 / 反序列化</li>
 * </ul>
 */
class SmsAuthAccountTest {

    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final SmsAuthAccountId ID = SmsAuthAccountId.from(MOBILE);

    /**
     * 永不过期的验证码，用于测试。
     */
    private static final VerificationCode CODE = new VerificationCode("654321", Instant.MAX, false);

    @Test
    void constructor_setsId() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(ID, account.getId());
    }

    @Test
    void getAuthAccountType_returnsS() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(AuthAccountType.S, account.getAuthAccountType());
    }

    @Test
    void defaultPolicy_isSixDigitsFiveMinutes() {
        assertEquals(SmsAuthAccount.DEFAULT_POLICY, VerificationCodePolicy.DEFAULT_SMS);
    }

    @Test
    void mobile_returnsFromId() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(MOBILE, account.getMobile());
    }

    @Test
    void verifyCode_correctNotExpired_returnsTrue() {
        var account = new SmsAuthAccount(ID, Active.TRUE, CODE, null);
        assertTrue(account.verifyCode(new RandomString("654321")));
    }


    @Test
    void verifyCode_wrongCode_returnsFalse() {
        var account = new SmsAuthAccount(ID, Active.TRUE, CODE, null);
        assertFalse(account.verifyCode(new RandomString("000000")));
    }

    @Test
    void replaceCode_injectsCode() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(account.getVerificationCode().isEmpty());
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }

    @Test
    void replaceCode_null_throws() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertThrows(IllegalArgumentException.class, () -> account.replaceCode(null));
    }

    @Test
    void replaceCode_replacesWhenNewCodeValid() {
        var account = new SmsAuthAccount(ID, Active.TRUE, CODE, null);
        var newCode = new VerificationCode("111111", Instant.MAX, false);
        assertTrue(account.replaceCode(newCode));
        assertEquals(Optional.of(newCode), account.getVerificationCode());
    }

    @Test
    void replaceCode_whenNewCodeExpired_returnsEmpty() {
        var expiredCode = new VerificationCode("expired", Instant.now().minusSeconds(1), false);
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertFalse(account.replaceCode(expiredCode));
        assertTrue(account.getVerificationCode().isEmpty()); // unchanged
    }

    @Test
    void replaceCode_whenOldCodeExpired_replaces() {
        var oldExpired = new VerificationCode("old", Instant.now().minusSeconds(1), false);
        var account = new SmsAuthAccount(ID, Active.TRUE, oldExpired, null);
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }

    @Test
    void replaceCode_whenOldCodeUsed_replaces() {
        var usedCode = new VerificationCode("used", Instant.now().minusSeconds(1), true);
        var account = new SmsAuthAccount(ID, Active.TRUE, usedCode, null);
        assertTrue(account.replaceCode(CODE));
        assertEquals(Optional.of(CODE), account.getVerificationCode());
    }


    @Test
    void activeTrue_isActive() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(account.isActive());
    }

    @Test
    void policy_customViaConstructor() {
        var customPolicy = new VerificationCodePolicy(4, java.time.Duration.ofMinutes(1));
        var account = new SmsAuthAccount(ID, Active.TRUE, null, customPolicy);
        assertEquals(customPolicy, account.getVerificationCodePolicy());
    }

    @Test
    void policy_null_returnsDefault() {
        var account = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertEquals(SmsAuthAccount.DEFAULT_POLICY, account.getVerificationCodePolicy());
    }

    // ——— factories ———

    @Test
    void createBuilder_setsDefaults() {
        var account = SmsAuthAccount.createBuilder()
                .mobile(MOBILE)
                .build();
        assertEquals(ID, account.getId());
        assertTrue(account.isActive());
        assertTrue(account.getVerificationCode().isEmpty());
    }

    @Test
    void restoreBuilder_restoresAllFields() {
        var account = SmsAuthAccount.restoreBuilder()
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
        var original = SmsAuthAccount.createBuilder()
                .mobile(MOBILE)
                .build();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, SmsAuthAccount.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAuthAccountType(), restored.getAuthAccountType());
        assertEquals(original.isActive(), restored.isActive());
        assertEquals(original.getMobile(), restored.getMobile());
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        // 添加 @EqualsAndHashCode(callSuper = true) 后实体使用字段相等
        var same = new SmsAuthAccount(ID, Active.TRUE, null, null);
        var equal = new SmsAuthAccount(ID, Active.TRUE, null, null);
        var diffMobile = SmsAuthAccountId.from(new Mobile("13900139000"));
        var diffId = new SmsAuthAccount(diffMobile, Active.TRUE, null, null);
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = new SmsAuthAccount(ID, Active.TRUE, null, null);
        assertTrue(a.toString().contains("SmsAuthAccount@"));
    }
}
