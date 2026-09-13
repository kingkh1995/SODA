package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.component.domain.types.LongId;
import com.soda.user.domain.types.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId 值对象")
class UserIdTest extends ComparableDomainPrimitiveContractTest<UserId> {

    private static final long VALID_ID = 1L;

    @Override
    protected Contract<UserId> contract() {
        return new Contract<>(UserId.class, () -> new UserId(42L), "42",
                "UserId[value=42]", "\"not-a-number\"", () -> new UserId(43L));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法 ID 创建实例")
        void should_create_when_validValue() {
            var id = new UserId(VALID_ID);
            assertThat(id.value()).isEqualTo(VALID_ID);
        }

        @Test
        @DisplayName("有效字符串 parse")
        void should_parse_when_validString() {
            assertThat(UserId.parse("1")).isEqualTo(new UserId(VALID_ID));
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("0 值拒绝（minValue exclusive）")
        void should_throw_when_zero() {
            assertThatThrownBy(() -> new UserId(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("负值拒绝")
        void should_throw_when_negative() {
            assertThatThrownBy(() -> new UserId(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new UserId(-100L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse null 字符串拒绝")
        void should_throw_when_nullString() {
            assertThatThrownBy(() -> UserId.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("parse 非数字字符串拒绝")
        void should_throw_when_notANumber() {
            assertThatThrownBy(() -> UserId.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("标识符")
    class Identity {
        @Test
        @DisplayName("identifier 返回类型化规范值")
        void should_returnTypedValue_when_identifier() {
            assertThat(new UserId(42L).identifier()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("转换")
    class Conversion {
        @Test
        @DisplayName("toLongId 产出规范 LongId")
        void should_produceCanonicalLongId_when_toLongId() {
            assertThat(new UserId(42L).toLongId()).isEqualTo(new LongId(42L));
        }

        @Test
        @DisplayName("toLongId 往返互逆")
        void should_beInverse_when_toLongId() {
            var id = new UserId(42L);
            assertThat(new UserId(id.toLongId().value())).isEqualTo(id);
        }
    }
}
