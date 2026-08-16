# 0023 — 注销终态键释放与归档审计（含终态守卫与持久化模型命名）

**Status**: accepted（2026-08-13）

> 2026-08-15 修订：`AbstractPersistable` 早期形态（transient isNew 标志 + `markNotNew()` 翻转）被 ADR-0024 取代——merge 全权委托后仅保留 Persistable 契约 + `isNew() = id == null` 的精简形态，仍为所有 PO 的强制统一基类；其余（键释放/归档/终态守卫/命名）继续有效。

## Context

`User` 注销（D→R）后行保留（ADR-0017 状态列表示），`uk_username`/`uk_mobile`/`uk_email` 全局唯一约束使三键被终态行**永久占用**——无法满足「注销后手机号/用户名/邮箱可再次注册」的需求。同时存在两个隐患：领域守卫（`mustEnable`/吸收态 IAE）之外的写路径可修改终态行；注销后原键（PII）滞留用户行，弱于《个保法》第 47 条「注销应删除或匿名化」。

行业实证（2026-08-13 调研）：微信（60 天后悔期后释放 + 注册次数限制）、微博（登录名释放可再注册）、抖音（7 天冷静期后释放）、B站（用户名释放）、Telegram（当日可再注册）——**释放行为是一致主流**；Google 反例（Gmail 地址永不回收）说明「哪些键释放」是产品决策。数据处置取向：大厂注销后删除或匿名化全部个人数据，审计靠日志/归档系统而非保留用户行。

## Decision

**键释放是基础设施表示决策，领域零感知**（延续 ADR-0017 §4 表示自由度）：

1. **D→R 迁移走 `UserGatewayImpl.save` 特殊分支**：`user_archive` 表同事务写入原键快照（id=原用户 ID/username/mobile/email）→ 三键置空落库（`username`/`mobile`/`email` = NULL）。NULL 不参与 MySQL 唯一索引（多 NULL 共存，官方语义）——`uk_*` 原样保留，`existsBy*`/`findBy*` 查询**零改动**（DB NULL 匹配不到任何真实键）。同事务性保证并发安全：双注销后写者 flush 时 `@Version` 冲突，归档行随事务一并回滚，无孤儿归档。
2. **领域默认值**：`Username` 增加常量 `REMOVED = new Username("removed")`（合法 DP 值）；convertor 恢复时空 `username` 映射 `REMOVED`。该值**只存在于领域内存，从不落库**——"removed" 仍是可注册用户名，无命名空间浪费。领域层零行为改动（`deregister()`/状态机/构造器全不动）。
3. **终态行不可写守卫（基础设施兜底）**：`save` 更新路径经状态列投影读取持久化态，`terminal()` 为真 → `IllegalStateException`（"terminal user row is immutable"）。按 `StateEnumType.terminal()` 泛化——新增终态（如永久封禁 PB）只需枚举成员标记，守卫零改动。
4. **`StateEnumType` 框架契约**：`com.soda.component.domain.StateEnumType extends EnumType`，提供实例谓词 `terminal()`（构造器注入字段，record 风格，常量声明处自文档）。`UserState`（R 终态）与 `VerificationState`（U 终态）实现。**不提供** `isInitial()`/`isNew()`：初始态语义随状态机而异（未来「待激活」态会使创建态不再是初始态）；`isNew` 与 Spring `Persistable#isNew()`（持久化状态检测）同名异义。
5. **持久化模型命名**：具体数据库模型类统一 `XxxPO`（`UserPO`/`VerificationPO`/`UserArchivePO`，JPA `@Entity` 注解保留）；审计+isNew 基类改名 `AbstractAuditable`（镜像所实现接口 `Auditable`，与 Spring Data `Auditable` 同名对齐；AbstractXxx implements Xxx 模式）。
6. **归档表为纯审计存储**（insert-only，`archive_time` 由 Spring Data auditing 填充（`@CreatedDate`）；无 update 概念故不复用审计列对 `created_date`/`last_modified_date`）：原键仅经合规审计通道访问；保存期限（个保法 19 条最短期限，网交办法三年为上限参考）届满后清除 job 处理——**演进路径，本期不做**。

