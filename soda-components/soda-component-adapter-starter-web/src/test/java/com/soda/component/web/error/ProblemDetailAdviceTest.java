package com.soda.component.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soda.component.api.error.ConflictException;
import com.soda.component.api.error.NotFoundException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * {@link ProblemDetailAdvice} 翻译矩阵（standalone MockMvc + dummy controller，不起 Spring 上下文）。
 *
 * <p>只钉翻译与渲染：项目失败信号按类型定码且 detail 即异常消息、乐观锁竞态 409 固定文案、 未捕获异常兜底 500 且不泄露内部消息；继承 {@code
 * ResponseEntityExceptionHandler} 的框架异常（请求体校验等） 一并译为 problem+json。各异常在业务栈中的真实抛出点归对应模块（抛出契约），本类不重复。
 */
@DisplayName("ProblemDetailAdvice 翻译矩阵")
class ProblemDetailAdviceTest {

    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new DummyController())
                    .setControllerAdvice(new ProblemDetailAdvice())
                    .build();

    /** dummy 请求体：只用于触发框架校验异常（@NotBlank 违约）。 */
    record EchoBody(@NotBlank String value) {}

    /** dummy 端点：按需抛出目标异常；位于 {@code com.soda} 内以满足 advice 的 {@code basePackages}。 */
    @SuppressWarnings("unused")
    @RestController
    static class DummyController {

        @PostMapping("/not-found")
        void notFound() {
            throw NotFoundException.entityNotFound("Widget", 42);
        }

        @PostMapping("/conflict")
        void conflict() {
            throw ConflictException.alreadyExists("Username", "alice");
        }

        @PostMapping("/bad-request")
        void badRequest() {
            throw new IllegalArgumentException("Invalid If-Match value");
        }

        @PostMapping("/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException("must be greater than 0", Set.of());
        }

        @PostMapping("/stale-write")
        void staleWrite() {
            throw new OptimisticLockingFailureException(
                    "Row was updated or deleted by another transaction");
        }

        @PostMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @PostMapping("/echo")
        String echo(@Valid @RequestBody EchoBody body) {
            return body.value();
        }
    }

    @Nested
    @DisplayName("项目失败信号")
    class ProjectFailures {

        @Test
        @DisplayName("ProblemDetailException 取类型自带状态码，detail 即消息，instance 取请求 URI")
        void should_renderStatusAndInstance_when_problemDetailException() throws Exception {
            mockMvc.perform(post("/not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Widget not found: 42"))
                    .andExpect(jsonPath("$.instance").value("/not-found"));
        }

        @Test
        @DisplayName("ConflictException → 409，detail 即冲突消息")
        void should_returnConflict_when_conflictException() throws Exception {
            mockMvc.perform(post("/conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Username already exists: alice"));
        }

        @Test
        @DisplayName("IllegalArgumentException → 400，detail 即异常消息")
        void should_returnBadRequest_when_illegalArgumentException() throws Exception {
            mockMvc.perform(post("/bad-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.detail").value("Invalid If-Match value"));
        }

        @Test
        @DisplayName("ConstraintViolationException → 400，detail 即违约消息")
        void should_returnBadRequest_when_constraintViolationException() throws Exception {
            mockMvc.perform(post("/constraint-violation"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("must be greater than 0"));
        }

        @Test
        @DisplayName("乐观锁竞态 → 409 且 detail 为固定文案（不泄露 SQL）")
        void should_returnConflict_when_optimisticLockingFailure() throws Exception {
            mockMvc.perform(post("/stale-write"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Concurrent modification"));
        }

        @Test
        @DisplayName("未捕获异常兜底 → 500 固定文案，不泄露内部消息")
        void should_returnInternalError_when_uncaughtException() throws Exception {
            var body =
                    mockMvc.perform(post("/boom"))
                            .andExpect(status().isInternalServerError())
                            .andExpect(
                                    content().contentTypeCompatibleWith("application/problem+json"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            assertThat(body).contains("\"status\":500").doesNotContain("secret internal detail");
        }
    }

    @Nested
    @DisplayName("框架异常（继承 handler）")
    class FrameworkFailures {

        @Test
        @DisplayName("非法请求体（@Valid 违约）→ 400 problem+json")
        void should_returnProblemJson_when_requestBodyInvalid() throws Exception {
            mockMvc.perform(
                            post("/echo")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"value\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
