---
type: Research
title: 写侧服务端生成字段（id / version / 审计时间）如何进入资源响应
description: 锚点：设计或评审「AIP 要求 Create/Update 响应返回资源本身，而 id/version/审计列由持久层在 saveAndFlush 后才生成」时读——按 AIP/RFC、DDD 一手文献（Evans/Vernon/Fowler）、主流实现（COLA/Axon/Spring Data/JPA/Hibernate）三层取证，逐条裁定四种候选方案并给出推荐形态。
tags: [ aip, ddd, jpa, persistence, gateway ]
status: stable
generated:
  by: research/01
  at: 2026-09-11T00:00:00Z
sources:
  - resource: https://google.aip.dev/133
    id: aip-133
    author: Google
    title: AIP-133 Standard methods - Create
  - resource: https://google.aip.dev/134
    id: aip-134
    author: Google
    title: AIP-134 Standard methods - Update
  - resource: https://google.aip.dev/135
    id: aip-135
    author: Google
    title: AIP-135 Standard methods - Delete
  - resource: https://google.aip.dev/148
    id: aip-148
    author: Google
    title: AIP-148 Standard fields
  - resource: https://google.aip.dev/154
    id: aip-154
    author: Google
    title: AIP-154 Resource freshness validation
  - resource: https://google.aip.dev/203
    id: aip-203
    author: Google
    title: AIP-203 Field behavior documentation
  - resource: https://www.rfc-editor.org/rfc/rfc9110.html
    id: rfc-9110
    author: IETF
    title: RFC 9110 HTTP Semantics
  - resource: https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf
    id: evans-ddd-reference
    author: Eric Evans
    title: Domain-Driven Design Reference (2015)
  - resource: https://www.informit.com/articles/article.aspx?p=2020371&seqNum=8
    id: iddd-ch10
    author: Vaughn Vernon
    title: Implementing Domain-Driven Design - Aggregates（出版社节选，含 Entities/Repositories 实现）
  - resource: https://github.com/VaughnVernon/IDDD_Samples
    id: iddd-samples
    author: Vaughn Vernon
    title: IDDD_Samples（官方样例代码）
  - resource: https://martinfowler.com/eaaCatalog/repository.html
    id: poeaa-repository
    author: Martin Fowler
    title: PoEAA - Repository
  - resource: https://github.com/alibaba/COLA
    id: alibaba-cola
    author: Alibaba
    title: COLA（Clean Object-oriented and Layered Architecture）
  - resource: https://github.com/kingkh1995/kk-ddd
    id: kk-ddd
    author: KaiKoo
    title: kk-ddd（DDD 分层参考项目，本地检出 7d8e1b8e）
  - resource: https://github.com/YunaiV/ruoyi-vue-pro
    id: yudao-cloud
    author: YunaiV
    title: yudao-cloud（本仓根目录只读参照副本）
  - resource: https://docs.axoniq.io/axon-framework-reference/4.13/axon-framework-commands/modeling/aggregate/
    id: axon-aggregate
    author: AxonIQ
    title: Axon Framework Reference - Aggregates
  - resource: https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html
    id: spring-data-persistence
    author: VMware/Spring
    title: Spring Data JPA - Persisting Entities
  - resource: https://docs.spring.io/spring-data/jpa/reference/auditing.html
    id: spring-data-auditing
    author: VMware/Spring
    title: Spring Data JPA - Auditing
  - resource: https://docs.hibernate.org/stable/orm/userguide/html_single/
    id: hibernate-userguide
    author: Red Hat
    title: Hibernate ORM User Guide
  - resource: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2.html
    id: jpa-32
    author: Eclipse Foundation
    title: Jakarta Persistence 3.2 Specification
---

# 写侧服务端生成字段（id / version / 审计时间）如何进入资源响应

