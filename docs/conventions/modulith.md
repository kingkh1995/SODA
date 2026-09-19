---
type: Convention
title: Spring Modulith 使用规范
description: 写/评审 Spring Modulith 模块声明与校验（`@ApplicationModule`、`allowedDependencies` 白名单、verify 与反向校验边界、模块可引用性）时读——P3C 三级 + ❌/✅ + 双清单；模块清单与治理在 framework-conventions §2 / §3；版本基线与禁用清单见 §1。
tags: [ spring-modulith, convention ]
status: stable
---

# Spring Modulith 使用规范

> **本篇**＝模块声明与依赖校验的定案与落地：§1 版本基线 → §2 模块声明与可见性（含 §2.4 可引用性）→ §3 依赖校验与治理 → §4
> 偏离表。
> **官方事实全量与版本锚点溯源**归
> [research/foundation-deps-official-practices.md § Spring Modulith](../research/foundation-deps-official-practices.md#spring-modulith)
> ——本篇不设来源章，来源列只写一级链接。
> **关联规范**：[framework-conventions](../framework-conventions.md) §2（模块清单与分层 starter 的构建结构）/
> §3（治理句与新增模块步骤）；[spring.md](spring.md) §5（事件发布与监听的事务语义）；
> [jspecify.md](jspecify.md)（包级 `@NullMarked` 与空性缺口）；[test-conventions](../test-conventions.md)
> （测试侧写法）；ADR：[0008](../adr/0008-layered-component-starters.md)（分层 starter）、
> [0019](../adr/0019-adapter-starter-channel-split.md)（adapter
> 通道拆分）、[0011](../adr/0011-verification-cross-module-domain.md)（验证跨模块领域与弱保证语义）。

## 适用范围与管辖面

管 `src/main` 的模块声明与依赖边界：18 处 `src/main` 的 `package-info.java` 的 `@ApplicationModule` 属性、
`allowedDependencies`
白名单、named interface 暴露面、`type` 判据、模块可引用性（入边，§2.4），以及
`ApplicationModules.of(...)` 校验实例实际覆盖的范围。另有 1 处声明位于 `soda-support-modulith` 的 `src/testFixtures`
（模块结构校验工具本身，见
§3.3）——合计 **19 处**，即校验实例枚举到的模块总数。

**`ModulithTest` 与 `ModulithTestSupport` 的测试侧用法归 [test-conventions](../test-conventions.md)，本篇只固化 main
侧契约**（模块声明与治理句），正文不描述测试写法。

何时 **不**读：

- 写 `src/test`（`ModulithTest` / `ModulithTestSupport` 的调用与断言） —— [test-conventions](../test-conventions.md)
- 判「分层 starter 怎么切、业务模块引哪几行」 —— [framework-conventions](../framework-conventions.md) §2（构建结构）
- 判「事件监听的事务语义、回调异常吞没、补偿怎么写」 —— [spring.md](spring.md) §5
- 判包级空性标注与缺口 —— [jspecify.md](jspecify.md)
- 要官方规则原文、未启用能力（Event Publication Registry / 事件外部化 / 文档生成 / 模块级 Flyway /
  `@ApplicationModuleTest`） ——
  [research 篇](../research/foundation-deps-official-practices.md#spring-modulith)

## 写清单

新增或修改 `src/main` 模块声明时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                                                                           |
|----|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | 新建模块先定 `type`：默认 `CLOSED`；仅当「本模块零出边＋被 ≥2 个模块依赖」同时成立才用 `OPEN`（§2.2）                                                                  |
| 2  | `type = CLOSED` 时把所需模块写进 `allowedDependencies`，只写逻辑名（相对 modulith 根包，非裸子包名）（§2.3）                                                           |
| 3  | 分清 `{}` 与留空：`{}`＝禁止依赖任何他模块，留空＝不限制（§2.3）                                                                                                       |
| 4  | 暴露子包加 `@NamedInterface`；名＝包本地名单数（`event` 非 `events`）；白名单 `domain::event` 引用；非法目标解析期拒绝（§2.3）                                         |
| 5  | 不新引入嵌套模块（本仓 0 个）；starter 族子包一律 internal，仅根包即 API（§2.3 / §2.4）                                                                                |
| 6  | 模块 `package-info.java` 写一行职责 Javadoc（模块描述的事实源）（§2.1）                                                                                                |
| 7  | 新增跨根引用写入消费方 FQN 白名单（`verify()` 会拦），并按 §3.1 判据定性（分层复用合法、业务互引违规）（§3.1）                                                         |
| 8  | 白名单预留项注明用途与空壳通道身份；非空壳模块的预留项不跨版本存续，删除前按 `DependencyDepth.ALL` 口径复核是否真未使用（§3.3）                                        |
| 9  | 模块结构校验工具放 `src/testFixtures`（`testFixturesApi` 引 core + AssertJ），消费方经 `testFixtures(...)` 引入；main 为空（§3.3）                                     |
| 10 | 新增事件监听器用 `AFTER_COMMIT`，有落库副作用时叠 `REQUIRES_NEW`；不写 `fallbackExecution`（§3.2）                                                                     |
| 11 | 每个应用根配且仅配一个 Modulith 一致性验证测试，委托 `CrossRootModules.verify()`（§3.3）                                                                               |
| 12 | §1：BOM / 直接依赖升级时逐项复核版本表与 1.2 清单，改动就地更新                                                                                                        |
| 13 | 新建模块先判**可引用性**：入口 / 通道模块（adapter 族、`start`、`queryserver`）整模块不可被引用，由接入方 `runtimeOnly` 封禁，且**豁免**「根包＝外部契约」约束（§2.4） |
| 14 | 完成判据：按文末检视清单逐条自检通过，本篇零跨文档重复，来源列均为一级 markdown 链接                                                                                   |

## 1. 版本基线

### 1.1 版本

| 项                          | 值                             | 依据                                                                                                           |
|-----------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------|
| Spring Modulith（项目锁定） | **2.1.0**                      | 根 `build.gradle` 的 `set('springModulithVersion', "2.1.0")` 与 `spring-modulith-bom`                          |
| 规范引据版本                | **2.1.1**                      | 参考文档 / Javadoc 取 2.1.1（2.1 线最新稳定版，与项目 2.1.0 同 minor）                                         |
| Spring Boot                 | **4.1.1**                      | 根 `build.gradle` 插件版本；`spring-modulith-core:2.1.0` 的编译基线为 Boot 4.1.0（patch 差）                   |
| Java                        | **25**                         | 根 `build.gradle` toolchain `JavaLanguageVersion.of(25)`                                                       |
| main 侧依赖                 | `spring-modulith-starter-core` | 根 `build.gradle` 的 `subprojects.dependencies`；归属复核见 §3.3                                               |
| 校验强度                    | 测试期                         | 无 `spring.modulith.runtime.verification-enabled` 命中（§1.2）；本仓规则的强制力落在两个应用根的一致性验证测试 |

### 1.2 禁用清单

| 本仓禁用项                                                                                              | 替代                                                             | 依据                                                                                                                                                                                                            | 影响面                                        |
|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `@Modulithic` / `@Modulith`（应用级定制注解）作为**运行期定制**或**校验手段**                           | `CrossRootModules` 单实例作权威校验入口                          | §3.1；`ModulithTestSupport.CrossRootModules` 用 `additionalPackages` + `useFullyQualifiedModuleNames` 并入三根 + FQN 白名单，`verify()` 覆盖全部跨根边（见 [ADR-0042](../adr/0042-dual-root-package-model.md)） | 已落地                                        |
| `spring.modulith.detection-strategy=explicitly-annotated` / 自定义 `ApplicationModuleDetectionStrategy` | 默认 `direct-subpackages` + 显式根包调用                         | §2.1；非默认策略只作为跨根治理的结构性备选手段                                                                                                                                                                  | 潜在                                          |
| `spring.modulith.runtime.verification-enabled=true`（运行期启动校验）                                   | 测试期的一致性验证测试                                           | §3.1；启用前须先解决两个启动类基包（`com.soda.user.start` / `com.soda.user.queryserver`）与测试侧根包视图的不一致                                                                                               | 潜在                                          |
| `@ApplicationEventListener`（1.x 废弃、2.0 已移除）                                                     | `@ApplicationModuleListener` 或手写 `AFTER_COMMIT` 监听器        | 2.0 GA 发布说明（移除 #1397）                                                                                                                                                                                   | 潜在                                          |
| `spring.modulith.republish-outstanding-events-on-restart`（旧名）                                       | `spring.modulith.events.republish-outstanding-events-on-restart` | 官方配置属性表标注 Deprecated as of 1.3                                                                                                                                                                         | 潜在                                          |
| events starter（`spring-modulith-starter-jpa` 等）与 `event_publication` 表                             | 无注册表 + 自建补偿（§4 E3）                                     | §4 E3；本行保留用于解释投递保证的现状                                                                                                                                                                           | 不适用                                        |
| 模块结构校验工具（`ModulithTestSupport`）进 main                                                        | 放 `src/testFixtures`（`testFixturesApi` 引 core + AssertJ）     | §3.3；main 为空则测试栈结构性地进不了任何生产类路径                                                                                                                                                             | 潜在（`soda-support-modulith` 原 1 处已清零） |

维护：BOM / 直接依赖升级时逐项复核本表与 1.2 清单（依据＝官方 release notes / 迁移指南），改动就地更新。

## 2. 模块声明与可见性

### 2.1 声明落点、模块清单与检测策略

模块由 `package-info.java` 的 `@ApplicationModule` 声明； **Gradle 项目与模块数是 N:1**（`soda-component-domain-types` 出
1 个模块、`soda-user-adapter` 出 3 个模块），清单事实源在这 19 处声明，不在 `.gradle`（其中 1 处在 `src/testFixtures`，见
§3.3）。

| 规则                                                                                  | 级别     | 官方来源                                                                                                                                  | SODA 落地                                                                                                                                                                 | 关联 ADR                                                                                                |
|---------------------------------------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| 默认检测：主应用类所在主包的**直接子包**即模块包                                      | 【强制】 | [Fundamentals · Modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.simple)                               | 本仓以**显式根包调用**为准（`ApplicationModules.of("com.soda.component")` / `of("com.soda.user")`），非主应用类派生——`soda-components` 全仓 0 个 `@SpringBootApplication` | —                                                                                                       |
| 模块元数据用 `@ApplicationModule` 声明在 `package-info.java`                          | 【强制】 | [@ApplicationModule Javadoc](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/ApplicationModule.html) | 19/19 落在 `package-info.java`；类型上的 TYPE 形态 0 处                                                                                                                   | —                                                                                                       |
| 模块清单与白名单的事实源是模块声明本身（Gradle 项目数 ≠ 模块数）                      | 【强制】 | [@ApplicationModule Javadoc](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/ApplicationModule.html) | 19 个模块对 17 个叶子项目；N:1 一处：`soda-user-adapter` 出 `consumer` / `job` / `web`                                                                                    | [0008](../adr/0008-layered-component-starters.md)、[0019](../adr/0019-adapter-starter-channel-split.md) |
| 模块描述可从 `package-info.java` 的 Javadoc 提取（2.0 起，文档提取通道）              | 【推荐】 | [2.0 GA 发布说明](https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released)                                     | 19/19 模块声明均有职责 Javadoc（含 `support.test`、`user.{queryserver,start}`）                                                                                           | —                                                                                                       |
| 每个模块的 `package-info.java` 写一行职责 Javadoc                                     | 【推荐】 | [2.0 GA 发布说明](https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released)                                     | 与上行同一落点；Javadoc 与 `package` 声明同段，是本仓模块描述的**事实源**                                                                                                 | —                                                                                                       |
| Modulith 2.x 的 API 空安全迁到 JSpecify；模块声明与之同构                             | 【强制】 | [2.0 GA 发布说明](https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released)                                     | 19 处模块声明全部带 `@NullMarked`；包级缺口与写法单源在 [jspecify.md](jspecify.md) §2.1，本篇不重述                                                                       | —                                                                                                       |
| 默认检测策略 `direct-subpackages`；换用 `explicitly-annotated` 或自定义策略需显式配置 | 【参考】 | [Fundamentals · Customizing modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#customizing-modules)              | 维持默认（否决引入 `explicitly-annotated`，§1.2）；该策略作为跨根治理的结构性备选手段留档                                                                                 | —                                                                                                       |

```java
// ❌ 模块声明齐全但无职责说明：模块描述与文档提取拿不到内容
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.queryserver;

// ✅ Javadoc 与 package 声明同段 —— 一行说清这个模块负责什么
/**
 * 读服务（混装风格）包 —— 查侧 Controller + Service + DAO，不经写侧分层。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.queryserver;
```

### 2.2 `type` 判据：默认 CLOSED

`CLOSED` 是默认值，也是本仓的默认选择；`OPEN` 需要同时满足两条准入条件（见下行「OPEN 准入判据」）才成立。

| 规则                                                                                                                                   | 级别     | 官方来源                                                                                                       | SODA 落地                                                                                                         | 关联 ADR |
|----------------------------------------------------------------------------------------------------------------------------------------|----------|----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|----------|
| `type = CLOSED`（默认）语义：其他模块只能引用基包（unnamed interface）与显式 `@NamedInterface` 包；其余子包视为 internal，被引用即违规 | 【强制】 | [Fundamentals · Advanced](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.advanced) | 18 处显式 `CLOSED`（含以 4 个 `@NamedInterface` 子包对外暴露的 `user.domain`）                                    | —        |
| `type = OPEN` 语义：所有子包并入 unnamed interface，跨模块可直入模块内部                                                               | 【强制】 | [Fundamentals · Open modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.open) | 4 处（`component.api` / `component.domain` / `user.api` / `support.test`），全为零出边模块                        | —        |
| OPEN 的官方定位：主要用于遗留代码库渐进迁移；正式模块化应用中出现 OPEN 通常暗示打包结构次优                                            | 【推荐】 | [Fundamentals · Open modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.open) | 收窄后保留的 4 处 OPEN 全部满足下行准入判据                                                                       | —        |
| **OPEN 准入判据**：仅当**同时**满足「本模块零出边（自身不依赖任何他模块）」＋「被 ≥2 个模块依赖」才可用 OPEN；否则必须 CLOSED          | 【强制】 | —（项目自定义）                                                                                                | 4 处 OPEN 全满足；`user.domain` 持有到 `component.*` 的跨根出边，按判据维持 `CLOSED` + 4 个子包 `@NamedInterface` | —        |

```java
// ❌「api/domain 一律 OPEN」：给一个有出边的业务模块开 OPEN，子包边界随之消失
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.soda.user.domain;   // 持有出边（含跨根）⇒ 不满足「零出边」准入条件

// ✅ 默认 CLOSED，白名单承接依赖；OPEN 只留给「零出边 + 被广泛依赖」的模块
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api", "domain"})
package com.soda.user.application;
```

`user.domain` 即该 ❌ 形态的实例：收窄后为 `CLOSED` + `allowedDependencies = {}`，4 个子包各带 `@NamedInterface`
对外暴露（§2.3）。

### 2.3 依赖白名单与 named interface

`allowedDependencies` 是模块 **出边**的白名单；`{}` 与留空语义相反，逻辑名与 FQN 不能混写。

| 规则                                                                                                                                                                          | 级别     | 官方来源                                                                                                                                                   | SODA 落地                                                                                                                                                                                                                                                                                                                                                                                                             | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `allowedDependencies = {}`（空数组）＝**禁止依赖任何他模块**；**留空**（默认哨兵值）＝完全不限制——两者语义相反                                                                | 【强制】 | [@ApplicationModule#allowedDependencies](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/ApplicationModule.html)      | 7 处 `{}`（零出边模块：`component.{api,domain,start}`、`support.test`、`user.{api,domain,start}`）；19 处无一处留空                                                                                                                                                                                                                                                                                                   | —        |
| 白名单名有两种命名制，**不可混写**：短逻辑名（相对 modulith 根包）vs **FQN**（完整包名，须配 `@Modulithic(useFullyQualifiedModuleNames = true)`）                             | 【强制】 | [@ApplicationModule#allowedDependencies](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/ApplicationModule.html)      | **本仓用 FQN**（14 处非空白名单全部为完整包名）——FQN 是唯一能表达并强制**跨根**依赖的命名制（§3.1）；代价是单根实例不再能 `verify()`                                                                                                                                                                                                                                                                                  | —        |
| named interface 用 `${moduleName}::${namedInterfaceName}` 引用；identifier 必须指向**已存在**的模块或 named interface，否则解析期抛 `IllegalArgumentException`                | 【强制】 | [AllowedDependency#of](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/core/ApplicationModule.AllowedDependency.html) | 6 条 `::` 白名单项：`user.application` → `{domain::types, domain::gateway, domain::service, domain::event}`、`user.infrastructure` → `{domain::types, domain::gateway}`。**`OPEN` 不豁免 `::`**（实测：`user.domain` 改 OPEN 后删 `::` 仍 FAIL `depends on named interface(s) 'domain :: types'`）——子包一旦有 `@NamedInterface`，消费方就必须显式写 `::`；要免掉 `::` 须**移除**子包的 `@NamedInterface`（实测通过） | —        |
| named interface 用 `@NamedInterface` 标在子包 `package-info.java` 或类型上；`value` / `name` 互为别名，包上默认取包本地名；`propagate`（默认 `true`）把签名中相关类型一并纳入 | 【推荐】 | [@NamedInterface Javadoc](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/NamedInterface.html)                        | **4 处**（全在子包 `package-info.java`，显式取值与包本地名一致）：`user.domain.{types,gateway,service,event}`——**全部有消费方 `::` 白名单引用，是唯一承载业务的 named interface 族**。starter 族（`component.*`）**不使用** named interface：根包即 API，子包一律 internal                                                                                                                                            | —        |
| 嵌套模块：显式标注子包即成为嵌套模块；子模块可访问父模块（含 internal），父/兄弟模块可访问子模块的暴露类型                                                                    | 【强制】 | [Fundamentals · Nested modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.nested)                                         | **本仓 0 个嵌套模块**——`component.domain.{types,util,gateway}` 已撤销 `@ApplicationModule`，改为跟随父模块 `component.domain`（OPEN）一并开放。**不要**新引入嵌套模块：顶级模块不得依赖他人嵌套模块（`Invalid sub-module reference`，实测），单根化时会成批违规                                                                                                                                                       | —        |

```java
// ❌ 把 {} 当「默认 / 不限制」写：模块却 import user.domain —— verify() 直接报白名单违规
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {})
package com.soda.user.application;   // 实际依赖 api 与 domain 的 4 个 named interface

// ✅ {} 就是「禁止依赖任何他模块」，只给零出边模块用；有依赖就逐个写出（含 :: 项）
@ApplicationModule(type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {"api", "domain", "domain::types", "domain::gateway", "domain::service", "domain::event"})
package com.soda.user.application;
```

```java
// ❌ 裸子包名：白名单解析期抛 IllegalArgumentException（No module found with name 'util'）
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"util"})
package com.soda.component.domain.gateway;

// ✅ 相对 modulith 根包的逻辑名；非模块子包并入其所属模块，只能整模块声明（不能写 `types`）
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"domain"})
package com.soda.user.infrastructure;
//   ——component.domain 为 OPEN 且其 types/util/gateway 非模块，故 {"domain"} 即可引用三者全部类型
```

```java
// ❌ CLOSED 模块的子包被跨包直入：既无 @NamedInterface，消费方白名单也无 :: 项
//   user.application 直接 import com.soda.user.domain.event.VerificationCreatedEvent

// ✅ 子包声明 named interface（名字取包本地名单数：event，非 events），消费方白名单用 :: 引用
@NullMarked
@NamedInterface("event")
package com.soda.user.domain.event;

@ApplicationModule(type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {"api", "domain", "domain::types", "domain::gateway", "domain::service", "domain::event"})
package com.soda.user.application;
```

### 2.4 模块可引用性（入边）

`allowedDependencies` 是 **出边**白名单（§2.3）。「谁能引用本模块」是 **入边**，Modulith **不提供生产方声明**——
`type` 与 `allowedDependencies` 都是出边属性， **没有任何模块声明能让本模块拒绝入边**。

| 规则                                                                                                                                                                                                                                                          | 级别     | 官方来源                                                                                                                                   | SODA 落地                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | 关联 ADR                                                                                                |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| 模块依赖校验是**消费方侧**的：`verify()` 拿每个模块自己的 `allowedDependencies` 比对**该模块的出边**；`type = CLOSED` 的语义是「我声明我能引用谁」，**不是**「我声明谁能引用我」                                                                              | 【强制】 | [Verifying Application Module Structure](https://docs.spring.io/spring-modulith/reference/verification.html)                               | 实测：消费方 OPEN（无白名单）→ 生产方 `CLOSED` + `{}` 仍被拒——拦它的是**消费方自己**的空白名单。**不要**指望 `CLOSED` 封住入边                                                                                                                                                                                                                                                                                                                                                      | —                                                                                                       |
| 可引用性的强制手段在**构建期隔离**，三类：① **DI-only 接入**用 `runtimeOnly`——目标模块不进消费方编译类路径，引用即**编译失败**（强于测试期校验）；② **独立部署单元**——全仓无任何模块依赖它；③ **空壳模块**——零类型，无可引用之物                              | 【强制】 | [java-library 插件 · 配置依赖图](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph) | ① adapter 族 + `user.application` + `user.infrastructure`（`soda-user-start/build.gradle` 的 `runtimeOnly project(':soda-user:soda-user-adapter')` 等）；② `soda-user-start` / `soda-user-query-server` / `soda-component-start-starter`（各为 `@SpringBootApplication` 可执行单元，0 个 build.gradle 依赖之）；③ `component.consumer` / `component.queryserver`（仅 `package-info.java`，且当前经 `implementation` 在编译类路径上——**非** `runtimeOnly` 保护，其零引用是空壳所致） | [0008](../adr/0008-layered-component-starters.md)、[0019](../adr/0019-adapter-starter-channel-split.md) |
| **模块可引用性分级**：入口 / 通道模块（`user.{web,job,consumer}`；`user.start`；`user.queryserver`；`component.start`）＝ 整模块**不可被任何模块引用**；其余模块 ＝ 经基包（unnamed interface）可被引用，`user.domain` 另经其 4 个 `@NamedInterface` 子包开放 | 【强制】 | —（项目自定义）                                                                                                                            | `src/main` 中从未成为任何模块引用目标的 **11 个模块**：`user.{web,job,consumer,start,queryserver,application,infrastructure}`、`component.{start,consumer,queryserver}`、`support.test`。其中 `user.{application,infrastructure}` 非入口模块，同属 DI-only 接入（只被 Spring 装配，无编译引用）。**`component.job` 不属本类**——它持有 `JobContext` 且被 `user.job` 引用，是框架层契约持有者                                                                                         | —                                                                                                       |
| 入口 / 通道模块**豁免**「根包＝外部契约」约束：根包即自身入口（`@RestController` / `@Component` / `@SpringBootApplication`），子包放内部实现——本类模块无需为迎合可引用性而重排包结构                                                                          | 【强制】 | —（项目自定义）                                                                                                                            | 现状即此形态，**保持不动**：`UserController`、`TokenCleanupJob`、`UserUpgradeConsumer`、`SodaUserApplication`、`SodaUserQueryServerApplication` 均位于模块根包                                                                                                                                                                                                                                                                                                                      | —                                                                                                       |
| 入边约束当前由**项目约定 + `runtimeOnly`** 承载；未落地专门的「入边无引用」机检                                                                                                                                                                               | 【参考】 | —（项目自定义）                                                                                                                            | 若要机检，须自行枚举各模块出边反推入边（Modulith 无「入边」直查 API，`getDirectDependencies(...)` 只给出边），可见范围＝各 `ApplicationModules.of(...)` 实例**内部**的模块对；跨根入边不在此视野，按 §4 E1 人工核对                                                                                                                                                                                                                                                                 | [0042](../adr/0042-dual-root-package-model.md)                                                          |
| **`component.*` starter 族：只开放根目录**——根包即模块 API（含所有被外部引用的类型），**子包一律 internal**，不加 `@NamedInterface`；跨根场景下 named interface 无法被消费方白名单引用，暴露子包等于无声承诺                                                  | 【强制】 | [Fundamentals · Advanced](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.advanced)                             | 已落地：`AbstractPersistable` / `AbstractAuditable` / `Auditable` 上浮至 `component.infrastructure` 根包；`EnumName` / `MobileNumber` 上浮至 `component.web` 根包；`@NamedInterface` 从 `infrastructure.persistence` / `web.validation` 撤销。`component.api` / `component.domain` 为 OPEN（子包本就是 API，不经 named interface）。`EnumValidator`（校验器实现）与 `JpaAuditingAutoConfiguration`（自动配置）**留在子包**——它们是实现，不是 API                                    | —                                                                                                       |

```java
// ❌ 以为给模块开 CLOSED 就「禁止被他模块引用」：CLOSED 只管出边，对方照样能 import 本模块基包类型
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;   // 真正让本模块不可被引用的是 start 侧接入的 runtimeOnly，不是这一行

// ✅ 不可被引用的模块由接入方 runtimeOnly 封禁；模块自身声明照常写（CLOSED + 出边白名单）
//   soda-user-start/build.gradle:  runtimeOnly project(':soda-user:soda-user-adapter')
```

## 3. 依赖校验与治理

### 3.1 校验语义与覆盖范围

`verify()` 的三条规则作用于 **被传入实例所覆盖的模块集**。本仓用 FQN 命名把三个应用根并入 **同一个**实例（
`CrossRootModules`），跨根依赖因而已进入校验视野。

| 规则                                                                                                                                                                                                                                                             | 级别     | 官方来源                                                                                                                                                                                               | SODA 落地                                                                                                                                                                                                                                | 关联 ADR                                             |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| `verify()` 三条规则：① 模块依赖构成 DAG（无环）② 只能经 API 包（named interface）访问其他模块 ③ 白名单外的模块依赖被拒。**校验范围＝被传入实例所覆盖的模块集**；本仓用 FQN 命名把三个应用根并入 **同一个**实例（`CrossRootModules`），跨根依赖因而已进入校验视野 | 【强制】 | [Verifying Application Module Structure](https://docs.spring.io/spring-modulith/reference/verification.html)                                                                                           | 合并实例 `CrossRootModules.verify()` 为唯一权威入口（19 个模块含跨根边全部纳入）；同根短名实例无法解析 FQN 白名单（`No module found with name 'com.soda.user.api'`）                                                                     | [0019](../adr/0019-adapter-starter-channel-split.md) |
| 官方校验**不含**「声明的白名单项必须被实际使用」——`allowedDependencies` 允许留白                                                                                                                                                                                 | 【强制】 | [Verifying Application Module Structure](https://docs.spring.io/spring-modulith/reference/verification.html)                                                                                           | 与 `ModulithTestSupport` 的断言语义一致；预留项按 §3.3 的期限规则管理                                                                                                                                                                    | —                                                    |
| 反向校验「实际使用但未声明」不是官方 API 语义（官方只有正向的「用错即拒」）                                                                                                                                                                                      | 【推荐】 | [ApplicationModule Javadoc](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/core/ApplicationModule.html)                                                          | `ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules` 自建 fail-fast 双保险；只覆盖 `CLOSED` 模块（`OPEN` 因无边界语义被排除）                                                                                             | —                                                    |
| OPEN 模块**不豁免**白名单校验（官方推导）：OPEN 只放开「可被穿透访问」，声明的白名单仍受约束                                                                                                                                                                     | 【强制】 | [Fundamentals · Open modules](https://docs.spring.io/spring-modulith/reference/fundamentals.html#modules.open) 对照 [Verification](https://docs.spring.io/spring-modulith/reference/verification.html) | 4 处 OPEN 全写 `{}`（零出边）⇒ 该推导是「OPEN 准入判据」的支撑，不另立条款                                                                                                                                                               | —                                                    |
| **跨根依赖已纳入校验**：白名单用 FQN + 单实例覆盖多根 ⇒ 跨根边与同根边同等受 `verify()` 约束                                                                                                                                                                     | 【强制】 | [@Modulithic#useFullyQualifiedModuleNames](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/Modulithic.html)                                                       | 实测：删 `user.api` 的 `"com.soda.component.api"` ⇒ 11 条违规；删 `user.web` 的跨根项 ⇒ 8 条违规。**同根短名实例无法解析 FQN 白名单**（`No module found with name 'com.soda.user.api'`）⇒ 权威 `verify()` 只能在 `CrossRootModules` 上跑 | —                                                    |
| 应用级定制 `@Modulithic`（`systemName` / `sharedModules` / `additionalPackages` / `useFullyQualifiedModuleNames`）与便捷组合注解 `@Modulith`                                                                                                                     | 【参考】 | [@Modulithic Javadoc](https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/Modulithic.html)                                                                            | **本仓用作权威校验实例**：`additionalPackages` 并入三根 + `useFullyQualifiedModuleNames = true` ⇒ 19 个模块（含跨根边）全部纳入 `verify()`。不引入 `@Modulith`、不用于运行期定制                                                         | [0042](../adr/0042-dual-root-package-model.md)       |

### 3.2 事件集成与事务语义

事件监听器的事务语义单源在 [spring.md](spring.md) §5。Modulith 侧仅补充：跨模块直接 bean 引用按 §3.1 判据定性；领域事件是跨模块交互的首选通道。

### 3.3 治理与构建归属

| 规则                                                                | 级别     | 官方来源                                                                                                      | SODA 落地                                                                                                                                                                                                                                                                                                                                                                                                                                           | 关联 ADR                                                |
|---------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| 一致性校验须在测试期显式触发，模块结构才算被门禁覆盖                | 【强制】 | [Verifying Application Module Structure](https://docs.spring.io/spring-modulith/reference/verification.html)  | 治理句：**合并实例 `CrossRootModules.verify()` 为唯一权威校验入口**（19 个模块含跨根边全部纳入）；治理句与新增模块步骤在 [framework-conventions](../framework-conventions.md) §3，测试体写法归 [test-conventions](../test-conventions.md)                                                                                                                                                                                                           | [0042](../adr/0042-dual-root-package-model.md)          |
| 白名单允许预留（`allowedDependencies` 可声明尚未使用的项）          | 【强制】 | [Verifying Application Module Structure](https://docs.spring.io/spring-modulith/reference/verification.html)  | 通道型模块（`component.` / `user.` 的 `consumer` / `job` / `queryserver` 共六处 `{api}`）的预留注明用途后保留——`consumer` 空壳是有意保留（待 MQ 集成，见 [0019](../adr/0019-adapter-starter-channel-split.md)）；非空壳模块的预留项**不得跨版本存续**。**判「某白名单项是否真未使用」须按 `DependencyDepth.ALL`（含传递边）口径复核，不能只看直接 import**——直接 import 为 0 的白名单项仍可能经传递边进入反向校验视野，误删会报 `X->[Y] UNDECLARED` | [0019](../adr/0019-adapter-starter-channel-split.md)    |
| 无注册表时事件投递语义由发布方自行保证（注册表为官方防丢手段）      | 【推荐】 | [Working with Application Events](https://docs.spring.io/spring-modulith/reference/events.html)               | 弱保证基线（§4 E3）：投递失败不可感知，靠「记录保持 INITIALIZED + 用户重发」自愈                                                                                                                                                                                                                                                                                                                                                                    | [0011](../adr/0011-verification-cross-module-domain.md) |
| 模块结构校验工具属测试侧；main 为空则测试栈结构性地进不了生产类路径 | 【强制】 | [java-test-fixtures 插件](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) | 已落地（`soda-support-modulith`）：源码在 `src/testFixtures`，**main 为空**；`testFixturesApi 'spring-modulith-core'` + `testFixturesApi 'org.assertj:assertj-core'`（二者进 fixtures 的 API 面，供消费方测试编译），消费方一律 `testImplementation(testFixtures(project(':soda-supports:soda-support-modulith')))`。断言：任一模块的 main `compileClasspath` 无 `assertj` / `spring-modulith-test` / `archunit`                                    | —                                                       |

```groovy
// ❌ 校验工具放 main：整条测试栈（AssertJ / spring-test / ArchUnit）随之进生产类路径
dependencies {
    implementation 'org.springframework.modulith:spring-modulith-starter-test'
}

// ✅ 源码放 src/testFixtures，main 为空；测试栈只进消费方的测试类路径
plugins { id 'java-test-fixtures' }
dependencies {
    testFixturesApi 'org.springframework.modulith:spring-modulith-core'
    testFixturesApi 'org.assertj:assertj-core'
}
// 消费方：testImplementation(testFixtures(project(':soda-supports:soda-support-modulith')))
```

## 4. 偏离表

| 偏离项                                                                          | 官方做法                                                                                | SODA 做法                                                                                                                                                                                                                                                                                                                                 | 理由                                                                                                                                                                                                                                                                  | 关联 ADR                                                |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| **E1** 双根包建模（`com.soda.component` / `com.soda.user`），跨根依赖无语义校验 | 单根，或用 `@Modulithic(additionalPackages)` 统一建模，全部依赖受 `verify()` 约束       | 两个独立应用根（读写分离的服务边界）；跨根边用 **FQN 白名单 + `CrossRootModules` 合并实例**纳入 `verify()`（§3.1），另有 `assertNoCrossRootCoupling()` 断言方向不变式（框架层不反依赖业务层 / 业务层跨根只指向框架层）。实测跨根 **173 条边 / 18 个模块对**，全部合法、反向 0 条（见 [ADR-0042](../adr/0042-dual-root-package-model.md)） | 双根与 starter 分层是 ADR-0008/0019/0042 的既定结构                                                                                                                                                                                                                   |
| **E2** 手写 `@TransactionalEventListener` 替代 `@ApplicationModuleListener`     | 用 `@ApplicationModuleListener`（`@Async` + `REQUIRES_NEW` + `AFTER_COMMIT`）           | 两处手写监听器：`VerificationCreatedEventHandler` ＝ `REQUIRES_NEW` + `AFTER_COMMIT`（有落库副作用）；`UserUpgradeConsumer` ＝ `AFTER_COMMIT`（纯日志消费者，无落库副作用）。两处均不写 `fallbackExecution`                                                                                                                               | 同步投递是有意选择（验证码投递须在响应前完成，且便于测试执行）。恒定组合＝`AFTER_COMMIT`（必）＋ 有落库副作用时 `REQUIRES_NEW`，两处的差异由职责决定、不是疏漏；`@Async` 按需且必须伴随 `@EnableAsync`——本仓不启用异步执行（[spring.md](spring.md) §5），故两处均不加 | [0011](../adr/0011-verification-cross-module-domain.md) |
| **E3** 无 Event Publication Registry，事件投递为弱保证                          | 引 `spring-modulith-starter-jpa` 启用注册表，配合 `@ApplicationModuleListener` 防丢事件 | 仅 `starter-core`；无 `event_publication` 表；投递失败对调用方不可感知（`afterCompletion` 回调异常被框架吞掉），靠「记录保持 INITIALIZED + 用户重发」自愈                                                                                                                                                                                 | 引入注册表连带 `event_publication` 表、事件序列化与完成模式清理策略（`update` 默认无界增长），属持久化面改动，超出本次改动范围；当前「先落库后投递 + 用户自愈」自洽                                                                                                   | [0011](../adr/0011-verification-cross-module-domain.md) |

## 检视清单

code-review 标准轴按本清单逐条核对（每组逐条可判定，括号内为落点）：

### 代码

1. 每个模块根包有 `package-info.java`，`@ApplicationModule` 落在其上；非声明文件零出现（§2.1）
2. 每个模块的 `package-info.java` 有一行职责 Javadoc，且与 `package` 声明同段（§2.1）
3. 模块 `package-info.java` 带 `@NullMarked`（包级空性单源在 jspecify.md）（§2.1）
4. `type` 显式声明：默认 `CLOSED`；`OPEN` 仅当「零出边＋被 ≥2 个模块依赖」同时成立（§2.2）
5. `allowedDependencies` 全为 **FQN**（完整包名），无短逻辑名混写、无裸子包名（`util` 形态）（§2.3）
6. `{}` 与留空未混用：`{}` 是「禁止依赖任何他模块」，留空是「不限制」（§2.3）
7. `::` 引用的 named interface 在目标模块内确有 `@NamedInterface`，且名称与包本地名一致（`event` 非 `events`）（§2.3）
8. `CLOSED` 模块的 internal 子包未被其他模块直接 import；starter 族（`component.*`）不加 `@NamedInterface`
   ——需暴露的类型上浮至根包（§2.3 / §2.4）
9. 无新增嵌套模块（本仓 0 个）；无 `domain.types` / `domain.util` / `domain.gateway` 一类已撤销的嵌套模块引用残留（§2.3）
10. 事件监听器为 `AFTER_COMMIT`，监听器内需事务性落库时叠 `REQUIRES_NEW`；无 `fallbackExecution`（§3.2 / §4 E2）
11. 入口 / 通道模块未见被其他模块 import；其接入方用 `runtimeOnly`（非 `implementation` / `api`）声明该模块（§2.4）

### 依赖与构建声明

12. 模块结构校验工具在 `src/testFixtures`，main 为空；消费方用 `testFixtures(...)` 引入（§3.3）
13. 无 `@Modulithic` / `@Modulith` / `detection-strategy` / 运行期校验属性 / events starter 的新引入（§1.2）
14. 无 `@ApplicationEventListener` / 废弃配置属性名（§1.2）

### 库专属

| 15. `CrossRootModules.verify()` 为唯一权威校验入口（19 个模块含跨根边全部纳入）；每个应用根有且仅有一个 Modulith
一致性验证测试委托之（§3.3） |
16. 反向校验覆盖全部 `CLOSED` 模块；`OPEN` 模块不计入其覆盖（§3.1）
17. 白名单预留项无跨版本存续；空壳通道（`consumer` / `job` / `queryserver`）例外有据可查；删除预留项前按
    `DependencyDepth.ALL` 口径复核过（§3.3）
    | 18. 新增跨根引用已写进被引用方的 FQN 白名单（`verify()` 会拦），并按 §3.1 判据定性为分层复用而非业务耦合（§3.3 / §4） |
