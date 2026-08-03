# PRD: soda-user — 用户身份与认证模块

**Status**: ready-for-agent

---

## Problem Statement

当前项目是一个 DDD 脚手架，已有领域共享模块（`soda-component-domain-starter`）和共享类型模块（`soda-component-support`），但没有任何业务模块的实体代码。第一个业务模块需要作为后续所有模块的样本代码，同时承担实际功能。

Yudao 参考实现了完整的 AdminUser 模块（用户 CRUD + 密码/短信/社交认证 + OAuth2 token + 权限），但采用 flat DO + 贫血 Service 架构。我们需要在 DDD 架构下重新设计，保留 Yudao 的所有业务能力，但采用充血领域模型。

## Solution

构建 `soda-user` 模块，采用 DDD 分层架构（COLA 4 层），作为第一个真实业务模块兼样本代码。核心设计：

- **User** 聚合根：用户身份信息（username, nickname, mobile, email, sex, avatar, status）
- **Account** 多态子实体体系：四种认证方式（密码/短信/邮箱验证码/社交）通过子类多态表达
- 认证行为在领域层完成（`PasswordAuthAccount.verify()`、`Verification.verify()`），基础设施依赖通过 Gateway 接口抽象
- 7 个子模块全建，当前交付范围 A：Domain 层 + ApplicationService 完整，Repository 待 infra 阶段

## User Stories

1. As an admin user, I want to create a new user with username/password, so that they can log into the system.
2. As an admin user, I want to update a user's profile (nickname, mobile, email, sex, avatar), so that user information stays current.
3. As an admin user, I want to change a user's username, so that the login name can be adjusted when needed.
4. As an admin user, I want to reset a user's password, so that they can regain access when they forget it.
5. As an admin user, I want to enable or disable a user account, so that access can be revoked or restored.
6. As an admin user, I want to delete a user, so that obsolete accounts can be removed from the system.
7. As an admin user, I want to view a paginated list of users with search/filter, so that I can manage users at scale.
8. As an admin user, I want to view a single user's detail information, so that I can review their profile.
9. As an admin user, I want to bind a social account (Gitee/DingTalk/WeChat) to a user, so that they can log in via third-party platforms.
10. As an admin user, I want to unbind a social account from a user, so that the login method can be removed.
11. As an authenticating user, I want to log in with username and password, so that I can access the system.
12. As an authenticating user, I want to log in with a mobile SMS verification code, so that I can access the system without a password.
13. As an authenticating user, I want to log in with an email verification code, so that I can access the system via email.
14. As an authenticating user, I want to log in with a social account (Gitee/DingTalk/WeChat), so that I can use an existing identity.
15. As an authenticating user, I want to send an SMS verification code to my mobile, so that I can proceed with SMS login.
16. As an authenticating user, I want to send an email verification code to my email, so that I can proceed with email login.
17. As an authenticating user, I want to receive meaningful error messages on failed login attempts (wrong password, disabled account, expired code), so that I understand what went wrong.
18. As an application developer, I want to find a user by username or mobile from the write side, so that the auth flow can load the aggregate.
19. As an application developer, I want to be notified when a user is created or their status changes, so that downstream modules can react (e.g., clear cache, init permissions).
20. As a developer of future modules, I want to follow the same module structure (7 sub-modules) and patterns (Entity/Aggregate/Gateway/AppService), so that architecture consistency is maintained.

## Implementation Decisions

### Module structure

Seven Gradle sub-modules under `soda-user/`:

| Sub-module | Package | Role |
|---|---|---|
| `soda-user-api` | `com.soda.user.api` | DTO / Command / Query / Feign 接口 |
| `soda-user-start` | `com.soda.user` | Spring Boot 启动入口，聚合 adapter + infra |
| `soda-user-adapter` | `com.soda.user.adapter` | Controller：DTO ↔ VO 转换 |
| `soda-user-application` | `com.soda.user.application` | ApplicationService：业务编排，按 Command 拆分 |
| `soda-user-domain` | `com.soda.user.domain` | Aggregate/Entity/DP/Gateway 接口 |
| `soda-user-infrastructure` | `com.soda.user.infrastructure` | Repository 实现，Mapper，PO |
| `soda-user-query-server` | `com.soda.user.queryserver` | 读服务，三层混装，复用 infra Mapper |

