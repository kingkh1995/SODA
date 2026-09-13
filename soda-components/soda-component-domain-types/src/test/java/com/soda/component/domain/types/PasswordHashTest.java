package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordHash —— PHC 白名单与格式感知遮蔽")
class PasswordHashTest extends DomainPrimitiveContractTest<PasswordHash> {

    private static final String BCRYPT = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String ARGON2ID = "$argon2id$v=19$m=65536,t=2,p=1$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG";

    @Override
    protected Contract<PasswordHash> contract() {
        return new Contract<>(PasswordHash.class, () -> PasswordHash.of(BCRYPT), "\"" + BCRYPT + "\"",
                "PasswordHash[masked=$2a$10$***]", "\"$md5$abcdef\"",
                () -> PasswordHash.of(ARGON2ID));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("接受 bcrypt 格式")
        void should_create_when_bcryptFormat() {
            assertThat(PasswordHash.of(BCRYPT).value()).isEqualTo(BCRYPT);
        }

        @Test
        @DisplayName("接受 argon2id 格式")
        void should_create_when_argon2idFormat() {
            assertThat(PasswordHash.of(ARGON2ID).value()).isEqualTo(ARGON2ID);
        }

        @Test
        @DisplayName("接受 scrypt / pbkdf2 前缀")
        void should_create_when_scryptOrPbkdf2Prefix() {
            var scrypt = "$scrypt$n=16384,r=8,p=1$c2FsdA$aHash";
            var pbkdf2 = "$pbkdf2-sha256$c=10000$c2FsdA$aHash";
            assertThat(PasswordHash.of(scrypt).value()).isEqualTo(scrypt);
            assertThat(PasswordHash.of(pbkdf2).value()).isEqualTo(pbkdf2);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> PasswordHash.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝空白输入")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> PasswordHash.of("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝裸 SHA-256 hex（快速摘要归 Digest）")
        void should_throw_when_bareHexDigest() {
            assertThatThrownBy(() -> PasswordHash.of("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝未知算法前缀")
        void should_throw_when_unknownPrefix() {
            assertThatThrownBy(() -> PasswordHash.of("$md5$abcdef"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("拒绝超过最大长度（默认 200）的哈希串")
        void should_throw_when_overMaxLength() {
            var overlyLong = "$2a$10$" + "a".repeat(194);
            assertThatThrownBy(() -> PasswordHash.of(overlyLong))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("脱敏")
    class Masking {
        @Test
        @DisplayName("bcrypt 遮蔽保留参数段 $2a$10$")
        void should_maskToParams_when_bcrypt() {
            assertThat(PasswordHash.of(BCRYPT).maskedValue()).isEqualTo("$2a$10$***");
        }

        @Test
        @DisplayName("argon2id 遮蔽保留完整成本参数、不含盐")
        void should_maskCostParamsWithoutSalt_when_argon2id() {
            assertThat(PasswordHash.of(ARGON2ID).maskedValue())
                    .isEqualTo("$argon2id$v=19$m=65536,t=2,p=1$***")
                    .doesNotContain("c29tZXNhbHQ");
        }

        @Test
        @DisplayName("scrypt 遮蔽保留成本参数、不含盐")
        void should_maskCostParamsWithoutSalt_when_scrypt() {
            assertThat(PasswordHash.of("$scrypt$n=16384,r=8,p=1$c2FsdA$aHash").maskedValue())
                    .isEqualTo("$scrypt$n=16384,r=8,p=1$***")
                    .doesNotContain("c2FsdA");
        }

        @Test
        @DisplayName("pbkdf2 遮蔽保留成本参数、不含盐")
        void should_maskCostParamsWithoutSalt_when_pbkdf2() {
            assertThat(PasswordHash.of("$pbkdf2-sha256$c=10000$c2FsdA$aHash").maskedValue())
                    .isEqualTo("$pbkdf2-sha256$c=10000$***")
                    .doesNotContain("c2FsdA");
        }
    }
}
