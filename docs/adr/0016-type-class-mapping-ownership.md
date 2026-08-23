# 0016 — 判别值映射归属与类型化查询收敛（type↔class）

**Status**: accepted

> 修订（2026-08-15）：`Verification` 多态塌缩为单类——本 ADR 的 verification 部分（class↔channel 映射 `VerificationChannels`、`@JsonTypeName` 判别、决策 8 类层次保留理由）**作废**；`VerificationChannel` 枚举仅保留命令输入判别；`AuthAccount` 侧不变。详见 ADR-0025。
>
> 修订（2026-08-16）：`VerificationChannel` 角色回归——① 预认证命令输入、② `VerificationRecipient` 判别/序列化属性（`channel` + `target` 双列持久化）；码形策略按场景而非通道选择（不引用通道默认，见 ADR-0026 §2/§6）。本 ADR verification 部分维持作废。

**Context**:

项目用 sealed 类层次表达"固定种类集"（`AuthAccount` 4 子类 / `Verification` 2 子类，JEP 409），判别值（短名）由领域枚举（`AuthAccountType` / `VerificationChannel`）承载；JSON 判别走 `@JsonTypeName` + Jackson 3 从 `permits` 自动发现子类（无 `@JsonSubTypes`）。

对设计结论的主流性审查（`docs/research/type-vs-class-mainstream-review.md`）判定方向全部符合主流实践，但标出三处过渡态违例：

| 现状 | 违例 |
|---|---|
| `VerificationChannel` 持 `Class<? extends Verification<?>>` 字段 + `of(Class<T>)` 反查 | 铁律 2/4：枚举不持 Class 引用、反查不进领域层；types 包反向依赖 domain 包 |
| `VerificationGateway` 双轨：`VerificationQuery(channel)` 与 `Class<T>` 变体并存 | 语义重复，两轨各自表达同一过滤 |
| 应用层 `requireLatestUnexpiredPending`：`VerificationChannel.of(type)` + `.map(type::cast)` | 枚举反查 + 显式强转成对出现（铁律 1 精神） |

grill-with-docs 会话（2026-08-05）定案：调用方（domain/app）消费 **class**；枚举是"数据形态"词汇（边界值、判别串、ID 前缀）；type↔class 映射的集中管理发生在**基础设施层**，domain 内不存在注册表。

**Decision**:

1. **实例侧编码不变（铁律 3）**：`getChannel()` / `getAccountType()` 抽象方法由子类覆写，编译器强制每个子类提供判别值。
2. **枚举去 Class 引用（铁律 4 收敛）**：`VerificationChannel` 删除 `type` 字段与 `of(Class<T>)`；保留 `S("sms")` / `E("email")` 纯枚举形态，与 `AuthAccountType` 同形。`domain.types` 包不再依赖 `domain` 包（包环解除）。
3. **class→判别值反查归基础设施**：推导只存在于 gateway 实现内部（建议独立解析类如 `VerificationChannels`，公开以支持一致性测试）。domain/app 不出现任何反查。
4. **Gateway 查询收敛单轨**：`VerificationQuery` 移除 `channel` 字段（保留 scene / status / validUntil 领域过滤条件）；`<T extends Verification<?>> Optional<T> findLatestByUserId(LongId, VerificationQuery, Class<T>)` 为唯一按类型查询。调用方直接得具体类型，应用层零强转。
5. **强转禁令**：domain/app 层禁止显式强转（`(SmsVerification) x`、`type::cast`）。类型收敛靠：① gateway 泛型返回；② 领域方法签名（`changeMobile(SmsVerification)` 编译期契约）；③ 模式匹配 switch（JEP 441）。类型擦除的桥接强转只允许存在于 gateway 实现内部一处（兼作行/类错配的第二道防线——立即 `ClassCastException`，错误实体不可能流入 domain）。
6. **调用方使用规则**：构造、按类型查询、分派、领域方法传参一律用 class；枚举只出现在三处——`getChannel()` 返回类型（实例侧编码）、边界字符串解析（`AuthAccountId.of(String)` 的 switch 路由）、测试锚点（铁律 5）。在 domain/app 代码中见到枚举常量引用（`VerificationChannel.S`、`AuthAccountType.P`）应停下来——要么用模式匹配拿子类，要么是子类行为（多态方法）。
7. **一致性测试扩展（铁律 5）**：现有 permits 完备 / 判别值唯一 / 与枚举 name 一致测试保留；新增对基础设施推导类的一致性断言（class → 枚举 → `@JsonTypeName` 三方一致），把"调用方搞错类型"的失败面锁死。
8. **Verification 类层次保留**：`SmsVerification` / `EmailVerification` 唯一具体差异是 `target` 类型（已参数化为 `T`）与判别值，行为零差异。保留理由：① `User.changeMobile(SmsVerification)` / `changeEmail(EmailVerification)` 的编译期通道契约；② 前瞻 `AuthenticatorVerification`（TOTP，真实行为差异；CONTEXT.md 已注"暂不实现"）；③ 与 `AuthAccount` 对称。**若未来确认不会出现行为差异的子类型，应折叠为单类 + channel 字段（Effective Java Item 23 tagged class 形态），届时重评本决策。**
9. **Jackson 判别声明维持现状**：`@JsonTypeName` + 反射测试兜底。sealed 自动发现（#5025）替代的是 `@JsonSubTypes` 注册表，不替代判别串声明；`@JsonTypeName` 仍是判别串唯一声明处（结论 7）。自定义 `TypeIdResolver`（`idFromValue` 返回 `getChannel().name()`，单一事实源）**推迟**——infrastructure 实现持久化时评估。

