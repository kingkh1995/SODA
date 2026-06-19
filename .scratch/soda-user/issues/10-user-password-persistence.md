## Parent

`.scratch/soda-user/PRD.md`

## What to build

User + PasswordAccount 的数据库持久化实现。采用类表继承。

**DB Schema** (Flyway migration):

```sql
-- V1__create_user_tables.sql

CREATE TABLE system_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    mobile VARCHAR(20),
    email VARCHAR(100),
    sex TINYINT,
    avatar VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_mobile (mobile),
    UNIQUE KEY uk_email (email)
);

CREATE TABLE system_user_account (
    account_id VARCHAR(100) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES system_user(id),
    INDEX idx_user_id (user_id)
);

CREATE TABLE system_user_password_account (
    account_id VARCHAR(100) PRIMARY KEY,
    password_hash VARCHAR(200) NOT NULL,
    FOREIGN KEY (account_id) REFERENCES system_user_account(account_id)
);
```

**Infra 模块：**
- `UserPO` / `UserMapper` — MyBatis Plus
- `UserAccountPO` / `UserAccountMapper` — 基表
- `PasswordAccountPO` / `PasswordAccountMapper` — 扩展表
- `UserRepositoryImpl` — 实现 `UserGateway`，处理 Account 多态组装和持久化（account_type 鉴别器分发）
- `PasswordEncoderImpl` — 包装 Spring Security `BCryptPasswordEncoder`，实现 `PasswordEncoder` Gateway

聚合加载策略：`findByUserId()` → 查 system_user + 查 system_user_account（所有 type）→ 按 account_type 分发到各扩展表查询 → 组装 User 聚合。

## Acceptance criteria

- [ ] Flyway migration 可执行（可通过 H2 验证 SQL 兼容性）
- [ ] `UserRepositoryImpl` 实现 UserGateway 全部方法
- [ ] 集成测试覆盖：create → findByUserId → existsByUsername → remove
- [ ] 集成测试验证 Account 多态正确组装和反组装

## Blocked by

`06-user-crud-appservices.md`
