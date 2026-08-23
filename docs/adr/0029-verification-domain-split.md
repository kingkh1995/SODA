# 0029 — Verification 领域拆分：通用验证中心下沉 component + User 门面隔离

**Status**: accepted（2026-08-21，grill-with-docs 两轮确认）

## Context

`VerificationCodePolicy` 存在过度设计（grill-with-docs 会话结论）：

- `DEFAULT_SMS`/`DEFAULT_EMAIL` 死常量——仅测试引用，生产唯一用 `UCC_CODE_POLICY`（`UserVerificationFactory` 私有常量）
- `SmsAuthAccount.DEFAULT_POLICY`/`EmailAuthAccount.DEFAULT_POLICY` 残留——ADR-0026 已移除字段，常量成死码
- 与 ADR-0026 自身否决的 `forChannel` 同因违反 YAGNI

同时，Verification 与 User 域强耦合——`Verification` 聚合、DP、端口全在 `soda-user-domain`，未来无法独立为 RPC 服务。

yudao-cloud 参考：`SmsCodeApi` 独立模块，验证与用户解耦。

## Decision

### 1. Verification 通用能力下沉到 component 子包

新建 `com.soda.component.verification`（`soda-components` 内子包，暂不新建 Gradle 模块）：

| 类型 | 包 |
|------|-----|
| 聚合根 `Verification` | `com.soda.component.verification` |
| DP `VerificationCode`/`VerificationCodePolicy`/`VerificationSource`/`VerificationRecipient`（+`SmsRecipient`/`EmailRecipient`） | 同上 |
| Enum `VerificationChannel`/`VerificationState` | 同上 |
| 端口 `VerificationRepository`（原 `VerificationGateway`） | 同上 |
| 事件 `VerificationCreatedEvent` | 同上 |

依赖：`com.soda.component.domain`（`Aggregate`/`EntityGateway`）、`com.soda.component.domain.types`（`Alphabet`/`PositiveInt`/`Uuid`/`Mobile`/`Email`/`Type`）。

反向零依赖——`com.soda.component.verification` 不引用 `soda-user`。

### 2. 删除死常量

- `VerificationCodePolicy.DEFAULT_SMS`/`DEFAULT_EMAIL` → **删除**
- `SmsAuthAccount.DEFAULT_POLICY`/`EmailAuthAccount.DEFAULT_POLICY` → **删除**

`VerificationCodePolicy` DP 形态保留（`record` + `Type` + 嵌套 DP，ADR-0018），无默认常量——策略完全由调用方决定。

### 3. UserVerification 门面 Entity（user domain 专属桥梁）

在 `soda-user-domain` 新建 `verification` 子包：

- **`UserVerification`**：领域 Entity / 门面。委托给 `Verification`，封装 source/recipient/policy 映射 + User 守卫。
  - 工厂方法：`forCredentialChangeMobile(userId, mobile, generator)` / `forCredentialChangeEmail(userId, email, generator)`
  - 消费包装：`of(Verification)` 从已加载聚合包装
  - 门面行为：`verifyCode(code)` / `markSent()` / `consume()`
  - 守卫：`isForUser(userId)` / `isCredentialChange()` / `isVerified()` / `newMobile()` / `newEmail()`
  - `UCC_POLICY` 私有静态常量（6位/5分钟/纯数字，ADR-0026 UCC 专属策略）
- **`UserVerificationScene`**：从 `com.soda.user.domain.types` 迁到 `com.soda.user.domain.verification`（调用方词汇，与门面强内聚）
- **`UserVerificationFactory`**：删除，职责并入 `UserVerification` 工厂方法

### 4. User 签名改造

`User.changeMobile(Verification)` / `changeEmail(Verification)` → `User.changeMobile(UserVerification)` / `changeEmail(UserVerification)`。

守卫改为通过 `UserVerification` 门面方法：`uv.isForUser(getId())` / `uv.isCredentialChange()` / `uv.isVerified()` / `uv.newMobile()`。

**User 只依赖 `UserVerification`，不引用 `Verification` 类型。**

### 5. 不建 VerificationService

ADR-0026 §7 的裁定仍然成立——当前仅 UCC 一个场景，主体是用户，编排归 `UserAuthService`（application 层）。`UserVerification` 是 domain 层门面 Entity（薄映射 + 守卫），不做持久化编排。

### 6. 不引入 @ConfigurationProperties

`UCC_POLICY` 硬编码在 `UserVerification` 私有常量。多场景（ULG/UPR/URG）落地时再评估配置化（YAGNI，同 ADR-0026 对 `forChannel` 的判定）。

## Considered Options

| 方案 | 结论 |
|---|---|
| 新建 `soda-component-verification` Gradle 模块 | 否决——暂不需要独立模块，子包足够；未来迁移时再提取 |
| `VerificationService` 端口（create/send/verify/invalidate） | 否决——ADR-0026 §7 已作废（YAGNI），编排留 UserAuthService |
| `@ConfigurationProperties` 策略配置 | 否决——当前仅 UCC 一个场景，硬编码最简（YAGNI） |
| `UserVerificationPolicy` 独立类 | 否决——内联在 `UserVerification` 最简（YAGNI） |
| `User.changeMobile` 保持 `Verification` 签名 | 否决——门面不隔离则形同虚设 |
| DP 放 `domain-types`（通用 DP 池） | 否决——验证专属 DP 污染通用 DP 池，新建独立子包 |
| 聚合放 `domain-starter`（基类 starter） | 否决——具体聚合不是基类，新建独立子包 |

## Consequences

- **代码迁移**：`Verification` 等 11 个类型从 `com.soda.user.domain[.types]` → `com.soda.component.verification`
- **代码删除**：`DEFAULT_SMS`/`DEFAULT_EMAIL`、`SmsAuthAccount.DEFAULT_POLICY`/`EmailAuthAccount.DEFAULT_POLICY`、`UserVerificationFactory`
- **代码新增**：`UserVerification`（门面 Entity）、`com.soda.component.verification.package-info.java`
- **代码修改**：`User.changeMobile/changeEmail` 签名 + 守卫、`UserAuthService` 编排改用 `UserVerification`、`UserVerificationScene` 迁包
- **CONTEXT.md**：Verification 相关词条更新位置引用；新增 `UserVerification` 词条
- **接缝**：`com.soda.component.verification` 可整体提取为独立 Gradle 模块 / RPC 服务，`soda-user` 零改动（仅替换 `UserVerification` 内部委托）

## 依据

- ADR-0026（策略决策属调用方场景、主体=用户、VerificationService 作废）
- ADR-0018（DP 嵌套组合、嵌套 DP 序列化）
- ADR-0008（Layered Component Starters、starter 为层基类）
- ADR-0016（映射归基础设施）
- yudao-cloud `SmsCodeApi`（独立模块，验证与用户解耦）
- grill-with-docs 两轮确认（Q1-Q8 全部按推荐）