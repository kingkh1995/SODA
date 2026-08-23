# 0025 — Verification 活跃键唯一性：scene:subject 统一键（subject 必填）

**Status**: accepted（2026-08-15，grill-with-docs 会话，用户逐项确认；subject-nullable → subject 必填修订于会话内）

> 实施修订（2026-08-15）：**消费反查改按 subject 键控**——`findFirstBySubjectAndScene(subject, scene, states)` 取代 `findFirstBySceneAndTarget`（消费命令 changeMobile/changeEmail 不含 target——换绑的新联系方式隐含在验证记录中，「按 subject 加载」即主体匹配守卫）；查询索引随之改为 `idx_subject_scene_state_expire_at (subject, scene, state, expire_at)`。原「按 (scene, target) 键控」基于「命令携带 target」的错误假设。

> 部分决策被 ADR-0026（2026-08-16）取代：通道判别从 `Subject` 迁至 `Recipient`（多态投递端点）；subject 降为 source 内裸键字符串（`Subject` 密封层级删除）；`VerificationScene` → `UserVerificationScene`（调用方词汇）；Gateway 契约改 source 键控（`existsBySource`/`findLatestBySourceAndStateIn`）。保留：`active_key` 机制、惰性 DELETE、终态不占唯一键、无 E 态。另：主体裁定（User，非 Verification）、`VerificationService` 作废、ULG/UPR subject=userId（仅 URG 保留端点值）、「端点场景 subject 即 target」仅 URG 适用——见 ADR-0026。
>
> 再修订（2026-08-16，检视后用户确认）：① **并发裁决改为原样上抛**——§1「翻译为领域错误」作废（ADR-0026 §7：预检 + DB 兜底，无翻译，`DataIntegrityViolationException` 原样传播）；② **V 终态标记撤回**——`active_key` 清 NULL 仅 U 终态（V 内存瞬态不落库、不涉槽位，见 ADR-0026 检视修订）；③ 消费反查命名以现状为准（`findLatestBySourceAndStateIn`，本文件 Consequences 中的 `findFirstBySceneAndTarget`/`findFirstBySubjectAndScene` 为沿革表述）。

## Context

`user_verification` 表自 2026-08-11 起显式「无唯一索引——唯一性由应用层 `hasUnexpiredActive(userId, scene)` 弱保证」。缺口：

1. **并发重复**：应用层预检 + 无 DB 硬保证——两个并发 `requestCode` 可同时通过，同键两条活跃验证（用户诉求：「能底层数据库机制保证防止并发重复问题」）。
2. **按场景 subject 身份不同**：UCC 的 subject 是 user（每 (scene, subject) 至多一条活跃：换绑码同用户至多一条——否则用户可对无限不同 target 发起 UCC 码，每 target 槽位互异，大量浪费发码资源）；ULG/UPR/URG 的 subject 即投递端点（每 (scene, target) 至多一条：登录/重置/注册码同手机号至多一条、防同 target 刷）——**端点场景下 target 即 subject**（phone 既是投递端点又是验证主体）。
3. **URG 无 user 键控**：注册发码时用户尚不存在——主体是待注册手机号本身（ADR-0021 已预见无用户场景）。
4. **多领域复用**：yudao 重构（sys_user/member 必然到来）。yudao 参考（`SmsCodeApi` 共享、`SmsSceneEnum` 跨领域前缀、`SmsCodeDO` 无主体列——防滥用走应用层 per-mobile 限流）说明共享形状；本项目选择 DB 级约束，主体为通用 `Subject`（未来 sys_user 扩展密封层级）。

## Decision

### 0. 领域解耦原则：Verification 只感知 subject，不感知 User

- `Verification` 聚合持有 **`subject`**（通用主体引用，**非空**），**不持有/不引用 User 聚合与 User 概念**——subject 是「该验证所针对的身份」这一通用领域概念。
- 持久化 `subject` 列是**基础设施映射**（Subject 序列化；未来多主体类型扩展密封层级）——列名是实现细节，领域概念是 subject。

