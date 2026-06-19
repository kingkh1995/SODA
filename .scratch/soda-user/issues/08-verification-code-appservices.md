## Parent

`.scratch/soda-user/PRD.md`

## What to build

验证码发送和校验的 AppService。

**API 模块：**
- `SendSmsCodeCommand` — mobile
- `SendEmailCodeCommand` — email
- `VerifyCodeCommand` — accountId, code

**App 模块：**
- `SendVerificationCodeAppService` — 根据 mobile/email 查找 User → 找到对应的 SmsAccount/EmailAuthAccount → `account.sendCode(codeGenerator, sender)` → save
- `VerifyCodeAppService` — 加载 User → 找到对应 Account → `account.verifyCode(inputCode)` → `account.useCode()` → save

`CodeGenerator` 和 `SmsSender`/`EmailSender` 通过 Gateway 注入，不持有具体实现。

## Acceptance criteria

- [ ] Command 编译通过
- [ ] AppService 单元测试（Mock Gateway）
- [ ] 测试：发送验证码后 VerificationCode 存入 Account
- [ ] 测试：校验成功后 verificationCode.used = true
- [ ] 测试：校验失败抛异常（不匹配 / 已过期）

## Blocked by

`06-user-crud-appservices.md`（需要 UserGateway 和基础 AppService 模式已建立）
