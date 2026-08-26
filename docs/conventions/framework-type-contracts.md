---
type: Convention
title: Framework 类型与契约
description: 查框架层类型契约、DP 类型清单表、Gateway 契约或持久化基类（Convertor/AbstractPersistable）时读——soda-component 领域类型契约注记；完整定义归代码 javadoc。
tags: [ convention, framework, types ]
status: stable
---

## 4. 类型与契约

条目格式＝识别行＋契约注记；类型的完整定义即代码 javadoc（单源），本节只给定位与防混淆约定。 **DP 类型清单表是 doc-conventions
§7 第 8 条「类型覆盖」判据的机械载体**：新增基类/接口/标记类型必须登记成行。

### Domain Primitive（领域原语）

不可变的值对象，承载领域含义，通过类型系统表达业务约束。所有 DP 必须：不可变、自校验（构造时验证）、value-based
相等、非空；序列化与可比较按需（完整契约见 dp-conventions §1）。参见 `Type` 接口。

**字面值语义**：DP 的内部字段以基本数据类型为主（`String`、`BigDecimal`、`Date/LocalDate/LocalDateTime`、`int`、`long`、
`boolean` 等可直接对应数据库列类型的值）， **允许嵌套持有其他 DP 作为字段**（值对象组合——Evans 蓝皮书/Vernon IDDD 主流模式，如
`VerificationCodePolicy` 持有 `Alphabet`/`PositiveInt`，见 ADR-0018 字符集升格为领域概念：Alphabet DP）。前提：组合消除重复校验与基本类型↔DP
转换，不为包装而包装。单字段 DP 映射到单数据库列，多字段 DP 映射到多数据库列，不做序列化编码或 JSON 合并入单列。嵌套 DP 在
JSON 中经各自 `@JsonValue`/creator 表现为基本类型值（见 ADR-0018 字符集升格为领域概念：Alphabet DP）。

**例外**：Identifier 类 DP 可使用自描述编码格式（如 `AuthAccountId` 的 `"{短名}:{业务键}"`），此类设计必须在 ADR
中显式记录理由。投递端点 DP `VerificationRecipient` 为多属性 DP，走主规则多列映射——非自描述格式例外（见 ADR-0026
Verification 双概念模型）。

### Entity

具有连续身份标识（identity thread）的领域对象。实现 `Identifiable`、`EventSource` 接口，直接持有 `Identifier` DP 作为身份标识。使用
Lombok `@EqualsAndHashCode` 生成基于字段的相等判断（排除 `domainEvents`），子类通过 `@EqualsAndHashCode(callSuper = true)`
继承父类字段。 **双 Builder 模式**：业务模块 Entity/Aggregate 采用双 `@Builder` 模式。`createBuilder()` 暴露业务字段（不含持久化
ID，由服务端 `assignId()` 填补），`builder()`（Lombok 默认命名，挂全参数恢复构造器）暴露全部持久化字段（含 ID）。 **JSON
序列化契约**：序列化走 `Entity` 基类字段可见性（`@JsonAutoDetect(fieldVisibility = ANY)`），属性名即字段名；反序列化唯一入口为
**全参数恢复构造器**——`@JsonCreator(mode = PROPERTIES)` + `@JsonProperty` + `@Builder`（restoreBuilder）统一挂在全参数构造器上，确保
JSON 反序列化与手动恢复路径一致。创建路径使用独立的无 id 构造器（`User` 拆双构造器；其余实体 id 非空由 `Entity` 构造器链路保证）。
`id` 参数标 `@JsonProperty(required = true)`，缺 id 由 Jackson `required` + `Entity.assignId` 非空校验双重拦截（双构造器实体的恢复构造器委托后经
`assignId` 补 id；单构造器实体由 `Entity` 构造器链路保证），JSON 缺 `id` 视为非法输入（实体 JSON
只表达已持久化状态）。新增字段必须同步补充构造器参数，round-trip 全等断言测试（`assertEquals(restored, original)` + 非法
JSON 拒绝）兜底防字段/参数漂移。 **多态实体分派（type↔class 规则，见 ADR-0016 判别值映射归属（type↔class））**：子类层次用
sealed class + `@JsonTypeInfo`/`@JsonTypeName`（Jackson 3 从 `permits` 子句自动发现，无需 `@JsonSubTypes`）。完整规则：

