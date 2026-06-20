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
- 认证行为在领域层完成（`PasswordAccount.verify()`、`Account.sendCode()`），基础设施依赖通过 Gateway 接口抽象
- 7 个子模块全建，当前交付范围 A：Domain 层完整，AppService 留桩

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
| `soda-user-app` | `com.soda.user.app` | ApplicationService：业务编排，按 Command 拆分 |
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
- `sex: Sex` — MALE / FEMALE
- `avatar: Avatar` — 头像 URL
- `status: UserStatus` — ENABLED / DISABLED
- `accounts: List<Account>` — 子实体集合

Key domain methods: `authenticate(AccountType, credential)`, `changeUsername(Username)`, `createPasswordAccount(PasswordEncoder)`, `addSocialAccount(SocialType, openId)`, `removeAccount(AccountId)`, `setMobile(Mobile)`, `setEmail(Email)`, `changeStatus(UserStatus)`

**Account** (abstract Entity)

| Subclass | AccountId | Extra fields | Behavior |
|---|---|---|---|
| `PasswordAccount` | `PasswordAccountId(userId)` | `passwordHash` (BCrypt hash) | `verify(rawPassword, PasswordEncoder)` |
| `SmsAccount` | `SmsAccountId(mobile)` | `VerificationCode?`, `VerificationCodePolicy` | `sendCode(CodeGenerator, SmsSender)`, `verifyCode(code)`, `useCode()` |
| `EmailAuthAccount` | `EmailAuthAccountId(email)` | `VerificationCode?`, `VerificationCodePolicy` | `sendCode(CodeGenerator, EmailSender)`, `verifyCode(code)`, `useCode()` |
| `SocialAccount` | `SocialAccountId(socialType, openId)` | none (encoded in ID) | (identity mapping, no credential to verify) |

**VerificationCode** DP: encapsulates `code`, `expireAt`, `used` (immutable after creation). Methods: `isExpired()`, `isUsed()`, `verify(inputCode)`, `use()`. Created inside `Account.sendCode()`.

**VerificationCodePolicy** DP: `codeLength` + `expiry` (Duration). Resolution order: per-account override → ServiceLoader SPI → subclass static `DEFAULT_POLICY`.

### Gateway interfaces (domain layer)

- `UserGateway extends EntityGateway<User, UserId>` — `findByUsername(Username)`, `findByMobile(Mobile)`, `existsByUsername(Username)`, `existsByMobile(Mobile)`, `existsByEmail(Email)`
- `PasswordEncoder extends Gateway` — `encode(raw)`, `matches(raw, hash)`
- `CodeGenerator extends Gateway` — `generate(length)`
- `SmsSender extends Gateway` — `send(mobile, code)`
- `EmailSender extends Gateway` — `send(email, code)`

### Lifecycle rules

- Creating a User automatically creates a `PasswordAccount` (one per User, required)
- Setting `User.mobile` automatically creates or updates the `SmsAccount`; clearing it removes the `SmsAccount`
- Same pattern for `User.email` and `EmailAuthAccount`
- `SocialAccount` is created/destroyed independently via explicit bind/unbind

### Persistence

Class Table Inheritance for Account:

- `system_user_account` (base table: account_id, user_id, account_type, active)
- `system_user_password_account` (extension: password_hash)
- `system_user_sms_account` (extension: verification fields, code_length, code_expiry)
- `system_user_email_account` (extension: verification fields, code_length, code_expiry)
- `system_user_social_account` (extension: social_type, open_id as part of composite key)

The base table supports `UserGateway.findByUserId()` batch loading. Repository uses `account_type` discriminator for polymorphic dispatch. All Account writes within a single transaction via User aggregate.

### Domain events

Events defined in `soda-user-api`:
- `UserCreatedEvent` — **implemented in current phase**
- `UserStatusChangedEvent` — **implemented in current phase**
- `PasswordChangedEvent` — definition only, publish TODO
- `AccountBoundEvent` — definition only, publish TODO
- `AccountUnboundEvent` — definition only, publish TODO
- `UserRemovedEvent` — definition only, publish TODO

