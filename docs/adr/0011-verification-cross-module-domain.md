# Verification — 跨模块通用验证领域

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

Application Service 编排（`UserAuthServiceImpl`）——按「ApplicationService 编排规范」：AppService 只做加载/委托/保存；验证码发起（生成码、构造 INITIALIZED 验证聚合、经聚合 `send(sender)` 发送）在 AppService 内展开（generator + sender 注入 AppService，2026-08-03 修订；发送封装为聚合行为方法并以 sender 参数注入，2026-08-07 修订），跨聚合消费流程在 `CredentialChangeDomainService`：
```java
// verifyMobile — AppService：加载 User（存在性校验）
//   → 唯一性校验：同一 (userId, scene=CC) 已存在未过期 PENDING（无论 target）→ 抛 IllegalArgumentException（不重发、不新建）
//   → 验证码发起（AppService 内展开）：生成码 → 构造 SmsVerification（scene=CC, INITIALIZED）→ verification.send(smsSender)（发送 + 转 PENDING）
//   → verificationGateway.save（落库 PENDING；发送失败时异常上抛、零落库）
// changeMobile — AppService：加载 User + 待验证聚合（CC + SmsVerification）
//   → CredentialChangeDomainService.changeMobile(user, verification, code)
//       内部：verification.verify(code) → user.changeMobile(verification) → verification.use()
//   → 保存顺序：先 userGateway.save(user)，再 verificationGateway.save(verification)（USED 终态）
//   → 失败路径（verify 抛异常）：不落库——实体无变更（无 attempts 可累计）
// 邮箱同理（验证码发起 / changeEmail）
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
