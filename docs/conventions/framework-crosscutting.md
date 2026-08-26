---
type: Convention
title: Framework 编排/异常/Logging/数据库/Code Style
description: 查编排/异常/JSpecify、Logging、数据库或 Code Style 约定时读——soda-component 跨切面约定；完整定义归代码 javadoc 与专项 ADR。
tags: [ convention, framework, crosscutting ]
status: stable
---

本文承载「跨切面约定」：编排（DomainService / AppService）、异常、Logging、数据库开发、Code Style。 **先按下方目录定位到关注点**
——文件 200 行，章节彼此独立无共享概念。

| 关注点                             | 章节    | 内容                                               |
|------------------------------------|---------|----------------------------------------------------|
| 编排（DomainService / AppService） | §5 上半 | 跨聚合编排规则、AppService 编排前置、终态守卫      |
| 异常 + JSpecify + 参数契约         | §5 下半 | 异常类使用约定、nullness 注解、AppService 入参约定 |
| Logging                            | §6      | web / application 入口日志                         |
| 数据库开发                         | §7      | 一行指针 → ADR-0022                                |
| Code Style                         | §8      | Import 规范（通配符禁用）                          |

## 5. 编排与异常约定

领域服务、应用服务编排、异常约定与 nullness 规范。

### DomainService

领域服务。承接 **单个聚合根无法表达**的领域编排——跨聚合的操作与 **用例流程的业务逻辑**（多个聚合的动作序列、外部副作用、维护不变量）。

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

**示例**（换绑验证）：`CredentialChangeDomainService` 只承载跨聚合编排 `changeMobile`/`changeEmail`（verify →
user.changeXxx → use）；验证码发起（UCC）由 `UserAuthService.requestChangeMobileCode`/`requestChangeEmailCode`
编排（前置查询拦截 → `UserVerificationFactory.newCredentialChangeVerification` 构造 INITIALIZED 聚合、注册
`VerificationCreatedEvent`，见 ADR-0026 Verification 双概念模型），物理发送由投递侧监听器在事务提交后按 recipient 分派（见
ADR-0011 验证是独立于用户聚合的可复用领域概念/ADR-0026 Verification 双概念模型）。AppService 负责查询前置、加载与 save 顺序（先
user 后 verification）。

### ApplicationService 编排规范

**AppService 只做编排**：加载主体聚合 → 执行用例流程 → 持久化（save）。多聚合交互（同时更改多个聚合）的流程逻辑封装进
`XxxDomainService`；单聚合创建流程（构造 + 发送副作用）可在 AppService 内展开，不在本层展开跨聚合编排。

