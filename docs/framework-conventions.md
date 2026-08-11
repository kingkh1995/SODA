# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-domain-types`）的基类、接口和通用类型约定。

## Language

### Domain Primitive（领域原语）
不可变的值对象，承载领域含义，通过类型系统表达业务约束。所有 DP 必须：不可变、自校验（构造时验证）、可序列化、可比较。参见 `Type` 接口。

**字面值语义**：DP 的内部字段以基本数据类型为主（`String`、`BigDecimal`、`Date/LocalDate/LocalDateTime`、`int`、`long`、`boolean` 等可直接对应数据库列类型的值），**允许嵌套持有其他 DP 作为字段**（值对象组合——Evans 蓝皮书/Vernon IDDD 主流模式，如 `VerificationCodePolicy` 持有 `Alphabet`/`PositiveInt`，见 ADR-0018）。前提：组合消除重复校验与基本类型↔DP 转换，不为包装而包装。单字段 DP 映射到单数据库列，多字段 DP 映射到多数据库列，不做序列化编码或 JSON 合并入单列。嵌套 DP 在 JSON 中经各自 `@JsonValue`/creator 表现为基本类型值（见 ADR-0018）。

**例外**：Identifier 类 DP 可使用自描述编码格式（如 `AuthAccountId` 的 `"{短名}:{业务键}"`），此类设计必须在 ADR 中显式记录理由。

### Entity
具有连续身份标识（identity thread）的领域对象。实现 `Identifiable`、`EventSource` 接口，直接持有 `Identifier` DP 作为身份标识。使用 Lombok `@EqualsAndHashCode` 生成基于字段的相等判断（排除 `domainEvents`），子类通过 `@EqualsAndHashCode(callSuper = true)` 继承父类字段。
**双 Builder 模式**：业务模块 Entity/Aggregate 采用双 `@Builder` 模式。`createBuilder()` 暴露业务字段（不含持久化 ID，由服务端 `assignId()` 填补），`builder()`（Lombok 默认命名，挂全参数恢复构造器）暴露全部持久化字段（含 ID）。
**JSON 序列化契约**：序列化走 `Entity` 基类字段可见性（`@JsonAutoDetect(fieldVisibility = ANY)`），属性名即字段名；反序列化唯一入口为**全参数恢复构造器**——`@JsonCreator(mode = PROPERTIES)` + `@JsonProperty` + `@Builder`（restoreBuilder）统一挂在全参数构造器上，确保 JSON 反序列化与手动恢复路径一致。创建路径使用独立的无 id 构造器（`User` 拆双构造器；其余实体 id 非空由 `Entity` 构造器链路保证）。`id` 参数标 `@JsonProperty(required = true)`，缺 id 由 Jackson `required` + `Entity.assignId` 非空校验双重拦截（双构造器实体的恢复构造器委托后经 `assignId` 补 id；单构造器实体由 `Entity` 构造器链路保证），JSON 缺 `id` 视为非法输入（实体 JSON 只表达已持久化状态）。新增字段必须同步补充构造器参数，round-trip 全等断言测试（`assertEquals(restored, original)` + 非法 JSON 拒绝）兜底防字段/参数漂移。
**多态实体分派（type↔class 规则，见 ADR-0016）**：子类层次用 sealed class + `@JsonTypeInfo`/`@JsonTypeName`（Jackson 3 从 `permits` 子句自动发现，无需 `@JsonSubTypes`）。完整规则：
- **何时用类**：种类间有行为或字段差异（含前瞻差异）→ sealed class 层次。调用方逻辑（构造、按类型查询、分派、领域方法传参）一律用 class。
- **何时用枚举**：纯标签/状态/元数据（无行为差异）→ 枚举（数据枚举，如 `VerificationStatus`/`VerificationScene`）；判别值作为边界合同（DB 列、JSON 判别串、ID 前缀）→ 短名判别枚举（`AuthAccountType`/`VerificationChannel`）。枚举是"数据形态"词汇，**不参与行为决策**——在 domain/app 逻辑中按判别枚举判断分支即反模式（用模式匹配拿具体子类）。
- **实例侧编码**：判别值由子类覆写抽象方法提供（`getChannel()` / `getAccountType()`），编译器强制每个子类实现。
- **枚举不持 Class 引用**：禁止 Type Object 注册表式反查（`of(Class)`）——`types` 包不反向依赖 `domain` 包。
- **反查归基础设施**：`class→判别值` 推导只存在于 gateway 实现（infra 解析类，如 `VerificationChannels`）；domain/app 零反查。
- **类型化查询**：gateway 查询用 `Class<T>` 参数 + 泛型返回 `Optional<T>`（JPA `find(Class, id)` 同款），查询对象不含判别枚举；调用方直接得具体类型。
- **强转禁令**：domain/app 禁止显式强转（`(Xxx) x`、`type::cast`）——类型收敛靠泛型返回、方法签名（`changeMobile(SmsVerification)`）、模式匹配；类型擦除的桥接强转只允许在 gateway 实现内部一处。
- **测试锁定**：permits 完备 / 判别值唯一 / 与枚举 name 一致 / 基础设施推导三方一致，反射测试兜底（`@JsonTypeName` 无法编译期绑定枚举 name，JLS §9.7.1）。
**字段规则**：Entity 的全部属性必须是 Domain Primitive（含 Identifier），不允许持有基础数据类型作为 Entity 字段。Entity 通过组装 DP 表达业务含义和约束，而非在字段上直接做参数校验。

