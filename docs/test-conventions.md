---
type: Convention
title: 测试规范
description: 写或改任何测试前读——分层映射（每层测什么/怎么测）、通用写法、覆盖率政策
tags: [convention, testing]
status: stable
---

# 测试规范

> 规范源（单源）：Google《Software Engineering at Google》ch11 Testing Overview / ch12 Unit Testing。来源调查与出处索引：
> `research/ut-testing-standards.md`。

> 单源：本规范是测试约定唯一落盘处；DP 必测分组披露于 `conventions/dp-test-conventions.md`。

> **来源与级别**（级别定义见 [doc-conventions.md](doc-conventions.md) §4.1）：上条 Google SWE book 规范源是**外部事实与建议**——
> 规模三分、Beyoncé Rule 等按 **【参考】/【推荐】** 读，偏离记理由、不判缺陷，出处与引文索引
> [research/ut-testing-standards.md](research/ut-testing-standards.md)；§1 五条写法与 §2–§4 的归属、门禁、覆盖率条款是**项目硬门**——
> **【强制】**、来源 `SODA-POLICY`，违反即审计缺陷（经项目采纳的 Google 建议按硬门计）。规则表按四列
> `| 规则 | 级别 | 来源 | SODA 落地 |` 标注；§1 规模表与 §2.3 分层映射矩阵是纯清单，不逐行标级。

## 1. 原则

**规模三分**（判定一个测试允许做什么；规模与范围正交，永远写尽可能小的测试）：

| 规模   | 允许                    | 禁止                             |
|--------|-------------------------|----------------------------------|
| small  | 单进程、单线程          | sleep、IO（网络/磁盘）、阻塞调用 |
| medium | 单机、多进程、localhost | 跨机                             |
| large  | 跨机器                  | —                                |

Spring 切片测试（`@DataJpaTest` / `@WebMvcTest` / `@JsonTest`）属 medium。

**Beyoncé Rule**：测一切你不想坏掉的行为——含失败路径与边界。

**五条写法**（全层强制）：

1. **unchanging tests**：纯重构、加功能、修 bug 都不改既有测试；只有行为变更才动测试。
2. **黑盒优先**：以使用方方式走公开 API；不测 private 与实现细节；helper 类不单独成单元，经其使用方覆盖。
3. **状态优于交互**：断言终态优先；交互验证仅限外部边界（§3 mock 边界行）。
4. **行为而非方法**：按行为（given/when/then）组织测试，不与被测方法一一对应。
5. **clarity**：测试体自含理解它所需的全部信息，且无冗余。

## 2. 测试归属

### 2.1 归属判据：按失败归属

测试放在 **失败应由谁解释**的模块，而不是按使用 `@SpringBootTest`、import
数量或“测试代码更方便放哪”归属：单模块公开契约能确定预期结果时，测试归该模块；只有真实装配下的跨边界结果才归应用入口。完整
Spring 上下文是测试技术形态，不是留在 `start` 的理由。

### 2.2 `start` E2E 准入

业务 E2E 只有同时满足以下条件时才允许留在 `start`：

1. 预期结果无法由任一单模块的公开契约独立确定；
2. 实际跨越至少两个模块边界，并涉及真实装配、事务、事件、HTTP 或数据库；
3. 各参与模块已有小而快的模块级测试；
4. 只断言跨边界组合结果，不复制下层行为矩阵；
5. 每条 E2E 都对应唯一的集成风险；删除后该装配接缝将无人覆盖。

`start` 负责完整上下文可启动和少量关键跨边界路径，不作为难以归属测试的收容箱。Modulith 全仓架构门禁归
`soda-supports/soda-support-modulith`；业务 E2E 不设固定类数上限，新增 E2E 仍须按 §2.2 准入。公共 Web advice / 参数解析归
`soda-component-adapter-starter-web`，业务 MVC 绑定与端点映射归 adapter，Gateway / Repository / 数据库约束归
infrastructure，应用编排与事务事件接线归 application。

### 2.3 分层映射

| 子模块                        | 规模                           | 测什么                                                                                                         | 怎么测                                                                                                                    |
|-------------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| api                           | —                              | —                                                                                                              | 不直测：Command/DTO 契约由 application/adapter 测试覆盖（写法 2）                                                         |
| domain                        | small                          | DP 按 `conventions/dp-test-conventions.md` 必测分组；聚合/领域服务按行为                                       | 纯 JUnit + AssertJ；聚合边界内不 mock                                                                                     |
| application                   | small                          | 用例成功/失败路径、领域事件发布、跨模块调用                                                                    | Mockito 直测；mock 仅外部边界；`*WiringTest` 保持最少                                                                     |
| adapter                       | small / medium                 | 请求→命令映射、DTO 组装、响应结构                                                                              | 直接实例化 controller/assembler；需验 MVC 绑定/参数校验/序列化时 `@WebMvcTest` + `@MockitoBean`（一个测试类只叠一个切片） |
| infrastructure                | small / medium                 | convertor 双向 round-trip；gateway/repository 真库行为矩阵（save 路由、终态守卫、腾槽等机制钉死在持久化 seam） | convertor Mockito 直测；gateway/repository `@DataJpaTest` 切片（H2，主数据源即 H2，默认事务回滚）                         |
| query-server                  | small / medium                 | 读模型组装、投影逻辑、DTO 序列化                                                                               | 组装 Mockito 直测；序列化 `@JsonTest`                                                                                     |
| component-adapter-starter-web | small                          | 公共 ProblemDetail 翻译矩阵、If-Match 入站归一化、出站 ETag 写头的 HTTP 级行为                                 | standalone MockMvc + dummy controller（无 Spring 上下文）；解析器纯单测                                                   |
| start                         | medium（E2E 远端化后升 large） | 上下文可启动、符合 §2.2 准入的关键端到端路径                                                                   | 数量受控；新行为先在低层补单测，E2E 只做守门员                                                                            |

