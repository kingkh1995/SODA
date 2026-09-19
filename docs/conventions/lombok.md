---
type: Convention
title: Lombok 使用规范
description: 写/评审 Lombok 注解（准入与禁用集、访问器与 fluent、构造器与 Builder、相等与调试、日志与构建接线）时读——P3C 三级 + ❌/✅ + 双清单；MapStruct binding 单源在 mapstruct.md；版本基线与禁用清单见 §1。
tags: [ lombok, convention ]
status: stable
---

# Lombok 使用规范

> **本篇**＝Lombok 用法的定案与落地：§1 版本基线 → §2–§6 规则章 → §7 偏离表。 **官方事实全量与版本锚点溯源**归
> [research/foundation-deps-official-practices.md § Lombok](../research/foundation-deps-official-practices.md#lombok)
> ——本篇不设来源章，来源列只写
> 一级链接。
> **关联规范**：[mapstruct](mapstruct.md) §4（`lombok-mapstruct-binding`
> 与处理器声明顺序的单源，本篇只指不述）、[jackson](jackson.md) §3（fluent
> 类型的 JSON 契约）、[framework-type-contracts](framework-type-contracts.md#entity)「Entity」（双 Builder 与实体 JSON
> 契约的类型契约单源）、
> [dp-conventions](../dp-conventions.md) §2.2（值对象侧相等性的单源）、根 [STYLEGUIDE](../../STYLEGUIDE.md)
> §2.1（记录类型优先的选择表——本仓「为什么禁用
> `@Data` / `@Value`」的依据在彼处）。

## 适用范围与管辖面

管 `src/main` 的全部 Lombok 用法：注解准入与禁用集、访问器形态、构造器与 Builder、相等性与调试、日志门面与构建接线。

何时 **不**读：

- 写 `src/test` / `src/testFixtures` —— 测试侧 Lombok 用法归 [test-conventions](../test-conventions.md)，本篇不描述（测试侧注解命中
  0，但大量消费
  `builder()` / `createBuilder()` 生成物）
- 判「值对象用 record 还是 `final class` + Lombok」 —— 根 [STYLEGUIDE](../../STYLEGUIDE.md) §2.1 与本篇 §2.1（本篇只收注解准入）
- 判 DP 侧的 `equals` / `hashCode` / `toString` 契约 —— [dp-conventions](../dp-conventions.md) §2.2 / §2.3（本篇只给注解落位）
- 改 MapStruct 处理器声明、`-parameters` 或 binding —— [mapstruct.md](mapstruct.md) §4
- 查 Entity 双 Builder 的构造器形态与 JSON 契约 —— [framework-type-contracts](framework-type-contracts.md#entity)
- 要官方规则原文、过时对照、`@Jacksonized` 全貌、`@UtilityClass`
  官方定位细节 —— [research 篇](../research/foundation-deps-official-practices.md#lombok)

## 写清单

写或改 `src/main` 代码时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                     |
|----|------------------------------------------------------------------------------------------------------------------|
| 1  | 注解先查准入：在用白名单 7 项之外，一律回到 §1.2 禁用清单取替代形态（§2.1/§2.2）                                 |
| 2  | 访问器：值对象类级 `@Accessors(fluent = true)` + `@Getter`；`@Setter` 只给持久化 PO（§2.1/§3.1）                 |
| 3  | fluent 类型接入 JSON 前先确认序列化入口——字面量家族接口继承的 `@JsonValue`，或类型契约里的字段可见性补偿（§3.2） |
| 4  | fluent 类型不得放在 MapStruct 映射源位（§3.2；mapper 侧见 mapstruct.md §4）                                      |
| 5  | enum 带字段就保留 `@RequiredArgsConstructor`——它是私有构造器的唯一来源，删掉即编译失败（§4.1）                   |
| 6  | `@Builder` 只标显式构造器或静态工厂，不标类；Entity/Aggregate 保持双 Builder（§4.2）                             |
| 7  | 相等性先分角色：Entity 族不标 `@EqualsAndHashCode`、不重写、不进 `Set`/`Map` 键，身份判断走 `getId()`（§5.1）    |
| 8  | 值对象侧 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@Include`；有父类必 `callSuper = true`（§5.2）   |
| 9  | `equals`/`hashCode` 一律不手写（含 Entity 与值对象）（§5.2）                                                     |
| 10 | 日志用 `@Slf4j` 单一门面；参数先落局部变量，不放表达式（§6.1）                                                   |
| 11 | 改禁用集时同步根 `lombok.config` 的逐特性键，并复跑 `java -jar lombok.jar config <仓库根>` 自检（§1.2/§6.3）     |
| 12 | 完成判据：文末检视清单逐条通过，本篇零跨文档重复，来源列均为一级 markdown 链接（§7/检视清单）                    |

## 1. 版本基线

### 1.1 版本

| 项                       | 值                                                                              | 依据                                                                                                             |
|--------------------------|---------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Lombok（构建栈依据版本） | **1.18.46**                                                                     | Spring Boot 4.1.1 BOM `<lombok.version>`；根 `build.gradle` 以无版本号坐标声明 `org.projectlombok:lombok`        |
| 对照：官方最新稳定版     | **1.18.48**                                                                     | 官方 changelog / Maven Central metadata——本仓**不升**，升级前先复核本节维护句两事                                |
| Java 语法级别            | **25**（toolchain）                                                             | 根 `build.gradle` `JavaLanguageVersion.of(25)`；Lombok 的 JDK 支持下限见研究篇「JDK 兼容矩阵」                   |
| 编译期接入               | `compileOnly` + `annotationProcessor`，不进运行时 classpath                     | 官方 Gradle 接入写法；根 `build.gradle` 依赖块（main / test 两组同构）                                           |
| 注解处理器顺序           | lombok 先于 mapstruct-processor                                                 | 根 `build.gradle` 依赖块的声明顺序；失效模式与 `lombok-mapstruct-binding` 单源在 [mapstruct.md](mapstruct.md) §4 |
| 项目级配置               | 仓库根 `lombok.config`（`config.stopBubbling = true` + 10 条逐特性键，见 §6.3） | 仓库根 `lombok.config`；仓库内另一份落在管辖面外的参照仓不计                                                     |
| 禁用集拦截               | 编译期**在位**：§1.2 十项命中即构建失败                                         | 同上——`lombok.<feature>.flagUsage = error` 把评审纪律升为编译不变量                                              |

### 1.2 禁用清单

| 本仓禁用项                             | 替代                                                                              | 依据                                                                                                                              | 影响面                                                            |
|----------------------------------------|-----------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `@Data`                                | 显式 `@Getter` + 持久化 PO 的 `@Setter` + 显式构造器                              | [@Data](https://projectlombok.org/features/Data)                                                                                  | 潜在                                                              |
| `@Value`                               | record（记录类型优先，见 [STYLEGUIDE §2.1](../../STYLEGUIDE.md#21-jdk-特性优先)） | [@Value](https://projectlombok.org/features/Value)                                                                                | 潜在                                                              |
| `@ToString`                            | 手写稳定 `toString()`（本仓 9 处值对象手写）                                      | [@ToString](https://projectlombok.org/features/ToString)                                                                          | 潜在                                                              |
| `@AllArgsConstructor`                  | 显式构造器 / 构造器上的 `@Builder`                                                | [构造器族](https://projectlombok.org/features/constructor)                                                                        | 潜在                                                              |
| `@SuperBuilder`                        | 叶子类 `@Builder`（本仓无继承链 Builder 需求）                                    | [@SuperBuilder](https://projectlombok.org/features/SuperBuilder)                                                                  | 潜在                                                              |
| `@SneakyThrows`                        | 显式处理或包装受检异常                                                            | [@SneakyThrows](https://projectlombok.org/features/SneakyThrows)                                                                  | 潜在                                                              |
| `@Cleanup`                             | try-with-resources                                                                | [@Cleanup](https://projectlombok.org/features/Cleanup)                                                                            | 潜在                                                              |
| `@UtilityClass`                        | 显式 `final class` + 私有构造器                                                   | [@UtilityClass](https://projectlombok.org/features/experimental/UtilityClass)（官方 status negative；非 star 静态 import 不可用） | 潜在                                                              |
| `val`                                  | JDK 原生 `var`                                                                    | [val](https://projectlombok.org/features/val)                                                                                     | 潜在                                                              |
| `var`（lombok 的 `var` 类型）          | JDK 原生 `var`                                                                    | [var](https://projectlombok.org/features/var)                                                                                     | 潜在（JDK 25 下不可观测：javac 独立拒绝受限类型名，无法构造反例） |
| `lombok.addNullAnnotations = jspecify` | 手写签名 + 包级 `@NullMarked`                                                     | [configuration](https://projectlombok.org/features/configuration)                                                                 | 不适用（保留该行用于解释行为）                                    |

拦截事实：上表十项禁用注解由根 `lombok.config` 拦截——`lombok.<feature>.flagUsage = error` 使命中即构建失败（键清单与四条硬约束见
§6.3）。
`lombok.addNullAnnotations` 一行 **不进入**该配置（不启用，理由见 §7 E4）——它在表内只为解释行为，无拦截语义。

维护：BOM / 直接依赖升级时逐项复核本表与 1.2 清单（依据＝官方 release notes / 迁移指南），改动就地更新；
**本库额外复核两事**——
① 升级 Lombok 时验证 `lombok.superBuilder.flagUsage` 是否仍生效： **1.18.48 起该键静默失效**（源码声明仍在、javac / eclipse
handler 零引用，
配置自检与 `config -g` 均看不出来）；② `lombok.config` 变更后必须复跑 `java -jar lombok.jar config <仓库根>`—— **rc=2
即存在未知键或坏值**
（未知键不会让构建失败，这是该机制唯一的防呆手段）。

## 2. 注解准入

### 2.1 白名单（本仓在用 7 项注解族）

类级注解等价于给全部非 static 字段加注解，字段级覆盖类级。白名单之外的注解不因「官方有」即可用——先回 §1.2。

| 规则                                                                                     | 级别     | 官方来源                                                                                                                           | SODA 落地                                                                                                                                                                                                                                           | 关联 ADR |
|------------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Getter` / `@Setter` 默认生成 `public` 访问器；类级＝全部非 static 字段，字段级覆盖类级 | 【强制】 | [GetterSetter](https://projectlombok.org/features/GetterSetter)                                                                    | `@Getter` 28 处（`Sex.java:15`、`SoftwareVersion.java:60`、`UserPO.java:41` 等）；`@Setter` 4 处全在持久化 PO（`UserPO.java:42`、`AbstractAuditable.java:49`）；domain / api / application 零 `@Setter`                                             | —        |
| 命名：`boolean` 字段用 `is` 前缀；包装 `Boolean` 一律 `get` 前缀                         | 【强制】 | [GetterSetter](https://projectlombok.org/features/GetterSetter)                                                                    | `UserPO.java:79-82` 的两个 `boolean` 字段生成 `isSmsLoginEnabled()` / `isEmailLoginEnabled()`；`Boolean` 包装字段 0 处（前瞻约束）                                                                                                                  | —        |
| 已存在同名同参数个数的方法则不生成并告警；`@Tolerate` 可显式隐藏手写方法                 | 【推荐】 | [GetterSetter](https://projectlombok.org/features/GetterSetter)                                                                    | 手写访问器与生成目标零冲突（`Entity.getId()` / `isIdentified()`、`SensitiveValue.value()`）；`@Tolerate` 0 处                                                                                                                                       | —        |
| 字段上的 nullity 注解会被复制到生成访问器（`lombok.copyableAnnotations` 可扩展复制清单） | 【强制】 | [GetterSetter](https://projectlombok.org/features/GetterSetter)、[configuration](https://projectlombok.org/features/configuration) | `UserPO` 的 `@Nullable` 字段在生成访问器上逐位保留（`javap -v` 实证 15 处＝5 字段 × 3 位置）；判空工具与消息策略单源在 [STYLEGUIDE §2.3](../../STYLEGUIDE.md#23-空值校验角色)                                                                       | —        |
| `@Getter` 可标 enum；`@Setter` **不能**用于 enum                                         | 【强制】 | [GetterSetter](https://projectlombok.org/features/GetterSetter)                                                                    | `src/main` 7 个枚举**全部**标 `@Getter`（`Sex.java:15`、`AuthAccountType.java:13`、`UserState.java:19`、`SocialType.java:14`、`UserVerificationScene.java:25`、`VerificationChannel.java:21`、`VerificationState.java:24`），enum 上 `@Setter` 0 处 | —        |

❌ 给 enum 标 `@Setter`（枚举常量是单例，setter 会破坏取值集合的全局不变量），❌ 用包装 `Boolean` 字段却指望拿到 `is` 前缀访问器（
`is` 前缀只给
primitive `boolean`；包装类型一律 `get`）：

```java
// ❌ enum 上标 @Setter
@Getter
@Setter
public enum UserState implements StateEnumType { …}

// ✅ enum 只给 @Getter（取值集合不可变）
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum UserState implements StateEnumType { …}

// ❌ 包装 Boolean：期望 isSmsLoginEnabled() 却得到 getSmsLoginEnabled()——JSON 属性名与调用点名称同时错位
private Boolean smsLoginEnabled;
// ✅ 需要 is 前缀就用 primitive boolean；本仓 PO 即此形态（UserPO.java:79-82）
private boolean smsLoginEnabled;
```

### 2.2 禁用集与替代形态

| 规则                                                                                                                                                                                                                                                                                                                                                         | 级别     | 官方来源                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | SODA 落地                                                                                               | 关联 ADR |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|----------|
| 禁用集十项：`@Data` / `@Value` / `@ToString` / `@AllArgsConstructor` / `@SuperBuilder` / `@SneakyThrows` / `@Cleanup` / `@UtilityClass` / `val` / `var`（lombok）一律不得出现在 `src/main`；`@UtilityClass` 另有一条判据——官方 status negative，且非 star 静态 import 不可用（javac 的静态导入解析先于 Lombok），故工具类保持显式 `final class` + 私有构造器 | 【强制】 | [@Data](https://projectlombok.org/features/Data)、[@Value](https://projectlombok.org/features/Value)、[@ToString](https://projectlombok.org/features/ToString)、[构造器族](https://projectlombok.org/features/constructor)、[@SuperBuilder](https://projectlombok.org/features/SuperBuilder)、[@SneakyThrows](https://projectlombok.org/features/SneakyThrows)、[@Cleanup](https://projectlombok.org/features/Cleanup)、[@UtilityClass](https://projectlombok.org/features/experimental/UtilityClass)、[val](https://projectlombok.org/features/val)、[var](https://projectlombok.org/features/var) | 逐项替代与依据见 §1.2；`src/main` 十项命中**全 0**；编译期拦截＝根 `lombok.config` 十项逐特性键（§6.3） | —        |
| **不写 blanket 键** `lombok.experimental.flagUsage`：它覆盖整个 experimental 包，会连带阻断 `@Accessors`（本仓 15 处）造成构建红                                                                                                                                                                                                                             | 【强制】 | [configuration](https://projectlombok.org/features/configuration)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | 仓库根 `lombok.config` 逐特性键，无 blanket 键，键名清单见 §6.3                                         | —        |

❌ 用 `@Data` 顶替显式成员（可变对象的 `equals`/`hashCode` 随字段漂移；带父类时官方 small print 自述是坏主意），或把
`@UtilityClass` 用在有
`import static` 交叉引用的工具类上（javac 先于 Lombok 解析符号，编译失败）：

```java
// ❌ @Data：getter + setter + equals + hashCode + toString 一次全生成，边界失控
@Data
public class UserPO {
    private Long id;
    private String nickname;
}

// ✅ 显式声明参与面：PO 只给 @Getter/@Setter，相等性不进 PO
@Getter
@Setter
public class UserPO {
    private Long id;
    private String nickname;
}
```

## 3. 访问器

### 3.1 形态与命名

本仓 15 处 `@Accessors` **全部**为 `fluent = true`，落在值对象（DP / `Identifier` / 枚举）——访问器名对齐 record 的`value()`
风格。

| 规则                                                                                                                                                                                        | 级别     | 官方来源                                                                | SODA 落地                                                                                                                                      | 关联 ADR |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Accessors(fluent = true)`：访问器去 `get`/`set`/`is` 前缀、方法名即字段名；`chain` 随之默认 `true`                                                                                        | 【强制】 | [@Accessors](https://projectlombok.org/features/experimental/Accessors) | 15 处（`DecimalLiteralType.java:37`、`SoftwareVersion.java:27`、`Email.java:23`、`Sex.java:16`、`AuthAccountId.java:29` 等），无第三种参数形态 | —        |
| `@Accessors.prefix` 官方 **strongly discourage**；配了前缀列表后不匹配前缀的字段被整体跳过并告警                                                                                            | 【强制】 | [@Accessors](https://projectlombok.org/features/experimental/Accessors) | `prefix` / 显式 `chain` 参数全仓 0 处                                                                                                          | —        |
| 最近一处 `@Accessors` 决定 lombok 其它特性查找 getter 的方式——实际机制是**命名约定**：fluent 形态下 `@EqualsAndHashCode` 认（手写或生成的）去前缀访问器；纳入字段没有该形态访问器时回落字段 | 【强制】 | [@Accessors](https://projectlombok.org/features/experimental/Accessors) | `SoftwareVersion.value` 字段无 `@Getter`，靠类级 `@Accessors` 兜住的去前缀访问器进入相等性；值对象侧按此自证（§5.2）                           | —        |

❌ 以为「类上写了 `@Accessors` 就整体改走 getter」，于是给不参与相等性的派生字段也补 `@Getter`；❌ 给字段配前缀再指望 lombok
自动去掉：

```java
// ❌ 派生字段补了 @Getter，且纳入字段同样有去前缀访问器——两者在相等性里的角色无法从注解面看出
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class SoftwareVersion implements StringLiteralType {
    @EqualsAndHashCode.Include
    private final String value;   // 规范值：有去前缀访问器 value()
    @Getter
    private final int major;                          // 派生：不该进相等性
}

// ✅ 判读口径：纳入字段必须有去前缀访问器（自动生成或手写），派生字段既不作 Include 也不靠 getter 兜
```

### 3.2 fluent 的跨库后果

fluent 脱离 JavaBean 规范：按反射取访问器的库（Jackson、MapStruct、框架绑定）读不到 `getXxx()`，官方以 `@Jacksonized` 作为补偿手段。

| 规则                                                                                                                                                                                                                                                                                                                                                        | 级别     | 官方来源                                                                                                                                                    | SODA 落地                                                                                                                                                                                                                                                                                                                                                                                                                       | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| fluent 类型必须自带序列化入口，不得裸用字段名交给 bean 推断：① 值对象实现字面量家族接口（`@JsonValue` 声明在接口的 `value()` 上，实现类继承即得）；② 枚举由 Jackson 按 `toString()` 读写（3.0 起默认启用；业务枚举不得覆写，故 wire 值 ≡ `name()` 短名），无需 `@JsonValue`；③ 契约带字段可见性补偿的类型按[类型契约](framework-type-contracts.md#entity)走 | 【强制】 | [@Accessors](https://projectlombok.org/features/experimental/Accessors)、[@Jacksonized](https://projectlombok.org/features/experimental/Jacksonized)        | 15 处 fluent 全部有入口：4 个值对象族（`SoftwareVersion` / `Email` / `DecimalLiteralType` / `AuthAccountId`）经 5 个家族接口之一继承 `@JsonValue`（`StringLiteralType.java:32` 是其中之一），4 个 `AuthAccountId` 子类随基类继承，7 个枚举按 `toString()` 读写短名（≡ `name()`）；Entity 族经 `@JsonAutoDetect(fieldVisibility = ANY)` 补偿。本仓**不启用** `@Jacksonized`——值对象已有更窄的家族接口入口，而 Entity 不用 fluent | —        |
| fluent 类型**不得**放在 MapStruct 映射源位（生成的 `name()` 不是 bean getter，属性不可见 → 静默产出空对象）；作映射**目标**不受此限                                                                                                                                                                                                                         | 【强制】 | [@Accessors](https://projectlombok.org/features/experimental/Accessors)、[MapStruct §14](https://mapstruct.org/documentation/stable/reference/html/#lombok) | 现状映射面全为 record，零命中；mapper 侧声明与失效模式的单源在 [mapstruct.md](mapstruct.md) §4                                                                                                                                                                                                                                                                                                                                  | —        |

❌ 让 fluent 值对象裸奔过 Jackson（家族接口未实现、无 `@JsonValue`），❌ 用 fluent 类型当 MapStruct 源：

```java
// ❌ 无家族接口 ⇒ 无 @JsonValue ⇒ 序列化成 {"value":"..."} 对象形态，线形态失控
@Accessors(fluent = true)
public final class SomeCode {
    private final String value;
}

// ✅ 实现家族接口：@JsonValue 声明在接口的 value() 上，实现类继承即得线形态
public final class SomeCode implements StringLiteralType { …
}
```

## 4. 构造器与 Builder

### 4.1 `@RequiredArgsConstructor` 与 enum

| 规则                                                                                                                                                    | 级别     | 官方来源                                                   | SODA 落地                                                                                                                                                                        | 关联 ADR |
|---------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@RequiredArgsConstructor` 入参＝未初始化的 `final` 字段（＋未初始化的 `@NonNull` 字段），顺序＝字段声明顺序                                            | 【强制】 | [构造器族](https://projectlombok.org/features/constructor) | 13 处类级：7 个枚举常量字段（`Sex.java:17`、`AuthAccountType.java:15`、`VerificationState.java:26` 等）＋ 5 个 Spring 组件注入 ＋ 1 个事件总线（`SpringDomainEventBus.java:14`） | —        |
| enum 常量字段上本注解**承重**：常量已初始化，lombok 因而直接生成私有构造器；删除注解即编译失败（无处理器时 javac 报「无法将枚举构造器应用到给定类型」） | 【强制】 | [构造器族](https://projectlombok.org/features/constructor) | 7 个枚举全部依赖该注解提供 `private Sex(String)` 之类的构造器；`javap` 实证生成物存在                                                                                            | —        |
| 显式构造器**不阻止** `@XArgsConstructor` 生成；生成签名与手写冲突 → **编译错误**                                                                        | 【参考】 | [构造器族](https://projectlombok.org/features/constructor) | 零冲突案例                                                                                                                                                                       | —        |
| `@NoArgsConstructor`：有 `final` 字段须 `force = true`；官方明示**不生成**任何 `@NonNull` 检查                                                          | 【强制】 | [构造器族](https://projectlombok.org/features/constructor) | 3 处 `(access = AccessLevel.PUBLIC)` 全在持久化 PO（`UserPO.java:43`、`VerificationPO.java:46`、`UserArchivePO.java:40`），目标类无 `final` 字段，无需 `force`                   | —        |

❌ 把 enum 上的 `@RequiredArgsConstructor` 当死注解删掉（enum 常量字段的构造器随之消失，编译失败）：

```java
// ❌ 删除注解：常量 M("male") 失去构造器
@Getter
@Accessors(fluent = true)
public enum Sex implements EnumType {
    M("male"), F("female");
    private final String desc;
}

// ✅ 保留：它是 enum 私有构造器的唯一来源
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Sex implements EnumType {
    M("male"), F("female");
    private final String desc;
}
```

### 4.2 Builder

| 规则                                                                                                                                                                         | 级别     | 官方来源                                               | SODA 落地                                                                                                                                                                             | 关联 ADR |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Builder` 标在**类上**＝额外生成 `@AllArgsConstructor(access = PACKAGE)` 并在其上再标 `@Builder`；已有显式构造器时必须改标到构造器上                                        | 【强制】 | [@Builder](https://projectlombok.org/features/Builder) | 12 处**全部**标在显式构造器或静态工厂，**零类级**（`User.java:83`、`Verification.java:67`、4 个 `AuthAccount` 子类的恢复构造器与 `createBuilder()` 静态工厂）——不触发隐式构造器副作用 | —        |
| `@Builder.Default`：未在 build 中显式设置则得 `0` / `null` / `false`，字段初始化器被移入静态方法（不可引用 `this` / `super` / 非静态成员），且**手写构造器不会**使用该默认值 | 【参考】 | [@Builder](https://projectlombok.org/features/Builder) | 全仓 0 处——默认值走构造器链路，不靠 `@Builder.Default`                                                                                                                                | —        |
| Entity / Aggregate 的 Builder 面（双 Builder：`createBuilder()` 业务字段 / `builder()` 全持久化字段）                                                                        | 【强制】 | [@Builder](https://projectlombok.org/features/Builder) | 形态与参数面的**单源**在 [framework-type-contracts](framework-type-contracts.md#entity)「双 Builder 模式」，本篇只出指；值对象一律静态工厂 `of` / `from` / `parse`，不给 Builder      | —        |

❌ 把 `@Builder` 标在类上（隐式多出一个包级全参构造器，与显式构造器并存即签名冲突或暴露非预期构造路径）：

```java
// ❌ 类级 @Builder：隐式追加 @AllArgsConstructor(access = PACKAGE)
@Builder
public class User extends Aggregate<UserId> { …
}

// ✅ 标在显式构造器上，构造路径可控（双 Builder 形态）
@Builder
private User(Username username, …,PasswordAuthAccount passwordAccount) { …}

@Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
private static User create(Username username, …) { …}
```

## 5. 相等与调试

### 5.1 实体族：不重写、经 `getId()`

领域 **实体**（`Entity` / `Aggregate` / `AuthAccount` 族）一律不重写 `equals` / `hashCode`，保持 `Object`引用相等；实体不具备可比较性，不得作
`Set` / `Map` 键。 **值对象**（DP / `Identifier` 族 / 字面量家族）相反——相等性必须基于值。决策与依据的完整记录见 ADR-0040。

| 规则                                                                                                                                                                          | 级别     | 官方来源                                                                  | SODA 落地                                                                                                                                                                        | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| Entity 族一律不重写 `equals` / `hashCode`（继承 `Object` 引用相等），实体不具备可比较性                                                                                       | 【强制】 | —（项目自定义）                                                           | 现行 `src/main` 实体侧命中 **0**——`Entity` 与 7 个子类（`AuthAccount` / `User` / `Verification` / 4 个 `AuthAccount` 子类）的 8 处声明已整批移除；值对象侧 19 处一律保留（§5.2） | ADR-0040 |
| 跨实例身份判断一律经主键 `getId()` 显式表达；`equals` 只表示「同一对象引用」，不得用作身份检查                                                                                | 【强制】 | —（项目自定义）                                                           | 生产侧唯一依赖点已整改：`User.addAccount` 的「已包含」判定走 `existing.getId().equals(account.getId())`（`User.java:200`），行为语义不变（同 id → no-op、同类型异 id → 拒绝）    | ADR-0040 |
| Entity 族不得作 `Set` / `Map` 键位；按身份去重时以 id 为键                                                                                                                    | 【强制】 | —（项目自定义）                                                           | `User.accounts` 为 `List`；`AuthAccount` 去重走主键比对                                                                                                                          | ADR-0040 |
| `passwordAccount` 永不进入 `accounts`——主键比对路径的前置不变量（密码账户创建瞬态无 id，取 `getId()` 抛 NPE）                                                                 | 【强制】 | —（项目自定义）                                                           | `User.java:199` 的首行守卫即此条；放宽该断言即引入 NPE                                                                                                                           | ADR-0040 |
| 移除必须**整批**：`callSuper = true` 指向已移除 `equals` 的父类时 lombok **静默退化**——编译零警告，生成「`Object.equals` 调用 ＋ 仅自有字段比较」，哈希不稳定的旧缺陷原样保留 | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode) | 8 处已同批移除，子类的 `callSuper = true` 随之消失（`callSuper = true` 现余 12 处全在值对象侧）；分批改会留下无告警的假修复                                                      | ADR-0040 |

❌ 给实体标 `@EqualsAndHashCode`（走 `getId()` ⇒ 瞬态实体抛 NPE；纳入可变业务字段 ⇒ `disable()` / `assignVersion()` 后
`Set.contains(同一实例)` 返回
false）；❌ 以 `equals` 当身份检查：

```java
// ❌ 字段相等 + 可变业务字段：哈希随状态漂移，且瞬态路径 NPE
@EqualsAndHashCode
public abstract class Entity<ID extends Identifier<?>> implements Identifiable<ID> { …
}

// ❌ 把 equals 当身份检查：引用相等下「同一逻辑实体的另一实例」判为不同
if(user.

equals(otherUser)){ … }

// ✅ 身份判断显式走主键；实体不进 hash 集合
        if(user.

getId().

equals(otherUser.getId())){ … }
```

### 5.2 值对象侧与生成物契约

值对象侧的注解落位与 `@Include` / `callSuper` 矩阵，语义单源在 [dp-conventions](../dp-conventions.md) §2.2；本节只给注解面。

| 规则                                                                                                                                                             | 级别     | 官方来源                                                                                                                            | SODA 落地                                                                                                                                                                                                 | 关联 ADR |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@EqualsAndHashCode` 默认纳入全部非 static 非 transient 字段；`onlyExplicitlyIncluded = true` 时仅 `@Include` 字段参与                                           | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)                                                           | 值对象侧 19 处声明；9 处 `onlyExplicitlyIncluded = true`，4 处 `@EqualsAndHashCode.Include`（`SensitiveValue.java:32`、`DecimalLiteralType.java:40`、`SoftwareVersion.java:54`、`AuthAccountId.java:44`） | —        |
| 继承场景：有显式父类必须显式给 `callSuper`；无父类却设 `callSuper = true` → **编译错误**                                                                         | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)                                                           | 12 处 `callSuper = true` 全在值对象侧（`Email.java:22`、`BankCard.java:18`、4 个 `XxxAuthAccountId` 等），继承矩阵 100% 显式；实体族 8 处随注解同批消失                                                   | —        |
| 非 final 且直接继承 `Object` 时生成 `canEqual`（JPA proxy 与子类不破坏 equals 契约）；手写 `equals` 必须同步覆盖                                                 | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)                                                           | `canEqual` 存在性已实证（`SensitiveValue` / `DecimalLiteralType` / `AuthAccountId` 等值对象侧）；本仓禁手写（下行）使其无落点                                                                             | —        |
| **禁手写** `equals` / `hashCode`——存在任何同名方法（无论返回类型）时 lombok 两个都不生成并告警，手写须自管 `canEqual` 与 hashCode 一致性                         | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)                                                           | 全仓手写 0 处                                                                                                                                                                                             | —        |
| 字段有 getter 时 `equals` / `hashCode` **调用 getter**（`doNotUseGetters = true` 可关）；数组按 deep 比较；`BigDecimal` 尺度问题需 `@Include(replaces = …)` 定制 | 【强制】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)                                                           | 访问路径按 §3.1 的 fluent 命名约定判读（`SoftwareVersion` 走 `value()`）；金额 / 百分比类以规范值 `String` 比较，已规避尺度陷阱；数组字段 0 处                                                            | —        |
| `hashCode` 算法与 `toString` 输出格式都**不承诺跨版本稳定**；`cacheStrategy` 只可用于不可变对象                                                                  | 【参考】 | [EqualsAndHashCode](https://projectlombok.org/features/EqualsAndHashCode)、[@ToString](https://projectlombok.org/features/ToString) | `cacheStrategy` 0 处；hashCode 不入库；9 处 `toString()` 全为手写稳定契约（`SensitiveValue.java:55`、`SoftwareVersion.java:167` 等），`@ToString` 0 处（§1.2）                                            | —        |

❌ 多态值对象子类漏 `callSuper = true`（静默继承基类相等语义 → 跨类误等）；❌ 手写 `equals`（连带抑制注解生成）：

```java
// ❌ 漏 callSuper：子类字段不进比较，且跨类可能误等
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class EmailAuthAccountId extends AuthAccountId { …
}

// ✅ 基类 onlyExplicitlyIncluded + 规范值 @Include；子类显式 callSuper
@EqualsAndHashCode(callSuper = true)
public final class EmailAuthAccountId extends AuthAccountId { …
}
```

### 5.3 生成物契约

生成成员的输出不是契约面：`hashCode` 值不得持久化、不得跨版本依赖；`toString` 输出不得被外部代码解析。本仓 `toString`
全为手写（§5.2 规则表末行），
日志内容走 `@Slf4j`（§6.1），不依赖生成物形态。

## 6. 日志与构建接线

### 6.1 `@Slf4j` 与门面统一

| 规则                                                                                                                       | 级别     | 官方来源                                      | SODA 落地                                                                                                                              | 关联 ADR |
|----------------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Slf4j` 生成 `private static final org.slf4j.Logger log = LoggerFactory.getLogger(X.class)`；已存在同名字段则不生成并告警 | 【强制】 | [log](https://projectlombok.org/features/log) | 8 处（`UserController.java:44`、`ProblemDetailAdvice.java:38`、`UserServiceImpl.java:40` 等）；手写 `Logger` 字段 0 处                 | —        |
| 其它 `@Log*` 变体各绑定一个具体日志门面；本仓统一 SLF4J，不用其余变体                                                      | 【强制】 | [log](https://projectlombok.org/features/log) | 仅 `@Slf4j` 8 处，其余变体 0 处                                                                                                        | —        |
| 日志表达式**不应含副作用**（未来版本可能按编译期日志级别插入 `if` 短路，副作用将随级别变化）                               | 【强制】 | [log](https://projectlombok.org/features/log) | 30 处日志调用逐处判读，含方法调用的 7 处全为纯函数；**本规则只按「副作用」维度在册**——日志内容安全（凭证明文不得进日志）不在本篇管辖面 | —        |

❌ 日志参数里现算或在表达式里带副作用，❌ 混用门面：

```java
// ❌ 参数含方法调用：短路行为一旦随级别变化，调用是否发生即不可预测
log.info("userState={}",user.getState());

// ✅ 先落局部变量再传参
var state = user.getState();
log.

info("userState={}",state);

// ❌ @Log4j2 / @CommonsLog 混用
// ✅ 统一 @Slf4j
```

### 6.2 `@NonNull` 与非空守卫分工

| 规则                                                                                                               | 级别     | 官方来源                                               | SODA 落地                                                                                                                                           | 关联 ADR |
|--------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 只有 `lombok.NonNull` 会在参数 / record 组件上插入检查（构造器在 `this()` / `super()` 之后）；已有等价检查则不重复 | 【强制】 | [@NonNull](https://projectlombok.org/features/NonNull) | 本仓 0 处——非空保证由 `Objects.requireNonNull` / `Assert` / `ValidateUtils` 承担，分工单源在 [STYLEGUIDE §2.3](../../STYLEGUIDE.md#23-空值校验角色) | —        |

❌ 用 `lombok.NonNull` 标注参数来「顺便」获得非空守卫（注解来自 Lombok 而非 JSpecify，与包级 `@NullMarked`
的两套空性体系并存；且守卫消息不可定制、构造器场景插在 `this()` / `super()` 之后）：

```java
// ❌ Lombok 注解当空性声明用：多一套空性体系，非空语义与 JSpecify 基线分叉
public void changeMobile(@lombok.NonNull Mobile mobile) { …}

// ✅ 声明式空性交给 JSpecify（包级 @NullMarked 已默认非空），运行时守卫交给显式工具
public void changeMobile(Mobile mobile) {
    Assert.notNull(mobile, "mobile must not be null");
    // …
}
```

### 6.3 处理器链与 `lombok.config`

| 规则                                                                                                                                              | 级别     | 官方来源                                                                           | SODA 落地                                                                                                          | 关联 ADR |
|---------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|----------|
| 与其它注解处理器共存：lombok 必须先于其它处理器执行，后者才能看到其生成的方法                                                                     | 【强制】 | [changelog](https://projectlombok.org/changelog)                                   | 根 `build.gradle` 声明顺序 lombok → mapstruct-processor（main / test 两组同序）                                    | —        |
| MapStruct 侧要求额外声明 `lombok-mapstruct-binding` 处理器，否则映射静默产出空对象（构建成功、零 warning，`unmappedTargetPolicy = ERROR` 拦不住） | 【强制】 | [MapStruct §14](https://mapstruct.org/documentation/stable/reference/html/#lombok) | **单源在 [mapstruct.md](mapstruct.md) §4**（声明现状、失效模式、`-parameters` 因果链全在彼处）；本篇只出指，不重述 | —        |
| `lombok.config`：目录级继承、就近优先，`config.stopBubbling = true` 定根；`lombok.<feature>.flagUsage = warning\|error` 可把禁用注解升为构建失败  | 【强制】 | [configuration](https://projectlombok.org/features/configuration)                  | 仓库根 `lombok.config` 已落（`config.stopBubbling = true` + §1.2 十项逐特性键）；§1.2 十项命中即构建失败           | —        |

仓库根 `lombok.config` 的定稿内容（10 条逐特性键 + 定根）：

```properties
config.stopBubbling=true
lombok.data.flagUsage=error
lombok.value.flagUsage=error
lombok.toString.flagUsage=error
lombok.allArgsConstructor.flagUsage=error
lombok.superBuilder.flagUsage=error
lombok.sneakyThrows.flagUsage=error
lombok.cleanup.flagUsage=error
lombok.utilityClass.flagUsage=error
lombok.val.flagUsage=error
lombok.var.flagUsage=error
```

四条硬约束（每条都有实测依据，违反即零保护或构建红）：

1. **禁写 `lombok.experimental.flagUsage`**——blanket 键覆盖整个 experimental 包，会阻断 `@Accessors`（本仓 15 处）与
   `@UtilityClass` 同包注解，一写即构建红。
2. **`@SuperBuilder` 的键是 `lombok.superBuilder.flagUsage`**， **不是** `lombok.experimental.superBuilder.flagUsage`（后者是
   unknown key， **静默失效**）。此键最易写错，须逐字照抄上表。
3. **`lombok.var` 一行不可观测**（JDK 25 下 javac 独立拒绝 `var` 作受限类型名，构造不出反例）——保留该行，影响面记「潜在」。
4. **未知键不会让构建失败**——拼错键名时 javac 只打 stderr 警告、rc=0、class 照常产出。自检动作：`lombok.config` 变更后跑
   `java -jar lombok.jar config <仓库根>`， **rc=2 表示有未知键或坏值**。

❌ 写 blanket 键或写错 `@SuperBuilder` 的键名，❌ 依赖「构建绿」证明配置生效：

```properties
# ❌ blanket 键：连带阻断 @Accessors（15 处）⇒ 构建红
lombok.experimental.flagUsage=error
# ❌ 带包前缀的 superBuilder 键：unknown key，静默失效（构建照常绿）
lombok.experimental.superBuilder.flagUsage=error
# ✅ 逐特性键
lombok.superBuilder.flagUsage=error
```

## 7. 偏离表

| 偏离项                                      | 官方做法                                                                   | SODA 做法                                                                                | 理由                                                                                                                                                                                                                                                                                                                                                                                        | 关联 ADR |
|---------------------------------------------|----------------------------------------------------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| **E1** 实体相等性语义                       | Lombok 默认按字段生成；Vernon（IDDD p.186）建议实体按 unique identity 重写 | **不重写**（保持 `Object` 引用相等），跨实例身份判断经 `getId()` 显式表达                | 字段相等会在瞬态期 NPE、保存后仍可达、状态变更后 `Set.contains` 失配，且纳入可变业务字段即破坏相等性不变量，故实体不生成 `equals` / `hashCode`；JPA 3.2 只要求主键类 / map-key embeddable 重写，Hibernate 现行指南明说可考虑完全不实现；Evans 指出引用身份在持久化后丢失，故身份判断必须显式化。前置条件（不进 hash 集合 / 判定逐点按 id / 不引入 JPA 集合关联与 detached 重挂）见 ADR-0040 | ADR-0040 |
| **E2** `lombok.config` 承担禁令             | 官方提供 `flagUsage` 机制，不规定项目是否启用                              | **启用**：根 `lombok.config` 落 10 条 `flagUsage = error` + `config.stopBubbling = true` | 官方对 `@Data` 等并无禁令，§1.2 属团队裁定；不启用该机制时禁令只能靠评审纪律，启用后升为编译期不变量——实测十项逐条可阻断（§6.3）                                                                                                                                                                                                                                                            | —        |
| **E3** `@RequiredArgsConstructor` 用于 enum | 官方定义为「未初始化 `final` 字段的构造器」，未讨论 enum                   | **保留用于 enum 常量字段**（7 个枚举）                                                   | 常量已初始化 ⇒ lombok 直接生成私有构造器，注解承重（删除即编译失败）；显式私有构造器为等价备选但无收益。官方文档未覆盖该用法，故记偏离                                                                                                                                                                                                                                                      | —        |
| **E4** `lombok.addNullAnnotations` 不启用   | 官方把 `jspecify` 标为 recommended，让生成代码带上 nullness 注解           | 不启用；空性真值走手写签名 + 包级 `@NullMarked`（[jspecify.md](jspecify.md)）            | 生成成员的 nullness 无消费者（本仓无 nullness 检查器与 CI）；启用会改变生成字节码而零收益；包级 `@NullMarked` 已为生成成员提供默认非空语义                                                                                                                                                                                                                                                  | —        |

## 检视清单

code-review 标准轴按本清单逐条核对（每组逐条可判定，括号内为落点）：

### 代码

1. `src/main` 十项禁用命中为 0（`@Data` / `@Value` / `@ToString` / `@AllArgsConstructor` / `@SuperBuilder` /
   `@SneakyThrows` / `@Cleanup` / `@UtilityClass` /
   `val` / lombok `var`）（§1.2）
2. `@Setter` 与 `@NoArgsConstructor` 只出现在 infrastructure 持久化 PO；domain / api / application 零命中（§2.1）
3. Entity 族无 `@EqualsAndHashCode`，全仓无手写 `equals` / `hashCode`（§5.1/§5.2）
4. 实体不出现在 `Set` / `Map` 键位；「是否已包含」类判定按 `accountId` / `getId()` 而非 `equals`（§5.1）
5. `passwordAccount` 不进入 `User.accounts`（§5.1）
6. 值对象侧 `@EqualsAndHashCode` 有父类必标 `callSuper = true`；`onlyExplicitlyIncluded` 与 `@Include` 配套；多态子类未漏
   `callSuper`（§5.2）
7. fluent 类型有序列化入口（家族接口继承的 `@JsonValue`，或类型契约的字段可见性补偿），无裸 fluent 对象形态外泄（§3.2）
8. fluent 类型未出现在任何 MapStruct 映射源位（§3.2）
9. `@Accessors` 只有 `fluent = true` 形态；无 `prefix` 参数、无显式 `chain`（§3.1）
10. enum 上的 `@RequiredArgsConstructor` 未被删除；enum 上无 `@Setter`（§2.1/§4.1）
11. `@Builder` 无类级标注；Entity/Aggregate 双 Builder 命名与参数面符合类型契约（§4.2）
12. 无 `lombok.NonNull`；非空守卫走 `Objects.requireNonNull` / `Assert` / `ValidateUtils`（§6.2）
13. `@Slf4j` 之外无其它 `@Log*`；日志参数无方法调用（副作用维度）（§6.1）
14. 无对生成物输出的外部依赖：`hashCode` 未持久化、`toString` 输出未被解析（§5.3）

### 依赖与构建声明

15. `org.projectlombok:lombok` 走 `compileOnly` + `annotationProcessor`，不进运行时 classpath（§1.1）
16. 处理器声明顺序 lombok 先于 mapstruct-processor（main / test 两组）（§6.3；`lombok-mapstruct-binding` 的解释见
    mapstruct.md §4）
17. 根 `lombok.config` 与 §1.2 十项一一对应，无 blanket 键 `lombok.experimental.flagUsage`，`config.stopBubbling = true`
    （§6.3）
18. `lombok.config` 变更后 `java -jar lombok.jar config <仓库根>` rc=0（§1/§6.3）

### 库专属

19. Lombok 升级后已复核 §1.1 版本表与 §1.2 禁用清单，且 `lombok.superBuilder.flagUsage` 仍生效（§1）
20. 同标 `@Accessors(fluent = true)` 与 `@EqualsAndHashCode` 的类，其纳入字段确有去前缀访问器（§3.1/§5.2）
