# 0015 — 异常类使用约定：构造器校验与业务参数校验（IAE）

> 修订（2026-08-02）：防御状态守卫改用 `Assert.state(condition, message)` → ISE（带开发消息）——Spring 7 的 Assert 仅剩带消息重载（`state(boolean)` 无消息版本已移除），消息仅作开发定位（ISE 仍映射 500，不承诺客户端反馈）；null 守卫维持 NPE（`Objects.requireNonNull`，JEP 358 自动帮助消息），错码维持带消息 IAE（`Assert.isTrue`）。应用：`Verification.verify` / `use`。
>
> 修订（2026-08-03）：新增 `Guard.state(boolean)`（`com.soda.component.domain.util.Guard`）提供无消息 ISE 形式，与带消息 `Assert.state` 并存；正文 Decision/Consequences 同步更新。
>
> 修订（2026-08-05）：判定修正——实体防御编程只覆盖「数据组成合法性」（结构不变量，null → NPE）；状态机业务前置（verify 非 PENDING / 已过期、use 非 VERIFIED）是业务合法性校验，转带消息 IAE（`Assert.isTrue`）。应用：`Verification.verify` / `use`；`VerificationTest` 5 处、`UserAuthServiceImplTest` 1 处断言同步。
>
> 修订（2026-08-07）：**ISE 分类退役**——防御只剩 null 守卫（NPE）；客户端可触发的一切（含状态机前置）归业务参数校验（IAE，带消息）；聚合内部结构性查找（`User.requirePasswordAccount` 密码账户缺失）用 `Optional.orElseThrow()` → `NoSuchElementException`（无消息）。`Guard.state` 删除（无调用方）。jspecify 注解按 Spring 最佳实践使用，编译期 nullness checker 暂不引入（详见「防御代码最少化」）。应用：`User.changePassword`、`UserTest` 断言同步。
>
> 修订（2026-08-07 二次）：**领域层运行时守卫全部退役**——构造器 / 方法参数 null 检查（`Objects.requireNonNull`）、`Identifiable.requireId()`、`User.requirePasswordAccount()` 全部删除。契约由 jspecify `@NullMarked` 声明（默认非空）+ 调用方遵守，未来编译期 checker（NullAway）enforce。聚合不变量**结构化**：`PasswordAuthAccount` 成为 `User` 构造器必填字段（ADR-0004 类型化，无密码账户的 User 不可表示；`accounts` 仅存可选账户，`addAccount` 拒绝密码账户）。JSON 恢复路径由 schema 强制（`@JsonProperty(required = true)`：id 与 passwordAccount 必填，缺 id 由 Jackson 在协议边界拒绝）。可空查找返回 Optional；requireXXX 模式（fetch → null check → throw IAE）仅留在 appservice。应用：`User` / `Entity` / `Identifiable` / `Verification` / 全部 `AuthAccount` 子类；测试同步（删除守卫断言，「缺少 id 的 JSON 拒绝」改为 schema 语义）。
>
> 修订（2026-08-07 三次）：**构造器校验回归（DP 式）**——实体构造器恢复参数校验：非空参数用 `ValidateUtils.notNull`（IAE，固定标准消息，与 DP 构造器完全一致）；**方法路径保持零守卫**（契约由 jspecify 声明）。「属性合法」的保证点回到构造器（创建 / 恢复路径统一拦截）。恢复路径 JSON 的**全部非空字段**声明 `@JsonProperty(required = true)`（schema 完整性 = 类型的镜像，缺字段由 Jackson 在协议边界拒绝），可空字段标 `@Nullable`。应用：`User` / `AuthAccount` 及子类 / `Verification` / `SmsVerification` / `EmailVerification` 构造器；`EmailAuthAccountTest` / `UserTest` 断言。
>
> 修订（2026-08-08）：**已被 ADR-0017（2026-08-09）取代**（生命周期收敛为单维状态机，本段仅作分类学沿革记录）。原内容：`User` 新增 transient `removed` 标志与 `remove()`（严格 transition：前置必须禁用 D，成功注册 `UserRemovedEvent` 并置位）；公共 `mustEnable()`（E 前置，变更方法统一调用）；私有 `assertNotRemoved()`（removed 后任何行为调用抛 IAE 带消息，含重复 remove）。分类不是非 null 守卫（jspecify 契约，已退役），而是**生命周期状态守卫**——物理删除后聚合不可再加载（跨请求由 appservice `require` not-found 兜底），本守卫只防御同事务内同一实例的误用（remove 后 save 的 JPA merge 行为未定义，可能幽灵复活；重复 remove 产生重复事件）。`isRemoved()` 供基础设施实现 save 防御（UserGateway javadoc 契约约定）。标志 `@JsonIgnore` + transient：不参与序列化与相等性（恢复路径必为存活聚合）。

