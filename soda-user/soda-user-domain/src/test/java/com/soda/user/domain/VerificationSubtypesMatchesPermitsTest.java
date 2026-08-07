package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 {@link Verification} 的 {@code permits} 子句与各子类上的 {@link JsonTypeName} 一致，
 * 且判别值与 {@link com.soda.user.domain.types.VerificationChannel} 枚举 name 一一对应。
 * <p>
 * 新增 {@link Verification} 子类时，Java 编译器强制 {@code permits} 完备性，
 * 但不会检查每个子类是否都有唯一的 {@code @JsonTypeName}。此测试确保这一点。
 */
class VerificationSubtypesMatchesPermitsTest {

    @Test
    void allPermittedSubtypesHaveJsonTypeName() {
        var permitted = Verification.class.getPermittedSubclasses();
        var typeNames = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .collect(Collectors.toSet());

        assertEquals(Set.of(permitted).size(), typeNames.size(),
                "permits 子句中的每个类必须有 @JsonTypeName 注解");
    }

    @Test
    void jsonTypeNamesAreUnique() {
        var permitted = Verification.class.getPermittedSubclasses();
        var names = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .map(JsonTypeName::value)
                .collect(Collectors.toList());

        assertEquals(names.size(), Set.copyOf(names).size(),
                "@JsonTypeName 值必须唯一: " + names);
    }

    @Test
    void jsonTypeNamesMatchChannelNames() {
        var permitted = Verification.class.getPermittedSubclasses();
        var actual = Arrays.stream(permitted)
                .collect(Collectors.toMap(
                        Class::getSimpleName,
                        cls -> cls.getAnnotation(JsonTypeName.class).value()));

        var expected = Map.of(
                "SmsVerification", "S",
                "EmailVerification", "E");

        assertEquals(expected, actual,
                "@JsonTypeName 值与 VerificationChannel 枚举 name 不一致");
    }
}
