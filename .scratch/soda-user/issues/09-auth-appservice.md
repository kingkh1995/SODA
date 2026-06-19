## Parent

`.scratch/soda-user/PRD.md`

## What to build

认证 AppService，不签发 Token。

**API 模块：**
- `AuthenticateCommand` — username 或 mobile 或 email，credential，accountType
- `AuthenticateResult` — userId, username, status（不含 Token）

**App 模块：**
- `AuthApplicationService.authenticate(AuthenticateCommand)`：
  - 根据 accountType 选择查找方式（PASSWORD → findByUsername，SMS → findByMobile 等）
  - 调用 `user.authenticate(accountType, credential)` — 委托给对应 Account
  - 验证失败抛业务异常（`BadCredentialsException` / `UserDisabledException` / `CodeExpiredException`）
  - 验证成功返回 `AuthenticateResult`

**Adapter 模块：**
- 无 Controller。（Token 签发已延期，没有 REST 端点需要暴露）

## Acceptance criteria

- [ ] Command + Result 编译通过
- [ ] AuthApplicationService 单元测试覆盖：
  - [ ] 密码认证成功
  - [ ] 密码错误
  - [ ] 用户已禁用
  - [ ] 验证码过期
  - [ ] 社交账号未绑定

## Blocked by

`04-user-account-entities.md` + `05-domain-events.md` + `06-user-crud-appservices.md` + `07-social-appservices.md` + `08-verification-code-appservices.md`
