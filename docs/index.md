# docs

文档 bundle 索引——按场景定位到对应文档时读；体系自身规范见 [doc-conventions.md](doc-conventions.md)。

| 一级条目                                             | 定位（何时读）                                                                                |
|------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| [adr/](adr/)                                         | 查架构决策 / 领域行为理由时读——决策记录（000N-slug 顺序编号，极简最终态）                     |
| [research/](research/)                               | 查一手来源调研 / 外部事实时读——研究笔记（wayfinder R 票产出）                                 |
| [agents/](agents/)                                   | 配 agent 工作流（issue tracker · triage labels · domain docs）时读                            |
| [doc-conventions.md](doc-conventions.md)             | 写/改文档、ADR 或 frontmatter 时读——文档体系标准（权威层级 · bundle 与保留名 · frontmatter 契约 · 来源登记与 profile · 写·检视双清单 · 合规硬门与 V1 覆盖） |
| [framework-conventions.md](framework-conventions.md) | 查框架层有什么 / 模块怎么治理时读——框架约定**索引**：分层总览 · Modulith 治理 · 单源指针（类型契约与跨切面正文在 `conventions/`） |
| [test-conventions.md](test-conventions.md)           | 写测试 / 看覆盖率政策时读——测试规范（分层映射 · 通用写法 · 覆盖率）                           |
| [dp-conventions.md](dp-conventions.md)               | 新增或修改 DP 时读——形态选择 / 行为契约 / 工厂命名 / 富血·缓存·SPI / 枚举 DP；含写·检视双清单 |
| [conventions/](conventions/)                         | 查专项规范族时读——按复杂度渐进补齐，类型契约总表见 `framework-type-contracts.md`              |

## conventions/

- `framework-type-contracts.md` — 框架类型契约（DP 清单表 / Entity / Aggregate / Gateway / DomainEvent 族）
- `framework-crosscutting.md` — 跨切面规范（编排 / 异常 / Logging / 数据库）
- `adapter.md` — Adapter 转换器约定（WebAssembler 契约语义：转换方向 · 方法命名 · 使用约定；MapStruct 机制规则按指针去 `mapstruct.md`）
- `dp-test-conventions.md` — DP 必测分组（@Nested 分组矩阵）
- `dp-validation-conventions.md` — DP 校验与归一化（三入口职责 / 校验收敛 / 编排 / 归一化细则）
- `dp-json-conventions.md` — DP JSON 序列化契约（模式总表 / 类型映射 / BigDecimal DP 模式 / 禁止 Serializable）
- `aip-api-conventions.md` — AIP API 设计规范（资源 / 方法 / 字段 / 分页 / 错误处理）
- `jackson.md` — Jackson 3 使用规范（接入与共享 / 注解落位 / 多态 / 3.0 行为基线）
- `jspecify.md` — JSpecify 空性标注规范（`@NullMarked` 基线 / `@Nullable` 位置契约 / 泛型与覆写 / 外部库边界）
- `lombok.md` — Lombok 使用规范（注解准入与禁用集 / 访问器与 fluent / 构造器与 Builder / 相等与调试 / 日志与构建接线）
- `mapstruct.md` — MapStruct 使用规范（`@Mapper` 与注入 / 映射语义 / 构建接线 / Null 与 JSpecify 缺口）
- `spring.md` — Spring Framework 使用规范（DI / 组件与自动配置 / 事务 / 事件 / Web MVC 扩展点 / 校验）
- `modulith.md` — Spring Modulith 使用规范（`@ApplicationModule` / `allowedDependencies` / 官方 `verify()` 与传递依赖门禁）

## research/

- `foundation-deps-official-practices.md` — 写 / 评审 Jackson 3 · JSpecify · Lombok · MapStruct · Spring Framework · Spring Modulith 用法，或核对某条项目规则的一级官方依据时读——六库官方规则清单 · 过时对照 · 逐条来源
- `entity-equality-in-ddd.md` — 裁定 Entity / Aggregate 是否重写 equals/hashCode（引用相等 + 显式按主键 id 比对）时读——Evans 2003 / Vernon 2013 / JPA / Hibernate 一手取证与证据缺口
- `write-side-server-generated-fields.md` — 设计或评审 Create / Update 响应如何回传服务端生成字段（id / version / 审计时间）时读——AIP·RFC、DDD 文献、主流实现三层取证与四方案裁定
- `type-vs-class-mainstream-review.md` — 审视多态实体 type-vs-class 设计（sealed / 枚举判据 · 网关 `Class<T>` 收参 · `@JsonTypeName` 判别）时读——对照主流 Java / DDD 实践，结论收敛 ADR-0016
- `ut-testing-standards.md` — 制定或评审单元测试规范（分层测什么 · 覆盖率政策 · mock 边界 · 切片选型）时读——Google SWE 书 ch11/12 基准调研；定案见 `test-conventions.md`
- `google-aip-api-design-spec.md` — 设计或评审 SODA REST API（资源 · 方法 · 字段 · 分页 · 错误 · 兼容性）时读——Google AIP 一手规则事实与本仓覆盖、缺口对照；项目定案见 `conventions/aip-api-conventions.md`
- `google-aip-full-pull.md` — 为 AIP 逐类对齐（API 规范整改）取全谱输入时读——7 类 66 个主文档的编号与类别清单，及其中 4 类 51 个 AIP 的规则逐行收录，仅记录不评价
- `google-comment-conventions.md` — 核对 Java 注释规则的来源章节归属、钉住版本或来源漂移时读——当前 §7 Javadoc 与 §4.8.6 的适用边界、比对记录；SODA 注释规则单源在根文档 STYLEGUIDE §4
- `non-java-comment-authorities.md` — 新增非 Java 注释载体、核对 `FORMAT-*` 来源版本与不可类推项时读——SQL/Flyway · YAML · `.properties` · Gradle/Groovy · Markdown · `lombok.config` · `.gitignore` 的一手来源事实与核验记录；规则单源在根文档 STYLEGUIDE §4.5
- `p3c-surface-format-retrofit.md` — 评估在 DP / Entity / AIP / 注释规范借鉴 P3C 表面格式（三级标定 · ❌/✅ · 表格列统一化）时读——形式要素、四点对照事实与采用边界；决策见 ADR-0036
- `okf-code-repo-adaptation.md` — 选 frontmatter 键、type 词表或定 bundle 布局、或复核 OKF 来源漂移与钉住版本时读——OKF v0.2 适配裁定与来源维护记录；项目定案见 `doc-conventions.md`
- `matt-skills-doc-contracts.md` — 重构 AGENTS / CONTEXT / ADR / STYLEGUIDE / `agents/` 结构、或复核 writing-for-agents 来源漂移与钉住版本时读——matt-pocock 技能硬编码的文档结构契约与可改边界
