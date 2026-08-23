# Framework Conventions

领域框架层（`soda-component-domain` / `soda-component-domain-types`）的基类、接口和通用类型约定。

## Language

### Domain Primitive（领域原语）
不可变的值对象，承载领域含义，通过类型系统表达业务约束。所有 DP 必须：不可变、自校验（构造时验证）、可序列化、可比较。参见 `Type` 接口。

**字面值语义**：DP 的内部字段以基本数据类型为主（`String`、`BigDecimal`、`Date/LocalDate/LocalDateTime`、`int`、`long`、`boolean` 等可直接对应数据库列类型的值），**允许嵌套持有其他 DP 作为字段**（值对象组合——Evans 蓝皮书/Vernon IDDD 主流模式，如 `VerificationCodePolicy` 持有 `Alphabet`/`PositiveInt`，见 ADR-0018）。前提：组合消除重复校验与基本类型↔DP 转换，不为包装而包装。单字段 DP 映射到单数据库列，多字段 DP 映射到多数据库列，不做序列化编码或 JSON 合并入单列。嵌套 DP 在 JSON 中经各自 `@JsonValue`/creator 表现为基本类型值（见 ADR-0018）。

**例外**：Identifier 类 DP 可使用自描述编码格式（如 `AuthAccountId` 的 `"{短名}:{业务键}"`），此类设计必须在 ADR 中显式记录理由。（投递端点 DP `VerificationRecipient` 为多属性 DP，走主规则多列映射——曾误作自描述格式例外，2026-08-16 检视修订作废，见 ADR-0026 §2/§10。）

### Entity
具有连续身份标识（identity thread）的领域对象。实现 `Identifiable`、`EventSource` 接口，直接持有 `Identifier` DP 作为身份标识。使用 Lombok `@EqualsAndHashCode` 生成基于字段的相等判断（排除 `domainEvents`），子类通过 `@EqualsAndHashCode(callSuper = true)` 继承父类字段。
**双 Builder 模式**：业务模块 Entity/Aggregate 采用双 `@Builder` 模式。`createBuilder()` 暴露业务字段（不含持久化 ID，由服务端 `assignId()` 填补），`builder()`（Lombok 默认命名，挂全参数恢复构造器）暴露全部持久化字段（含 ID）。
**JSON 序列化契约**：序列化走 `Entity` 基类字段可见性（`@JsonAutoDetect(fieldVisibility = ANY)`），属性名即字段名；反序列化唯一入口为**全参数恢复构造器**——`@JsonCreator(mode = PROPERTIES)` + `@JsonProperty` + `@Builder`（restoreBuilder）统一挂在全参数构造器上，确保 JSON 反序列化与手动恢复路径一致。创建路径使用独立的无 id 构造器（`User` 拆双构造器；其余实体 id 非空由 `Entity` 构造器链路保证）。`id` 参数标 `@JsonProperty(required = true)`，缺 id 由 Jackson `required` + `Entity.assignId` 非空校验双重拦截（双构造器实体的恢复构造器委托后经 `assignId` 补 id；单构造器实体由 `Entity` 构造器链路保证），JSON 缺 `id` 视为非法输入（实体 JSON 只表达已持久化状态）。新增字段必须同步补充构造器参数，round-trip 全等断言测试（`assertEquals(restored, original)` + 非法 JSON 拒绝）兜底防字段/参数漂移。
**多态实体分派（type↔class 规则，见 ADR-0016）**：子类层次用 sealed class + `@JsonTypeInfo`/`@JsonTypeName`（Jackson 3 从 `permits` 子句自动发现，无需 `@JsonSubTypes`）。完整规则：
- **何时用类**：种类间有行为或字段差异（含前瞻差异）→ sealed class 层次。调用方逻辑（构造、按类型查询、分派、领域方法传参）一律用 class。
- **何时用枚举**：纯标签/状态/元数据（无行为差异）→ 枚举（数据枚举，如 `UserState`/`VerificationState`/`UserVerificationScene`）；判别值作为边界合同（DB 列、JSON 判别串、ID 前缀）→ 短名判别枚举（`AuthAccountType`/`VerificationChannel`）。枚举是"数据形态"词汇，**不参与行为决策**——在 domain/app 逻辑中按判别枚举判断分支即反模式（用模式匹配拿具体子类）。
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

