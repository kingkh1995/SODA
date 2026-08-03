# 0015 — 异常类使用约定：防御（NPE / ISE）与参数校验（IAE）

> 修订（2026-08-02）：防御状态守卫改用 `Assert.state(condition, message)` → ISE（带开发消息）——Spring 7 的 Assert 仅剩带消息重载（`state(boolean)` 无消息版本已移除），消息仅作开发定位（ISE 仍映射 500，不承诺客户端反馈）；null 守卫维持 NPE（`Objects.requireNonNull`，JEP 358 自动帮助消息），错码维持带消息 IAE（`Assert.isTrue`）。应用：`Verification.verify` / `use`。
>
> 修订（2026-08-03）：新增 `Guard.state(boolean)`（`com.soda.component.domain.util.Guard`）提供无消息 ISE 形式，与带消息 `Assert.state` 并存；正文 Decision/Consequences 同步更新。

**Status**: accepted

## Context

写侧（ApplicationService / DomainService / Entity）的异常用法此前混杂：`Assert.notNull` / `Assert.state` / 手写 `throw` / `Objects.requireNonNull` 并存，IAE 与 ISE 的界限靠直觉判断，无书面约定。两个问题悬而未决：

1. **客户端可预期触发的失败**（错码、过期、超限、未请求验证码、重复用户名）与**业务上不应出现的状态**（如用户无密码账户）各应抛什么异常？
2. 既有代码存在反例：`Verification.verify` / `use` 对过期、超限、状态错位抛 `IllegalStateException`（客户端可预期触发）；`UserAuthServiceImpl` 对「无 pending 验证」用 `Assert.state`。

ADR-0013 已定义 `ErrorInfo` 信封与 AIP-193 语义码映射，但异常类 → HTTP 的接线未实现（deferred，见 issue 17）。本 ADR 只定异常类使用约定，不实现接线。

代码事实：注册强制创建密码账户（`UserServiceImpl` 无条件 hash password），故「用户无密码账户」在正确代码与正确客户端流程下不可能出现。

## Decision

### 两类异常：防御编程（NPE / ISE）与参数校验（IAE）

| 类别 | 检查形式 | 工具 → 异常 | 消息 |
|---|---|---|---|
| 防御编程 | 参数或自身字段为 null（契约违反） | `Objects.requireNonNull` → `NullPointerException` | 无 |
| 防御编程 | 非 null 条件不满足（自身状态不允许操作） | `Assert.state(condition, message)` → `IllegalStateException`（开发消息）；无消息形式用 `Guard.state(boolean)` | 开发定位消息（不承诺客户端） |
| 参数校验 | 输入值不合法 / 业务规则拒绝（客户端可预期） | `Assert.isTrue` / `Assert.notNull` → `IllegalArgumentException` | 带消息 |

- **NPE 与 ISE 本质同类**：都是对自身不合法状态的防御（编程错误防御），区别只在检查形式——null 用 NPE，其他条件用 ISE。判定不区分「参数 null」还是「自身字段 null」：null 一律 NPE。
- **防御 ISE 的两种表达**：带开发消息用 Spring `Assert.state(condition, message)`（Spring 7 仅剩带消息重载）；无消息形式用 `Guard.state(boolean)`（`com.soda.component.domain.util.Guard`，2026-08-03 新增，见修订注记）。带消息的 IAE 用 `Assert.isTrue` / `Assert.notNull`。
- **消息策略**：防御（NPE/ISE）无承诺消息——ISE 消息仅作开发定位（映射 500，不反馈客户端）；校验（IAE）带消息——IAE 消息是客户端当前唯一的反馈通道（wiring 未实现，无 reason 可依赖；ADR-0013：msg 不本地化）。
- 手写 `throw` 用于多分支 / 带副作用场景（如 `Verification.verify` 错码前需 `attempt()`），异常类仍须符合本分类。
- `Objects.requireNonNullElse` 仅用于默认值模式（如 `User` 构造器 accounts 缺省），不属于守卫。

### 防御编程异常词汇表（JDK 原生）

防御编程不自定义异常，用 JDK 原生类按失败模式细分：

