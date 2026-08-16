# 0021 — Verification 聚合根认定与 VerificationService 拓扑

**Status**: accepted（2026-08-12，grill-with-docs 会话，用户逐项确认）

> 被 ADR-0026（2026-08-16）取代：`VerificationService.requestCode` 统一入口**作废**——主体裁定为 User（非 Verification），发码用例按场景归属用户侧应用服务（UCC → `UserAuthService.requestChangeMobileCode`/`requestChangeEmailCode`）；`RequestCodeCommand` 删除，api 按用例拆分（per-use-case 命令，scene/channel 隐式，api→domain 临时依赖解除）；「发送主体是 Verification，User 是协助方」措辞撤销——Verification 为协助方聚合。保留：Verification 聚合根判据（URG 独立存在）、AFTER_COMMIT 事件投递、`CredentialChangeDomainService` 消费编排、同事务双 save 策略。

> 修订（2026-08-12，用户决定）：**Command 携带领域枚举**——`RequestCodeCommand.scene`/`channel` 由 String 短名改为 `VerificationScene`/`VerificationChannel` 枚举（JSON 线格式不变，枚举按 name() 短名序列化）；api 模块临时声明 `soda-user-domain` 依赖（`api` config 传递可见），web 模块 `allowedDependencies` 增加 `domain`。代价：触发「api→domain 正式方案」悬案提前，用户确认临时接受，正式方案落地后复核（见决策 6 与被否定方案）。

## 问题

1. **分类矛盾**：`Verification` 自有 Gateway（`VerificationGateway`）、自有表、自有 Repository、独立生命周期——实践中已是聚合根，但分类为「Entity 非聚合根」（Entity 由聚合根持久化，Verification 不是）。
2. **主体聚合归属**：发码用例中实际被创建/变更的是 Verification，User 只读（状态检查）；但「决策 + 创建」收在 `User.requestChangeXxxCode` 领域方法里。RG/LG/PR 场景（已确认全部会来）下「发码」没有 User 可当决策者——RG 发码时用户尚不存在。
3. **发送机制困惑**：发送代码本不在 UserAuthService（在 `VerificationCreatedEventHandler` + `Verification.send(sender)` 聚合方法），但事件机制是否过度设计需裁断。
4. **多聚合事务**：changeMobile 同事务 save User + Verification——是否符合 DDD 最佳实践需定策。

## 决策

### 1. Verification 是独立聚合根（重分类）

判据（用户提出）：能否脱离 user 独立存在？生命周期是否与 user 一致？

- **能独立存在**：RG（注册）场景发码时用户尚不存在——验证码先于用户创建，记录无 user。
- **生命周期不一致**：验证码 5-30 分钟 vs 用户永久；RG 验证先于 user 诞生，CC 验证在 user 生命周期中途消亡。

结论：`Verification extends Entity<UUId>` → `extends Aggregate<UUId>`。`Aggregate` 是 `Entity` 的标记子类（零附加字段/行为，见 `soda-component-domain-starter` 源码），重分类为纯声明变更，无 schema/持久化影响。子类型密封层次（Sms/Email）与 ADR-0016 类型规则不变。

连带模型变更：

- `Verification.userId` 必填 → 可空（`@Nullable`，仅 RG 无 user）；`createBuilder().userId(…)` 可空
- **构造器按 scene 归一化 userId**：scene≠RG（CC/PR/LG）必须非空（IAE）、scene=RG（注册）静默置空（归一化，不校验）——规则本体在构造器（聚合完整性，恢复路径同样拦截非 RG 场景的损坏数据），AppService 仅保留流程前置（加载用户必须有 ID + 防拆箱 NPE）；`User.changeMobile`/`changeEmail` 守卫顺序：scene → userId → status（scene 是更粗的语义闸门，先于绑定校验）
- `User.changeMobile`/`changeEmail` 的 userId 匹配守卫**无条件生效**（CC 验证 userId 由构造器保证非空，无 null 分支）
- 基础设施：`verification.user_id` 列 NOT NULL → NULL（V1 create table 直改——开发阶段单 V1 约定，见 ADR-0022；无 FK）

