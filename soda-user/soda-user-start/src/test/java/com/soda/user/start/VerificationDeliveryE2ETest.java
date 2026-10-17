package com.soda.user.start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soda.component.domain.gateway.SmsSender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验证码投递事务 E2E（完整上下文 + H2）：普通同步 {@code @EventListener} 在发布方事务内消费事件（ADR-0044）—— 发送成功则 I→P 落库、核验后换绑
 * U；发送异常向上传播，本次发码事务整体回滚。
 *
 * <p>只断言跨边界组合结果（test-conventions §2.2）：码值与落库状态无 HTTP 观察点，经 JdbcTemplate 读取； 监听器接线的 mock 级语义归
 * soda-user-application，槽位与腾槽机制归 soda-user-infrastructure。 {@code SmsSender} 为外部边界（test-conventions
 * §3 mock 边界行）故 mock。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@AutoConfigureMockMvc
@DisplayName("验证码投递事务 E2E（H2）")
class VerificationDeliveryE2ETest {

    @MockitoBean private SmsSender smsSender;

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.update("DELETE FROM user_archive");
    }

    private long createUser() throws Exception {
        var body =
                mockMvc.perform(
                                post("/api/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\"deliver\",\"password\":\"password123\",\"nickname\":\"Deliver\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return com.jayway.jsonpath.JsonPath.<Number>read(body, "$.id").longValue();
    }

    /** 读库取码（码值无 HTTP 观察点）：查询本身断言发码事务提交时 P 行已存在。 */
    private String pendingCode(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT code FROM verification WHERE subject = ? AND scene = 'UCC' AND state = 'P'",
                String.class,
                Long.toString(userId));
    }

    @Test
    @DisplayName("发送成功：发码事务内投递落 P，核验换绑后 U + mobile 联动更新")
    void should_completeChangeMobile_when_deliverySucceeds() throws Exception {
        var userId = createUser();

        mockMvc.perform(
                        post("/api/users/{id}:requestChangeMobile", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"newMobile\":\"13900139114\"}"))
                .andExpect(status().isOk());

        var code = pendingCode(userId);
        assertThat(code).isNotBlank();

        mockMvc.perform(
                        post("/api/users/{id}:changeMobile", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT state FROM verification WHERE subject = ? AND scene = 'UCC'",
                                String.class,
                                Long.toString(userId)))
                .isEqualTo("U");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT mobile FROM `user` WHERE id = ?", String.class, userId))
                .isEqualTo("13900139114");
    }

    @Test
    @DisplayName("发送失败：异常向上传播（500）且发码事务回滚，verification 零行")
    void should_rollbackVerification_when_deliveryFails() throws Exception {
        doThrow(new IllegalStateException("delivery failed")).when(smsSender).send(any(), any());
        var userId = createUser();

        mockMvc.perform(
                        post("/api/users/{id}:requestChangeMobile", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"newMobile\":\"13900139114\"}"))
                .andExpect(status().isInternalServerError());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM verification WHERE subject = ? AND scene = 'UCC'",
                                Long.class,
                                Long.toString(userId)))
                .isZero();
    }
}
