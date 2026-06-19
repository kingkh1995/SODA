## Parent

`.scratch/soda-user/PRD.md`

## What to build

`soda-user-domain` 中的实体和聚合根。

**User** (Aggregate root, extends `Aggregate<UserId>`)：
- 字段：userId, username, nickname, mobile, email, sex, avatar, status, accounts
- 构造器：`User(Username, Nickname, PasswordEncoder)` — 创建时自动生成 PasswordAccount
- 工厂方法/便捷方法：`createPasswordAccount(PasswordEncoder)`, `addSocialAccount(SocialType, openId)`, `removeAccount(AccountId)`, `setMobile(Mobile)` → 自动创建/删除 SmsAccount，`setEmail(Email)` → 自动创建/删除 EmailAuthAccount，`changeStatus(UserStatus)`, `changeUsername(Username)`, `authenticate(AccountType, credential)` → 委托给对应 Account

**PasswordAccount** (Entity, extends `Account<PasswordAccountId>`)：
- `passwordHash: String`（由 PasswordEncoder 编码后的值）
- `verify(rawPassword, PasswordEncoder): boolean` — 域方法

**SmsAccount** (Entity, extends `Account<SmsAccountId>`)：
- `verificationCode: VerificationCode?`
- `verificationCodePolicy: VerificationCodePolicy?`（覆写用）
- `static DEFAULT_POLICY = new VerificationCodePolicy(6, 5min)`
- `sendCode(CodeGenerator, SmsSender)` — 生成验证码 + 存储 + 发送
- `verifyCode(String): boolean` — 校验验证码
- `useCode()` — 标记已使用

**EmailAuthAccount** (Entity, extends `Account<EmailAuthAccountId>`)：
- 同 SmsAccount 但不含 `SmsSender`，替换为 `EmailSender`
- `static DEFAULT_POLICY = new VerificationCodePolicy(8, 30min)`

**SocialAccount** (Entity, extends `Account<SocialAccountId>`)：
- 无额外字段（socialType + openId 编码在 SocialAccountId 中）
- 纯标识映射，无密码可验证

## Acceptance criteria

- [ ] 所有实体编译通过
- [ ] 单元测试覆盖：
  - User 构造时自动创建 PasswordAccount
  - `setMobile()` / `setEmail()` 自动创建对应 Account
  - `authenticate()` 分发到正确的 Account 子类
  - PasswordAccount.verify() 委托 PasswordEncoder
  - SmsAccount.sendCode() 生成 VerificationCode + 调用 SmsSender
  - SmsAccount.verifyCode() 成功/失败/过期
  - SocialAccount 创建和相等性
  - `flushEvents()` 包含预期事件
- [ ] 测试不依赖 Spring 上下文和 DB

## Blocked by

`02-domain-primitives.md` + `03-gateway-interfaces.md`
