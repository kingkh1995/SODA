# 0017 — 聚合生命周期与删除语义：终态 R 持久化，领域无擦除

**Status**: accepted

## Context

`User` 的删除此前是 hybrid 设计（ADR-0015「生命周期守卫」类别的延续）：

- 领域方法 `remove()`：前置必须 `D`，注册 `UserRemovedEvent`，置位 **transient** `removed` 标志；
- 落点是 `gateway.remove(user)` **物理删行**；
- 守卫分裂成二维：`mustEnable()`（状态前置）+ `assertNotRemoved()`（删除前置），每个变更方法顶部手动调用——调用约定，非结构约束；
- `isRemoved()` 泄漏到 gateway javadoc 契约（"save 前应检查"），第二道防线只是文档。

三个问题：

1. **语义错位**：领域宣称"删除"是状态迁移（有前置、有事件），落点却是基础设施擦除。行删除后终态无从观察，`removed` 只能做 transient flag——二维生命周期，两个守卫。
2. **业务不变量无处安放**："仅禁用可删除"是真实业务规则，擦除建模无法承载它（Vernon EAD Part I：删除应建模为受限业务不变量，见文末依据）。
3. **zombie 复活是规范级真实风险**：JPA 3.2 规范（ch03 Merging Detached State）：merge 行已被删除的 detached 实体 → 创建新 managed 副本 → flush 时 INSERT；Spring Data `SimpleJpaRepository.save()` 对非 new 实体一律 `merge`（4.0.6 源码 `isNew ? persist : merge`）。"remove 后 save"不是理论风险。

## Decision

**删除建模为领域终态迁移，擦除不属于领域。** 依据主流 DDD 实践（源码级核实，见文末依据）：

1. **`UserState` 增加吸收态 `R`（Removed / 注销）**：`{E, D, R}` 单维度。R 后无任何操作——`mustEnable()`（state == E）天然覆盖 R，`assertNotRemoved()` 与 transient flag 删除。`disable()` / `enable()` 遇 R 抛带消息 IAE（吸收态）。
2. **领域方法 `deregister()` 替换 `remove()`**：D→R 严格迁移（前置必须 D，业务不变量），注册 `UserDeregisteredEvent`（事件随方法一并改名，领域层无 remove 词汇）。命名用业务词（注销）——终态表示下物理上无删除，`remove()` 名会撒谎。
3. **gateway 仅保留 `save`**：`gateway.remove()` 与 `isRemoved()` javadoc 契约删除。终态持久化走 save，表示由基础设施决定。
4. **基础设施表示自由度（受业务可观测性约束）**：R 的持久化表示（状态列 / 软删列 / 删行 / 归档表）是基础设施的私有决定，领域不知晓。约束：表示决定业务可观测性——产品需要"注销后可查询/审计"则只能选保留类表示；接受"注销即消失"则可删行（事件为持久记录）。**当前实现选状态列（保留）**：与 yudao 读侧语义同构（deleted 列 → state != R 过滤）、引用完整性、审计完整，且删除路径不存在 → zombie 向量整体消失。
5. **防御归属**：领域 = 状态机前置（IAE，延续 ADR-0015 业务校验分类）；应用 = 流程编排（require → deregister → save）；基础设施 = 表示 + 乐观锁（未来引入物理清理时的跨事务防护）。领域零基础设施感知。

### 与 Verification 的一致性

`Verification` 已是单一维度状态机（I/P/V/U 严格迁移，无第二 flag）。本 ADR 将 `User` 收敛到同一形态：生命周期 = 一个枚举 + 命令迁移方法。此后新增状态聚合一律沿用此形态。

### 操作语义（延续 ADR-0015 分类）

| 方法 | 语义 | 已到达目标态 | R（吸收态） |
|---|---|---|---|
| `disable()` / `enable()` | set-state 幂等 | no-op，不发事件 | IAE（带消息） |
| `deregister()` | 严格 transition（D→R） | 非 D → IAE | IAE（带消息） |

