package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link AuthAccount} 的 {@code permits} 子句与各子类上的 {@link JsonTypeName} 一致。
 * <p>
 * 新增 {@link AuthAccount} 子类时，Java 编译器强制 {@code permits} 完备性，
 * 但不会检查每个子类是否都有唯一的 {@code @JsonTypeName}。此测试确保这一点。
 */
class AuthAccountSubtypesMatchesPermitsTest {

    @Test
    void allPermittedSubtypesHaveJsonTypeName() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var typeNames = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .collect(Collectors.toSet());

        // 每个 permits 子类都必须有 @JsonTypeName 注解
        assertEquals(Set.of(permitted).size(), typeNames.size(),
                "permits 子句中的每个类必须有 @JsonTypeName 注解");
    }

    @Test
    void jsonTypeNamesAreUnique() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var names = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .map(JsonTypeName::value)
                .collect(Collectors.toList());

        assertEquals(names.size(), Set.copyOf(names).size(),
                "@JsonTypeName 值必须唯一: " + names);
    }

    @Test
    void jsonTypeNamesMatchExpected() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var actual = Arrays.stream(permitted)
                .collect(Collectors.toMap(
                        Class::getSimpleName,
                        cls -> cls.getAnnotation(JsonTypeName.class).value()));

        var expected = Map.of(
                "PasswordAuthAccount", "P",
                "SmsAuthAccount", "S",
                "EmailAuthAccount", "E",
                "SocialAuthAccount", "O");

        assertEquals(expected, actual,
                "@JsonTypeName 值不符合预期");
    }
}
