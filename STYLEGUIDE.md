---
type: Convention
title: Soda 编码规范
description: 写 Java / 加注解 / 写注释 / 改依赖前读——编码与注释的仓库单一标准
tags: [convention, styleguide, code]
status: stable
---

# 编码规范

> 编码风格与注释的仓库单源；框架层有什么归 `docs/framework-conventions.md`（类型契约见
> conventions/framework-type-contracts.md，跨切面见 conventions/framework-crosscutting.md）。测试规范见
> `docs/test-conventions.md`；DP 必测分组见 `docs/conventions/dp-test-conventions.md`。

## 1. 语言与术语

- 代码注释与文档用中文（项目裁定）；术语以 `CONTEXT.md` 为 **单源**——保留英文原词、对齐其词表，本文件不重定义
- 异常消息用英文

## 2. 代码风格

### 2.1 JDK 特性优先

优先使用 JDK 最新特性，不重复造轮子：

| 场景                    | 优先使用                                                                                                          | 禁止                                                         |
|-------------------------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| 不可变值对象（DP、DTO） | `record`（JDK 16+）                                                                                               | 手写 class + `@EqualsAndHashCode` + `toString()`             |
| 封闭集合建模            | `sealed` + `permits`，switch 分派依赖编译期穷举                                                                   | sealed 层次的 switch 写 `default` 兜底；把开放继承当封闭集用 |
| 空值校验 + 值归一化     | record 紧凑构造                                                                                                   | 在工厂方法中校验                                             |
| 工厂方法（DP）          | `of(T)`（参数即底层规范值）、`from`/`fromXxx`（跨类型转换）；`parse(String)` 仅当 `of`/构造器参数非 String 时出现 | 禁止 `valueOf(Object)`                                       |
| 局部变量类型            | `var`（JDK 10+）                                                                                                  | 显式冗长类型                                                 |
| DP 序列化               | `Serializable` + `@Serial`（仅在明确需要时）                                                                      | 无 `implements Serializable` 时留 `@Serial`                  |

`var` 仅限局部变量——字段、方法参数、返回值不用。

### 2.2 校验参数顺序

`ValidateUtils` 校验方法参数遵循 Spring `Assert` 风格：被校验值在前，辅助参数在后（`minValue(value, 0, false)`）。辅助参数视为可信，仅被校验值允许 `@Nullable`。

### 2.3 空值校验角色

| 角色 | 判空工具 | 消息 |
|---|---|---|
| DP 类 | `ValidateUtils.notNull(value)` | 固定默认消息 |
| Entity 类 | `Assert.notNull(value, "field must not be null")` | 每处自解释 |
| 工具类 | `ValidateUtils.notNull(value)` | 固定默认消息 |

Entity 承载业务状态，判空应明确哪个字段为 null；DP 类型简单，默认消息足够。

### 2.4 控制流

- `if` / `for` / `while` 必须带 `{}`，禁止省略单行体
- 非必要不嵌套；优先卫语句（guard clause）快速失败；避免 `else` / `else if`
- 禁止显式类型转换，用泛型 / `Comparable<Self>` / 模式匹配消除强转

### 2.5 类内成员访问（`this`）

「写显式、读裸、调用裸」：赋值与集合增删必须 `this.`；字段读取、本类方法调用、委托协作调用禁止 `this.`。

### 2.6 异常

优先 `IllegalArgumentException` / `IllegalStateException`（不使用已废弃异常）；不在非异常路径上构造异常；工具类内联 IAE
消息，不引入消息工厂类。 **需要被 HTTP 状态码细分的业务拒绝**走 `ProblemDetailException` 族（静态工厂按场景创建，见
ADR-0015），其余拒绝维持 IAE / ISE。

### 2.7 成员顺序

**静态初始化依赖优先于可见性排序**：`static final` 常量若在构造/初始化表达式中引用另一静态字段，被引用者必须先声明——静态初始化按文本顺序执行，倒序即
`NullPointerException` → `ExceptionInInitializerError`（类首次使用即崩，编译期不报）。 **禁止无功能理由的成员重排**
：提交中的成员移位必须由本规范条款或可观察行为驱动，不得混入顺手的「整理」——无规则的排序审美正是上条事故的成因。

## 3. 注解

### 3.1 Jackson

| 用途 | 注解 | 位置 |
|---|---|---|
| 序列化 | `@JsonValue` | record 组件（`@JsonValue long value`）或访问器方法 |
| 反序列化 | 无需（Jackson 3 原生推断 record 典范构造器，模式总表见 dp-conventions §5.1） | — |

