---
type: Convention
title: Jackson 使用规范
description: 写/评审 Jackson 3 用法（接入与共享、注解落位、多态、3.0 行为基线）时读——四列规则表 + ❌/✅ + 写·检视双清单（doc-conventions §4.3）；DP 域契约去 dp-json-conventions；版本基线与禁用清单见 §1。
tags: [ jackson, convention ]
status: stable
---

# Jackson 使用规范

本仓的 Jackson 用法只有一层：Boot 自动配置提供的共享 mapper ＋ 类型上的注解。 **注解落位是主战场**，而线格式由 Jackson 3.0 的
默认值决定——两者都写在这里。

> **单源指针**
> - **本规范**：Jackson 用法的 **定案与落地**——规则表 / §1 版本基线与禁用清单 / 偏离表 / 写·检视双清单。
> - **Research 溯源**：[foundation-deps-official-practices.md](../research/foundation-deps-official-practices.md#jackson)
>   — 官方事实全量（规则清单 / 过时对照 / 一级来源），本篇不设来源章。
> - **关联规范**：[dp-json-conventions](dp-json-conventions.md)（DP 的 JSON 契约：模式总表 / 类型映射 / BigDecimal DP
    模式 / 禁
>   `Serializable`）、[framework-type-contracts](framework-type-contracts.md)（Entity JSON 契约与多态实体分派）、
>   [aip-api-conventions](aip-api-conventions.md)（wire 字段命名与出站 null 策略）、
>   [test-conventions](../test-conventions.md)（测试侧 mapper）。
> - **格式**：【强制 / 推荐 / 参考】三档＋❌/✅ 反例正例＋四列规则表（[doc-conventions §4.3](../doc-conventions.md#43-规则表与示例的表达约定)），形式采纳＝[ADR-0036](../adr/0036-p3c-surface-format-retrofit.md)；
>   §1.2 禁用清单的依据列保留逐条链接（纯清单，不在列固定内）。

**来源登记**（登记结构与封闭 ID 语义见 [doc-conventions §4.2](../doc-conventions.md#42-来源登记)；本库版本锚点集中本表）：

| 来源 ID        | 含义                                                               | 钉住版本与链接入口                                                                                                                                             |
|----------------|--------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `JACKSON-DOC`  | Jackson 3 官方文档、javadoc、迁移指南与 issue                     | 项目锁定 **3.1.5**（§1.1，Boot 4.1.1 BOM 属性）；逐条一级链接与过时对照单源在 [研究篇 § Jackson](../research/foundation-deps-official-practices.md#jackson)      |
| `SODA-POLICY`  | 项目硬门与项目扩展（官方无对应强制条款，或比官方更严）           | 本篇（封闭来源列见 [doc-conventions §4.2](../doc-conventions.md#42-来源登记)）                                                                                 |
| `SODA-DECISION` | 已裁定的项目取舍                                                  | 行内 `ADR-NNNN`（落在 `SODA 落地` 单元格）；理由见 §6 偏离表与对应 ADR 正文                                                                                    |

## 适用范围与管辖面

- **管辖面**：`src/main` 的 Java 源码与配置（`application.yml`）。`src/test`、`src/testFixtures` 的 mapper 用法不入本篇——测试侧两套
  mapper（`JacksonTestUtil` / `DomainTestUtil`）归 [test-conventions](../test-conventions.md)。
- **何时读**：新增／修改任何 `@Json*` 注解；判断 wire（线格式）是否变化；升级 Jackson／Boot BOM；评审 Jackson 相关改动。
- **何时不读**：写 DP 的 JSON 契约（`@JsonValue` / creator 落位、标量映射、BigDecimal、时间点）→
  [dp-json-conventions](dp-json-conventions.md)；写 Entity／Aggregate 的序列化契约与多态分派 →
  [framework-type-contracts](framework-type-contracts.md)；判 wire 字段命名与出站 null 是否省略 →
  [aip-api-conventions](aip-api-conventions.md)
  ；查官方全量规则 → [研究篇](../research/foundation-deps-official-practices.md#jackson)。

## 写清单

改到 Jackson 相关代码时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                                      |
|----|-----------------------------------------------------------------------------------------------------------------------------------|
| 1  | 先判形态：该类型是否已有 JSON 契约 → 取 [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)（§3）                |
| 2  | 多字段 record 零注解（按 component 名推断）；`-parameters` 是该推断的前提，删开关即静默破坏全部 record 绑定（§3）                 |
| 3  | 单字段 record（非家族形态）：`@JsonValue` 标在 record **component** 上（§3）                                                      |
| 4  | class + private 构造器：标量入口显式 `@JsonCreator(mode = DELEGATING)`（§3）                                                      |
| 5  | Entity 恢复构造器：`mode = PROPERTIES` + 每参非空 `@JsonProperty`，非空字段补 `required = true`（§3）                             |
| 6  | 实体加字段：同步补恢复构造器参数与 round-trip 断言（契约见 [framework-type-contracts](framework-type-contracts.md#entity)）（§3） |
| 7  | 隐藏属性用 `@JsonIgnore`；无新决策不动未知字段 / 大小写 / null 输出策略（§3、§5）                                                 |
| 8  | 多态层级：基类 `@JsonTypeInfo(use = Id.NAME, property = …)`，每个 `permits` 子类补 `@JsonTypeName` 且与领域枚举短名一致（§4）     |
| 9  | 业务枚举不覆写 `toString()`——wire 值即 `name()` 短名（§5）                                                                        |
| 10 | 不手建 mapper：不 `new`、不 builder、不加 `spring.jackson.*`；要定制经 `JsonMapperBuilderCustomizer`（§2）                        |
| 11 | 不引入 §1.2 禁用清单任一项；`com.fasterxml.jackson.annotation.*` 是正确包名，勿替换（§1）                                         |
| 12 | 改动涉默认行为前查 §5：primitive 收 null、尾随 token、空 bean、未知字段、大小写（§5）                                             |
| 13 | wire 可能变化时先落断言（round-trip ＋ 非法 JSON 拒绝），写法见 [test-conventions](../test-conventions.md)（§5）                  |
| 14 | 完成判据：`src/main` 无 `tools.jackson.databind` / `core` import；wire 变化有断言（§5）                                       |

## 1. 版本基线

### 1.1 版本

| 项                      | 值                  | 依据                                                                                    |
|-------------------------|---------------------|-----------------------------------------------------------------------------------------|
| Jackson databind / core | `3.1.5`             | Spring Boot 4.1.1 BOM 属性 `<jackson-bom.version>`（groupId `tools.jackson`）           |
| jackson-annotations     | `2.21`              | 同一 BOM 属性 `<jackson.version.annotations>`；3.x 复用 2.x 注解构件，无 3.x 注解发布线 |
| Spring Boot             | `4.1.1`             | 根 `build.gradle`（Boot Gradle 插件）＋ `dependencyManagement` 导入其 BOM                |
| Java                    | `25`                | 根 `build.gradle` 的 toolchain 声明                                                      |
| 依据的版本线            | 3.1（3.x 首个 LTS） | [研究篇 Jackson 版本锚点](../research/foundation-deps-official-practices.md#jackson)    |

### 1.2 禁用清单

收录口径＝ **本仓可能误用**的过时形态（2.x 旧知识高发位）；其余过时项留研究篇。收录行对当前代码均为潜在风险，故影响面一律 `潜在`。

| 本仓禁用项                                                             | 替代                                                  | 依据                                                                                                                                                  | 影响面 |
|------------------------------------------------------------------------|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| `com.fasterxml.jackson.databind.*` / `com.fasterxml.jackson.core.*` 包 | `tools.jackson.databind.*` / `tools.jackson.core.*`   | [MIGRATING_TO_JACKSON_3 §2](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md)                                        | 潜在   |
| `new ObjectMapper()`（无参）                                           | `JsonMapper.builder()` 构建（§2：生产侧由 Boot 提供） | [MapperBuilder](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/cfg/MapperBuilder.java)                   | 潜在   |
| `configure(...)` / `setSerializationInclusion(...)` 等 mapper setter   | builder 链式配置                                      | [jackson-databind#5270](https://github.com/FasterXML/jackson-databind/issues/5270)                                                                    | 潜在   |
| `Module`                                                               | `JacksonModule`                                       | [jackson-databind#3037](https://github.com/FasterXML/jackson-databind/issues/3037)                                                                    | 潜在   |
| `JsonSerializer` / `JsonDeserializer`（自定义实现基类）                | `ValueSerializer` / `ValueDeserializer`               | [jackson-databind#3044](https://github.com/FasterXML/jackson-databind/issues/3044)                                                                    | 潜在   |
| checked `JsonProcessingException` / `JsonMappingException`             | `JacksonException` / `DatabindException`（unchecked） | [JSTEP-4](https://github.com/FasterXML/jackson-future-ideas/wiki/JSTEP-4)                                                                             | 潜在   |
| `JsonNode.fields()` / `TextNode` / `isContainerNode()`                 | `properties()` / `StringNode` / `isContainer()`       | [jackson-databind#4875](https://github.com/FasterXML/jackson-databind/issues/4875)                                                                    | 潜在   |
| `PropertyNamingStrategy.*` 旧常量                                      | `PropertyNamingStrategies.*`                          | [PropertyNamingStrategies](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/PropertyNamingStrategies.java) | 潜在   |
| `jackson-datatype-jsr310` / `-jdk8` / `-module-parameter-names`        | 3.0 起内建于 databind，删除依赖与模块注册             | [jackson-databind#1774](https://github.com/FasterXML/jackson-databind/issues/1774)                                                                    | 潜在   |
| `Jackson2ObjectMapperBuilderCustomizer`                                | `JsonMapperBuilderCustomizer`                         | [Boot：定制 JsonMapper](https://docs.spring.io/spring-boot/how-to/spring-mvc.html#howto.spring-mvc.customize-jackson-jsonmapper)                      | 潜在   |

**非禁用说明行**：`com.fasterxml.jackson.annotation.*` 在 Jackson 3 **保留原名**（3.x 复用 annotations 2.21 构件），
`src/main` 中该包 import 是 **正确写法**，不得误判为 Jackson 2 残留
（[Jackson 3.0 Release Notes「Annotations」节](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)）。

## 2. 接入与共享

本仓 **不持有 mapper**：`src/main` 全量扫描零
`tools.jackson.databind` /
`tools.jackson.core` import，装配全权由 Spring Boot 自动配置承担。因此本节规则以 **禁止性条款**形态落地——禁止的理由是 3.0
起
mapper 不可变（`ObjectMapper` 无配置 setter）且实例线程安全、全应用共享一份即可。

| 规则                                                                                                                                        | 级别     | 来源          | SODA 落地                                                                                                                                                |
|---------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| mapper 一律经 `JsonMapper.builder(...).build()` 构建、配置只在 builder 上做；3.0 起实例不可变且线程安全，全应用共享复用一个实例，不逐次新建 | 【强制】 | `JACKSON-DOC` | **守现状**：`src/main` 零 mapper 定义、零 Jackson API 调用 ⇒ 落地形态＝main **不 `new`、不 builder**；共享实例由 Boot 自动配置提供                        |
| 格式专用 mapper 必须用对应子类型（JSON 即 `JsonMapper`）；`new ObjectMapper(格式 factory)` / `MappingJsonFactory` 已不允许                  | 【强制】 | `JACKSON-DOC` | 本仓无带参 `new ObjectMapper(`、无 `MappingJsonFactory`；该反模式同时入 §1.2 禁用清单                                                                     |
| 需要定制 mapper 时经 Boot 的 `JsonMapperBuilderCustomizer` bean 追加配置，不替换自动配置的 mapper bean、不散落配置                          | 【推荐】 | `JACKSON-DOC` | 无定制需求：`src/main` 无 customizer bean，`application.yml` 无 `spring.jackson.*` 键；该扩展点保留为唯一定制面                                            |

❌ 在 main 里自建 mapper——与 Boot 提供的实例并存，出现第二套配置口径，且绕过 `spring.jackson.*`：

```java
// ❌ 自建实例，配置面漂移
private final JsonMapper mapper = JsonMapper.builder().build();

// ✅ 注入 Boot 提供的共享实例；要定制就加一个 JsonMapperBuilderCustomizer bean
private final JsonMapper mapper;   // 构造器注入
```

## 3. 注解落位

形态总表（record / class / enum / 密封族如何取注解） **单源**
在 [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)
——本节只写注解本身的落位机制、组合与适用面；Entity 的序列化契约（字段可见性、恢复构造器、多态分派）单源在
[framework-type-contracts](framework-type-contracts.md#entity)。

| 规则                                                                                                                                         | 级别     | 来源            | SODA 落地                                                                                                                                                    |
|----------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| record 典范构造器即 creator，是 Jackson 内建的 creator 选择结果；多字段 record 的属性绑定按 component 名推断，故无需也**不得**补显式声明     | 【强制】 | `JACKSON-DOC`   | 全部 record 零 `@JsonCreator`；**各形态取哪套注解**单源在 [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)，本篇不重述该表                |
| 构造器参数名检测 3.0 起内建（`MapperFeature.DETECT_PARAMETER_NAMES` 默认启用），前提是 class 以 `-parameters` 编译                           | 【推荐】 | `JACKSON-DOC`   | `-parameters` 在根 `build.gradle` 的 `subprojects` 块声明、覆盖全模块；该开关是**全部 record 免注解绑定的前提**，移除即静默破坏组件名绑定                     |
| 标量 delegating 入口显式 `@JsonCreator(mode = JsonCreator.Mode.DELEGATING)`（适用面＝class + private 构造器形态）                            | 【推荐】 | `JACKSON-DOC`   | class 侧在用该形态；record 形态经接口继承 `@JsonValue` 得同能力（例外脚注见[研究篇](../research/foundation-deps-official-practices.md#jackson)）（ADR-0028）    |
| 显式属性型 creator：每个参数标非空 `@JsonProperty`（名不得为空串；单参无 `@JsonProperty` 会被判为 delegating）                               | 【强制】 | `JACKSON-DOC`   | 在用 `PROPERTIES` creator 的参数全部带 `@JsonProperty` 名；裸 `@JsonCreator` 多参形态亦全带名                                                               |
| 非空 creator 参数标 `@JsonProperty(required = true)`；缺字段由 Jackson 边界拒绝，null 值另由 `FAIL_ON_NULL_CREATOR_PROPERTIES`（默认关）控制 | 【推荐】 | `JACKSON-DOC`   | 非空 creator 参数逐参标 `required = true`；`FAIL_ON_NULL_CREATOR_PROPERTIES` **不开**属有意设计——可空字段走 `@Nullable`，缺 id 由 `required` ＋ `assignId` 双屏障拦截（ADR-0015） |
| `@JsonValue` 至多一个 accessor；enum 声明时兼作反序列化取值；POJO 用它须配对 `@JsonCreator`                                                  | 【强制】 | `JACKSON-DOC`   | 字面量家族接口与 `UpdateMask` 组件各自落在唯一 accessor、无同类型双标；枚举不标 `@JsonValue`（ADR-0028）                                                     |
| `@JsonAutoDetect` 各元素默认 `Visibility.DEFAULT`；显式非默认用法受支持，但必须能说出动机                                                    | 【强制】 | `JACKSON-DOC`   | 落点 `Entity.java`：`fieldVisibility = ANY` ＋ `getterVisibility` / `isGetterVisibility` / `setterVisibility = NONE`——`isGetterVisibility = NONE` 屏蔽 `isIdentified()` 派生谓词被序列化为 `identified` 属性，`fieldVisibility = ANY` 为输出 private 字段；子类经继承沿用，不重复标注 |
| 逻辑维度隐藏用 `@JsonIgnore`；结构性非 wire 字段与 `transient` 双标                                                                          | 【推荐】 | `JACKSON-DOC`   | 落点 `Aggregate.java`（`domainEvents` 是领域事件收集器、非线格式数据，且在 `fieldVisibility = ANY` 下会被默认序列化 ⇒ 结构性必要）；未知字段容忍度依赖 §5 的 3.0 默认，策略归契约层 |

❌ 同一类型上出现两个 `@JsonValue`——Jackson 无法裁决用哪个，序列化结果取决于发现顺序：

```java
// ❌ 两个 accessor 都是值入口
@JsonValue
public String value() {
    return value;
}

@JsonValue
public String normalized() {
    return value.strip();
}

// ✅ 唯一 accessor（形态取舍见 dp-json-conventions §1 模式总表）
@JsonValue
public String value() {
    return value;
}
```

❌ 实体恢复构造器漏 `required = true`——缺字段的 JSON 静默构造出瞬态实体，错误从边界推迟到持久化：

```java
// ❌ 缺字段不拒绝
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
private User(@JsonProperty("id") UserId id, @JsonProperty("version") ConcurrencyVersion version, …) { …}

// ✅ 非空字段标 required = true（契约与双屏障见 framework-type-contracts「Entity」）
@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
private User(@JsonProperty(value = "id", required = true) UserId id,
             @JsonProperty(value = "version", required = true) ConcurrencyVersion version, …) { …}
```

## 4. 多态与类型标识

类型 id 一律用 **逻辑名**而非类名：线格式因此不暴露类路径（`Id.CLASS` 对不可信输入是攻击面），判别串同时也是领域词汇。本仓只有
一处多态实体层级（`AuthAccount`），sealed 的 `permits` 子句让子类注册免手工维护。

| 规则                                                                                                                                           | 级别     | 来源                    | SODA 落地                                                                                                                                                                     |
|------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@JsonTypeInfo` 的 `use` 无默认值（必填）；`include` 默认 `As.PROPERTY`、`property` 默认空串、`visible` 默认 false                             | 【强制】 | `JACKSON-DOC`           | 落点 `AuthAccount.java`（`use = Id.NAME` ＋ `property = "authAccountType"`）；`include` / `visible` 取默认，与两个构造器均无判别参数一致（判别属性被消费、不进构造器）（ADR-0004 / ADR-0006） |
| 类型 id 用逻辑名 `Id.NAME` ＋ `@JsonTypeName`（或注册子类型）建立映射；**禁止** `Id.CLASS` 处理不可信输入                                      | 【强制】 | `JACKSON-DOC`           | `Id.NAME` ＋ `@JsonTypeName` 完整覆盖 `permits` 子类（`P` / `S` / `E` / `O`），零 `Id.CLASS`（ADR-0006 / ADR-0016）                                                            |
| Java 17 sealed 类型的 `permits` 子类由 Jackson 自动发现，无需手工 `@JsonSubTypes`；不采用 `Id.DEDUCTION`（显式判别串可对不可信输入施加白名单） | 【推荐】 | `JACKSON-DOC`           | 无 `@JsonSubTypes`（合规）；`Id.DEDUCTION` 未使用（ADR-0006）                                                                                                                  |
| 多态严格性保持默认：`REQUIRE_TYPE_ID_FOR_SUBTYPES` ＋ `FAIL_ON_INVALID_SUBTYPE` 均启用 ⇒ 缺 / 非法 type id 抛异常                              | 【强制】 | `JACKSON-DOC`           | 无配置覆盖（`application.yml` 无 `spring.jackson.*` 键）⇒ 默认生效；行为由测试锁定（未知判别符拒绝）                                                                            |
| 判别串与领域枚举短名一一对应——判别串是边界合同的一部分，不只是 Jackson 的元数据                                                                | 【强制】 | `SODA-DECISION`（ADR-0016） | `@JsonTypeName("P"/"S"/"E"/"O")` 对照 `AuthAccountType` 常量；反射测试锁定 permits 完备 / 判别值唯一 / 与枚举 `name()` 一致（类型契约见 [framework-type-contracts](framework-type-contracts.md#entity)） |

❌ 手工登记子类型并用类名做 type id——线格式耦合全限定类名，且新增子类时漏登记只会在运行时暴露：

```java
// ❌ 类名 id + 手工子类型表
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonSubTypes(@JsonSubTypes.Type(PasswordAuthAccount.class))
public abstract sealed class AuthAccount<ID extends AuthAccountId> permits … {}

// ✅ 逻辑名 + permits 自动发现，判别串与领域枚举短名一致
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authAccountType")
public abstract sealed class AuthAccount<ID extends AuthAccountId> permits PasswordAuthAccount, SmsAuthAccount, … {}

@JsonTypeName("P")
public final class PasswordAuthAccount extends AuthAccount<PasswordAuthAccountId> {
}
```

## 5. 3.0 行为基线

Jackson 3.0 反转了一批默认值；本仓 **全部保持默认、不加配置**（配置面零 `spring.jackson.*` 键）。【强制】项的共同性质是：
一旦被反转就静默改变线格式或改变拒绝行为——这些默认值就是合同，配置面越少越安全。

| 规则                                                                                                                                                                           | 级别     | 来源            | SODA 落地                                                                                                                                                                                          |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enum 按 `toString()` 读写（3.0 起默认启用，2.x 用 `name()`）；业务枚举**不得覆写 `toString()`**——短名即对外合同                                                                | 【强制】 | `JACKSON-DOC`   | 业务枚举零 `toString()` 覆写 ⇒ wire 值 ≡ `name()` 短名，与持久化短名一致；`ACCEPT_CASE_INSENSITIVE_ENUMS` 默认关（ADR-0005）                                                                          |
| null 绑 primitive 抛异常：`FAIL_ON_NULL_FOR_PRIMITIVES` 3.0 起默认启用，**保持启用**                                                                                           | 【强制】 | `JACKSON-DOC`   | primitive wire 字段全为输出或任务参数语义，无「null 应静默转 0」场景；入站可空用包装类型                                                                                                            |
| 尾随 token 拒绝：`FAIL_ON_TRAILING_TOKENS` 3.0 起默认启用，**保持启用**                                                                                                        | 【强制】 | `JACKSON-DOC`   | 无配置覆盖；`src/main` 不引用 `DeserializationFeature`（无从关闭）                                                                                                                                  |
| 空 bean 不报错：`FAIL_ON_EMPTY_BEANS` 3.0 起默认关闭，**保持关闭**                                                                                                             | 【强制】 | `JACKSON-DOC`   | `SecretValue` 输出 `{}` 的前提（测试断言 `isEqualTo("{}")`）；旧知识下误开即破坏该行为（ADR-0030）                                                                                                |
| 未知字段容忍：`FAIL_ON_UNKNOWN_PROPERTIES` 3.0 起默认关闭，保持默认；严格化属契约层决策                                                                                        | 【推荐】 | `JACKSON-DOC`   | 无配置覆盖 ⇒ 默认容忍；是否 strict 归 [aip-api-conventions](aip-api-conventions.md)                                                                                                                 |
| 大小写敏感保持默认（`ACCEPT_CASE_INSENSITIVE_PROPERTIES` / `_ENUMS` 默认关）；少数已知别名用 `@JsonAlias` 精确声明                                                             | 【推荐】 | `JACKSON-DOC`   | 无 `@JsonAlias`、无 `@JsonNaming`（合规）                                                                                                                                                           |
| JSR-310 内建于 databind（无需 `jackson-datatype-jsr310`）；时间戳 feature 3.0 起默认关 ⇒ ISO-8601 文本                                                                         | 【推荐】 | `JACKSON-DOC`   | 构建不引入 `jackson-datatype-jsr310`；裸 `Instant` / `Duration` 组件按默认 ISO-8601；领域时间点走 `EpochMilli`（毫秒 long）显式覆盖（域契约见 [dp-json-conventions](dp-json-conventions.md)）（ADR-0031） |
| BigDecimal 不出 wire：金额 / 精度敏感值以 `String`（`toPlainString()`）承载，不用 Jackson 的 BigDecimal 特性                                                                   | 【推荐】 | `JACKSON-DOC`   | 规范值 String、`BigDecimal` 仅作派生缓存 ⇒ 对全部 BigDecimal feature 默认值变更免疫；域契约单源见 [dp-json-conventions §3](dp-json-conventions.md#3-bigdecimal-dp-模式)（ADR-0031）                 |
| 属性排序：`SORT_PROPERTIES_ALPHABETICALLY` 3.0 起默认启用——普通 POJO 走字母序，record 组件与带 `PROPERTIES` creator 的 class 保持声明序                                        | 【参考】 | `JACKSON-DOC`   | `Entity` 族有显式 creator ⇒ wire 序＝声明序；只有新增**无 creator 的 POJO** 才会遭遇字母序                                                                                                          |
| 命名策略经 builder `propertyNamingStrategy(...)` 或 `spring.jackson.property-naming-strategy`；`ALLOW_EXPLICIT_PROPERTY_RENAMING` 默认关 ⇒ 显式 `@JsonProperty` 名不被策略改写 | 【参考】 | `JACKSON-DOC`   | 无策略配置：`@JsonProperty` 名逐字等于 Java 名，故显式名为冗余声明；**字段命名口径（camelCase vs lower_snake_case）不属本篇**，归 [aip-api-conventions](aip-api-conventions.md) §4.1 契约层          |
| Jackson 2 兼容开关**不得开启**：`builderWithJackson2Defaults()` / `spring.jackson.use-jackson2-defaults=true`                                                                  | 【参考】 | `JACKSON-DOC`   | 绿地 Jackson 3，本仓无 Jackson 2 迁移面：无残留需要兼容，开启反而回退本表前四项【强制】默认值                                                                                                      |
| 逐属性 null 语义（`@JsonSetter(nulls = …)`）与序列化侧 null 省略（`@JsonInclude` / `spring.jackson.default-property-inclusion`）是**机制**，是否省略 null 属契约层决策         | 【参考】 | `JACKSON-DOC`   | 无该类声明 ⇒ 出站按默认输出 `null`；策略归 [aip-api-conventions](aip-api-conventions.md) §4.6                                                                                                       |

❌ 覆写 `toString()`——Jackson 3 起 wire 值随之改动，短名合同被静默改写（且与持久化短名脱钩）：

```java
// ❌ 线值随 toString() 漂移
public enum UserState implements StateEnumType {
    E("enabled") {
        @Override
        public String toString() {
            return "enabled";
        }
    };
}

// ✅ 不覆写：wire 值即 name() 短名
public enum UserState implements StateEnumType {E("enabled"), D("disabled"), R("deregistered");}
```

❌ 为「更严格」打开 `FAIL_ON_EMPTY_BEANS`——`SecretValue` 的 `{}` 输出会变成异常：

```yaml
# ❌ 旧知识下的加固，破坏秘密值载体
spring.jackson.serialization.fail-on-empty-beans: true

# ✅ 保持 3.0 默认（关闭）；需要严格性时逐类型处理，不改全局开关
```

## 6. 偏离表

本库无偏离：逐条规则审计后无需登记例外的行为偏离；个别「部分合规」经实测确认合规——成因是规则文字与本栈形态不合，落在 §3、§5 对应规则行（如 record 经接口继承 `@JsonValue` 的例外脚注）。

## 检视清单

每条可判定，括号为规则落点（处置词表与逐项回放见 [doc-conventions §7.2](../doc-conventions.md#72-检视清单服务迁移审计)）：

### 代码

1. 新增 / 修改类型的注解落位与 [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)逐行一致（§3）
2. 多字段 record 零 `@JsonCreator`、零 `@JsonProperty`；单字段 record 的 `@JsonValue` 标在 component 上（§3）
3. class + private 构造器有显式 `@JsonCreator(mode = DELEGATING)`；枚举提供 `of(String)` 入口（§3）
4. `PROPERTIES` creator 每参带非空 `@JsonProperty`；非空字段带 `required = true`（§3）
5. 实体新增字段已同步补恢复构造器参数，且 round-trip 全等 ＋ 非法 JSON 拒绝断言齐备（§3）
6. 每处 `@JsonAutoDetect` / `@JsonIgnore` 都能说出动机，未被当作冗余删除（§3）
7. 多态基类 `use = Id.NAME` 且 `property` 显式命名；每个 `permits` 子类有 `@JsonTypeName` 且与领域枚举短名一致（§4）
8. 无 `Id.CLASS`、无 `@JsonSubTypes`、无 `@JsonTypeName` 遗漏或多余（§4）
9. 业务枚举无 `toString()` 覆写；wire 值＝`name()` 短名（§5）
10. 未新增 `spring.jackson.*` 键；无 mapper 自建（`new` / builder）（§2）
11. `src/main` 仍无 `tools.jackson.databind` / `tools.jackson.core` import（§2）
12. `com.fasterxml.jackson.annotation.*` 未被替换成其它包名（§1.2 说明行）
13. wire 相关改动有对应断言，且断言不依赖 `toString()` 覆写或属性输出顺序（§5）

### 依赖与构建声明

14. `build.gradle` 的 `-parameters` 未被移除，且仍在 `subprojects` 块内覆盖全模块（§3）
15. 未引入 `jackson-datatype-*` / `jackson-module-parameter-names`，未引入 Jackson 2 构件线（§1.2）
16. 未开启 `spring.jackson.use-jackson2-defaults`（§5）

### 序列化边界（库专属）

17. 多态判别串变更时，反射一致性测试（permits 完备 / 判别值唯一 / 与枚举 `name()` 一致）同步更新（§4）
18. 敏感 / 秘密值的序列化姿态未被放宽：`SecretValue` 仍输出 `{}`，家族 DP 的 `@JsonValue` 仍走接口继承路径（§5）
