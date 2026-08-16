package com.soda.user.start;

import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.UserService;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeleteUserCommand;
import com.soda.user.api.command.RequestChangeMobileCodeCommand;
import com.soda.user.domain.EmailAuthAccount;
import com.soda.user.domain.SmsAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 写侧持久化端到端集成测试（H2 内存库 MODE=MySQL + Flyway）。
 * <p>
 * 验证 19 号票验收链路：create → findByXxx → requestChangeMobileCode（CC/S，2026-08-16 回归
 * UserAuthService，见 ADR-0026）→ changeMobile（I→P→USED，AFTER_COMMIT 监听器真实投递落 P）
 * → deregister（R 态落库）；
 * 单表账户派生组装、唯一约束、乐观锁冲突。
 * <p>
 * 测试方法不包事务——AFTER_COMMIT 事件依赖真实提交触发（监听器投递 → P 落库）。
 * 每测试前清表保证隔离。
 */
@SpringBootTest(classes = SodaUserApplication.class)
@DisplayName("写侧持久化端到端（H2）")
class PersistenceEndToEndTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private VerificationGateway verificationGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM verification");
        jdbcTemplate.update("DELETE FROM `user`");
        // 注销归档（D→R 测试写入；同内存库跨测试类复用，防未来归档断言隔离陷阱）
        jdbcTemplate.update("DELETE FROM user_archive");
    }

    @Test
    @DisplayName("create → findByUsername → 单表派生组装（password_hash + mobile 派生 Sms 账户）")
    void should_createAndReconstitute() {
        var created = userService.createUser(new CreateUserCommand(
                "alice", "Passw0rd!", "Alice", "13900139000", null, null, null));

        var byName = userGateway.findByUsername(new Username("alice")).orElseThrow();
        assertThat(byName.getId()).isEqualTo(new UserId(created.id()));
        // password_hash 列 → PasswordAuthAccount（BCrypt 哈希，非明文）
        assertThat(byName.getPasswordAccount().getPasswordHash().value()).isNotEqualTo("Passw0rd!");
        // mobile 列 → 派生 SmsAuthAccount
        assertThat(byName.getMobile()).contains(new Mobile("13900139000"));
        assertThat(byName.getAccounts()).hasSize(1);
        assertThat(byName.getAccounts().get(0)).isInstanceOf(SmsAuthAccount.class);
        assertThat(((SmsAuthAccount) byName.getAccounts().get(0)).getMobile())
                .isEqualTo(new Mobile("13900139000"));
        assertThat(byName.getState()).isEqualTo(UserState.E);
    }

    @Test
    @DisplayName("findByMobile / findByEmail 单表派生组装（Sms + Email 两账户）")
    void should_findByMobileAndEmail() {
        userService.createUser(new CreateUserCommand(
                "bobby", "Passw0rd!", "Bobby", "13900139001", "bob@test.com", null, null));

        var byMobile = userGateway.findByMobile(new Mobile("13900139001")).orElseThrow();
        assertThat(byMobile.getUsername()).isEqualTo(new Username("bobby"));
        assertThat(byMobile.getAccounts()).hasSize(2);
        assertThat(byMobile.getAccounts()).anyMatch(a -> a instanceof SmsAuthAccount);
        assertThat(byMobile.getAccounts()).anyMatch(a -> a instanceof EmailAuthAccount);

        var byEmail = userGateway.findByEmail(new com.soda.component.domain.types.Email("bob@test.com"))
                .orElseThrow();
        assertThat(byEmail.getUsername()).isEqualTo(new Username("bobby"));
    }

    @Test
    @DisplayName("端到端：requestChangeMobileCode（I→P，监听器投递）→ changeMobile（USED，mobile 更新 + Sms 账户联动）")
    void should_changeMobile_endToEnd() {
        var created = userService.createUser(new CreateUserCommand(
                "carol", "Passw0rd!", "Carol", "13900139002", null, null, null));
        var userId = created.id();

        // 第一步：发码 → 提交后 AFTER_COMMIT 监听器投递（log 桩）→ P 落库
        userAuthService.requestChangeMobileCode(new RequestChangeMobileCodeCommand(userId, "13900139111"));
        var pending = verificationGateway.findLatestBySourceAndStateIn(
                        VerificationSource.of("UCC", Long.toString(userId)),
                        List.of(VerificationState.P))
                .orElseThrow();
        assertThat(pending.getRecipient()).isEqualTo(new SmsRecipient(new Mobile("13900139111")));

        // 第二步：核验并换绑 → verification USED，user.mobile 更新 + Sms 账户替换联动
        var code = pending.getCode().code();
        userAuthService.changeMobile(new ChangeMobileCommand(userId, code));

        var changed = userGateway.findById(new UserId(userId)).orElseThrow();
        assertThat(changed.getMobile()).contains(new Mobile("13900139111"));
        var smsAccount = (SmsAuthAccount) changed.getAccounts().get(0);
        assertThat(smsAccount.getMobile()).isEqualTo(new Mobile("13900139111"));

        var used = verificationGateway.findLatestBySourceAndStateIn(
                        VerificationSource.of("UCC", Long.toString(userId)),
                        List.of(VerificationState.U))
                .orElseThrow();
        assertThat(used.getState()).isEqualTo(VerificationState.U);
    }

    @Test
    @DisplayName("deregister → R 态落库：键释放（三键 NULL）+ 归档原键 + 恢复补 REMOVED + 键可再注册 + 终态行拒写（ADR-0023）")
    void should_deregisterPersistRemovedState() {
        var created = userService.createUser(new CreateUserCommand(
                "dave", "Passw0rd!", "Dave", "13900139004", null, null, null));

        userService.disableUser(new com.soda.user.api.command.DisableUserCommand(created.id()));
        userService.deleteUser(new DeleteUserCommand(created.id()));

        // R 态可恢复，username 为领域默认值 REMOVED（键释放，ADR-0023）
        var removed = userGateway.findById(new UserId(created.id())).orElseThrow();
        assertThat(removed.getState()).isEqualTo(UserState.R);
        assertThat(removed.getUsername()).isEqualTo(Username.REMOVED);
        assertThat(removed.getMobile()).isEmpty();

        // DB 层三键置空（NULL 不参与唯一索引，多 R 行共存）
        var row = jdbcTemplate.queryForMap("SELECT username, mobile, email FROM `user` WHERE id = ?", created.id());
        assertThat(row.get("username")).isNull();
        assertThat(row.get("mobile")).isNull();
        assertThat(row.get("email")).isNull();

        // 归档表同事务写入原键快照（审计通道）
        var archived = jdbcTemplate.queryForMap(
                "SELECT id, username, mobile, email FROM user_archive WHERE id = ?", created.id());
        assertThat(archived.get("username")).isEqualTo("dave");
        assertThat(archived.get("mobile")).isEqualTo("13900139004");

        // 键已释放：同名用户名可再次注册（existsBy 不撞 R 行 NULL）
        var reRegistered = userService.createUser(new CreateUserCommand(
                "dave", "Passw0rd!", "Dave2", null, null, null, null));
        assertThat(reRegistered.id()).isNotEqualTo(created.id());

        // 终态行不可写（基础设施兜底：网关内裸抛 ISE 终态判定）：再次 save R 用户抛 IllegalStateException
        assertThatThrownBy(() -> userGateway.save(removed))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("唯一约束：同 username 二次保存抛 DataIntegrityViolationException（DB 兜底）")
    void should_uniqueUsernameViolation() {
        var first = User.createBuilder()
                .username(new Username("evelyn"))
                .nickname(new Nickname("Eve"))
                .passwordHash(new com.soda.component.domain.types.CredentialHash("$2a$10$stub"))
                .build();
        userGateway.save(first);

        var duplicate = User.createBuilder()
                .username(new Username("evelyn"))
                .nickname(new Nickname("Eve2"))
                .passwordHash(new com.soda.component.domain.types.CredentialHash("$2a$10$stub"))
                .build();
        assertThatThrownBy(() -> userGateway.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("唯一约束：同 mobile 二次保存抛 DataIntegrityViolationException（DB 兜底）")
    void should_uniqueMobileViolation() {
        userService.createUser(new CreateUserCommand(
                "mallory", "Passw0rd!", "Mallory", "13900139005", null, null, null));

        var duplicate = User.createBuilder()
                .username(new Username("mallory2"))
                .nickname(new Nickname("Mallory2"))
                .mobile(new Mobile("13900139005"))
                .passwordHash(new com.soda.component.domain.types.CredentialHash("$2a$10$stub"))
                .build();
        assertThatThrownBy(() -> userGateway.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("唯一约束：同 email 二次保存抛 DataIntegrityViolationException（DB 兜底）")
    void should_uniqueEmailViolation() {
        userService.createUser(new CreateUserCommand(
                "nancy", "Passw0rd!", "Nancy", null, "nancy@test.com", null, null));

        var duplicate = User.createBuilder()
                .username(new Username("nancy2"))
                .nickname(new Nickname("Nancy2"))
                .email(new com.soda.component.domain.types.Email("nancy@test.com"))
                .passwordHash(new com.soda.component.domain.types.CredentialHash("$2a$10$stub"))
                .build();
        assertThatThrownBy(() -> userGateway.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("乐观锁冲突：同 version 并发 save 后者抛 ObjectOptimisticLockingFailureException")
    void should_optimisticLockConflict() {
        var created = userService.createUser(new CreateUserCommand(
                "frank", "Passw0rd!", "Frank", null, null, null, null));
        var userId = new UserId(created.id());

        // 两个加载，version 均为 0（createUser 后未变更）
        var u1 = userGateway.findById(userId).orElseThrow();
        var u2 = userGateway.findById(userId).orElseThrow();

        // u2 先提交（version 0→1）
        u2.changeNickname(new Nickname("Frank_2"));
        userGateway.save(u2);

        // u1 持旧 version 提交 → 冲突
        u1.changeNickname(new Nickname("Frank_1"));
        assertThatThrownBy(() -> userGateway.save(u1))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("登录开关（V2）：停用手机号登录 → 持久化 → 恢复 active=false → 启用还原")
    void should_persistLoginSwitches() {
        var created = userService.createUser(new CreateUserCommand(
                "olivia", "Passw0rd!", "Olivia", "13900139006", "olivia@test.com", null, null));
        var userId = new UserId(created.id());

        // 停用手机号登录（支付宝/阿里云模式：mobile 仍是账号标识，仅登录方式关闭）
        var user = userGateway.findById(userId).orElseThrow();
        var smsAccount = (SmsAuthAccount) user.getAccounts().stream()
                .filter(a -> a.getAccountType().equals(com.soda.user.domain.types.SmsAuthAccountId.ACCOUNT_TYPE))
                .findFirst().orElseThrow();
        smsAccount.deactivate();
        userGateway.save(user);

        var reloaded = userGateway.findById(userId).orElseThrow();
        assertThat(reloaded.getMobile()).contains(new Mobile("13900139006")); // 标识仍在
        var reloadedSms = (SmsAuthAccount) reloaded.getAccounts().stream()
                .filter(a -> a.getAccountType().equals(com.soda.user.domain.types.SmsAuthAccountId.ACCOUNT_TYPE))
                .findFirst().orElseThrow();
        assertThat(reloadedSms.isActive()).isFalse(); // 登录方式已关闭

        // 启用还原
        reloadedSms.activate();
        userGateway.save(reloaded);

        var restored = userGateway.findById(userId).orElseThrow();
        var restoredSms = (SmsAuthAccount) restored.getAccounts().stream()
                .filter(a -> a.getAccountType().equals(com.soda.user.domain.types.SmsAuthAccountId.ACCOUNT_TYPE))
                .findFirst().orElseThrow();
        assertThat(restoredSms.isActive()).isTrue();
    }

    @Test
    @DisplayName("修改后 version 递增（恢复路径随持久化数据流转）")
    void should_versionIncrementOnSave() {
        var created = userService.createUser(new CreateUserCommand(
                "grace", "Passw0rd!", "Grace", null, null, null, null));
        var userId = new UserId(created.id());

        assertThat(userGateway.findById(userId).orElseThrow().getVersion().value()).isZero();

        userService.changeUsername(new com.soda.user.api.command.ChangeUsernameCommand(created.id(), "grace2"));

        assertThat(userGateway.findById(userId).orElseThrow().getVersion().value()).isEqualTo(1);
    }

    @Test
    @DisplayName("槽位唯一性（uk_active_key subject 维度）：同主体多 target 拒绝、跨主体同 target 允许（ADR-0025）")
    void should_activeSlotRejectSameSubjectDifferentTarget() {
        var created = userService.createUser(new CreateUserCommand(
                "heidi", "Passw0rd!", "Heidi", "13900139003", null, null, null));
        var userId = created.id();

        userAuthService.requestChangeMobileCode(new RequestChangeMobileCodeCommand(userId, "13900139112"));
        // AFTER_COMMIT 后存在未过期 P——槽位被占

        // 同主体换 target 重发：source 维度拒绝（每用户每场景至多一条活跃）
        assertThatThrownBy(() -> userAuthService.requestChangeMobileCode(
                new RequestChangeMobileCodeCommand(userId, "13900139113")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active verification already exists");

        // 跨主体同 target：允许（UCC 无 target 维度——码是持码凭证，输家收不到码无法消费）
        var other = userService.createUser(new CreateUserCommand(
                "heidi2", "Passw0rd!", "Heidi2", null, null, null, null));
        assertThatCode(() -> userAuthService.requestChangeMobileCode(
                new RequestChangeMobileCodeCommand(other.id(), "13900139112")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("VerificationGateway：无匹配验证返回 empty（findLatestBySourceAndStateIn 按 source 键控）")
    void should_findLatestBySourceAndStateInEmpty() {
        var created = userService.createUser(new CreateUserCommand(
                "ivan", "Passw0rd!", "Ivan", null, null, null, null));

        var result = verificationGateway.findLatestBySourceAndStateIn(
                VerificationSource.of("UCC", Long.toString(created.id())),
                List.of(VerificationState.P));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("更新路径（toPersistence 直接 save，无审计列搬运）：created_date 保持原值、last_modified_date 自动刷新（updatable=false + auditing，2026-08-15 修订）")
    void should_updatePreserveAuditColumns() throws Exception {
        var created = userService.createUser(new CreateUserCommand(
                "audit", "Passw0rd!", "Audit", "13900139005", null, null, null));

        var before = jdbcTemplate.queryForMap(
                "SELECT created_date, last_modified_date FROM `user` WHERE id = ?", created.id());
        assertThat(before.get("created_date")).isNotNull();
        assertThat(before.get("last_modified_date")).isNotNull();

        // DATETIME 秒精度（H2 MODE=MySQL）：跨秒确保 last_modified_date 可区分
        Thread.sleep(1100);

        // 更新路径：加载 → 改昵称 → save（toPersistence 直接，审计列 null → merge 自动处理）
        var user = userGateway.findByUsername(new Username("audit")).orElseThrow();
        user.changeNickname(new Nickname("Audit2"));
        userGateway.save(user);

        var after = jdbcTemplate.queryForMap(
                "SELECT created_date, last_modified_date, nickname FROM `user` WHERE id = ?", created.id());
        assertThat(after.get("nickname")).isEqualTo("Audit2");
        // created_date：基类 updatable=false → 不进 UPDATE → 保持原值（merge 的 null 不覆盖）
        assertThat(after.get("created_date")).isEqualTo(before.get("created_date"));
        // last_modified_date：auditing @PreUpdate 刷新（跨秒后不等于创建时刻）
        assertThat(after.get("last_modified_date")).isNotEqualTo(before.get("last_modified_date"));
    }
}
