package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SmsAuthAccount} 单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>默认策略和类型</li>
 *   <li>createBuilder / restoreBuilder 工厂方法</li>
 *   <li>Jackson 序列化 / 反序列化</li>
 * </ul>
 */
class SmsAuthAccountTest {

    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final SmsAuthAccountId ID = SmsAuthAccountId.from(MOBILE);

    @Test
    void constructor_setsId() {
        var account = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertEquals(ID, account.getId());
    }

    @Test
    void getAuthAccountType_returnsS() {
        var account = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertEquals(AuthAccountType.S, account.getAuthAccountType());
    }

    @Test
    void defaultPolicy_isSixDigitsFiveMinutes() {
        assertEquals(SmsAuthAccount.DEFAULT_POLICY, VerificationCodePolicy.DEFAULT_SMS);
    }

    @Test
    void mobile_returnsFromId() {
        var account = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertEquals(MOBILE, account.getMobile());
    }

    @Test
    void activeTrue_isActive() {
        var account = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertTrue(account.isActive());
    }

    @Test
    void policy_customViaConstructor() {
        var customPolicy = new VerificationCodePolicy(4, java.time.Duration.ofMinutes(1));
        var account = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(customPolicy).build();
        assertEquals(customPolicy, account.getVerificationCodePolicy());
    }

    @Test
    void policy_null_returnsDefault() {
        var account = SmsAuthAccount.createBuilder()
                .mobile(MOBILE)
                .build();
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
    }

    @Test
    void restoreBuilder_restoresAllFields() {
        var account = SmsAuthAccount.restoreBuilder()
                .id(ID)
                .active(Active.FALSE)
                .verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
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
        var same = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        var equal = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        var diffMobile = SmsAuthAccountId.from(new Mobile("13900139000"));
        var diffId = SmsAuthAccount.restoreBuilder().id(diffMobile).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = SmsAuthAccount.restoreBuilder().id(ID).active(Active.TRUE).verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY).build();
        assertTrue(a.toString().contains("SmsAuthAccount@"));
    }
}