### 1. 唯一性机制：单列 `active_key` = `scene:subject`（统一键，基础设施硬保证）

- 新增列 `active_key`：**活跃键** = `"{scene}:{subject序列化}"`——subject 序列化带类型前缀（`U:42` / `S:13800138000` / `E:user@x.com`，AuthAccountId 同构）。组合函数 `compose(scene, subject)` = `scene + ":" + subject.serialize()`（infra 单点，convertor/预检/惰性清理共用）——**单一规则，无 NULL 分支**。
- **subject 必填**：`UCC` → `UserSubject`（防同主体对无限不同 target 发码）；`ULG`/`UPR`/`URG` → `MobileSubject`/`EmailSubject`（投递端点即主体——`channel=S` 构造 `MobileSubject(target)`，subject 与 target 同值）。
- `UNIQUE KEY uk_active_key (active_key)`。
- **占槽**：`state ∈ {I, P}` 且未过期（I/P 均占——AFTER_COMMIT 投递窗口与 send 失败滞留的 I 同样拒绝重发，与 2026-08-11「计入 I 态」决策一致）。
- **释放**：`V`/`U` 迁移时 convertor 推导置 NULL（`toPersistence` 全量构造，ADR-0024）；**过期 I/P 行由 requestCode 同事务 DELETE 整行**（`WHERE active_key = :key AND expire_at < :now`——行删除即释放槽位，且不积累过期垃圾）。
- **并发裁决**：唯一索引是最终仲裁——无论间隙锁行为如何，同键并发 INSERT 至多一个成功；失败方 `DataIntegrityViolationException` **原样上抛**（预检 + DB 兜底，无翻译——2026-08-16 会话修订，见 ADR-0026 §7）。
- **requestCode 同事务形状**：前置预检（`existsBySubjectAndScene`，fail-fast 友好错误）→ 惰性 DELETE（按本次键）→ INSERT（convertor 设 `active_key`）→ 撞 `uk_active_key` → 领域错误。
- **无 E 态**：状态机 `I → P → V → U` 不动，ADR-0011「过期是派生判断」不反转——过期行不物化状态，靠惰性 DELETE 释放。
- **应用维护**（非生成列）：无 E 态下生成列不可行——生成列写时求值、时间不可入键，过期 P 行永不改状态则永占槽；值由 convertor 单点推导（I/P → compose，否则 NULL）+ 惰性 DELETE，反同步风险由单点 + 集成测试锁定。
- **被否：双固定列（`active_slot` scene:channel:target + `user_slot` user_id:scene）**——对 UCC 强加其不需要的 target 维度（跨主体同 target 并存无害，码是持码凭证，输家收不到码无法消费）；scene:subject 统一键单列更简且严格 per-scene 适配。

### 2. subject 必填（取代 subject-nullable）

- `Verification.subject: Subject` **非空**——`UCC` → `UserSubject`（认证会话主体，adapter 从会话解析注入命令）；`ULG`/`UPR`/`URG` → 端点 subject（`MobileSubject`/`EmailSubject`，值即 target）。「验证必须有一个主体」成立：URG 的主体是待注册手机号（与 target 同值）。
- 持久化：`subject VARCHAR` 列（通用序列化，取代 `user_id` 列）；`target` 列保留（投递端点原始值——UCC 的新联系方式 ≠ subject；端点场景与 subject 端点值同值）；`channel` 列已删除（多态塌缩，见 Decision 5）。
- **消费按 subject 键控反查**（`findFirstBySubjectAndScene`——消费命令不含 target，换绑的新联系方式隐含在验证记录中；按 subject 加载即主体匹配守卫，持码人即收码人），subject 匹配守卫在 `User.changeXxx` 保留为防御纵深。
- 每手机号限流（yudao `SmsCodeDO.todayIndex` 先例：每日条数 + 发送间隔）——未来票（管顺序滥用，与 `active_key` 的并发 = 1 互补）。
- **命令的 subject 为服务契约级可空字段**：`RequestCodeCommand{scene, channel, target, @Nullable Subject subject}`——UCC 由 adapter 从认证会话解析注入（**客户端请求体零主体概念**——无法指定发码对象），端点场景为空（服务内按 channel+target 构造 `MobileSubject`/`EmailSubject`）。构造归一化保证 subject 恒非空（UCC 必填注入、端点场景由 target 构造）。