**Status**: accepted

## Context

写侧（ApplicationService / DomainService / Entity）的异常用法此前混杂：`Assert.notNull` / `Assert.state` / 手写 `throw` / `Objects.requireNonNull` 并存，IAE 与 ISE 的界限靠直觉判断，无书面约定。两个问题悬而未决：

1. **客户端可预期触发的失败**（错码、过期、超限、未请求验证码、重复用户名）与**业务上不应出现的状态**（如用户无密码账户）各应抛什么异常？
2. 既有代码存在反例：`Verification.verify` / `use` 对过期、超限、状态错位抛 `IllegalStateException`（客户端可预期触发）；`UserAuthServiceImpl` 对「无 pending 验证」用 `Assert.state`。

ADR-0013 已定义 `ErrorInfo` 信封与 AIP-193 语义码映射，但异常类 → HTTP 的接线未实现（deferred，见 issue 17）。本 ADR 只定异常类使用约定，不实现接线。

代码事实：注册强制创建密码账户（`User.register` 无条件 `addAccount(PasswordAuthAccount)`），故「用户无密码账户」在正确代码与正确客户端流程下不可能出现——它是聚合结构不变量（ADR-0004），不是客户端可预期失败。

## Decision

### 构造器校验（DP 式）+ 方法零守卫（2026-08-07 三次修订）

| 类别 | 机制 | 异常 |
|---|---|---|
| 业务参数校验（输入值 / 业务规则 / 状态机前置，客户端可预期） | `Assert.isTrue` / `Assert.notNull` | `IllegalArgumentException`（带消息） |
| 构造器参数校验（创建与恢复路径统一） | `ValidateUtils.notNull`（固定标准消息，与 DP 构造器一致） | `IllegalArgumentException` |
| 方法参数 null 契约违反 | **无运行时守卫** — jspecify `@NullMarked` 声明（默认非空）+ 调用方遵守；真实 NPE 由 JEP 358 提供帮助消息；未来编译期 checker（NullAway）enforce | `NullPointerException` |
| 聚合内部结构不变量 | **类型化**（构造器必填字段，如 `User.passwordAccount`）+ JSON schema（全部非空字段 `required = true`） | 不可表示 / 边界拒绝 |
| 可空查找 | `Optional` 返回，由调用方决定（appservice 用 requireXXX 模式 → IAE） | — |