### Aggregate
聚合一致性边界内的顶层实体，负责保证聚合内部的所有不变量不被破坏。对聚合的所有操作必须通过聚合根进行。

### Identifiable
可标识的领域对象标记接口（`domain.Identifiable`），提供 `getId()` 和 `isIdentified()` 查询契约。所有 Entity 和 Aggregate 必须实现此接口。

### Type
所有领域原语的根标记接口。扩展 `Serializable`。类型安全的可比较性由子类各自实现 `Comparable<Self>` 保证。

### EnumType
枚举类型的根标记接口（`com.soda.component.domain.EnumType`），继承 `Type`，同时也是 Domain Primitive。提供 `desc()` 返回英文描述。序列化使用枚举 `name()` 短名，各枚举额外提供 `of(String)`（`@JsonCreator` 入口）。

### Identifier
不可变的领域原语，扩展 `Type`，在限界上下文内唯一标识一个实体。底层值类型是泛型的（`Identifier<T extends Comparable<T>>`）。子类自行实现 `Comparable<Self>`。实现类需提供 `identifier()` 返回类型化值，以及基于值的 `equals()`/`hashCode()`。

### LongId
通用长整型标识符 DP（`domain.types.LongId`），实现 `Identifier<Long>`，位于可选模块 `soda-component-domain-types`。提供 `parse(String)`。默认使用服务端生成策略。

### UUId
UUID 格式标识符 DP（`domain.types.UUId`），实现 `Identifier<String>`。校验规则：格式匹配 `8-4-4-4-12` 十六进制，归一化为小写。提供 `random()` 随机生成。默认使用客户端生成策略。

### Version
乐观锁版本号 DP（`domain.types.Version`），实现 `Type`。基于 `int`，带内部缓存（[0, 99]）。提供 `of(int)`、`parse(String)`、`next()`。初始版本 `INITIAL = 0`。

### PositiveInt
正整数 DP（`domain.types.PositiveInt`），实现 `Type` + `Comparable`。值 >= 1。

### RandomString
随机字符串 DP（`domain.types.RandomString`），实现 `Type`。由 `RandomStringGenerator` 生成。字符集由调用方以 `Alphabet` 指定（领域拥有，见 ADR-0018），随机源由基础设施层决定。

