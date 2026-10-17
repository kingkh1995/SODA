---
type: Research
title: OKF 适配研究：OKF v0.2 在代码仓库文档场景的适用性
description: 锚点：为 SODA 文档选 frontmatter 键、type 词表或定 bundle 布局、或复核 OKF 来源漂移与钉住版本时读——OKF v0.2 适配裁定（status 全类采纳、verified/sources 按类、docs/ 为 bundle）与来源维护记录。
tags: [okf, frontmatter, documentation]
status: stable
generated:
  by: wayfinder/01
  at: 2026-08-14T00:00:00Z
verified:
  - by: human:mm
    at: 2026-08-14T00:00:00Z
  - by: human:mm
    at: 2026-09-28T00:00:00Z
stale_after: 2026-12-27T00:00:00Z
sources:
  - resource: https://github.com/GoogleCloudPlatform/open-knowledge-format
    id: okf-spec-v0.2
    author: GoogleCloudPlatform
    title: OKF v0.2 SPEC（canonical 上游，钉住 ad30107）
---
# OKF 适配研究：OKF v0.2 在代码仓库文档场景的适用性（wayfinder 01）

> 来源：OKF v0.2 规范（canonical 上游 `GoogleCloudPlatform/open-knowledge-format@ad30107`，2026-08-21，钉住事实见 §七）、该提交下的 `README.md`、`bundles/` 示例
> bundle、`tests/`（消费者实际强制的内容）。
> 用途：为「文档体系设计定案」（wayfinder 05）提供 OKF 侧输入；复核 OKF 来源漂移或钉住版本时读 §七。

## 零、OKF 是什么（一句话）

OKF = 目录化的 markdown 文件（YAML frontmatter + 结构化 body），`type` 是唯一必填键；v0.2 把 provenance（`sources`）、trust（`generated`/`verified`）、lifecycle（`status`/`stale_after`）、attestation（Attested Computation）做成一等公民；用 `index.md` 渐进披露、`log.md` 记录历史、markdown 链接表达概念间关系（SPEC §1-§3）。

**设计目标**：数据/知识目录——BigQuery 表、指标、playbook，常由 agent 生成、被 agent 消费。**非目标**（SPEC §1）：不定义概念类型 taxonomy、不规定存储/服务、不替代领域 schema。

## 一、每类仓库文档应采哪些 frontmatter 键、type 词表建议

OKF 键族（SPEC §4.1、§5）：基础（`type` 必填 / `title` / `description` / `resource` / `tags`）；provenance（`sources[].resource/id/title/author/usage_count/last_modified` + `usage_window`）；trust（`generated.{by,at}` / `verified[].{by,at}`）；lifecycle（`status`: draft|stable|deprecated / `stale_after`）。扩展键任意允许（§4.1 Extensions），消费者不得拒绝未知键（§11）。

