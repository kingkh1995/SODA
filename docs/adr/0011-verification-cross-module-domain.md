# Verification — 跨模块通用验证领域

> 再修订（2026-08-16）：主体裁定为 **User**（非 Verification）——`VerificationService.requestCode` 作废，发码用例回归 `UserAuthService`（per-use-case 命令，api 按用例拆分）；通道判别从 `Subject` 迁至 **`Recipient`**（多态投递端点，channel 只在 Recipient——`Subject` 密封层级删除，subject 降为 source 内裸键字符串）；`VerificationScene` → `UserVerificationScene`；策略决策移出聚合（create 必传，默认在 `UserVerificationFactory` 内联选择）。**机制保留**：I→P→V→U 状态机、AFTER_COMMIT 事件投递、`CredentialChangeDomainService` 消费编排、同事务双 save、`active_key` 唯一槽位。详见 ADR-0026。
>
> 沿革说明（2026-08-16 检视修订）：本文正文（决策/编排示例）仍为密封子类层次 + `VerificationScene`/`VerificationStatus` + `VerificationService` 编排的沿革表述，**不改写**——现状以顶部 2026-08-15（单类塌缩）与 2026-08-16（source/recipient 重构、主体裁定、发码回归）修订注记及 ADR-0025/0026 为准。

