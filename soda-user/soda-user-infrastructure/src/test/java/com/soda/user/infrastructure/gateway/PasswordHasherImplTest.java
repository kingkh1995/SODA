package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PasswordHasherImpl} 单元测试 —— 真实 BCrypt 行为与 {@code needsRehash} 强度判定。
 */
@DisplayName("PasswordHasherImpl")
class PasswordHasherImplTest {

    /**
     * 盐 + 校验和段（53 字符），换前缀构造不同成本的合法 bcrypt 形态。
     */
    private static final String BCRYPT_TAIL = "N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final PasswordHasherImpl hasher = new PasswordHasherImpl();

    @Nested
    @DisplayName("hash")
    class Hash {

        @Test
        @DisplayName("hash 产物为当前成本的 bcrypt 形态且可自验证")
        void should_produceVerifiableBcryptAtCurrentCost() {
            var h = hasher.hash(new SecretValue("s3cret!"));
            assertThat(h.value()).startsWith("$2a$10$").hasSize(60);
            assertThat(hasher.verify(h, new SecretValue("s3cret!"))).isTrue();
            assertThat(hasher.verify(h, new SecretValue("other"))).isFalse();
        }

        @Test
        @DisplayName("hash —— 恰好 72 字节可接受，73 字节拒绝")
        void should_enforce72ByteInputLimit() {
            var boundary = hasher.hash(new SecretValue("a".repeat(72)));
            assertThat(hasher.verify(boundary, new SecretValue("a".repeat(72)))).isTrue();
            assertThatThrownBy(() -> hasher.hash(new SecretValue("a".repeat(73))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("hash —— 上限按 UTF-8 字节计而非字符（24 个汉字 = 72 字节可过）")
        void should_countBytesNotChars() {
            assertThat(hasher.hash(new SecretValue("密".repeat(24))).value()).startsWith("$2a$10$");
            assertThatThrownBy(() -> hasher.hash(new SecretValue("密".repeat(25))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("needsRehash 强度判定")
    class NeedsRehash {

        @Test
        @DisplayName("needsRehash —— 成本低于当前配置为真")
        void should_flagLowCostAsNeedingUpgrade() {
            assertThat(hasher.needsRehash(PasswordHash.of("$2a$08$" + BCRYPT_TAIL))).isTrue();
        }

        @Test
        @DisplayName("needsRehash —— 成本不低于当前配置为假（含等值边界）")
        void should_notFlagCurrentOrHigherCost() {
            assertThat(hasher.needsRehash(PasswordHash.of("$2a$10$" + BCRYPT_TAIL))).isFalse();
            assertThat(hasher.needsRehash(PasswordHash.of("$2y$12$" + BCRYPT_TAIL))).isFalse();
        }

        @Test
        @DisplayName("needsRehash —— 非 bcrypt 形态不评估，返回假")
        void should_notFlagNonBcryptForm() {
            assertThat(hasher.needsRehash(
                    PasswordHash.of("$argon2id$v=19$m=65536,t=2,p=1$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG")))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("verify 拒绝错误候选（恒定时间比较由 BCrypt 实现承担）")
        void should_rejectWrongCandidate() {
            var h = hasher.hash(new SecretValue("correct-horse"));
            assertThat(hasher.verify(h, new SecretValue("wrong"))).isFalse();
        }

        @Test
        @DisplayName("verify 不拦超长候选 —— 存量截断哈希靠对称性照常匹配")
        void should_allowLongCandidateOnVerify() {
            var stored = hasher.hash(new SecretValue("a".repeat(72)));
            assertThat(hasher.verify(stored, new SecretValue("a".repeat(72) + "-entirely-different-tail")))
                    .as("BCrypt 只取前 72 字节：与存量哈希同前缀即匹配（守卫只在 hash 入口）")
                    .isTrue();
        }
    }
}
