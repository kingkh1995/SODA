package com.soda.user.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VerificationSource 请求源")
class VerificationSourceTest extends DomainPrimitiveContractTest<VerificationSource> {

    @Override
    protected Contract<VerificationSource> contract() {
        return new Contract<>(VerificationSource.class, () -> new VerificationSource("UCC", "42"),
                "{\"scene\":\"UCC\",\"subject\":\"42\"}", "VerificationSource[scene=UCC, subject=42]",
                "{}", () -> new VerificationSource("UPR", "42"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 scene/subject 创建实例")
        void should_create_when_validFields() {
            var source = new VerificationSource("UCC", "42");
            assertThat(source.scene()).isEqualTo("UCC");
            assertThat(source.subject()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("null scene 拒绝")
        void should_throw_when_sceneIsNull() {
            assertThatThrownBy(() -> new VerificationSource(null, "42"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白 scene 拒绝")
        void should_throw_when_sceneIsBlank() {
            assertThatThrownBy(() -> new VerificationSource("  ", "42"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null subject 拒绝")
        void should_throw_when_subjectIsNull() {
            assertThatThrownBy(() -> new VerificationSource("UCC", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白 subject 拒绝")
        void should_throw_when_subjectIsBlank() {
            assertThatThrownBy(() -> new VerificationSource("UCC", "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {

        @Test
        @DisplayName("compositeKey 拼为 scene:subject")
        void should_composeSceneColonSubject() {
            assertThat(new VerificationSource("UCC", "42").compositeKey()).isEqualTo("UCC:42");
        }

        @Test
        @DisplayName("URG 端点作 subject")
        void should_useEndpoint_when_sceneUrg() {
            assertThat(new VerificationSource("URG", "13800138000").compositeKey())
                    .isEqualTo("URG:13800138000");
        }
    }
}
