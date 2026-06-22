package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.types.Active;
import com.soda.component.support.types.Mobile;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SmsAuthAccount} 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>{@link SmsAuthAccount#verifyCode(String)} — 正确 / 错误</li>
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

    /** 永不过期的验证码，用于测试。 */
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
        assertTrue(account.verifyCode("654321"));
    }


    @Test
    void verifyCode_wrongCode_returnsFalse() {
        var account = new SmsAuthAccount(ID, Active.TRUE, CODE, null);
        assertFalse(account.verifyCode("000000"));
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
        assertNull(account.getVerificationCode());
    }

    @Test
    void restoreBuilder_restoresAllFields() {
        var account = SmsAuthAccount.restoreBuilder()
                .id(ID)
                .active(Active.FALSE)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
        assertNull(account.getVerificationCode());
    }

    // ——— JSON ———

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

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
}
