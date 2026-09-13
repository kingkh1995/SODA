package com.soda.component.domain.types;

import com.soda.component.domain.testutil.EnumDomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sex 枚举")
class SexTest extends EnumDomainPrimitiveContractTest<Sex> {

    @Override
    protected EnumContract<Sex> contract() {
        return new EnumContract<>(Sex.class, "\"INVALID\"");
    }

    @Nested
    @DisplayName("查找")
    class Lookup {

        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(Sex.values()).containsExactly(Sex.M, Sex.F);
        }

        @Test
        @DisplayName("合法名逐一查找返回对应枚举")
        void should_findByName_when_validName() {
            assertThat(Sex.of("M")).isEqualTo(Sex.M);
            assertThat(Sex.of("F")).isEqualTo(Sex.F);
        }

        @Test
        @DisplayName("null 名拒绝")
        void should_throw_when_nameIsNull() {
            assertThatThrownBy(() -> Sex.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空名拒绝")
        void should_throw_when_nameIsBlank() {
            assertThatThrownBy(() -> Sex.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("未知名拒绝")
        void should_throw_when_unknownName() {
            assertThatThrownBy(() -> Sex.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @Test
        @DisplayName("各枚举常量 desc() 返回描述")
        void should_haveCorrectDesc() {
            assertThat(Sex.M.desc()).isEqualTo("male");
            assertThat(Sex.F.desc()).isEqualTo("female");
        }

        @Test
        @DisplayName("toString 返回枚举名")
        void should_returnName() {
            assertThat(Sex.M).hasToString("M");
        }
    }
}
