---
type: Decision Record
title: Adapter 组件按入站通道拆分
description: 拆分或新建 adapter starter 时读：adapter 组件按入站通道拆分，家族与基础设施并列不隶属。
tags: [gradle, starters, modulith]
status: stable
---

# 0019 — Adapter 组件按入站通道拆分

adapter 组件按入站通道拆分；子模块清单事实源在 `soda-components/settings.gradle`；本 ADR 不重述。原单体模块从构建移除；包名沿用「
`com.soda.component.web|job|consumer`」，「adapter」只作构建级家族标签不进包名。 **具体 Gradle 子模块名（
`soda-component-adapter-starter-web` 等）以 `soda-components/settings.gradle` 为事实源，本 ADR 不重述**。拆分依据是 Spring
Modulith 边界早已三分而 Gradle 打包滞后，属打包对齐；adapter 家族依赖 `{api}`，与基础设施（`{domain}`）并列不隶属，业务模块按所用通道声明。

## Consequences

- consumer 空壳为有意保留（待 MQ 集成）；rpc starter 与 BOM 平台等首个真实消费者出现再建。
- 三通道不配独立 ModulithTest，由组件家族级测试经 testRuntimeOnly 覆盖。