### 3. 场景扁平助记码（ADR-0005 修订）

- `VerificationScene` 取值 `CC/PR/LG/RG` → **`UCC/UPR/ULG/URG`**（领域前缀助记码，符合 ADR-0005 规则 1「1-4 字符」；SocialType 扁平助记码先例）。列 `VARCHAR(2)` → `VARCHAR(4)`。
- 跨领域命名空间隔离：未来 sys_user 场景用 `S*` 前缀（`SLG` 等），单枚举共享、值不冲突（yudao `SmsSceneEnum` MEMBER_*/ADMIN_* 同构；数字空档方案因反转 ADR-0005「短名优于 int code」Rationale 出局）。
- 现在即改（V1 直改免费，ADR-0022），避免未来重命名 + 数据迁移。

### 4. 场景入键

- 键含 scene（`scene:subject`）：跨场景同 target 允许并存（UCC 换绑码与 URG 注册码可同刻存在——两码均发至 target 手机，持码人即收码人，互不干扰；输家收不到码、无法消费，无完整性风险）。修正此前「scene-free」建议（其基于「输家撞 uk_mobile 晚失败」的错误分析——实际输家无码不可消费）。
- 场景化系统类比：Twilio Verify「每手机号至多一条活跃验证」（Twilio 无场景概念，等价于每 (场景, 手机号) 一条）。

### 5. 多态塌缩与 channel 收敛：Verification 单类，通道判别栖身 Subject

- **Verification 塌缩为单类**（撤销 ADR-0011 2026-08-05 类层次保留理由与 2026-08-07 send 聚合行为定位）：`SmsVerification`/`EmailVerification` 子类型删除——聚合的 `verify/use/isExpiredAt` 全通道同构，唯一行为差异（send 投递）是**投递侧职责**；多态栖身密封 `Subject`（`MobileSubject`/`EmailSubject`/`UserSubject` 模式匹配穷尽分派，仓库「行为分派走子类型模式匹配或子类方法」惯例）。
- **send 分派移监听器**：投递侧监听器按投递端点（target 类型）匹配选择 `SmsSender`/`EmailSender`（UCC 场景 subject 是用户——无通道语义，投递端点在 target 新联系方式；端点场景 subject 即 target）；聚合保留 `I → P` 状态迁移行为（「PENDING 蕴含已送达」不变量不变）。
- **JSON 无判别属性**：单类无 `@JsonTypeInfo`，`subject` 序列化自带类型前缀。
- **restore 平凡化**：单类无判别映射——`VerificationChannels`（ADR-0016 verification 部分）作废；`subject` 前缀路由即够。
- **`channel` 列删除**：单类 restore 无需判别列；消费查询按 subject 键控（`findFirstBySubjectAndScene`，见 Decision 6）。
- `VerificationChannel` 枚举**仅保留命令输入判别**：`RequestCodeCommand.channel`（`S`/`E` 决定 subject 构造 `MobileSubject`/`EmailSubject`）——无持久化、无聚合访问器、无查询参数。
- **撤销理由（ADR-0011 2026-08-05 三理由逐一回应）**：① 编译期 target 契约（`changeMobile(SmsVerification)`）——消费流 target 本就来自命令（等值校验），单类 + subject 匹配等价，损失可接受；② 前瞻 `AuthenticatorVerification` 行为差异——YAGNI，届时真实行为差异出现再重新密封（今日两通道无行为差异）；③ 与 `AuthAccount` 对称——不对称成理由：AuthAccount 子类型有丰富行为差异（多方法/守卫），Verification 只有 send 一个薄差异。

### 6. Gateway 契约（existsBy 命名规范）