> 再修订（2026-08-15）：**Verification 塌缩为单类**——`SmsVerification`/`EmailVerification` 子类型删除（撤销 2026-08-05 类层次保留三理由与 2026-08-07 send 聚合行为定位）；通道判别栖身密封 `Subject`；send 分派移投递侧监听器（subject 模式匹配），聚合保留 `I → P` 迁移行为；`channel` 列、`VerificationChannels` 映射、JSON 判别属性删除；`VerificationChannel` 枚举仅命令输入判别。详见 ADR-0025。
>
> 修订（2026-08-12）：**验证码发起迁出 User 聚合**——`User.requestChangeMobileCode`/`requestChangeEmailCode` 删除，发码用例统一由 `VerificationService.requestCode` 编排；`Verification` 重分类为独立聚合根（`extends Aggregate<UUId>`）、`userId` 可空（RG 无 user）、CC 前置（启用态/target≠当前值）降级为应用层编排。其余机制不变：I→P→V→U 状态机、事件驱动 AFTER_COMMIT 投递、`CredentialChangeDomainService` 消费编排、同事务双 save 策略。详见 ADR-0021。
>
> 后续修订（2026-08-01）：`Verification` 重新分类为独立验证实体（无子实体，`extends Entity<UUId>`），与其子类 `SmsVerification`/`EmailVerification` 一并移至 `com.soda.user.domain`（soda-user-domain）。
>
> 同日再修订：`VerificationCode`/`VerificationPolicy`/`VerificationScene`/`VerificationStatus` 四个验证 DP 同步迁至 `com.soda.user.domain.types`（soda-user-domain）——验证不是通用业务类型，与 `Verification` 实体同域。其余决策（验证状态不内嵌 SmsAuthAccount、外部实体参数传递）保持不变。
>
> 再修订（2026-08-02）：`VerificationCode` 移除 `attempts`（不再跟踪尝试次数，`attempt()`/`exceededAttempts()` 删除）；`VerificationPolicy` 删除并并入 `VerificationCodePolicy`（唯一验证码策略，作为 `Verification.policy` 与账号配置字段，原双 DP 同形漂移消除）；`Verification.verify` 守卫简化为 待验证 → 未过期 → 码匹配，`inputCode` 为 null 时抛 NPE（ADR-0015 契约违反）；失败路径（错码/过期）不再落库——实体无变更，无 attempts 可累计。
>
> 同日再修订：`verifyMobile`/`verifyEmail` 的幂等检查收紧为**唯一性校验**——同一 (userId, scene=CC) 至多一条未过期 PENDING：已存在时 ApplicationService 抛 `IllegalArgumentException` 拒绝（不再仅 target 相同时静默返回）。查询语义见 `VerificationGateway.findLatestUnexpiredByUserAndScene`（status + validUntil 过滤，default 便捷检查 `hasUnexpiredPending`）。
>
> 再修订（2026-08-03）：职责拆分——`requestMobileVerification`/`requestEmailVerification`（生成码、构造 PENDING 验证聚合、发送）从 `CredentialChangeDomainService` 移至 `UserAuthServiceImpl.verifyMobile`/`verifyEmail` 内展开。依据：领域服务只承载**跨聚合编排**（同时更改多个聚合的流程），验证码发起只涉及单聚合创建，属应用层用例流程；`CredentialChangeDomainService` 仅保留 `changeMobile`/`changeEmail`（verify → user.changeXxx → use）。sender/generator 类 gateway 随之注入 AppService（framework-conventions「ApplicationService 编排规范」规则 5-6 同步修订）。
>
> 同日再修订：`VerificationGateway` 查询收敛为通用方法 `findLatestByUserId(userId, VerificationQuery)`——仅 userId 必传，scene / status / channel / validUntil 均为可选过滤（`VerificationQuery` 嵌套 record）；原 `findLatestUnexpiredByUserAndScene` 改名并参数化，类型收敛变体保留 `Class<T>` 参数。
>
> 再修订（2026-08-03）：`findLatestByUserId` 的「最新」定义为按 `VerificationCode.expireAt()` 倒序（expireAt 最晚者优先）——聚合无创建时间戳，策略差异（DEFAULT_SMS 5 分钟 vs DEFAULT_EMAIL 30 分钟）下创建顺序与过期顺序可能不一致；唯一性校验保证 (userId, scene) 至多一条未过期 PENDING，调用方只需「未过期的待验证聚合」语义，排序键不影响判定。
>
> 再修订（2026-08-05）：gateway 查询收敛为 `Class<T>` 单轨——`VerificationQuery` 移除 `channel` 字段（保留 scene / status / validUntil）；`VerificationChannel` 删除 `Class` 字段与 `of(Class)` 反查（class→判别值映射归基础设施，见 ADR-0016）；应用层 `requireLatestUnexpiredPending` 随之消除 `VerificationChannel.of(type)` 与 `type::cast`。类层次保留理由明确化：编译期 target 契约（`changeMobile(SmsVerification)`）+ 前瞻 `AuthenticatorVerification` 行为差异 + 与 `AuthAccount` 对称（ADR-0016 决策 8）。
>
> 同日再修订：`Verification` 移除 `policy` 字段——策略仅在创建时作为输入（决定码长与过期时间），效果已物化进 `VerificationCode`（code + expireAt），后续状态机（verify 守卫、过期派生）、查询排序、应用层编排均不读取；`VerificationCodePolicy` 仅保留账号配置字段角色。`create` 工厂仍要求 policy 参数。
>
> 再修订（2026-08-07）：`VerificationStatus` 增加 `I`(Initialized)——工厂创建即为 INITIALIZED（未发送）；发送从 AppService 直接调用 sender 改为聚合行为方法 `send(SmsSender)` / `send(EmailSender)`（sender 以参数注入，领域层不持有 gateway 端口），发送成功后 `I → P`，发送失败（异常）时状态保持 `I`、不落库——「PENDING 蕴含已送达」不变量成立。状态机 `I → P → V → U`；`isPending` / 唯一性校验（status=P 查询）语义不变。
>
> 同日再修订：`Verification` 的 `create` 工厂 policy 参数**可空化**并置于末位——缺省取子类静态 `DEFAULT_POLICY`（`SmsVerification.DEFAULT_POLICY` = `VerificationCodePolicy.DEFAULT_SMS`，`EmailVerification.DEFAULT_POLICY` = `DEFAULT_EMAIL`），与 `SmsAuthAccount`/`EmailAuthAccount` 的 `create` 模式一致（per-account 覆盖 → 子类静态 `DEFAULT_POLICY` 解析链）；调用方（`UserAuthServiceImpl.verifyMobile`/`verifyEmail`）不再传 policy。
>
> 同日再修订：`User.changeMobile` / `changeEmail` 增加**场景校验**——验证聚合 `scene` 必须为 `CC`（credential change），其他场景（PR/LG/RG）的验证码不可用于换绑凭证；带消息 IAE（"verification scene must be credential change"）。应用层查询本就按 CC 过滤（`requireLatestUnexpiredPending`），本守卫在聚合边界防御性收口（ADR-0015 状态前置）。
>
> 同日再修订：`UserAuthServiceImpl.verifyMobile` / `verifyEmail` 增加**发码前置校验**（fail-fast，避免浪费发送）——(1) target 与用户当前 mobile/email 相同 → 带消息 IAE（"cannot change to the same mobile/email"，与 `User.changeXxx` 领域守卫同消息，先于发码拦截）；(2) target 全局唯一——`UserGateway.existsByMobile` / `existsByEmail`（对齐 `createUser` 的重复手机号/邮箱校验）。领域层 `changeXxx` 守卫保留（纵深防御，换绑同值/场景仍由聚合收口）。
>
> 再修订（2026-08-09）：`SmsAuthAccount`/`EmailAuthAccount` **移除** `verificationCodePolicy` 字段（不再持久化）——码形是通道级规则（短信=纯数字、邮箱=字母数字），非账号数据：生产代码零处读取该字段、所有创建路径恒取通道默认、override 链（SPI）未实现，属死字段（YAGNI）。**`DEFAULT_POLICY` 常量保留**（per-account 覆盖预留契约，见 ADR-0018）；create 的可空 policy 覆盖参数一并删除（零生产传参，与死字段同源 YAGNI，契约删除）。撤销 2026-08-05"仅保留账号配置字段角色"的定位（字段角色不再，常量角色保留）。`VerificationCodePolicy` 角色收敛为**通道常量载体 + `Verification.create` 输入类型**；`Verification.create` 的 policy 可空参数保留（缺省取子类静态 `DEFAULT_POLICY`）。同日 `VerificationCodePolicy` 重构为**嵌套 DP 值对象**——`codeLength`（`PositiveInt`）、`expiry`、`codeAlphabet`（`Alphabet`）三要素一体，嵌套 DP 在 JSON 中序列化为基本类型值（ADR-0018）：`DEFAULT_SMS` = 6位/5分钟/`Alphabet.DIGITS`、`DEFAULT_EMAIL` = 8位/30分钟/`Alphabet.ALPHANUMERIC`；`RandomStringGenerator` 契约变为 `generate(PositiveInt, Alphabet)`（见 ADR-0018）。

