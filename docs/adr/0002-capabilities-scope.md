# 0002 — Cacheable / Lockable / Trackable 能力范围决策

**Status**: accepted

**Context**:

项目需要确定 Entity 和 Aggregate 是否应携带缓存（Cacheable）、分布式锁（Lockable）、变更追踪（Trackable）能力。

候选方案讨论了三轮：

1. **接口方案**：Cacheable / Lockable 作为 Entity 的可选接口，Trackable 作为 Aggregate 的可选接口。子类按需实现。
2. **链路修正**：Lockable / Cacheable 移至 Aggregate 层（一致性边界为聚合根），Trackable 仍在 Repository 层。
3. **再次修正**：Lockable / Cacheable 作为 Entity 必选能力，所有 Entity 自带 `cacheKey()` / `lockKey()` 方法。

**Decision**:

这三种能力**都不放在领域层（Entity / Aggregate）**。具体分层：

| 能力 | 归属层 | 实现方式 | 说明 |
|---|---|---|---|
| Cacheable | **Application** | Spring `@Cacheable` | 缓存区域名（cache name）和 key SpEL 写在 ApplicationService 上。领域层零缓存感知。 |
| Lockable | **Application** | 自定义 `@Lockable` 注解 + AOP（参照 Spring `@Cacheable` 设计模式） | lock resource 通过 SpEL 或工具类从 Entity.id 推导，不侵入 Entity 基类。 |
| Trackable | **Infrastructure** (Repository) | 参考 kk-ddd `AggregateTrackingManager` | Repository 内部 snapshot/diff 实现部分更新，Aggregate 本身无追踪逻辑。 |

**Rationale**：

- **领域层纯化**：Entity 的唯一职责是业务身份和业务行为。缓存、锁定、持久化优化不是领域概念。
- **基础设施无关性**：领域层不应引用或感知 Redis、分布式锁 SDK、MyBatis 等基础设施类型或配置。
- **接口无意义时的退化**：如果所有 Entity 都拥有某能力（如 `cacheKey()`），那么接口表达不了"可选"语义，退化为基类方法。而基类方法又应与领域相关，否则是职责膨胀。
- **复用 kk-ddd 验证结论**：kk-ddd `AggregateTrackingManager` 已在 Repository 层证明可行，无需搬进 Aggregate。
- **现有代码无需修改**：当前 `Entity`（仅 id + getter）和 `Aggregate`（空继承）保持不动。

**Consequences**:

- **Positive**: 领域层保持最小接口，不引入基础设施依赖。开发者不会在 Entity 上看到和方法无关的 key/ttl 方法。
- **Positive**: ApplicationService 显式声明缓存和锁策略，便于 review 和测试。
- **Negative**: ApplicationService 中需要手工或工具类推导 entity key（`ClassName + ":" + entity.getId().identifier()`），不能通过 Entity 方法直接获取。建议在 `soda-component-support.util` 提供 `KeyUtils` 工具类解决重复劳动。
- **Negative**: 读服务（query-server）如需缓存独立配置，与写侧 ApplicationService 的缓存策略可能不一致，需团队约定。

**Considered alternatives**:

| 方案 | 放弃原因 |
|---|---|
| Cacheable/Lockable 接口放 Entity | 缓存/锁定是基础设施概念，领域层不应感知。接口无意义时退化为基类方法。 |
| Lockable 放 Aggregate 层 | 锁的粒度与聚合根一致，但锁 key 推导（`className:id`）不是领域行为，不应由 Aggregate 提供。 |
| Trackable 放 Aggregate | 变更追踪是持久化优化，不是领域不变量。kk-ddd 证明 Repository 层实现可行。 |
| 用注解在 Entity 上声明 key | 注解也是依赖（`@Cacheable` 在领域层暴露 Spring 框架类型）。 |

**Related documents**:

- `CONTEXT.md` — 术语定义（Cacheable / Lockable / Trackable）
- `0001-module-architecture.md` — 模块分层基础
- kk-ddd `AggregateTrackingManager` — 变更追踪参考实现
