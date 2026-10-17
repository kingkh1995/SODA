---
type: Convention
title: Spring Modulith 使用规范
description: 写/评审 Spring Modulith 模块声明与校验（`@ApplicationModule`、`allowedDependencies` 白名单、官方 `verify()`、传递依赖门禁、跨根方向政策与模块可引用性）时读——四列规则表 + ❌/✅ + 双清单（doc-conventions §4.3）；模块清单与治理在 framework-conventions §2 / §3；版本基线与禁用清单见 §1。
tags: [ spring-modulith, convention ]
status: stable
---

# Spring Modulith 使用规范

> **本篇**＝模块声明与依赖校验的定案与落地：§1 版本基线 → §2 模块声明与可见性（含 §2.4 可引用性）→ §3 依赖校验与治理 → §4
> 偏离表。
> **官方事实全量与版本锚点溯源**归
> [research/foundation-deps-official-practices.md § Spring Modulith](../research/foundation-deps-official-practices.md#spring-modulith)
> ——本篇不设来源章，规则表「来源」列只写下面来源登记的来源 ID。
> **格式**：【强制 / 推荐 / 参考】三档＋❌/✅ 反例正例＋四列规则表（[doc-conventions §4.3](../doc-conventions.md#43-规则表与示例的表达约定)），形式采纳＝[ADR-0036](../adr/0036-p3c-surface-format-retrofit.md)。
> **关联规范**：[framework-conventions](../framework-conventions.md) §2（模块清单与分层 starter 的构建结构）/
> §3（治理句与新增模块步骤）；[spring.md](spring.md) §5（事件发布与监听的事务语义）；
> [jspecify.md](jspecify.md)（包级 `@NullMarked` 与空性缺口）；[test-conventions](../test-conventions.md)（Modulith
> 测试夹具、架构门禁与诊断入口）。
> 关联 ADR：[0019](../adr/0019-adapter-starter-channel-split.md)（adapter
> 通道拆分）、[0021](../adr/0021-verification-aggregate-root-and-topology.md)（Verification
> 拓扑）、[0044](../adr/0044-local-domain-event-transaction.md)（本地事件事务语义）。

**来源登记**（登记结构与封闭 ID 语义见 [doc-conventions §4.2](../doc-conventions.md#42-来源登记)；本库版本锚点集中本表）：

| 来源 ID         | 含义                                                              | 钉住版本与链接入口                                                                                                                                        |
|-----------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MODULITH-DOC`  | Spring Modulith 官方 reference、Javadoc 与官方发布说明            | 引据规范 **2.1.1**、项目锁定 **2.1.0**（§1.1）；逐条一级链接在 [研究篇 § Spring Modulith](../research/foundation-deps-official-practices.md#spring-modulith) |
| `GRADLE-DOC`    | Gradle 官方 user guide（`java-library` 插件的依赖图与配置语义）   | 随 wrapper 的 Gradle 版本；链接 [java-library 插件 · 配置依赖图](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph) |
| `SODA-POLICY`   | 项目硬门与项目扩展（官方无对应强制条款，或比官方更严）            | 本篇（封闭来源列见 [doc-conventions §4.2](../doc-conventions.md#42-来源登记)）                                                                             |
| `SODA-DECISION` | 已裁定的项目取舍                                                  | 行内 `ADR-NNNN`（落在 `来源` 或 `SODA 落地` 单元格）；理由见 §4 偏离表与对应 ADR 正文                                                                       |

## 适用范围与管辖面

管 `src/main` 的模块声明与依赖边界：各模块 `package-info.java` 的 `@ApplicationModule` 属性、
`allowedDependencies` 白名单、named interface 暴露面、`type` 判据、模块可引用性（入边，§2.4），以及
`ApplicationModules.of(...)` 校验实例实际覆盖的范围。测试源中的 `SodaModulith` 只声明合并扫描配置，不是应用模块。

**`SodaModulithArchitectureTest` 的测试侧用法归 [test-conventions](../test-conventions.md)
，本篇只固化 main
侧契约**（模块声明与治理句），正文不描述测试写法。

何时 **不**读：

- 写 `src/test`（`SodaModulithArchitectureTest` 的调用与断言） —— [test-conventions](../test-conventions.md)
- 判「分层 starter 怎么切、业务模块引哪几行」 —— [framework-conventions](../framework-conventions.md) §2（构建结构）
- 判「事件监听器的事务语义、回调异常吞没、补偿怎么写」 —— [spring.md](spring.md) §5
- 判包级空性标注与缺口 —— [jspecify.md](jspecify.md)
- 要官方规则原文、未启用能力（Event Publication Registry、事件外部化、文档生成、模块级 Flyway、`@ApplicationModuleTest`） ——
  [research 篇](../research/foundation-deps-official-practices.md#spring-modulith)

## 写清单

新增或修改 `src/main` 模块声明时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                                                                                                                                                                                            |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | 新建模块先定 `type`：默认 `CLOSED`；仅当「本模块零出边＋被 ≥2 个模块依赖」同时成立才用 `OPEN`（§2.2）；`user.domain` 是已登记的共享领域 module 例外                                                                                                                                     |
| 2  | `type = CLOSED` 时把所需模块写进 `allowedDependencies`，使用与合并扫描配置一致的 FQN（§2.3）。**留空＝官方不限制、`verify()` 放行；项目传递依赖门禁把“留空且有跨模块依赖”的 CLOSED 模块判红（实测），CLOSED 必须显式写白名单**（[0045](../adr/0045-modulith-gate-minimal-backstop.md)） |
| 3  | 分清 `{}` 与留空：`{}`＝禁止依赖任何他模块，留空＝不限制（§2.3）                                                                                                                                                                                                                        |
| 4  | CLOSED 模块暴露子包时加 `@NamedInterface`；名＝包本地名单数（`event` 非 `events`）；白名单 `domain::event` 引用；非法目标解析期拒绝（§2.3）                                                                                                                                             |
| 5  | 不新引入嵌套模块（本仓零落点）；starter 族子包一律 internal，仅根包即 API（§2.3 / §2.4）                                                                                                                                                                                                |
| 6  | 模块 `package-info.java` 写一行职责 Javadoc（模块描述的事实源）（§2.1）                                                                                                                                                                                                                 |
| 7  | 新增跨根引用写入消费方 FQN 白名单（`verify()` 会拦），并按 §3.1 判据定性（分层复用合法、业务互引违规）（§3.1）                                                                                                                                                                          |
| 8  | 白名单预留项注明用途与空壳通道身份；非空壳模块的预留项不跨版本存续，删除前按项目约定的依赖口径复核是否真未使用（§3.3）                                                                                                                                                              |
| 9  | 全仓 Modulith 门禁的参数与上报机制单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)；本篇只固化 main 侧契约——测试支持包不进应用模块图（§3.3）                                                                                                                 |
| 10 | 本地事件一律普通同步 `@EventListener`；不启用 `@TransactionalEventListener` / `REQUIRES_NEW` / `fallbackExecution`（§3.2；与发布方同事务的事务语义单源 [spring.md §5](spring.md#5-事件) ＋ [ADR-0044](../adr/0044-local-domain-event-transaction.md)）                                       |
| 11 | 新建模块先判**可引用性**：入口 / 通道模块（adapter 族、`start`、`queryserver`）整模块不可被引用，由接入方 `runtimeOnly` 封禁，且**豁免**「根包＝外部契约」约束（§2.4）                                                                                                                  |

## 1. 版本基线

### 1.1 版本

| 项                          | 值                             | 依据                                                                  |
|-----------------------------|--------------------------------|-----------------------------------------------------------------------|
| Spring Modulith（项目锁定） | **2.1.0**                      | 根 `build.gradle` 的 `springModulithVersion` 与 `spring-modulith-bom` |
| 规范引据版本                | **2.1.1**                      | 官方参考文档 / Javadoc；与项目 2.1.0 同 minor                         |
| Spring Boot                 | **4.1.1**                      | 根 `build.gradle` 插件版本                                            |
| Java                        | **25**                         | 根 `build.gradle` toolchain                                           |
| main 侧依赖                 | `spring-modulith-starter-core` | 根 `build.gradle` 的 `subprojects.dependencies`                       |
| 校验强度                    | 测试期                         | 架构门禁在测试期显式触发；入口与结构见 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁) |

### 1.2 禁用清单

| 本仓禁用项                                                                 | 替代                                                             | 依据                                                                                            | 影响面 |
|----------------------------------------------------------------------------|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|--------|
| `@Modulithic` / `@Modulith` 作为运行期定制或校验手段                       | 仅在测试源用 `SodaModulith` 声明合并扫描配置                     | §3.1；合并两个应用根的配置单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁) | 已落地 |
| `spring.modulith.detection-strategy=explicitly-annotated` / 自定义检测策略 | 默认 `direct-subpackages` + 显式根配置                           | §2.1；非默认策略只作为跨根治理的结构性备选手段                                                  | 潜在   |
| `spring.modulith.runtime.verification-enabled=true`                        | 测试期显式执行 `verify()`                                        | §3.3；本仓不在应用启动期执行 Modulith 校验                                                      | 潜在   |
| `@ApplicationEventListener`                                                | 普通同步 `@EventListener`（本地事件按 ADR-0044）                 | 官方 deprecated-list                                                                            | 潜在   |
| events starter 与 `event_publication` 表                                   | 无注册表 + 自建补偿（§4 E3）                                     | §4 E3                                                                                           | 不适用 |
| 模块结构校验工具进入 main                                                  | 仅在测试源显式触发（门禁结构见 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)） | main 为空                                                                                       | 已落地 |

## 2. 模块声明与可见性

### 2.1 声明落点、模块清单与检测策略

模块由 `package-info.java` 的 `@ApplicationModule` 声明； **Gradle 项目与模块数是 N:1**，清单事实源在各 module 的
`package-info.java` 声明，不在 `.gradle`。

| 规则                                                         | 级别     | 来源            | SODA 落地                                                                                               |
|--------------------------------------------------------------|----------|-----------------|---------------------------------------------------------------------------------------------------------|
| 默认检测：主应用类所在主包的直接子包即模块包                 | 【强制】 | `MODULITH-DOC`  | 本仓以显式 `ApplicationModules.of(SodaModulith.class)` 为准；测试运行时显式提供两个应用根的全部模块输出 |
| 模块元数据用 `@ApplicationModule` 声明在 `package-info.java` | 【强制】 | `MODULITH-DOC`  | 所有模块声明均落在 `package-info.java`；类型上的 TYPE 形态不作为声明入口                                |
| 模块清单与白名单的事实源是模块声明本身                       | 【强制】 | `MODULITH-DOC`  | Gradle 项目与 Modulith module 不要求一一对应；事实以 `package-info.java` 与当前依赖图为准（ADR-0008 / ADR-0019） |
| 模块描述可从 `package-info.java` 的 Javadoc 提取             | 【推荐】 | `MODULITH-DOC`  | 模块声明均写职责 Javadoc；测试源中的合并根类型不属于应用模块                                            |
| 每个模块的 `package-info.java` 写一行职责 Javadoc            | 【推荐】 | `MODULITH-DOC`  | 与 `package` 声明同段，模块描述的事实源是源码声明                                                       |
| Modulith 2.x 的 API 空安全迁到 JSpecify                      | 【强制】 | `MODULITH-DOC`  | 模块声明带 `@NullMarked`；包级写法单源在 [jspecify.md](jspecify.md) §2.1                                |
| 默认检测策略 `direct-subpackages`；换用其他策略需显式配置    | 【参考】 | `MODULITH-DOC`  | 维持默认；其他策略仅作为跨根治理的结构性备选手段                                                        |
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

`CLOSED` 是默认值，也是本仓的默认选择；`OPEN` 通常需要同时满足两条准入条件（见下行「OPEN 准入判据」）。`user.domain`
是本仓明确登记的共享领域 module 例外：它保留 OPEN，以允许 application 与 infrastructure 直接使用领域子包。

| 规则                                                                                                              | 级别     | 来源            | SODA 落地                                                              |
|-------------------------------------------------------------------------------------------------------------------|----------|-----------------|------------------------------------------------------------------------|
| `type = CLOSED`（默认）语义：其他模块只能引用基包与显式 `@NamedInterface` 包；其余子包视为 internal，被引用即违规 | 【强制】 | `MODULITH-DOC`  | 作为默认规则                                                           |
| `type = OPEN` 语义：所有子包并入 unnamed interface，跨模块可直入模块内部                                          | 【强制】 | `MODULITH-DOC`  | `user.domain` 按共享领域 module 例外保持 OPEN                          |
| OPEN 的官方定位：主要用于遗留代码库渐进迁移；正式模块化应用中出现 OPEN 通常暗示打包结构次优                       | 【推荐】 | `MODULITH-DOC`  | `user.domain` 的 OPEN 是项目明确接受的共享领域例外，不主张其是最优终态 |
| **OPEN 准入判据**：默认仅当“本模块零出边＋被 ≥2 个模块依赖”同时成立才可用 OPEN；共享领域 module 可登记例外        | 【强制】 | `SODA-POLICY`   | `user.domain` 的例外理由与影响面写入本节                               |

```java
// ❌ 业务 module 有出边却直接 OPEN：子包边界随之消失
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.example.domain;

// ✅ 默认 CLOSED，白名单承接依赖；OPEN 只留给零出边且被广泛依赖的 module
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api", "domain"})
package com.example.application;
```

`user.domain` 当前为 `OPEN`，作为 User 侧共享领域 module，允许 application 与 infrastructure 直接使用 `types`、`gateway`、
`service`、`event` 子包；不引入 named interface。这是通用 CLOSED 规则的项目例外。

### 2.3 依赖白名单与 named interface

`allowedDependencies` 是模块 **出边**的白名单；`{}` 与留空语义相反，逻辑名与 FQN 不能混写。注意留空的官方语义是
“不限制”（`verify()` 放行），而本仓传递依赖门禁把它当“未声明”处理：CLOSED 模块只要跨模块依赖，就必须显式写白名单
（写清单 #2，[0045](../adr/0045-modulith-gate-minimal-backstop.md)）。

| 规则                                                                                                                                                                          | 级别     | 来源            | SODA 落地                                                         |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------|-------------------------------------------------------------------|
| `allowedDependencies = {}`（空数组）＝**禁止依赖任何他模块**；**留空**（默认哨兵值）＝完全不限制                                                                              | 【强制】 | `MODULITH-DOC`  | 具体值以模块声明为准                                              |
| 白名单名有两种命名制：**不可混写**：短逻辑名 vs **FQN**（须配 `@Modulithic(useFullyQualifiedModuleNames = true)`）                                                            | 【强制】 | `MODULITH-DOC`  | 本仓非空白名单使用 FQN，以表达并强制跨根依赖（ADR-0042）          |
| named interface 用 `${moduleName}::${namedInterfaceName}` 引用；identifier 必须指向**已存在**的模块或 named interface                                                         | 【强制】 | `MODULITH-DOC`  | CLOSED module 按需使用 named interface                            |
| named interface 用 `@NamedInterface` 标在子包 `package-info.java` 或类型上；`value` / `name` 互为别名，包上默认取包本地名；`propagate`（默认 `true`）把签名中相关类型一并纳入 | 【推荐】 | `MODULITH-DOC`  | `user.domain` 不使用 named interface；其他 CLOSED module 按需声明 |
| 嵌套模块：显式标注子包即成为嵌套模块；子模块可访问父模块，父/兄弟模块可访问子模块暴露类型                                                                                     | 【强制】 | `MODULITH-DOC`  | 本仓不新增嵌套模块                                                |

`user.domain` 不使用 `@NamedInterface`：它是 OPEN module，`types`、`gateway`、`service`、`event` 等子包随 unnamed interface
对外可见。`::` 规则只适用于 CLOSED module 的显式 named interface。

```java
// ❌ CLOSED 模块用空数组锁死，却直接依赖 user.domain
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {})
package com.soda.user.application;

// ✅ CLOSED 模块使用与合并扫描一致的 FQN；OPEN 的 user.domain 按模块整体声明
@ApplicationModule(type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {"com.soda.user.api", "com.soda.user.domain", "com.soda.component.domain"})
package com.soda.user.application;
```

```java
// ❌ 把 user.domain 的子包当成模块名
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"com.soda.user.domain.types"})
package com.soda.user.application;