### 2.4 Modulith 架构门禁

Spring Modulith 门禁集中在 `soda-supports/soda-support-modulith/src/test` 的单一测试类 `SodaModulithArchitectureTest`
，没有 `src/testFixtures` 源集，也没有测试支持包（治理边界见 [ADR-0045](adr/0045-modulith-gate-minimal-backstop.md)）。`main`
保持为空，测试栈不进入任何生产类路径，模块结构类工具也不进 main。

`SodaModulith` 使用 `@Modulithic` 合并 `com.soda.component` 与 `com.soda.user` 两个应用根，并启用
`useFullyQualifiedModuleNames = true`。测试运行时纳入 **除本模块外的全部子项目**（`soda-support-modulith/build.gradle`
从根工程子项目推导类路径，新增根自动进类路径、无需手工登记，扫描范围只由 `additionalPackages` 决定），类内四个平铺测试依次执行：

1. 官方 `verify()`：模块图、DAG、API 边界与 `allowedDependencies` 校验；
2. CLOSED 模块传递依赖白名单：以 `DependencyDepth.ALL` 检查实际依赖是否全部落在白名单内，OPEN 模块按官方语义跳过；
3. 跨根方向断言：框架层（`com.soda.component`）不得依赖业务层，业务层跨根只允许指向框架层；
4. 应用根存在断言：`APPLICATION_ROOTS` 中的每个根都在模块图中——把根从 `additionalPackages` 删掉即红。

**新增应用根的登记步骤（单源在本节）**：除把根写进 `SodaModulith.additionalPackages` 外，还需在
`SodaModulithArchitectureTest.APPLICATION_ROOTS` 补一行——否则该根从此不在存在断言的保护范围内；已登记的两个根
（`com.soda.component` / `com.soda.user`）由此兜底。该登记步骤是门禁的测试侧配置，不写进测试源码注释。

**运行与诊断**：

- 改动模块声明或应用根后运行 `./gradlew :soda-supports:soda-support-modulith:test`，四项断言须全绿；
- 人工查看结构使用 `./gradlew :soda-supports:soda-support-modulith:printModulithStructure`；该 JavaExec 任务是诊断入口，不是测试门禁。

超出官方语义的检查不进测试（ADR-0045）。 **唯一实测到的静默通过情形**：既没有 `@ApplicationModule` 声明、又没有任何跨模块
依赖的新模块——门禁无从判断“该声明没声明”。它一旦产生依赖立刻转红（传递依赖门禁按 CLOSED ＋ 空白名单处理），兜底靠
`conventions/modulith.md` 写清单与检视清单。

另一个方向的实测澄清：CLOSED 模块 **省略** `allowedDependencies` 时，官方 `verify()` 放行（哨兵值＝不限制），但项目传递依赖
门禁判红（`getAllowedDependencies()` 为空集，实际依赖全部视为未声明）——官方这一盲点正是传递门禁存在的理由。

支持模块测试资源保留 `archRule.failOnEmptyShould=false`：对合并 FQN 根执行官方 `verify()` 时，Spring Modulith 2.1.0
内部的 ArchUnit cycle rule 可能没有匹配到可评估的类；探针将该属性设为 `true` 时架构测试失败，设为 `false` 时通过。
该配置只放宽 ArchUnit 的空 `should` 结果，不放宽 Modulith 的依赖违规断言。

## 3. 通用写法（全层适用）