### 2. 服务拓扑：新增 VerificationService，发码用例迁出 UserAuthService

- 新增 `VerificationService`（接口 `com.soda.user.api.VerificationService`，实现 `soda-user-application`）：`requestCode(RequestCodeCommand)` 为**所有验证码发起用例的唯一入口**（对齐「每个聚合根一个 Service」约定——Verification 聚合根 → VerificationService）。
- `UserAuthService` 删除 `requestChangeMobileCode`/`requestChangeEmailCode`，保留 `changePassword`/`changeMobile`/`changeEmail`（凭证变更用例，消费验证码）。
- `User.requestChangeMobileCode`/`requestChangeEmailCode` 领域方法删除：CC 前置（启用态、target ≠ 当前值）从 User 领域方法降级为应用层编排 Assert——代价确认：用户状态规则从领域层搬到编排层；收益：全场景（RG/LG/PR）发码单一入口，RG 无 user 可作决策者。
- 消费流程不变：`CredentialChangeDomainService`（verify → change → use）编排，AppService 同事务双 save；`VerificationCreatedEventHandler` 不动。

### 3. requestCode 编排（当前实现面：仅 CC）

```java
// requestCode(RequestCodeCommand{userId, scene=CC, channel=S|E, target})
// 前置（fail-fast）：
//   userGateway 读 user（存在性）→ Assert 启用态 + target ≠ 当前值
//   → userGateway.existsByMobile/Email(target) 全局唯一
//   → verificationGateway.hasUnexpiredActive(userId, CC) 无未过期活跃
// 创建：SmsVerification/EmailVerification.createBuilder().userId(...).scene(CC)
//       .target(...).generator(...) → 工厂注册 VerificationCreatedEvent
// 持久化：save(I) → publishAll(flushEvents) → AFTER_COMMIT 监听器 send → P 落库
//        （ADR-0011 机制不变）
```

- 构造侧按 channel 判别值建子类（S→`SmsVerification`，E→`EmailVerification`）属**输入判别**（判别值即输入），非行为分派——ADR-0016「按判别枚举判断分支即反模式」不适用（无具体子类可模式匹配）。
- 非 CC 场景（PR/LG/RG）当前抛 IAE（fail-fast，不静默创建）；实现是未来票。RG 票的接缝：`hasUnexpiredActive` 按 userId+scene 查询，RG 无 userId——届时需 target 维度查询。

### 4. 发送机制：保留 AFTER_COMMIT 事件

直发（事务内调 SMTP/SMS）回归 ADR-0011 2026-08-10 修复的两个缺陷：(a) DB 事务跨外部投递通道持有（IDDD 反模式，SMTP 超时期间持锁）；(b) 幽灵码窗口（发送成功但事务回滚 → 有码无记录）。「无事件但正确」= TransactionTemplate 手拆两段事务，代码量 ≥ 监听器且更不 Spring。发送由 Verification 聚合行为（`send(SmsSender)`/`send(EmailSender)`）控制、AFTER_COMMIT 时机由 verification 侧事件驱动、User 零感知——「发送归属 verification」与「不需要 UserAuthService 感知」同时满足。

### 5. 多聚合事务策略：务实同事务（一类决策）

默认「一个事务一个聚合」（Vernon IDDD）；同事务多聚合 save 的例外四条件：**同限界上下文、短事务、无外部 I/O、不变量真实共享**。消费流程（CC：user+verification；未来 PR：password+verification；RG：user 创建+verification）统一适用：`CredentialChangeDomainService` 编排 + AppService 同事务双 save。

原子性非承重分析（CC）：重放被 `User.changeMobile`/`changeEmail` 的 target ≠ 当前值守卫挡住（换绑后当前值 == 目标，二次提交必失败）；USED 标记是审计级而非一致性承重。同事务的收益：USED 与凭证变更永不被分开观察，零成本。代价：偏离教义默认值——已记录，判定为可接受例外。

### 6. API 契约（按项目接口设计规范：ADR-0009 / 0012 / 0013 / 0014 + framework-conventions）

