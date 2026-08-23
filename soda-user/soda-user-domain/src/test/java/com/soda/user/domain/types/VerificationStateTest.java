package com.soda.user.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationState 枚举")
class VerificationStateTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(VerificationState.values()).hasSize(4);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
                I,     initialized
                P,     pending
                V,     verified
                U,     used
            """)
    @DisplayName("各枚举常量 desc() 正确")
    void should_haveCorrectDesc(String name, String desc) {
        assertThat(VerificationState.valueOf(name).desc()).isEqualTo(desc);
    }

    @ParameterizedTest(name = "{0}.terminal() = {1}")
    @CsvSource(textBlock = """
                I,     false
                P,     false
                V,     false
                U,     true
            """)
    @DisplayName("terminal() 终态判定（StateEnumType 契约，ADR-0023；V 内存瞬态非终态——不落库不占槽）")
    void should_terminal(String name, boolean terminal) {
        assertThat(VerificationState.valueOf(name).terminal()).isEqualTo(terminal);
    }

    @Test
    @DisplayName("of(null) 抛出异常")
    void should_throw_when_null() {
        assertThatThrownBy(() -> VerificationState.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Jackson round-trip")
    void should_serializeDeserialize() throws Exception {
        assertThat(MAPPER.writeValueAsString(VerificationState.I)).isEqualTo("\"I\"");
        assertThat(MAPPER.readValue("\"I\"", VerificationState.class)).isEqualTo(VerificationState.I);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", VerificationState.class))
                .isInstanceOf(JacksonException.class);
    }
}
