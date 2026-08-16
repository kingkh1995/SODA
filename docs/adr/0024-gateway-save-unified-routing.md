# 0024 — Gateway save 统一路由：insert / put 全量更新 / 终态处理

**Status**: accepted（2026-08-15）

> 修订（2026-08-15 同日演进，二次修订）：convertor 仅保留 `toPersistence`（全量构造新 PO）——
> 「显式搬运审计列」语义撤销：merge 全量拷贝（含 null）不会覆盖审计列——`created_date` 由基类
> `updatable=false` 保护（UPDATE 语句排除该列）、`last_modified_date` 由 auditing `@PreUpdate`
> 刷新（实证：更新路径不搬运审计列两列均正确，见 `PersistenceEndToEndTest.should_updatePreserveAuditColumns`；
> 原「auditing 缺失防御」论据不成立——auditing 缺失时创建路径 `@PrePersist` 不填审计列，
> INSERT NOT NULL 违反，系统创建即不可用）。
> 「托管实例原地覆写」（Considered Option A 已否决路径）仍禁止：flush 用加载快照版本校验，
> 陈旧聚合冲突无法自动检测，2026-08-15 实证踩中后回退。
> `PersistenceGuard.assertNotTerminal` 已删——终态拒写收敛在 `UserGatewayImpl.save` 内联行终态判定裸抛 ISE
> （防御编程不携消息，异常类型 + 栈帧即语义）：检查对象仍是**行状态**（D→R 迁移合法写终态 R，行状态
> 才是权威，见 ADR-0023 语义）。
> <p>
> **`@Version` 强制曾当日引入后撤销**：`AbstractAuditable` 曾加统一 `@Version`（VerificationPO
> 随之获得版本路由），随后撤销——Verification 领域**不带版本令牌**（不同于 User：领域
> `version` 跨读改写间隙携带），每次更新同事务 `findById` 取当前版本，PO 层版本对跨加载
> 竞态**空转**（乐观锁永不触发）；Verification 的并发安全来自状态机单调迁移 + 源状态断言
> （重复应用幂等，无反向迁移）+ changeMobile 同事务的 User 版本兜底。故 `@Version` 仅保留
> 于 UserPO（领域令牌承重，`should_optimisticLockConflict` 原生通过），user_verification 无
> version 列；未来出现「持有验证实体跨读改写间隙再 save」的用例（如 resend）时，再为领域
> `Verification` 加 version（User 同款）。

## Context

`Verification` 聚合创建时即由领域层分配 id（`SmsVerification.create` → `UUId.random()`），save 时恒已标识——`EntityGateway.save` 文档化的「isIdentified() → update」启发式对客户端生成 id 的聚合失效（新建聚合已标识）。`VerificationGatewayImpl.save` 此前依赖两个妥协：

1. `id == null` 生成分支——客户端生成 id 场景永不命中（死代码），且命中必抛 `PersistenceException`（`VerificationPO.@Id` 无 `@GeneratedValue`）；
2. 盲目 `markNotNew()` 恒走 merge——insert/update 判别被委托给 merge 的运行时存在性探测：每次 INSERT 前多一次 SELECT，且 merge 对已删行 zombie 重插（ADR-0017 明列的风险向量）。

同时 save 的三语义（insert / put 全量更新 / 终态处理）此前散落在「id 启发式 + merge 存在性探测」的隐式行为里，与表示自由度（ADR-0017 §4）不匹配——终态（User R / Verification U）如何落库是基础设施策略，领域层永不表达 remove。

演进（本决策当日）：先采「findById 判别 + 托管实例复用 + overwrite 覆写」，后发现其与 JPA 乐观锁原生机制互斥——托管实例携带加载快照版本，flush 校验恒对最新快照放行，陈旧聚合的并发冲突无法被 JPA 自动检测，被迫手动比对 version（违背「让 Spring Data JPA 底层校验」的诉求）。故回退到 **merge 全权委托**。

## Decision

save 保持单一入口（`EntityGateway.save`），**全权委托 Spring Data JPA**——不实现 Persistable transient 标志，`isNew()` 为 `id == null` 判定（Spring Data 自身 AbstractPersistable 语义，收进 `AbstractAuditable`）：

