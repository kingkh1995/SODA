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

@DisplayName("已脱敏邮箱值对象")
class MaskedEmailTest extends DomainPrimitiveContractTest<MaskedEmail> {

    private static final String VALID_MASKED = "t***@example.com";

    @Override
    protected Contract<MaskedEmail> contract() {
        return new Contract<>(MaskedEmail.class, () -> new MaskedEmail(VALID_MASKED),
                "\"" + VALID_MASKED + "\"", "MaskedEmail[value=" + VALID_MASKED + "]",
                "\"test@example.com\"", () -> new MaskedEmail("a***@domain.org"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var me = new MaskedEmail(VALID_MASKED);
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
            assertThatThrownBy(() -> new MaskedEmail(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始邮箱等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawEmail() {
            assertThat(MaskedEmail.from(Email.of("test@example.com")))
                    .isEqualTo(new MaskedEmail("t***@example.com"));
        }
    }
}
