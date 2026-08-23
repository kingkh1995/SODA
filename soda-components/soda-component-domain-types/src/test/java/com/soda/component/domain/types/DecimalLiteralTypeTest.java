package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.soda.component.domain.testutil.JacksonTestUtil.assertRoundTrip;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DecimalLiteralType} 基类契约测试——防回归缓存不变量与类型作用域相等性。
 */
@DisplayName("DecimalLiteralType 基类契约")
class DecimalLiteralTypeTest {

    @Nested
    @DisplayName("跨子类不相等（getClass 作用域）")
    class CrossTypeEquality {

        @Test
        @DisplayName("规范串相同的 WanYuan 与 Percentage 不相等")
        void should_notEqual_acrossTypes_sameCanonicalString() {
            var yuan = WanYuan.from(new BigDecimal("1.00"));
            var pct = Percentage.from(new BigDecimal("1.00"));
            assertThat(yuan).isNotEqualTo(pct);
            assertThat(pct).isNotEqualTo(yuan);
        }

        @Test
        @DisplayName("同类型规范串相同则相等、哈希一致")
        void should_equal_withinType_sameCanonicalString() {
            assertThat(WanYuan.from(new BigDecimal("1.00")))
                    .isEqualTo(WanYuan.from(new BigDecimal("1.00")))
                    .hasSameHashCodeAs(WanYuan.from(new BigDecimal("1.00")));
            assertThat(Percentage.from(new BigDecimal("50.00")))
                    .isEqualTo(Percentage.from(new BigDecimal("50.00")));
        }
    }

    @Nested
    @DisplayName("缓存不变量")
    class CacheInvariant {

        @Test
        @DisplayName("value()（@JsonValue）与 decimalValue() 同源于规范化 BigDecimal")
        void should_valueAndDecimalValue_fromSameNormalized() {
            var yuan = WanYuan.from(new BigDecimal("1.5"));
            assertThat(yuan.value()).isEqualTo("1.50");
            assertThat(yuan.decimalValue()).isEqualByComparingTo("1.5");
            assertThat(yuan.value()).isEqualTo(yuan.decimalValue().toPlainString());
        }
    }

    @Nested
    @DisplayName("@JsonValue 跨三层继承")
    class JsonValueInheritance {

        @Test
        @DisplayName("WanYuan 序列化为裸 String 标量（接口→基类→子类）")
        void should_serializeWanYuanAsBareScalar() throws Exception {
            var json = JacksonTestUtil.mapper().writeValueAsString(WanYuan.from(new BigDecimal("1.50")));
            assertThat(json).isEqualTo("\"1.50\"");
        }

        @Test
        @DisplayName("Percentage 序列化为裸 String 标量")
        void should_serializePercentageAsBareScalar() throws Exception {
            var json = JacksonTestUtil.mapper().writeValueAsString(Percentage.from(new BigDecimal("12.34")));
            assertThat(json).isEqualTo("\"12.34\"");
        }

        @Test
        @DisplayName("round-trip 一致")
        void should_roundTrip() throws Exception {
            assertRoundTrip(WanYuan.from(new BigDecimal("1.50")), WanYuan.class);
            assertRoundTrip(Percentage.from(new BigDecimal("12.34")), Percentage.class);
        }
    }
}
