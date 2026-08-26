package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link AuthAccount} 的 {@code permits} 子句与各子类上的 {@link JsonTypeName} 一致。
 * 新增 {@link AuthAccount} 子类时，Java 编译器强制 {@code permits} 完备性，
 * 但不会检查每个子类是否都有唯一的 {@code @JsonTypeName}。此测试确保这一点。
 *
 * @see docs/doc-conventions.md §7（合规基准）
 */
@DisplayName("AuthAccount 密封子类与 @JsonTypeName 一致性")
class AuthAccountSubtypesMatchesPermitsTest {

    @Test
    @DisplayName("每个 permits 子类都标注 @JsonTypeName")
    void should_annotateJsonTypeName_when_everyPermittedSubtype() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var typeNames = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .collect(Collectors.toSet());

        assertThat(typeNames)
                .as("permits 子句中的每个类必须有 @JsonTypeName 注解")
                .hasSize(permitted.length);
    }

    @Test
    @DisplayName("@JsonTypeName 值在子类间唯一")
    void should_haveUniqueJsonTypeName_when_subtypesAnnotated() {
        var permitted = AuthAccount.class.getPermittedSubclasses();
        var names = Arrays.stream(permitted)
                .map(cls -> cls.getAnnotation(JsonTypeName.class))
                .map(JsonTypeName::value)
                .collect(Collectors.toList());

        assertThat(Set.copyOf(names))
                .as("@JsonTypeName 值必须唯一: " + names)
                .hasSameSizeAs(names);
    }

    @Test
    @DisplayName("@JsonTypeName 值与预期判别符一致")
    void should_matchExpectedJsonTypeName_when_mappingChecked() {
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

        assertThat(actual)
                .as("@JsonTypeName 值不符合预期")
                .isEqualTo(expected);
    }
}