- **何时用类**：种类间有行为或字段差异（含前瞻差异）→ sealed class 层次。调用方逻辑（构造、按类型查询、分派、领域方法传参）一律用
  class。
- **何时用枚举**：纯标签/状态/元数据（无行为差异）→ 枚举（数据枚举，如 `UserState`/`VerificationState`/
  `UserVerificationScene`）；判别值作为边界合同（DB 列、JSON 判别串、ID 前缀）→ 短名判别枚举（`AuthAccountType`/
  `VerificationChannel`）。枚举 **只承载数据形态**（边界值/判别串/ID 前缀）；行为决策 **一律用模式匹配拿具体子类**。
- **实例侧编码**：判别值由子类覆写抽象方法提供（`getChannel()` / `getAccountType()`），编译器强制每个子类实现。
- **枚举不持 Class 引用**：禁止 Type Object 注册表式反查（`of(Class)`）——`types` 包不反向依赖 `domain` 包。
- **反查归基础设施**：`class→判别值` 推导只存在于 gateway 实现（infra 解析类，如 `VerificationChannels`）；domain/app 零反查。
- **类型化查询**：gateway 查询用 `Class<T>` 参数 + 泛型返回 `Optional<T>`（JPA `find(Class, id)`
  同款），查询对象不含判别枚举；调用方直接得具体类型。
- **强转禁令**：domain/app 禁止显式强转（`(Xxx) x`、`type::cast`）——类型收敛靠泛型返回、方法签名（
  `User.changeMobile(Verification)` / `credentialChangeService.change(User, Verification, RandomString)`
  ）、模式匹配；类型擦除的桥接强转只允许在 gateway 实现内部一处（`Verification` 单类塌缩见 ADR-0026 双概念模型）。
- **测试锁定**：permits 完备 / 判别值唯一 / 与枚举 name 一致 / 基础设施推导三方一致，反射测试兜底（`@JsonTypeName`
  无法编译期绑定枚举 name，JLS §9.7.1）。 **字段规则**：Entity 的全部属性必须是 Domain Primitive（含
  Identifier），不允许持有基础数据类型作为 Entity 字段。Entity 通过组装 DP 表达业务含义和约束，而非在字段上直接做参数校验。

### Aggregate

聚合一致性边界内的顶层实体，负责保证聚合内部的所有不变量不被破坏。对聚合的所有操作必须通过聚合根进行。

### Identifiable

可标识的领域对象标记接口（`domain.Identifiable`），提供 `getId()` 和 `isIdentified()` 查询契约。所有 Entity 和 Aggregate
必须实现此接口。

### Type

所有领域原语的根标记接口， **不继承 `Serializable`**（JDK 序列化按需显式实现）；可比较性由子类在有自然顺序时实现
`Comparable<Self>`（契约见 dp-conventions §1）。

### 字面量家族（StringLiteralType / LongLiteralType / IntLiteralType / BooleanLiteralType / DoubleLiteralType）

单属性字面量 DP 契约（`com.soda.component.domain.*LiteralType`，见 ADR-0028 字面量类型家族）——包装一个不可变基本类型字面量（String/原语），暴露
`value()` 裸值（原语，免装箱/拆箱）。五家族互不关联（IntSupplier 式，无共享根），各 `extends Type`；`EnumType`
与五家族平行（枚举是封闭常量集，常量自身即值，不包装字面量——ADR-0028 字面量类型家族）。JSON 序列化机制单源见 dp-conventions
§5（模式总表）。

### EnumType

枚举类型的契约接口（`com.soda.component.domain.EnumType`），继承 `Type`，同时也是 Domain Primitive（ADR-0005）。提供 `desc()`
返回英文描述。序列化：Jackson 原生输出 `name()` 短名（与持久化短名一致），各枚举提供 `of(String)`（`@JsonCreator` 入口）。与字面量家族（
`StringLiteralType` 等）平行，不参与家族契约（ADR-0028）。