`RequestCodeCommand`（soda-user-api/command，record + `@JsonProperty`，**2026-08-12 修订：携带领域枚举**——api 临时依赖 domain；`target` 保持字符串，领域校验在 DP 构造器，ADR-0014 格式级注解暂不加）：

```java
public record RequestCodeCommand(
        @JsonProperty("userId") @Nullable Long userId,
        @JsonProperty("scene") VerificationScene scene,    // CC|PR|LG|RG
        @JsonProperty("channel") VerificationChannel channel,  // S|E
        @JsonProperty("target") String target              // S=手机号 / E=邮箱
) implements Command {}
```

- AppService 直接消费 `scene`/`channel` 枚举（无 .of() 解析），按 channel 构造 `Mobile`/`Email`（领域校验在 DP 构造器）
- JSON 线格式不变：枚举按 name() 短名序列化，`VerificationScene.of` 仍由 JSON 反序列化使用
- 端点维持用户域路径（AIP-136 camelCase 自定义方法，ADR-0012 合规；资源标识符在路径上，ADR-0009 规则 3）：

| 端点 | Request | Command 组装 |
|---|---|---|
| `POST /api/users/{id}:requestChangeMobileCode` | `RequestChangeMobileCodeRequest{newMobile}` | userId=路径 id, scene=CC, channel=S, target=newMobile |
| `POST /api/users/{id}:requestChangeEmailCode` | `RequestChangeEmailCodeRequest{newEmail}` | userId=路径 id, scene=CC, channel=E, target=newEmail |

- 控制器内部改调 `verificationService.requestCode`（WebAssembler 手写 default 方法 `toSmsRequestCodeCommand`/`toEmailRequestCodeCommand`——to{Command 类名} + 通道限定，ADR-0009 动作前缀消歧；直接引用领域枚举，无字符串镜像）；`changeMobile`/`changeEmail`/`changePassword` 端点不变（仍走 UserAuthService）
- RG/LG/PR 落地时再开泛化 verification 端点（如 `POST /api/verifications:requestCode`）——本轮不开（只有 CC 一个调用方，Request 校验需按 channel 条件化，adapter 复杂化）

## 被否定的方案

| 方案 | 否定原因 |
|---|---|
| VerificationService 直发（无事件） | 回归幽灵码窗口 + 事务跨外部通道（ADR-0011 修复的缺陷）；TransactionTemplate 替代不比事件简单 |
| 消费流程事件驱动 USED | 原子性非承重前提下机制净增（每场景事件 + 监听器 + 异步窗口） |
| Verification 收回 User 聚合做子实体 | 生命周期不匹配（临时 vs 永久），ADR-0011 已否定，重申 |
| Command 携带领域枚举（正式） | 引入 api → domain 依赖，把「api→domain 正式方案」悬案提前卷入本轮（范围蔓延）——2026-08-12 用户决定临时采纳（api 临时声明 domain），正式方案落地后复核 |
| 现在就开泛化 verification 端点 | RG/LG/PR 未实现，泛化端点只有 CC 一个调用方；Request 校验需按 channel 条件化 |
| 维持 User 作为发码决策者 | RG 无 user 可当决策者——全场景发码需要 Verification 作为主体聚合，User 决策点届时必须重构（二次重构代价） |

## 后果

- Verification 聚合根 + userId 可空 + `VerificationService.requestCode` 唯一入口；UserAuthService 瘦身为凭证变更用例（3 方法）
- 用户状态规则（启用态、target ≠ 当前值）从 User 领域方法降级为应用层编排（已确认代价）
- 机制保留：事件驱动投递、I→P→V→U 状态机、`CredentialChangeDomainService`、`VerificationGateway` 查询
- CONTEXT.md / framework-conventions 同步修订；ADR-0011 顶部修订注记

## 参考

- ADR-0011（验证跨模块领域，机制沿革）；ADR-0016（type↔class 映射规则）；ADR-0009 / 0012 / 0013 / 0014（接口设计规范）
- framework-conventions「ApplicationService 编排规范」规则 5/6
- yudao-cloud `SmsCodeApi`（验证独立服务先例）；Vernon IDDD「one aggregate per transaction」（例外四条件）
