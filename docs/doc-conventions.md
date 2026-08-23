---
type: Convention
title: Doc Conventions — 文档体系标准
description: SODA 文档体系标准：知识分层模型、frontmatter schema 契约、bundle 布局、链接约定、写作原则、ADR 模板、合规基准。
tags: [convention, docs, okf]
status: draft
---

# Doc Conventions

SODA 文档体系标准（OKF v0.2 适配 × Google 注释规范 × agent 写作原则）。本文件是**唯一标准落盘处**：新文档类、新 frontmatter 键、写作规则、ADR 模板的变更都改这里；其余文档只做指针（单源原则）。

## 1. 知识分层模型（四类知识各归其位）

| 知识类型 | 归属 | 载体 |
|---|---|---|
| **词汇**（概念是什么、用什么词） | `CONTEXT.md`（业务词汇） | 定义 + `_Avoid_`。**框架层无词汇**——类型即代码，定义在 javadoc；防混淆是命名约定（framework-conventions §2） |
| **决策**（为什么这样设计） | `docs/adr/` | Context / Decision / Consequences（V1 阶段限制见 §9） |
| **契约**（不变量、前置、副作用） | **与代码同处的注释**（Javadoc） | STYLEGUIDE §4 规范 |
| **约定**（怎么写代码 / 怎么写文档 / 框架怎么用） | `STYLEGUIDE.md`、`docs/doc-conventions.md`、`docs/framework-conventions.md`（索引）+ `docs/conventions/*`（类型规范族） | 规则 + 示例 |

禁止跨层重复：词汇的语义不进 ADR 正文；实现细节不进词汇表；决策的理由不进注释（注释只留 ADR 锚点）；同一种约定不写进两份约定文档。判断标准——一处改了，别处不需要跟着改。

## 2. Bundle 布局

**docs/ 为 OKF bundle**（`type` 是唯一必填 frontmatter 键）。`.scratch/`（tracker 工作流数据）与根文档在 bundle 之外——bundle 合规只约束 docs/ 内文件。

```
docs/
├── index.md                  ← bundle 入口（渐进披露，无 frontmatter，仅目录）
├── adr/                      ← 决策记录（000N-slug.md，极简模板见 §4）
├── research/                 ← 研究笔记（引用外部材料）
├── agents/                   ← agent 技能契约（setup 产物，不在 OKF schema 约束内）
├── framework-conventions.md  ← 约定索引：分层总览 + Modulith 治理 + 类型清单表（覆盖清单）+ 导航
├── test-conventions.md       ← 测试规范：分层映射 + 通用写法 + 覆盖率政策
├── conventions/              ← 类型设计规范族（按复杂度拆分，模板见 §2.4）
│   ├── entity-aggregate.md   ├── domain-marker-types.md   ├── domain-service-event.md
│   ├── gateway.md            ├── dp-conventions.md        ├── dp-spec-simple.md
│   ├── dp-spec-composite.md  ├── dp-spec-identifier.md    ├── dp-spec-cache-util.md
│   ├── dp-test-conventions.md├── application.md           ├── adapter.md
│   └── infrastructure.md
└── doc-conventions.md        ← 本文件
```

`docs/index.md` 只列一级条目（目录 + 一句话定位，不承载正文——OKF §8）；conventions/ 族经 framework-conventions 类型清单表触达，不在 index 展开：

```markdown
# docs — Soda 文档库

| 位置 | 内容 |
|---|---|
| adr/ | 架构决策记录（000N-slug 顺序编号，极简模板） |
| research/ | 外部规范 / 来源研究笔记 |
| agents/ | agent 技能契约（setup 技能产物） |
| framework-conventions.md | 约定索引：分层总览 + Modulith 治理 + 类型清单表 |
| test-conventions.md | 测试规范：分层映射 + 通用写法 + 覆盖率政策 |
| conventions/ | 类型设计规范族（entity-aggregate / dp 谱系 / application / adapter / infrastructure…） |
| doc-conventions.md | 本文档体系标准 |
```

**不引入 `log.md`**（OKF §9 为 MAY，非合规必需）：变更史由 git log/blame 承载；V1 零残留条款（§9）禁止历史回溯内容入库——log 条目本身即历史回溯。

### 2.4 类型设计规范模板

每个基类 / 接口标记类型（domain-starter 基类、AbstractAppService 等）**必须有一份设计规范**——覆盖清单见 `framework-conventions.md` 类型清单表（合规第 8 条）。条目模板：

- **识别行**：类型名 + 包 + 角色（基类 / 标记接口 / DP）
- **设计规则**：不变式、契约、怎么写子类 / 实现
- **反模式**：禁止用法
- **测试要求**：引用 dp-test-conventions 或本类测试约定
- **关联 ADR**

组织：按**复杂度拆分 + 关联性合并**——复杂类型独立成文（Entity/Aggregate、DP 谱系），简单关联类型合并一篇（字面值类型族、infrastructure 持久化基类）。边界实施时调整。

### 2.5 跨文档链接约定

