package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SmsAuthAccountId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsAuthAccountId 值对象")
class SmsAuthAccountIdTest extends ComparableDomainPrimitiveContractTest<SmsAuthAccountId> {

    private static final Mobile VALID_MOBILE = Mobile.of("13800138000");

    @Override
    protected Contract<SmsAuthAccountId> contract() {
        return new Contract<>(SmsAuthAccountId.class, () -> SmsAuthAccountId.of("S:13800138000"),
                "\"S:13800138000\"", "SmsAuthAccountId[value=S:13800138000]", "\"invalid\"",
                () -> SmsAuthAccountId.of("S:13900139000"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(Mobile) 创建实例带 S: 前缀")
        void should_createWithPrefix_when_fromMobile() {
            var id = SmsAuthAccountId.from(VALID_MOBILE);
            assertThat(id.value()).isEqualTo("S:13800138000");
            assertThat(id.mobile()).isEqualTo(VALID_MOBILE);
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            assertThat(SmsAuthAccountId.from(VALID_MOBILE))
                    .isEqualTo(SmsAuthAccountId.of("S:13800138000"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            assertThat(SmsAuthAccountId.of("S:13800138000").value()).isEqualTo("S:13800138000");
        }

        @Test
        @DisplayName("authAccountType 返回 S")
        void should_returnS_when_authAccountType() {
            assertThat(SmsAuthAccountId.of("S:13800138000").accountType()).isEqualTo(AuthAccountType.S);
        }
    }

    @Nested
    @DisplayName("路由")
    class Routing {
        @Test
        @DisplayName("JSON 反序列化以本子类为声明类型")
        void should_routeToSmsSubtype_when_json() throws Exception {
            assertThat(MAPPER.readValue("\"S:13800138000\"", SmsAuthAccountId.class))
                    .isEqualTo(SmsAuthAccountId.of("S:13800138000"));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("from(null) 抛出异常")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> SmsAuthAccountId.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> SmsAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "S:", "sms:13800138000", "not-a-mobile"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> SmsAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("标识符")
    class Identity {
        @Test
        @DisplayName("identifier 返回规范化编码值")
        void should_returnCanonicalValue_when_identifier() {
            assertThat(SmsAuthAccountId.of("S:13800138000").identifier()).isEqualTo("S:13800138000");
        }
    }
}
