---
type: Convention
title: MapStruct 使用规范
description: 写/评审 MapStruct 映射（`@Mapper` 与注入、映射语义、构建接线、Null 与 JSpecify 缺口）时读——P3C 三级 + ❌/✅ + 双清单；转换器契约语义在 adapter.md；版本基线与禁用清单见 §1。
tags: [ mapstruct, convention ]
status: stable
---

# MapStruct 使用规范

MapStruct 在本仓只有一个落点： **adapter 层的协议转换器**（`Request → Command` / `DTO → Response`）。本篇是它的
**机制规则单源**
——`@Mapper` 形态、映射语义、构建接线、null 与 JSpecify 缺口；转换器 **契约语义**（方向 / 命名 / 完成判据）在
[adapter.md](adapter.md) §1，本篇只指不述。

> **单源指针**
> - **本规范**：MapStruct 用法的 **定案与落地**——规则表 / §1 版本基线与禁用清单 / 偏离表 / 写·检视双清单。
> - **Research
    溯源**：[foundation-deps-official-practices.md](../research/foundation-deps-official-practices.md#mapstruct)
>   — 官方事实全量（规则清单 / 过时对照 / 一级来源）。
> - **关联规范**：[adapter.md](adapter.md)（WebAssembler 契约语义与命名完成判据）、
>   [lombok.md](lombok.md)（处理器共存的 Lombok 侧，binding 单源在本篇 §4）、
>   [framework-type-contracts.md](framework-type-contracts.md)「Convertor（基础设施）」（手写侧边界）、
>   [test-conventions.md](../test-conventions.md)（测试侧 `Mappers.getMapper` 用法与映射测试）。
> - **格式**：P3C【强制 / 推荐 / 参考】三级 + ❌/✅ 反例正例；规则表列固定 `规则 | 级别 | 官方来源 | SODA 落地 | 关联 ADR`。

## 适用范围与管辖面

**适用**：`src/main` 下 **一切 MapStruct 用法**——`@Mapper` 接口（本仓现为 `XxxWebAssembler`）、其映射方法签名与注解、以及让处理器跑起来的构建接线。

**不适用**（去别处读）：

- 转换器 **契约语义**——方向、方法命名、参数承载、完成判据——在 [adapter.md](adapter.md) §1
- 持久化侧 PO ↔ 领域聚合的 **convertor**：手写，不是 MapStruct（§2「适用边界」行；写法见
  [framework-type-contracts.md](framework-type-contracts.md)「Convertor（基础设施）」）
- Lombok 注解本身的准入与禁用集——[lombok.md](lombok.md) §2；本篇只写两者 **共存**的构建接线（§4）
- 官方完整能力面中本仓无落点的部分（`@MapperConfig` / 生命周期钩子 / `@Context` / 限定符 / 处理器选项）——研究篇
  [#mapstruct](../research/foundation-deps-official-practices.md#mapstruct)，本篇不收
- 测试侧的 `Mappers.getMapper(...)` 与映射测试写法——[test-conventions.md](../test-conventions.md)

## 写清单

新增或修改 mapper 时按动作序逐条执行（括号内为落点）：

| #  | 动作                                                                                                          |
|----|---------------------------------------------------------------------------------------------------------------|
| 1  | 判适用：只用于 adapter 协议转换（`Request → Command` / `DTO → Response`）；持久化 PO ↔ 领域聚合一律手写（§2） |
| 2  | 接口标 `@Mapper`：`componentModel` 用 SPRING 常量 + `unmappedTargetPolicy = ERROR`（§2）                      |
| 3  | 生产侧经构造器注入取用 mapper；不写 `Mappers.getMapper(...)`（§2）                                            |
| 4  | 先靠 by-name 隐式映射；仅名称不一致 / 嵌套路径 / 歧义 / 常量表达式时才写 `@Mapping`（§3）                     |
| 5  | 多参方法：参数名与目标组件一一对应；同名歧义用 `source = "param.prop"` 消歧（§3）                             |
| 6  | 目标优先 record（走构造器映射）；构造候选多于一个时用 `@Default` 定夺（§3）                                   |
| 7  | 有意不映射的目标属性写 `@Mapping(target = "…", ignore = true)` 自证，不靠沉默通过（§3）                       |
| 8  | 源类型避开 `@Accessors(fluent = true)` 值对象（无 JavaBean getter）；作目标不设限（§3）                       |
| 9  | 构建接线：`mapstruct` 编译期依赖 + processor 走 `annotationProcessor`，binding 与 Lombok 同链在位（§4）       |
| 10 | 不关闭编译调试信息，且 `-parameters` 与调试信息不同时关断（§4）                                               |
| 11 | 生成实现（`*Impl.java`）不手改、不入库；改映射改接口（§4）                                                    |
| 12 | null 行为按默认策略显式约定；不指望 `@Nullable` 影响 mapper 生成（§5）                                        |
| 13 | 完成判据：`build` 绿 + 本清单逐条 + 文末检视清单无未决项（assembler 命名完成判据在 adapter.md §1）            |

## 1. 版本基线

### 1.1 版本

| 项                         | 值                            | 依据                                                                                                 |
|----------------------------|-------------------------------|------------------------------------------------------------------------------------------------------|
| MapStruct                  | **1.6.3**                     | `build.gradle:31` `mapstructVersion`；与官方 stable 一致                                             |
| `lombok-mapstruct-binding` | **0.2.0**                     | 本仓构建接线（`build.gradle:41` / `:50`）；官方仅 0.1.0 / 0.2.0 两版                                 |
| Lombok                     | 1.18.46                       | Spring Boot 4.1.1 BOM 管理；落入「≥ 1.18.16 需 binding」区间，见 1.2                                 |
| Spring Boot                | 4.1.1                         | `build.gradle:2` 插件版本（BOM 来源）                                                                |
| Java                       | 25                            | `build.gradle:20` toolchain                                                                          |
| Gradle                     | 9.5.1                         | wrapper `distributionUrl`                                                                            |
| 上游版本线（挂账）         | 1.7.0.Beta1 / Beta2（预发布） | 1.7.0.Beta2 起识别 JSpecify：源 `@NonNull` 跳过 null 检查、目标 `@NonNull` 强制检查——升级时须重跑 §5 |

### 1.2 禁用清单

| 本仓禁用项                                                          | 替代                                                                  | 依据                                                                                   | 影响面                                           |
|---------------------------------------------------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------|--------------------------------------------------|
| 关闭编译调试信息（`options.debug = false`）**且**移除 `-parameters` | 保持 javac 默认 `-g`（现行）；`-parameters` 保持（`build.gradle:61`） | 两通道同时关断 → 跨模块 record 目标编译失败 `Unmapped target properties: "arg0, arg1"` | 潜在                                             |
| `@Accessors(fluent = true)` 类型作 mapper **源**                    | 以 record 或 `@Getter` 形态类型作源                                   | fluent 生成无 `get` / `is` 前缀的访问器，不被认作 JavaBean getter ⇒ 源属性零可见       | 潜在（main 15 处 fluent 值对象，零 mapper 落点） |
| 生产代码 `Mappers.getMapper(...)`                                   | Spring 注入生成 Bean                                                  | 有 DI 时官方要求注入取用；`src/main` 零命中                                            | 潜在                                             |
| 手改 / 入库生成实现（`*Impl.java`）                                 | 处理器生成（`build/`、`bin/` 已 gitignore）                           | 生成物随接口重生成，手改静默丢失                                                       | 潜在                                             |
| `mapstruct-jdk8` 及包装坐标                                         | `org.mapstruct:mapstruct`                                             | 依赖树实测仅 `1.6.3`，无旧版混入                                                       | 不适用                                           |

维护：BOM / 直接依赖升级时逐项复核本表与 1.2 清单（依据＝官方 release notes / 迁移指南），改动就地更新。

## 2. Mapper 定义与注入

| 规则                                                                                                                                                                            | 级别     | 官方来源                                                                                                                                                                                                                                                                    | SODA 落地                                                                                                                                        | 关联 ADR             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|----------------------|
| **适用边界**：MapStruct 只承载 adapter 协议转换（`Request → Command` / `DTO → Response`）；infrastructure 的 PO ↔ 领域聚合 convertor **手写**（final 类 + 全量构造含显式 null） | 【强制】 | —（项目自定义）                                                                                                                                                                                                                                                             | `UserWebAssembler.java:40` 是 main 唯一 mapper；手写侧规则见 [framework-type-contracts.md](framework-type-contracts.md)「Convertor（基础设施）」 | ADR-0009<br>ADR-0024 |
| `@Mapper` 标在**接口**上，实现由注解处理器在编译期生成（纯方法调用、无反射、无运行时依赖）                                                                                      | 【强制】 | [参考指南 #defining-mapper](https://mapstruct.org/documentation/stable/reference/html/#defining-mapper)                                                                                                                                                                     | `UserWebAssembler.java:40`                                                                                                                       | ADR-0009             |
| `componentModel` 固定 `MappingConstants.ComponentModel.SPRING`——**用官方常量，不写字面量**；生成实现为 Spring Bean、由容器装配                                                  | 【强制】 | [API `MappingConstants.ComponentModel`](https://mapstruct.org/documentation/stable/api/org/mapstruct/MappingConstants.ComponentModel.html)<br>[参考指南 #using-dependency-injection](https://mapstruct.org/documentation/stable/reference/html/#using-dependency-injection) | `UserWebAssembler.java:40`（`:27` import）                                                                                                       | —                    |
| 有 DI 框架时一律**依赖注入**取用 mapper（本仓：Controller 构造器注入），生产代码不用 `Mappers.getMapper(...)` 工厂                                                              | 【强制】 | [参考指南 #using-dependency-injection](https://mapstruct.org/documentation/stable/reference/html/#using-dependency-injection)                                                                                                                                               | `UserController.java:51`；`src/main` 零 `Mappers`                                                                                                | ADR-0009             |

❌ 手写 `componentModel` 字面量——拼写错误无编译期保护，且与官方常量脱钩：

```java
// ❌ 字面量，拼错即静默取默认模型
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserWebAssembler {
}

// ✅ 官方常量（org.mapstruct.MappingConstants.ComponentModel）
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserWebAssembler {
}
```

❌ 生产代码用 `Mappers` 工厂取 mapper——绕过 Spring 装配，`uses` 引用与配置注入随之失效：

```java
// ❌ 生产代码里自取实现
private final UserWebAssembler assembler = Mappers.getMapper(UserWebAssembler.class);

// ✅ 生成 Bean 由容器注入（单构造器免 @Autowired）
public UserController(UserWebAssembler assembler) {
    this.assembler = assembler;
}
```

## 3. 映射语义

| 规则                                                                                                                                             | 级别     | 官方来源                                                                                                                                                | SODA 落地                                                                              | 关联 ADR             |
|--------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|----------------------|
| **默认 by-name 隐式映射**（含父类属性）：属性名相同即自动映射；仅名称不一致 / 嵌套路径 / 忽略 / 常量 / 表达式 / 歧义消解才写 `@Mapping`          | 【强制】 | [参考指南 #basic-mappings](https://mapstruct.org/documentation/stable/reference/html/#basic-mappings)                                                   | `UserWebAssembler.java:45-88` 13 个方法零 `@Mapping`                                   | —                    |
| 多源参数方法按属性名映射；**同名属性歧义必须**用 `source = "param.prop"` 消歧；全部源参数为 `null` 时返回 `null`                                 | 【强制】 | [参考指南 #mappings-with-several-source-parameters](https://mapstruct.org/documentation/stable/reference/html/#mappings-with-several-source-parameters) | 7 个多参方法的参数名与目标组件一一对应，无同名歧义（如 `toUpdateCommand` 的 `userId`） | ADR-0009<br>ADR-0021 |
| 目标构造选择顺序：builder → 标 `@Default` 的构造器 → 单一 public 构造器 → 无参构造器；多候选无 `@Default` → **编译错误**                         | 【强制】 | [参考指南 #mapping-with-constructors](https://mapstruct.org/documentation/stable/reference/html/#mapping-with-constructors)                             | 目标全为 record，唯一 canonical 构造器                                                 | —                    |
| `record` 目标走**构造器映射**路径；跨模块 record 的组件名经 class 文件元数据读取                                                                 | 【强制】 | [参考指南 #mapping-with-constructors](https://mapstruct.org/documentation/stable/reference/html/#mapping-with-constructors)                             | `soda-user-api` 的 Command / Response 为跨模块 record 目标（13 方法全命中）            | —                    |
| 集合映射：元素类型不同则逐元素调用元素映射方法；返回接口类型时实例化固定实现（`List`→`ArrayList`、`Set`→`LinkedHashSet`、`Map`→`LinkedHashMap`） | 【强制】 | [参考指南 #mapping-collections](https://mapstruct.org/documentation/stable/reference/html/#mapping-collections)                                         | `UserWebAssembler.java:88` `toResponseList`                                            | —                    |
| `unmappedTargetPolicy` 默认 `WARN`；本仓取 `ReportingPolicy.ERROR`——漏映射在编译期拦截                                                           | 【强制】 | [API `Mapper#unmappedTargetPolicy`](https://mapstruct.org/documentation/stable/api/org/mapstruct/Mapper.html#unmappedTargetPolicy())                    | `UserWebAssembler.java:40`                                                             | —                    |
| 有意不映射的目标属性**必须**写 `@Mapping(target = "…", ignore = true)` 自证；靠 `ERROR` 沉默通过不可接受                                         | 【强制】 | [参考指南 #configuration-options](https://mapstruct.org/documentation/stable/reference/html/#configuration-options)                                     | 当前零缺口（13 方法目标组件全部 by-name 覆盖），无 `ignore` 需求                       | —                    |
| 源类型**不得**为 `@Accessors(fluent = true)` 值对象——其访问器 `name()` 不是 JavaBean getter，源属性零可见；作 mapper **目标**不设限              | 【强制】 | —（项目自定义）                                                                                                                                         | main 15 处 fluent 值对象（`domain.types`），当期零 mapper 落点                         | —                    |

❌ 多源参数方法里只写属性名，而该属性在多个源参数上可见——MapStruct 无法选择，编译期报「Several possible source properties」：

```java
// ❌ 两个源参数都暴露 scene：只写属性名即歧义
@Mapping(target = "scene", source = "scene")
UpdateUserCommand toUpdateCommand(SceneContext context, UpdateUserRequest request);

// ✅ 用「参数名.属性」全限定消歧
@Mapping(target = "scene", source = "context.scene")
UpdateUserCommand toUpdateCommand(SceneContext context, UpdateUserRequest request);
```

❌ 以 `@Accessors(fluent = true)` 值对象作 mapper 源——访问器无 `get` / `is` 前缀，源属性零可见；同形态作 **目标**可用：

```java
// ❌ 源为 fluent 类型（domain.types 下的值对象）
CreateUserCommand toCreateCommand(Email email);   // 源属性零可见 ⇒ Unmapped target properties

// ✅ 源为 record（或 @Getter 形态）；fluent 类型仍可作目标
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
interface SomeAssembler {
    UserResponse toResponse(UserDTO dto);
}
```

## 4. 构建接线

| 规则                                                                                                                                                                | 级别     | 官方来源                                                                                                                                                                                                                         | SODA 落地                                                                       | 关联 ADR |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|----------|
| `mapstruct` 走**编译期**依赖（`compileOnly`）；`mapstruct-processor` 走 `annotationProcessor`，测试代码走 `testAnnotationProcessor`——处理器不得放 compile classpath | 【强制】 | [安装与接线](https://mapstruct.org/documentation/installation/)                                                                                                                                                                  | `build.gradle:38` / `:40`（main）、`:46` / `:49`（test）                        | —        |
| `lombok-mapstruct-binding` **必须**与 Lombok 同链在位：`annotationProcessor` + `testAnnotationProcessor` 各一行                                                     | 【强制】 | [FAQ 「Can I use MapStruct together with Project Lombok」](https://mapstruct.org/faq/#Can-I-use-MapStruct-together-with-Project-Lombok)<br>[参考指南 #lombok](https://mapstruct.org/documentation/stable/reference/html/#lombok) | `build.gradle:41` / `:50`（`org.projectlombok:lombok-mapstruct-binding:0.2.0`） | —        |
| 形参名通道**不同时关断**：`LocalVariableTable`（javac 默认 `-g`）或 `MethodParameters`（`-parameters`）任一在位即可；两者同关断则跨模块 record 目标编译失败         | 【强制】 | [javac 手册](https://docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html)<br>[参考指南 #mapping-with-constructors](https://mapstruct.org/documentation/stable/reference/html/#mapping-with-constructors)                 | `build.gradle:61` `-parameters` 保留；`options.debug` 未关闭                    | —        |
| 生成实现（`*Impl.java`）不手改、不入库；映射变更只改 mapper 接口                                                                                                    | 【强制】 | —（项目自定义）                                                                                                                                                                                                                  | `.gitignore:50`（`build/`）、`:38`（`bin/`）已覆盖                              | —        |

**`lombok-mapstruct-binding`（单源）**：本仓 Lombok 1.18.46 与 MapStruct 1.6.3 同链编译，binding 必须在位。它的价值是
**顺序保险**
而非「当前必需的修复」：现行处理器链是 lombok 先、mapstruct 后，此顺序下 **即使缺 binding 也生成正确实现**（实测 `@Getter`
`@Setter` 目标映射正确、`BUILD SUCCESSFUL`）。真正的失效条件是 **顺序反转**——`mapstruct-processor` 排在 Lombok
之前时，MapStruct
看不到 Lombok 尚未应用的访问器，目标侧零可见属性，遂生成 `return new Bean();` 这样的 **空对象**且 **零 error 零 warning**
，此时
`unmappedTargetPolicy = ERROR` 无从拦截（没有属性可报）。binding 使映射结果与处理器位次无关，把这条静默失效路径封死。

形参名通道同理，是 **双通道冗余**而非单点前提：javac 默认 `-g` 写 `LocalVariableTable`，`-parameters` 写 `MethodParameters`
—— **任一在位**，MapStruct 即可取得构造器 / record 目标的形参名（实测清空 `-parameters`
后生成实现逐字节相同；多参方法的形参名取自同轮源码，与两者无关）。真触发条件是 **两通道同时关断**——跨模块 record 目标报
`Unmapped target properties: "arg0, arg1"`。故本仓的硬约定是「不同时关断」，而非「必须开 `-parameters`」。

❌ 寄望「顺序恰好正确」而不声明 binding——顺序由依赖解析决定，不由 mapper 代码决定：

```groovy
// ❌ 处理器链只有 Lombok 与 MapStruct processor
annotationProcessor 'org.projectlombok:lombok'
annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
// ✅ 补官方 binding 制品，main 与 test 各一行
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```

❌ 关闭调试信息同时移除 `-parameters`（形参名两通道全断）：

```groovy
// ❌ 两通道同时关断
tasks.withType(JavaCompile).configureEach {
    options.debug = false
    options.compilerArgs.remove('-parameters')
}
// ✅ 保持 javac 默认 -g；-parameters 保留
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs << '-parameters'
}
```

依赖声明的层级选择（`implementation` / `compileOnly` / `annotationProcessor`
的通用规则）归 [STYLEGUIDE.md](../../STYLEGUIDE.md) §5；本节只写 MapStruct 特有的接线。

## 5. Null 策略与 JSpecify 缺口

| 规则                                                                                                                                                                                | 级别     | 官方来源                                                                                                                                                                                                                            | SODA 落地                                                                                                                  | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|----------|
| **1.6.3 不识别 `@Nullable` / `@NonNull` / `@NullMarked`**：这些注解对本版 mapper 生成**零影响**，null 行为完全由本节下表的默认策略决定——不得据 `@Nullable` 推断 mapper 的 null 检查 | 【强制】 | [参考指南 #checking-source-property-for-null-arguments](https://mapstruct.org/documentation/stable/reference/html/#checking-source-property-for-null-arguments)<br>[Release Notes](https://github.com/mapstruct/mapstruct/releases) | `UserWebAssembler.java:51` `@Nullable Integer expectedVersion`、`UserDTO.java:17-20` 的 `@Nullable` 组件对本版 mapper 惰性 | —        |
| null 策略保持默认：`NullValueMappingStrategy.RETURN_NULL`、`NullValueCheckStrategy.ON_IMPLICIT_CONVERSION`；`NullValuePropertyMappingStrategy` 只作用于 update 方法（本仓无）       | 【参考】 | [FAQ null 策略](https://mapstruct.org/faq/#How-do-I-handle-null-properties-in-the-source-bean)                                                                                                                                      | 零策略配置                                                                                                                 | —        |
| 跨模块 record 目标一旦出现「可空源 → 非空构造参数」的组合，**必须**用策略（`defaultValue` / 显式 `@Mapping`）表达，不能寄望注解驱动                                                 | 【参考】 | [参考指南 #mapping-result-for-null-arguments](https://mapstruct.org/documentation/stable/reference/html/#mapping-result-for-null-arguments)                                                                                         | 当期零命中（目标组件均可空或由策略覆盖）                                                                                   | —        |

❌ 指望 `@Nullable` 改变 mapper 的 null 检查——1.6.3 不识别这类注解，写了没有效果：

```java
// ❌ 以为 @Nullable 会让生成实现多/少一层 null 检查（1.6.3 无此行为）
UserResponse toSummary(UserDTO dto, @Nullable Integer revision);
// ✅ 注解表达调用方契约与静态分析；mapper 的 null 行为由显式策略与默认值约定承载
```

## 6. 偏离表

| 偏离项                     | 官方做法                                                                      | SODA 做法                                                                         | 理由                                                                     | 关联 ADR              |
|----------------------------|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------|-----------------------|
| JSpecify nullness 感知缺口 | 自 1.7.0.Beta2 起识别：源 `@NonNull` 跳过 null 检查、目标 `@NonNull` 强制检查 | 1.6.3 下 `@Nullable` / `@NonNull` 对 mapper 惰性，null 行为全由 §5 的默认策略决定 | 版本锚定当前构建栈：不升级、不引入预发布版；升级义务由 §1 末行维护句承载 | —（版本事实，非决策） |

## 检视清单

code-review 标准轴按本清单逐条核对（每条可判定；对应规则见括号内落点）：

### 代码

1. 适用边界：只出现在 `web/assembler/`；持久化 convertor 未被 MapStruct 接管（§2）
2. `@Mapper` 标在接口上，`componentModel` 用 `MappingConstants.ComponentModel.SPRING` 常量（非字面量）（§2）
3. 生产代码无 `Mappers.getMapper(...)`；mapper 经构造器注入取用（§2）
4. 映射靠 by-name；每处 `@Mapping` 都有名称不一致 / 嵌套 / 歧义 / 常量表达式的具体理由（§3）
5. 多参方法参数名与目标组件一一对应；同名歧义已用 `source = "param.prop"` 消歧（§3）
6. 目标为 record 时无手写转换代码；构造候选唯一或已用 `@Default` 定夺（§3）
7. `unmappedTargetPolicy = ReportingPolicy.ERROR` 在位；有意不映射处有 `ignore = true`（§3）
8. mapper 源类型中无 `@Accessors(fluent = true)` 值对象（§3；形态判据见 [lombok.md](lombok.md) §3.2）
9. 生成实现未被手改、未入库；`*Impl.java` 不出现在版本控制（§4）
10. null 语义由显式策略或方法契约承载；无「靠 `@Nullable` 让 mapper 少生成 null 检查」的假设（§5）

### 依赖与构建声明

11. `mapstruct` 为 `compileOnly`、`mapstruct-processor` 为 `annotationProcessor`；测试侧有对应的 `test*` 声明（§4）
12. `lombok-mapstruct-binding` 在 `annotationProcessor` 与 `testAnnotationProcessor` 各有一行，版本与 Lombok 匹配（§4）
13. 编译选项未同时关断调试信息与 `-parameters`；跨模块 record 目标编译通过（§4）

### 库专属

14. 新增 mapper 方法后 `build` 绿——`ERROR` 策略下漏映射不会沉默通过（§3）
15. `toResponseList` 一类集合方法仍逐元素委托元素映射，未改成手工循环（§3）
16. 未改动的默认策略（`NullValueMappingStrategy` / `NullValueCheckStrategy`）在评审中未被当作缺陷上报（§5）
17. 目标 record 的新增组件若确不映射，已写 `ignore = true` 而非调低 `unmappedTargetPolicy`（§3）
