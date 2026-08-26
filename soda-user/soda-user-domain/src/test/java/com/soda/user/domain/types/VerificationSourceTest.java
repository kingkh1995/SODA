package com.soda.user.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationSource 请求源")
class VerificationSourceTest {

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 scene/subject 创建实例")
        void should_create_when_validFields() {
            var source = VerificationSource.of("UCC", "42");
            assertThat(source.scene()).isEqualTo("UCC");
            assertThat(source.subject()).isEqualTo("42");
        }

        @Test
        @DisplayName("null scene 拒绝")
        void should_throw_when_sceneIsNull() {
            assertThatThrownBy(() -> VerificationSource.of(null, "42"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白 scene 拒绝")
        void should_throw_when_sceneIsBlank() {
            assertThatThrownBy(() -> VerificationSource.of("  ", "42"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null subject 拒绝")
        void should_throw_when_subjectIsNull() {
            assertThatThrownBy(() -> VerificationSource.of("UCC", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白 subject 拒绝")
        void should_throw_when_subjectIsBlank() {
            assertThatThrownBy(() -> VerificationSource.of("UCC", "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("复合键")
    class CompositeKey {

        @Test
        @DisplayName("compositeKey 拼为 scene:subject")
        void should_composeSceneColonSubject() {
            assertThat(VerificationSource.of("UCC", "42").compositeKey()).isEqualTo("UCC:42");
        }

        @Test
        @DisplayName("URG 端点作 subject")
        void should_useEndpoint_when_sceneUrg() {
            assertThat(VerificationSource.of("URG", "13800138000").compositeKey())
                    .isEqualTo("URG:13800138000");
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同 scene/subject 相等")
        void should_beEqual_when_sameFields() {
            assertThat(VerificationSource.of("UCC", "42"))
                    .isEqualTo(VerificationSource.of("UCC", "42"));
        }

        @Test
        @DisplayName("不同 scene 不等")
        void should_notBeEqual_when_differentScene() {
            assertThat(VerificationSource.of("UCC", "42"))
                    .isNotEqualTo(VerificationSource.of("UPR", "42"));
        }

        @Test
        @DisplayName("不同 subject 不等")
        void should_notBeEqual_when_differentSubject() {
            assertThat(VerificationSource.of("UCC", "42"))
                    .isNotEqualTo(VerificationSource.of("UCC", "43"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(VerificationSource.of("UCC", "42"))
                    .hasSameHashCodeAs(VerificationSource.of("UCC", "42"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(VerificationSource.of("UCC", "42"))
                    .hasToString("VerificationSource[scene=UCC, subject=42]");
        }
    }
}