### Alphabet
字符集 DP（`domain.types.Alphabet`），实现 `Type`。包装任意字符集字符串（开集，非枚举）——`RandomStringGenerator` 以它为输入决定输出字符池。不变量：非空、字符唯一（集合语义，重复 = 隐式加权）、size ≥ 2（熵下限）。提供 `size()` 与严格索引 `charAt(int)`（0 ≤ index < size，越界 IAE）；随机源由生成器负责（`SecureRandom.nextInt(size)`，见 ADR-0018）。常用字符集常量：`DIGITS`、`UNAMBIGUOUS_ALPHANUMERIC`（去混淆数字+大写字母，剔除 0/1/I/O）。

### Secret
秘密值基类（`domain.Secret`），`Type` 的子类型，用于敏感数据。自动脱敏 toString（`Xxx[***]`），通过私有构造器 + 非标准访问器命名拒绝序列化。

### RawCredential
原始凭证（`domain.types.RawCredential`），继承 `Secret`。通用载体，不绑定算法。用于传递密码、API Key、Token 等长期有效凭证的原始值给 `CredentialHasher`。不可 JSON 序列化。

### CredentialHash
凭证哈希 DP（`domain.types.CredentialHash`），实现 `Type`。算法无关，不校验格式，仅约束非 blank。

### Active
激活状态 DP（`domain.types.Active`），实现 `Type`。通用 boolean 值封装，缓存 `TRUE`/`FALSE` 单例。提供 `negate()` 取反。

### Email（support type）
电子邮箱地址 DP（`domain.types.Email`），实现 `Type`。校验格式并归一化为小写。提供 `localPart()` 和 `domain()`。

### WanYuan
人民币万元 DP（`domain.types.WanYuan`），实现 `Type`。内部以万元单位存储，精度到百元。提供 `fromYuan(BigDecimal)` 从元转换、`toYuan()` 转回元。

### SmsContent
短信内容 DP（`domain.types.SmsContent`），实现 `Type`。最长 70 字符。

### EmailContent
邮件内容 DP（`domain.types.EmailContent`），实现 `Type`。由 `subject`（最长 255 字符）和 `body` 组成。

### Cacheable
应用层缓存关注点。通过 Spring `@Cacheable` 在 ApplicationService 上声明缓存区域和 key，领域层零缓存感知。不允许在 Entity/Aggregate 上添加与缓存相关的接口或基类方法。

### Lockable
应用层锁定关注点。通过自定义 `@Lockable` 注解声明锁资源 key，领域层零锁定感知。不允许在 Entity/Aggregate 上添加与锁相关的接口或基类方法。

### Trackable
基础设施层持久化优化。Repository 实现层基于 snapshot/diff 做部分更新，Aggregate 本身无追踪逻辑。不允许在 Aggregate 上添加变更追踪接口或基类方法。

### KeyUtils
工具方法（`com.soda.component.domain.util`），用于从 Entity 推导缓存/锁资源 key。不在 Entity 基类上实现 `cacheKey()`/`lockKey()`。

### Gateway
标记接口，无方法无泛型。供 IOC 容器扫描和 AOP 切面识别。所有 Gateway 接口的根。

### EntityGateway
实体持久化契约，继承 `Gateway`。泛型 `<T extends Entity<ID>, ID extends Identifier<?>>`。提供 `save(T)`、`findById(ID)`、`findAllById(Iterable<ID>)`。`save` 返回 `ID`（可能新生成）。无删除契约——终态（如注销 R）由 `save` 持久化（ADR-0017）。

### CredentialHasher
凭证哈希器契约（`domain.gateway.CredentialHasher`），继承 `Gateway`。提供 `hash(RawCredential) → CredentialHash` 和 `matches(RawCredential, CredentialHash)`。实现层可对接 BCrypt、Argon2、SCrypt 等。

