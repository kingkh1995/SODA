---
type: Decision Record
title: JPA 扫描根由持久化模块自持
description: 实体与 Repository 的扫描根声明归 infrastructure 模块，启动入口只保留 @SpringBootApplication。
tags: [ modules, persistence, jpa ]
status: stable
---

# 0043 — JPA 扫描根由持久化模块自持

实体与仓储都住在 infrastructure 模块的子包里，扫描根这一知识因此也归该模块持有：它以被组件扫描发现的
`JpaPersistenceConfiguration` 声明 `@EntityScan` / `@EnableJpaRepositories` 的扫描范围，两个启动入口只保留
`@SpringBootApplication`。理由是分层——启动入口只该做组件扫描与容器启动，不该知道持久化类型住在哪个包；扫描根挪进模块后，入口模块的编译类路径不再需要任何
JPA 构件，读写两个部署单元也不会各自维护一份可能分歧的包名。

## Consequences

- 扫描范围随模块一同演进：新增实体或仓储无需改动任何启动入口。
- 持久化切片测试的引导配置 `@Import` 同一个 `JpaPersistenceConfiguration`，生产装配与切片装配的扫描根不再各写一份，消除两处漂移。
