# 0022 — 开发阶段数据库管理约定（H2-only / 单 V1 直改 / 严禁外键与存储过程）

**Status**: accepted（2026-08-12）

> 再修订（2026-08-16）：示例引用的 `user_verification.user_id` 归一化设计已随 ADR-0021 撤销——Verification 现以 `source.subject` 必填承载主体（UCC/ULG/UPR = userId 裸键串、URG = 端点值，无 `user_id` 列，见 ADR-0026 §1/§10）。「完整性由应用层聚合边界保证」原则不变，示例引用改为 ADR-0026。

## 问题

开发阶段不提供真实 MySQL 实例，数据源为 H2 内存库（MySQL 兼容模式），schema 由 Flyway 执行 SQL 脚本创建。在无真实数据库、无存量数据的前提下：

1. **增量迁移（V1 → V2 → …）引入无谓成本**：迁移脚本与 create table 双份维护，历史脚本随 schema 演进失真（实际每次都是全量重建）。
2. **外键违反架构约束**：阿里《Java 开发手册》【强制】不得使用外键与级联；「完整性由应用层聚合边界保证」（如 `verification.subject` 必填——UCC/ULG/UPR = userId 裸键串、URG = 端点值，见 ADR-0026）与 DB 外键语义冲突。
3. **存储过程 / 触发器 / 视图等 DB 端逻辑**：业务逻辑下沉 DB，迁移成本高、测试困难，与「业务逻辑在领域层/应用层」的 DDD 分层冲突。

## 决策

1. **只用 H2 模拟 MySQL**：`jdbc:h2:mem:soda_user;MODE=MySQL;…`（内存库每次启动重建，见 soda-user-start `application.yml`）。不接真实 MySQL；切真库时另加 profile，并**冻结 V1 为基线**、启用增量迁移。
2. **schema 只保留 V1 一个版本**：`db/migration/` 仅 `V1__init_user_tables.sql`；schema 变更**直接修改 create table 语句**，严禁新增 V2/V3… 增量迁移（开发阶段无存量数据、H2 内存库每次从零执行，无 Flyway 校验和问题）。
3. **严禁外键**（阿里手册【强制】）：不得使用外键与级联；一切外键概念必须在应用层解决（聚合边界 + 领域不变量）。
4. **严禁存储过程、触发器、视图等 DB 端逻辑**：DDL 仅表达表、列、索引；业务校验一律在应用层/领域层。

## 被否定的方案

| 方案 | 否定原因 |
|---|---|
| 标准增量迁移（V1/V2/…） | 开发阶段无存量数据，双份维护、历史脚本失真；切真库时再启用 |
| 允许外键 | 阿里手册【强制】；与聚合边界架构冲突（完整性由应用层保证） |
| DB 端逻辑（存储过程/触发器/视图） | 业务逻辑与 DB 耦合，迁移成本高、测试难 |

## 后果

- V1 直改会丢失「迁移历史」——开发阶段接受（无存量数据）；真库 cutover 时以 V1 定稿版为基线
- 检查点：新增迁移 SQL 文件 / 出现 FK / 出现存储过程 → 违反本 ADR（framework-conventions「Database（开发阶段）」节同步）
- 本 ADR 属开发阶段约定，真库 cutover 时以新 ADR 取代（冻结 V1 + 启用增量迁移）

## 参考

- 阿里《Java 开发手册》（禁止使用外键/级联、【强制】存储过程使用约束）
- ADR-0026（subject 必填；ADR-0021 的 userId 可空/归一化设计已撤销）；soda-user-start `application.yml`（H2 数据源配置）