### RandomStringGenerator
随机字符串生成器契约（`domain.gateway.RandomStringGenerator`），继承 `Gateway`。提供 `generate(PositiveInt, Alphabet) → RandomString`——长度与字符集由调用方指定，实现层只负责随机源与装配。通用能力，不绑定任何业务场景（见 ADR-0018）。

### SmsSender
短信发送器契约（`domain.gateway.SmsSender`），继承 `Gateway`。提供 `send(Mobile, SmsContent)`。

### EmailSender
邮件发送器契约（`domain.gateway.EmailSender`），继承 `Gateway`。提供 `send(Email, EmailContent)`。

### DomainEvent
领域事件基接口，泛型 `<ID extends Identifier<?>>`。提供 `entityId()` 和 `occurredAt()`。业务模块用 `record` 实现，类型参数 `ID` 与 Entity 一致。

**事件载荷约定**（2026-08-07）：事件携带**值数据**（`entityId` + `occurredAt` + 业务事实），构造参数不携带聚合引用——不可变快照、可序列化（outbox）、可测试。`UserCreatedEvent` 是唯一例外：创建时 id 尚未分配，持 `user` 引用延迟求值 `entityId()`（assignId 前返回 null，契约要求持久化后取，见 ADR-0015）。

### DomainEventBus
领域事件总线接口，继承 `Gateway`。提供 `publish(DomainEvent<?>)` 和 `publishAll(Iterable<? extends DomainEvent<?>>)`。

### EventSource
领域事件来源标记接口，泛型 `<ID extends Identifier<?>>`。`Entity` 实现此接口表明自身可作为领域事件来源。通过 `flushEvents()` 取出已注册事件。

### DomainService

领域服务。承接**单个聚合根无法表达**的领域编排——跨聚合的操作与**用例流程的业务逻辑**（多个聚合的动作序列、外部副作用、维护不变量）。

**位置**：`soda-xxx-domain` 的 `domain/service/` 子包（`@ApplicationModule` 内部，领域层自包含）。

**约束**：
- 无状态（final 类 + 无字段或仅常量）；流程逻辑集中于此，符合单一职责（SRP）
- **允许持有 gateway 端口**——仅限副作用/生成类（`SmsSender`、`EmailSender`、`RandomStringGenerator` 等），流程中的发送/生成在此执行
- **禁止持久化**——不持有任何 Repository / Gateway 的写端口；save 一律由 ApplicationService 执行（保存顺序、失败补偿、事务边界属于应用层职责）
- **不建议查询加载**——聚合以参数注入，加载留在 ApplicationService；必要时可在方法内调用查询类 gateway，但不作为默认做法
- 方法签名用领域类型（聚合根 + DP），不用 Command/DTO
- 标注 `@Service`（Spring 容器管理，构造器注入；domain 模块经传递依赖已含 spring-context）
- 实现 `DomainService` 标记接口（`com.soda.component.domain.DomainService`，类似 `Gateway` 的定位：供 IOC 扫描 / AOP 识别）
- 命名：`XxxDomainService`（COLA 风格，如 `CredentialChangeDomainService`），避免与聚合内方法重名

**示例**（换绑验证）：`CredentialChangeDomainService` 只承载跨聚合编排 `changeMobile`/`changeEmail`（verify → user.changeXxx → use）；验证码发起（前置 + 生成码、构造 INITIALIZED 聚合、注册 `VerificationCreatedEvent`）委托 `User.requestChangeMobileCode`/`requestChangeEmailCode`（User 决策 + 创建），物理发送由投递侧监听器在事务提交后执行（见 ADR-0011）。AppService 负责查询前置、加载与 save 顺序（先 user 后 verification）。

### ApplicationService 编排规范

**AppService 只做编排**：加载主体聚合 → 执行用例流程 → 持久化（save）。多聚合交互（同时更改多个聚合）的流程逻辑封装进 `XxxDomainService`；单聚合创建流程（构造 + 发送副作用）可在 AppService 内展开，不在本层展开跨聚合编排。

