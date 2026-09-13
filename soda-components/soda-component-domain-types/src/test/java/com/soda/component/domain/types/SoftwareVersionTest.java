package com.soda.component.domain.types;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SoftwareVersion 值对象")
class SoftwareVersionTest extends ComparableDomainPrimitiveContractTest<SoftwareVersion> {

    @Override
    protected Contract<SoftwareVersion> contract() {
        return new Contract<>(SoftwareVersion.class, () -> SoftwareVersion.of("v2.1.3"), "\"v2.1.3\"",
                "SoftwareVersion[value=v2.1.3]", "\"2.1.3\"", () -> SoftwareVersion.of("v2.1.10"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法字符串创建实例")
        void should_create_when_validString() {
            var v = SoftwareVersion.of("v2.1.3");
            assertThat(v.value()).isEqualTo("v2.1.3");
            assertThat(v.major()).isEqualTo(2);
            assertThat(v.minor()).isEqualTo(1);
            assertThat(v.patch()).isEqualTo(3);
        }

        @Test
        @DisplayName("前缀大小写不敏感，规范值统一小写")
        void should_normalize_when_uppercasePrefix() {
            assertThat(SoftwareVersion.of("V2.1.3").value()).isEqualTo("v2.1.3");
        }

        @Test
        @DisplayName("前导 0 归一化")
        void should_normalize_when_leadingZeros() {
            assertThat(SoftwareVersion.of("v002.001.003").value()).isEqualTo("v2.1.3");
        }

        @Test
        @DisplayName("三段数值构造")
        void should_create_when_fromSegments() {
            assertThat(SoftwareVersion.from(2, 1, 3).value()).isEqualTo("v2.1.3");
        }

        @Test
        @DisplayName("打包 int 还原")
        void should_create_when_fromPackedInt() {
            assertThat(SoftwareVersion.fromPackedInt(2_001_003).value()).isEqualTo("v2.1.3");
        }

        @Test
        @DisplayName("边界值可构造")
        void should_create_when_boundaryValues() {
            assertThat(SoftwareVersion.of("v0.0.0").value()).isEqualTo("v0.0.0");
            assertThat(SoftwareVersion.of("v999.999.999").value()).isEqualTo("v999.999.999");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        static Stream<String> invalidFormats() {
            return Stream.of(
                    "2.1.3",     // 缺前缀
                    "v2.1",      // 缺 patch 段
                    "v2",        // 缺两段
                    "v2.1.3.4",  // 多一段
                    "v2..3",     // 空段
                    "v1000.1.2", // 段超 999
                    "v2.1.3 ",   // 尾部空白（全串匹配失败）
                    "vv2.1.3",   // 双前缀
                    "v-1.2.3",   // 负号
                    "abc"        // 完全非法
            );
        }

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> SoftwareVersion.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("空字符串拒绝")
        void should_throw_when_valueIsEmpty() {
            assertThatThrownBy(() -> SoftwareVersion.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> SoftwareVersion.of("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @ParameterizedTest
        @MethodSource("invalidFormats")
        @DisplayName("非法格式拒绝")
        void should_throw_when_invalidFormat(String s) {
            assertThatThrownBy(() -> SoftwareVersion.of(s))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid format");
        }

        @Test
        @DisplayName("三段数值越界拒绝")
        void should_throw_when_segmentOutOfRange() {
            assertThatThrownBy(() -> SoftwareVersion.from(1000, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999");
            assertThatThrownBy(() -> SoftwareVersion.from(0, -1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999");
        }

        @Test
        @DisplayName("打包 int 越界拒绝")
        void should_throw_when_packedIntOutOfRange() {
            assertThatThrownBy(() -> SoftwareVersion.fromPackedInt(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999999999");
            assertThatThrownBy(() -> SoftwareVersion.fromPackedInt(1_000_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999999999");
        }
    }

    @Nested
    @DisplayName("打包 int")
    class PackedInt {
        @Test
        @DisplayName("打包编码正确")
        void should_pack() {
            assertThat(SoftwareVersion.of("v2.1.3").toPackedInt()).isEqualTo(2_001_003);
            assertThat(SoftwareVersion.of("v0.0.0").toPackedInt()).isZero();
            assertThat(SoftwareVersion.of("v999.999.999").toPackedInt()).isEqualTo(999_999_999);
        }

        @Test
        @DisplayName("打包双向 round-trip")
        void should_roundTrip() {
            var v = SoftwareVersion.of("v2.1.3");
            assertThat(SoftwareVersion.fromPackedInt(v.toPackedInt())).isEqualTo(v);
        }

        @Test
        @DisplayName("打包序与版本序单调一致")
        void should_preserveOrder() {
            assertThat(SoftwareVersion.of("v2.1.3").toPackedInt())
                    .isLessThan(SoftwareVersion.of("v2.10.0").toPackedInt());
        }
    }

    @Nested
    @DisplayName("步进")
    class Next {
        @Test
        @DisplayName("nextPatch 递增 patch")
        void should_nextPatch() {
            assertThat(SoftwareVersion.of("v2.1.3").nextPatch()).isEqualTo(SoftwareVersion.of("v2.1.4"));
        }

        @Test
        @DisplayName("nextMinor 递增 minor 并清零 patch")
        void should_nextMinor() {
            assertThat(SoftwareVersion.of("v2.1.3").nextMinor()).isEqualTo(SoftwareVersion.of("v2.2.0"));
        }

        @Test
        @DisplayName("nextMajor 递增 major 并清零低位")
        void should_nextMajor() {
            assertThat(SoftwareVersion.of("v2.1.3").nextMajor()).isEqualTo(SoftwareVersion.of("v3.0.0"));
        }

        @Test
        @DisplayName("next 不修改原实例")
        void should_notMutate() {
            var v = SoftwareVersion.of("v2.1.3");
            var next = v.nextPatch();
            assertThat(next).isEqualTo(SoftwareVersion.of("v2.1.4"));
            assertThat(v).isEqualTo(SoftwareVersion.of("v2.1.3"));
        }

        @Test
        @DisplayName("段位到 999 时抛错，不进位")
        void should_throw_when_segmentAtMax() {
            assertThatThrownBy(() -> SoftwareVersion.of("v2.1.999").nextPatch())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999");
            assertThatThrownBy(() -> SoftwareVersion.of("v2.999.5").nextMinor())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999");
            assertThatThrownBy(() -> SoftwareVersion.of("v999.1.1").nextMajor())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be between 0 and 999");
        }
    }
}