```java
public interface VerificationGateway extends EntityGateway<Verification<?>, UUId> {

    /** 契约：该 (subject, scene) 是否存在占用活跃键的行（所有场景统一——subject 即该场景的身份：UCC=用户、端点场景=投递端点）。活跃/过期判定与键组合为基础设施实现细节。 */
    boolean existsBySubjectAndScene(Subject subject, VerificationScene scene);

    /** 消费反查：最新一条匹配 (subject, scene, states)，按 expire_at 倒序；未过期过滤为基础设施实现细节。
     *  按 subject 键控（2026-08-15 实施修正：消费命令不含 target——换绑的新联系方式隐含在验证记录中，
     *  以 UserSubject 加载即主体匹配守卫） */
    Optional<Verification> findFirstBySubjectAndScene(
            Subject subject, VerificationScene scene,
            Collection<VerificationState> states);
}
```

- **existsBy 语义**：键存在性 + 唯一性约定（同 `UserGateway.existsByUsername`）——契约不出现「活跃/过期」措辞；活跃谓词（`state IN ('I','P') AND expire_at > now`）与键组合（`compose`）由基础设施实现（`VerificationGatewayImpl` 委托 Spring Data 派生查询，`WHERE active_key = :composed AND expire_at > :now` 走 `uk_active_key`）。
- **惰性 DELETE 契约**：`deleteExpiredBySubjectAndScene(subject, scene)`——与 existsBy 对称，`WHERE active_key = :composed AND expire_at < :now`。为过期垃圾行物理清理——不同于 ADR-0017「终态由 save 持久化」的删除禁令（终态 V/U 行保留，仅过期未用 I/P 行删除）。
- `VerificationQuery` record 与 `findLatestByUserId` 删除——固定形状查询用**显式参数**（Spring Data 主流）；查询对象/规格模式只用于开放过滤。
- 消费 `states={P}`（changeMobile 流程加载 PENDING）；结果类型由 channel 判别，密封模式匹配收敛。
- **时钟**：不引入 `validUntil` 参数，infra 内部 `Instant.now()`；时钟一致性（过期判定、预检判定统一时钟源）为后续统一设计（未来票）。
- **命名规范入 framework-conventions**：gateway 存在性谓词一律 `existsBy*`，语义 = 键存在性 + 唯一性约定，过滤语义为基础设施实现细节。

### 7. 表与索引