Entity JSON 契约见 conventions/framework-type-contracts.md「Entity」条目；框架契约型注解（`@JsonTypeInfo`/`@JsonAutoDetect`
等）随各自契约文档，不入本章。

### 3.2 JSpecify 空性标注

项目使用 JSpecify 1.0（Spring 7 自身即 `@NullMarked`），覆盖全部 4 个注解；编译期明确空性，杜绝 NPE。本节是空性标注规则的
**单源**——框架层文档只指不述。

| 注解 | 语义 | 本项目使用 |
|---|---|---|
| `@NullMarked` | 作用域内默认 `@NonNull` | ✅ 必须 — 仅包级（`package-info.java` 第一行） |
| `@Nullable` | 该类型可含 null | ✅ 按场景（见下） |
| `@NonNull` | 排除 null | ⚠️ 仅非空投影，禁止冗余标注 |
| `@NullUnmarked` | 退出 `@NullMarked` | ❌ 禁止 |

**标注位置**：字段类型、参数、返回、类型参数（`List<@Nullable T>`）、record 组件。 **必须加 `@Nullable`**：实体的可选字段（getter
返回 `Optional<T>`）、瞬态字段（`private @Nullable ID id`——创建路径为 null，由 `isIdentified()`
窄化守卫）、工厂/构造器中对应可选字段的参数、立即校验拒绝
null 的工具参数。 **禁止加 `@Nullable`**：public/protected 方法返回值（用 `Optional<T>` 或空集合；项目约定返回值永不为
null）。 **`getId()` 例外**：`Identifiable.getId()` 返回 `@NonNull`，未标识时抛 `NullPointerException`（防御编程：调用方
bug，异常类型即语义）；瞬态语义由 `isIdentified()` 表达。 **`assignId` 参数守卫**：`Entity.assignId` 用
`Objects.requireNonNull` 抛 NPE（基础设施回调，DB 生成 ID 失败 = 系统 bug，非业务决策分支）；同值重复回填幂等，异值回填抛裸
`IllegalStateException`（两个持久化身份赋给同一实体 = 基础设施 bug）。 **不返回
`Optional<@Nullable T>`**——Optional 本身表达可空返回。
**局部变量**：根类型不标注，由赋值推断。
**外部库互操作**：未标 `@NullMarked` 的库返回值是 unspecified nullness——调用点显式检查或 `Optional.ofNullable()` 包装。

### 3.3 Lombok