### StateEnumType

状态机枚举标记接口（`com.soda.component.domain.StateEnumType`），继承 `EnumType`。状态机形态 = 单一维度枚举 +
聚合根命令迁移方法（ADR-0017）。提供实例谓词 `terminal()` 终态（吸收态）判定，供领域守卫与基础设施兜底统一消费——终态行不可写守卫按
`terminal()` 泛化，新增终态只需枚举成员标记，守卫零改动（ADR-0023）。实现约定：状态属性（如 `terminal`）用 **构造器注入字段**
（record 风格，与 `desc` 字段一致），常量声明处自文档；谓词用实例方法（多态分派是接口价值所在，静态方法无法按枚举分派）；
**不提供** `isInitial()`/`isNew()`：初始态语义随状态机而异、无通用谓词；`isNew` 与 Spring `Persistable#isNew()`
（持久化状态检测）同名异义。

### Identifier

不可变的领域原语，扩展 `Type`，在限界上下文内唯一标识一个实体。底层值类型是泛型的（`Identifier<T extends Comparable<T>>`
）。子类自行实现 `Comparable<Self>`。实现类需提供 `identifier()` 返回类型化值，以及基于值的 `equals()`/`hashCode()`。

### DP 类型清单表

每行：类型 + 一句话契约。完整定义在代码 javadoc，决策理由在 ADR 描述（按主题索引见 `docs/adr/_index.md`）。

| 类型                                                                                             | 一句话契约                                                                                   |
|--------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `StringLiteralType` 等五字面量家族                                                               | 单属性字面量 DP 契约：包装不可变基本类型，`value()` 裸值＋标量 JSON 双向                     |
| `DecimalLiteralType`                                                                             | 小数字面量基类（规范值 String、BigDecimal 为派生缓存）                                       |
| `LongId` / `Uuid`                                                                                | 长整型 / UUID 标识符 DP（Uuid 归一化小写、客户端生成）                                       |
| `Version`                                                                                        | 乐观锁版本号 DP（int、缓存 [0,99]、`next()` 步进）                                           |
| `PositiveInt`                                                                                    | 正整数 DP（≥ 1）                                                                             |
| `RandomString` / `Alphabet`                                                                      | 随机字符串 / 字符集 DP；字符池常量收拢于 Alphabet，随机源归生成器                            |
| `SensitiveValue`（位于 `domain.types` 子包）                                                     | 敏感数据 DP 基类：toString 恒脱敏（Mobile/Email 等继承）；位于 types 模块、与具体 DP 同包    |
| `SecretValue`                                                                                    | 瞬态凭证载体：永不序列化、引用级相等、toString 全遮蔽                                        |
| `PasswordHash`                                                                                   | PHC 口令哈希（哈希族唯一 SensitiveValue 特例，遮蔽至盐段前）                                 |
| `Ciphertext`                                                                                     | JWE 可逆加密信封（alg=dir＋A256GCM＋kid 自验证、类型擦除解密）                               |
| `Digest`                                                                                         | 32 字节等值摘要：敏感原值的单向替代品（hex／标准 base64）                                    |
| `Active`                                                                                         | boolean 值封装（TRUE/FALSE 单例、`negate()`）                                                |
| `Email`                                                                                          | 邮箱地址 DP（格式校验＋小写归一化）                                                          |
| `Percentage` / `Fen` / `WanYuan`                                                                 | 百分比 / 分（负值合法退款冲正）/ 万元金额 DP（DecimalLiteralType 系，超 Fen 值域用 WanYuan） |
| `EpochMilli`                                                                                     | epoch 毫秒绝对时间点 DP（毫秒精度互逆 `instant()`；契约就位、暂无生产消费方）                |
| `SoftwareVersion`                                                                                | 三段式版本号 DP（小写 v 前缀、逐段比较与步进、999 封顶不进位）                               |
| `SmsContent` / `EmailContent`                                                                    | 短信内容（≤70 字符）/ 邮件内容（subject ≤255＋body）                                         |
| `Masked*` 五族（MaskedMobile / MaskedBankCard / MaskedIdCard / MaskedChineseName / MaskedEmail） | 敏感值的展示伴生 DP：PATTERN 星号掩码输出（星号段有上界）、原值不可逆推                      |
| `Mobile` / `BankCard` / `IdCard` / `ChineseName`                                                 | 敏感字面量具体族：格式校验＋归一化＋toString 脱敏（SensitiveValue 系）；等值查询走盲索引     |
| `Sex`                                                                                            | 性别枚举 DP（EnumType 系：`M`/`F`，Jackson `name()` 短名）                                   |

