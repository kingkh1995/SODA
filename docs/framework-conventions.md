# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-domain-types`）的基类、接口和通用类型约定。

## Language

### Domain Primitive（领域原语）
不可变的值对象，承载领域含义，通过类型系统表达业务约束。所有 DP 必须：不可变、自校验（构造时验证）、可序列化、可比较。参见 `Type` 接口。

**字面值语义**：DP 的内部字段应为基础数据类型（`String`、`BigDecimal`、`Date/LocalDate/LocalDateTime`、`int`、`long`、`boolean` 等可直接对应数据库列类型的值），不允许嵌套持有另一个 DP 作为字段。单字段 DP 映射到单数据库列，多字段 DP 映射到多数据库列，不做序列化编码或 JSON 合并入单列。

**例外**：Identifier 类 DP 可使用自描述编码格式（如 `AuthAccountId` 的 `"{短名}:{业务键}"`），此类设计必须在 ADR 中显式记录理由。

### Entity
具有连续身份标识（identity thread）的领域对象。实现 `Identifiable`、`EventSource` 接口，直接持有 `Identifier` DP 作为身份标识。使用 Lombok `@EqualsAndHashCode` 生成基于字段的相等判断（排除 `domainEvents`），子类通过 `@EqualsAndHashCode(callSuper = true)` 继承父类字段。
**双 Builder 模式**：业务模块 Entity/Aggregate 采用双 `@Builder` 模式。`createBuilder()` 暴露业务字段（不含持久化 ID，由服务端 `assignId()` 填补），`restoreBuilder()` 暴露全部持久化字段（含 ID）。两种路径共享同一个 `@JsonCreator(mode = Mode.PROPERTIES)` + `@JsonProperty` 构造器，确保 JSON 反序列化与手动恢复路径一致。
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
乐观锁版本号 DP（`domain.types.Version`），实现 `Type`。基于 `int`，带内部缓存（[0, 99]）。提供 `of(int)`、`parse(String)`、`next()`。初始版本 `PRIMARY = 0`。

### PositiveInt
正整数 DP（`domain.types.PositiveInt`），实现 `Type` + `Comparable`。值 >= 1。

### RandomString
随机字符串 DP（`domain.types.RandomString`），实现 `Type`。由 `RandomStringGenerator` 生成。字符集和随机源由基础设施层决定，领域层只关心长度。

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
实体持久化契约，继承 `Gateway`。泛型 `<T extends Entity<ID>, ID extends Identifier<?>>`。提供 `save(T)`、`remove(T)`、`findById(ID)`、`findAllById(Iterable<ID>)`。`save` 返回 `ID`（可能新生成），`remove` 接收实体。

### CredentialHasher
凭证哈希器契约（`domain.gateway.CredentialHasher`），继承 `Gateway`。提供 `hash(RawCredential) → CredentialHash` 和 `matches(RawCredential, CredentialHash)`。实现层可对接 BCrypt、Argon2、SCrypt 等。

### RandomStringGenerator
随机字符串生成器契约（`domain.gateway.RandomStringGenerator`），继承 `Gateway`。提供 `generate(PositiveInt) → RandomString`。

### SmsSender
短信发送器契约（`domain.gateway.SmsSender`），继承 `Gateway`。提供 `send(Mobile, SmsContent)`。

### EmailSender
邮件发送器契约（`domain.gateway.EmailSender`），继承 `Gateway`。提供 `send(Email, EmailContent)`。

### DomainEvent
领域事件基接口，泛型 `<ID extends Identifier<?>>`。提供 `entityId()` 和 `occurredAt()`。业务模块用 `record` 实现，类型参数 `ID` 与 Entity 一致。

### DomainEventBus
领域事件总线接口，继承 `Gateway`。提供 `fire(DomainEvent<?>)` 和 `fireAll(Iterable<? extends DomainEvent<?>>)`。

### EventSource
领域事件来源标记接口，泛型 `<ID extends Identifier<?>>`。`Entity` 实现此接口表明自身可作为领域事件来源。通过 `flushEvents()` 取出已注册事件。

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

**Entity 创建**：使用 `XxxEntity.createBuilder()` / `restoreBuilder()` 双 Builder 模式。当构建逻辑涉及跨聚合引用或需要依赖注入时，引入 `factory/*Factory`，但当前 Builder 模式已足够。

**Command 定义**：Java `record` + `@JsonProperty`，无需继承基类。

**未来演进**：当 Service 数量增多或需要统一 AOP 切面时，可在 `soda-component-application-starter` 中引入 `CommandExecutor<CMD, RESULT>` 接口供 application 模块内部使用。

## Code Style

### Import conventions

禁止通配符导入（`import com.soda.xxx.*`）。所有导入必须显式声明到具体的 class/interface。
IDE 中对应的设置：`Preferences → Editor → Code Style → Java → Imports → "Class count to use import with '*'" → 999`。