### 字面量家族（StringLiteralType / LongLiteralType / IntLiteralType / BooleanLiteralType / DoubleLiteralType）
单属性字面量 DP 契约（`com.soda.component.domain.*LiteralType`，见 ADR-0028）——包装一个不可变基本类型字面量（String/原语），暴露 `value()` 裸值（原语，免装箱/拆箱）。五家族互不关联（IntSupplier 式，无共享根），各 `extends Type`。`@JsonValue` 声明于家族接口的 `value()` 上，实现类继承即获标量 JSON 序列化与反序列化（Jackson 3.1.4 实证双向）：record 与单 public 构造器 class 零 Jackson 代码；private 构造器 + 工厂（缓存/单例/解析）保留 `@JsonCreator(DELEGATING)`（构造入口不可继承）。`EnumType` 与五家族平行（枚举是封闭常量集，常量自身即值，不包装字面量——ADR-0028 修订注记）。

### EnumType
枚举类型的契约接口（`com.soda.component.domain.EnumType`），继承 `Type`，同时也是 Domain Primitive（ADR-0005）。提供 `desc()` 返回英文描述。序列化：Jackson 原生输出 `name()` 短名（与持久化短名一致），各枚举提供 `of(String)`（`@JsonCreator` 入口）。与字面量家族（`StringLiteralType` 等）平行，不参与家族契约（ADR-0028 修订注记）。

### StateEnumType
状态机枚举标记接口（`com.soda.component.domain.StateEnumType`），继承 `EnumType`。状态机形态 = 单一维度枚举 + 聚合根命令迁移方法（ADR-0017）。提供实例谓词 `terminal()` 终态（吸收态）判定，供领域守卫与基础设施兜底统一消费——终态行不可写守卫按 `terminal()` 泛化，新增终态只需枚举成员标记，守卫零改动（ADR-0023）。实现约定：终态以常量子类覆写 `terminal()` 标记（仅终态成员覆写返回 `true`，声明处自文档）；谓词用实例方法（多态分派是接口价值所在，静态方法无法按枚举分派）；**不提供** `isInitial()`/`isNew()`：初始态语义随状态机而异（未来「待激活」态会使创建态不再是初始态）；`isNew` 与 Spring `Persistable#isNew()`（持久化状态检测）同名异义。

### Identifier
不可变的领域原语，扩展 `Type`，在限界上下文内唯一标识一个实体。底层值类型是泛型的（`Identifier<T extends Comparable<T>>`）。子类自行实现 `Comparable<Self>`。实现类需提供 `identifier()` 返回类型化值，以及基于值的 `equals()`/`hashCode()`。

### LongId
通用长整型标识符 DP（`domain.types.LongId`），实现 `Identifier<Long>`，位于可选模块 `soda-component-domain-types`。提供 `parse(String)`。默认使用服务端生成策略。

### Uuid
UUID 格式标识符 DP（`domain.types.Uuid`），实现 `Identifier<String>`。校验规则：格式匹配 `8-4-4-4-12` 十六进制，归一化为小写。提供 `random()` 随机生成。默认使用客户端生成策略。

### Version
乐观锁版本号 DP（`domain.types.Version`），实现 `Type`。基于 `int`，带内部缓存（[0, 99]）。提供 `of(int)`、`parse(String)`、`next()`。初始版本 `INITIAL = 0`。

### PositiveInt
正整数 DP（`domain.types.PositiveInt`），实现 `Type` + `Comparable`。值 >= 1。

### RandomString
随机字符串 DP（`domain.types.RandomString`），实现 `Type`。由 `RandomStringGenerator` 生成。字符集由调用方以 `Alphabet` 指定（领域拥有，见 ADR-0018），随机源由基础设施层决定。

### Alphabet
字符集 DP（`domain.types.Alphabet`），实现 `Type`。包装任意字符集字符串（开集，非枚举）——`RandomStringGenerator` 以它为输入决定输出字符池。不变量：非空、字符唯一（集合语义，重复 = 隐式加权）、size ≥ 2（熵下限）。提供 `size()` 与严格索引 `charAt(int)`（0 ≤ index < size，越界 IAE）；随机源由生成器负责（`SecureRandom.nextInt(size)`，见 ADR-0018）。常用字符集常量：`DIGITS`、`UNAMBIGUOUS_ALPHANUMERIC`（去混淆数字+大写字母，剔除 0/1/I/O）。

