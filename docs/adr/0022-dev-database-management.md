---
type: Decision Record
title: 开发阶段数据库管理约定（H2-only / 单 V1 直改 / 严禁外键与存储过程）
description: 开发阶段建表或写 Flyway 迁移时读：仅 H2 内存库、单 V1 直改、严禁外键与存储过程等 DB 端逻辑。
tags: [database, flyway, h2, dev-stage]
status: stable
---

# 0022 — 开发阶段数据库管理约定（H2-only / 单 V1 直改 / 严禁外键与存储过程）

开发阶段不提供真实 MySQL、无存量数据——数据源为 H2 内存库（MySQL 兼容模式），schema 由 Flyway 执行 `db/migration/` 下 SQL
创建。schema 只保留 V1 一个版本，变更直接修改 create table 语句，严禁新增 V2/V3 增量迁移（迁移脚本与表定义双份维护是无谓成本；切真库时冻结
V1 定稿版为基线再启用增量迁移，另立 ADR）。严禁外键与级联（阿里《Java 开发手册》【强制】——完整性由应用层聚合边界保证）与存储过程/触发器/视图等
DB 端逻辑（业务校验一律在应用层/领域层，DDL 仅表达表、列、索引）。

## Consequences

- 检查点：新增迁移 SQL 文件、出现 FK、出现存储过程 → 违反本 ADR
- 本 ADR 属开发阶段约定，真库 cutover 时以新 ADR 取代（冻结 V1 基线 + 启用增量迁移）