- **构造器 = 属性合法的保证点**：实体构造器（创建 / 恢复路径统一，恢复委托创建构造器）对非空参数用 `ValidateUtils.notNull` 校验——与 DP 构造器完全一致（同一工具、同一固定消息、同一 IAE）。「DDD 保证属性合法」由此落地：构造之后方法不再重复校验。
- **方法路径零守卫**：业务方法无 `Objects.requireNonNull`；`Identifiable.requireId()`、`User.requirePasswordAccount()` 已删除。事件注册等内部调用直接传递（如 `new PasswordChangedEvent(getId())`），实体假设调用方已保证 id 分配（appservice 从网关加载的聚合必然已持久化）。
- **不变量结构化 + schema 完整性**：ADR-0004「User 必有密码账户」由 `User` 构造器必填 `PasswordAuthAccount` 字段强制（`accounts` 仅存可选账户，`addAccount` 以 IAE 拒绝密码账户）；恢复路径 JSON 的**全部非空字段**声明 `@JsonProperty(required = true)`（schema = 类型的镜像），可空字段（mobile / email / sex / avatar / accounts）标 `@Nullable`——缺字段由 Jackson 在协议边界拒绝（框架能力，非领域守卫）。
- **判定原则（检查对象）**：检查「参数值 / 业务状态是否允许操作」→ IAE 校验——客户端可预期触发的一切（错码、过期、重复用户名、未请求验证码、状态机前置）都是业务校验；「值是否为 null」→ 构造器拦截，方法不检查（契约）。
- **消息策略**：IAE 带消息——是客户端当前唯一的反馈通道（wiring 未实现，无 reason 可依赖；ADR-0013：msg 不本地化）。构造器校验用 `ValidateUtils` 固定消息（"must not be null"），不自定义。
- `Objects.requireNonNullElse` 仅用于默认值模式（如 `User` 构造器 accounts 缺省），不属于守卫。

### 防御异常词汇表（JDK 原生，预留）

防御层已无显式守卫，以下 JDK 异常按失败模式预留（出现即登记，无场景不引入，YAGNI）：

| 失败模式 | JDK 异常 | 触发方式 | 现状 |
|---|---|---|---|
| null 契约违反 | `NullPointerException` | 真实解引用（JEP 358 帮助消息）；未来 checker enforce | 预留（无显式守卫） |
| 聚合内部结构性查找为空 | `NoSuchElementException` | `Optional.orElseThrow()` | 预留（领域当前无使用点） |
| 变体不支持该操作 | `UnsupportedOperationException` | 默认实现 `throw`（JDK 不可变集合惯例） | 预留 |
| 类型混淆 | `ClassCastException` | 强转自动 | 预留（typed lookup 已消除强转） |

### 防御代码最少化（JDK / 框架能力优先）

- **构造器校验 + 方法零守卫**（2026-08-07 三次修订）：属性合法的保证点在构造器（`ValidateUtils.notNull`，与 DP 一致，固定消息）；方法路径零守卫——契约（jspecify `@NullMarked`）+ 结构（构造器签名 / schema required）+ 边界校验（IAE）三分
- 边界用框架能力：JSON 必填字段 = `@JsonProperty(required = true)`（全部非空字段，Jackson 在协议边界拒绝，非领域守卫）；DTO 可空字段映射 = `Optional.map().orElse(null)`（合法用法）
- 业务校验 = `Assert.isTrue` / `Assert.notNull`（带消息）；JDK / Assert 全覆盖，手写 `throw` 无场景
- **范围边界**：值对象校验（`ValidateUtils` → IAE）与 appservice 参数校验（`Assert` → IAE）是外部输入校验，当前为临时方案（wiring 时演进为带 reason 的业务异常，见 issue 17）
- **编译期 checker（未来）**：jspecify 注解按 Spring 最佳实践（包级 `@NullMarked`、只标 `@Nullable`、不返回 `Optional<@Nullable T>`；Spring 7 自身即 `@NullMarked`）。路线已核实可行：Error Prone 2.45.0（支持 Java 25）+ NullAway 0.12.6（`NullAway:OnlyNullMarked` 渐进）+ `NullAway:CustomContractAnnotations=org.springframework.lang.Contract`（Spring 7 `Assert.notNull` 已带 `@Contract("null, _ -> fail")`）

### 操作语义：set-state（幂等）与 transition（严格）

| 语义 | 定义 | 已到达目标状态时 | 事件 |
|---|---|---|---|
| set-state（最终状态语义） | 「确保到达目标状态」 | 直接 return（no-op） | 不发 |
| transition（一次性迁移语义） | 「从 X 迁移到 Y」，前置必须精确 | 前置检查失败 → IAE（带消息） | 迁移成功才发 |