**链接形式：全仓库统一相对路径**（从链接所在文件出发，如 ADR 链词汇表写 `../../CONTEXT.md`）。不用根相对 `/…`——OKF §6.1 推荐该形式换文档移动稳定性，但 GitHub 渲染把 `/…` 解析到域根造成断链；本仓库不用 OKF 工具链，docs/ 结构由本标准固定、不预期重组。断链容忍（OKF §6.1）：目标未写不构成错误，渐进写作友好。

跨知识层互链规则：

| 方向 | 规则 |
|---|---|
| 代码 → 文档 | javadoc 文字锚点「见 ADR-0021」——非 markdown 链接，grep 可达 |
| ADR → 代码 | prose 符号名（如 `VerificationService`），不写文件路径 / 链接——代码是事实源，路径易腐 |
| CONTEXT ↔ 其他 | 互不挂链接——纯词汇表（matt CONTEXT-FORMAT 硬契约），术语靠词表寻址，grep 即达 |
| 文档 ↔ 文档 | markdown 相对路径（根文档 ↔ docs/、docs/ 内部同一规则；AGENTS.md 指针同此） |

**根文档**（README / AGENTS / CONTEXT / STYLEGUIDE）在 bundle 外：加 frontmatter 是推荐扩展（喂搜索/索引），不加不违规。

## 3. Frontmatter schema 契约

### 3.1 type 词表（6 值，自解释）

