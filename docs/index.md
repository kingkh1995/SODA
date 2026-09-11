# docs

文档 bundle 索引——按场景定位到对应文档时读；体系自身规范见 [doc-conventions.md](doc-conventions.md)。

| 一级条目                                             | 定位（何时读）                                                                   |
|------------------------------------------------------|----------------------------------------------------------------------------------|
| [adr/](adr/)                                         | 查架构决策 / 领域行为理由时读——决策记录（000N-slug 顺序编号，极简最终态）        |
| [research/](research/)                               | 查一手来源调研 / 外部事实时读——研究笔记（wayfinder R 票产出）                    |
| [agents/](agents/)                                   | 配 agent 工作流（issue tracker · triage labels · domain docs）时读               |
| [doc-conventions.md](doc-conventions.md)             | 写/改文档或文档规范时读——文档体系标准（frontmatter schema · 合规基准 · V1 条款） |
| [framework-conventions.md](framework-conventions.md) | 框架约定正文：类型契约注记 · Modulith 治理 · 编排/异常/数据库/Logging · 单源指针 |
| [test-conventions.md](test-conventions.md)           | 写测试 / 看覆盖率政策时读——测试规范（分层映射 · 通用写法 · 覆盖率）              |
| [dp-conventions.md](dp-conventions.md)               | 设计 Domain Primitive 时读——DP 设计规范                                          |
| [conventions/](conventions/)                         | 查专项规范族时读——按复杂度渐进补齐，类型契约总表见 `framework-type-contracts.md` |

## conventions/

- `framework-type-contracts.md` — 框架类型契约（DP 清单表 / Entity / Aggregate / Gateway / DomainEvent 族）
- `framework-crosscutting.md` — 跨切面规范（编排 / 异常 / Logging / 数据库）
- `adapter.md` — WebAssembler + MapStruct 注解速查
- `dp-test-conventions.md` — DP 必测分组（JUnit 标签矩阵）
- `dp-validation-conventions.md` — DP 校验与归一化（PassUtils / ValidateUtils 职责分层）
- `dp-json-conventions.md` — DP JSON 序列化契约（Jackson 模式总表）
- `aip-api-conventions.md` — AIP API 设计规范（资源 / 方法 / 字段 / 分页 / 错误处理）