### Domain model

**User** (Aggregate root, extends `Aggregate<UserId>`)

Fields:
- `userId: UserId` (LongId) — 不可变
- `username: Username` — 4-30 位字母数字，可变（唯一性约束）
- `nickname: Nickname` — 显示名
- `mobile: Mobile` — 手机号，可选；也是 SmsAccountId 派生源
- `email: Email` — 邮箱，可选；也是 EmailAuthAccountId 派生源
- `sex: Sex` — `M`(Male) / `F`(Female)
- `avatar: Avatar` — 头像 URL
* `status: UserState` — `E`(Enabled) / `D`(Disabled)

**AuthAccount** (abstract Entity, extends `Entity<AuthAccountId>`)  

| Subclass | AccountId | Extra fields | Behavior |
|---|---|---|---|
| `PasswordAuthAccount` | `PasswordAuthAccountId(userId)` | `passwordHash` (CredentialHash) | `verify(RawCredential, CredentialHasher)`, `changePassword(RawCredential, CredentialHasher)` |
| `SmsAuthAccount` | `SmsAuthAccountId(mobile)` | `VerificationCodePolicy` | `getMobile()` |
| `EmailAuthAccount` | `EmailAuthAccountId(email)` | `VerificationCodePolicy` | `getEmail()` |
| `SocialAuthAccount` | `SocialAuthAccountId(socialType, openId)` | none (encoded in ID) | (identity mapping, no credential to verify) |

已迁移至独立的 `Verification` 聚合（见 ADR-0011）。
**VerificationCodePolicy** DP: record — `codeLength` (int) + `expiry` (Duration). Named defaults: `DEFAULT_SMS` (6 位/5 分钟), `DEFAULT_EMAIL` (8 位/30 分钟). Resolution order: per-account field → subclass DEFAULT_POLICY reference. (ServiceLoader SPI 暂未实现.)

### Gateway interfaces (domain layer)

- `UserGateway extends EntityGateway<User, UserId>` — `findByUsername(Username)`, `findByMobile(Mobile)`, `findByEmail(Email)`, `existsByUsername(Username)`, `existsByMobile(Mobile)`, `existsByEmail(Email)`
- `CredentialHasher extends Gateway` — `hash(RawCredential) → CredentialHash`, `matches(RawCredential, CredentialHash)` (定义在 support.gateway)
- `RandomStringGenerator extends Gateway` — `generate(PositiveInt) → RandomString` (定义在 support.gateway)
- `SmsSender extends Gateway` — `send(Mobile, SmsContent)` (定义在 support.gateway)
- `EmailSender extends Gateway` — `send(Email, EmailContent)` (定义在 support.gateway)

### Lifecycle rules

- Creating a User automatically creates a `PasswordAuthAccount` (one per User, required) — **已实现**（User.create）
- Setting `User.mobile` automatically creates or updates the `SmsAuthAccount`; clearing it removes the `SmsAuthAccount` — **已实现**（changeMobile）
- Same pattern for `User.email` and `EmailAuthAccount` — **已实现**（changeEmail）
- `SocialAuthAccount` is created/destroyed independently via explicit bind/unbind — **TODO: 待实现**（issue-07；User.addAccount 去重需按 socialType 重做）

### Persistence

Class Table Inheritance for Account:

- `system_user_account` (base table: account_id, user_id, account_type, active)
- `system_user_password_account` (extension: password_hash)
- `system_user_sms_account` (extension: verification fields, code_length, code_expiry)
- `system_user_email_account` (extension: verification fields, code_length, code_expiry)
- `system_user_social_account` (extension: social_type, open_id as part of composite key)