**参数契约**：AppService 不校验入参（存在性 / 格式 / 合法性），调用方必须保证参数合法；校验只在协议边界（HTTP `@Valid`）与领域层 DP 构造器执行。方法入参默认非空，仅可为 `null` 时标注 `@Nullable`（JSpecify，api 包已 `@NullMarked`）；Command 对象属性遵循同一约定——未标注 `@Nullable` 的属性默认非空。实现中不得对 Command 及其属性做防御性 null 检查。

**可空性处理（Optional 优先）**：可空返回值一律用 `Optional<T>` 表达——Gateway 查询（`findById`/`findByXxx` 返回 `Optional`，不存在返回 `empty()`）、聚合可空属性的 getter（`Optional.ofNullable(field)`，如 `User.getMobile()`）、查找类方法（如 `findActiveAccount`）；调用方用 `orElseThrow` / `map` / `flatMap` / `filter` 消费，不做 `== null` 判空。**保持 `@Nullable` 而非 Optional 的位置**：方法参数、实体字段、JSON 边界（record 组件 / Request / Response / DTO），以及框架层绑定（`Entity.getId()` 被 ORM / 序列化 / `isIdentified()` 依赖；`MapTypeCache` / `ArrayTypeCache` 内部缓存）。构造可空值（`@Nullable String` → `@Nullable VO`）用 `Optional.ofNullable(...).map(...).orElse(null)`，条件更新（如 `changeNickname`）用 `.map(...).ifPresent(...)`。

每个 AppService 有一个**主体聚合**（该用例主要操作的对象，通常是其同名聚合根）。编排规则：

1. **主体聚合的 action 方法可直接在 AppService 中调用**（加载主体 → 调 action → save）
2. **外部 domain（其他聚合）的 action 方法禁止在 AppService 中直接调用**（get/读取除外）
3. 若用例不修改外部 domain：外部聚合作为主体聚合 action 方法的**参数**传入，逻辑封装进主体聚合内部
4. 若用例**修改**外部 domain（调用其 action 即修改其状态）：抽取 `XxxDomainService`，把编排封装进领域服务
5. **流程副作用**（发送验证码/通知等）随流程所在层执行：单聚合创建流程中，发送类副作用封装为聚合行为方法（`SmsVerification.send(SmsSender)` / `EmailVerification.send(EmailSender)`），**投递时机在事务提交后**——创建工厂注册 `VerificationCreatedEvent`，AppService 持久化后发布，投递侧监听器（`@TransactionalEventListener(AFTER_COMMIT)`）调用聚合 `send(sender)` 并落库（DB 事务不跨外部投递通道持有；sender 契约保证投递成功才返回，见 ADR-0011）；跨聚合流程的副作用随流程进入 `XxxDomainService`（sender 类 gateway 允许注入领域服务）
6. **外部聚合的工厂构造**（`createBuilder()...build()`）不属于 action，可在 AppService 内与流程副作用一起展开（单聚合创建用例：生成码 → 构造聚合（INITIALIZED）→ save → 发布 `VerificationCreatedEvent` → 提交后由监听器 `verification.send(sender)`（发送 + 转 PENDING）。发码用例可进一步把「决策 + 创建」收进主体聚合行为方法（`User.requestChangeMobileCode`/`requestChangeEmailCode`：自检规则 + 创建 + 注册事件，见 ADR-0011），AppService 退化为查询前置 + save + 发布）

典型反例：AppService 直接 `verification.verify(code)` 再 `user.changeMobile(...)`——verify 修改外部聚合状态，必须经 `CredentialChangeDomainService`。

### ApplicationService

应用层编排服务。每个聚合根一个 `XxxService`（接口）+ `XxxServiceImpl`（实现）。

**接口 → `soda-xxx-api` 模块**（公开契约），**实现 → `soda-xxx-application` 模块**（`@Service` + 构造器注入）。

Controller 只注入接口，不感知实现：