### SecretValue
秘密值 DP（`domain.types.SecretValue`），`Type` 的子类型。通用载体，不绑定算法。用于传递密码、API Key、Token 等长期有效凭证的原始值给 `PasswordHasher`。安全姿态：不实现 `StringLiteralType`（无 `@JsonValue value()`），访问器命名 `rawValue()` 规避序列化框架发现，toString 全遮蔽（`SecretValue[***]`），引用级相等——不可 JSON 序列化，瞬态使用后丢弃。

### PasswordHash
口令哈希 DP（`domain.types.PasswordHash`），`SensitiveValue` 子类（ADR-0033 哈希族唯一敏感特例）。PHC 自描述格式，前缀白名单：argon2id/i/d、bcrypt `$2[abcy]$`、scrypt、pbkdf2(-sha256/-sha512)?。toString 强制遮蔽；`maskedValue()` 格式感知截断，保留至盐段之前。长度上限经 `TypeConfigProvider.passwordHashMaxLength()` 配置（默认 200，至少 128）。

### Ciphertext
可逆加密信封 DP（`domain.types.Ciphertext`），record 实现 `StringLiteralType`。JWE compact 五段式自验证：alg=dir + enc=A256GCM + kid 非空（无需钥匙即可解析的明文头白名单）。类型擦除——解密由调用方提供目标字面量类型。无钥惰性、非攻击素材，无遮蔽义务。

### Digest
等值摘要 DP（`domain.types.Digest`），record 实现 `StringLiteralType`。恰好 32 字节，hex 或标准 base64（RFC 4648 §4 含 padding；base64url 不接受）。

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
**save 全权委托约定（ADR-0024）**：PO 的 `isNew()` = `id == null` 判定（收在 `AbstractPersistable`——所有 PO 强制继承的统一基类），save = `repository.save(toPersistence(domain))`——id==null（服务端生成 id 创建路径）→ persist（INSERT + `assignId` 回填）；id 恒有 → merge 按行存在性统一路由 insert / put（全量更新，领域 null = 清空列）/ 终态处理。要点：① 乐观锁由 merge 自动校验（`@Version` 版本不一致 → 异常 → 事务回滚），网关不做手动比对/行数判断；② convertor `toPersistence` 全量构造（含显式 null），是 merge 拷贝的唯一来源，无覆写原语；③ 终态守卫/归档等聚合级策略在 gateway 内裁决；④ 客户端生成 id 的聚合 save 前置：必须已标识；⑤ 同事务加载享一级缓存 ctx 命中，update 路径零额外 SQL；⑥ insert-only 表（如 `user_archive`）id 恒有 → 恒 merge，insert 多一次 PK SELECT，低频可接受；⑦ 持久化防御编程用 JDK 设施/裸抛（终态拒写裸抛 ISE、未标识 `Objects.requireNonNull` → NPE、行不存在 `orElseThrow()` → NSE），不携消息——异常类型 + 栈帧即语义；不建守卫工具类、不散落自定义消息。

### Convertor（基础设施）

基础设施层领域聚合 ↔ 持久化模型的转换器（`soda-xxx-infrastructure` 的 `infrastructure/convertor/` 子包，COLA 惯例）。

**命名**：`XxxConvertor`（每个聚合一个）；final 类 + 私有构造器（工具类）。

**公共方法**（层命名，全项目统一）：

| 方法 | 方向 | 语义 |
|---|---|---|
| `toDomain(PO)` | 持久化行 → 领域聚合 | 恢复路径（判别组装、null → Optional.empty 等） |
| `toPersistence(domain)` | 领域聚合 → PO | 全量构造（含显式 null 清空列；id 可空 → persist 后回填；id 有值 → merge 按行路由，见 ADR-0024） |

