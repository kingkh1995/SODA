---
type: Convention
title: JSpecify 使用规范
description: 写/评审空性标注（`@NullMarked` 基线、`@Nullable` 位置契约、泛型与覆写、外部库边界）时读——P3C 三级 + ❌/✅ + 双清单；版本基线与禁用清单见 §1。
tags: [ jspecify, convention ]
status: stable
---

# JSpecify 使用规范

> **本篇**＝空性标注的定案与落地：§1 版本基线 → §2–§5 规则章 → §6 偏离表。 **官方事实全量与版本锚点溯源**归
> [research/foundation-deps-official-practices.md § JSpecify](../research/foundation-deps-official-practices.md#jspecify)
> ——本篇不设来源章，来源列只写
> 一级链接。
> **关联规范**：[framework-crosscutting](framework-crosscutting.md) §5「参数契约」（AppService
> 入参契约，本篇只指不述）与「Exception（异常类约定）」表的
> 「方法参数 null 契约违反」行；[framework-type-contracts](framework-type-contracts.md)「Entity / Identifiable /
> AbstractPersistable」（`getId()` /
> `assignId` / 持久化载体的类型契约）；[dp-json-conventions](dp-json-conventions.md)（DP 属性与序列化链路）；根
> [STYLEGUIDE](../../STYLEGUIDE.md#23-空值校验角色)（判空角色：`ValidateUtils` / `Assert` 分工的单源）。

## 适用范围与管辖面

管 `src/main` 的全部空性标注：`@NullMarked` 基线、`@Nullable` 落位、泛型签名、覆写契约、外部库边界，以及其它 nullness 注解族的迁移。

何时 **不**读：

- 写 `src/test` / `src/testFixtures` —— 测试侧空性用法归 [test-conventions](../test-conventions.md)，本篇不描述（实测测试侧
  `@NullMarked` /
  `@Nullable` / `@NonNull` 命中各 0、`package-info.java` 0 份，测试整体处于 null-unmarked）
- 判「AppService 入参要不要校验」「Command 属性默认是否非空」 —— [framework-crosscutting](framework-crosscutting.md)
  §5「参数契约」（应用层契约）
- 选判空工具与消息策略 —— 根 [STYLEGUIDE](../../STYLEGUIDE.md#23-空值校验角色)
- 查 `getId()` / `assignId` / PO 基类的类型契约 —— [framework-type-contracts](framework-type-contracts.md)
- 要官方规则原文、过时对照、工具链选项（Kotlin 互操作 / NullAway / module
  级标注） —— [research 篇](../research/foundation-deps-official-practices.md#jspecify)

## 写清单

新增或修改 `src/main` 代码时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                                |
|----|-----------------------------------------------------------------------------------------------------------------------------|
| 1  | 新建包先落 `package-info.java` + `@NullMarked`（与 `package` 声明同段；子包不继承父包，漏建即整包落入 unspecified）（§2.1） |
| 2  | 只给真正可为 null 的位置写 `@Nullable`；`@NullMarked` 作用域内不写即非空（§2/§3）                                           |
| 3  | `@Nullable` 紧贴类型、置于注解串末位（`@JsonProperty("mobile") @Nullable String mobile`），禁用声明位写法（§3.1）           |
| 4  | 可空**数据载体**（Command / Request / Response / DTO 及其访问器）用 `@Nullable T`，不返回 `Optional<T>`（§3.2）             |
| 5  | 领域 / 网关的「查询无结果」返回 `Optional<T>`，不 `return null`，也不写 `Optional<@Nullable T>`（§3.2）                     |
| 6  | 覆写父类型带 `@Nullable` 的位置逐位重复标注（返回位为准；形参与返回位同用时两处同改）（§4.2）                               |
| 7  | 需要可空类型实参时写 `<E extends @Nullable Object>` 或 `@Nullable E` projection（§4.1）                                     |
| 8  | 未 `@NullMarked` 的外部库返回值先显式检查或 `Optional.ofNullable(...)` 包装，再进领域（§4.3）                               |
| 9  | 非空保证由 `Objects.requireNonNull` / `Assert` / `ValidateUtils` 承担，注解不替代运行时校验（§4.3）                         |
| 10 | nullness 注解只 import `org.jspecify.annotations.*`；其它注解族零出现（§1.2/§5）                                            |
| 11 | 持久化 PO 的字段与 `getId()` 不标 nullness（§3.4/§6）                                                                       |
| 12 | §1：BOM / 直接依赖升级时逐项复核版本表与 1.2 清单，并确认 `spring-core` 仍传递 `org.jspecify:jspecify`                      |
| 13 | 完成判据：按文末检视清单逐条自检通过，本篇零跨文档重复，来源列均为一级 markdown 链接                                        |

## 1. 版本基线

### 1.1 版本

| 项                                    | 值                          | 依据                                                                                                                                                       |
|---------------------------------------|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JSpecify 构件 `org.jspecify:jspecify` | **1.0.1**                   | Spring Boot 4.1.1 BOM 钉 `jspecify.version=1.0.1`；上游 `spring-core-7.0.9.pom` 以 `compile` scope 声明 1.0.0                                              |
| JSpecify 规范文本                     | **1.0.0**                   | 规范页头部 `version 1.0.0`；1.0.1 release notes 仅文档与构建调整、**无语义变更**——两者并存不是版本冲突                                                     |
| 依赖解析路径                          | 传递（BOM → `spring-core`） | 21 个 `build.gradle`（18 个模块 + 3 聚合）的 `jspecify` 字面量命中 0；`org.jspecify` 被全部 18 个模块的 `src/main` 引用；构件进 fat jar 的 `BOOT-INF/lib/` |
| Spring Framework                      | **7.0.9**                   | Boot 4.1.1 解析的 `spring-framework.version`；框架自身已 `@NullMarked`，契约位字节码带 `org.jspecify.annotations.Nullable`                                 |
| Spring Data Commons                   | **4.1.1**                   | `Persistable.getId()` 字节码为 `METHOD_RETURN org.jspecify.annotations.Nullable`（见 §6 E3）                                                               |
| Jakarta Annotations                   | **3.0.0**                   | Boot 4.1.1 BOM；仅作 §1.2 禁用行的依据                                                                                                                     |
| Java                                  | **25**                      | 根 `build.gradle` toolchain `JavaLanguageVersion.of(25)`——type-use 注解字节码完整保留                                                                      |
| nullness 检查器                       | **未启用**                  | 21 个 `build.gradle` 的 `NullAway` / `errorprone` / `-XepOpt` 命中 0——本仓规则的强制力全落在人工检视                                                       |

### 1.2 禁用清单

| 本仓禁用项                                                                    | 替代                                | 依据                                                                                                                                                                                   | 影响面                            |
|-------------------------------------------------------------------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| `javax.annotation.Nullable` / `Nonnull`（JSR-305）                            | `org.jspecify.annotations.Nullable` | [JSR 305 官方状态 Dormant](https://jcp.org/en/jsr/detail?id=305)、[JSpecify 迁移指南](https://jspecify.dev/docs/using/)                                                                | 潜在                              |
| JSR-305 `@ParametersAreNonnullByDefault` / 自定义 `@TypeQualifierDefault`     | `@NullMarked`                       | [JSpecify using · 默认注解](https://jspecify.dev/docs/using/#defaulting-annotations)（官方明示**不等价**：对泛型实参与上界效果更完整）                                                 | 潜在                              |
| `jakarta.annotation.Nullable`（声明式，无 `@Target(TYPE_USE)`）               | `org.jspecify.annotations.Nullable` | [Jakarta Annotations 3.0 `@Nullable`](https://jakarta.ee/specifications/annotations/3.0/apidocs/jakarta.annotation/jakarta/annotation/nullable)（自述静态分析工具一般应视同无注解）    | 潜在（旧知识高发；迁移形态见 §5） |
| `org.jetbrains.annotations.Nullable` / `NotNull`                              | 同上                                | [JSpecify 成员组织](https://jspecify.dev/about/)、[IDEA 默认可空性注解清单](https://www.jetbrains.com/help/idea/annotating-source-code.html)（JetBrains 无官方迁移指南）               | 潜在                              |
| Spring `org.springframework.lang.Nullable` / `@NonNullApi` / `@NonNullFields` | JSpecify 四注解                     | [Spring Framework · Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)（7 起 deprecated，让位于 JSpecify；声明式 → type-use，数组与泛型语义需改写） | 潜在                              |

维护：BOM / 直接依赖升级时逐项复核本表与 1.2 清单（依据＝官方 release notes / 迁移指南），改动就地更新；并额外确认
`spring-core` 仍传递
`org.jspecify:jspecify`（§5 的传递边即本仓唯一依赖来源）。

## 2. 空性基线

`@NullMarked` 是全部规则的地基：作用域内未标注的类型 usage 一律读作 **非空**，`@Nullable` 是唯一的例外出口。

| 规则                                                                                                                                                   | 级别     | 官方来源                                                                                         | SODA 落地                                                                                      | 关联 ADR |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|----------|
| `@NullMarked` 语义：被标注元素的传递包含范围内，未标注的类型 usage 默认非空                                                                            | 【强制】 | [NullMarked Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/NullMarked.html)     | 51 份 `package-info.java` 全量 `@NullMarked`，覆盖 51 个含 `.java` 的包（§2.1 的不变式已成立） | —        |
| `@NullMarked` 可标注位置：module / package / class（含 interface、enum、record）/ 方法 / 构造器；字段、形参、局部变量、类型参数、record 组件**不可**标 | 【强制】 | [spec · 识别声明](https://jspecify.dev/docs/spec/#recognized-declaration)                        | 全仓仅用于 `package` 级；非 `package-info.java` 文件命中 0                                     | —        |
| 包级作用域写在 `package-info.java` 的 `package` 声明上；**包不继承**——`com.foo` 的标注不覆盖 `com.foo.bar`                                             | 【强制】 | [spec · null-marked 作用域](https://jspecify.dev/docs/spec/#null-marked-scope)                   | 每包一份 `package-info.java`（不变式见 §2.1）；子包漏建是本规则的高发失误                      | —        |
| 非 module path（classpath）下包级标注可能被多份 `package-info` 解释——官方要求「非常小心」                                                              | 【参考】 | [NullMarked Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/NullMarked.html)     | 51 个包名跨模块零重复、无 `module-info.java` ⇒ 保持「一包一模块」即可                          | —        |
| `@NullUnmarked` 仅用于渐进迁移；全量迁移完成后禁用是合理的                                                                                             | 【强制】 | [NullUnmarked Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/NullUnmarked.html) | 全仓 0 处；本仓明文禁用（§2.2）                                                                | —        |
| `@NonNull` 语义（排除 null）；null-marked 作用域内很少需要                                                                                             | 【强制】 | [NonNull Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/NonNull.html)           | 代码标注 0 处（仅 Javadoc 文本提及）⇒ 本仓不使用（§2.2）                                       | —        |

### 2.1 包不继承：每包一份 `package-info.java`

包级标注 **不**向下继承，是本节最容易误判的一条：父包 `@NullMarked` 不会让子包进入 null-marked 作用域。 **不变式**：任一含
`.java` 的 `src/main`
包都必须有 `package-info.java`，注解与 `package` 声明同段（`@NullMarked` 可与其后的 `@ApplicationModule` 并列，也可单独出现）。判定式——
**含 `.java` 的包数
== `package-info.java` 份数**（当前 51 == 51）；两个数字不等即说明有包整体落入 unspecified。

失效面恰在最需要空性信息的地方： **HTTP 入参 record 包**与 **JPA 载体包**。误判形态是把「写了 `@Nullable`」当成「包已在
null-marked 作用域内」——`@Nullable` 自身携带语义、
在哪个包都成立，但同包内 **其余**未标注 usage 会在缺 `package-info.java` 时整体降级为
unspecified，「默认非空」的保证静默消失（工具无检查器时完全无声）。

本规则的高发场景是 **既有模块根的子包首次新增时漏建** `package-info.java`。校准集（全为模块根的子包）：
`component/api/command`（2 类）、
`component/infrastructure/eventbus`（2）、`component/infrastructure/persistence`（4）、`user/consumer/message`（1）、
`user/job/param`（1）、`user/web/assembler`（1）、
`user/web/request`（8）、`user/web/response`（1）——这 8 包 20 类共同说明误判的样子：包内写着 14 处 `@Nullable`，注解可见，包本身却不在
null-marked 作用域里。子包补建的
形态固定为「 **只**加 `@NullMarked` + 中文包 Javadoc， **不带** `@ApplicationModule`」：`@ApplicationModule`
只属模块根包，子包带了会造出并不存在的模块边界；同类形态见
`user/domain/types/package-info.java`。

```java
// ❌ 只给父包建 package-info.java，子包靠「继承」拿到默认非空
//   package com.soda.user.web;            ← @NullMarked 只覆盖本包
//   package com.soda.user.web.request;    ← 无 package-info.java ⇒ 整包 unspecified，@Nullable 之外全失去非空保证

// ✅ 每个含 .java 的包各一份，注解与 package 声明同段
@NullMarked
package com.soda.user.web.request;
```

### 2.2 `@NonNull` 与 `@NullUnmarked` 不进入本仓

两条各自对应一个可判定的禁令： **不写 `@NonNull`**（含 non-null projection——本仓需要「可空的值」时一律用 `@Nullable X` 参数或
`@Nullable E` projection，不用「非空上界 + `@NonNull` 投影」的另一套写法）； **不写 `@NullUnmarked`**（51 个包已全量
`@NullMarked`，渐进迁移阶段已结束）。
作用域内确实要表达「排除 null」时，答案是 **什么都不写**——`@NullMarked` 已经给了它。

## 3. `@Nullable` 契约

`@Nullable` 是本仓唯一的可空表达。它标的是 **类型 usage**（不是「字段」「参数」这类声明），因此位置与写法都有清单。

| 规则                                                                                                                              | 级别     | 官方来源                                                                                                                                                                                           | SODA 落地                                                                           | 关联 ADR |
|-----------------------------------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|----------|
| `@Nullable` 语义：该类型 usage 把 `null` 视为合法取值（`@Target(TYPE_USE)`、`RUNTIME` retention）                                 | 【强制】 | [Nullable Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html)                                                                                                           | 86 处加权（去重 78 行）：record 组件 26 / 形参 44 / 字段 13 / 返回 3                | —        |
| 可标注位置清单：方法返回类型、方法 / 构造器形参、lambda 形参、字段、record 组件、类型参数上界、非通配类型实参、通配上界、数组组件 | 【强制】 | [spec · 识别 type-use](https://jspecify.dev/docs/spec/#recognized-type-use)                                                                                                                        | 86 处逐处落在清单内；清单外位置 0 处                                                | —        |
| 局部变量**根类型**不可标注（可空性由流分析推断）；类型实参与数组组件照常可标                                                      | 【强制】 | [Nullable Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html)                                                                                                           | 局部变量标注 0 处                                                                   | —        |
| 强转根类型、`instanceof` 各部分、primitive、`throws`、receiver 不可标注；需要「断言非空」时用 `Objects.requireNonNull`            | 【强制】 | [设计 FAQ · casts](https://jspecify.dev/docs/nullness-design-faq/#casts)                                                                                                                           | 上述位置 0 处；`Objects.requireNonNull` 落在 `Entity.getId()` 与 `Entity.assignId`  | —        |
| record 组件上的 type-use 注解向生成的字段 / 访问器 / 构造参数传播；显式重写这些成员时须再标注                                     | 【强制】 | [Nullable Javadoc](https://jspecify.dev/docs/api/org/jspecify/annotations/Nullable.html)                                                                                                           | 字节码实证传播成立；带 `@Nullable` 组件的 record 无一显式重写访问器                 | —        |
| record 的 `equals(Object)` 参数语义恒 nullable（`UNION_NULL`），null-marked 下**预期**显式标 `@Nullable`                          | 【强制】 | [spec · record equals](https://jspecify.dev/docs/spec/#expected-annotations-on-record-classes-equals-methods)                                                                                      | javac 生成成员无源码落点 ⇒ 偏离登记（§6 E1）                                        | —        |
| 可空返回按**角色二分**：数据载体用 `@Nullable T`，领域 / 网关的「查询无结果」用 `Optional<T>`                                     | 【强制】 | [JDK `Optional` API Note](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html)、[设计 FAQ](https://jspecify.dev/docs/nullness-design-faq/#billion-dollar-mistake) | 数据载体 `Optional` 返回 0 处；20 处 `Optional` 返回声明全为领域 / 网关语义（§3.2） | ADR-0041 |
| 书写风格：注解紧贴类型、与类型同行（`private @Nullable String f`）                                                                | 【参考】 | [Spring · Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)                                                                                                    | 声明位写法 0 处；注解串末位是本仓唯一形态（§3.1）                                   | —        |

### 3.1 位置白名单与书写形态

**白名单**＝规则表第 2 行的九个位置。 **黑名单**＝清单外一切位置（局部变量根类型、强转、`instanceof`、primitive、`throws`
、receiver），加两类写法： **声明位**（把 type-use 注解当成声明注解用）与 **注解串中段**（`@Nullable` 与类型之间夹了别的注解，语义仍对但读不出
type-use 本质）。

```java
// ❌ 声明位：读到的是「这个字段可空」，不是「这个类型 usage 可为 null」
@Nullable
private String mobile;

// ❌ 注解串中段：类型被推到行尾，type-use 边界看不出来
@Nullable
@JsonProperty("mobile")
@MobileNumber
String mobile;

// ✅ 紧贴类型、置于注解串末位
@JsonProperty("mobile")
@MobileNumber
@Nullable
String mobile;
```

### 3.2 可空返回的角色二分：`@Nullable T` 还是 `Optional`

按 **角色**选载体，不按个人偏好：

| 角色                                                                         | 载体          | 判据                                                                                                                                                                                 |
|------------------------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 数据载体（Command / Request / Response / DTO 等记录型 bean，含其属性访问器） | `@Nullable T` | 属性访问器是**取值直通**，不承担「有没有结果」的叙事；`Optional` 会在此破坏 Jackson / MapStruct 直通链路（两者都不把 `Optional` 当一等类型），并把「未携带」与「显式空」合并成同一态 |
| 领域 / 网关的「查询无结果」                                                  | `Optional<T>` | 返回值确实在讲「没有这个结果」，正是 JDK API Note 给 `Optional` 划定的用途                                                                                                           |

附带两条硬约束： **不返回 `Optional<@Nullable T>`**（`Optional` 自身即表达可空返回，双层可空无消费者）；`Optional` 变量本身不得为
`null`。

现状实测：数据载体 `Optional` 返回 0 处；20 处 `Optional` 返回声明全在领域 / 网关——`User` 的属性访问器组与 `findAccount`
（5）、`EntityGateway` / `UserGateway` /
`VerificationGateway` 的查询端口（5）、6 处 gateway 实现、4 处 Spring Data repository 查询；无一处落在 Command / Request /
Response / DTO。

```java
// ❌ 数据载体属性用 Optional 包装：Jackson / MapStruct 直通链路断裂，「未携带」与「显式空」不可分
public record UserResponse(Optional<String> nickname) {
}

// ✅ 数据载体用 @Nullable T
public record UserResponse(@JsonProperty("nickname") @Nullable String nickname) {
}

// ✅ 「查询无结果」才是 Optional 的位置（领域 / 网关）
Optional<User> findById(LongId id);
```

### 3.3 record 组件的传播

`@Nullable` 标在 record 组件上即可覆盖生成的字段、访问器与典范构造参数—— **不要**为了「显式」去重写访问器再标一遍（重写了才必须再标）。唯一无源码落点的生成成员是
`equals(Object)`：其参数在规范里语义恒 nullable 且预期显式标注，但 javac 生成的成员无法标注、手写 `equals` 是更大的坏事——登记为偏离（§6
E1）。

### 3.4 持久化载体（PO）的空性口径

PO 的字段按 **实例可空性**逐字段判定，与 DB 列约束一致：

- **DB 列可空**（`@Column(nullable = true)`，含 `@Column` 缺省）⇒ 字段标 `@Nullable`——瞬态构造下确为 null，不标即在
  `@NullMarked` 包内谎称非空。
- **DB 列非空**（`@Column(nullable = false)`）⇒ 不标——列约束保证已持久化行非空。
- **主键 `id` 字段不标**——`@GeneratedValue` 前为 null，故 `getId()` 不适用「未标识抛 NPE」条款（§6 E2）。

PO 是 JPA 反射载体：构造器可绕过、字段可被审计与 `@Version` 机制后置填充，「未标注即非空」在这里不是事实陈述——这正是可空列必须标注的原因。
与 `@NullMarked` 体系的已知缝隙登记在 §6 E2/E3，靠 `AbstractPersistable` 的 javadoc 说明。

## 4. 泛型 · 覆写 · 外部库

| 规则                                                                                                                        | 级别     | 官方来源                                                                                           | SODA 落地                                                                                                                                           | 关联 ADR |
|-----------------------------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 覆写**不继承** nullness：覆写方法须在父声明可空的位置自行重复标注                                                           | 【强制】 | [spec · augmented type of usage](https://jspecify.dev/docs/spec/#augmented-type-of-usage)          | 两处 Spring 契约覆写已按位标注：`IfMatchResolver.resolveArgument` 返回位、`HttpValidatorHeadersAdvice.beforeBodyWrite` 返回位 + `body` 形参（§4.2） | ADR-0039 |
| 泛型上界默认非空：`<E>` 即非空上界，接受可空类型实参必须写 `<E extends @Nullable Object>`                                   | 【推荐】 | [user guide · 声明泛型](https://jspecify.dev/docs/user-guide/#declaring-generics)                  | 需可空**值**处一律用 projection（`JobContext`、`ValidateUtils`）；可空上界 0 处且无受阻点（§4.1）                                                   | —        |
| 未标注类型变量具 parametric nullness（跟随实参）；`@Nullable E` 是 nullable projection，「`E` 本身非空也可能为 null」时使用 | 【推荐】 | [spec](https://jspecify.dev/docs/spec/)                                                            | 同上行；用法均为显式 projection                                                                                                                     | —        |
| 通配符与类型参数差异：`List<?>` 的隐含上界是 `@Nullable Object`，而 `List<? extends Object>` 元素非空                       | 【参考】 | [spec · 无界通配](https://jspecify.dev/docs/spec/#unbounded-wildcard)                              | 30 处 `<?>` 全为约束位 / 标记位通配（`Identifier<?>`、`AuthAccount<?>` 等）；容器型无界通配 0 处                                                    | —        |
| 外部库互操作：null-unmarked 的类型 usage 为 unspecified nullness                                                            | 【推荐】 | [spec · 多世界](https://jspecify.dev/docs/spec/#multiple-worlds)                                   | 外部栈已迁移（Spring Framework / Spring Data 的契约位字节码带 JSpecify 注解）；调用点显式检查齐备（§4.3）                                           | —        |
| 注解不产生运行时强制——不替代显式参数校验                                                                                    | 【强制】 | [JSpecify 注解包说明](https://jspecify.dev/docs/api/org/jspecify/annotations/package-summary.html) | 运行时校验齐备：`ValidateUtils` / `Assert` / `@Valid` 与注解并行（分工见 §4.3）                                                                     | ADR-0015 |

### 4.1 泛型签名

默认非空的上界是 **不写即得**；要接受 `null` 作为类型实参时才显式放宽。表达「值可为 null」优先用 projection（
`@Nullable T value`），只有整个泛型 API 需要可空实参时才动上界
（`<E extends @Nullable Object>`）。两者作用点不同，别互相替代。

### 4.2 覆写：本仓要求显式重复标注

父类型的 nullness 选择不会随覆写传递——不重复标注，覆写体在 null-marked 上下文里被读成非空，而实现可能 `return null` 或原样
`return body`。本仓口径按 **位**执行：

- 返回位：父声明 `@Nullable` 即必须标（`IfMatchResolver.resolveArgument`——实现确实返回 `null`）
- 形参位：与返回位同用时两处同改（`HttpValidatorHeadersAdvice.beforeBodyWrite`——`return body`
  直通父语义）；父声明的可空形参若实现内未使用，保持非空声明，不补标
- 收窄是另一个方向：把父声明的 `@Nullable` 收窄为非空是合法的更强契约（返回型协变），但只对「收窄后恒成立」的类型使用（§6 E3）

```java
// ❌ 覆写不重复标注：null-marked 上下文读作非空，方法体却 return null
@Override
public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                              NativeWebRequest request, WebDataBinderFactory binderFactory) {
    return null;   // 返回位缺 @Nullable
}

// ✅ 父声明可空的返回位重复标注（返回类型与修饰符同行，`@Nullable` 紧贴类型）
@Override
public @Nullable Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest request, WebDataBinderFactory binderFactory) {
    return null;
}
```

### 4.3 外部库边界与运行时校验分工

外部库分两种读法： **已迁移**的（Spring Framework 7 / Spring Data——其契约位带 JSpecify 注解，版本锚点见 §1，覆写按 §4.2 对齐）；
**未迁移**的（无 `@NullMarked`、无
type-use 注解）返回值是 unspecified—— **在调用点**显式检查或 `Optional.ofNullable(...)` 包装，不要假定非空、也不要假定可空。

注解与运行时校验各司其职：注解说明 **契约**（工具读，不是运行时行为），越界仍是 NPE 或 `NullPointerException` 语义；参数合法性该拦的拦——
构造器校验走 `ValidateUtils`（判空角色见 [STYLEGUIDE](../../STYLEGUIDE.md#23-空值校验角色)），方法级业务断言走 `Assert`
（异常类型与消息策略见
[framework-crosscutting](framework-crosscutting.md) §5「Exception」），协议边界走 `@Valid`。AppService 入参 **不做**防御性
null 检查（契约由调用方保证，见
[framework-crosscutting](framework-crosscutting.md) §5「参数契约」）。

## 5. 禁用与迁移

| 规则                                                                       | 级别     | 官方来源                                           | SODA 落地                                                                                                                       | 关联 ADR |
|----------------------------------------------------------------------------|----------|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|----------|
| 依赖声明：**不**用 `provided` / `optional` 隐藏 JSpecify，随运行时一起提供 | 【推荐】 | [JSpecify using](https://jspecify.dev/docs/using/) | 经 Boot BOM → `spring-core` 传递（非 `provided` / `optional`，fat jar 内含该构件）⇒ 不违反规则原文；不直接声明，升级复核句见 §1 | —        |

迁移动作（禁用族清单见 §1.2）：换 import 到 `org.jspecify.annotations.Nullable`，并按 type-use 语义 **重判**
原文位置——声明注解 → type-use 的映射不是等价替换：
数组元素与数组本身分开标（`@Nullable String[]` 与 `String @Nullable []` 含义不同）、泛型实参与上界随 `@NullMarked` 获得比
JSR-305 默认注解更完整的语义、覆写父类
`@Nullable` 时 **留空**即可（JSpecify 语义不继承，`@NonNull` 也不再需要用来撤销父类可空）。本仓把 `org.jspecify.annotations`
作为 **唯一** nullness 注解族：`jakarta.annotation.*`
在 `src/main` 零命中。遇到 `jakarta.annotation.Nullable` 时的改法固定为 **只换 import、使用端不动**——换完使注解与所在方法的
javadoc、`null` 分支、上游可空 accessor 三方一致
（这类 import 能编过通常是 `jakarta.annotation-api` 传递依赖的巧合；换掉后该隐性依赖即解除，而 `jakarta.annotation-api` 作为
Spring 的传递依赖本身仍在，属正常）。

```java
// ❌ jakarta 声明注解 + 声明位：@NullMarked 之下该形参仍读作非空，与「null = 未携带」的实现和 javadoc 矛盾

import jakarta.annotation.Nullable;

protected final T requireIfMatch(ID id, @Nullable IntLiteralType expectedVersion) { …}

// ✅ 唯一可空注解族：org.jspecify.annotations.Nullable（type-use）
import org.jspecify.annotations.Nullable;

protected final T requireIfMatch(ID id, @Nullable IntLiteralType expectedVersion) { …}
```

## 6. 偏离表

| 偏离项                                         | 官方做法                                                           | SODA 做法                                                                                        | 理由                                                                                                                                                                                                                                                 | 关联 ADR |
|------------------------------------------------|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| **E1** record 自动生成的 `equals(Object)` 参数 | 规范**预期**显式写 `@Nullable`（`UNION_NULL` 特例）                | 不标注（javac 生成，源码无落点）                                                                 | 语义不因缺标注而改变；手写 `equals` 才是更大的坏事。启用 nullness 检查器时须确认其 `UNION_NULL` 豁免生效                                                                                                                                             | —        |
| **E2** PO（持久化载体）主键字段的空性          | `@NullMarked` 下未标注 usage 视为非空                              | 主键 `id` 字段不标；可空 DB 列**标** `@Nullable`（§3.4）；`getId()` 不适用「未标识抛 NPE」条款   | PO 是 JPA 反射载体——构造器可绕过、字段可被审计 / `@Version` 后置填充，瞬态期确为 null。主键由 `@GeneratedValue` 生成故瞬态必 null，属框架载体与空性声明体系的已知缝隙（`AbstractPersistable.isNew()` 正依赖 `getId() == null`），靠基类 javadoc 记录 | ADR-0024 |
| **E3** PO 的 `getId()` 收窄覆写                | 覆写须重复父声明的 `@Nullable`（`Persistable.getId()` 父声明可空） | 覆写返回非空（Lombok 直通字段访问器）                                                            | 返回型协变收窄合法：持久化行的主键恒存在，收窄表达「已持久化」这一更强契约。与 E2 合看，问题只在瞬态路径                                                                                                                                             | ADR-0024 |
| **E4** 可空返回的载体形态                      | 官方不规定；JDK API Note 主张 `Optional` 仅用于「明确表示无结果」  | 按角色二分：数据载体用 `@Nullable T`（禁 `Optional`），领域 / 网关「查询无结果」用 `Optional<T>` | 记录型数据载体的属性访问器不应用 `Optional` 包装：属 JDK API Note 警示的误用，且会破坏 Jackson / MapStruct 直通链路、使「未携带」与「显式空」丧失区分（§3.2）                                                                                        | ADR-0041 |
| **E5** JSpecify 依赖声明                       | 官方推荐**不要**用 `provided` / `optional` 隐藏，随运行时提供      | 不直接声明，经 Boot BOM → `spring-core` 传递获得                                                 | 现状不是 `provided` / `optional`，运行时在 fat jar 内 ⇒ 不违反规则原文。风险：Spring 侧移除该传递边即 18 个模块的 `org.jspecify` import 断链——已登记，按 §1 维护句复核                                                                               | —        |

## 检视清单

code-review 标准轴按本清单逐条核对（每组逐条可判定，括号内为落点）：

### 代码

1. 任一含 `.java` 的 `src/main` 包缺 `package-info.java` ⇒ 失败（§2.1）
2. `package-info.java` 内 `@NullMarked` 与 `package` 声明同段；同段内无 `@NullUnmarked`（§2.1/§2.2）
3. 非 `package-info.java` 文件出现 `@NullMarked` 或 `@NullUnmarked` ⇒ 失败（§2.2）
4. `@NonNull` 零出现（Javadoc 文本提及不计）；无可空上界 + `@NonNull` 投影的另一套写法（§2.2）
5. `@Nullable` 只出现在九个白名单位置（返回 / 形参 / lambda 形参 / 字段 / record 组件 / 上界 / 实参 / 通配上界 /
   数组组件）（§3/§3.1）
6. `@Nullable` 未出现在黑名单位置（局部变量根类型、强转、`instanceof`、primitive、`throws`、receiver）⇒ 通过（§3.1）
7. `@Nullable` 紧贴类型且在注解串末位；无 `@Nullable private String` 声明位形态（§3.1）
8. 数据载体（Command / Request / Response / DTO）无 `Optional` 返回、无 `Optional` 字段 ⇒ 通过（§3.2）
9. 领域 / 网关「查询无结果」返回 `Optional<T>`；此类方法无 `return null`；无 `Optional<@Nullable T>`（§3.2）
10. 带 `@Nullable` 组件的 record 未显式重写访问器；无手写 `equals`（§3.3/§6 E1）
11. 覆写父类型带 `@Nullable` 的位置逐位重复标注（返回位为准，形参与返回位同用时两处同改）（§4.2）
12. 需要可空类型实参处用 `<E extends @Nullable Object>` 或 `@Nullable E` projection，未改用非空上界（§4.1）
13. 未 `@NullMarked` 的外部库返回值进领域前有显式检查或 `Optional.ofNullable(...)` 包装（§4.3）
14. 非空保证由 `Objects.requireNonNull` / `Assert` / `ValidateUtils` 承担；AppService 未加防御性 null 检查（§4.3）
15. PO 字段标注与 DB 列约束一致（可空列有 `@Nullable`、非空列无）；`id` 字段不标；`Persistable.getId()` 覆写按 §6 E3
    口径核对（§3.4/§6）

### 依赖与构建声明

16. 无 `javax.annotation.*` / `org.jetbrains.annotations.*` / `org.springframework.lang.*` /
    `jakarta.annotation.Nullable` 的 import（§1.2/§5）
17. 无模块用 `compileOnly` / `provided` / `optional` 隐藏 nullness 注解链（§5）
18. BOM / 直接依赖升级后版本表与 1.2 清单已复核，且 `spring-core` → `org.jspecify:jspecify` 传递边仍在（§1）

### 库专属

19. 覆写外部库契约时未用 `@NonNull` 去「撤销」父类 `@Nullable`（Spring 6 时代写法）——留空即可（§4.2/§5）
20. 启用 nullness 检查器时，record `equals` 的 `UNION_NULL` 豁免与本节偏离表逐行复核（§6）