**命名/使用约定**：不把 `Alphabet` 称 Policy（与验证码策略词冲突）或 charset（与 `java.nio.charset.Charset` 混淆）；`Fen`/
`WanYuan` 单位明示、不裸称「元」；不把 `EpochMilli` 叫 Timestamp（JDBC 类型歧义），领域时间字段不用裸 `Instant`（线上格式不受 DP
边界保护）；`SoftwareVersion` 不是 Version（乐观锁计数器）也不是 SemVer（无 pre-release/build）；审计列属基础设施、保持裸
`Instant`。`ArrayTypeCache` / `MapTypeCache` 是类型缓存设施、非 Domain Primitive，不入本表。

### Cacheable

应用层缓存关注点。通过 Spring `@Cacheable` 在 ApplicationService 上声明缓存区域和 key，领域层零缓存感知。不允许在
Entity/Aggregate 上添加与缓存相关的接口或基类方法。

### Lockable

应用层锁定关注点。通过自定义 `@Lockable` 注解声明锁资源 key，领域层零锁定感知。不允许在 Entity/Aggregate 上添加与锁相关的接口或基类方法。

### Trackable

基础设施层持久化优化。Repository 实现层基于 snapshot/diff 做部分更新，Aggregate 本身无追踪逻辑。不允许在 Aggregate
上添加变更追踪接口或基类方法。

### KeyUtils

工具方法（`com.soda.component.domain.util`），用于从 Entity 推导缓存/锁资源 key。不在 Entity 基类上实现 `cacheKey()`/
`lockKey()`。

### Gateway

标记接口，无方法无泛型。供 IOC 容器扫描和 AOP 切面识别。所有 Gateway 接口的根。 **接口 Javadoc 契约原则（ADR-0038）**
：只承载调用方契约（方法语义、前后置、异常语义、领域 ADR 交叉引用），禁止引用基础设施实现类、基础设施 ADR，或以 `@see`
指向基础设施包——接口是稳定契约面，与基础设施实现解耦。Javadoc 写法与不得写什么的契约见 [STYLEGUIDE](../../STYLEGUIDE.md)
§4.5（意图与契约）。

### EntityGateway

实体持久化契约，继承 `Gateway`。泛型 `<T extends Entity<ID>, ID extends Identifier<?>>`。提供 `save(T)`、`findById(ID)`、
`findAllById(Iterable<ID>)`。`save` 返回 `ID`（可能新生成）。无删除契约——终态（如注销 R）由 `save` 持久化（ADR-0017）。 **save
全权委托约定（ADR-0024）**：PO 的 `isNew()` = `id == null` 判定（收在 `AbstractPersistable`——所有 PO
强制继承的统一基类），save = `repository.save(toPersistence(domain))`——id==null（服务端生成 id 创建路径）→ persist（INSERT +
`assignId` 回填）；id 恒有 → merge 按行存在性统一路由 insert / put（全量更新，领域 null = 清空列）/ 终态处理。要点：① 乐观锁由
merge 自动校验（`@Version` 版本不一致 → 异常 → 事务回滚），网关不做手动比对/行数判断；② convertor `toPersistence` 全量构造（含显式
null），是 merge 拷贝的唯一来源，无覆写原语；③ 终态守卫/归档等聚合级策略在 gateway 内裁决；④ 客户端生成 id 的聚合 save
前置：必须已标识；⑤ 同事务加载享一级缓存 ctx 命中，update 路径零额外 SQL；⑥ insert-only 表（如 `user_archive`）id 恒有 → 恒
merge，insert 多一次 PK SELECT，低频可接受；⑦ 持久化防御编程用 JDK 设施/裸抛（终态拒写裸抛 ISE、未标识
`Objects.requireNonNull` → NPE、行不存在 `orElseThrow()` → NSE），不携消息——异常类型 + 栈帧即语义；不建守卫工具类、不散落自定义消息。