```java
@RestController
public class UserController {
    private final UserService userService;  // ← 接口在 api 模块
    public UserController(UserService userService) { ... }
}
```

**Adapter 与 Application 的边界**：`adapter` 的 `build.gradle` 声明 `implementation project(':soda-user-application')`（运行时 classpath），但 ModulithTest 强制 adapter 代码只引用 `api` 模块的类，不得 `import` application 模块的任何类。依赖方向为 `adapter → api (编译) + application (运行时)`。

### 模块依赖与 build.gradle 声明规则

**api → domain（待定）**：`api` 依赖 `domain` 的正式方案未定。当前 `soda-xxx-api` 只临时直接声明 `soda-component-domain-types`（契约使用其枚举，如 Sex），**不**声明 `domain` 模块；`soda-component-api-starter` 也**不**依赖 `domain-starter`（api 层框架与 domain 框架解耦）。

**声明规则**：

1. 能经传递依赖到达的一律不声明（`api` 依赖向下游传播 compile + runtime，`implementation` 只传播 runtime）
2. 业务模块（api / application / adapter / infrastructure）：不需要传递给下一级的声明为 `implementation`，需要传递的才用 `api`
3. starter 模块：依赖尽量声明为 `api`（承担为整层提供能力的职责），除非明确不需要传递给依赖方
4. 各层 Spring 能力由对应 starter 提供（如 application 层的 spring-tx 经 application-starter，web 通道的校验能力经 adapter-starter-web）；业务模块不重复声明。例外：consumer 的 `@TransactionalEventListener` 所需 spring-tx 暂由业务模块自声明（consumer 通道为空壳，见 ADR-0019）
5. 临时依赖（如 user-api → domain-types）不视为可传递：其他模块按需自声明，以便将来优化时不受影响

**包结构**：

```
com.soda.xxx.application/         ← @ApplicationModule(CLOSED, deps: {api, domain})
├── service/                       ← *ServiceImpl（必有）
├── command/                       ← *Processor（写操作，需时才加）
├── query/                         ← *Processor（读操作，需时才加）
├── event/                         ← *Handler（领域事件处理，需时才加）
├── factory/                       ← *Factory（复杂创建，需时才加）
└── convertor/                     ← *Convertor（DTO 转换，需时才加）
```

|子包|类后缀|职责|触发条件|
|---|---|---|---|
|`service/`|`ServiceImpl`|ApplicationService 实现，编排 domain gateway + event bus|必有 — 每个聚合根一个|
|`command/`|`Processor`|写操作执行器，处理 `api/command/` 的 Command|ServiceImpl 方法 > 10 或复杂编排时|
|`query/`|`Processor`|读操作执行器，处理 `api/query/` 的 Query|读操作需要跨聚合编排时|
|`event/`|`Handler`|领域事件处理器，响应 `domain/event/` 的 DomainEvent|有领域事件需要订阅时|
|`factory/`|`Factory`|复杂聚合根创建器，Command → Entity|创建涉及 DI 或跨聚合引用时|
|`convertor/`|`Convertor`|双向转换 domain Entity ↔ api DTO|映射逻辑复杂到影响可读性时|

**分拆/合并规则**：一个聚合根一个 Service，方法数不超过 10 个。当方法超过 10 个或出现复杂编排时，从 `service/` 的 ServiceImpl 按 Command 拆出 `command/*Processor`（COLA 风格），但对外接口保持一个。

**Entity 创建**：使用 `XxxEntity.createBuilder()` / `builder()` 双 Builder 模式。当构建逻辑涉及跨聚合引用或需要依赖注入时，引入 `factory/*Factory`，但当前 Builder 模式已足够。

**Command 定义**：Java `record` + `@JsonProperty`，无需继承基类。可空属性标注 JSpecify `@Nullable`，未标注则默认非空（见「参数契约」）。