> **结论摘要**
> - **AIP 义务落在「响应表示」，不落在领域模型。** AIP-133/134 要求响应是资源本身且 fully-populated，AIP-154 要求服务端在输出中提供当前
    etag——规范全篇未规定服务端如何取得这些值（读回 / 内存 / 计算）。因此「领域层感知持久化事实」本身不是 AIP
    的要求，是当前实现把「资源组装」放到了聚合上的结果。
> - **DDD 一手证据按字段分裂**：① 标识——Evans 明确「可来自外部或由系统自造」，Vernon 官方样例一律 **持久化前**由应用/仓储分配（
    `nextIdentity()` + UUID），`save()` 返回 `void`；② 版本——归实体/聚合自身（`ConcurrencySafeEntity` 层超类），但
    **递增归持久化机制**，Hibernate 明文「应用禁止改动版本号」，Vernon 反对手工 bump（"leaks infrastructural concerns into
    the model"）；③ 审计时间戳——DDD 一手无实体列模式，Evans 定位为「audit trails…not usually suited to be used for the logic
    of the program itself」，属基础设施/横切元数据（与 ADR-0031 一致）。
> - **主流实现里，回填即便存在也只覆盖 id，且无人返回完整资源**：COLA 示例 gateway 写侧全是 `void` +
    `Response.buildSuccess()`（不返回资源，本就不满足 AIP）；Axon 创建命令由框架自动返回聚合标识符，JPA 路径在同一受管实例上就地填充；kk-ddd
    确有回填，但 **只回填 id**（`Entity.fillInId`，write-once + 二次调用抛 IAE），且创建接口只返回 id；Spring Data
    的官方契约是「使用返回值」（`merge` 返回托管副本，入参不变）。→ 「响应组装」这一层没有现成答案可抄，必须自行合成。
> - **推荐形态**（四方案裁定见 §3）：响应组装归应用层，持久化事实经 **gateway 的 save 返回值**（
    `SaveResult{id, version, 时间戳}`）流向 DTO，而不是经聚合的 `assignXxx` 变异流向 DTO；`assignVersion` 可删除，`assignId`
    仅在「保留 DB 自增 + 领域事件需要新建实体 ID」时保留一个 write-once 通道。若想根除回填，把标识生成挪到应用/领域侧（TSID/UUIDv7，Vernon
    策略之「应用生成」）；版本与审计 **不要**下放领域层——那才是过度设计。
> - **落地状态（2026-09-11）**：经复审， **保留回填**成为终态；本笔记 §4.1 的 `SaveResult` 建议经权衡 **不落地**（它会让聚合的
    version 停止更新，而 `toPersistence` 把聚合 version 写入 merge 校验依据——见 §4.1 补记）。实际落地仅两项最小收紧：
    `Entity.assignId` / `User.assignVersion` 参数守卫；DP 更名 `Version` → `ConcurrencyVersion`（明示并发语义，与
    `SoftwareVersion` 区分）。
>
> **§4.1 补记（2026-09-11，推翻该节核心建议）**：`UserConvertor.toPersistence` 把 **聚合的 version** 写入 PO，该值被
> `em.merge` 用作 `WHERE version = ?` 的乐观锁校验依据。因此「删 `assignVersion`、版本只经 `SaveResult`
> 流向响应」会让聚合停留旧值 → 下次 save 直接 `ObjectOptimisticLockingFailureException`。version 不是只读展示字段，而是
> **写协议输入**；`assignVersion` 不是坏味道，是 PO/聚合分离下「ORM 自动回写领域字段」的手工等价物（IDDD
> `ConcurrencySafeEntity.concurrencyVersion` + Hibernate `<version>` 即自动形态）。 **§4.1 的删除建议与 §4.4 的首选项作废；§4.4
的守卫建议保留并已落地。**

---

## 1. 问题重述（三个被混淆的层次）

当前实现把三件事绑定在一个对象上：AIP 的 **响应表示**、DDD 的 **领域模型**、JPA 的 **持久化事实**。三者义务不同：