规则：
- **领域对象是行的唯一事实源**：`toPersistence` 逐列赋值（含 null——领域 null = 清空列），创建/更新路径共用同一转换，领域列单点定义不漂移
- **不原地覆写托管实例**（ADR-0024）：save 对 id 有值走 `em.merge`，托管实例是加载快照供版本校验；变异它会使乐观锁冲突检测失效（2026-08-15 实证回退）
- **审计列自动处理**（2026-08-15 修订）：`toPersistence` 不构造审计列（null 即可）——created_date 由基类 `updatable=false` 保护（不进 UPDATE）、last_modified_date 由 auditing `@PreUpdate` 刷新；merge 的 null 不会覆盖审计列（实证：`PersistenceEndToEndTest.should_updatePreserveAuditColumns`）
- **一持久化形状一 convertor**：归档/审计等第二张表用独立 convertor 类（如 `UserArchiveConvertor.toPersistence(User)` → `UserArchivePO`），不塞进聚合 convertor
- **不设共享的「逐列应用」私有方法**：`toPersistence` 内联全量构造，避免半转换 helper（部分构造 + 变异入参的混淆）；私有辅助方法（如从聚合派生列值）命名自由

### 查询契约命名（existsBy / findBy）
- **存在性谓词一律 `existsBy*`**（Spring Data 派生查询词汇表有 `existsBy` 无 `hasBy`；先例 `UserGateway.existsByUsername`）。`has*` 是领域对象自问状态的命名（`isPending`/`isExpiredAt`），不用于网关。
- **语义 = 键存在性 + 唯一性约定**：`existsBy*` 与 DB 唯一索引成对出现（应用层预检 + DB 兜底，如 `uk_username`、`uk_active_key`）；过滤语义（活跃/未过期/状态等）在契约 javadoc 声明、**实现细节归基础设施**——契约名不拼过滤措辞（例：`existsBySceneAndChannelAndTarget` 的活跃/过期判定由基础设施实现，见 ADR-0025）。
- **消费/检索用 `findBy`/`findFirstBy` + 显式参数**；固定形状查询不用查询对象（`VerificationQuery` 已删除，ADR-0025）——查询对象/规格模式只用于开放过滤（管理端任意筛选组合）。

### PasswordHasher
口令哈希器契约（`domain.gateway.PasswordHasher`），继承 `Gateway`（ADR-0033）。`hash(SecretValue) → PasswordHash` 与 `verify(PasswordHash, SecretValue) → boolean`；慢 KDF 与成本参数由实现层配置。参考实现 `PasswordHasherImpl`（BCrypt，spring-security-crypto 独立依赖）。

### Digester
等值摘要器契约（`domain.gateway.Digester`），继承 `Gateway`。`digest(SecretValue)` 高熵令牌无钥快摘要 + `index(SensitiveValue)` 盲索引（归一化 + 字段级派生钥 HMAC），同产 32 字节 `Digest`。

### Encryptor / Decryptor
加密/解密契约（`domain.gateway.Encryptor`/`Decryptor`），各继承 `Gateway`，ISP 分离读写侧。`encrypt(StringLiteralType) → Ciphertext`；`decrypt(Ciphertext, Class<T>) → T` 类型擦除解密与 `decryptGeneric(Ciphertext) → String`。

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
领域事件来源标记接口，泛型 `<ID extends Identifier<?>>`。`Aggregate`（聚合根）实现此接口表明自身可作为领域事件来源（2026-08-16 事件源能力从 `Entity` 迁至 `Aggregate`，普通实体如 `AuthAccount` 不实现）。通过 `flushEvents()` 取出已注册事件。

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

**示例**（换绑验证）：`CredentialChangeDomainService` 只承载跨聚合编排 `changeMobile`/`changeEmail`（verify → user.changeXxx → use）；验证码发起（UCC）由 `UserAuthService.requestChangeMobileCode`/`requestChangeEmailCode` 编排（前置查询拦截 → `UserVerificationFactory.newCredentialChangeVerification` 构造 INITIALIZED 聚合、注册 `VerificationCreatedEvent`，见 ADR-0026），物理发送由投递侧监听器在事务提交后按 recipient 分派（见 ADR-0011/0026）。AppService 负责查询前置、加载与 save 顺序（先 user 后 verification）。

### ApplicationService 编排规范

**AppService 只做编排**：加载主体聚合 → 执行用例流程 → 持久化（save）。多聚合交互（同时更改多个聚合）的流程逻辑封装进 `XxxDomainService`；单聚合创建流程（构造 + 发送副作用）可在 AppService 内展开，不在本层展开跨聚合编排。

