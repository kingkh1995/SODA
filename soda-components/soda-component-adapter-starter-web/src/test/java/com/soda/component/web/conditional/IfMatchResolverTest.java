package com.soda.component.web.conditional;

import com.soda.component.web.IfMatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link IfMatchResolver} 入站归一化行为：缺失 / 通配放行、首值提取、弱标签与非法值拒绝。
 */
@DisplayName("IfMatchResolver 入站归一化")
class IfMatchResolverTest {

    private final IfMatchResolver resolver = new IfMatchResolver();

    private static MethodParameter param(String name, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(Sample.class.getDeclaredMethod(name, type), 0);
    }

    private static NativeWebRequest webRequest(String... ifMatchValues) {
        var request = mock(NativeWebRequest.class);
        when(request.getHeaderValues(HttpHeaders.IF_MATCH))
                .thenReturn(ifMatchValues.length == 0 ? null : ifMatchValues);
        return request;
    }

    private Object resolve(String... ifMatchValues) throws Exception {
        return resolver.resolveArgument(param("handle", Integer.class), null, webRequest(ifMatchValues), null);
    }

    @SuppressWarnings("unused")
    private static class Sample {

        void handle(@IfMatch Integer version) {
        }

        void plain(String value) {
        }

        void wrongType(@IfMatch String version) {
        }
    }

    @Nested
    @DisplayName("参数支持判断")
    class SupportsParameter {

        @Test
        @DisplayName("@IfMatch Integer 参数支持")
        void should_support_when_ifMatchIntegerParameter() throws Exception {
            assertThat(resolver.supportsParameter(param("handle", Integer.class))).isTrue();
        }

        @Test
        @DisplayName("无注解参数不支持")
        void should_notSupport_when_plainParameter() throws Exception {
            assertThat(resolver.supportsParameter(param("plain", String.class))).isFalse();
        }
    }

    @Nested
    @DisplayName("归一化放行与取值")
    class Normalization {

        @Test
        @DisplayName("缺失 If-Match → null 放行")
        void should_returnNull_when_ifMatchMissing() throws Exception {
            assertThat(resolver.resolveArgument(param("handle", Integer.class), null, webRequest(), null)).isNull();
        }

        @Test
        @DisplayName("通配 * → null 放行")
        void should_returnNull_when_ifMatchWildcard() throws Exception {
            assertThat(resolver.resolveArgument(param("handle", Integer.class), null, webRequest("*"), null)).isNull();
        }

        @Test
        @DisplayName("单值 → 期望版本")
        void should_returnVersion_when_singleValue() throws Exception {
            assertThat(resolve("\"3\"")).isEqualTo(3);
        }

        @Test
        @DisplayName("多值 → 取首值")
        void should_returnFirst_when_multipleValues() throws Exception {
            assertThat(resolve("\"5\"", "\"7\"")).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("拒绝")
    class Rejection {

        @Test
        @DisplayName("弱标签 → 412")
        void should_rejectWeakValidator_when_ifMatchWeak() throws Exception {
            assertThatThrownBy(() -> resolve("W/\"3\""))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED))
                    .hasMessageContaining("requires strong comparison");
        }

        @Test
        @DisplayName("非法值 → 400")
        void should_rejectInvalidValue_when_unparsable() throws Exception {
            assertThatThrownBy(() -> resolve("\"abc\""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid If-Match value");
        }

        @Test
        @DisplayName("非 Integer 参数 → 装配期快速失败")
        void should_failFast_when_nonIntegerParameter() throws Exception {
            assertThatThrownBy(() -> resolver.resolveArgument(
                    param("wrongType", String.class), null, webRequest("\"3\""), null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("@IfMatch 仅支持 Integer 参数");
        }
    }
}
