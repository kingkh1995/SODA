## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成** — 代码合并至 `main`（未推送）。

## What was built

`soda-user-domain` 中的实体和聚合根。

**User** (Aggregate root, extends `Aggregate<UserId>`)：
- 字段：userId, username, nickname, mobile, email, sex, avatar, status, accounts
- 双 Builder 模式：`createBuilder()` — 不含 ID（服务端生成）+ `active=TRUE`；`restoreBuilder()` — 全字段显式传入
- 自动生成 `PasswordAuthAccount`（`active=TRUE`）
- `authenticate(AuthAccountType, RawPassword)` → sealed 模式匹配，委托给对应 AuthAccount
- `findAccount(Predicate<AuthAccount>)` / `findAccount(AuthAccountType)` 查找账户
- `getAccounts()` 返回不可修改视图
- 注册事件：`UserCreatedEvent`（构造时）、`UserStatusChangedEvent`（状态变更时）

**PasswordAuthAccount** (Entity, extends `AuthAccount<PasswordAuthAccountId>`)
**SmsAuthAccount** (Entity, extends `AuthAccount<SmsAuthAccountId>`)
**EmailAuthAccount** (Entity, extends `AuthAccount<EmailAuthAccountId>`)
**SocialAuthAccount** (Entity, extends `AuthAccount<SocialAuthAccountId>`)

各子类提供双 Builder 模式（`createBuilder` / `restoreBuilder`），Jackson 注解通过构造参数 `@JsonProperty("fieldName")` + `@JsonCreator(mode = PROPERTIES)` 保证序列化稳定性。

## Acceptance criteria

- [x] 所有实体编译通过
- [x] 单元测试覆盖：
  - User 构造时自动创建 PasswordAuthAccount
  - `setMobile()` / `setEmail()` 自动创建对应 AuthAccount
  - `authenticate()` 分发到正确的 AuthAccount 子类
  - PasswordAuthAccount.verify() 委托 PasswordEncoder
  - SmsAuthAccount.sendCode() 生成 VerificationCode + 调用 SmsSender
  - SmsAuthAccount.verifyCode() 成功/失败/过期
  - SocialAuthAccount 创建和相等性
  - `flushEvents()` 包含预期事件
- [x] 测试不依赖 Spring 上下文和 DB