| 层次                | 责任                                              | 关键约束                                                            |
|---------------------|---------------------------------------------------|---------------------------------------------------------------------|
| 响应表示（AIP/RFC） | Create/Update 返回资源本身；etag 为当前值         | 是应用/适配层契约，不是领域契约                                     |
| 领域模型（DDD）     | 业务不变量、身份连续性、可比对版本令牌            | 不承载审计等基础设施表示（ADR-0031）；版本只读不递增                |
| 持久化事实（JPA）   | 生成/递增 id、version、审计列，落在**受管实例**上 | `merge` 返回托管副本 X'，回填只到 X'；IDENTITY 主键 Insert 后才有值 |

**机制事实**（下表，来源见 §2.4）：`saveAndFlush` 之后，服务端生成值 **都已在返回值实例上可见**——问题不是「值不可得」，而是「值经哪条路径进入响应」。

## 2. 证据

### 2.1 AIP / RFC：要求的是响应

- **AIP-133**：「The response message must be the resource itself. There is no `CreateBookResponse`.」；「The response should
  include the fully-populated resource, and must include any fields that were provided unless they are input
  only」。[来源: AIP-133](https://google.aip.dev/133) [primary]
- **AIP-134**：同款 MUST；另「must include any fields that were sent and included in the update
  mask」。[来源: AIP-134](https://google.aip.dev/134) [primary]
- **AIP-135**：硬删响应 `google.protobuf.Empty`；软删响应为资源本身（AIP-164）；保护删除的 etag 失配必须
  `ABORTED`。[来源: AIP-135](https://google.aip.dev/135) [primary]
- **AIP-154**：「The etag field **must be provided by the server on output**」；缺失 etag 时 SHOULD 放行；失配必须
  `ABORTED`。[来源: AIP-154](https://google.aip.dev/154) [primary]
- **AIP-203**：`OUTPUT_ONLY` 字段「provided in responses… server must clear out any value in this field on
  input」。[来源: AIP-203](https://google.aip.dev/203) [primary]
- **RFC 9110 §9.3.3**：POST 创建资源时「the origin server SHOULD send a 201 (Created) response containing a Location
  header field…and a representation that describes the status of the request while referring to the new resource (s)
  」；§13.1.1：If-Match 为假「the origin server MUST NOT perform the requested
  method」。[来源: RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html) [primary]
- **AIP 不涉及实现机制**：AIP 规范文本只定义响应形状与校验语义，不规定读回 / 内存计算 / ORM 回填 [inference，依据上述文本]。

### 2.2 DDD 一手：三个字段的归属

- **标识（identity）**：Evans「This means of identification **may come from the outside, or it may be an arbitrary
  identifier created by and for the system**, but it must correspond to the identity distinctions in the model」（Entities
  节）； **未**规定生成时机， **未**评价
  store-generated。[来源: Evans DDD Reference, Entities](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf) [primary]
- **Repository 语义**：Evans「create a service that can provide the **illusion of an in-memory collection**…Provide
  methods to **add and remove** objects…Return fully instantiated objects」—— **无任何 save
  返回值语义**。[来源: Evans DDD Reference, Repositories](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf) [primary]
- **Vernon 官方样例**（一手代码）：`ProductRepository.nextIdentity()` → `ProductService` 先
  `new Product(tenantId, productRepository.nextIdentity(), …)`，再 `add(product)`，最后 `return product.productId().id()`
  ——ID 来自 **内存对象**；agilepm / collaboration / identityaccess 三个子域同款（`nextIdentity()` =
  `UUID.randomUUID()...`，`save/add` 返回 `void`）。identityaccess 的 `User.hbm.xml` 是唯一例外：Hibernate 把 DB 自增
  **代理键**写入领域基类 `IdentifiedDomainObject.id`（域身份是 `(tenantId, username)`
  ，二者分离）。[来源: IDDD_Samples](https://github.com/VaughnVernon/IDDD_Samples) [primary]
- **版本归属**：Vernon「Class `ConcurrencySafeEntity` is a **Layer Supertype** [Fowler, P of EAA] used to manage
  **surrogate identity and optimistic concurrency versioning**」；「That type is the **domain-specific identity**, and it
  is **different from the surrogate identity**」；反对手工递增：「this code **leaks infrastructural concerns into the
  model**」。[来源: IDDD ch10 出版社节选](https://www.informit.com/articles/article.aspx?p=2020371&seqNum=8) [primary]
- **Hibernate 明文**：「 **Your application is forbidden from altering the version number set by Hibernate.**」；另「A
  version or timestamp property can never be null for a detached instance」（null 版本被当作
  transient）。[来源: Hibernate ORM User Guide](https://docs.hibernate.org/stable/orm/userguide/html_single/) [primary]
- **审计时间戳**：DDD 一手未见实体承载审计列的模式（[未找到]）；Evans 对审计的定位是「 **Audit trails can allow tracing, but
  are not usually suited to be used for the logic of the program itself**」（Domain Events 节）[primary]；Fowler Audit Log
  亦为横切记录，非领域概念 [primary，martinfowler.com/eaaDev/AuditLog.html]。→ 与 ADR-0031 的「审计列属基础设施表示」一致。
- **Reconstitution（从持久化重建领域对象）**：Evans 的 Repository 明文「Return fully instantiated objects…」，Vernon 样例中
  Hibernate 的 `.hbm.xml` 映射与仓储实现均属此路径—— **重建本身是正规职责**；但其定位是「装载」，无一手来源支持「save
  后立即重建以取服务端字段」[inference]。Fowler Identity Map「Ensures that each object gets loaded only once by keeping
  every loaded object in a
  map」——同事务内同一行应对应同一对象。[来源: PoEAA Identity Map](https://martinfowler.com/eaaCatalog/identityMap.html) [primary]

### 2.3 主流实现（COLA / Axon）与框架契约（Spring Data / JPA）

- **COLA（直接读源码，master）**：`UserProfileGateway.create/update` 与 `MetricGateway.save` 全部 `void`；
  `UserProfileAddCmdExe` / `UserProfileUpdateCmdExe` 执行后恒 `return Response.buildSuccess()`（响应体不含资源）；唯一显式「服务端生成
  ID 回填」是 MyBatis `MetricMapper.xml` 的 `useGeneratedKeys="true" keyProperty="id"`，回填目标是 **DO**（仅用于构造事件载荷），领域实体
  `MetricItem` 连 id 字段都没有；charge 示例（JPA）领域实体即 JPA 实体，`chargeGateway.saveAll(...)` 返回值被丢弃。全仓无
  `@Version`、无 auditing、无「响应返回资源」。[来源: alibaba/COLA](https://github.com/alibaba/COLA) [primary]
  → **COLA 不是本问题的参考解法**：它既不满足 AIP 的资源响应，也没有版本/审计回填。真正可借鉴的是它的 **边界态度**：gateway
  写侧不把持久化细节回灌领域。
- **kk-ddd（本地检出 `7d8e1b8e`，github.com/kingkh1995/kk-ddd）**：与 SODA 最接近的独立参照。
  `AccountRepositoryImpl.onInsert` / `UserRepositoryImpl.onInsert` 在 MyBatis `useGeneratedKeys="true"` 插入后调用
  `entity.fillInId(AccountId.of(data.getId()))`—— **回填只覆盖 id，不覆盖 version、不覆盖审计时间**（`AccountPO`/`UserPO` 有
  `createTime/updateTime/version` 列，但不进领域）。`Entity.fillInId` 契约为 write-once + fail-loud：已标识再调用抛
  `IllegalArgumentException("Already identified!")`、null 入参同样拒绝（`ddd-support-types/.../core/Entity.java`）。创建用例只返回
  id（`AccountAppServiceImpl.createAccount` 返回 `long`），Controller `@ResponseStatus(201)` 返回 `Long`—— **只满足 AIP-133
  的「有响应」精神，不返回完整资源**。[来源: kk-ddd](https://github.com/kingkh1995/kk-ddd) [primary]
  → 该样例直接印证 §4.4 的收紧方向（write-once + 冲突 fail-loud），同时说明「只回填 id」是独立项目的共同选择；
  **version/审计回填在五家参照中均无先例**。
- **yudao-cloud（本仓根目录只读参照副本）**：MyBatis-Plus，主键策略由 `IdTypeEnvironmentPostProcessor`
  按数据库类型决定（MySQL → `IdType.AUTO` 自增；PostgreSQL / Oracle / H2 → `IdType.INPUT` 应用侧输入）；创建接口
  `UserController.createUser` 返回 `CommonResult<Long>`—— **只返回 id，不返回资源**。无 DDD 领域层（DO/VO +
  MyBatis-Plus），仅作「id-only 响应」的旁证。[来源: yudao-cloud](https://github.com/YunaiV/ruoyi-vue-pro) [primary]
- **Axon**：`@AggregateIdentifier` 字段必须在「首个命令之后 / 首个事件发布之前」有值（事件溯源形态下必须在首个事件的
  `@EventSourcingHandler` 中设置）；创建命令的 handler 返回 `void` 时，框架 **自动返回聚合标识符**；`IdentifierFactory`
  生成的是消息 id（事件/命令/查询），业务可显式用它生成聚合 id；JPA 状态存储路径 `persist` + `flush`
  ，在同一受管实例上就地填充，无显式回填。[来源: Axon Reference Aggregates](https://docs.axoniq.io/axon-framework-reference/4.13/axon-framework-commands/modeling/aggregate/)、[AxonFramework 源码](https://github.com/AxonIQ/AxonFramework) [primary]
- **Spring Data 契约**：`CrudRepository.save` javadoc「 **Use the returned instance for further operations as the save
  operation might have changed the entity instance completely.**」；`SimpleJpaRepository.save` 源码：isNew →
  `persist(entity); return entity`（同一实例），否则 `return entityManager.merge(entity)`（ **托管副本 X'**，入参不变）——因此
  merge 路径的
  version/审计值只出现在返回值上。[来源: CrudRepository javadoc](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/CrudRepository.html)、[SimpleJpaRepository 源码](https://github.com/spring-projects/spring-data-jpa) [primary]
- **isNew 判定（回填方案的另一个前提）**：默认「有 version 属性看 version 是否 null，否则看 id 是否 null」； **手工分配 id
  的实体**需实现 `Persistable.isNew()`（官方示例：`@Transient boolean isNew` + `@PostPersist @PostLoad`
  翻转标记）。[来源: Spring Data JPA Persisting Entities](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html) [primary]
- **审计写入时机**：`AuditingEntityListener` 的 `@PrePersist`/`@PreUpdate` 回调；`@CreatedDate` 仅新建写入，
  `@LastModifiedDate` 新建与更新均写（默认 `modifyOnCreation=true`）；merge 路径回调作用于
  X'。[来源: Spring Data Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html) +
  `AuditingEntityListener` 源码 [primary]

### 2.4 机制表：值 → 何时可得 → 在哪个实例上可见

| 值                                   | 产生时机                                                                                                                          | persist 路径          | merge 路径                       | 来源                                              |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-----------------------|----------------------------------|---------------------------------------------------|
| ID（IDENTITY）                       | INSERT 执行时（post-insert）；`PrePersist` 回调中**不可用**                                                                       | `save()` 入参同一实例 | `save()` 返回的托管副本 X'       | JPA 3.2 §3.6.3；Hibernate §3.7.10/§27.4 [primary] |
| ID（SEQUENCE/TABLE）                 | `persist()` 期间取号，`PrePersist` 中**已可用**                                                                                   | 同上                  | 同上                             | JPA 3.2 §3.6.3 [primary]                          |
| `@Version`                           | 「updated by the persistence provider each time the state of an entity instance is written to the database」= flush/commit 写库时 | 入参实例              | X'                               | JPA 3.2 §3.5.2 [primary]                          |
| `@CreatedDate` / `@LastModifiedDate` | `@PrePersist` / `@PreUpdate` 回调                                                                                                 | 入参实例              | X'（回调作用于拷贝后的托管实例） | Spring Data 源码 + JPA 3.2 §3.6.3 [primary]       |

## 3. 四种候选方案逐条裁定

| # | 方案                       | 裁定             | 依据                                                                                                                                                                                                                                                                                                              |
|---|----------------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | 违反 AIP，调用方必须再 Get | **驳回**         | AIP-133/134 为 MUST；AIP-154 要求服务端输出当前 etag；本仓读侧在 query-server，写后立即 GET 还有可见性/一致性代价（且 D-14 已否决为 If-Match 做预读）。                                                                                                                                                           |
| 2 | save 后从 PO 重建新 entity | **不作通用机制** | 重建（reconstitution）本身是 Repository 正规职责（Evans「Return fully instantiated objects」），不算 DDD 违规；但作为写后响应机制违反同事务 Identity Map/Unit of Work 的「同一行 ↔ 同一对象」语义（Fowler），产生双份对象与额外映射，且并未解决「领域感知」——只是换了个实例感知。适用于装载/读路径。              |
| 3 | 返回过期资源信息           | **驳回**         | AIP「fully-populated resource」+ AIP-154「must be provided by the server on output」；ETag 陈旧则客户端回带必失配（ADR-0037 已论证漂移链）。                                                                                                                                                                      |
| 4 | 领域全权生成所有值         | **按字段分裂**   | 标识：主流认可（Vernon 策略「Application Generates Identity」，IDDD/Axon 样例即此），是根因解、不算过度设计；版本：不可——Hibernate 明文禁止应用改版本号，Vernon 称手工 bump 为「leaks infrastructural concerns into the model」；审计：一手文献无领域依据，ADR-0031 已归基础设施。**一刀切下放领域 = 过度设计**。 |

## 4. 推荐形态

### 4.1 核心重构：持久化事实走端口返回值，不走领域变异

- 响应的组装是 **应用层**职责：`ResourceDTO = (聚合的业务状态, SaveResult 持久化事实)`。聚合不再需要为「响应好看」而持有
  id/version/审计的全部权威值。
- 持久化端口返回持久化事实：`EntityGateway.save(T) → SaveResult<ID>{ID id, 可选 version, 可选时间戳}`（由网关从
  `saveAndFlush` 的返回值 X' 读取）。这正是 Spring Data 官方契约「使用返回值」在端口边界上的表达，与 ADR-0024 的 save
  统一路由不冲突（只改返回值形状）。
- 装载路径不变：version 仍由恢复构造器从持久化状态装入聚合，`requireIfMatch`（ADR-0037）读的是装载值——条件请求的比对不依赖回填。响应
  ETag 取 `SaveResult.version`。
- 由此 **`Versioned.assignVersion` 失去唯一调用方，可删除**；`assignId` 的取舍见 §4.3。
- 审计字段：现状（ADR-0031：不入领域与出站模型）保持不变；若将来按 AIP-148 暴露 `create_time`/`update_time`，同样经
  `SaveResult` 携带——接受「审计时间戳进入端口契约」这一点取舍，或明确维持 AIP-148 偏离。

### 4.2 领域层真正需要什么（回答「领域层怎么感知」）

- **身份**：领域需要（实体连续性、领域事件载荷、跨聚合引用）。这是唯一「必须感知」的持久化事实。
- **版本**：领域需要 **读**（If-Match 令牌），不需要 **写**；递增归 JPA。
- **审计时间**：领域 **不需要**，且不应承载。
- 响应字段本身不是领域需求——它是契约模型（DTO/Response）的组装输入。

### 4.3 身份生成时机：两条路线（本仓需二选一）

- **路线 A（根因解，Vernon 主流）**：应用层/领域服务先取号（`IdGenerator` 端口或 `Repository.nextIdentity()`
  ，ULID/UUIDv7/TSID）→ 构造聚合时身份已知 → `assignId` 与 `UserCreatedEvent` 的延迟求值同时消失，响应/事件天然带 id。Long
  可保留（TSID/snowflake 为 64 位、时间有序，规避 UUID 随机主键的索引页分裂代价）。代价：发号器实现（时钟/worker id 或库）、主键不再由
  DB 保证唯一、手工分配 id 需按官方章节实现 `Persistable.isNew()` 瞬态标记、迁移成本。
- **路线 B（务实解，保留 DB 自增）**：接受「代理键在 INSERT 后才存在」，保留 **唯一一个** write-once `assignId`
  （新建路径的领域事件载荷需要它；装载路径本就有 id），version/审计走 §4.1 的 `SaveResult`。这就是 Hibernate 在 IDDD 样例里对
  `IdentifiedDomainObject.id` 做的事（写入领域对象字段），也是 kk-ddd `fillInId` 的形态——不是 DDD
  违规，只是把隐式机制显式化，代价是身份与存储技术耦合、身份在持久化前不可知。

### 4.4 防御收紧（针对「不 saveAndFlush 也能被调用」）

- 首选 **消除方法面**：§4.1 删除 `assignVersion`；若走路线 A，`assignId` 一并删除。没有任何暴露 = 没有防御缺口。
- 保留 `assignId` 时：write-once（首次生效）+ **冲突值 fail-loud**（现实现 `isIdentified()` 时静默返回，建议改为值不一致抛
  ISE； **已落地**：`Entity.assignId` 同值幂等、异值抛裸 ISE）；javadoc 明确「基础设施回填持久化事实，非业务方法；调用时点 =
  flush 之后」。 **先例**：kk-ddd 的 `Entity.fillInId` 即此形态——已标识再调用直接抛
  `IllegalArgumentException("Already identified!")`，且拒绝 null 入参（`null` 会掩盖持久化故障）。
- 递增防护：`assignVersion` 若保留，必须单调（拒绝回退值）与拒绝 null；否则宁可删除。 **已落地**：`User.assignVersion`
  接受同值/递增、回退抛裸 ISE（AD 校验非空由 `ConcurrencyVersion.of` 承担）。
- 无法用 ArchUnit 兜底：ADR-0036 明确不引入机械结构检查，Modulith 只治理模块依赖。因此「把方法暴露给业务却靠纪律约束」是本仓最弱的一环——这本身就是选「消除」而非「治理」的理由。

## 5. 与现有决策的关系

- 本笔记是 **输入**，不是决策：落地时按仓库惯例落 ADR（就地更新 [ADR-0024](../adr/0024-gateway-save-unified-routing.md) 的
  save 契约与 [ADR-0037](../adr/0037-optimistic-lock-version-guard.md) 的 assignVersion 机制；身份生成时机若改，另立新
  ADR）。[ADR-0031](../adr/0031-wire-semantic-literals-decimal-epochmilli.md) 不变。
- 与 [aip-api-conventions.md](../conventions/aip-api-conventions.md) §3.4/§3.5/§9.2 的偏离表联动：F-03
  类「响应是资源本身」的合规不依赖领域感知，可在本形态下保持。

## 6. 未找到 / 未核实

- IDDD 第 5 章「When the Timing of Identity Generation Matters」正文原句（官方样章与镜像不可得；以官方目录/索引 +
  出版社节选 [primary] + 逐段引述 [secondary] 佐证）。
- PoEAA `registerNew` 官方摘要原文（官方页无正文）。
- DDD 一手文献中「审计时间戳由聚合承载」的模式论述——确认不存在。
- jMolecules（Spring 生态的 DDD 注解库）对 `@Version`/审计注解置于领域类的官方表述——本次未取得可核验来源，不引用。
- 清点覆盖五家参照：COLA / kk-ddd / yudao-cloud / Axon / IDDD_Samples； **无一家返回满足 AIP-133/134 的完整资源**（COLA
  返空、其余返 id）。