> 再修订（2026-08-10）：**验证码发起重构为「User 决策创建 + 事件驱动投递」**，取代 2026-08-03"发起在 AppService 内展开"与 2026-08-07"AppService 内调 send"两段（沿革保留）。
>
> 1. **User 聚合行为方法**：新增 `User.requestChangeMobileCode(Mobile, RandomStringGenerator)` / `requestChangeEmailCode(Email, RandomStringGenerator)`——自检规则（`mustEnable` + 目标与当前值不同，顺带修复原请求路径缺 `mustEnable` 的洞）→ 创建 INITIALIZED 验证实体（scene=CC 硬编码）→ 返回实体（不持有、不持久化）。跨实例前置（`existsByMobile`/`existsByEmail` 全局唯一、`hasUnexpiredPending` 无未过期 PENDING）留在 ApplicationService 查询拦截（fail-fast，需查询故不入聚合）。
> 2. **投递改 AFTER_COMMIT 事件驱动**：`Verification` 创建工厂（`createBuilder`，同 `User.createBuilder` 模式）注册单一 `VerificationCreatedEvent`（携带实体）；AppService 持久化（I 落库）后 `publishAll(verification.flushEvents())`；`VerificationCreatedEventHandler`（application/event，`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)` + `@Transactional`）JEP 441 模式匹配选通道 sender（ADR-0016 规则 6）→ `verification.send(sender)` → P 落库（新事务）。
> 3. **动机**：(a) DB 事务不再跨外部投递通道（SMTP/短信）持有（IDDD 反模式修复）；(b) 记录先于发送持久化——"sender 成功但事务回滚→有码无记录"的幽灵码窗口消除；(c) AppService 依赖收敛（`EmailSender`/`SmsSender` 移出，仅留 `RandomStringGenerator`——码必须同步生成落库）。
> 4. **失败语义**：sender 契约升级——`EmailSender`/`SmsSender` 类级 javadoc 声明"返回即已确认投递，实现层自行保证（内部重试/补偿）；抛异常视为未投递"。监听器**无重试无 catch**：send 抛异常（契约违反）→ 异常上抛、记录保持 INITIALIZED（无变更不落库），用户重发自愈。"PENDING 蕴含已送达"不变量不变（P 仅在 send 返回后落）。残余窗口（诚实记录）：发送成功后 P 落库失败 → 码作废、记录停 I，用户重发自愈——与旧窗口同源（发送后瞬间 DB 故障），非 outbox+MQ 不可消除；email/SMS 本质 at-most-once，投递层本无事务保证。
> 5. **状态语义**：`I` = 已创建、待投递（原"已初始化未发送"扩展）；`P` 语义不变。并发双请求竞态（先查后建非原子）不变；孤儿 I 清理 job 与 DB 唯一约束列为基础设施 TODO。
> 6. **命名**：RPC/AppService/Controller 全链路 `verifyMobile`/`verifyEmail` → `requestChangeMobileCode`/`requestChangeEmailCode`（`Verification.verify` 的校验语义无碰撞；Command/Request/URL 同步改）。
>
> 再修订（2026-08-13）：`VerificationStatus` 改名 `VerificationState`（对齐 ADR-0005 命名规则——状态机枚举用 `XxxState`）；状态属性统一 `state`：`Verification` 实体字段 `status`→`state`（getter `getState()`）、`VerificationQuery.status`→`state`、`user_verification.status` 列→`state`（含索引名 `idx_user_id_scene_state_expire_at`）、JSON 判别属性 `status`→`state`。本文正文及既往修订中的 `VerificationStatus`/`status` 为改名前的沿革表述，不改写。

