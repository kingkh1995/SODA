package com.soda.user.start;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 条件请求 E2E（完整上下文 + H2，见 ADR-0037/0039）。
 *
 * <p>只断言跨边界组合结果（test-conventions §2.2）：域版本 → 出站 {@code ETag} → 入站 {@code If-Match} → 应用层版本守卫 → 递增
 * ETag 的完整往返，以及公开错误合同（412 失配 / 404 缺失）。advice 翻译矩阵归 soda-component-adapter-starter-web， MVC
 * 绑定与端点映射归 soda-user-adapter，版本守卫路径归 soda-user-application。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@AutoConfigureMockMvc
@DisplayName("条件请求 E2E（H2）")
class ConditionalRequestE2ETest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.update("DELETE FROM user_archive");
    }

    private static String createBody() {
        return "{\"username\":\"conduser\",\"password\":\"password123\",\"nickname\":\"Cond\"}";
    }

    private static long idOf(String createResponseBody) {
        return com.jayway.jsonpath.JsonPath.<Number>read(createResponseBody, "$.id").longValue();
    }

    private String createUser() throws Exception {
        return mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DisplayName("创建响应带强验证器 ETag（域版本派生）")
    void should_returnEtag_when_create() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("响应 version 回带作下次 If-Match：连续两次条件 PATCH 均放行且 ETag 递增")
    void should_bumpEtag_when_chainedConditionalPatches() throws Exception {
        var id = idOf(createUser());

        mockMvc.perform(
                        patch("/api/users/{id}", id)
                                .header(HttpHeaders.IF_MATCH, "\"0\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nickname\":\"Cond1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.nickname").value("Cond1"));

        mockMvc.perform(
                        patch("/api/users/{id}", id)
                                .header(HttpHeaders.IF_MATCH, "\"1\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nickname\":\"Cond2\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.nickname").value("Cond2"));
    }

    @Test
    @DisplayName("If-Match 失配 → 412 Precondition Failed（RFC 9110 §15.5.13）")
    void should_reject_when_ifMatchMisses() throws Exception {
        var id = idOf(createUser());

        mockMvc.perform(
                        patch("/api/users/{id}", id)
                                .header(HttpHeaders.IF_MATCH, "\"999\"")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nickname\":\"Cond2\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.instance").value("/api/users/" + id))
                .andExpect(jsonPath("$.detail").value("If-Match 999 does not match current 0"));
    }

    @Test
    @DisplayName("资源不存在应返回 404（RFC 9110 §15.5.5；AIP-135 NOT_FOUND）")
    void should_returnNotFound_when_userMissing() throws Exception {
        mockMvc.perform(
                        patch("/api/users/{id}", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nickname\":\"Ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.instance").value("/api/users/999999"))
                .andExpect(jsonPath("$.detail").value("User not found: 999999"));
    }
}
