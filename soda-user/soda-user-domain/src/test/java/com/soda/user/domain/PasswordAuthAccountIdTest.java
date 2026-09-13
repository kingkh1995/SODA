package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordAuthAccountId 值对象")
class PasswordAuthAccountIdTest extends ComparableDomainPrimitiveContractTest<PasswordAuthAccountId> {

    @Override
    protected Contract<PasswordAuthAccountId> contract() {
        return new Contract<>(PasswordAuthAccountId.class, () -> PasswordAuthAccountId.of("P:42"),
                "\"P:42\"", "PasswordAuthAccountId[value=P:42]", "\"invalid\"",
                () -> PasswordAuthAccountId.of("P:43"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(UserId) 创建实例带 P: 前缀")
        void should_createWithPrefix_when_fromUserId() {
            var id = PasswordAuthAccountId.from(new UserId(42L));
            assertThat(id.value()).isEqualTo("P:42");
            assertThat(id.userId()).isEqualTo(new UserId(42L));
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            assertThat(PasswordAuthAccountId.from(new UserId(42L)))
                    .isEqualTo(PasswordAuthAccountId.of("P:42"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            assertThat(PasswordAuthAccountId.of("P:42").value()).isEqualTo("P:42");
        }

        @Test
        @DisplayName("authAccountType 返回 P")
        void should_returnP_when_authAccountType() {
            assertThat(PasswordAuthAccountId.from(new UserId(42L)).accountType()).isEqualTo(AuthAccountType.P);
        }
    }

    @Nested
    @DisplayName("路由")
    class Routing {
        @Test
        @DisplayName("JSON 反序列化以本子类为声明类型")
        void should_routeToPasswordSubtype_when_json() throws Exception {
            assertThat(MAPPER.readValue("\"P:42\"", PasswordAuthAccountId.class))
                    .isEqualTo(PasswordAuthAccountId.of("P:42"));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("from(null) 抛出异常")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> PasswordAuthAccountId.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> PasswordAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "P:", "Q:42", "42"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> PasswordAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("标识符")
    class Identity {
        @Test
        @DisplayName("identifier 返回规范化编码值")
        void should_returnCanonicalValue_when_identifier() {
            assertThat(PasswordAuthAccountId.of("P:42").identifier()).isEqualTo("P:42");
        }
    }
}
