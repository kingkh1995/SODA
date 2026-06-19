## Parent

`.scratch/soda-user/PRD.md`

## What to build

SmsAccount / EmailAuthAccount / SocialAccount 三张扩展表的持久化实现。

**DB Schema** (Flyway migration):

```sql
-- V2__create_account_extension_tables.sql

CREATE TABLE system_user_sms_account (
    account_id VARCHAR(100) PRIMARY KEY,
    verification_code VARCHAR(10),
    verification_expire_at DATETIME,
    verification_used BOOLEAN DEFAULT FALSE,
    code_length INT NOT NULL DEFAULT 6,
    code_expiry_minutes INT NOT NULL DEFAULT 5,
    FOREIGN KEY (account_id) REFERENCES system_user_account(account_id)
);

CREATE TABLE system_user_email_account (
    account_id VARCHAR(100) PRIMARY KEY,
    verification_code VARCHAR(10),
    verification_expire_at DATETIME,
    verification_used BOOLEAN DEFAULT FALSE,
    code_length INT NOT NULL DEFAULT 8,
    code_expiry_minutes INT NOT NULL DEFAULT 30,
    FOREIGN KEY (account_id) REFERENCES system_user_account(account_id)
);

CREATE TABLE system_user_social_account (
    account_id VARCHAR(100) PRIMARY KEY,
    social_type VARCHAR(20) NOT NULL,
    open_id VARCHAR(200) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES system_user_account(account_id),
    UNIQUE KEY uk_social_type_openid (social_type, open_id)
);
```

**Infra 模块：**
- `SmsAccountPO` / `SmsAccountMapper`
- `EmailAccountPO` / `EmailAccountMapper`
- `SocialAccountPO` / `SocialAccountMapper`

**更新 `UserRepositoryImpl`：**
- `findByUserId()` 加入 3 张扩展表的查询分发逻辑
- `save()` 加入对应扩展表的 insert/update/delete

**启用 Gateway 实现（占位，可后续完善）：**
- `CodeGeneratorImpl` — `SecureRandom` 数字验证码
- `SmsSenderImpl` / `EmailSenderImpl` — `log.warn("未配置短信/邮件发送器")` 桩实现

## Acceptance criteria

- [ ] Flyway 迁移通过
- [ ] 集成测试验证 SmsAccount 持久化和反组装
- [ ] 集成测试验证 SocialAccount 唯一约束（同 type + openId 不可重复）
- [ ] UserRepositoryImpl 完整支持所有 4 种 Account 类型的多态 CRUD

## Blocked by

`09-auth-appservice.md?` No — blocked by `06-user-crud-appservices.md` (needs UserRepositoryImpl) + `07-social-appservices.md` + `08-verification-code-appservices.md`