## Considered Options

- **保留现状（remove + transient flag + gateway.remove + javadoc 契约）** — 拒绝：二维生命周期、调用约定守卫、`isRemoved` 泄漏、zombie 是规范级真实风险。
- **擦除表示（B：领域到达 R 后删行，事件兜底）** — 拒绝（作为当前默认）：产品需要注销可查询（yudao 引用完整性、审计）；B 需乐观锁防复活，防御成本更高。领域代码与保留表示完全一致，未来需要时可作为表示切换，无需改领域。
- **D 即终态（吸收 disable，不新增 R）** — 拒绝：`enable()` 与之矛盾；"仅禁用可删除"规则变空洞；禁用（可逆）与注销（不可逆）是产品区分的两件事。
- **状态机对象 / 框架（Spring Statemachine / stateless4j / GoF State）** — 拒绝：refactoring.guru「few states → overkill」；Vernon 正文立场 = 枚举字段 + 根命令迁移；IDDD ch.10「DI of Repository/Domain Service into Aggregate harmful」。
- **领域方法保留 `remove()` 命名** — 拒绝：终态表示下物理上无删除，命名撒谎；`deregister`/注销 与 UL 一致。

## Consequences

- 代码改造范围（待实施）：
  - `User`：`UserState` 增加 `R`；`remove()` → `deregister()`（D→R 严格，前置 D，发 `UserDeregisteredEvent`）；删除 `removed` flag / `assertNotRemoved()` / `isRemoved()`；`mustEnable()` 保持（覆盖 R）；`disable()` / `enable()` 增加 R → IAE。
  - `UserServiceImpl.deleteUser`：`require → deregister() → save`（不再 `gateway.remove`）。
  - `UserGateway`：删除 `remove(user)` 与 isRemoved javadoc 契约；`save` 承载终态持久化。共享基类 `EntityGateway.remove()` 一并删除（重构后零调用方，领域层无删除契约）。
  - 读侧（query-server）：正常查询过滤 `state != R`；管理端可见。
  - 测试：`UserTest` / `UserServiceImplTest` 断言同步（`isRemoved()` → state == R 语义、remove → deregister、R 吸收态断言）。
- CONTEXT.md 已同步：`UserState` 增加 R 与 `deregister()` 跃迁；`User` 词条增加 `deregister()`；「删除」退出领域词汇（Avoid 清单）。
- 接线（issue 17）不受影响：`deregister()` 前置仍为 IAE → 400。

## 依据（源码级核实，2026-08-09）

- Vernon, *Effective Aggregate Design* Part I（kalele.io PDF）: "If a backlog item is committed to a sprint, we must not allow it to be removed from the system. There are other ways for the team to prevent inappropriate removal without being arbitrarily restrictive."
- Vernon, *IDDD* 1st ed. ch.10 "Aggregates"（InformIT 官方摘录 seqNum=7/8）: 状态不变量管在根上、状态变更命令 bump version；"Dependency injection of a Repository or Domain Service into an Aggregate should generally be viewed as harmful."
- refactoring.guru / State（GoF 阐释）: "overkill if a state machine has only a few states or rarely changes."
- JPA 3.2 规范（jakartaee/persistence 源码 ch03-entity-operations.adoc）: Merging Detached State（zombie 向量：无既有 managed 副本则创建新副本 → flush INSERT；有版本须校验一致）；Removal（detached → IAE 或 flush 失败；`remove()` 不修改实体字段）。
- Spring Data 4.0.6 `SimpleJpaRepository`（Maven Central 源码）: `save = isNew ? persist : merge`；`deleteById = findById().ifPresent(delete)`；`delete(T)` 找不到为 NOOP。
- 「don't delete aggregates」为社区共识，非 Vernon/Evans 逐字——本 ADR 不依赖该句，依赖上述直接引文。