1. **路由由 isNew + merge 内建**：`repository.save(toPersistence(domain))`——id==null（服务端生成 id 创建路径）→ persist（INSERT，IDENTITY 立即生成 id 回填 `assignId`）；id 恒有（客户端生成 id 聚合、既有行更新）→ merge 按行存在性统一路由（无行 INSERT、有行 detached 状态全量拷贝——含 null，领域 null = 清空列——→ flush 时 dirty-check 出 UPDATE）。insert/update 判别由框架内建，网关零判别逻辑。
2. **乐观锁原生**：`@Version` 聚合（UserPO）由 merge 规范强制版本校验（JPA 3.2 §3.3.7.1）——领域 version 与托管/持久化态不一致 → `OptimisticLockException`（Hibernate `StaleStateException` → Spring 翻译 `ObjectOptimisticLockingFailureException`）→ 事务整体回滚。无手动比对、无行数判断。同事务加载（应用层「update 前必加载」不变量）享 ctx 命中，update 路径零额外 SQL。
3. **终态处理是聚合级基础设施策略**：领域层只表达状态（`deregister()` → R、`use()` → U），无 remove 词汇。Verification 终态 U 走 put 路径写状态保留行；User 终态 R 在网关内归档原键（`user_archive`）+ 键置空 + merge 落库（同事务，冲突回滚归档行）。remove（物理删行）为演进路径——本框架无 DELETE 契约（ADR-0017）；如未来某聚合采用，在对应 gateway.save 内裁决，领域与 app 层零改动。
4. **基类精简但保留**：`AbstractPersistable` 以精简形态保留——仅 `implements Persistable<ID>` + `isNew() = getId() == null`（Spring Data 自身 AbstractPersistable 语义），**所有 PO 强制继承**（`AbstractAuditable` 继承之，`UserArchivePO` 直接继承）。早期 transient isNew 标志 + `markNotNew()` + `@PostLoad`/`@PostPersist` 翻转机制删除。`overwrite`/findById 判别/手动版本比对等中间产物全部删除。
5. **convertor 全量构造**：`toPersistence` 逐列赋值（含显式 null），是 merge 全量拷贝的唯一来源；无覆写原语（merge 整体拷贝，无需逐字段覆写既有实例）。

## Considered Options

| 方案 | 结论 |
|---|---|
| A. findById 判别 + 托管实例复用 + overwrite（曾采用） | 否决（当日演进）：与 JPA 乐观锁互斥——flush 用加载快照版本校验，陈旧聚合冲突无法自动检测，被迫手动比对 version |
| B. 领域持久化状态标志（`Entity.isPersisted` + `markPersisted`） | 拒绝：领域基类引入持久化状态字段；merge 全权委托下无需任何判别状态 |
| C. 网关拆显式 `insert()` / `update()` | 拒绝：判别权散落调用点，推翻 save 统一入口契约；选错 = PK 冲突或 zombie |
| D. save 内 `existsById` 探测 | 拒绝：insert 两次查询 + 检查与写入间竞态窗口 |
| E. 手写 identity map 复用 PO 实例 | 拒绝：需要判别的两条 save（create、PENDING）均无同事务 load，方案失效大半；进程级弱引用 map 复杂且对序列化边界脆弱 |
| F. `em.lock(managed, OPTIMISTIC)` 补校验 | 拒绝：失败点后移到 flush/提交、需向网关注入 EntityManager、未实证——用魔法换 merge 原生机制，不划算 |

## Consequences

- `VerificationGatewayImpl.save` / `UserGatewayImpl.save`：`repository.save(toPersistence(domain))` 全权委托 + 聚合策略（守卫/归档/创建回填）；SQL 逐流同价（create = SELECT + INSERT 或 persist、PENDING = SELECT + UPDATE、同事务 update = 零额外查询）；zombie 行为与 merge 现状相同（无行即 INSERT），无回归。
- 乐观锁：`@Version` 聚合由 merge 自动校验（版本不一致 → 异常 → 事务回滚），`should_optimisticLockConflict` 原生通过。
- `AbstractPersistable` 精简保留（Persistable 契约 + id==null 判定，所有 PO 强制继承；取代 ADR-0023 的 transient 标志部分）；`markNotNew`/`overwrite`/findById 判别/手动版本比对全部移除。
- `EntityGateway.save` 契约：insert / put 全量更新 / 终态处理的统一入口，路由由框架内建；客户端生成 id 聚合 save 前置：必须已标识。
- 测试：`PersistenceEndToEndTest` / `VerificationFailureResendTest`（create / PENDING / changeMobile / deregister / 乐观锁冲突全链路）、convertor 单测（toPersistence 全量构造含 null 清空）、ModulithTest。

## 依据（2026-08-15 梳理）

- `SimpleJpaRepository.save` = `isNew ? persist : merge`；`Persistable.isNew()` = `id == null`（`AbstractPersistable` 精简形态）——本仓库 PO 中：VerificationPO/UserArchivePO id 恒有 → 恒 merge；UserPO 创建路径 id==null → persist、更新路径 id 恒有 → merge；有 `@Version` 的实体（UserPO）isNew 判定由 version 值决定（version 属性存在时优先于 Persistable），创建路径 version=0 亦走 merge（transient → INSERT），两种判定结果一致。
- JPA merge 语义：detached 实体状态全量拷贝（含 null）到托管实例；无行则 INSERT；`@Version` 实体版本不一致 → `OptimisticLockException`（JPA 3.2 §3.3.7.1，规范强制；`should_optimisticLockConflict` 实证）。
- 同事务加载享一级缓存 ctx 命中（PersistenceContext identity map），merge 免二次 SELECT。
- 初始化归属判据：依赖基础设施能力（DB 自增）→ 基础设施；纯计算/常量（`UUId.random()`、`Version.INITIAL`）→ 领域。
- ADR-0017（终态持久化、表示自由度、zombie merge 向量）、ADR-0023（键释放/归档/守卫——isNew 部分被本决策取代）。
