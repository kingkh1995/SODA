## Parent

`.scratch/soda-user/PRD.md`

## Status

**待办** — 评审 commit `0b9969b`（未推送）后续项。

## What is deferred

评审修复（Q9/Q14-g/h/i 决定）中确认本次不做、留待后续的接线与基建工作：

### 1. 异常处理接线（Q9）

无 `@RestControllerAdvice` / `ErrorInfo` 接线。当前 Controller 直接抛领域异常（`IllegalArgumentException` / `IllegalStateException`），HTTP 层返回默认 500，不遵循 ADR-0013 的 `ErrorInfo`（reason/domain/metadata）信封。

- 建立统一异常处理：领域异常 → AIP-193 `ErrorInfo` 响应
  - **异常分类已定型（2026-08-01，ADR-0015）**：防御（NPE/ISE 无消息，null → NPE、非 null 条件 → 手写 `throw new IllegalStateException()`）+ 参数校验（IAE 带消息）；操作语义：set-state（`disable`/`enable`）幂等 no-op、transition（`verify`/`use`）严格；`changeMobile` 同值换绑抛 IAE（决策 B）。既有反例已随 ADR 清理（Verification、User、Identifiable、UserDTOConvertor、EmailAuthAccount、SmsAuthAccount）。接线仍按本条 deferred：IAE → 400 `INVALID_ARGUMENT`，NPE/ISE → 500 `INTERNAL`，`MethodArgumentNotValidException` → 400；409/429 语义暂不启用。
- `@Email` 校验收紧：`VerifyEmailRequest`/`CreateUserRequest` 目前用 `@Email`，无进一步域名级校验（领域 `Email` DP 更严）

### 2. Email/SmsVerification 去重（Q14-h/g）— 已解决（2026-08-01）

**决策：发送幂等**（ADR-0011 已同步）。`verifyMobile`/`verifyEmail` 在请求前检查同 (userId, scene=CC)
是否已存在**未过期且 target 相同**的 PENDING 验证：存在则直接返回（不重发、不新建），否则新建 + 发送 + 落库。
由此同一 (userId, scene) 至多存在一条未过期 pending，`findPendingByUserAndScene` 的语义即为「最新一条待验证聚合」。

- 原候选（旧 pending 作废/唯一索引 upsert）不再需要；**并发双请求的兜底唯一索引**移至 infra 阶段（见 #3）

### 3. 基础设施 Gateway 实现（infra 阶段）

当前 `soda-user-infrastructure` 仅有 `package-info`。以下 bean 缺失导致 `SodaUserApplication` 无法完整启动（构建与测试已全绿，启动冒烟止步于此）：

- `UserGateway`、`VerificationGateway`（Repository 实现，含 Account 多态持久化）
- `CredentialHasher`、`RandomStringGenerator`、`SmsSender`、`EmailSender`
- **并发兜底**：幂等检查是应用层逻辑，并发请求可能同时通过 → 为 (userId, scene) 未过期 PENDING 加部分唯一索引

验收标准：`SodaUserApplication` 启动 + verifyMobile→changeMobile / verifyEmail→changeEmail 真库端到端落库（PENDING→USED）。当前等价验证：`UserAuthServiceImplTest`（mock 网关，断言落库语义）+ `SpringDomainEventBusConfigurationTest`（事件总线 Bean 装配，已通过）。

### 4. changePassword 旧密码校验（Q14-d 文档矛盾）

`ChangePasswordRequest`/`ChangePasswordCommand` 仅含 `newPassword`，无旧密码校验（ADR-0010 已同步为 `{ newPassword }`）。若产品要求旧密码验证，需加 `oldPassword` 字段 + `PasswordAuthAccount.verify(RawCredential, CredentialHasher)` 前置校验。

## Review of commit 8f00c60 — decisions (2026-08-03)

评审 `8f00c60`（相对 `b76da2a`）的修复决定，已全部落地：

| # | 决定 | 落地 |
|---|---|---|
| D1 | `RandomString.matches` 不恢复，删测试用例 | RandomStringTest 删两用例 |
| D2 | AccountBound/Unbound 事件维持删除，规范同步 | PRD/issue-05 事件表更新，User.java 悬空注释删除 |
| D3 | addAccount 去重暂不按 socialType 重做 | issue-07 备注去重注意 |
| D4 | VerificationGateway「最新」契约 = 按 expireAt 倒序 | gateway javadoc + ADR-0011 同步 |
| D5 | 新增 `Guard.state(boolean)`（无消息 ISE），其余不动 | domain-types `com.soda.component.domain.util.Guard` |
| D6 | Result 保留 NON_NULL，ErrorInfo 同步加注解，ADR-0013 示例更新 | ErrorInfo/Result/ADR-0013 |
| D7 | user-api 保留 `api domain-types` 传递（adapter 不自声明） | user-api/adapter build.gradle 注释+依赖 |
| D8 | UserUpgradeConsumer 恢复 `@TransactionalEventListener` | 已恢复（AFTER_COMMIT + fallbackExecution） |
| D9 | `Command.validateOnly()` 保留，issue 跟踪 | 本表即跟踪；AIP-163 接线时实现 |
| D10 | `changePassword` 缺密码账户保留裸 NSE，不处理 | 不改 |
| D11 | 规范漂移以代码为准全量对齐 | PRD/issue-05/07/17/ADR-0005/0011/0013/0015 |
| D12 | 机械修复批（除日志去敏外全做） | junk 文件删除+gitignore；fixtures E/D；Modulith 补 application；javadoc 死链/文档；Sms/EmailVerification 可见性；expiry 边界测试；publishAll null 守卫；ErrorInfo JSpecify |

> **不做（用户决定）**：Controller 请求日志敏感字段去敏——`createUser`/`changePassword` 等继续全量打印 request（含明文 password/newPassword/code）。风险已知（评审 BLOCK 项），后续如启用需：日志仅打 username/userId，或按字段脱敏。
