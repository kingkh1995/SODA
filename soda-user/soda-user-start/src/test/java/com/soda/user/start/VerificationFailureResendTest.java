package com.soda.user.start;

import com.soda.component.domain.gateway.SmsSender;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.RequestChangeMobileCommand;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 投递失败自愈链路集成测试（H2）。
 * <p>
 * 用 {@code @MockitoBean} 把 {@link SmsSender} 替换为抛异常实现（投递契约违反，
 * ADR-0011）：send 失败 → 记录保持 I → 槽位预检（existsBySource）计入 I 拒绝
 * 重发 → 过期后惰性 DELETE 腾槽 → 重发成功（新 I 落库，发送仍失败——弱保证语义，机制见 ADR-0026）。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@DisplayName("投递失败自愈链路（H2）")
class VerificationFailureResendTest {

    @MockitoBean
    private SmsSender smsSender;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private VerificationGateway verificationGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static RequestChangeMobileCommand codeCommand(long userId, String target) {
        return new RequestChangeMobileCommand(userId, target);
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        // 注销归档（同内存库跨测试类复用，防未来归档断言隔离陷阱）
        jdbcTemplate.update("DELETE FROM user_archive");
    }

    @Test
    @DisplayName("send 失败 → I 滞留 → 槽位计入 I 拒绝重发 → 过期后惰性 DELETE 腾槽重发允许（新 I 落库，发送仍失败）")
    void should_resendAfterExpiredInitialized() {
        doThrow(new IllegalStateException("delivery failed")).when(smsSender).send(any(), any());
        var created = userService.createUser(new CreateUserCommand(
                "judy", "Passw0rd!", "Judy", "13900139004", null, null, null));
        var userId = created.id();
        var source = VerificationSource.of("UCC", Long.toString(userId));

        // 第一次发码：send 抛异常（投递契约违反）→ Spring 7 afterCompletion 吞异常（记录 ERROR 日志）、
        // 记录保持 I（send 失败未 markSent，PENDING 蕴含已送达不变量不破坏）
        assertThatCode(() -> userAuthService.requestChangeMobile(codeCommand(userId, "13900139114")))
                .doesNotThrowAnyException();

        // I 滞留：未过期 I 存在（占槽）→ existsBySource 计入 I 拒绝重发
        var stuck = verificationGateway.findLatestBySourceAndStateIn(
                        source, List.of(VerificationState.I))
                .orElseThrow();
        assertThat(stuck.getState()).isEqualTo(VerificationState.I);
        assertThatThrownBy(() -> userAuthService.requestChangeMobile(codeCommand(userId, "13900139115")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active verification already exists");

        // 过期（把 expire_at 改到过去；列按 UTC 存储，Hibernate TIMESTAMP_UTC 规范化）
        jdbcTemplate.update("UPDATE verification SET expire_at = ?",
                Instant.now().minus(10, ChronoUnit.MINUTES));
        // 重发：gateway.save 内同事务惰性 DELETE 过期 I 行（腾槽）→ 新 I 落库（发送仍失败）
        assertThatCode(() -> userAuthService.requestChangeMobile(codeCommand(userId, "13900139115")))
                .doesNotThrowAnyException();

        var latest = verificationGateway.findLatestBySourceAndStateIn(
                        source, List.of(VerificationState.I))
                .orElseThrow();
        assertThat(latest.getState()).isEqualTo(VerificationState.I);
        // 旧过期行已被惰性 DELETE 移除（行删除即释放 active_key，见 ADR-0026）
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM verification WHERE subject = ? AND scene = 'UCC'",
                Long.class, Long.toString(userId))).isEqualTo(1L);
    }
}
