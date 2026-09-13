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

@DisplayName("已脱敏手机号值对象")
class MaskedMobileTest extends DomainPrimitiveContractTest<MaskedMobile> {

    private static final String VALID_MASKED = "138****8000";

    @Override
    protected Contract<MaskedMobile> contract() {
        return new Contract<>(MaskedMobile.class, () -> new MaskedMobile(VALID_MASKED),
                "\"" + VALID_MASKED + "\"", "MaskedMobile[value=" + VALID_MASKED + "]",
                "\"13800138000\"", () -> new MaskedMobile("139****1234"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var mm = new MaskedMobile(VALID_MASKED);
            assertThat(mm.value()).isEqualTo(VALID_MASKED);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "13800138000", "138***8000", "138*******8000", "abc****1234", "138****800"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedMobile(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始手机号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawMobile() {
            assertThat(MaskedMobile.from(Mobile.of("13800138000")))
                    .isEqualTo(new MaskedMobile("138****8000"));
        }
    }
}