`Verification` 作为独立验证实体放在 `com.soda.user.domain`（soda-user-domain，2026-08-01 修订），支持多种验证方式（SMS、Email、Authenticator），与 `AuthAccount` 对称设计。User 的 `changeMobile` / `changeEmail` 接收对应 Verification 子类型作为参数，验证通过后执行领域行为。

## 问题

`SmsAuthAccount` 同时承担两个职责：持久的认证凭证（登录用的手机号）和临时的验证状态（验证码 code + expiresAt + used）。这两个概念生命周期完全不同：

- AuthAccount：永久，跟随 User 聚合
- 把临时验证码（5分钟生命周期）塞进持久的 SmsAuthAccount——污染领域实体，生命周期不匹配
- 验证码和 User 聚合的生命周期被绑定——但它们本不应该绑定
把临时验证码存在 SmsAuthAccount 里，导致：
1. 每次保存 User 聚合都附带保存临时验证码——污染持久化
2. 验证码和 User 聚合的生命周期被绑定——但它们本不应该绑定
3. 并发修改同一个 User 时，验证码状态会互相覆盖

同时，验证是一个跨用例复用的业务概念——手机换绑、邮箱换绑、登录验证都需要。它作为用户域内独立的领域类型存在，而不是 User 聚合的私有实现。

## 决策

### 验证聚合（soda-user-domain）

`Verification` 是密封类层次结构，与 `AuthAccount` 对称：

```java
// 独立验证实体（sealed class，extends Entity）
public abstract sealed class Verification extends Entity<UUId>
    permits SmsVerification, EmailVerification {
    VerificationScene scene;   // 区分不同用途，防止多调用方冲突
    VerificationStatus status; // I → P → V → U（I=已初始化未发送，send 后转 P；过期是派生判断，不落状态）
    VerificationCode code;     // code + expireAt（单一事实源；used 不落字段，生命周期完全由 status 表达）
    LongId userId;
    // 策略不落实体——创建时作为输入（码长 + 过期时间），效果物化进 VerificationCode
    // ...
}

// 场景枚举（与 yudao SmsSceneEnum 对齐）
public enum VerificationScene implements EnumType {
    CC("Credential Change"),     // 换绑联系方式/凭证变更（手机/邮箱，由 Verification subtype 区分通道）
    PR("Password Reset"),     // 找回密码
    LG("Login"),              // 登录验证
    RG("Register");           // 注册验证

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationScene of(String name) {
        return ParseUtils.parseEnum(VerificationScene.class, name);
    }
}

// 子类型
public final class SmsVerification extends Verification {
    Mobile target;      // 新手机号
}

public final class EmailVerification extends Verification {
    Email target;       // 新邮箱
}
```

状态机：`I → P → V → U`（`I` 为工厂创建后的初始态；`send(SmsSender)` / `send(EmailSender)` 发送成功后转 `P`，发送失败保持 `I`）。`verify(Instant at, RandomString)`（时钟注入）依次前置：待验证状态 → 未过期（`code.expiredAt(at)`）→ 码匹配；前置失败抛带消息 IAE（业务参数校验，ADR-0015）；失败路径（错码/过期）不落库——实体无变更（无 attempts 可累计）。`use()` 仅允许 V→U。

### 验证 DP 归属

`VerificationCodePolicy`（codeLength + expiry；`maxAttempts` 已移除——不再跟踪尝试次数）与 `VerificationCode`、`VerificationScene`、`VerificationStatus` 一并位于 `soda-user-domain` 的 `com.soda.user.domain.types`——验证概念不是通用业务类型，随 `Verification` 实体同域。命名默认值：`DEFAULT_SMS`（6位/5分钟）、`DEFAULT_EMAIL`（8位/30分钟）。~~`SmsAuthAccount` 保留 `VerificationCodePolicy`（codeLength + expiry）字段作为认证方式的配置。~~ **已被 2026-08-09 修订取代**（字段与 create 参数均已移除，仅 `DEFAULT_POLICY` 常量保留，见顶部修订注记；本句仅作沿革记录）。

### User 的 changeMobile / changeEmail

Verification 作为外部聚合，User 不主动修改它，而是接收它作为参数：