### API・Adapter 类型指针

api / adapter 层类型的契约语义归专项文档与 javadoc，此处只作寻址：

| 类型                   | 定位                                                                                                  | 去哪                                                             |
|------------------------|-------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `Result` / `ErrorInfo` | REST 统一信封 `{code, msg, data, error}` 与错误结构（reason/domain/metadata）                         | javadoc（`com.soda.component.web`）、ADR-0013                    |
| `Command`              | 写操作命令 marker（api 层，不带校验注解——校验在 Request `@Valid` 与 DP 构造器）                       | [adapter.md](adapter.md) §1、ADR-0009                            |
| `Request` / `Response` | HTTP 协议体专用类型（adapter 层，Response 不含信封）                                                  | [adapter.md](adapter.md) §1                                      |
| `WebAssembler`         | MapStruct 协议转换器（Request→Command 入站、DTO→Response 出站，`to{Action}Command` 命名、同参不同名） | [adapter.md](adapter.md)、[STYLEGUIDE](../../STYLEGUIDE.md) §3.4 |

### AbstractPersistable / AbstractAuditable（基础设施持久化基类）

识别行：`com.soda.component.infrastructure.persistence`——`AbstractPersistable` 是所有 PO 的强制统一基类；
`AbstractAuditable` 在其上叠加审计列。

设计规则：`isNew()` = `id == null`（save 路由判据，见 ADR-0024 Gateway save 统一路由；有 `@Version` 的实体由 version 值决定、优先于
id 判定）；不声明
`@Id` 字段（PO 主键策略异构，各 PO 自行声明）；审计列 `created_date` / `last_modified_date`（Instant，UTC 字面值）由 Spring
Data auditing 填充，`created_date` 不可更新（`updatable=false`）；`toPersistence` 不构造审计列（null 即可，merge
不覆盖，见「Convertor（基础设施）」）。

反模式：PO 不继承统一基类自造 `isNew()`；convertor 手工写审计列；基类实现 `equals`/`hashCode`/`toString`（PO
不参与业务相等性，保持最小面）。

测试要求：审计列行为经 `PersistenceEndToEndTest` 实证（`should_updatePreserveAuditColumns`）；PO 基类契约随 repository
`@DataJpaTest` 覆盖。

关联 ADR：ADR-0024（save 统一路由）、ADR-0022（数据库开发约定）。

### Convertor（基础设施）

基础设施层领域聚合 ↔ 持久化模型的转换器（`soda-xxx-infrastructure` 的 `infrastructure/convertor/` 子包，COLA 惯例）。

**命名**：`XxxConvertor`（每个聚合一个）；final 类 + 私有构造器（工具类）。

**公共方法**（层命名，全项目统一）：

| 方法                    | 方向                | 语义                                                                                                                  |
|-------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------|
| `toDomain(PO)`          | 持久化行 → 领域聚合 | 恢复路径（判别组装、null → Optional.empty 等）                                                                        |
| `toPersistence(domain)` | 领域聚合 → PO       | 全量构造（含显式 null 清空列；id 可空 → persist 后回填；id 有值 → merge 按行路由，见 ADR-0024 Gateway save 统一路由） |

规则：

- **领域对象是行的唯一事实源**：`toPersistence` 逐列赋值（含 null——领域 null = 清空列），创建/更新路径共用同一转换，领域列单点定义不漂移
- **不原地覆写托管实例**（ADR-0024）：save 对 id 有值走 `em.merge`，托管实例是加载快照供版本校验；变异它会使乐观锁冲突检测失效
- **审计列自动处理**：`toPersistence` 不构造审计列（null 即可）——created_date 由基类 `updatable=false` 保护（不进
  UPDATE）、last_modified_date 由 auditing `@PreUpdate` 刷新；merge 的 null 不会覆盖审计列（实证：
  `PersistenceEndToEndTest.should_updatePreserveAuditColumns`）
