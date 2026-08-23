package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.component.domain.testutil.JacksonTestUtil.mapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("已脱敏邮箱值对象")
class MaskedEmailTest {

    private static final String VALID_MASKED = "t***@example.com";
    private static final String VALID_MASKED_2 = "a***@domain.org";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var me = MaskedEmail.of(VALID_MASKED);
            assertThat(me.value()).isEqualTo(VALID_MASKED);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "test@example.com", "te***@example.com", "***@example.com", "t***@example"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> MaskedEmail.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始邮箱等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawEmail() {
            assertThat(MaskedEmail.from(new Email("test@example.com")))
                    .isEqualTo(MaskedEmail.of("t***@example.com"));
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(MaskedEmail.of(VALID_MASKED)).isEqualTo(MaskedEmail.of(VALID_MASKED));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(MaskedEmail.of(VALID_MASKED)).isNotEqualTo(MaskedEmail.of(VALID_MASKED_2));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(MaskedEmail.of(VALID_MASKED)).hasSameHashCodeAs(MaskedEmail.of(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notEqual_otherMaskedType() {
            assertThat(MaskedEmail.of(VALID_MASKED)).isNotEqualTo(MaskedChineseName.of("张*"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("record 标准格式")
        void should_toStringRecordFormat() {
            assertThat(MaskedEmail.of(VALID_MASKED)).hasToString("MaskedEmail[value=t***@example.com]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            String json = mapper().writeValueAsString(MaskedEmail.of(VALID_MASKED));
            assertThat(json).isEqualTo("\"" + VALID_MASKED + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var me = mapper().readValue("\"" + VALID_MASKED + "\"", MaskedEmail.class);
            assertThat(me.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(MaskedEmail.of(VALID_MASKED), MaskedEmail.class);
        }
    }
}
