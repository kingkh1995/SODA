---
type: Research
title: matt-pocock-skills 文档消费契约研究
description: 锚点：重构 SODA 文档体系（AGENTS/CONTEXT/ADR/STYLEGUIDE/agents 目录）、或复核 writing-for-agents 来源漂移与钉住版本时读——matt-pocock 技能硬编码的结构契约与可改边界。
tags: [agent-skills, documentation, contracts]
status: stable
generated:
  by: wayfinder/03
  at: 2026-08-14T00:00:00Z
verified:
  - by: human:mm
    at: 2026-08-14T00:00:00Z
  - by: human:mm
    at: 2026-09-28T00:00:00Z
stale_after: 2026-12-27T00:00:00Z
sources:
  - resource: https://github.com/mattpocock/skills
    id: mattpocock-skills
    author: Matt Pocock
    title: matt-pocock/skills（SKILL.md · docs/agents 模板 · FORMAT 文件；钉住 c55ee46）
---
# matt-pocock-skills 文档消费契约研究（wayfinder 03）

> 来源：canonical 上游 [`mattpocock/skills`](https://github.com/mattpocock/skills) 的 `SKILL.md` 原文、`docs/agents/`
> 模板、CONTEXT-FORMAT.md / ADR-FORMAT.md、AGENT-BRIEF.md；本地安装副本与上游逐字一致（钉住与比对记录见 §六）。
> 用途：为「文档体系设计定案」（wayfinder 05）提供"技能侧契约"输入——技能硬编码哪些结构、哪些可以改。

## 一、技能 → 文档映射（谁读什么、期望什么结构）

| 技能 | 读什么 | 期望的结构（硬契约） |
|---|---|---|
| **setup-matt-pocock-skills** | 仓库现状：AGENTS.md/CLAUDE.md、CONTEXT.md、docs/adr/、.scratch/、monorepo 信号 | 产出 `docs/agents/{issue-tracker,domain,triage-labels}.md` + AGENTS.md 内技能入口块（本仓现行节名 `## Agent 工作流`；无则建 AGENTS.md 而非 CLAUDE.md） |
| **domain.md 消费规则**（全技能共用） | CONTEXT.md（或 CONTEXT-MAP.md）+ docs/adr/（工作区域相关） | 先读再探索；用词汇表术语；ADR 冲突必须显式 surface |
| **domain-modeling** | CONTEXT.md、docs/adr/ | CONTEXT-FORMAT：`# 名称` + 描述 + `## Language` + 术语条目（术语 + 描述 + `_Avoid_:`）；**定义 1-2 句、不掺实现细节**。ADR-FORMAT：`docs/adr/000N-slug.md` 顺序编号、标题 + 1-3 句正文；Status 为可选 frontmatter |
| **grill-with-docs** | = grilling + domain-modeling | 边聊边写 CONTEXT.md / ADR（惰性创建） |
| **to-spec** | 仓库状态 + 领域词汇 + 相关 ADR | 产出 spec 发布到 tracker，模板：Problem Statement / Solution / User Stories（长篇编号列表）/ Implementation Decisions（**禁文件路径与代码片段**）/ Testing Decisions / Out of Scope / Further Notes；打 ready-for-agent 标签 |
| **to-tickets** | spec/issue 全文 + 代码库 | 产出 `.<scratch>/<feature>/issues/NN-<slug>.md` 每票一文件，模板：What to build / Blocked by / Status: ready-for-agent / Acceptance criteria（勾选）；**禁文件路径**（原型编码决策片段例外） |
| **implement** | spec / 票据 | 按票据实现；tdd 于接缝；最后 code-review；提交到当前分支 |
| **triage** | issue/PR 全文、.out-of-scope/*.md、代码库 | 状态机标签（needs-triage/needs-info/ready-for-agent/ready-for-human/wontfix）；产出 AGENT-BRIEF（Category/Summary/Current/Desired/Key interfaces/Acceptance criteria/Out of scope——**禁文件路径与行号**）或 Triage Notes |
| **wayfinder** | map.md + issues/NN-<slug>.md | 地图：Destination/Notes/Decisions so far/Not yet specified/Out of scope；票据：Type:/Status:/Blocked by: 行 + Question body；frontier = 开放无阻塞未认领 |
| **code-review** | diff + 仓库编码标准 + 源 issue/spec | Standards 轴读"仓库文档化的编码标准"（= STYLEGUIDE.md） |
| **tdd** | 测试约定 | red-green-refactor；行为测试不测实现细节 |
| **research** | 一手来源 | 发现写单文件 markdown，存"仓库既有的此类笔记位置"（= docs/research/） |
| **writing-for-agents** | 任何被 agent 触达的文档 | context pointer 措辞决定触达可靠性；两负荷（context/cognitive）；信息层级（in-file step / in-file reference / disclosed reference）；leading words；单源；无 no-op |

## 二、context load 分析（哪些文档什么时候进上下文）

按 writing-for-agents 的两负荷框架，SODA 现状：

| 文档 | 进入上下文方式 | load 类型 | 技能侧结构约束 |
|---|---|---|---|
| AGENTS.md | **每会话自动加载**（本 harness 证实） | context load，常驻 | 技能只依赖其触发行措辞与路由角色，不解析内部结构；正文详略不受契约约束 |
| CONTEXT.md | domain.md 规则驱动，探索前读取 | pointer 触达（disclosed） | 被 domain-modeling / to-spec / to-tickets / triage 按 CONTEXT-FORMAT 标题解析：`## Language` ＋ 术语条目（术语 + 描述 + `_Avoid_`） |
| docs/adr/* | 改代码前读相关 ADR（domain.md） | pointer 触达 | 位置 `docs/adr/` ＋ `000N-slug.md` 顺序编号是硬契约 |
| docs/agents/* | 技能显式查找（setup 产物） | pointer 触达 | 三件套的存在性与角色是 setup 契约 |
| STYLEGUIDE.md | code-review 按"仓库编码标准"读取 | pointer 触达 | 存在性与标题需让 code-review 识别为标准；正文内容自由 |
| .scratch/ | 技能按 tracker 约定读写 | 工作流数据 | 布局与 `Status:`/`Blocked by:` 行由 to-spec / to-tickets / triage / wayfinder 硬编码（本项目已裁定 out of scope） |

**要点**：文档的入口措辞（context pointer）决定技能是否触达。措辞与单源规则由 `writing-for-agents` 承载（本项目的触发分支见 [doc-conventions.md](../doc-conventions.md) §7.3），本篇不复述。

## 三、可改 vs 硬编码（重构的自由度边界）

### 硬编码在技能里的（改了技能就读不到 → 重构必须满足）

1. CONTEXT.md 的 **`## Language` 标题 + 术语条目结构**（CONTEXT-FORMAT 解析依赖标题）
2. ADR 的 **位置 `docs/adr/` + `000N-slug.md` 编号 + 顺序递增**
3. 本地 tracker 的文件布局（`.scratch/<feature>/PRD.md` + `issues/NN-<slug>.md`、Status/Blocked by 行）
4. to-spec / to-tickets / AGENT-BRIEF 的**模板结构**（技能内嵌）
5. triage 五状态角色 + 标签映射（docs/agents/triage-labels.md）
6. setup 产物三件套的位置与角色（docs/agents/*）
7. wayfinder 的地图/票据布局（Type:/Status:/Blocked by: 行）

### 仓库侧约定（可随重构调整）

1. **docs/agents/domain.md 的消费规则本身**（setup 写入、可编辑——"先读 CONTEXT.md + 相关 ADR"可以改措辞、加读 STYLEGUIDE 等）
2. AGENTS.md 内容（指针层措辞）
3. STYLEGUIDE.md 内容（编码标准本体）
4. CONTEXT.md 内容的详略与范围（**但不能破坏 §硬1 的结构**）
5. ADR 的正文详略（位置/编号是契约，**详略不是**——见 §四 张力）
6. docs/research/ 的存放约定（research 技能按"既有约定"写，SODA 已有）

### 契约张力（技能模板 vs 本项目现状）

| 张力 | 技能模板 | 本项目现状 | 落点 |
|---|---|---|---|
| 词汇表 | CONTEXT-FORMAT：定义 1–2 句、只收领域特有术语、零实现细节 | 根 `CONTEXT.md` 有长段落与实现注记 | 根文档在 bundle 外，格式取舍归其独立治理边界（[doc-conventions.md](../doc-conventions.md) §3.3）；本篇只记录差异 |
| ADR | ADR-FORMAT：标题 ＋ 1–3 句正文，Status 可选 | 标准已定 V1 形态：正文 1–3 句，`Consequences` 仅在非显然连带影响时写 | 规则单源 [doc-conventions.md](../doc-conventions.md) §5.3；技能侧位置与编号契约未变（上节硬编码 1–2） |
| frontmatter | 技能按标题与文件路径解析，不读 YAML | 根文档与 `docs/` 采用 OKF frontmatter | 兼容：frontmatter 不破坏标题解析（原型验证）；frontmatter 规则单源 [doc-conventions.md](../doc-conventions.md) §3 |

本篇只记录契约事实与差异，不承载文档格式规则。

## 四、MUST-satisfy 清单（重构后文档集必须满足）

1. AGENTS.md 存在且保持精简（每会话自动加载；指针层，不复述正文）
2. CONTEXT.md 存在，含 `## Language` 标题与术语条目结构（术语 + 描述 + `_Avoid_:`）
3. ADR 在 `docs/adr/000N-slug.md`，编号顺序递增；正文详略可改
4. `docs/agents/` 三件套存在（setup 契约）；domain.md 措辞可改
5. STYLEGUIDE.md 存在且能被 code-review 识别为编码标准
6. `.scratch/` 布局不动（tracker 硬契约；out of scope）
7. to-spec/to-tickets/AGENT-BRIEF 模板结构不动（技能内嵌）
8. 新增 frontmatter 不得破坏 2/3 的解析结构（原型验证）
9. 用词对齐领域词汇表、跨文档单源两条要求由 `domain-modeling` 与 `writing-for-agents` 承载（本项目的触发分支见 [doc-conventions.md](../doc-conventions.md) §7.3）——此处不复制其正文

## 五、定案落点

本篇只保留技能契约事实。文档体系的项目定案（frontmatter 契约、bundle 布局、ADR 与导航 profile、写·检视清单）单源在 [doc-conventions.md](../doc-conventions.md)，本篇不复制其正文，也不再给重构建议。

## 六、来源与钉住版本（维护记录）

- **canonical 上游**：[`mattpocock/skills`](https://github.com/mattpocock/skills)；钉住 commit `c55ee46073ed923f86ce59a5eb3b6d895095d1b7`；`SKILL.md` SHA-256 `1c0c4ebf2d221917591144f0ced4fe46dae13301cb3d6d93fc88ecf0dda6aed2`。
- **本地一致性**：本地安装副本的 `SKILL.md` 与该提交逐字一致（SHA-256 同值）。
- **比对记录**：2026-09-28 `git ls-remote` canonical `HEAD` = `c55ee46` ⇒ **无漂移**，钉住版本与规范零改动；`stale_after` = 2026-12-27（本次确认日 ＋ 90 天），到期即检。
- **比对记录**：2026-10-17 全量终检比对 `git ls-remote` canonical `HEAD` = `49dd158d1076134a641b33efb035946536778336` ≠ 钉住 `c55ee46`；compare diff 118 个文件**未命中**被引文本 `writing-for-agents/SKILL.md`（字样命中仅其 docs 页与一条 out-of-scope 说明；其余为其它技能与站点文件）⇒ **L1**：已比对、影响面：无，钉住版本与规范零改动；本地安装副本 `SKILL.md` SHA-256 复测仍为 `1c0c4ebf…aed2`，与登记一致。
- **流程**：比对触发、L1–L3 分级与维护 owner 见 [doc-conventions.md](../doc-conventions.md) §9.2。