**Rationale**:

- 与主流对照：`Class<T>` 参数 + 泛型返回 = JPA `EntityManager.find(Class, id)` / Spring Data `Repository<T, ID>` 的运行时类令牌惯用法；判别值推导在 adapter/基础设施 = Hexagonal 边界翻译职责；"枚举不持 Class 引用" = 依赖单向无环 + 接口多态关联（Effective Java Item 38）；"调用方消费 class" = 结论 1/2（行为差异 → sealed；边界值 → 短名）+ JEP 409/441。
- 强转单点化的理由：泛型擦除使"行 → `Optional<T>`"必然存在一次桥接；把它限定在 gateway 实现内部，domain/app 调用方获得编译器完整类型信息，桥接处同时充当运行时防线。
- 查询对象去 channel 的取舍（调研文档争议点 2）：`channel` 是领域概念，剔除后查询对象自描述性下降；换取调用方无需知道 "channel↔class" 映射、失败模式响亮（误传类型 → 空结果 → 明确异常）。纯数据类"按通道统计"需求走基础设施查询模型，不回灌 gateway。
- 调用方"搞错类型"的失败模式：误传 `EmailVerification.class` 到 changeMobile 流程 → 推导 E → 查无匹配 → `IllegalArgumentException`（响亮失败，属业务流写错而非映射错）；流程签名（`changeMobile(SmsVerification)`）与边界强转双层兜底，不存在静默返回错误实体的路径。

**Consequences**:

| Positive | Negative |
|---|---|
| domain/app 零枚举反查、零显式强转 | `VerificationQuery` 失去 channel 自描述性（用 `Class<T>` 等价表达） |
| types 包不再反向依赖 domain 包 | 调用方误传 Class 是运行时失败（空结果 → 异常），非编译期——由流程签名 + 边界强转兜底 |
| 映射三方锁定（实例侧编码 + infra 推导 + 测试） | 未来 gateway 实现需在内部维护推导逻辑 |
| 新增子类型 checklist 简化：类 + `@JsonTypeName` + 枚举常量 + 测试 | — |

**Considered alternatives**:

| 方案 | 放弃原因 |
|---|---|
| Type Object 注册表留在 domain（现状） | 违反铁律 4（包环）；Type Object 论文明确 "Use inheritance; it's easier"（类型集固定时） |
| 按 channel 拆多个 gateway 实现类 | channel 是过滤值不是策略；N 份样板，`Class<T>` 参数变摆设 |
| `VerificationQuery(channel)` 单轨 + 应用层模式匹配 | 全部调用方都传具体类，channel 冗余；损失编译期类型（`Optional<Verification<?>>` 需运行时收敛） |
| 类型化 default 方法（`findLatestUnexpiredSms(...)` 等） | YAGNI：当前一个查询形状两个子类型；第二个查询形状出现时叠加 |
| 自定义 TypeIdResolver 作为判别串单一事实源 | 推迟：infra 实现持久化时评估；sealed 发现下 `@JsonTypeName` 仍是声明处，测试兜底不变 |

**Related documents**:

- `0004-account-polymorphism-and-persistence.md` — AuthAccount 多态与 `account_type` 鉴别分发（分发实现同属基础设施）
- `0005-enum-short-name.md` — 短名标识设计（枚举形态基准）
- `0011-verification-cross-module-domain.md` — Verification 领域设计（gateway 收敛与层次理由的落地修订）
- `docs/research/type-vs-class-mainstream-review.md` — 主流实践审查（本决策依据）
- `CONTEXT.md` — VerificationChannel 词条
