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

@DisplayName("已脱敏真实姓名值对象")
class MaskedChineseNameTest {

    private static final String VALID_MASKED = "张*";
    private static final String VALID_MASKED_2 = "李*";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("单姓脱敏格式创建实例")
        void should_create_when_singleSurname() {
            var mrn = MaskedChineseName.of(VALID_MASKED);
            assertThat(mrn.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("两字前缀脱敏格式已收紧为非法（均匀规则无特例）")
        void should_throw_when_compoundPrefix() {
            assertThatThrownBy(() -> MaskedChineseName.of("欧阳*"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "张三", "*", "张*三", "张", "1*"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> MaskedChineseName.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 单姓原始姓名等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawChineseName() {
            assertThat(MaskedChineseName.from(new ChineseName("张三")))
                    .isEqualTo(MaskedChineseName.of("张*"));
        }

        @Test
        @DisplayName("复姓同样只保留首字（均匀规则）")
        void should_maskCompoundSurnameUniformly_when_rawChineseName() {
            assertThat(MaskedChineseName.from(new ChineseName("欧阳修")).value()).isEqualTo("欧**");
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(MaskedChineseName.of(VALID_MASKED)).isEqualTo(MaskedChineseName.of(VALID_MASKED));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(MaskedChineseName.of(VALID_MASKED)).isNotEqualTo(MaskedChineseName.of(VALID_MASKED_2));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(MaskedChineseName.of(VALID_MASKED)).hasSameHashCodeAs(MaskedChineseName.of(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notEqual_otherMaskedType() {
            assertThat(MaskedChineseName.of(VALID_MASKED)).isNotEqualTo(MaskedEmail.of("t***@example.com"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("record 标准格式")
        void should_toStringRecordFormat() {
            assertThat(MaskedChineseName.of(VALID_MASKED)).hasToString("MaskedChineseName[value=张*]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            String json = mapper().writeValueAsString(MaskedChineseName.of(VALID_MASKED));
            assertThat(json).isEqualTo("\"" + VALID_MASKED + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var mrn = mapper().readValue("\"" + VALID_MASKED + "\"", MaskedChineseName.class);
            assertThat(mrn.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(MaskedChineseName.of(VALID_MASKED), MaskedChineseName.class);
        }
    }
}