```java
public void changeMobile(SmsVerification verification) {
    Assert.isTrue(Objects.equals(verification.getUserId(), getId().toLongId()), "verification userId must match this user");
    Assert.isTrue(Objects.equals(verification.getScene(), VerificationScene.CC), "verification scene must be credential change"); // 非 CC 场景的验证码不可用于换绑
    Assert.isTrue(Objects.equals(verification.getStatus(), VerificationStatus.V), "verification must be verified"); // USED 是终态，不可重放
    Mobile newMobile = verification.getTarget();
    Assert.isTrue(!Objects.equals(this.mobile, newMobile), "cannot change to the same mobile");
    this.mobile = newMobile;
    // 联动替换 SmsAuthAccount...
}

public void changeEmail(EmailVerification verification) {
    Assert.isTrue(Objects.equals(verification.getUserId(), getId().toLongId()), "verification userId must match this user");
    Assert.isTrue(Objects.equals(verification.getScene(), VerificationScene.CC), "verification scene must be credential change"); // 非 CC 场景的验证码不可用于换绑
    Assert.isTrue(Objects.equals(verification.getStatus(), VerificationStatus.V), "verification must be verified"); // USED 是终态，不可重放
    Email newEmail = verification.getTarget();
    Assert.isTrue(!Objects.equals(this.email, newEmail), "cannot change to the same email");
    this.email = newEmail;
    // 联动替换 EmailAuthAccount...
}
```

Application Service 编排（`UserAuthServiceImpl`）——按「ApplicationService 编排规范」：AppService 只做加载/委托/保存。~~验证码发起（生成码、构造 INITIALIZED 验证聚合、经聚合 `send(sender)` 发送）在 AppService 内展开（generator + sender 注入 AppService，2026-08-03 修订；发送封装为聚合行为方法并以 sender 参数注入，2026-08-07 修订）~~ **已被 2026-08-10 修订取代**——验证码发起改为 User 聚合行为（决策 + 创建）+ AFTER_COMMIT 事件驱动投递（沿革见顶部修订注记），AppService 仅保留跨实例前置（目标全局唯一、无未过期 PENDING）fail-fast 查询拦截。跨聚合消费流程在 `CredentialChangeDomainService`：
```java
// requestChangeMobileCode — AppService：加载 User（存在性校验）
//   → 前置 fail-fast：目标与当前值不同、existsByMobile 全局唯一、hasUnexpiredPending 无未过期 PENDING → 抛 IllegalArgumentException（不重发、不新建）
//   → 领域：user.requestChangeMobileCode(newMobile, generator) → SmsVerification（scene=CC, INITIALIZED，注册 VerificationCreatedEvent）
//   → verificationGateway.save（I 落库）→ publishAll(verification.flushEvents())
//   → 投递：VerificationCreatedEventHandler（AFTER_COMMIT）→ verification.send(smsSender) → P 落库（新事务）
// changeMobile — AppService：加载 User + 待验证聚合（CC + SmsVerification）
//   → CredentialChangeDomainService.changeMobile(user, verification, code)
//       内部：verification.verify(code) → user.changeMobile(verification) → verification.use()
//   → 保存顺序：先 userGateway.save(user)，再 verificationGateway.save(verification)（USED 终态）
//   → 失败路径（verify 抛异常）：不落库——实体无变更（无 attempts 可累计）
// 邮箱同理（requestChangeEmailCode / changeEmail）
```

### SmsAuthAccount 重构

删除临时状态字段，保留配置：
- **删除**：`verificationCode`、`verifyCode()`、`replaceCode()`、`useCode()`
- ~~**保留**：`VerificationCodePolicy`（认证方式的验证码策略配置）~~ —— **已被 2026-08-09 修订取代**（字段已移除，仅 `DEFAULT_POLICY` 常量保留，见顶部修订注记；本行仅作沿革记录）

## 被否定的方案

| 方案 | 否定原因 |
|---|---|
| 验证码存在 SmsAuthAccount | 临时数据污染领域实体，生命周期不匹配 |
| 验证码存在 UserGateway | Verification 是独立聚合，不应嵌入其他聚合的 Gateway |
| Verification 作为 User 子实体 | 验证码是临时的，User 是永久的，生命周期不匹配 |
| User.changeMobile 不接收 Verification | 无法在领域层守卫"必须已验证"的业务规则 |

## 参考

- **yudao-cloud**: `SmsCodeApi` 独立模块，通过 RPC 调用——验证与用户解耦
- **Eric Evans DDD**: 外部聚合通过参数传入，不被当前聚合修改
- **COLA 示例**: 跨模块共享 Domain Types
