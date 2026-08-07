package com.soda.user.domain;

import com.soda.component.domain.types.Active;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.SocialType;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SocialAuthAccount} 单元测试。
 * <p>
 * 验证构造、类型、社交平台属性、工厂方法、Jackson 序列化。
 */
class SocialAuthAccountTest {

    private static final SocialAuthAccountId ID = SocialAuthAccountId.from(SocialType.GE, "open123");

    @Test
    void constructor_setsId() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(ID, account.getId());
    }

    @Test
    void getAccountType_returnsO() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(AuthAccountType.O, account.getAccountType());
    }

    @Test
    void socialType_returnsFromId() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals(SocialType.GE, account.getSocialType());
    }

    @Test
    void openId_returnsFromId() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertEquals("open123", account.getOpenId());
    }

    @Test
    void activeTrue_isActive() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertTrue(account.isActive());
    }

    @Test
    void activeFalse_isInactive() {
        var account = SocialAuthAccount.builder().id(ID).active(Active.FALSE).build();
        assertFalse(account.isActive());
    }

    @Test
    void equal_whenSameId() {
        var a = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var b = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertNotSame(a, b);
        assertEquals(ID, a.getId());
        assertEquals(ID, b.getId());
        assertEquals(a.getId(), b.getId());
    }


    @Test
    void notEqual_whenDifferentPlatform() {
        var gitee = SocialAuthAccountId.from(SocialType.GE, "open123");
        var dingtalk = SocialAuthAccountId.from(SocialType.DT, "open123");
        assertNotEquals(gitee, dingtalk);
    }

    @Test
    void notEqual_whenDifferentOpenId() {
        var id1 = SocialAuthAccountId.from(SocialType.GE, "open123");
        var id2 = SocialAuthAccountId.from(SocialType.GE, "open456");
        assertNotEquals(id1, id2);
    }

    // ——— factories ———

    @Test
    void createBuilder_setsDefaults() {
        var account = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("open123")
                .build();
        assertEquals(ID, account.getId());
        assertTrue(account.isActive());
    }

    @Test
    void builder_restoresAllFields() {
        var account = SocialAuthAccount.builder()
                .id(ID)
                .active(Active.FALSE)
                .build();
        assertEquals(ID, account.getId());
        assertFalse(account.isActive());
    }

    // ——— JSON ———


    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("open123")
                .build();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, SocialAuthAccount.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_rejectsMissingId() {
        var json = """
                {"active":true}
                """;
        assertThrows(JacksonException.class, () -> MAPPER.readValue(json, SocialAuthAccount.class));
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        // 添加 @EqualsAndHashCode(callSuper = true) 后实体使用字段相等
        var same = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var equal = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        var diffId = SocialAuthAccount.builder().id(SocialAuthAccountId.from(SocialType.GE, "otherOpen")).active(Active.TRUE).build();
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = SocialAuthAccount.builder().id(ID).active(Active.TRUE).build();
        assertTrue(a.toString().contains("SocialAuthAccount@"));
    }
}
