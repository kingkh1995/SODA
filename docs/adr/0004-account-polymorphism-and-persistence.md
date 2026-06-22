# 0004 — AuthAccount 多态设计与持久化策略

**Status**: accepted

**Context**:

项目需要为 `soda-user` 模块设计认证机制。Yudao 参考实现将密码直接存放在 `AdminUserDO.password` 字段，社交登录使用独立的 `SocialUserDO + SocialUserBindDO` 关联表，短信登录依托 `AdminUserDO.mobile` + `SmsCodeDO` 临时验证码。

在 DDD 架构下，我们需要确定：
1. 认证方式在领域层的表达形式（flat 字段 vs 抽象 Entity）
2. 多态 Entity 的数据库映射策略

讨论过程中先后排除了"将 password 作为 User 直接字段"和"不做 Account 抽象、直接在 User 上用 if-else 分发"两个方向，最终确定 Account 作为 User 的子实体、子类多态表达不同认证方式。

**Decision**:

### 领域设计

`AuthAccount` 是 `User` 聚合下的抽象子实体，子类多态：

| 子类 | AuthAccountId 工厂 | 持有属性 | 格式示例 |
|------|-------------------|---------|---------|
| `PasswordAuthAccount` | `PasswordAuthAccountId.from(userId)` | `UserId` | `"P:42"` |
| `SmsAuthAccount` | `SmsAuthAccountId.from(mobile)` | `Mobile` | `"S:13800138000"` |
| `EmailAuthAccount` | `EmailAuthAccountId.from(email)` | `Email` | `"E:user@example.com"` |
| `SocialAuthAccount` | `SocialAuthAccountId.from(socialType, openId)` | `SocialType` + `openId` | `"O:GE:open123"` |

所有 AuthAccountId 继承自密封基类 {@link AuthAccountId}，统一 {@link Identifier}{@code <String>}，使用 {@link AuthAccountType} 短名前缀。反序列化通过各子类的 {@code valueOf(Object)} 完成。

`AuthAccountType` 为枚举（`soda-user.domain.enums.AuthAccountType`，短名 {@code P / S / E / O}），提供鉴别能力。数据库存储短名字符串。

生命周期规则：
- `PasswordAuthAccount` 在 `User` 创建时自动创建，一个 User 只有一个
- `SmsAuthAccount` / `EmailAuthAccount` 随 User.mobile / User.email 的设值自动创建或删除
- `SocialAuthAccount` 通过绑定/解绑流程独立添加和删除

验证码行为由 ApplicationService 编排：生成随机码、创建 `VerificationCode` DP 后通过 `replaceCode(VerificationCode)` 注入到 AuthAccount 子类，并调用发送器 Gateway 发送。Domain 层不持有任何 Gateway 或 Generator 依赖。
验证码策略（长度、过期时间）由 `VerificationCodePolicy` DP 表达，AuthAccount 子类持有静态默认值，构造时可选传入自定义策略，实例化后不可变更（移除了 `overridePolicy()` 方法）。
`AuthAccount` 构造器接受显式 `boolean active` 参数（而非默认 true），确保激活状态的语义明确。

### 持久化策略

采用 **类表继承**（Class Table Inheritance）：

```
system_user_account           ← 基表
├── account_id (PK)
├── user_id (FK → system_user)
├── account_type (VARCHAR)
├── active (BOOLEAN)
└── ...通用审计字段

system_user_password_account  ← PasswordAuthAccount 扩展表
├── account_id (PK + FK)
└── password_hash (VARCHAR)

system_user_sms_account       ← SmsAuthAccount 扩展表
├── account_id (PK + FK)
├── verification_code (VARCHAR?)
├── verification_expire_at (DATETIME?)
├── verification_used (BOOLEAN?)
├── code_length (INT)
└── code_expiry_minutes (INT)

system_user_email_account     ← EmailAuthAccount 扩展表
├── account_id (PK + FK)
├── verification_code (VARCHAR?)
├── verification_expire_at (DATETIME?)
├── verification_used (BOOLEAN?)
├── code_length (INT)
└── code_expiry_minutes (INT)

system_user_social_account    ← SocialAuthAccount 扩展表
├── account_id (PK + FK)
└── (socialType + openId 编码在 account_id 中)
```

Repository 层通过 `account_type` 鉴别器分发到正确的子类映射。

**Rationale**：

- **领域职责正确**：认证方式是独立的领域概念，有自身的生命周期和业务规则（验证码过期、密码编码、社交平台映射），不应作为 User 的散列字段。
- **扩展性**：新增认证方式只需新增 AuthAccount 子类 + 扩展表，User 聚合不修改（OCP）。
- **类表继承优于单表继承**：无 nullable 列（子类特有字段只在扩展表中），schema 紧凑；基表支持 `findByUserId()` 全量查询，扩展表通过 FK 按需 join。
- **类表继承优于每类一表**：基表提供统一查询入口，ApplicationService 可通过 `UserGateway.findByUserId()` 一次加载所有 AuthAccount，不需要每种子类单独查询。
- **对齐账户业务概念**：Yudao 的 `password` / `mobile` / `SocialUser` 三种认证存储方式在 DDD 中被统一为同一抽象层次的概念，而非三个不同的数据模式。

| Positive | Negative |
|----------|----------|
| 新增认证方式（如指纹、硬件 Key）只需新增子类，User 零修改 | 写入时需要操作多张表（基表 + 扩展表），事务跨度增加 |
| Schema 紧凑，每种认证只存自己的字段 | Repository 需要 `account_type` 鉴别分发，实现复杂度略高于单表 |
| `UserGateway.findByUserId()` 可一次性查询所有 AuthAccount | 纯查询场景（query-server）不需要 AuthAccount 信息时仍需 join |
| 领域模型与 DB 映射一致，无阻抗失配 | — |
| VerificationCode DP 封装验证码业务规则，不散落在 Service 层 | — |
| 【2026-06-21】Domain 层不再依赖 Gateway/Generator 接口（`sendCode` 移出），Application 层负责编排发送流程 | — |
| 【2026-06-21】`overridePolicy()` 移除，策略在构造时设定，实例后不可变 | — |
| 【2026-06-21】构造器显式要求 `active` 参数，消除默认 true 的隐式行为 | 所有调用方（含测试）都必须传递 active 参数 |

**Considered alternatives**:

| 方案 | 放弃原因 |
|------|---------|
| **Yudao 扁平 DO 方式**：password 直接作 User 字段，SocialUser 独立 DO | 无法表达"认证方式"统一概念；if-else 分散在 Service 层；不支持未来新增认证方式 |
| **Account 非多态，单个类 + 联合字段**：一张 Account 表包含所有可能字段 | 大量 nullable 列，无类型安全，Java 层用 if-else 或 switch 分发 |
| **单表继承**：一张 system_user_account 包含所有子类字段 + type 鉴别器 | schema 不紧凑，大量可选列，数据库约束能力弱 |
| **每类一表**：四个独立表，无基表 | 无法统一查询某个 User 的所有 AuthAccount，ApplicationService 需要多次调用不同 Repository |
| **Social 作为独立聚合根（Yudao 方式）** | SocialAuthAccount 的生命周期完全依附于 User（绑定/解绑），无独立存在意义。如果未来需要跨用户复用社交身份（同一社交账号绑定多个本地账号），可以再拆分为独立聚合 |

**Related documents**:

- `CONTEXT.md` — User、AuthAccount、AuthAccountType 等术语
- `0001-module-architecture.md` — soda-user 7 子模块结构
- `skill://grill-with-docs` — 本决策的讨论过程记录
