package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("凭证哈希值对象")
class CredentialHashTest {

    private static final String VALID_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法哈希值创建实例")
        void should_create_when_validHash() {
            var ch = new CredentialHash(VALID_HASH);
            assertThat(ch.value()).isEqualTo(VALID_HASH);
        }

        @Test
        @DisplayName("任意非空白字符串创建实例")
        void should_create_when_anyNonBlank() {
            var ch = new CredentialHash("any-hash-value");
            assertThat(ch.value()).isEqualTo("any-hash-value");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 或空字符串拒绝")
        void should_throw_when_valueIsNullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new CredentialHash(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new CredentialHash(VALID_HASH)).isEqualTo(new CredentialHash(VALID_HASH));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new CredentialHash(VALID_HASH)).isNotEqualTo(new CredentialHash("other-hash"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new CredentialHash(VALID_HASH);
            var b = new CredentialHash(VALID_HASH);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new CredentialHash("hash")).hasToString("CredentialHash[value=hash]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new CredentialHash(VALID_HASH);
            JacksonTestUtil.assertRoundTrip(original, CredentialHash.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new CredentialHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
            assertThat(json).isEqualTo("\"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy\"", CredentialHash.class))
                    .isEqualTo(new CredentialHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", CredentialHash.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