The base table supports `UserGateway.findByUserId()` batch loading. Repository uses `account_type` discriminator for polymorphic dispatch. All Account writes within a single transaction via User aggregate.

### Domain events

Events defined in `soda-user-domain`:
- `UserCreatedEvent` — **implemented**
- `UserStateChangedEvent` — **implemented**
- `PasswordChangedEvent` — **implemented**（User.changePassword 注册）
- `UserRemovedEvent` — **implemented**（UserServiceImpl.deleteUser 直接 publish）

> 修订（2026-08-03）：`AccountBoundEvent` / `AccountUnboundEvent` 已删除——社交绑定功能未实现，事件定义随之下架；社交绑定设计时（issue-07）重新定义所需事件。

### DTO / VO separation

`soda-user-api` contains shared DTO and Command types. `soda-user-adapter` defines separate VO types and an Assembler for DTO ↔ VO conversion. This follows the pattern from ADR-0001.

### ApplicationService 粒度与边界

**已重构（2026-07-30）**：修改为 2 个 Service 接口 + `UserAuthService`，方法名按 ADR-0010 显式意图。

```
soda-user-api:   UserService (interface)
                  UserAuthService (interface)
soda-user-application:   UserServiceImpl + UserAuthServiceImpl
```

UserService 方法：
- `createUser(CreateUserCommand)` → `UserDTO`
- `updateUser(UpdateUserCommand)`
- `deleteUser(DeleteUserCommand)`
- `disableUser(DisableUserCommand)`
- `enableUser(EnableUserCommand)`
- `changeUsername(ChangeUsernameCommand)`

UserAuthService 方法：
- `changePassword(ChangePasswordCommand)`
- `verifyMobile(VerifyMobileCommand)`
- `changeMobile(ChangeMobileCommand)`
- `verifyEmail(VerifyEmailCommand)`
- `changeEmail(ChangeEmailCommand)`

**Adapter → App 边界**：`build.gradle` 用 `runtimeOnly project(':soda-user-application')`（运行时 classpath，编译期不引用 app 类），ModulithTest 强制 adapter 代码不得 import app 模块的类。

**覆盖范围**：当前 11 个方法。后续扩展在此接口上追加，不超过 10 个方法/接口。

**备选**：`CommandExecutor` 接口和 `DomainFactory` 保留为未来复杂场景备用。


### Token / Session

**Deferred to a future module.** 登录验证（`authenticate`）与 Token 创建/刷新/校验/登出均不在本次范围（记入待办）。

### Settings.gradle

```groovy
include 'soda-user'
include 'soda-user:soda-user-api'
include 'soda-user:soda-user-start'
include 'soda-user:soda-user-adapter'
include 'soda-user:soda-user-application'
include 'soda-user:soda-user-domain'
include 'soda-user:soda-user-infrastructure'
include 'soda-user:soda-user-query-server'
```

## Testing Decisions

### What makes a good test

- **Test external behavior, not implementation details.** DP test verifies construction, validation failure, equality, serialization, and comparison — not internal field layout. Aggregate test verifies business rule outcomes (e.g., `user.authenticate()` succeeds for correct password, fails for wrong password), not which private method was called.
- **Use the highest seam possible.** Unit-test domain logic with zero infrastructure (pure Java, no Spring context). Use mocked Gateways for ApplicationService tests.
- **Domain events are tested by asserting they appear in `flushEvents()` after the triggering business method**, not by verifying the event bus was called.

### Prior art

| Test type | Existing example | Location |
|---|---|---|
| DP unit tests | `UUIdTest`, `EmailTest`, `VersionTest`, `WanYuanTest` | `soda-component-support/src/test/...` |
| ModulithTest | `ModulithTest` | `soda-component-support/src/test/...` |

### Test plan

**Phase 1 — DP unit tests** (infrastructure-free, JUnit 5 only)
- `UserIdTest`, `UsernameTest`, `NicknameTest`, `MobileTest`, `SexTest`, `AvatarTest`
- `AuthAccountTypeTest`, `SocialTypeTest`
- `VerificationCodePolicyTest`（验证码聚合测试见 `soda-user-domain` 的 `VerificationTest`）
- `PasswordAccountIdTest`, `SmsAccountIdTest`, `EmailAuthAccountIdTest`, `SocialAccountIdTest`