```sql
CREATE TABLE `verification` (
    `id`          VARCHAR(36)  NOT NULL COMMENT '验证实体 ID（UUId）',
    `subject`     VARCHAR(120) NOT NULL COMMENT '验证主体（Subject 序列化，带类型前缀：U:42=用户、S:13800138000=手机端点、E:user@x.com=邮箱端点——UCC=用户、ULG/UPR/URG=投递端点即 target）',
    `scene`       VARCHAR(4)   NOT NULL COMMENT '验证场景（UCC/UPR/ULG/URG 扁平助记码，ADR-0005 修订）',
    `target`      VARCHAR(100) NOT NULL COMMENT '投递端点原始值（UCC=新联系方式；端点场景与 subject 端点值同值——value 格式互斥，channel 不入列不入查询）',
    `code`        VARCHAR(10)  NOT NULL COMMENT '验证码',
    `state`       VARCHAR(1)   NOT NULL COMMENT '验证状态（I/P/V/U，无 E 态，过期派生判断）',
    `expire_at`   DATETIME     NOT NULL COMMENT '过期时间（UTC）',
    `active_key`  VARCHAR(160) NULL COMMENT '活跃键（scene:subject 序列化；I/P 且未过期占槽，U 终态迁移清 NULL（V 内存瞬态不落库不涉槽位）、过期 I/P 行 DELETE 释放；uk_active_key 硬保证单活跃）',
    `created_date`       DATETIME     NOT NULL COMMENT '创建时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值，见 ADR-0022）',
    `last_modified_date` DATETIME     NOT NULL COMMENT '最后更新时间（审计，Spring Data auditing 维护，UTC 字面值；无 DB 默认值/ON UPDATE，见 ADR-0022）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_active_key` (`active_key`),
    KEY `idx_subject_scene_state_expire_at` (`subject`, `scene`, `state`, `expire_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '验证码实体表（subject 必填；唯一性由 uk_active_key = scene:subject 硬保证）';
```

- 表名 `user_verification` → `verification`（去 user_，subject 通用形状）。
- 查询索引 `(target, scene, state, expire_at)`：等值列按选择性（target 高基数 > scene > state）、范围列（expire_at）殿后；消费反查走全键。预检与惰性 DELETE 走 `uk_active_key`（`active_key = :composed` 等值 + expire_at 残余过滤）。V1 直改（ADR-0022）。

## Considered Options

| 方案 | 结论 |
|---|---|
| 双固定列（`active_slot` scene:channel:target + `user_slot` user_id:scene） | 否决——对 UCC 强加其不需要的 target 维度；scene:subject 统一键单列更简 |
| 按场景双分支组合（UCC→scene:subject；端点→scene:channel:target） | 收敛为 scene:subject 统一键——端点场景的 subject 即 target（phone 既是 subject 又是 target），subject 必填消除 NULL 分支与回退 |
| subject-nullable（URG 无主体 → NULL） | 否决——「验证必须有主体」成立：URG 的主体是待注册手机号（= target）；subject 必填 |
| 生成列（`GENERATED ALWAYS AS (CASE WHEN state IN ('I','P') THEN …)` + UNIQUE） | spike 验证可行（H2 2.4.240 MySQL-mode）；最终否决——无 E 态下生成列时间不可入键，过期 P 行永占槽 |
| E 态（Expired 终态 + 惰性迁移 P→E） | 否决：反转 ADR-0011「过期派生」、状态机污染；「过期行 DELETE」替代 |
| partial unique index | MySQL 不支持（Postgres-only）——精确表达「仅活跃行唯一」的唯一机制 |
| FOR UPDATE / gap lock | issue-19 既定否决；时间谓词下间隙锁范围脆弱，非约束级保证 |
| 独立槽位表（PK 唯一键） | 同样需要惰性清理 + 多一张表 + join，纯增复杂度 |
| `hasUnexpiredActive(userId, scene)` 现状 | 正是要修的洞（并发竞态、URG 无键控、按场景 subject 策略缺失） |
| 查询对象（VerificationQuery） | 删除：固定形状查询用显式参数（Spring Data 主流）；查询对象只用于开放过滤 |
| Verification 子类型层级（Sms/Email） | 塌缩为单类——唯一行为差异（send 投递）是投递侧职责，多态栖身密封 Subject；撤销 ADR-0011 2026-08-05 保留理由（typed-target 损失可接受 / AuthenticatorVerification YAGNI / AuthAccount 对称不成立） |

## Consequences

- **V1 直改**（ADR-0022）：表更名 `verification`、`user_id` 列 → `subject` 列（通用序列化）、加 `active_key` + `uk_active_key`、`scene` 列宽 2→4、新查询索引（旧 `idx_user_id_scene_status_expire_at` 删除——消费改 subject 键控，`idx_subject_scene_state_expire_at`）。
- **代码**：领域 `Verification` 塌缩为**单类**（`subject: Subject` 非空、`target: Type`——`Mobile`/`Email` 构造边界校验；`verify/use/isExpiredAt` 同构；send 分派移监听器——投递侧按投递端点（target 类型）匹配 `SmsSender`/`EmailSender`，聚合保留 `I→P` 迁移行为；`Subject` 密封 `UserSubject`/`MobileSubject`/`EmailSubject` 序列化带类型前缀，AuthAccountId 同构；构造归一化：UCC 注入 `UserSubject`、端点场景由 channel+target 构造——subject 恒非空）；`VerificationPO` 映射 `subject`/`target`/`active_key`（无 channel 列）；`VerificationConvertor.toPersistence` 推导 `active_key`（I/P → `compose(scene, subject)`，否则 NULL；ADR-0024 全量构造）——`compose` 为 infra 单点函数；`VerificationGateway` 契约重写（`existsBySubjectAndScene`/`deleteExpiredBySubjectAndScene`/`findFirstBySceneAndTarget`（沿革名——现状为 source 键控 `findLatestBySourceAndStateIn`，见 ADR-0026 §8），删 VerificationQuery/findLatestByUserId/类型收敛重载/VerificationChannels）；`VerificationServiceImpl` 主体 `AbstractAppService<Verification<?>, UUId, VerificationGateway>`（UCC 发起走本服务——发送主体是 Verification，User 为协助方：UCC 分支经 UserGateway 协作者加载用户做场景前置；subject 由 adapter 从会话注入命令）；`UserAuthServiceImpl` 消费按 (scene, target) 键控 + subject 匹配守卫；`VerificationChannel` 枚举仅命令输入判别（无持久化/聚合访问器/查询参数）。
- **V 态保留且永不落库**：原子流 verify→change→use 单事务，save 只写 U；V 是「码已核对」事实的唯一载体（`use()` 前置、changeMobile 守卫 VERIFIED 承重），且为未来拆分流（verify 一步/confirm 一步）预留。V 为内存瞬态、不落库不占槽（2026-08-16 检视修订：V 终态标记撤回，`active_key` 清 NULL 仅 U 终态；若未来拆分流持久化 V，V 行将保持占槽直至 U，过期 V 行 DELETE 谓词届时扩展含 V——接缝）。
- **测试**：`PersistenceEndToEndTest`/`VerificationFailureResendTest` 扩展——并发同键双发（唯一索引拒后发）、过期重发（惰性 DELETE 腾槽）、verify→use 槽位释放、跨场景同 target 共存（UCC vs URG）、同主体多 target UCC 拒绝（subject 维度）、跨主体同 target UCC 允许（持码凭证）、终态行 `active_key=NULL` 恢复；convertor 单测（compose 三态 + subject 构造归一化）；gateway 契约测试。
- **文档**：ADR-0005 修订注记（VerificationScene 新短名 + VerificationChannel 定位）；CONTEXT.md 更新（subject 必填、活跃验证词汇、scene:subject 统一键、existsBy 契约）；framework-conventions 增 existsBy 命名规范。
- **接缝（未来票）**：跨领域共享服务（`Subject` 密封层级扩展 `SysUserSubject`、场景单枚举 `S*` 前缀）、每手机号限流（yudao todayIndex）、时钟一致性统一设计、拆分流 V 落库与 DELETE 谓词扩展。

## 依据

- H2 2.4.240（MODE=MySQL, DATABASE_TO_LOWER=TRUE，应用实际配置）spike 实证「状态派生键 + 唯一索引」机制：重复活跃槽位插入被拒（`JdbcSQLIntegrityConstraintViolationException` → `DataIntegrityViolationException`）、状态迁移释放槽位、终态多行共存（历史保留）、跨键共存。最终单列 `active_key` 为 vanilla 唯一索引 + NULL 语义（MySQL 官方：唯一索引允许多个 NULL，ADR-0023 已引 bug #8173）。
- Twilio Verify：每手机号至多一条活跃验证（HTTP 409）——场景化系统等价于每 (场景, 手机号) 一条（端点场景）。
- yudao-cloud 参考：`SmsCodeApi`（system 模块共享 RPC）、`SmsSceneEnum`（MEMBER_*/ADMIN_* 单枚举跨领域前缀）、`SmsCodeDO`（id/mobile/code/scene/createIp/todayIndex/used/usedTime/usedIp、`idx_mobile`）——无主体列，防滥用走应用层 per-mobile 限流；本项目以 subject 必填 + DB 级约束补足。
- ADR-0011（验证跨模块、状态机、过期派生）、ADR-0016（class↔channel 映射归基础设施）、ADR-0021（Verification 聚合根、URG 无 user 接缝）、ADR-0023（终态不占唯一索引 NULL 原则、PO 命名）、ADR-0024（save 全权委托、convertor 全量构造）。
