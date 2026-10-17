package com.soda.component.web.conditional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link HttpValidatorHeadersAdvice} 出站 {@code ETag} 行为（standalone MockMvc，不起 Spring 上下文）： 响应体实现
 * {@link com.soda.component.web.HttpValidatorSource} 时按 {@code version()} 写头且不改 body， 否则不写头（见
 * ADR-0039）。与真实版本守卫的线上往返由应用根 {@code ConditionalRequestE2ETest} 覆盖。
 */
@DisplayName("HttpValidatorHeadersAdvice 出站 ETag")
class HttpValidatorHeadersAdviceTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DummyController())
                    .setControllerAdvice(new HttpValidatorHeadersAdvice())
                    .build();

    /**
     * dummy 响应体：record 组件 {@code version} 即 {@link com.soda.component.web.HttpValidatorSource} 实现。
     */
    record VersionedBody(int version, String nickname)
            implements com.soda.component.web.HttpValidatorSource {}

    record PlainBody(String nickname) {}

    @SuppressWarnings("unused")
    @RestController
    static class DummyController {

        @PostMapping("/versioned")
        VersionedBody versioned() {
            return new VersionedBody(3, "Neo");
        }

        @PostMapping("/plain")
        PlainBody plain() {
            return new PlainBody("Neo");
        }
    }

    @Test
    @DisplayName("响应体是验证器源 → 写 ETag 头（version 派生），body 原样返回")
    void should_writeEtag_when_bodyImplementsValidatorSource() throws Exception {
        mockMvc.perform(post("/versioned"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"3\""))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.nickname").value("Neo"));
    }

    @Test
    @DisplayName("响应体不是验证器源 → 不写 ETag 头")
    void should_skipEtag_when_bodyNotValidatorSource() throws Exception {
        mockMvc.perform(post("/plain"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.nickname").value("Neo"));
    }
}