**未来演进**：当 Service 数量增多或需要统一 AOP 切面时，可在 `soda-component-application-starter` 中引入 `CommandExecutor<CMD, RESULT>` 接口供 application 模块内部使用。

### Exception（异常类约定）

写侧（ApplicationService / DomainService / Entity）异常约定：**构造器校验（DP 式）+ 方法零守卫**（详见 ADR-0015）：

| 类别 | 机制 | 异常 |
|---|---|---|
| 业务参数校验（输入值不合法 / 业务规则拒绝 / 状态机前置，客户端可预期） | `Assert.isTrue` / `Assert.notNull` | IAE（带消息） |
| 构造器参数校验（创建与恢复路径统一） | `ValidateUtils.notNull`（固定标准消息，与 DP 一致） | IAE |
| 方法参数 null 契约违反 | 无守卫 — jspecify `@NullMarked` 契约 + 调用方遵守；未来编译期 checker（NullAway）enforce | NPE（预留） |
| 聚合内部结构不变量 | 类型化（构造器必填字段，如 `User.passwordAccount`）+ JSON schema（全部非空字段 `required = true`，可空字段 `@Nullable`） | 不可表示 / 边界拒绝 |

判定原则（检查对象）：检查「参数值 / 业务状态是否允许操作」→ IAE 校验（客户端可预期的一切：错码、过期、重复用户名、未请求验证码、状态前置）；「值是否为 null」→ 构造器拦截（ValidateUtils），方法不检查（契约）。

规则：
- 消息策略：IAE 带消息（客户端唯一反馈通道，wiring 未实现前不可省）；构造器校验用 `ValidateUtils` 固定消息，不自定义
- 操作语义：set-state（`disable` / `enable`）幂等 no-op、不发事件；transition（`verify` / `use`）业务状态前置失败抛带消息 IAE；`changeMobile` / `changeEmail` 同值换绑抛 IAE（产品决策，见 ADR-0015）
- requireXXX 模式只在 appservice：网关加载后 null 校验 → IAE（User not found / No pending）；可空查找返回 `Optional`
- `Objects.requireNonNullElse` 仅用于默认值模式（如 `User` 构造器 accounts 缺省），不属于守卫
- AppService 的 `Assert` 只用于网关加载结果的存在性 / 状态前置检查，不做 Command 属性级校验（见「参数契约」）；Adapter 层不做 Assert（协议边界由 `@Valid` 负责）；Infrastructure 用 `Optional` 表达可空，正常流程不抛异常
- HTTP 映射（接线 deferred，见 issue 17）：IAE → 400 `INVALID_ARGUMENT`，NPE / NoSuchElementException → 500 `INTERNAL`（预留），`MethodArgumentNotValidException` → 400

### JSpecify（nullness 注解规范）

按 Spring 生态最佳实践（Spring 7 自身即 `@NullMarked`）：
- 包级 `@NullMarked`（package-info.java）声明默认非空；**只标 `@Nullable`，不标 `@NonNull`**（噪音）
- 标注位置：字段类型、参数、返回、类型参数（`List<@Nullable T>`）、record 组件
- 不返回 `Optional<@Nullable T>`——Optional 本身表达可空返回
- 瞬态字段（如 `Entity.id`）标 `@Nullable`，使用点由调用方保证非空（方法路径无运行时窄化守卫）
- **构造器校验 + 方法零守卫**（2026-08-07 三次修订）：null 契约在构造器由 `ValidateUtils.notNull` 拦截（与 DP 一致），方法路径由注解声明 + 调用方遵守；恢复路径 JSON 非空字段 `@JsonProperty(required = true)`；编译期 checker（NullAway + Error Prone）未来引入，路线见 ADR-0015

## Code Style

### Import conventions

禁止通配符导入（`import com.soda.xxx.*`）。所有导入必须显式声明到具体的 class/interface。
IDE 中对应的设置：`Preferences → Editor → Code Style → Java → Imports → "Class count to use import with '*'" → 999`。