| 规则                                                                                                                                             | 级别     | 来源         | SODA 落地      |
|--------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------|----------------|
| **断言**：AssertJ-only，禁 `org.junit.jupiter.api.Assertions.*`；异常用 `assertThatThrownBy` 链式                                                  | 【强制】 | `SODA-POLICY` | 检视清单 3     |
| **命名**：方法 `should_<行为>`（条件不足以消歧时补 `_when_<条件>`）；`@DisplayName` 中文说明                                                       | 【强制】 | `SODA-POLICY` | 检视清单 3     |
| **分组**：`@Nested` 按行为分组；方法数 ≤6 平铺                                                                                                    | 【强制】 | `SODA-POLICY` | 检视清单 4     |
| **数据**：内联字面值；同一值 ≥3 个方法复用才提取类内常量                                                                                          | 【强制】 | `SODA-POLICY` | 检视清单 5     |
| **参数化**：`@ParameterizedTest` 仅 ≥4 组同构数据（`@NullAndEmptySource` 单独使用＝2 组，豁免该基线；与其它 Source 组合按合并组数计，≥3 组不豁免）；不足 4 组的同族同构负例允许内联——同一场景的 ≤3 个反例合并在一个方法内串行断言（示例：`AvatarTest` 相对路径 3 例）；≥10 组用 `@MethodSource` 分离 | 【强制】 | `SODA-POLICY` | 检视清单 4     |
| **断言密度**：每个方法一个逻辑断言（一个场景）；同族同构负例合并（上行）视作一个场景                                                             | 【强制】 | `SODA-POLICY` | 检视清单 4     |
| **复用**：共享测试基础设施（Mapper/TestUtil）允许；共享数据 fixture 与测试基类禁止                                                                | 【强制】 | `SODA-POLICY` | 检视清单 5     |
| **mock 边界**：进程外或模块边界（gateway、eventBus、外部服务、他模块 api）可 mock 且可验证交互；聚合/服务内部协作禁 mock                           | 【强制】 | `SODA-POLICY` | 检视清单 6     |
| **环境依赖**：Clock 等可注入依赖用真实固定实例（`Clock.fixed`），不 mock                                                                           | 【强制】 | `SODA-POLICY` | 检视清单 6     |
| **修 bug**：先写复现单测（红）再改码                                                                                                             | 【强制】 | `SODA-POLICY` | 检视清单 7     |

## 4. 覆盖率政策

**V1（现状，无 CI）**：覆盖率是发现工具，不设门。可选接入 JaCoCo 只出 HTML 报告：

```groovy
plugins { id 'jacoco' }
test { finalizedBy jacocoTestReport }
```

未覆盖代码人工判断是否该测（domain 模型与算法重点）。覆盖率只从 small 测试测量，避免大测试虚高。

充分性判据是「很少漏 bug 到生产 + 改代码时不因怕回归而犹豫」，不是数字：高覆盖率不证明测试质量，低覆盖率说明测得不够。

## 5. 写清单

新增或修改测试时按序执行（括号内为落点）：

1. 先定归属：按「失败应由谁解释」落模块，E2E 同时满足五条准入才进 `start`（§2.1 / §2.2）。
2. 取尽可能小的规模档（§1 规模三分）。
3. 按分层映射确定「测什么 / 怎么测」；DP 必测分组按 [conventions/dp-test-conventions.md](conventions/dp-test-conventions.md)（§2.3）。
4. 按 §3 规则表逐条落地测试体：断言、命名、分组、数据、参数化、断言密度、复用、mock 边界、环境依赖。
5. 改动模块声明或应用根时，按 §2.4 运行全仓 Modulith 架构门禁并确认四项断言通过，应用根变更同步登记。
6. 覆盖率只作发现工具，不设门；不新增覆盖率或测试数量门禁（§4）。
7. **完成判据**：按 §6 检视清单逐条自检通过；Modulith 门禁机制只在 §2.4 一处成文，本篇其余位置只留指针。

## 6. 检视清单

code-review 检视轴按本清单逐条核对（每条可判定，括号内为落点）：

1. 测试落在失败归属模块；`start` 内 E2E 逐条满足五条准入，且只断言跨边界组合结果（§2.1 / §2.2）
2. 规模档与规模三分一致：small 无 IO 与跨机调用，medium 不跨机（§1）
3. 断言 AssertJ-only、异常链式断言；方法名 `should_<行为>`，`@DisplayName` 中文（§3 断言 / 命名）
4. `@Nested` 按行为分组、方法数 ≤6；参数化达 ≥4 组同构数据阈值、同族负例合并串行断言；每个方法一个逻辑断言（§3 分组 / 参数化 / 断言密度）
5. 数据内联；无共享数据 fixture 与测试基类，共享基础设施仅限 Mapper/TestUtil 类（§3 数据 / 复用）
6. mock 只在进程外或模块边界，聚合/服务内部协作无 mock；Clock 等用真实固定实例（§3 mock 边界 / 环境依赖）
7. 修 bug 的改动带复现单测（§3 修 bug）
8. 覆盖率未被当作门禁或验收数字引用；本篇不存在无触发条件的未来门禁条款（§4）
9. 新增应用根时 `SodaModulith.additionalPackages` 与 `APPLICATION_ROOTS` 同步登记，门禁四项断言全绿（§2.4）
10. Modulith 架构门禁的断言、命令与登记步骤只在 §2.4 成文，本篇其他章节不复述（§2.4）
11. 每条规则可判定、互不重复，且带级别与来源；外部建议未被写成不标 `SODA-POLICY` 的硬门（来源与级别块）