// ✅ user.domain 是 OPEN module，声明其模块 FQN 即可覆盖其内部子包
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"com.soda.user.domain"})
package com.soda.user.application;
```

### 2.4 模块可引用性（入边）

`allowedDependencies` 是 **出边**白名单（§2.3）。「谁能引用本模块」是 **入边**，Modulith **不提供生产方声明**——
`type` 与 `allowedDependencies` 都是出边属性， **没有任何模块声明能让本模块拒绝入边**。

| 规则                                                                                                                                                                                                                                    | 级别     | 来源            | SODA 落地                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 模块依赖校验是**消费方侧**的：`verify()` 拿每个模块自己的 `allowedDependencies` 比对**该模块的出边**；`type = CLOSED` 的语义是「我声明我能引用谁」，**不是**「我声明谁能引用我」                                                        | 【强制】 | `MODULITH-DOC`  | 实测：消费方 OPEN（无白名单）→ 生产方 `CLOSED` + `{}` 仍被拒——拦它的是**消费方自己**的空白名单。**不要**指望 `CLOSED` 封住入边                                                                                                                                                                                                                                                                                                                                                       |
| 可引用性的强制手段在**构建期隔离**，三类：① **DI-only 接入**用 `runtimeOnly`——目标模块不进消费方编译类路径，引用即**编译失败**（强于测试期校验）；② **独立部署单元**——全仓无任何模块依赖它；③ **空壳模块**——零类型，无可引用之物        | 【强制】 | `GRADLE-DOC`    | ① adapter 族 + `user.application` + `user.infrastructure`（`soda-user-start/build.gradle` 的 `runtimeOnly project(':soda-user:soda-user-adapter')` 等）；② `soda-user-start` / `soda-user-query-server` / `soda-component-start-starter`（各为 `@SpringBootApplication` 可执行单元，`build.gradle` 无一依赖之）；③ `component.consumer` / `component.queryserver`（仅 `package-info.java`，且当前经 `implementation` 在编译类路径上——**非** `runtimeOnly` 保护，其零引用是空壳所致）（ADR-0008 / ADR-0019） |
| **模块可引用性分级**：入口 / 通道模块（`user.{web,job,consumer}`；`user.start`；`user.queryserver`；`component.start`）＝ 整模块**不可被任何模块引用**；其余模块 ＝ 按各自 module 声明决定；`user.domain` 作为共享领域 module 保持 OPEN | 【强制】 | `SODA-POLICY`   | 入口 / 通道模块的不可引用性由构建期 `runtimeOnly` 与当前依赖图共同保证                                                                                                                                                                                                                                                                                                                                                                                                               |
| 入口 / 通道模块**豁免**「根包＝外部契约」约束：根包即自身入口（`@RestController` / `@Component` / `@SpringBootApplication`），子包放内部实现——本类模块无需为迎合可引用性而重排包结构                                                    | 【强制】 | `SODA-POLICY`   | 现状即此形态，**保持不动**：`UserController`、`TokenCleanupJob`、`UserUpgradeConsumer`、`SodaUserApplication`、`SodaUserQueryServerApplication` 均位于模块根包                                                                                                                                                                                                                                                                                                                         |
| 入边约束当前由**项目约定 + `runtimeOnly`** 承载；未落地专门的「入边无引用」机检                                                                                                                                                         | 【参考】 | `SODA-POLICY`   | 若要机检，须自行枚举各模块出边反推入边（Modulith 无「入边」直查 API，`getDirectDependencies(...)` 只给出边），可见范围＝各 `ApplicationModules.of(...)` 实例**内部**的模块对；跨根入边不在此视野，按 §4 E1 人工核对（ADR-0042）                                                                                                                                                                                  |
| **`component.*` starter 族：只开放根目录**——根包即模块 API（含所有被外部引用的类型），**子包一律 internal**，不加 `@NamedInterface`；跨根场景下 named interface 无法被消费方白名单引用，暴露子包等于无声承诺                            | 【强制】 | `MODULITH-DOC`  | 已落地：`AbstractPersistable` / `AbstractAuditable` / `Auditable` 上浮至 `component.infrastructure` 根包；`EnumName` / `MobileNumber` 上浮至 `component.web` 根包；`@NamedInterface` 从 `infrastructure.persistence` / `web.validation` 撤销。`component.api` / `component.domain` 为 OPEN（子包本就是 API，不经 named interface）。`EnumValidator`（校验器实现）与 `JpaAuditingAutoConfiguration`（自动配置）**留在子包**——它们是实现，不是 API                                    |

```java
// ❌ 以为给模块开 CLOSED 就「禁止被他模块引用」：CLOSED 只管出边，对方照样能 import 本模块基包类型
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;   // 真正让本模块不可被引用的是 start 侧接入的 runtimeOnly，不是这一行

