package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("已脱敏真实姓名值对象")
class MaskedChineseNameTest extends DomainPrimitiveContractTest<MaskedChineseName> {

    /**
     * maskOf(ChineseName.of("张三")) = "张*" — 2 字原始名脱敏输出（保留首字 + 1 个 `*`）
     */
    private static final String VALID_MASKED = "张*";

    @Override
    protected Contract<MaskedChineseName> contract() {
        return new Contract<>(MaskedChineseName.class, () -> new MaskedChineseName(VALID_MASKED),
                "\"" + VALID_MASKED + "\"", "MaskedChineseName[value=" + VALID_MASKED + "]",
                "\"张三\"", () -> new MaskedChineseName("李*"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("单姓脱敏格式（首字 + 1 个 `*`）创建实例")
        void should_create_when_singleSurname() {
            var mcn = new MaskedChineseName(VALID_MASKED);
            assertThat(mcn.value()).isEqualTo(VALID_MASKED);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "张", "张**", "李*明", "a*", "1*", "**", "*张", "张*张", "张**"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedChineseName(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始中文姓名等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawChineseName() {
            assertThat(MaskedChineseName.from(ChineseName.of("张三")))
                    .isEqualTo(new MaskedChineseName(VALID_MASKED));
        }
    }
}
