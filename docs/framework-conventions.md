# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-support`）的基类、接口和通用类型约定。

## Language

### Domain Primitive（领域原语）
不可变的值对象，承载领域含义，通过类型系统表达业务约束。所有 DP 必须：不可变、自校验（构造时验证）、可序列化、可比较。参见 `Type` 接口。

### Entity
具有连续身份标识（identity thread）的领域对象。实现 `Identifiable`、`EventSource` 接口，直接持有 `Identifier` DP 作为身份标识。不覆写 `equals`/`hashCode`。

**双 Builder 模式**：业务模块 Entity/Aggregate 采用双 `@Builder` 模式。`createBuilder()` 暴露业务字段（不含持久化 ID，由服务端 `assignId()` 填补），`restoreBuilder()` 暴露全部持久化字段（含 ID）。两种路径共享同一个 `@JsonCreator(mode = Mode.PROPERTIES)` + `@JsonProperty` 构造器，确保 JSON 反序列化与手动恢复路径一致。

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
通用长整型标识符 DP（`support.types.LongId`），实现 `Identifier<Long>`，位于可选模块 `soda-component-support`。提供 `parse(String)`。默认使用服务端生成策略。

### UUId
UUID 格式标识符 DP（`support.types.UUId`），实现 `Identifier<String>`。校验规则：格式匹配 `8-4-4-4-12` 十六进制，归一化为小写。提供 `random()` 随机生成。默认使用客户端生成策略。

### Version
乐观锁版本号 DP（`support.types.Version`），实现 `Type`。基于 `int`，带内部缓存（[0, 99]）。提供 `of(int)`、`parse(String)`、`next()`。初始版本 `PRIMARY = 0`。

### PositiveInt
正整数 DP（`support.types.PositiveInt`），实现 `Type` + `Comparable`。值 >= 1。

### RandomString
随机字符串 DP（`support.types.RandomString`），实现 `Type`。由 `RandomStringGenerator` 生成。字符集和随机源由基础设施层决定，领域层只关心长度。

### Secret
秘密值基类（`domain.Secret`），`Type` 的子类型，用于敏感数据。自动脱敏 toString（`Xxx[***]`），通过私有构造器 + 非标准访问器命名拒绝序列化。

### RawCredential
原始凭证（`support.types.RawCredential`），继承 `Secret`。通用载体，不绑定算法。用于传递密码、API Key、Token 等长期有效凭证的原始值给 `CredentialHasher`。不可 JSON 序列化。

### CredentialHash
凭证哈希 DP（`support.types.CredentialHash`），实现 `Type`。算法无关，不校验格式，仅约束非 blank。

### Active
激活状态 DP（`support.types.Active`），实现 `Type`。通用 boolean 值封装，缓存 `TRUE`/`FALSE` 单例。提供 `negate()` 取反。

### Email（support type）
电子邮箱地址 DP（`support.types.Email`），实现 `Type`。校验格式并归一化为小写。提供 `localPart()` 和 `domain()`。

### WanYuan
人民币万元 DP（`support.types.WanYuan`），实现 `Type`。内部以万元单位存储，精度到百元。提供 `fromYuan(BigDecimal)` 从元转换、`toYuan()` 转回元。

### SmsContent
短信内容 DP（`support.types.SmsContent`），实现 `Type`。最长 70 字符。

### EmailContent
邮件内容 DP（`support.types.EmailContent`），实现 `Type`。由 `subject`（最长 255 字符）和 `body` 组成。

### Cacheable
应用层缓存关注点。通过 Spring `@Cacheable` 在 ApplicationService 上声明缓存区域和 key，领域层零缓存感知。不允许在 Entity/Aggregate 上添加与缓存相关的接口或基类方法。

### Lockable
应用层锁定关注点。通过自定义 `@Lockable` 注解声明锁资源 key，领域层零锁定感知。不允许在 Entity/Aggregate 上添加与锁相关的接口或基类方法。

### Trackable
基础设施层持久化优化。Repository 实现层基于 snapshot/diff 做部分更新，Aggregate 本身无追踪逻辑。不允许在 Aggregate 上添加变更追踪接口或基类方法。

### KeyUtils
工具方法（`com.soda.component.support.util`），用于从 Entity 推导缓存/锁资源 key。不在 Entity 基类上实现 `cacheKey()`/`lockKey()`。

### Gateway
标记接口，无方法无泛型。供 IOC 容器扫描和 AOP 切面识别。所有 Gateway 接口的根。

### EntityGateway
实体持久化契约，继承 `Gateway`。泛型 `<T extends Entity<ID>, ID extends Identifier<?>>`。提供 `save(T)`、`remove(T)`、`findById(ID)`、`findAllById(Iterable<ID>)`。`save` 返回 `ID`（可能新生成），`remove` 接收实体。

### CredentialHasher
凭证哈希器契约（`support.gateway.CredentialHasher`），继承 `Gateway`。提供 `hash(RawCredential) → CredentialHash` 和 `matches(RawCredential, CredentialHash)`。实现层可对接 BCrypt、Argon2、SCrypt 等。

### RandomStringGenerator
随机字符串生成器契约（`support.gateway.RandomStringGenerator`），继承 `Gateway`。提供 `generate(PositiveInt) → RandomString`。

### SmsSender
短信发送器契约（`support.gateway.SmsSender`），继承 `Gateway`。提供 `send(Mobile, SmsContent)`。

### EmailSender
邮件发送器契约（`support.gateway.EmailSender`），继承 `Gateway`。提供 `send(Email, EmailContent)`。

### DomainEvent
领域事件基接口，泛型 `<ID extends Identifier<?>>`。提供 `entityId()` 和 `occurredAt()`。业务模块用 `record` 实现，类型参数 `ID` 与 Entity 一致。

### DomainEventBus
领域事件总线接口，继承 `Gateway`。提供 `fire(DomainEvent<?>)` 和 `fireAll(Iterable<? extends DomainEvent<?>>)`。

### EventSource
领域事件来源标记接口，泛型 `<ID extends Identifier<?>>`。`Entity` 实现此接口表明自身可作为领域事件来源。通过 `flushEvents()` 取出已注册事件。