// ✅ 不可被引用的模块由接入方 runtimeOnly 封禁；模块自身声明照常写（CLOSED + 出边白名单）
//   soda-user-start/build.gradle:  runtimeOnly project(':soda-user:soda-user-adapter')
```

## 3. 依赖校验与治理

### 3.1 校验语义与覆盖范围

`verify()` 的三条规则作用于被传入实例所覆盖的模块集；项目另以传递依赖门禁补充白名单反向约束，以跨根方向与应用根存在断言
补足项目政策。本仓由测试源中的 `SodaModulith` 合并两个应用根，跨根依赖因此进入同一校验视野——合并根的登记步骤、断言结构与
上报机制的单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)，本篇只固化 main 侧契约。

| 规则                                                                                  | 级别     | 来源            | SODA 落地                                                                                                          |
|---------------------------------------------------------------------------------------|----------|-----------------|--------------------------------------------------------------------------------------------------------------------|
| `verify()` 检查模块级 DAG、API 包访问边界及 `allowedDependencies` 白名单              | 【强制】 | `MODULITH-DOC`  | `SodaModulith` 覆盖两个应用根声明的全部 module；FQN 白名单在合并实例中解析（ADR-0042）                              |
| 官方校验允许白名单预留，不要求声明项必须被实际使用                                    | 【强制】 | `MODULITH-DOC`  | 预留项按 §3.3 的期限规则管理                                                                                       |
| 项目传递依赖门禁检查 CLOSED 模块的实际依赖是否全部落在 `allowedDependencies` 白名单内 | 【强制】 | `MODULITH-DOC`  | 传递依赖门禁的深度参数与上报机制单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)；OPEN module 按官方语义跳过（ADR-0042） |
| 跨根方向是项目政策，不属于官方 `verify()` 默认规则                                    | 【推荐】 | `MODULITH-DOC`  | 项目政策：框架层不反依赖业务层、业务层跨根只指向框架层；断言实现与依赖深度单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)（ADR-0042） |
| 测试支持代码不是应用模块                                                              | 【强制】 | `MODULITH-DOC`  | `src/test` / `src/testFixtures` 不声明 `@ApplicationModule`；合并根只包含 `com.soda.component` 与 `com.soda.user`（根登记与存在断言的落点见 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)）（ADR-0045） |
| 应用级定制 `@Modulithic` 用于测试扫描配置                                             | 【参考】 | `MODULITH-DOC`  | 仅测试源使用，不用于运行期定制；合并根配置与参数单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)（ADR-0042） |

### 3.2 事件集成与事务语义

事件监听器的事务语义单源在 [spring.md](spring.md) §5。Modulith 侧仅补充：跨模块直接 bean 引用按 §3.1 判据定性；领域事件是跨模块交互的首选通道。

### 3.3 治理与构建归属

| 规则                                                              | 级别     | 来源                  | SODA 落地                                                                                              |
|-------------------------------------------------------------------|----------|-----------------------|--------------------------------------------------------------------------------------------------------|
| 一致性校验须在测试期显式触发，模块结构才算被门禁覆盖              | 【强制】 | `MODULITH-DOC`        | 门禁入口与运行 / 诊断方式单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)（ADR-0042） |
| 白名单允许预留                                                    | 【强制】 | `MODULITH-DOC`        | 通道型模块的预留注明用途后保留；删除前按项目约定的依赖口径复核（ADR-0019）                              |
| 本地事件由普通同步 listener 处理，发布方负责事务；不启用 Registry | 【推荐】 | `MODULITH-DOC`        | 事件事务与双写窗口见 ADR-0044                                                                          |
| 架构测试、构建依赖与人工诊断入口的写法                            | 【强制】 | `SODA-DECISION`（ADR-0045） | 统一归 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)；本篇不重复测试侧配置       |


## 4. 偏离表

| 偏离项                                                                                        | 官方做法                                                                             | SODA 做法                                                                                                                                                                       | 理由                                                                      | 关联 ADR                                              |
|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|-------------------------------------------------------|
| **E1** 双根包建模（`com.soda.component` / `com.soda.user`），跨根方向与传递依赖由项目政策补充 | 单根，或用 `@Modulithic(additionalPackages)` 统一建模，全部依赖受 `verify()` 约束    | 两个独立应用根；`SodaModulith` 以 FQN 白名单合并两个根，官方 `verify()` 负责结构规则，传递门禁与跨根方向不变式由架构门禁负责（断言结构见 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)） | 双根与 starter 分层是 ADR-0008/0019/0042 的既定结构                       | [0042](../adr/0042-dual-root-package-model.md)        |
| **E2** 本地领域事件采用普通同步 `@EventListener`，与发布方事务同事务                          | `@ApplicationModuleListener`（`@Async` + `REQUIRES_NEW` + `AFTER_COMMIT`）+ Registry | `VerificationCreatedEventHandler` 使用 `@EventListener` + `@Transactional`（默认 `REQUIRED`）；`UserUpgradeConsumer` 使用普通 `@EventListener`；不启用异步、Registry 或独立事务 | 本地事件只需在进程内解耦；接受外部发送与数据库提交的双写窗口，见 ADR-0044 | [0044](../adr/0044-local-domain-event-transaction.md) |
| **E3** 不使用 Event Publication Registry                                                      | 引入 Registry 以获得可重放、可恢复的模块事件                                         | 仅 `starter-core`；本地事件载荷携带聚合引用，不承诺序列化；发送异常向发布方传播并由事务回滚                                                                                     | Registry 会引入事件 schema、持久化表与清理策略，超出当前范围              | [0044](../adr/0044-local-domain-event-transaction.md) |

## 检视清单

每条可判定，括号为规则落点（处置词表与逐项回放见 [doc-conventions §7.2](../doc-conventions.md#72-检视清单服务迁移审计)）：

### 代码

1. 每个模块根包有 `package-info.java`，`@ApplicationModule` 落在其上；非声明文件零出现（§2.1）
2. 每个模块的 `package-info.java` 有一行职责 Javadoc，且与 `package` 声明同段（§2.1）
3. 模块 `package-info.java` 带 `@NullMarked`（包级空性单源在 jspecify.md）（§2.1）
4. `type` 显式声明：默认 `CLOSED`；`OPEN` 需满足通用准入判据或登记为 `user.domain` 共享领域例外（§2.2）
5. `allowedDependencies` 全为 FQN（完整包名），无短逻辑名混写、无裸子包名（§2.3）
6. `{}` 与留空未混用：`{}` 是“禁止依赖任何他模块”，留空是“不限制”（§2.3）
7. `::` 引用的 named interface 在目标模块内确有 `@NamedInterface`，且名称与包本地名一致（§2.3）
8. CLOSED module 的 internal 子包未被其他模块直接 import；`user.domain` 的 OPEN 例外按 §2.2 记录（§2.3 / §2.4）
9. 无新增嵌套模块；无已撤销的嵌套模块引用残留（§2.3）
10. 本地事件使用普通同步 `@EventListener`，事务语义按 §3.2 与 ADR-0044 处理（§3.2 / §4 E2）
11. 入口 / 通道模块未见被其他模块 import；其接入方用 `runtimeOnly` 声明（§2.4）

### 依赖与构建声明

12. 测试支持包不声明应用模块；门禁归属与测试侧结构按 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁) 核对（§3.3）
13. 无运行期 `@Modulith` / `@Modulithic` 定制、无 `detection-strategy` 覆盖、无运行期校验属性（§1.2）
14. 无 `@ApplicationEventListener` / 废弃配置属性名（§1.2）

### 架构门禁

15. Modulith 门禁的断言结构、依赖深度与运行 / 诊断入口单源在 [test-conventions §2.4](../test-conventions.md#24-modulith-架构门禁)；本篇核对
    main 侧契约与跨根方向政策（§3.1 / §3.3）
16. 跨根方向只允许“业务层 → 框架层”（框架层只认 `com.soda.component`，其余根一律业务层）
17. 白名单预留项无跨版本存续；空壳通道例外有据可查
18. 新增跨根引用已写入消费方 FQN 白名单，并由跨根方向断言定性