**参数契约**：AppService 不校验入参（存在性 / 格式 / 合法性），调用方必须保证参数合法；校验只在协议边界（HTTP `@Valid`）与领域层 DP 构造器执行。方法入参默认非空，仅可为 `null` 时标注 `@Nullable`（JSpecify，api 包已 `@NullMarked`）；Command 对象属性遵循同一约定——未标注 `@Nullable` 的属性默认非空。实现中不得对 Command 及其属性做防御性 null 检查。

**可空性处理（Optional 优先）**：可空返回值一律用 `Optional<T>` 表达——Gateway 查询（`findById`/`findByXxx` 返回 `Optional`，不存在返回 `empty()`）、聚合可空属性的 getter（`Optional.ofNullable(field)`，如 `User.getMobile()`）、查找类方法（如 `findActiveAccount`）；调用方用 `orElseThrow` / `map` / `flatMap` / `filter` 消费，不做 `== null` 判空。**保持 `@Nullable` 而非 Optional 的位置**：方法参数、实体字段、JSON 边界（record 组件 / Request / Response / DTO），以及框架层绑定（`Entity.getId()` 被 ORM / 序列化 / `isIdentified()` 依赖；`MapTypeCache` / `ArrayTypeCache` 内部缓存）。构造可空值（`@Nullable String` → `@Nullable VO`）用 `Optional.ofNullable(...).map(...).orElse(null)`，条件更新（如 `changeNickname`）用 `.map(...).ifPresent(...)`。

每个 AppService 有一个**主体聚合**（该用例主要操作的对象，通常是其同名聚合根）。编排规则：

1. **主体聚合的 action 方法可直接在 AppService 中调用**（加载主体 → 调 action → save）
2. **外部 domain（其他聚合）的 action 方法禁止在 AppService 中直接调用**（get/读取除外）
3. 若用例不修改外部 domain：外部聚合作为主体聚合 action 方法的**参数**传入，逻辑封装进主体聚合内部
4. 若用例**修改**外部 domain（调用其 action 即修改其状态）：抽取 `XxxDomainService`，把编排封装进领域服务
5. **流程副作用**（发送验证码/通知等）随流程所在层执行：发送类副作用在**投递侧**执行（**投递时机在事务提交后**）——创建工厂注册 `VerificationCreatedEvent`，AppService 持久化后发布，投递侧监听器（`@TransactionalEventListener(AFTER_COMMIT)`）按 recipient 类型分派 `SmsSender`/`EmailSender` 并 `markSent` 落库（DB 事务不跨外部投递通道持有；sender 契约保证投递成功才返回，见 ADR-0011/0026）；跨聚合流程的副作用随流程进入 `XxxDomainService`（sender 类 gateway 允许注入领域服务）
6. **外部聚合的工厂构造**（`createBuilder()...build()`）不属于 action，可在 AppService 内与流程副作用一起展开（发码用例：`UserAuthService.requestChangeMobileCode`/`requestChangeEmailCode`——UCC 前置（跨实例查询拦截：启用态 + target ≠ 当前值 + 目标全局唯一 + `existsBySource` 无活跃验证）→ `UserVerificationFactory.newCredentialChangeVerification` 构造 INITIALIZED 验证聚合（scene=UCC 方法内写死）→ save（convertor 设 active_key；惰性 DELETE 过期行收敛在 gateway 实现内）→ 发布 `VerificationCreatedEvent` → 提交后由监听器按 recipient 分派 sender 并 markSent。主体聚合自检规则可收进聚合行为方法，跨实例查询必须留在 AppService；2026-08-16 决策：发码的主体 = User，Verification 为协助方聚合（见 ADR-0026 §7））

**终态守卫（Stateful）**：`Aggregate` 基类实现 `Stateful`（聚合根自动具备状态机契约——`getState()` 暴露状态枚举（非空）、`isTerminal()` 委托 `StateEnumType.terminal()` 判定终态，单一事实源在枚举；聚合的 `getState()` 由 `@Getter` 生成即满足，无需显式方法/泛型；普通实体如 AuthAccount 不实现本契约）；加载后经 `AbstractAppService.requireNotTerminal(id)` 统一守卫——终态（吸收态）实体禁止一切写，抛 IAE（业务拒绝，与领域 R 吸收态 IAE 一致；持久层兜底为网关 save 内行终态判定裸抛的 ISE，防御编程不携消息）。用例级状态前置（如 CC 发码要求启用态）仍为用例业务断言，不进通用守卫。新增状态机聚合：状态枚举实现 `StateEnumType` + 聚合的 `getState()` 返回它（@Getter 即满足），零额外样板（术语见 CONTEXT.md）。

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

