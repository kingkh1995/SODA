package com.soda.user.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationState 枚举")
class VerificationStateTest extends EnumDomainPrimitiveContractTest<VerificationState> {

    @Override
    protected EnumContract<VerificationState> contract() {
        return new EnumContract<>(VerificationState.class, "\"INVALID\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @ParameterizedTest(name = "of({0}) → {0}")
        @CsvSource({"I", "P", "V", "U"})
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName(String name) {
            assertThat(VerificationState.of(name)).isEqualTo(VerificationState.valueOf(name));
        }

        @Test
        @DisplayName("null / 空 / 未知名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> VerificationState.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> VerificationState.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> VerificationState.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(VerificationState.values()).containsExactly(
                    VerificationState.I, VerificationState.P, VerificationState.V, VerificationState.U);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @ParameterizedTest(name = "{0} → desc={1}")
        @CsvSource(textBlock = """
                    I,     initialized
                    P,     pending
                    V,     verified
                    U,     used
                """)
        @DisplayName("desc 逐常量")
        void should_exposeDesc(String name, String desc) {
            assertThat(VerificationState.valueOf(name).desc()).isEqualTo(desc);
        }

        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(VerificationState.I).hasToString("I");
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {

        @ParameterizedTest(name = "{0}.terminal() = {1}")
        @CsvSource(textBlock = """
                    I,     false
                    P,     false
                    V,     false
                    U,     true
                """)
        @DisplayName("terminal() 终态判定（StateEnumType 契约，ADR-0023；V 内存瞬态非终态——不落库不占槽）")
        void should_exposeTerminal(String name, boolean terminal) {
            assertThat(VerificationState.valueOf(name).terminal()).isEqualTo(terminal);
        }
    }
}