| 失败模式 | JDK 异常 | 触发方式 | 现状 |
|---|---|---|---|
| null 契约违反 | `NullPointerException` | `Objects.requireNonNull` / 解引用自动（JEP 358 帮助消息） | 使用中 |
| 状态前置不满足 | `IllegalStateException` | `Assert.state(cond, msg)` 带开发消息 / `Guard.state(boolean)` 无消息 | 使用中（`Verification.verify` / `use`） |
| 容器 / Optional 查找为空（结构性不变量违反） | `NoSuchElementException` | `Optional.orElseThrow()`（无参，消息为 JDK 自带） | 新增（`User.changePassword` 密码账户缺失） |
| 变体不支持该操作 | `UnsupportedOperationException` | 默认实现 `throw`（JDK 不可变集合惯例） | 预留，当前无场景 |
| 类型混淆 | `ClassCastException` | 强转自动 | 预留（typed lookup 已消除强转） |

- **NoSuchElement 边界**：只用于聚合内部查找（领域对象自身承诺的结构不变量）；网关加载「未找到」是客户端可预期 → IAE（临时方案，接线时演进为带 reason 的业务异常，不归防御类）。
- 不要用 `orElse(null)` + `requireNonNull` 把 Optional 空值转成 null——丢失「查找为空」与「值为 null」的区分；空值 = 不变量违反时直接用 `orElseThrow()`。
- `UnsupportedOperationException` / `ClassCastException` 无场景不引入，词汇表先行登记，出现场景再使用（YAGNI）。

### 防御代码最少化（JDK 能力优先）

防御代码的原则：**先找 JDK 现成能力，再考虑手写守卫；守卫存在但不重复**。

- null 守卫 = `Objects.requireNonNull`（可内联进赋值 / 调用，一行）；不写消息——JEP 358 自动提供帮助消息（含参数名 / 表达式），比手写消息更好
- **冗余守卫删除**：依赖链上后续调用已自动 NPE 时（如 `generator.get()`），前置守卫删除——JVM 的 NPE 帮助消息比手写守卫信息量更大
- 查找为空 = `Optional.orElseThrow()`；DTO 可空字段映射 = `Optional.map().orElse(null)`（合法用法，非守卫）
- JDK 无对应的才手写：状态前置（ISE）、带副作用分支（错码 `attempt()`）
- **范围边界**：值对象校验（`ValidateUtils` → IAE）与 appservice 参数校验（`Assert` → IAE）是外部输入校验，当前为临时方案，明确不在防御优化范围（wiring 时演进为带 reason 的业务异常，见 issue 17）

### 操作语义：set-state（幂等）与 transition（严格）

| 语义 | 定义 | 已到达目标状态时 | 事件 |
|---|---|---|---|
| set-state（最终状态语义） | 「确保到达目标状态」 | 直接 return（no-op） | 不发 |
| transition（一次性迁移语义） | 「从 X 迁移到 Y」，前置必须精确 | 前置检查失败 → ISE（自身状态）/ IAE（输入） | 迁移成功才发 |

- set-state（`User.disable` / `enable`）：幂等 no-op，不注册事件——重试 / 双击 / 消息重投天然安全（REST PUT/DELETE 语义）。
- transition（`Verification.verify` / `use`）：严格——非 PENDING / 过期 / 超限 / 非 VERIFIED 均抛无消息 ISE；验证码不匹配抛带消息 IAE（输入校验）。API 层重试幂等（幂等键）将来在接线时引入，领域方法不负责重试去重。
- 例外（产品决策）：`changeMobile` / `changeEmail` 换绑到同值抛带消息 IAE（「cannot change to the same mobile」）——换绑是昂贵的验证码流程，同值几乎必然是操作失误，报错比静默成功好；与 disable 的 no-op 不矛盾（廉价开关 vs 昂贵带码流程）。

### HTTP 映射（接线 deferred，见 issue 17）

