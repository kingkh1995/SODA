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

## 2. 分层映射

| 子模块         | 规模                           | 测什么                                                                                                         | 怎么测                                                                                                                    |
|----------------|--------------------------------|----------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| api            | —                              | —                                                                                                              | 不直测：Command/DTO 契约由 application/adapter 测试覆盖（写法 2）                                                         |
| domain         | small                          | DP 按 `conventions/dp-test-conventions.md` 必测分组；聚合/领域服务按行为                                       | 纯 JUnit + AssertJ；聚合边界内不 mock                                                                                     |
| application    | small                          | 用例成功/失败路径、领域事件发布、跨模块调用                                                                    | Mockito 直测；mock 仅外部边界；`*WiringTest` 保持最少                                                                     |
| adapter        | small / medium                 | 请求→命令映射、DTO 组装、响应结构                                                                              | 直接实例化 controller/assembler；需验 MVC 绑定/参数校验/序列化时 `@WebMvcTest` + `@MockitoBean`（一个测试类只叠一个切片） |
| infrastructure | small / medium                 | convertor 双向 round-trip；gateway/repository 真库行为矩阵（save 路由、终态守卫、腾槽等机制钉死在持久化 seam） | convertor Mockito 直测；gateway/repository `@DataJpaTest` 切片（H2，主数据源即 H2，默认事务回滚）                         |
| query-server   | small / medium                 | 读模型组装、投影逻辑、DTO 序列化                                                                               | 组装 Mockito 直测；序列化 `@JsonTest`                                                                                     |
| start          | medium（E2E 远端化后升 large） | 模块依赖（ModulithTest）、上下文可启动、关键端到端路径                                                         | 数量受控；新行为先在低层补单测，E2E 只做守门员                                                                            |

## 3. 通用写法（全层适用）

| 条款      | 规则                                                                                                      |
|-----------|-----------------------------------------------------------------------------------------------------------|
| 断言      | AssertJ-only，禁 `org.junit.jupiter.api.Assertions.*`；异常用 `assertThatThrownBy` 链式                   |
| 命名      | 方法 `should_expectedBehavior_when_condition`；`@DisplayName` 中文说明                                    |
| 分组      | `@Nested` 按行为分组；方法数 ≤6 平铺                                                                      |
| 数据      | 内联字面值；同一值 ≥3 个方法复用才提取类内常量                                                            |
| 参数化    | `@ParameterizedTest` 仅 ≥4 组同构数据；≥10 组用 `@MethodSource` 分离                                      |
| 断言密度  | 每个方法一个逻辑断言（一个场景）                                                                          |
| 复用      | 共享测试基础设施（Mapper/TestUtil）允许；共享数据 fixture 与测试基类禁止                                  |
| mock 边界 | 进程外或模块边界（gateway、eventBus、外部服务、他模块 api）可 mock 且可验证交互；聚合/服务内部协作禁 mock |
| 环境依赖  | Clock 等可注入依赖用真实固定实例（`Clock.fixed`），不 mock                                                |
| 修 bug    | 先写复现单测（红）再改码                                                                                  |

## 4. 覆盖率政策

**V1（现状，无 CI）**：覆盖率是发现工具，不设门。可选接入 JaCoCo 只出 HTML 报告：

```groovy
plugins { id 'jacoco' }
test { finalizedBy jacocoTestReport }
```

未覆盖代码人工判断是否该测（domain 模型与算法重点）。覆盖率只从 small 测试测量，避免大测试虚高。

**CI 时代**（CI 落地后启用）：new-code 覆盖率 ≥80% + 新增可覆盖行 <20 豁免（fudge），只卡新代码；实现机制见
`research/ut-testing-standards.md` §2.3。

充分性判据是「很少漏 bug 到生产 + 改代码时不因怕回归而犹豫」，不是数字：高覆盖率不证明测试质量，低覆盖率说明测得不够。
