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

@DisplayName("手机号值对象")
class MobileTest extends DomainPrimitiveContractTest<Mobile> {

    private static final String VALID_MOBILE = "13800138000";

    @Override
    protected Contract<Mobile> contract() {
        return new Contract<>(Mobile.class, () -> Mobile.of(VALID_MOBILE), "\"13800138000\"",
                "Mobile[masked=138****8000]", "12345", () -> Mobile.of("13900139000"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法手机号创建实例")
        void should_create_when_validMobile() {
            var mobile = Mobile.of(VALID_MOBILE);
            assertThat(mobile.value()).isEqualTo(VALID_MOBILE);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> Mobile.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> Mobile.of("  " + VALID_MOBILE + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"1380013800", "138001380000", "12300138000", "1380013800a", "abc", "  "})
        @DisplayName("非法手机号格式拒绝")
        void should_throw_when_invalidPattern(String invalid) {
            assertThatThrownBy(() -> Mobile.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
