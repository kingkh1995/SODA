# 0003 — Entity ID 生命周期设计

**Status**: superseded by three-constructor approach (see note)

**Context**:

项目需要确定 Entity 的 ID 字段行为。初始设计（0001）使用 `final ID id` 配合强制构造器 `Entity(ID id)`，要求所有 Entity 在构造时必有 ID。

该设计排除了两个真实场景：
- **DB 自增 / 序列**：insert 前无 ID，需持久化后填补
- **MyBatis-Plus ASSIGN_ID / 框架生成**：构造时无 ID，save 前由基础设施层生成

同时，领域层是否应感知 ID 的生命周期策略也是一轮讨论重点。

候选方案迭代了四轮：

1. **final ID + 单一构造**（原始设计）：Entity 必须有 ID。UUID 可用，DB 自增不可用。
2. **非 final id + Fillable 接口**：id 改为非 final，`Fillable` 接口暴露 `fillInId()`。UUID 场景不实现 Fillable，DB 自增场景实现。问题：两个构造路径不一致。
3. **策略标记接口**：`ClientGeneratedId` / `ServerAssignedId` 标记接口，Entity 构造时 instanceof 检查。问题：所有策略写死在基类 if-else 链，违反 OCP。
4. **策略模式 — IdPolicy 组合**（选定）：`IdPolicy` 接口定义三个工厂方法，Entity 组合一个策略实例。新增策略只需新增工厂方法，Entity 零修改。

**Decision**:

Entity 的 ID 生命周期通过 `IdPolicy` 策略组合控制，策略接口预置三种行为：

```java
// 客户端自动生成（UUID / Snowflake）
static <ID> IdPolicy<ID> generated(Supplier<ID> supplier)

// 手动注入（外部预分配 ID）
static <ID> IdPolicy<ID> manual(ID id)

// 服务端生成（DB 自增 / 序列）
static <ID> IdPolicy<ID> deferred()
```

Entity 基类只有一个构造器 `Entity(IdPolicy<ID> policy)`，所有子类走统一入口。
每种 `Identifier` 类型声明 `AUTO` 常量（如 `UUId.AUTO`），提供默认策略，避免重复传参。

新增文件：

- `Identifiable.java` — 查询接口（`getId()` / `isIdentified()`），所有 Entity 实现
- `IdPolicy.java` — 策略接口 + 工厂方法 + `@FunctionalInterface`
- `IdPolicies.java` — 包级私有，持有 deferred 无状态单例

修改文件：

- `Entity.java` — `id` 非 final，组合 `IdPolicy`，实现 `Identifiable`，提供 `assignId()` / `sameIdentityAs()`，不覆写 `equals`/`hashCode`
- `Aggregate.java` — 构造器透传 `IdPolicy`
- `UUId.java` — 默认策略常量 `AUTO = IdPolicy.generated(UUId::random)`
- `LongId.java` — 默认策略常量 `AUTO = IdPolicy.deferred()`

**Rationale**：

- **领域职责正确**：ID 生命周期是 Entity 的固有属性（身份标识何时产生），不是基础设施泄露。它与 Cacheable（缓存区域）有本质区别。
- **OCP 满足**：新增 ID 策略只需 `IdPolicy` 新增工厂方法，Entity 基类不需要改动。相比当前方案之前几种方案（final id 排除了真实场景，标记接口改 if-else 链），这是唯一满足 OCP 的方案。
- **单构造路径**：所有子类通过 `super(IdPolicy)` 构造，不提供 `super(ID)` 替代路径。三种行为统一入口。
- **组合优于继承**：Entity 不覆写 ID 行为方法（no hooks），由策略对象决策。Entity 基类代码量最小化。
- **覆盖业界主流场景**：MyBatis-Plus 的 `AUTO` / `ASSIGN_ID` / `INPUT`、JPA 的 `IDENTITY` / `SEQUENCE` / `UUID` / 手动赋值，均在三种策略覆盖范围内。
- **类型安全**：`IdPolicy` 泛型绑定 `ID extends Identifier<?>`，Complie-time 确保 ID 类型与策略一致。

**Consequences**:

| Positive | Negative |
|---|---|
| 覆盖三种真实 ID 行为，不留盲区 | Entity 新增 `policy` 字段，多一个引用（4-8 字节） |
| 新增策略 Entity 不修改，OCP 满足 | `deferred()` 需要 unchecked cast（Java 泛型擦除） |
| `assignId()` 不再泛用——`generated` / `manual` 子类调用即抛异常 | 约定要求开发者在 `Aggregate` 子类使用 `ID.AUTO` 常量而非手写 `IdPolicy.generated(fn)`，减少匿名类分配 |
| 不存在"ID 突变"问题（transient Entity 的 equals/hashCode 不可调用） | — |

**Considered alternatives**:

| 方案 | 放弃原因 |
|---|---|
| `final ID` + 单构造 | DB 自增 / ASSIGN_ID 场景无法支持 |
| 非 final id + 两个构造 `Entity()` / `Entity(ID)` | 行为由构造器隐式表达，缺乏显式契约；`assignId()` 无法区分"手动注入不允许 fill"和"服务端生成允许 fill" |
| 标记接口（`ClientGeneratedId` / `ServerAssignedId`）| 新增策略需修改 Entity 基类 if-else 链，违反 OCP |
| 只在 `Identifier` 类型上声明策略 | ID DP 是值对象（record），不应对生命周期策略负责；策略与 Entity 相关，不是 ID 的属性 |

**Post-implementation note**: `IdPolicy` 方案最终未采用。代码使用更简洁的三构造器方案（`Entity(ID)`/`Entity(Supplier)`/`Entity()`）+ `public final void assignId()`（已存在则忽略）。核心区别：策略由构造器重载隐式表达而非组合对象，代码量更少；`assignId` 不再抛异常，public 可被 AppService 调用。
