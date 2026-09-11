package com.soda.user.start;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 条件请求传输契约完整 MVC 验证（见 ADR-0039）。
 * <p>
 * 覆盖线上行为而非装配写法：写响应带强验证器 {@code ETag}（域版本派生）、{@code If-Match} 命中放行、
 * 失配 412、缺失放行。跑在完整上下文，确保 advice 与参数解析器真实生效。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@AutoConfigureMockMvc
@DisplayName("条件请求 ETag / If-Match（完整 MVC）")
class ConditionalRequestMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static String createBody(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"password123\",\"nickname\":\"Cond\"}";
    }

    private static long idOf(String createResponseBody) {
        return com.jayway.jsonpath.JsonPath.<Number>read(createResponseBody, "$.id").longValue();
    }

    private static String uniqueName(String prefix) {
        return prefix + System.nanoTime() % 100000;
    }

    /**
     * 新建用户并返回其 id；调用方自行断言响应头。
     */
    private long createUser(String username) throws Exception {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        var created = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(created);
    }

    @Test
    @DisplayName("创建响应带强验证器 ETag（域版本派生，含引号）")
    void should_returnEtag_when_create() throws Exception {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(uniqueName("etag"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("If-Match 命中 → 放行，且响应 ETag 为递增后的版本")
    void should_passThrough_when_ifMatchHits() throws Exception {
        var id = createUser(uniqueName("hit"));

        mockMvc.perform(patch("/api/users/{id}", id)
                        .header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Cond2\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.nickname").value("Cond2"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("If-Match 失配 → 412 Precondition Failed（RFC 9110 §15.5.13）")
    void should_reject_when_ifMatchMisses() throws Exception {
        var id = createUser(uniqueName("miss"));

        mockMvc.perform(patch("/api/users/{id}", id)
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Cond2\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.instance").value("/api/users/" + id))
                .andExpect(jsonPath("$.detail").value("If-Match 999 does not match current 0"));
    }

    @Test
    @DisplayName("If-Match 缺失 → 放行（缺席不构成并发声明）")
    void should_passThrough_when_ifMatchAbsent() throws Exception {
        var id = createUser(uniqueName("absent"));

        mockMvc.perform(patch("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Cond2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Cond2"));
    }
}