**api → domain（待定）**：`api` 依赖 `domain` 的正式方案未定。当前 `soda-xxx-api` 只临时直接声明 `soda-component-domain-types`（契约使用其枚举，如 Sex），**不**声明 `domain` 模块；`soda-component-api-starter` 也**不**依赖 `domain-starter`（api 层框架与 domain 框架解耦）。~~临时例外（2026-08-12）~~：`soda-user-api` 曾临时声明 `soda-user-domain`——`RequestCodeCommand` 携带 `VerificationScene`/`VerificationChannel` 领域枚举（用户决定替代字符串短名）——2026-08-16 以消除需求的方式了结：`RequestCodeCommand` 删除、api 按用例拆分（scene/channel 隐式，方法即场景），api→domain 临时依赖解除（见 ADR-0026 §7）。

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

> 例外（既有实践）：按关注点拆分服务——`UserService`（身份 CRUD）与 `UserAuthService`（凭证变更）同属 User 聚合根的双服务先例。协助方聚合（如 `Verification`）无独立 AppService——发码用例按主体归属用户侧服务（UCC 在 `UserAuthService`，见 ADR-0026 §7）。

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
- 消息策略：IAE 带消息（客户端唯一反馈通道，wiring 未实现前不可省）；构造器校验用 `ValidateUtils` 固定消息，不自定义。例外：防御编程守卫（调用方按契约调用、兜底拦截——网关内裸抛 ISE / `Objects.requireNonNull` NPE / `orElseThrow` NSE / 不支持的操作 `UnsupportedOperationException`）不携消息，非客户端反馈通道，契约违反即调用方 bug，异常类型 + 栈帧即诊断
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

## Logging（入口日志）

- **web 入口**：controller 打印方法名 + request（`log.info("方法名: request={}", request)`），现行格式
- **application 入口**：AppService 打印 command（`log.info("方法名: command={}", command)`）——两层层级不同，各自打印一次，不互相替代，也不出现第三处重复
- **已知债务**：request/command 含敏感字段（密码、验证码等）时 record toString 会明文入日志——脱敏方案为遗留项，暂缓处理

## Database（开发阶段）

数据源为 H2 内存库（`jdbc:h2:mem:…;MODE=MySQL`，见 soda-user-start `application.yml`），**不接真实 MySQL**；schema 由 Flyway 执行 `db/migration/` 下 SQL 创建。

**开发阶段约定**（2026-08-12，见 ADR-0022）：
- **单 V1**：`db/migration/` 只保留 `V1__init_user_tables.sql`——schema 变更**直接修改 create table 语句**，严禁新增 V2/V3… 增量迁移（无存量数据；切真库时冻结 V1 为基线再启用增量迁移）
- **严禁外键**（阿里手册【强制】）：不得使用外键与级联，完整性由应用层聚合边界保证
- **严禁存储过程 / 触发器 / 视图**等 DB 端逻辑：业务逻辑一律在应用层/领域层实现，DDL 仅表达表、列、索引
- 表名单数；`user` 等保留字反引号包裹；枚举列用短名（ADR-0005）；审计字段（`created_date`/`last_modified_date`，2026-08-13 修订替代 `create_time`/`update_time` + DB 默认值方案）：由 Spring Data auditing 维护（`@CreatedDate`/`@LastModifiedDate`，经 `JpaAuditingAutoConfiguration`），**无 DB 默认值**——`CURRENT_TIMESTAMP` 按会话时区生成会漂移，由应用恒填充；时间类型 `Instant` + `TIMESTAMP_UTC`，列存 UTC 字面值。insert-only 归档表（如 `user_archive`）用单一时间戳（`archive_time`，`@CreatedDate`），不复用审计列对

## Code Style

### Import conventions

禁止通配符导入（`import com.soda.xxx.*`）。所有导入必须显式声明到具体的 class/interface。
IDE 中对应的设置：`Preferences → Editor → Code Style → Java → Imports → "Class count to use import with '*'" → 999`。
