package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

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
 *   <li>类型与标识</li>
 *   <li>createBuilder / builder 工厂方法</li>
 *   <li>Jackson 序列化 / 反序列化</li>
 * </ul>
 */
class EmailAuthAccountTest {

    private static final Email EMAIL = new Email("test@example.com");
    private static final EmailAuthAccountId ID = EmailAuthAccountId.from(EMAIL);

    @Test
    void constructor_setsId() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(ID, account.getId());
    }

    @Test
    void getAccountType_returnsE() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(AuthAccountType.E, account.getAccountType());
    }

    @Test
    void email_returnsFromId() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(EMAIL, account.getEmail());
    }

    @Test
    void activeTrue_isActive() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertTrue(account.isActive());
    }

    @Test
    void activeFalse_isInactive() {
        var account = EmailAuthAccount.builder().id(ID).active(Active.FALSE).build();
        assertFalse(account.isActive());
    }

    @Test
    void defaultPolicy_isEightDigitsThirtyMinutes() {
        assertEquals(EmailAuthAccount.DEFAULT_POLICY, VerificationCodePolicy.DEFAULT_EMAIL);
    }

    // ——— factories ———

    @Test
    void createBuilder_setsDefaults() {
        var account = EmailAuthAccount.createBuilder()
                .email(EMAIL)
                .build();
        assertEquals(EmailAuthAccountId.from(EMAIL), account.getId());
        assertTrue(account.isActive());
    }

    @Test
    void builder_restoresAllFields() {
        var account = EmailAuthAccount.builder()
                .id(ID)
                .active(Active.FALSE)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
    }

    // ——— JSON ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var account = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var json = MAPPER.writeValueAsString(account);
        var deserialized = MAPPER.readValue(json, EmailAuthAccount.class);
        assertEquals(account, deserialized);
    }

    @Test
    void jackson_rejectsMissingId() {
        var json = """
                {"active":true}
                """;
        assertThrows(JacksonException.class, () -> MAPPER.readValue(json, EmailAuthAccount.class));
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        var same = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var equal = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var diffEmail = EmailAuthAccountId.from(new Email("other@example.com"));
        var diffId = EmailAuthAccount.builder().id(diffEmail).active(Active.TRUE).build();
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = EmailAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertTrue(a.toString().contains("EmailAuthAccount@"));
    }
}
