package com.soda.user.domain;

import com.soda.component.support.types.Active;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.SocialType;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
        var account = new SocialAuthAccount(ID, Active.TRUE);
        assertEquals(ID, account.getId());
    }

    @Test
    void getAuthAccountType_returnsO() {
        var account = new SocialAuthAccount(ID, Active.TRUE);
        assertEquals(AuthAccountType.O, account.getAuthAccountType());
    }

    @Test
    void socialType_returnsFromId() {
        var account = new SocialAuthAccount(ID, Active.TRUE);
        assertEquals(SocialType.GE, account.getSocialType());
    }

    @Test
    void openId_returnsFromId() {
        var account = new SocialAuthAccount(ID, Active.TRUE);
        assertEquals("open123", account.getOpenId());
    }

    @Test
    void activeTrue_isActive() {
        var account = new SocialAuthAccount(ID, Active.TRUE);
        assertTrue(account.isActive());
    }

    @Test
    void activeFalse_isInactive() {
        var account = new SocialAuthAccount(ID, Active.FALSE);
        assertFalse(account.isActive());
    }

    @Test
    void equal_whenSameId() {
        var a = new SocialAuthAccount(ID, Active.TRUE);
        var b = new SocialAuthAccount(ID, Active.TRUE);
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
    void restoreBuilder_restoresAllFields() {
        var account = SocialAuthAccount.restoreBuilder()
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
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAuthAccountType(), restored.getAuthAccountType());
        assertEquals(original.isActive(), restored.isActive());
    }

    // ——— identity ———

    @Test
    void equals_byFields() {
        // 添加 @EqualsAndHashCode(callSuper = true) 后实体使用字段相等
        var same = new SocialAuthAccount(ID, Active.TRUE);
        var equal = new SocialAuthAccount(ID, Active.TRUE);
        var diffId = new SocialAuthAccount(SocialAuthAccountId.from(SocialType.GE, "otherOpen"), Active.TRUE);
        assertEquals(same, equal, "相同字段应相等");
        assertNotEquals(same, diffId, "不同 ID 不应相等");
    }

    @Test
    void toString_containsClassName() {
        var a = new SocialAuthAccount(ID, Active.TRUE);
        assertTrue(a.toString().contains("SocialAuthAccount@"));
    }
}