| type | 文档类 | 必填键 | 按类可选键 |
|---|---|---|---|
| `Repo Overview` | 根 README | type, title, description | tags, status |
| `Agent Guide` | AGENTS.md | type, title, description | tags, status |
| `Glossary` | CONTEXT.md | type, title, description | tags, status |
| `Convention` | STYLEGUIDE / framework-conventions / test-conventions / dp-conventions / dp-test-conventions / doc-conventions | type, title, description | tags, status |
| `Decision Record` | docs/adr/* | type, title, description | tags, status |
| `Research` | docs/research/* | type, title, description, **sources** | tags, status, generated, verified |

未知 type 必须被宽容消费（OKF §11）——词表可扩展，不集中注册。OKF 的 `resource` 键（数据目录的资源绑定语义）不采纳：research 的 provenance 由 `sources` 覆盖，代码仓库中代码本身是事实源。

### 3.2 键契约

| 键 | 必填 | 语义 | 说明 |
|---|---|---|---|
| `type` | ✅ | 文档类（§3.1 词表） | 唯一必填键（OKF §4.1） |
| `title` | 除 index 外 ✅ | 人类标题 | |
| `description` | ✅ | 一句话定位，**即 context pointer** | 措辞决定 agent 何时触达（writing-for-agents） |
| `tags` | | 主题标签 | 数组 |
| `status` | 推荐（含 ADR；OKF 仅 `type` 必填，可省略） | draft \| stable \| deprecated \| superseded | 生命周期（OKF §5.4 词表为 draft/stable/deprecated；**superseded 为项目扩展**——承 matt ADR「被替代，链接指向替代者」语义），**全文档类统一词表**（ADR 同表，不用 matt 的 proposed/accepted）。**V1 阶段仅用 draft/stable**（过时即删，不设 deprecated/superseded）——见 §9 |
| `sources` | Research ✅ | `[{resource, id, title, author}]` | provenance（OKF §5.1）；research 必用，其余不用 |
| `generated` | agent 独立产物时 | `{by, at}` | 整份文档由 agent 生成为独立产物时填（research 输出等）；人写/结对文档不填（git blame 已覆盖） |
| `verified` | agent 生成文档经人工复核后 | `[{by, at}]` | `by` 用 `human:<id>`——人类复核提升信任层级（OKF §5.3） |
| `stale_after` | 引用易变外部事实时 | ISO 日期 | 默认省略 |
| 扩展键 | | 任意 | 消费者不得拒绝（OKF §4.1 Extensions / §11）；`usage_count` 等 OKF 数据目录键留扩展、不预置（01） |

**actor 约定**（`generated.by` / `verified[].by` 取值，照采纳 OKF §7）：

| 角色 | 格式 | 示例 |
|---|---|---|
| agent/工具 | `<producer>/<version>` | `wayfinder/01` |
| 人 | `human:<id>`（git 用户名） | `human:mm` |
| 自动流程 | `process:<id>` | `process:nightly-sync` |

信任分级（OKF §5.3）以 `human:` 前缀为键——人工确认的内容必须用 `human:` 前缀记录。

### 3.3 示例

```yaml
---
type: Decision Record
title: Verification 聚合根认定与 VerificationService 拓扑
description: Verification 重分类为独立聚合根；requestCode 成为发码唯一入口。
tags: [verification, aggregate-root]
status: stable
---
```

```yaml
---
type: Research
title: OKF 在代码仓库文档场景的适配研究
description: OKF v0.2 对 SODA 文档体系的适用性：type 词表、status 采纳、bundle 布局。
sources:
  - resource: https://github.com/GoogleCloudPlatform/knowledge-catalog
    id: okf-spec
    title: OKF v0.2 SPEC.md
status: stable
generated:
  by: wayfinder/01
  at: 2026-08-14
verified:
  - by: human:mm
    at: 2026-08-14
---
```

## 4. ADR 模板

ADR 按 matt ADR-FORMAT：**标题 + 1-3 句**（背景 + 决策 + 为什么）。价值在"记下定了什么、为什么"，不在填章节。实现细节（代码片段、端点表、编排伪码）不写——代码是事实源。可选节只在该节有真实价值时写：

- **Consequences** — 非显然的连带影响才写（如 api 临时依赖 domain）
- **Considered Options**（被否方案）— **V1 阶段禁止**（零残留，§9）；退出 V1 后按 matt 恢复为可选

V1 阶段（§9）：只写最终态——status 仅 draft/stable（推荐键，可省略）、不写修订沿革、过时即删。

```markdown
---
type: Decision Record
title: <决策名>
description: <一句话：决策内容>
status: stable    # 推荐键（OKF 仅 type 必填，可省略）；V1 阶段仅 draft|stable（§9）
---

# 000N — <决策名>

{1-3 句：背景 + 决策 + 为什么}

## Consequences    # 可选——非显然的连带影响才写
- ...
```

## 5. 写作原则

- **agent 两负荷**：常驻上下文只放指针（AGENTS.md 每会话加载——每行都是触发词，不是正文）；正文经指针披露（progressive disclosure）
- **单源**：同一含义只在一处（§1 分层模型）；文档之间只互相指，不互相抄
- **入口措辞即触达**：`description` 与 AGENTS.md 指针写"何时读"的分支，不写文档身份
- **no-op 测试**：删掉某句，读者是否失去信息？——失去则留，否则删整句
- **人类可读**：渐进披露的层级对人类也是索引；认知负荷是人的选择成本，不追求为零
- **语言**：中文正文；术语/代码标识符保留英文原文，对齐 CONTEXT.md

## 6. 注释规范

编码注释标准在 `STYLEGUIDE.md §4`（Google Java Style §7 适配）。本文档不复制正文——注释标准变更改 STYLEGUIDE，本文件仅指。

## 7. 合规基准（机械可检查）

docs/ 内每个非保留名 .md：

- [ ] YAML frontmatter 可解析，`type` 非空且在词表（或明确扩展）
- [ ] `title`/`description` 非空（index.md 除外）
- [ ] ADR：`status` ∈ {draft, stable}（V1 阶段，§9；推荐键可省略）；文件名 `000N-slug.md` 顺序递增；正文 1-3 句 + 可选 Consequences（§4）
- [ ] Research：`sources` 非空
- [ ] 分层合规：CONTEXT.md 只含业务词汇（无实现细节）；ADR 无代码片段
- [ ] 链接：全部相对路径（从所在文件出发，§2.5）；无根相对 `/…` 链接
- [ ] 不破坏技能契约：CONTEXT.md 含 `## Language`；ADR 位置/编号不变；docs/agents/ 三件套存在
- [ ] 类型覆盖：framework-conventions 类型清单表中的每个基类/接口标记类型都有对应规范文档（conventions/ 族，§2.4）

检查方式：

- **代码 + 注释**：code-review 标准轴按 `STYLEGUIDE.md` 核查——注释规范自动生效，无需额外机制
- **文档变更**：在 code-review 中按本基准核查（spec 轴外新增基准轴；不引入新技能）
- **机械项**（frontmatter / 词表 / status / ADR 编号 / `## Language` / 链接）：脚本检查，07 交接方案定义验证协议（可作 CI 步骤）
- **内容过期不是 lint 项**：分层模型（§1）保证文档不重复代码/环境事实——过期源在设计上消除；git blame 是唯一辅助工具

## 8. 文档生命周期

- **新增文档**：按 §3 契约补 frontmatter → 落 bundle 位置 → 在 `docs/index.md` 加一行 → 在 AGENTS.md 加指针（如需要触发）
- **status 流转**：draft（起草）→ stable（定案）→ deprecated / superseded（被替代，正文链接指向替代者）。**V1 阶段例外见 §9**（仅 draft/stable，过时即删，决策变更就地更新）
- **删除**：过时/被替代文档删除（git 历史保留；index.md 移除条目）。**V1 阶段**：过时即删，零残留（§9）

## 9. V1 阶段条款（退出 V1 时整节删除）

代码库始终处于 V1：文档与注释**只记录最终决策结论**，不做决策回溯；历史过时内容直接删除，项目内零残留（git 历史承载过往）。本节是对 §3.2 / §4 / §8 默认生命周期的**阶段覆盖**：

1. `status` 仅用 `draft | stable`——§3.2 默认词表的 deprecated / superseded 暂停
2. ADR 正文不写被否方案、修订沿革——决策变更**就地更新**为最终状态，不另开 ADR
3. 过时内容**直接删除**，不标记状态

**退出 V1**：删除本节，§3.2 / §4 / §8 默认恢复。agent 按正常生命周期写作（完整 status 词表、superseded 链、被替代文档标记），无需其他改动。
