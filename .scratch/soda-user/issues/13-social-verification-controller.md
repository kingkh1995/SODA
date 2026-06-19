## Parent

`.scratch/soda-user/PRD.md`

## What to build

社交账号和验证码相关的 Controller 端点。

**VO 定义**（`soda-user-adapter` 模块）：
- `BindSocialVO` / `SocialAccountVO`
- `SendCodeVO` / `VerifyCodeVO`

**Controller 端点：**

| 端点 | 方法 | AppService |
|------|------|------------|
| `POST /api/system/user/social/bind` | `SocialController.bind(BindSocialVO)` → `SocialBindAppService` | |
| `POST /api/system/user/social/unbind` | `SocialController.unbind(UnbindSocialVO)` → `SocialUnbindAppService` | |
| `POST /api/system/user/send-sms-code` | `VerificationCodeController.sendSmsCode(SendCodeVO)` → `SendVerificationCodeAppService` | |
| `POST /api/system/user/verify-code` | `VerificationCodeController.verifyCode(VerifyCodeVO)` → `VerifyCodeAppService` | |

## Acceptance criteria

- [ ] 所有 Controller 端点编译通过
- [ ] 集成测试覆盖 bind → unbind 流程
- [ ] 集成测试覆盖 send code → verify code 流程

## Blocked by

`11-sms-email-social-persistence.md`