| 注解 | 使用场景 |
|---|---|
| `@Getter` | 字段 getter；Entity 基类不适用（需 final 语义） |
| `@Accessors(fluent = true)` | 访问器名 `value()` 对齐 record 风格 |
| `@RequiredArgsConstructor` | 基类构造函数 |
| `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | 非 record 的 class DP，配合 `@Include` 显式标记参与字段 |
| `@UtilityClass` | 不用于有 `import static` 交叉引用的工具类（编译期符号解析先于 Lombok）——保持显式 `final class` + 私有构造器 |
| `@Builder` | 仅限 Entity/Aggregate 双 Builder（恢复构造器 + `createBuilder()`）；值对象一律静态工厂 `of`/`from`/`parse` |
| `@Setter` / `@NoArgsConstructor(access = PUBLIC)` | 仅限持久化 PO（infrastructure 载体）；domain / api / application 禁止 |
| `@Slf4j` | 日志允许 |
| 其余（`@Value` / `@With` / `@Data` / `@SuperBuilder` / `@AllArgsConstructor`） | 禁用——record 与显式构造器已覆盖其场景 |

### 3.4 MapStruct

| 规则       | 内容                                                                                                                                                                                                                                    |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 适用边界   | 仅 adapter 协议转换（Request → Command、DTO → Response）；infrastructure PO ↔ 领域 convertor 手写——final 类 + 全量构造含显式 null 清空列语义（见 conventions/framework-type-contracts.md「Convertor（基础设施）」节），不采用 MapStruct |
| 组件模型   | `@Mapper(componentModel = "spring")` 固定——生产注入生成 bean；`Mappers.getMapper(...)` 仅测试                                                                                                                                           |
| 映射策略   | by-name 优先；仅名称不一致时写显式 `@Mapping`                                                                                                                                                                                           |
| 未映射目标 | `unmappedTargetPolicy = ERROR`——协议字段漏映射编译期拦截；有意不映射须显式 `ignore = true` 自证                                                                                                                                         |

转换器契约语义（命名、方向、使用约定）归 `docs/conventions/adapter.md` WebAssembler 条目。

## 4. 注释规范

适用面：一切随代码提交的解释性文本——Java Javadoc 与行注释、Flyway 迁移脚本的 DDL COMMENT、配置文件注释。

依据：Google Java Style §7（格式/摘要/块标签/两例外/TODO）+ Developer Style（主动语态/现在时/简洁）+ agent 意图型注释实践。

执行：code-review 以本文件为核查基准——§4.5 no-op 是语义判据，人工判定；机械格式项（块标签顺序、空描述、装饰边框）可加
checkstyle 规则，漂移真实出现时再评估启用。

### 4.1 必须写 Javadoc

public/protected 的类、接口、方法、构造器、字段（Google §7.3）。本项目落点：

| 层级 | 要求 |
|---|---|
| domain：聚合根 / 子实体 / 领域服务 / 事件 | 必写（类级 + 业务方法） |
| domain：DP / Identifier / 枚举 | 必写（类级一行契约 + 非平凡方法） |
| api：接口 / Command / DTO | 必写 |
| application：AppService | 必写（类级一行职责 + 非平凡编排方法） |
| adapter：Controller 端点 / Request / Response | 必写（端点标注业务语义） |
| infrastructure：实现类 / 转换器 | 覆写例外为主（4.4），可省略 |
| query-server | 类级一行；查询方法签名自明即省略（读侧混装，从简） |
| private / 包私有 | 不强制；承载意图的必写 |

### 4.2 摘要片段

第一句是**摘要片段**：陈述句、句号收尾、描述行为（Google §7.1.2 / §7.2）。禁止 `@return` 引用式：

```java
// ❌
/** @return 客户 ID */
// ✅
/** 返回客户 ID。 */
```

摘要与后续描述之间空一行；新段落用空行或显式 `<p>`。

### 4.3 块标签

顺序固定：`@param` → `@return` → `@throws` → `@deprecated`（Google §7.1.3）。**不允许空描述**；续行相对 `@` 缩进 ≥ 4 空格。

### 4.4 例外

- **自解释方法**（§7.3.1）：签名自明、无更多值得说明的内容 → 省略。`getXxx()` 访问器、纯状态谓词（`isPending()`）通常属于此类。
- **覆写**（§7.3.2）：`@Override` 方法不重复父类 Javadoc；确有补充契约才写。

### 4.5 内容原则：写意图与契约，不写行为复述

注释是**意图载体**，不是行为记录。每句注释过 **no-op 测试**：删掉它，读者是否失去信息？——失去则留，否则删整句（WfA：no-op 删整句）。

| 写                                                                 | 不写                                                                                |
|--------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| 不变量、前置/后置条件、吸收态语义                                  | 复述方法名的单句（"修改用户名。"）                                                  |
| 规则**为什么**存在（最终态理由）                                   | 逐步描述实现过程                                                                    |
| CONTEXT.md 术语原词、「见 ADR-NNNN」锚点（编号即定位、标题即命名） | 复述 CONTEXT.md 词条定义；inline 展开 ADR 决策内容                                  |
| 签名之外的契约（可空、时钟注入、副作用、事务边界）                 | 显而易见的直译                                                                      |
| 反直觉语义（如 `isVerified()` 含已使用态）                         | 环境可见事实（序列化细节、框架声明）                                                |
| —                                                                  | **决策历史**：日期、变更沿革、"原为…现为…"、被否方案——演进叙事归 git 历史，不归注释 |

### 4.6 格式

- Javadoc 块 `/** */`，内部行以 `*` 开头与起始对齐；无块标签且单行可容纳时用 `/** … */`
- 禁止装饰性星号边框（盒子注释）
- 行注释 `//`，与所注释代码同级缩进

### 4.7 TODO

`TODO: <链接> - <说明串>`。前缀保留英文大写（工具可识别），说明用中文。仅标记**临时的、不完美的代码**——不是清晰代码或透彻理解的替代品（Google §7.4）。

```java
// TODO: https://gitee.com/zhijiantianya/yudao-cloud/issues/123 - RG 场景发码接入后移除 fail-fast
```

## 5. 依赖管理

- `implementation`：运行时需要的依赖（Jackson 注解、spring-modulith 等）
- `compileOnly`：仅编译期需要（Lombok）
- 基础模块不用 `api` 暴露传递依赖，除非被依赖的是核心框架能力
- `testImplementation`：测试需要的依赖