**可空性处理（Optional 优先）**：规则单一权威见 [STYLEGUIDE §3.2](../../STYLEGUIDE.md#32-jspecify-空性标注)
，本节仅指不述。AppService 消费侧补充：`Optional` 链上构造可空值用 `Optional.ofNullable(...).map(...).orElse(null)`，条件更新（如
`changeNickname`）用 `.map(...).ifPresent(...)`——这一段不在 STYLEGUIDE。

每个 AppService 有一个 **主体聚合**（该用例主要操作的对象，通常是其同名聚合根）。编排规则：

1. **主体聚合的 action 方法可直接在 AppService 中调用**（加载主体 → 调 action → save）
2. **外部 domain（其他聚合）的 action 方法禁止在 AppService 中直接调用**（get/读取除外）
3. 若用例不修改外部 domain：外部聚合作为主体聚合 action 方法的 **参数**传入，逻辑封装进主体聚合内部
4. 若用例 **修改**外部 domain（调用其 action 即修改其状态）：抽取 `XxxDomainService`，把编排封装进领域服务
5. **流程副作用**（发送验证码/通知等）随流程所在层执行：发送类副作用在 **投递侧**执行（ **投递时机在事务提交后**）——创建工厂注册
   `VerificationCreatedEvent`，AppService 持久化后发布，投递侧监听器（`@TransactionalEventListener(AFTER_COMMIT)`）按
   recipient 类型分派 `SmsSender`/`EmailSender` 并 `markSent` 落库（DB 事务不跨外部投递通道持有；sender 契约保证投递成功才返回，见
   ADR-0011 验证是独立于用户聚合的可复用领域概念/ADR-0026 Verification 双概念模型）；跨聚合流程的副作用随流程进入
   `XxxDomainService`（sender 类 gateway 允许注入领域服务）
6. **外部聚合的工厂构造**（`createBuilder()...build()`）不属于 action，可在 AppService 内与流程副作用一起展开（UCC
   完整示例见 [ADR-0026](../adr/0026-verification-source-recipient.md)（双概念模型））。

**终态守卫（Stateful）**：加载后经 `AbstractAppService.requireNotTerminal(id)` 统一守卫——终态实体禁止一切写抛 IAE；持久层兜底为网关
save 内行终态判定裸抛的 ISE（防御编程不携消息）。用例级状态前置（如 CC
发码要求启用态）仍为用例业务断言，不进通用守卫。Stateful / StateEnumType 契约单一源在两者 javadoc（终态判定收在枚举
`terminal()`），新增状态机聚合零额外样板；现行实现方：`User`（R 终态）、`Verification`（U 终态）。

典型反例：AppService 直接 `verification.verify(code)` 再 `user.changeMobile(...)`——verify 修改外部聚合状态，必须经
`CredentialChangeDomainService`。

### ApplicationService

应用层编排服务。每个聚合根一个 `XxxService`（接口）+ `XxxServiceImpl`（实现）。

**接口 → `soda-xxx-api` 模块**（公开契约）， **实现 → `soda-xxx-application` 模块**（`@Service` + 构造器注入）。

Controller 只注入接口，不感知实现：

```java

@RestController
public class UserController {
    private final UserService userService;  // ← 接口在 api 模块

    public UserController(UserService userService) { ...}
}
```

**Adapter 与 Application 的边界**：`adapter` 的 `build.gradle` 声明 `implementation project(':soda-user-application')`
（运行时 classpath），但 ModulithTest 强制 adapter 代码只引用 `api` 模块的类，不得 `import` application 模块的任何类。依赖方向为
`adapter → api (编译) + application (运行时)`。

### 模块依赖与 build.gradle 声明规则

**api → domain**：当前 `soda-xxx-api` 只直接声明 `soda-component-domain-types`（契约使用其枚举，如 Sex）， **不**声明
`domain` 模块；`soda-component-api-starter` 也 **不**依赖 `domain-starter`（api 层框架与 domain 框架解耦）。

**声明规则**：

1. 能经传递依赖到达的一律不声明（`api` 依赖向下游传播 compile + runtime，`implementation` 只传播 runtime）
2. 业务模块（api / application / adapter / infrastructure）：不需要传递给下一级的声明为 `implementation`，需要传递的才用
   `api`
3. starter 模块：依赖尽量声明为 `api`（承担为整层提供能力的职责），除非明确不需要传递给依赖方
4. 各层 Spring 能力由对应 starter 提供（如 application 层的 spring-tx 经 application-starter，web 通道的校验能力经
   adapter-starter-web）；业务模块不重复声明。例外：consumer 的 `@TransactionalEventListener` 所需 spring-tx
   由业务模块自声明（consumer 通道为空壳，见 ADR-0019 Adapter 组件按入站通道拆分）
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

| 子包         | 类后缀        | 职责                                                     | 触发条件                           |
|--------------|---------------|----------------------------------------------------------|------------------------------------|
| `service/`   | `ServiceImpl` | ApplicationService 实现，编排 domain gateway + event bus | 必有 — 每个聚合根一个              |
| `command/`   | `Processor`   | 写操作执行器，处理 `api/command/` 的 Command             | ServiceImpl 方法 > 10 或复杂编排时 |
| `query/`     | `Processor`   | 读操作执行器，处理 `api/query/` 的 Query                 | 读操作需要跨聚合编排时             |
| `event/`     | `Handler`     | 领域事件处理器，响应 `domain/event/` 的 DomainEvent      | 有领域事件需要订阅时               |
| `factory/`   | `Factory`     | 复杂聚合根创建器，Command → Entity                       | 创建涉及 DI 或跨聚合引用时         |
| `convertor/` | `Convertor`   | 双向转换 domain Entity ↔ api DTO                         | 映射逻辑复杂到影响可读性时         |

**分拆/合并规则**：一个聚合根一个 Service，方法数不超过 10 个。当方法超过 10 个或出现复杂编排时，从 `service/` 的
ServiceImpl 按 Command 拆出 `command/*Processor`（COLA 风格），但对外接口保持一个。

> 例外（既有实践）：按关注点拆分服务——`UserService`（身份 CRUD）与 `UserAuthService`（凭证变更）同属 User 聚合根的双服务先例。协助方聚合（如
> `Verification`）无独立 AppService——发码用例按主体归属用户侧服务（UCC 在 `UserAuthService`，见 ADR-0026 Verification
> 双概念模型）。

**Entity 创建**：使用 `XxxEntity.createBuilder()` / `builder()` 双 Builder 模式。当构建逻辑涉及跨聚合引用或需要依赖注入时，引入
`factory/*Factory`，但当前 Builder 模式已足够。

**Command 定义**：Java `record` + `@JsonProperty`，无需继承基类。可空属性标注 JSpecify `@Nullable`，未标注则默认非空（见「参数契约」）。

### Exception（异常类约定）

写侧（ApplicationService / DomainService / Entity）异常约定： **构造器校验（DP 式）+ 方法零守卫**（详见 ADR-0015
异常类使用约定：构造器校验与方法零守卫）：

| 类别                                                                   | 机制                                                                                                                     | 异常                |
|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|---------------------|
| 业务参数校验（输入值不合法 / 业务规则拒绝 / 状态机前置，客户端可预期） | `Assert.isTrue` / `Assert.notNull`                                                                                       | IAE（带消息）       |
| 构造器参数校验（创建与恢复路径统一）                                   | `ValidateUtils.notNull`（判空角色与固定消息见 [STYLEGUIDE §2.3](../../STYLEGUIDE.md#23-空值校验角色)）                   | IAE                 |
| 方法参数 null 契约违反                                                 | 无守卫 — jspecify `@NullMarked` 契约 + 调用方遵守                                                                        | NPE                 |
| 聚合内部结构不变量                                                     | 类型化（构造器必填字段，如 `User.passwordAccount`）+ JSON schema（全部非空字段 `required = true`，可空字段 `@Nullable`） | 不可表示 / 边界拒绝 |

判定原则（检查对象）：检查「参数值 / 业务状态是否允许操作」→ IAE 校验（客户端可预期的一切：错码、过期、重复用户名、未请求验证码、状态前置）；「值是否为
null」→ 构造器拦截（ValidateUtils），方法不检查（契约）。

规则：

- 消息策略：IAE
  带消息（客户端唯一反馈通道）；构造器校验的固定消息约定单一权威在 [STYLEGUIDE §2.3](../../STYLEGUIDE.md#23-空值校验角色)
  ，本节不重述。例外：防御编程守卫（调用方按契约调用、兜底拦截——网关内裸抛 ISE / `Objects.requireNonNull` NPE / `orElseThrow`
  NSE / 不支持的操作 `UnsupportedOperationException`
  ）不携消息，非客户端反馈通道，契约违反即调用方 bug，异常类型 + 栈帧即诊断
- 操作语义：set-state（`disable` / `enable`）幂等 no-op、不发事件；transition（`verify` / `use`）业务状态前置失败抛带消息 IAE；
  `changeMobile` / `changeEmail` 同值换绑抛 IAE（产品决策，见 ADR-0015 异常类使用约定：构造器校验与方法零守卫）
- requireXXX 模式只在 appservice：网关加载后 null 校验 → IAE（User not found / No pending）；可空查找返回 `Optional`
- `Objects.requireNonNullElse` 仅用于默认值模式（如 `User` 构造器 accounts 缺省），不属于守卫
- AppService 的 `Assert` 只用于网关加载结果的存在性 / 状态前置检查，不做 Command 属性级校验（见「参数契约」）；Adapter 层不做
  Assert（协议边界由 `@Valid` 负责）；Infrastructure 用 `Optional` 表达可空，正常流程不抛异常
- HTTP 映射：IAE → 400 `INVALID_ARGUMENT`，NPE / NoSuchElementException → 500 `INTERNAL`，
  `MethodArgumentNotValidException` → 400

### JSpecify（nullness 注解规范）

规则单一权威侧在根 [STYLEGUIDE](../../STYLEGUIDE.md) §3.2（AGENTS 注解/JSpecify
触发词路由彼处），本节仅指不述。写侧异常与守卫配套约定见上文「Exception（异常类约定）」，决策路线见 ADR-0015
异常类使用约定：构造器校验与方法零守卫。

### 参数契约

AppService 实现不校验入参（存在性 / 格式 / 业务约束），调用方必须保证参数合法。 方法入参默认非空；仅在可为 {@code null} 时标注
{@code @Nullable}（JSpecify，api 包已 {@code @NullMarked}）。 Command 对象属性遵循同一约定：未标注 {@code @Nullable} 的属性默认非空。
实现中不得对 Command 及其属性做防御性 null 检查。

## 6. Logging（入口日志）

- **web 入口**：controller 打印方法名 + request（`log.info("方法名: request={}", request)`），现行格式
- **application 入口**：AppService 打印 command（`log.info("方法名: command={}", command)`）——两层层级不同，各自打印一次，不互相替代，也不出现第三处重复

## 7. 数据库开发约定

数据源为 H2 内存库（`jdbc:h2:mem:…;MODE=MySQL`，见 soda-user-start `application.yml`）， **不接真实 MySQL**；schema 由 Flyway
执行 `db/migration/` 下 SQL 创建。 开发阶段约定（单 V1 / 严禁外键 /
严禁存储过程等）见 [ADR-0022](../adr/0022-dev-database-management.md)——本节不重述。

## 8. Code Style

### Import conventions

禁止通配符导入（`import com.soda.xxx.*`）。所有导入必须显式声明到具体的 class/interface。 IDE 中对应的设置：
`Preferences → Editor → Code Style → Java → Imports → "Class count to use import with '*'" → 999`。
