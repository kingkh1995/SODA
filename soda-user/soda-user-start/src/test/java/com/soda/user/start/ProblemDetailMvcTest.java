package com.soda.user.start;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 错误响应 ProblemDetail 完整 MVC 验证。
 * <p>
 * 跑在完整上下文（非切片）：框架异常与项目异常共用一个 advice（starter-web {@code ProblemDetailAdvice}
 * 继承基类），状态码与 {@code detail} 端到端可验。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@AutoConfigureMockMvc
@DisplayName("错误响应 ProblemDetail（完整 MVC）")
class ProblemDetailMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 从 Create 响应体取新建用户 id（裸 {@code UserResponse}，非信封）。
     */
    private static long idOf(String createResponseBody) {
        return com.jayway.jsonpath.JsonPath.<Number>read(createResponseBody, "$.id").longValue();
    }

    @Test
    @DisplayName("非法请求体应由 flag advice 返回 400 problem+json")
    void should_returnProblemJson_when_requestBodyInvalid() throws Exception {
        var body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"123\",\"nickname\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"status\":400");
    }

    @Test
    @DisplayName("重复用户名应译为 409 Conflict（RFC 9110 §15.5.10；AIP-133 ALREADY_EXISTS）")
    void should_returnConflict_when_usernameDuplicated() throws Exception {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        var unique = "dup" + System.nanoTime() % 100000;
        var valid = "{\"username\":\"" + unique + "\",\"password\":\"password123\",\"nickname\":\"Dup\"}";
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valid))
                .andExpect(status().isCreated());
        // 重复 username -> ConflictException.alreadyExists -> 409，消息即 detail
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valid))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Username already exists: " + unique));
    }

    @Test
    @DisplayName("资源不存在应返回 404（RFC 9110 §15.5.5；AIP-135 NOT_FOUND）")
    void should_returnNotFound_when_userMissing() throws Exception {
        mockMvc.perform(patch("/api/users/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.instance").value("/api/users/999999"))
                .andExpect(jsonPath("$.detail").value("User not found: 999999"));
    }

    @Test
    @DisplayName("路径变量违约（@Positive）应返回 400 且 detail 携带违约消息，而非 500 或被吞掉")
    void should_returnBadRequest_when_pathVariableViolatesConstraint() throws Exception {
        mockMvc.perform(patch("/api/users/{id}", -5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail", containsString("must be greater than 0")));
    }

    @Test
    @DisplayName("update_mask 含未知字段名应返回 400（应用层 IAE，T-02 端到端）")
    void should_returnInvalidArgument_when_updateMaskUnknownField() throws Exception {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        var unique = "mask" + System.nanoTime() % 100000;
        var created = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + unique + "\",\"password\":\"password123\",\"nickname\":\"Mask\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var id = idOf(created);

        // UpdateMask.parse 白名单外字段 -> IAE -> starter-web ProblemDetailAdvice 译 400（真实 service，非 mock）
        // If-Match 带创建时版本（0），使请求真正走到 UpdateMask.parse 白名单分支
        mockMvc.perform(patch("/api/users/{id}", id)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"updateMask\":\"password\",\"nickname\":\"Mask2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.instance").value("/api/users/" + id))
                .andExpect(jsonPath("$.detail", containsString("unknown update_mask fields")));
    }
}