### DTO / VO separation

`soda-user-api` contains shared DTO and Command types. `soda-user-adapter` defines separate VO types and an Assembler for DTO ↔ VO conversion. This follows the pattern from ADR-0001.

### ApplicationService granularity

COLA-style: one AppService per Command.

- `UserCreateAppService` — accepts `CreateUserCommand`, returns `UserId`
- `UserUpdateAppService` — accepts `UpdateUserCommand`
- `UserDeleteAppService` — accepts `UserId`
- `UserPasswordAppService` — accepts `UpdatePasswordCommand`
- `UserStatusAppService` — accepts `UpdateUserStatusCommand`
- `UserProfileAppService` — accepts `UpdateProfileCommand`
- `UsernameChangeAppService` — accepts `ChangeUsernameCommand`
- `SendVerificationCodeAppService` — accepts `SendSmsCodeCommand` / `SendEmailCodeCommand`
- `SocialBindAppService` — accepts `BindSocialCommand`
- `SocialUnbindAppService` — accepts `UnbindSocialCommand`

### Token / Session

**Deferred to a future module.** Current phase ends at `authenticate()` returning the User aggregate. Token creation, refresh, validation, and logout are out of scope (recorded as TODO).

### Settings.gradle

```groovy
include 'soda-user'
include 'soda-user:soda-user-api'
include 'soda-user:soda-user-start'
include 'soda-user:soda-user-adapter'
include 'soda-user:soda-user-app'
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
- `UserIdTest`, `UsernameTest`, `NicknameTest`, `MobileTest`, `SexTest`, `AvatarTest`, `UserStatusTest`
- `AccountTypeTest`, `SocialTypeTest`
- `VerificationCodePolicyTest`, `VerificationCodeTest`
- `PasswordAccountIdTest`, `SmsAccountIdTest`, `EmailAuthAccountIdTest`, `SocialAccountIdTest`

Each tests: valid construction, invalid construction (throws), equality, Jackson serialization round-trip, `compareTo()`.

**Phase 2 — Domain Entity/Aggregate tests** (infrastructure-free, mocked Gateways)
- `UserTest` — creation, `authenticate()` dispatch to correct Account, `changeUsername()` (unique constraint enforced via callback), `setMobile()` (auto-creates SmsAccount), `setEmail()` (auto-creates EmailAuthAccount), `addSocialAccount()`, `removeAccount()`, `flushEvents()` contains expected events
- `PasswordAccountTest` — `verify()` matches/mismatch, `verify()` delegates to `PasswordEncoder`
- `SmsAccountTest` — `sendCode()` creates VerificationCode + calls SmsSender, `verifyCode()` success/failure/expired, `useCode()` marks used
- `EmailAuthAccountTest` — same pattern as SmsAccount
- `SocialAccountTest` — identity test (creation, equality)

**Phase 3 — ApplicationService tests** (Spring boot test, mocked Gateways)
- `UserCreateAppServiceTest` — happy path + uniqueness violation + event published
- `UserStatusAppServiceTest` — disable/enable + UserStatusChangedEvent
- Remaining AppServices: basic orchestration verification

**Phase 4 — Modulith structural test**
- `ModulithTest` in each sub-module with `@ApplicationModule` annotation
- Template from `soda-component-support/ModulithTest.java`

**Phase 5 — Repository integration test** (future phase, depends on DB setup)
- `UserGatewayImplTest` — CRUD with in-memory DB (H2)
- Polymorphic Account persistence (base table + extension tables)

### Test module placement

- DP + Domain tests: `soda-user-domain/src/test/`
- ApplicationService tests: `soda-user-app/src/test/`
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
- The `sendCode()` domain method on SmsAccount/EmailAuthAccount both generates the code and dispatches it via a Gateway. This keeps verification code generation (length, expiry) as domain policy while leaving the actual SMS/email delivery to infrastructure.
