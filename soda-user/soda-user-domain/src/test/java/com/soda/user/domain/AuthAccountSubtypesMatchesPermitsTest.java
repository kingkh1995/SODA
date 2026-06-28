package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link AuthAccount} 的 {@code permits} 子句与 {@link JsonSubTypes} 列表一致。
 * <p>
 * 新增 {@link AuthAccount} 子类时，Java 编译器强制 {@code permits} 完备性，
 * 但不会检查 {@code @JsonSubTypes}。此测试确保两处列表始终同步。
 */
class AuthAccountSubtypesMatchesPermitsTest {

    @Test
    void jsonSubTypesMatchesPermits() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var jsonSubTypes = AuthAccount.class.getAnnotation(JsonSubTypes.class).value();

        var permittedSet = Set.of(permitted);
        var jsonSubTypeSet = Stream.of(jsonSubTypes)
                .map(JsonSubTypes.Type::value)
                .collect(Collectors.toSet());

        assertEquals(permittedSet, jsonSubTypeSet,
                "@JsonSubTypes 必须包含 permits 子句中声明的所有子类");
    }
}