| 文档类                           | type 建议                | 基础键                    | trust/lifecycle 建议                              | 说明                                                                                     |
|----------------------------------|--------------------------|---------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------|
| README.md                        | `Repo Overview`          | title/description/tags    | status: stable                                    | 人类入口（见 §三）；modern-web-guidance 先例：与 agent 文档分置                          |
| AGENTS.md                        | —（未登记；现状无 frontmatter） | —                       | —                                                 | **每会话自动加载**——根文档在 bundle 外，frontmatter 取舍由其 agent 契约决定、现状不带 frontmatter（`docs/doc-conventions.md` §3.3、§5.5）；正文为短指针层，描述即 context pointer            |
| CONTEXT.md                       | `Glossary`               | description/tags          | status                                            | 词汇表；必须保持 domain-modeling 可读（# Language + 术语 + _Avoid_，见 [matt-skills-doc-contracts.md](matt-skills-doc-contracts.md)）         |
| STYLEGUIDE.md                    | `Convention`             | description/tags          | status: stable + verified(可选)                   | code-review 以它为"仓库编码标准"                                                         |
| 约定文档（docs/framework-*）     | `Convention`             | description/tags          | status                                            | 同上族                                                                                   |
| ADR（docs/adr/000N-*.md）        | `Decision Record`        | description/tags          | **status: draft\|stable\|deprecated\|superseded** | 与 ADR-FORMAT 既有 Status 概念天然同构；superseded 用链接表达                            |
| research 笔记（docs/research/*） | `Research` / `Reference` | description/tags/resource | **sources（核心适用区）+ generated + verified**   | 研究文档天然带引用；agent 生成的研究由 `generated.by` 记录、人类 `verified` 提升信任层级 |
| doc-conventions（`docs/doc-conventions.md`） | `Convention`             | description/tags          | status: stable                                    | 标准落盘处                                                                               |

**type 词表**：OKF 明确 type 不集中注册（§4.1），取值自解释即可。上表取值够用（本项目现登记词表见 `docs/doc-conventions.md` §3.1）；未知 type 消费者须宽容（§11）。

## 二、provenance/trust/lifecycle 在 git 仓库里的价值

**git 已覆盖的部分**：`generated.at`（谁何时写）≈ git blame/历史；actor 约定（§7）≈ git author——对**人类撰写**的文档是冗余。

**git 不覆盖、OKF 独有的部分**：

| 机制 | git 覆盖？ | 对仓库文档的价值 | 建议 |
|---|---|---|---|
| `status`（draft/stable/deprecated） | ✗ | **高**——ADR 的 proposed/accepted/superseded、文档草稿 vs 定稿，git 表达不了"当前是否有效" | **采纳**；词表映射到本项目已有 Status 语义 |
| `verified`（人类复核 → trust tier，§5.3） | ✗ | **中**——agent 生成内容（research/原型/自动文档）进仓库后，"机器确认 vs 人类复核"是可消费信号；纯人类文档则冗余 | **采纳但克制**：仅 agent 生成或高风险文档用 |
| `sources` + 逐条脚注归因（§5.1） | ✗ | **中**——research/引用型文档的核心价值；普通约定文档不需要 | **按类采纳**：research 必用，其余不用 |
| `generated` | 部分（≈git blame） | 低——除非内容由 agent 生成需标明 producer/version | 省略为主；agent 生成时用 |
| `stale_after` | ✗ | 低——仓库文档的"新鲜度"由代码本身表达（代码变了文档就过时）；`stale_after` 只对引用外部易变事实（API 版本号）的文档有意义 | 默认省略；需要时允许 |
| `usage_count`/`usage_window` | ✗ | **极低**——仓库文档没有 dashboard 浏览量语义 | **跳过** |

**结论**：采纳 `status`（全类）+ `verified`/`sources`（按类）；`generated`/`stale_after`/`usage_count` 作为**允许但不要求**的扩展键保留（schema 宽松性保证未来 agent 维护文档时可用，§11）。

## 三、bundle 布局：仓库根 vs docs/

OKF bundle = 目录树；**合规性要求树内每个非保留名 .md 都有 frontmatter**（§11 第 1 条：every non-reserved .md 可解析 frontmatter + 非空 type）。

**方案 A：仓库根即 bundle**（index.md 在根，docs/、根文档都是概念）
- ✅ 语义完整：整个仓库的知识是一个 bundle；README/AGENTS/CONTEXT/STYLEGUIDE 全纳入
- ❌ **冲突**：`.scratch/` 内 PRD + 票据是 setup 技能工作流产物（**本项目已裁定 out of scope**，不改格式）——树内存在无 frontmatter 的 .md，**整树不合规**（OKF 无 ignore 机制；SPEC §3 仅保留 index.md/log.md 特殊名）
- ❌ 根 index.md 与 README.md 职责重叠（§8 规定 index.md 无 frontmatter 仅列目录；README 是人类叙事入口——两文档并存需明确分工）

**方案 B：docs/ 为 bundle**（docs/index.md + docs/adr/ + docs/research/ + …）
- ✅ 合规干净：docs/ 内全是我们可控制的文档；`.scratch/` 与根文档在 bundle 之外，无合规压力
- ✅ index.md 渐进披露只覆盖 docs/，量级合适（根目录概念文档数量少，不需要索引）
- ✅ 根文档（README/AGENTS/CONTEXT/STYLEGUIDE）**可选**加 frontmatter——它们不在 bundle 内，加了是扩展、不加不违规
- ❌ 语义分裂：根 4 文档 + docs/ 两部分知识不在一棵树下；跨域链接（CONTEXT ↔ ADR）要用 bundle 相对路径 `/adr/…` 或跨出 bundle 的链接（OKF 允许链接指向 bundle 外，§6.2 接受绝对 URL/相对路径）

**推荐**：方案 B（docs/ 为 bundle），根文档作为 bundle 外入口按需轻量 frontmatter（已按此定案，见 [doc-conventions.md](../doc-conventions.md) §2）；若坚持"全部文档统一 schema"，需先处理 `.scratch/` 排除问题（方案 A + 显式非合规区声明）。

## 四、跨文档链接与 references/ 约定

- **绝对（bundle 相对）链接是 OKF 的推荐形式**（§6.1）：`/adr/0012-url-naming-convention.md`——文档移动不破链；相对链接仅限同目录相邻概念。**本项目偏离该推荐**：规则单源见 [doc-conventions.md](../doc-conventions.md) §6.2 与偏离表 D1（从所在文件出发的相对路径，禁用根相对 `/…`——GitHub 会把它解析到域根）；此处只记录上游原文倾向。
- 链接表达的关系类型（父/子、引用、依赖）由周边 prose 传达，链接本身无类型（§6.1）——**agent 消费时可把全部链接当有向边建图**（viz 就是这么做的）。
- `references/` 约定（§6.3）：镜像外部材料/运行说明为一级概念。对 SODA：`docs/research/` 已承担"引用外部材料"职责，可视为 references/ 的本地化；若未来要镜像 yudao 文档片段/第三方规范，建 `docs/references/`。
- 断链不违规（§6.1 Consumers MUST tolerate broken links）——渐进写作友好；**本项目偏离**：文档级硬门要求目标与锚点可达（[doc-conventions.md](../doc-conventions.md) 偏离表 D2）。
- 注意：AGENTS.md/CONTEXT.md 在 bundle（docs/）之外时，CONTEXT ↔ ADR 互链是跨 bundle 链接——用相对路径（`docs/adr/…`）比 `/adr/…`（会解析到仓库根）更稳。

## 五、现实先例

- **OKF 示例 bundle**：OKF 自身的示例 bundle（canonical 仓 `GoogleCloudPlatform/open-knowledge-format@ad30107` 中的 ga4、stackoverflow、crypto_bitcoin、acme_retail——全是表/指标/计算）**没有代码仓库文档场景的先例**。
- **代码仓库 + agent 文档**：最接近的现实实践是 [GoogleChrome/modern-web-guidance-src 的 CONTEXT.md](https://github.com/GoogleChrome/modern-web-guidance-src/blob/main/CONTEXT.md)——仓库自述为 auto-maintained LLM context 文档，明确声明不替代人类贡献者 README，两者分置。它**不用 OKF frontmatter**，但验证了"代码仓库里为 agent 维护独立上下文文档"这一模式。
- **结论**：OKF 应用于代码仓库文档**没有现成先例**，属新颖适配——正因无先例，先以原型验证适配是否成立，再作定案，且需明示"我们是在扩展 OKF 的应用边界"这一事实。

## 六、定案落点

本篇只保留来源事实与适配分析。frontmatter 键与 `type` 词表、bundle 布局、链接形式的项目定案单源在 [doc-conventions.md](../doc-conventions.md)（§3.1/§3.2、§2、§6.2 与偏离表 D1/D2），本篇不复制其正文。

## 七、来源与钉住版本（维护记录）

- **canonical 上游**：[`GoogleCloudPlatform/open-knowledge-format`](https://github.com/GoogleCloudPlatform/open-knowledge-format)；本轮规范 **OKF 0.2**，钉住 commit [`ad30107c31c06aec8a7d5636e0d1058118604e6f`](https://github.com/GoogleCloudPlatform/open-knowledge-format/commit/ad30107c31c06aec8a7d5636e0d1058118604e6f)（2026-08-21）；上游当前无 release/tag，故以 commit 钉住。
- **离线快照**：`knowledge-catalog/okf/` 中的 `SPEC.md` 与该 commit 的 `SPEC.md` 字节一致（1,006 行），但其 README 已声明该目录冻结、迁至独立仓库——快照只作只读对照，不是权威来源；规则引用一律走 canonical 上游（[doc-conventions.md](../doc-conventions.md) §4.2、§9.2）。
- **比对记录**：2026-09-28 `git ls-remote` 比对 canonical `HEAD` = `ad30107` ⇒ **无漂移**，钉住版本与规范零改动；`stale_after` = 2026-12-27（本次确认日 + 90 天），到期即检。
- **比对记录**：2026-10-17 全量终检比对 `git ls-remote` canonical `HEAD` = `ad30107c31c06aec8a7d5636e0d1058118604e6f` ＝ 钉住 commit ⇒ **无漂移**；已比对、影响面：无，钉住版本与规范零改动。
- **流程**：比对触发、L1–L3 分级与 owner 见 [doc-conventions.md](../doc-conventions.md) §9.2。
