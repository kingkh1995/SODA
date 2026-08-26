---
type: Decision Record
title: 分层 starter：一架构层一组件模块
description: 新业务模块引入分层能力时读——soda-components 每层一个 starter 子模块携带基线类型与 Spring 能力，start-starter 纯脚手架、部署聚合由业务 start 模块完成。
tags: [gradle, starters, modulith]
status: stable
---

# 0008 — 分层 starter：一架构层一组件模块

`soda-components` 为每个架构层提供一个 starter 子模块（子模块清单事实源在 `soda-components/settings.gradle` 的 `include`
列表；本 ADR 不重述）——各 starter 携带该层基类与 Spring 能力，业务模块按层各引一行即得整层类型与传递依赖；跨模块依赖方向由
Spring Modulith 在测试期强制成 DAG。 **模块列表与传递依赖关系以 `soda-components/settings.gradle` 与各 starter 的
`package-info.java` / `build.gradle` 为事实源，本 ADR 不重述**（事实会随 starter 调整漂移，记载无意义）。start-starter
是纯脚手架（仅包声明与家族测试），不做部署聚合——写服务由各业务 start 模块以
runtimeOnly 引入自己的 adapter 与 infrastructure 完成装配，组件层不提供统一打包入口。
