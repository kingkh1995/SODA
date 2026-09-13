package com.soda.user.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserState 枚举")
class UserStateTest extends EnumDomainPrimitiveContractTest<UserState> {

    @Override
    protected EnumContract<UserState> contract() {
        return new EnumContract<>(UserState.class, "\"INVALID\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @Test
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName() {
            assertThat(UserState.of("E")).isEqualTo(UserState.E);
            assertThat(UserState.of("D")).isEqualTo(UserState.D);
            assertThat(UserState.of("R")).isEqualTo(UserState.R);
        }

        @Test
        @DisplayName("null / 空 / 未知名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> UserState.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> UserState.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> UserState.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(UserState.values()).containsExactly(UserState.E, UserState.D, UserState.R);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @Test
        @DisplayName("desc 逐常量")
        void should_exposeDesc() {
            assertThat(UserState.E.desc()).isEqualTo("enabled");
            assertThat(UserState.D.desc()).isEqualTo("disabled");
            assertThat(UserState.R.desc()).isEqualTo("deregistered");
        }

        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(UserState.E).hasToString("E");
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {

        @Test
        @DisplayName("terminal() 终态判定（StateEnumType 契约，ADR-0023）")
        void should_exposeTerminal() {
            assertThat(UserState.E.terminal()).isFalse();
            assertThat(UserState.D.terminal()).isFalse();
            assertThat(UserState.R.terminal()).isTrue();
        }
    }
}
