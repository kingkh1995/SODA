package com.soda.user.domain;

import com.soda.component.support.types.LongId;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordAuthAccountIdTest {

    @Test void from_createsWithPrefix() {
        var id = PasswordAuthAccountId.from(new UserId(42L));
        assertEquals("P:42", id.value());
        assertEquals(new UserId(42L), id.userId());
    }
    @Test void from_equivalentToValueOf() {
        assertEquals(PasswordAuthAccountId.from(new UserId(42L)), PasswordAuthAccountId.of("P:42"));
    }
    @Test void from_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordAuthAccountId.from(null));
    }
    @Test void valueOf_string_creates() {
        assertEquals("P:42", PasswordAuthAccountId.of("P:42").value());
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "P:", "Q:42", "42"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> PasswordAuthAccountId.of(invalid));
    }
    @Test void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordAuthAccountId.of(null));
    }
    @Test void identifier_returnsString() {
        assertEquals("P:42", PasswordAuthAccountId.from(new UserId(42L)).identifier());
    }
    @Test void type_returnsP() {
        assertEquals(AuthAccountType.P, PasswordAuthAccountId.ACCOUNT_TYPE);
    }
    @Test void equal_whenSameValue() {
        assertEquals(
                PasswordAuthAccountId.from(new UserId(1L)),
                PasswordAuthAccountId.from(new UserId(1L)));
    }
    @Test void notEqual_whenDifferentValue() {
        assertNotEquals(
                PasswordAuthAccountId.from(new UserId(1L)),
                PasswordAuthAccountId.from(new UserId(2L)));
    }
    @Test void compareTo_delegatesToStringCompare() {
        var a = PasswordAuthAccountId.from(new UserId(1L));
        var b = PasswordAuthAccountId.from(new UserId(2L));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
    @Test void jackson_serializeDeserialize() throws Exception {
        var original = PasswordAuthAccountId.from(new UserId(42L));
        var json = MAPPER.writeValueAsString(original);
        assertEquals("\"P:42\"", json);
        var restored = MAPPER.readValue(json, PasswordAuthAccountId.class);
        assertEquals(original, restored);
    }
    @Test void toLongId_convertsToLongId() {
        assertEquals(new LongId(42L), PasswordAuthAccountId.from(new UserId(42L)).toLongId());
    }
    @Test void toLongId_roundTripProducesSameValue() {
        var id = PasswordAuthAccountId.from(new UserId(42L));
        assertEquals(42L, id.toLongId().value());
    }
}
