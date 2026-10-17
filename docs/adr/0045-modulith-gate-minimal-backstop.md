---
type: Decision Record
title: Modulith 门禁只做最小兜底
description: 评审或修改 SodaModulithArchitectureTest 时读——官方 verify ＋ 两条项目不变式 ＋ 根存在断言，不引入登记表 / 第二事实源 / 模块声明机检。
tags: [ modulith, testing, governance ]
status: stable
---

# 0045 — Modulith 门禁只做最小兜底

`SodaModulithArchitectureTest` 是唯一全仓 Modulith 门禁，内容只做最小兜底：官方 `verify()`、ADR-0042 的两条项目不变式（CLOSED
传递依赖白名单、跨根方向），外加“两个应用根都还在模块图中”的存在断言；超出官方语义的检查（模块声明完整性、覆盖范围的独立事实源、规则容器）一律不进测试，交给项目开发规范与检视清单。理由是约束最终落在开发者身上，按
Spring Modulith 官方用法管理模块引用即可，为风格统一付出跨模块读源码一类的复杂度不划算。

## Consequences

- 新增应用根必须登记到 `SodaModulith.additionalPackages`，否则门禁看不见它；两个已登记根由存在断言兜底， **新根从无到有的登记步骤由
  test-conventions §2.4 承载，不由测试承载**。
- 唯一实测静默通过的情形不加断言：既无 `@ApplicationModule` 声明、又零跨模块依赖的新模块——门禁无从判断“该声明没声明”，但它一旦产生依赖即转红；由
  `conventions/modulith.md` 写清单与检视清单兜底。反向地，官方的一个盲点由项目传递依赖门禁补上：CLOSED 模块省略
  `allowedDependencies` 时官方 `verify()` 放行，项目门禁判红。
- 门禁只有一个测试类、四个平铺测试方法；不再有 `src/testFixtures` 源集，测试依赖走根 `build.gradle` 统一提供的
  `testImplementation`。