| 异常 | HTTP | reason |
|---|---|---|
| `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `NullPointerException`（参数守卫） / `IllegalStateException` | 500 | `INTERNAL` |
| `MethodArgumentNotValidException`（adapter `@Valid`） | 400 | `INVALID_ARGUMENT` |

ADR-0013 的 `ALREADY_EXISTS`(409) / `RESOURCE_EXHAUSTED`(429) 行**暂不启用**：两桶分类下客户端失败统一 400。将来需要语义区分（如前端区分「用户名被占用」与「格式错误」）时，再引入携带 reason / HTTP 状态的业务异常类型。

## Considered Options

- **防御检查用 `AssertionError`（「不可能发生」语义）** — 比 ISE 更强的断言。拒绝：Spring 生态与 java.util 惯例用 ISE；Error 与 Exception 的处理路径不同（测试框架、全局 handler、日志级别行为差异）；`assert` 关键字默认关闭，部分采用会导致不一致。
- **同值换绑 no-op（纯 REST 语义）** — 更新到同值返回成功，与 disable 的幂等一致。拒绝：昂贵验证码流程中同值 = 操作失误，报错反馈更明确；且避免「验证码不消费」的微妙状态。
- **依赖 Assert 无消息重载（`Assert.state(boolean)`）** — Spring 6 存在，Spring 7 已移除（仅剩带消息版本）。无消息 ISE 由仓库自有 `Guard.state(boolean)` 提供（2026-08-03）。
- **参数守卫用 `Assert.notNull`（IAE）** — 初版 ADR 的「守卫豁免」选择，与 Spring 生态惯例一致。问题：编程错误与客户端错误同桶，全局映射下 bug 会被 400 伪装。修订为守卫归 NPE（`Objects.requireNonNull`）。
- **三桶（新增业务规则异常携带 reason/HTTP 状态）** — 语义更丰富，409/429 可直接表达，前端可按 reason 分支。代价：多一个异常类型与映射行，当前阶段无程序化消费方。拒绝，409/429 语义 deferred。
- **防御式守卫用 `Assert.state`（ISE）** — 字面贴合「代码未处理 → ISE」。代价：null 守卫抛 ISE 违背 Spring 生态与 Java 惯例，读代码者会困惑。拒绝。
- **只写文档不清理既有反例** — 约定与 8 处既有 ISE 反例并存，从第一天起被违反。拒绝，随本 ADR 一并清理。

## Consequences

- 既有代码清理（随本 ADR，含二次修订）：
  - **状态检查 → ISE**：`Verification.verify`（非 pending / 过期 / 超限）与 `use`（非 VERIFIED）用 `Assert.state(condition, message)`（开发定位消息）；无消息形式可用 `Guard.state(boolean)`。`VerificationTest` 6 处、`UserAuthServiceImplTest` 2 处断言同步。
  - **null 守卫 → NPE**：`Identifiable.requireId()`（瞬态 id）、构造器守卫（`User` / `AuthAccount` / `PasswordAuthAccount` / `Verification` 共 16 处）、`UserDTOConvertor` 改用 `user.requireId()`。
  - **IAE 带消息保留**：`Verification.verify` 错码、`User.changeMobile` / `changeEmail` 验证码状态 / 归属 / 同值、appservice 全部（User not found / No pending / 重复用户名）。
  - **守卫去消息**：`EmailAuthAccount` / `SmsAuthAccount` 的 `requireNonNull` 移除消息参数。
  - **NoSuchElement 细化**：`User.changePassword` 密码账户缺失用 `findActiveAccount(...).orElseThrow()`（NoSuchElementException）——「查找为空」与「值为 null」分离，JDK Optional 惯用法（不再误述为 NPE）。
  - **Entity 基类守卫 JDK 化**：`Assert.notNull`（IAE 带消息）→ `Objects.requireNonNull`（NPE 无消息），共 5 处；`Entity(Supplier)` 删除冗余的 generator 守卫（`generator.get()` 自动 NPE + JEP 358 帮助消息）。
- 新增代码遵循本约定，code review 按「防御（NPE/ISE 无消息）/ 校验（IAE 带消息）」+ 操作语义检查。
- 接线（issue 17）按映射表施工，无需再议；实现时 IAE 的 message 为开发调试信息（ADR-0013：不本地化）。