- set-state（`User.disable` / `enable`）：幂等 no-op，不注册事件——重试 / 双击 / 消息重投天然安全（REST PUT/DELETE 语义）。
- transition（`Verification.verify` / `use` / 子类 `send`）：严格——非 PENDING / 过期 / 非 VERIFIED / 非 INITIALIZED 抛带消息 IAE（业务状态前置，客户端可预期）；验证码不匹配抛带消息 IAE（输入校验）。API 层重试幂等（幂等键）将来在接线时引入，领域方法不负责重试去重。
- 例外（产品决策）：`changeMobile` / `changeEmail` 换绑到同值抛带消息 IAE（「cannot change to the same mobile」）——换绑是昂贵的验证码流程，同值几乎必然是操作失误，报错比静默成功好；与 disable 的 no-op 不矛盾（廉价开关 vs 昂贵带码流程）。

### HTTP 映射（接线 deferred，见 issue 17）

| 异常 | HTTP | reason |
|---|---|---|
| `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `NullPointerException` / `NoSuchElementException`（预留） | 500 | `INTERNAL` |
| `MethodArgumentNotValidException`（adapter `@Valid`） | 400 | `INVALID_ARGUMENT` |

ADR-0013 的 `ALREADY_EXISTS`(409) / `RESOURCE_EXHAUSTED`(429) 行**暂不启用**：当前分类下客户端失败统一 400。将来需要语义区分（如前端区分「用户名被占用」与「格式错误」）时，再引入携带 reason / HTTP 状态的业务异常类型。

## Considered Options

- **方法路径运行时 null 守卫（业务方法 `Objects.requireNonNull`、`requireId()`、`requirePasswordAccount()`）** — 2026-08-07 前版本。拒绝（二次修订）：方法守卫与 jspecify 契约重复——`@NullMarked` 已声明默认非空，调用方遵守后守卫永不触发（死代码）；`requirePasswordAccount` 由结构化必填字段替代（不可表示的状态无需检查）；`UserCreatedEvent.entityId()` 保留延迟求值语义（assignId 前返回 null，契约要求持久化后取）。**构造器除外**——三次修订回归 DP 式构造器校验（`ValidateUtils`，属性合法的保证点）。
- **防御检查用 `AssertionError`（「不可能发生」语义）** — 比 ISE 更强的断言。拒绝：Spring 生态与 java.util 惯例用 ISE；Error 与 Exception 的处理路径不同（测试框架、全局 handler、日志级别行为差异）；`assert` 关键字默认关闭，部分采用会导致不一致。
- **保留 ISE 分类（结构不变量防御）** — 2026-08-05 曾保留给「数据组成合法性」，配套 `Guard.state`（2026-08-03）。拒绝（2026-08-07）：非 null 条件要么客户端可触发（归 IAE），要么是聚合内部结构性查找（归 `NoSuchElementException`）；保留 ISE 需要额外工具且分类判定负担重。`Guard` 已删除。
- **同值换绑 no-op（纯 REST 语义）** — 更新到同值返回成功，与 disable 的幂等一致。拒绝：昂贵验证码流程中同值 = 操作失误，报错反馈更明确；且避免「验证码不消费」的微妙状态。
- **依赖 Assert 无消息重载（`Assert.state(boolean)`）** — Spring 6 存在，Spring 7 已移除（仅剩带消息版本）。曾由仓库自有 `Guard.state(boolean)` 提供（2026-08-03），随 ISE 退役一并删除（2026-08-07）。
- **参数守卫用 `Assert.notNull`（IAE）** — 初版 ADR 的「守卫豁免」选择，与 Spring 生态惯例一致。问题：编程错误与客户端错误同桶，全局映射下 bug 会被 400 伪装。修订为守卫归 NPE（`Objects.requireNonNull`）。
- **三桶（新增业务规则异常携带 reason/HTTP 状态）** — 语义更丰富，409/429 可直接表达，前端可按 reason 分支。代价：多一个异常类型与映射行，当前阶段无程序化消费方。拒绝，409/429 语义 deferred。
- **防御式守卫用 `Assert.state`（ISE）** — 字面贴合「代码未处理 → ISE」。代价：null 守卫抛 ISE 违背 Spring 生态与 Java 惯例，读代码者会困惑。拒绝。
- **只写文档不清理既有反例** — 约定与既有反例并存，从第一天起被违反。拒绝，随本 ADR 一并清理。

## Consequences

- 既有代码清理（随本 ADR 演进）：
  - **状态检查 → IAE（2026-08-05）**：`Verification.verify`（非 pending / 过期）与 `use`（非 VERIFIED）用 `Assert.isTrue(condition, message)`（业务状态前置 → 业务合法性校验，客户端可预期）。`VerificationTest` 5 处、`UserAuthServiceImplTest` 1 处断言同步。
  - **null 守卫 → NPE**：`Identifiable.requireId()`（瞬态 id）、构造器守卫（`User` / `AuthAccount` / `PasswordAuthAccount` / `Verification` 共 16 处）、`UserDTOConvertor` 改用 `user.requireId()`。
  - **IAE 带消息保留**：`Verification.verify` 错码、`User.changeMobile` / `changeEmail` 验证码状态 / 归属 / 同值、appservice 全部（User not found / No pending / 重复用户名）。
  - **守卫去消息**：`EmailAuthAccount` / `SmsAuthAccount` 的 `requireNonNull` 移除消息参数。
  - **NoSuchElement 细化（2026-08-07）**：`User.requirePasswordAccount` 密码账户缺失用 `orElseThrow()`（NoSuchElementException，无消息）——「查找为空」与「值为 null」分离，JDK Optional 惯用法；`UserTest` 断言同步（ISE → NoSuchElement，去消息断言）。
  - **Entity 基类守卫 JDK 化**：`Assert.notNull`（IAE 带消息）→ `Objects.requireNonNull`（NPE 无消息），共 5 处；`Entity(Supplier)` 删除冗余的 generator 守卫（`generator.get()` 自动 NPE + JEP 358 帮助消息）。
  - **ISE 退役与 `Guard` 删除（2026-08-07）**：`Guard.state` 无调用方，删除；领域 / 应用层无 `IllegalStateException` 残留。
  - **领域层守卫退役（2026-08-07 二次）**：`Entity` / `Identifiable` / `User` / `AuthAccount` 及子类 / `Verification` / `SmsVerification` / `EmailVerification` 的构造与参数守卫全部删除；`User.passwordAccount` 独立必填字段（`accounts` 拒绝密码账户）；`@JsonProperty(required = true)` 声明恢复路径必填字段（id、passwordAccount）；`UserCreatedEvent.entityId()` 改为延迟求值（assignId 前返回 null）；`UserDTOConvertor` 用 `user.getId()`。测试同步：删除守卫断言（null credential / hasher / policy、无密码账户、verify(null)），「缺少 id 的 JSON 拒绝」保留为 schema 语义。
  - **构造器校验回归（2026-08-07 三次）**：实体构造器（`User` / `AuthAccount` / `PasswordAuthAccount` / `EmailAuthAccount` / `SmsAuthAccount` / `Verification`）非空参数改用 `ValidateUtils.notNull`（IAE 固定消息，与 DP 一致）；恢复路径 JSON 全部非空字段 `@JsonProperty(required = true)`，可空字段标 `@Nullable`。测试：`EmailAuthAccountTest.constructor_rejectsNullPolicy`（IAE 语义）、`UserTest` 缺 passwordAccount → IAE。
- 新增代码遵循本约定，code review 按「null 守卫（NPE 无消息）/ 业务校验（IAE 带消息）」+ 操作语义检查。
- 接线（issue 17）按映射表施工，无需再议；实现时 IAE 的 message 为开发调试信息（ADR-0013：不本地化）。
