## Parent

`.scratch/soda-user/PRD.md`

## Status

**已完成** — 代码合并至 `main`（未推送）。

## What was built

`soda-user-domain` 中的实体和聚合根。

**User** (Aggregate root, extends `Aggregate<UserId>`)：
- 字段：userId, username, nickname, mobile, email, sex, avatar, status, accounts（均为 @Nullable 除 username/nickname）
- 双 Builder 模式：`createBuilder()` — 无 ID（服务端生成），accounts 为空；`restoreBuilder()` — 全字段显式传入（含 accounts）
- 当前**不**自动创建任何 AuthAccount（PasswordAuthAccount 等需外部显式创建后通过 restoreBuilder 传入）
- `authenticate(AuthAccountType, String, CredentialHasher)` → sealed 模式匹配，委托给对应 AuthAccount
- `findAccount(Predicate<AuthAccount>)` 查找账户
- `getAccounts()` 返回不可修改视图
- 注册事件：`UserCreatedEvent`（构造时）。`UserStatusChangedEvent` **待变更方法实现后添加**。

**PasswordAuthAccount** (Entity, extends `AuthAccount<PasswordAuthAccountId>`) — 双 Builder，`verify(RawCredential, CredentialHasher)`，`changePassword(RawCredential, CredentialHasher)`
**SmsAuthAccount** (Entity, extends `AuthAccount<SmsAuthAccountId>`) — 双 Builder，`replaceCode(VerificationCode)`（含过期保护），`verifyCode(RandomString)`，`useCode()`
**EmailAuthAccount** (Entity, extends `AuthAccount<EmailAuthAccountId>`) — 与 SmsAuthAccount 相同模式
**SocialAuthAccount** (Entity, extends `AuthAccount<SocialAuthAccountId>`) — 双 Builder，纯标识映射，`getSocialType()` / `getOpenId()` 从 ID 中提取


## Acceptance criteria

- [x] 所有实体编译通过
- [x] 单元测试覆盖：
  - User 创建时不自动创建任何 AuthAccount（需通过 restoreBuilder 显式传入）
  - `authenticate()` 分发到正确的 AuthAccount 子类（sealed 模式匹配）
  - PasswordAuthAccount.verify() 委托 CredentialHasher
  - SmsAuthAccount.replaceCode() 替换逻辑（过期保护）
  - SmsAuthAccount.verifyCode() 成功/失败/过期
  - SocialAuthAccount 创建和相等性
  - Jackson 序列化/反序列化所有 4 种 AuthAccount 子类
  - `flushEvents()` 包含 UserCreatedEvent（entityId 延迟求值）
- [x] 测试不依赖 Spring 上下文和 DB
