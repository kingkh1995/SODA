# matt-pocock-skills 文档消费契约研究（wayfinder 03）

> 研究日期：2026-08-14。来源：`D:\Workspace\skills`（matt-pocock/skills 源码克隆）与 `C:\Users\mm\.claude\skills`（本地安装副本）的 SKILL.md 原文、`docs/agents/` 模板、CONTEXT-FORMAT.md / ADR-FORMAT.md、AGENT-BRIEF.md。
> 用途：为「文档体系设计定案」（wayfinder 05）提供"技能侧契约"输入——重构后的文档集必须满足什么、可以改什么。

## 一、技能 → 文档映射（谁读什么、期望什么结构）

| 技能 | 读什么 | 期望的结构（硬契约） |
|---|---|---|
| **setup-matt-pocock-skills** | 仓库现状：AGENTS.md/CLAUDE.md、CONTEXT.md、docs/adr/、.scratch/、monorepo 信号 | 产出 `docs/agents/{issue-tracker,domain,triage-labels}.md` + AGENTS.md 内 `## Agent skills` 块（无则建 AGENTS.md 而非 CLAUDE.md） |
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

| 文档 | 进入上下文方式 | load 类型 | 重构含义 |
|---|---|---|---|
| AGENTS.md | **每会话自动加载**（本 harness 证实） | context load，常驻 | 必须保持精简；每行都该是"指针"而非"正文"。重构时内容可改，但**不能指望它承载大段知识** |
| CONTEXT.md | domain.md 规则驱动，探索前读取 | pointer 触达（disclosed） | 词汇表被 domain-modeling/to-spec/to-tickets/triage 读——**结构必须兼容 CONTEXT-FORMAT**；内容可重写但要"术语 + _Avoid_ + 无实现细节" |
| docs/adr/* | 改代码前读相关 ADR（domain.md） | pointer 触达 | 位置（docs/adr/）与编号（000N-slug）是硬契约 |
| docs/agents/* | 技能显式查找（setup 产物） | pointer 触达 | 文件存在性 + 内容角色是 setup 契约 |
| STYLEGUIDE.md | code-review 按"仓库编码标准"读取 | pointer 触达 | 标题/存在性需让 code-review 能识别为标准；内容自由 |
| .scratch/ | 技能按 tracker 约定读写 | 工作流数据 | **格式由 to-spec/to-tickets/triage/wayfinder 硬编码**——本项目已裁定 out of scope，正确 |

**要点**：重构后文档的"入口措辞"（context pointer）决定技能是否触达。AGENTS.md 是全局指针层——它应该指向"何时读 CONTEXT.md、何时读 ADR、何时读 STYLEGUIDE"，而不是复述它们的内容（单源原则）。

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

### 契约张力（重构必须裁决的冲突点）

1. **CONTEXT-FORMAT"定义 1-2 句、只收领域特有术语、零实现细节" vs SODA CONTEXT.md 现状**：SODA 的 CONTEXT.md 有长段落描述、实现细节（序列化格式、Modulith 表、ADR 引用注记）。严格按格式，大量内容要被移除/外移；按 SODA 现状，则是在扩展格式。**05 需裁决**：收编到格式（外移实现细节到 ADR/conventions），还是显式扩展本项目的词汇表格式（如条目允许"行为描述 + 实现注记"两段）。
2. **ADR-FORMAT"1-3 句即完整 ADR" vs SODA 23 个长篇结构化 ADR**（问题/决策/被否定方案/后果/参考）：SODA 的更严格（信息量大、可审计），但偏离技能模板。**05 需裁决**：保留 SODA 结构（并把它声明为本项目的 ADR 扩展格式），还是削足适履。
3. **OKF frontmatter vs 技能解析**：CONTEXT.md/ADR 加 YAML frontmatter 是否破坏技能读取？——不会：CONTEXT-FORMAT 按标题解析（frontmatter 是 `---` 分隔的头部，标题仍在）；ADR-FORMAT 的 Status 本来就是 frontmatter 用法。**frontmatter 与技能契约兼容**，但 05 需验证原型（04 票）。

## 四、MUST-satisfy 清单（重构后文档集必须满足）

1. AGENTS.md 存在且保持精简（每会话自动加载；指针层，不复述正文）
2. CONTEXT.md 存在，含 `## Language` 标题与术语条目结构（术语 + 描述 + `_Avoid_:`）
3. ADR 在 `docs/adr/000N-slug.md`，编号顺序递增；正文详略可改
4. `docs/agents/` 三件套存在（setup 契约）；domain.md 措辞可改
5. STYLEGUIDE.md 存在且能被 code-review 识别为编码标准
6. `.scratch/` 布局不动（tracker 硬契约；out of scope）
7. to-spec/to-tickets/AGENT-BRIEF 模板结构不动（技能内嵌）
8. 新增 frontmatter 不得破坏 2/3 的解析结构（原型验证）
9. 文档用词对齐 CONTEXT.md 词汇表（domain-modeling 要求，贯穿所有技能输出）
10. 任何文档若被改写，保持"单源"——同一含义不跨文档重复（writing-for-agents 剪枝原则）

## 五、决策支持摘要（给 05）

- 重构的自由度主要在**内容层**（详略、措辞、范围、风格），**结构层**（标题、位置、编号、模板、tracker 布局）大多是技能硬契约。
- 两处实质裁决点：CONTEXT.md 的"格式紧凑 vs 现状丰富"、ADR 的"极简模板 vs 现状长篇"——建议都走"显式扩展"路线（声明为本项目扩展格式，写入 doc-conventions），因为现状信息量是资产，且 23 个 ADR 的回退成本不可接受。
- 文档体系重构不破坏技能的前提：结构契约清单（§四）逐条满足 + 04 原型验证 frontmatter 兼容性。