Each tests: valid construction, invalid construction (throws), equality, Jackson serialization round-trip, `compareTo()`（枚举 DP 使用 Java 内置比较）。

**Phase 2 — Domain Entity/Aggregate tests** (infrastructure-free, mocked Gateways)
- `UserTest` — creation (auto-creates `PasswordAuthAccount` when `passwordHash` provided), changeXxx 变更方法（含 changeMobile/changeEmail 拒绝未验证/用户不匹配路径、changeUsername），Jackson round-trip for all 4 account types, `flushEvents()` contains `UserCreatedEvent` (lazy entityId resolution)
- `PasswordAuthAccountTest` — `verify()` matches/mismatch, `verify()` delegates to `CredentialHasher`, `changePassword()` updates hash
- `SmsAuthAccountTest` — 构造/恢复（策略必传、默认策略）、类型派发、Jackson round-trip
- `EmailAuthAccountTest` — same pattern as SmsAuthAccount
- `SocialAuthAccountTest` — identity test (creation, equality, Jackson round-trip)

**Phase 3 — ApplicationService tests** (mocked Gateways)
- `UserServiceImplTest` — create happy path + uniqueness violation + disable/enable + events
- `UserAuthServiceImplTest` — verifyMobile/verifyEmail 落库 PENDING + 发送；changeMobile/changeEmail 成功路径（user 保存 + verification 落库 USED）；拒绝路径（错码/过期/user 不存在/无 pending verification，不落库）

**Phase 4 — Modulith structural test**
- `ModulithTest` in each sub-module with `@ApplicationModule` annotation
- Template from `soda-component-support/ModulithTest.java`

**Phase 5 — Repository integration test** (future phase, depends on DB setup)
- `UserGatewayImplTest` — CRUD with in-memory DB (H2)
- Polymorphic Account persistence (base table + extension tables)

### Test module placement

- DP + Domain tests: `soda-user-domain/src/test/`
- ApplicationService tests: `soda-user-application/src/test/`
- Repository tests: `soda-user-infrastructure/src/test/`
- ModulithTest: each sub-module's `src/test/`

## Out of Scope

- **Token / OAuth2 session management** (access token, refresh token, logout). The auth flow ends at `User.authenticate()` returning the User. Token creation is deferred.
- **Permission / Role / Menu**. These belong to a future `soda-system` module.
- **Dept / Post / organizational hierarchy**. Not part of `soda-user`.
- **User import/export (Excel)**. Yudao has this as an admin convenience feature; not core domain.
- **Admin UI / frontend**. API-only. The adapter layer exposes REST endpoints for the Vben/Vue admin UI to consume, but UI development is separate.
- **Rate limiting / brute-force protection**. Login rate limiting is an infrastructure concern to be added later.
- **Multi-tenancy**. Yudao has tenant support; deferred for now.

## Further Notes

- All Yudao-specific implementation details (SSH, SMS channel adapters, specific social SDKs) are infrastructure-layer concerns. The domain layer references them only through Gateway interfaces.
- `soda-user-query-server` is intentionally layered differently (traditional 3-layer, no DDD) per CQRS decision in ADR-0001. It reuses Mapper/DAO from `soda-user-infrastructure` but does NOT depend on `soda-user-domain` or `soda-component-support`.
- Username is mutable. The uniqueness constraint on username is enforced via `UserGateway.existsByUsername()` at the ApplicationService level, with a DB unique index as a safety net.
- 手机/邮箱换绑验证：验证码由 `Verification` 实体管理（scene=CC）。AppService 通过 `RandomStringGenerator` 生成验证码，创建 Verification 实体落库并发送；确认变更时经 `CredentialChangeService` 编排 verify → change → use，先存 user 再存 verification；失败路径（错码/过期）不落库（实体无变更）。