## Considered Options

| 方案 | 结论 |
|---|---|
| **A. 事务内键置空 + 领域默认值 REMOVED**（选定） | 行业主流行为；NULL 免碰撞；查询零改动；个保法合规；领域零行为改动 |
| B. 生成列部分唯一（`IF(state='R',NULL,key)` + UNIQUE） | 拒绝：PII 永存弱合规（大厂实证删除/匿名化）；existsBy/findBy 全部加 state 过滤；每新终态改 DDL |
| C. 归档 + 删行（移行至 user_archive 后 DELETE） | 拒绝：推翻 ADR-0017「保留」表示；zombie merge 向量回归（ADR-0017 明列）；与「终态行守卫」目标矛盾 |
| 哨兵/标记（DB 存 `"R:"+id`） | 拒绝：非法 DP 值污染持久化层，convertor 被迫状态分支，领域类型兼容成本 |
| 常量默认值直存 DB（`"removed"` 落列） | 拒绝：`uk_username` 下第 2 个 R 行即碰撞（注销迁移写入时刻，领域/守卫均拦不住） |
| 领域 username 改 `@Nullable` | 未选：等价表达，但领域字段/getter/构造器/JSON 四处改动——用户选择 convertor 补默认值保领域零改动 |
| 归档回填（恢复时从归档查回原键） | 拒绝：归档变**恢复依赖**→ 永久保留不可清除（个保法 19 条不可实现）；PII 经写侧回灌；读写不一致（query-server 显示 NULL vs 写侧原值） |

## Consequences

- 代码改动（已实施）：`Username.REMOVED`；`StateEnumType` + `UserState`/`VerificationState.terminal()`；`UserPO` username 可空；`UserConvertor.toDomain` 空值映射 + `toArchivePersistence`；`UserArchivePO`/`UserArchiveRepository`；`UserGatewayImpl.save` 分支（守卫 + 归档 + 置空，持久化态经 `UserRepository.findById` 读取，守卫为网关内行终态判定裸抛 ISE）；V1 直改（username NULL + `user_archive` 表，ADR-0022 约定）；命名重构：`UserEntity`→`UserPO`/`VerificationEntity`→`VerificationPO`/`AbstractAuditableEntity`→`AbstractAuditable`，持久化基类拆分为 `AbstractPersistable`（官方 isNew 模板，独立复用；不采用 Spring Data `AbstractPersistable`——id==null 判定对客户端分配 id 失效、`@Id @GeneratedValue` 硬编码、setId protected、无审计列）+ `AbstractAuditable`（审计列），`UserArchivePO` 继承前者。
- 查询语义：R 行三键 NULL，`findByUsername/Mobile/Email`、`existsBy*` 天然不命中——键释放后立即可再注册，**零查询改动**。
- 读侧（query-server，未建）：R 行 username 显示空（NULL），无需标记翻译。
- 契约：恢复的 R User 内存 `username = REMOVED`（非真实登录名）；R 行吸收态无 username 业务，影响面为零。
- 测试：`UserConvertorTest`（空键恢复 REMOVED）、`PersistenceEndToEndTest`（deregister 全链路：三键 NULL + 归档原键 + 同名再注册 + 终态行拒写）、`UserStateTest`/`VerificationStateTest`（terminal 契约）。
- 真库 cutover（ADR-0022）：V1 定稿版为基线；归档表保留期策略在引入清除 job 时另立 ADR。

## 依据（2026-08-13 调研）

- 微信（60 天后悔期/次数限制）、微博（登录名释放）、B站（用户名释放）、抖音（7 天冷静期）、Telegram（当日可再注册）、Google（Gmail 永不回收）——注销资源释放实证。
- MySQL 官方语义：唯一索引允许多个 NULL（NULLs distinct，bug #8173）。
- 《个人信息保护法》第 19 条（最短期限）、第 47 条（注销应删除或匿名化）；《电子商务法》第 31 条/网交办法（交易与身份信息三年保存上限参考）。
- ADR-0017（终态 R 持久化、表示自由度）、ADR-0022（V1 直改、H2-only）。