- **一持久化形状一 convertor**：归档/审计等第二张表用独立 convertor 类（如 `UserArchiveConvertor.toPersistence(User)` →
  `UserArchivePO`），不塞进聚合 convertor
- **不设共享的「逐列应用」私有方法**：`toPersistence` 内联全量构造，避免半转换 helper（部分构造 +
  变异入参的混淆）；私有辅助方法（如从聚合派生列值）命名自由

### 查询契约命名（existsBy / findBy）

- **存在性谓词一律 `existsBy*`**（Spring Data 派生查询词汇表有 `existsBy` 无 `hasBy`；先例
  `UserGateway.existsByUsername`）。`has*` 是领域对象自问状态的命名（`isPending`/`isExpiredAt`），不用于网关。
- **语义 = 键存在性 + 唯一性约定**：`existsBy*` 与 DB 唯一索引成对出现（应用层预检 + DB 兜底，如 `uk_username`、
  `uk_active_key`）；过滤语义（活跃/未过期/状态等）在契约 javadoc 声明、 **实现细节归基础设施**——契约名不拼过滤措辞（例：
  `existsBySceneAndChannelAndTarget` 的活跃/过期判定由基础设施实现，见 ADR-0025 活跃验证唯一性）。

### Gateway 契约一览

| Gateway                     | 契约                                                                                                                          |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `PasswordHasher`            | `hash(SecretValue) → PasswordHash`、`verify`、`needsRehash`（登录透明升级判定）；慢 KDF 与成本参数归实现层（参考实现 BCrypt） |
| `RandomStringGenerator`     | `generate(PositiveInt, Alphabet) → RandomString`；字符集由调用方以 Alphabet 指定、随机源归实现层                              |
| `Digester`                  | `digest(SecretValue)` 高熵令牌无钥快摘要＋`index(SensitiveValue)` 盲索引，同产 32 字节 `Digest`                               |
| `Encryptor` / `Decryptor`   | 加解密 ISP 分离读写侧：`encrypt(StringLiteralType) → Ciphertext`、类型擦除 `decrypt(Ciphertext, Class<T>)`                    |
| `SmsSender` / `EmailSender` | `send(Mobile, SmsContent)` / `send(Email, EmailContent)`；sender 契约保证投递成功才返回                                       |

**盲索引模式**（`Digester.index`）：低熵敏感字面量的等值查询指纹——归一化后以独立主钥做 keyed 摘要（确定性、不可还原），与密文列组成两列模式（ct
还原＋bidx 等值查询）。使用约定：不用明文列建唯一索引、不把盲索引当还原依据、不对低熵值做无钥哈希（可被秒爆）（ADR-0030/0033）。

### DomainEvent

领域事件基接口，泛型 `<ID extends Identifier<?>>`。提供 `entityId()` 和 `occurredAt()`。业务模块用 `record` 实现，类型参数
`ID` 与 Entity 一致。

**事件载荷约定**：事件携带 **值数据**（`entityId` + `occurredAt` + 业务事实），构造参数不携带聚合引用——不可变快照、可序列化（outbox）、可测试。
`UserCreatedEvent` 是唯一例外：创建时 id 尚未分配，持 `user` 引用延迟求值 `entityId()`（assignId 前返回 null，契约要求持久化后取，见
ADR-0015）。

### DomainEventBus

领域事件总线接口，继承 `Gateway`。提供 `publish(DomainEvent<?>)` 和 `publishAll(Iterable<? extends DomainEvent<?>>)`。

### EventSource

领域事件来源标记接口，泛型 `<ID extends Identifier<?>>`。`Aggregate`（聚合根）实现此接口表明自身可作为领域事件来源——普通实体如
`AuthAccount` 不实现。通过 `flushEvents()` 取出已注册事件。
