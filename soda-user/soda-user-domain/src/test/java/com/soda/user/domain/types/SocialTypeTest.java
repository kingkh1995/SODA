package com.soda.user.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialType 枚举")
class SocialTypeTest extends EnumDomainPrimitiveContractTest<SocialType> {

    @Override
    protected EnumContract<SocialType> contract() {
        return new EnumContract<>(SocialType.class, "\"INVALID\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @ParameterizedTest(name = "of({0}) → {0}")
        @CsvSource({"GE", "DT", "WENT", "WMP", "WOPN", "WMIN", "ALIP"})
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName(String name) {
            assertThat(SocialType.of(name)).isEqualTo(SocialType.valueOf(name));
        }

        @Test
        @DisplayName("null / 空 / 未知名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> SocialType.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SocialType.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SocialType.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(SocialType.values()).containsExactly(
                    SocialType.GE, SocialType.DT, SocialType.WENT, SocialType.WMP,
                    SocialType.WOPN, SocialType.WMIN, SocialType.ALIP);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @ParameterizedTest(name = "{0} → desc={1}")
        @CsvSource(textBlock = """
                    GE,    gitee
                    DT,    ding-talk
                    WENT,  wechat-work
                    WMP,   wechat-mp
                    WOPN,  wechat-open
                    WMIN,  wechat-mini
                    ALIP,  alipay-mini
                """)
        @DisplayName("desc 逐常量")
        void should_exposeDesc(String name, String desc) {
            assertThat(SocialType.valueOf(name).desc()).isEqualTo(desc);
        }

        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(SocialType.GE).hasToString("GE");
        }
    }
}
