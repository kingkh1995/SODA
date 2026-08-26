---
type: Research
title: Google 注释规范研究
description: 锚点：制定或评审 SODA 注释/Javadoc 标准（格式、块标签、必写范围、中文冲突、agent 注释）时，按 Google Java Style §7 与 Developer Style 规则迁移执行。
tags: [google-style, javadoc, comments]
status: stable
generated:
  by: wayfinder/02
  at: 2026-08-14T00:00:00Z
verified:
  - by: human:mm
    at: 2026-08-14T00:00:00Z
sources:
  - resource: https://google.github.io/styleguide/javaguide.html
    id: google-java-style
    author: Google
    title: Google Java Style Guide（§7 Comments）
  - resource: https://developers.google.com/style
    id: google-developer-style
    author: Google
    title: Google Developer Documentation Style Guide
---
# Google 注释规范研究（wayfinder 02）

> 用途：为「注释标准定案」（wayfinder 06）提供 Google 侧输入。
> 来源均为 Google 官方文档或 Google 工程师一手材料；引用保留原文，转述标注〔转述〕。

## 一、Google Java Style Guide 注释规则（javaguide.html §7）

官方地址：https://google.github.io/styleguide/javaguide.html

### 1.1 Javadoc 格式（§7.1.1 General form）

- Javadoc 块以 `/**` 开头、`*/` 结尾，内部每行以 `*` 开头（与起始 `/**` 对齐）。
- 整个注释（含定界符）能在一行放下、且不含块标签时，可用单行形式 `/** … */`。
- 〔转述〕禁止装饰性星号边框（如 `/** * * * */` 之类的盒子）——这是块注释规则，Javadoc 同样适用。

### 1.2 段落（§7.1.2 Paragraphs）

- 第一个句子是**摘要片段（summary fragment）**：以句号结尾的完整陈述句，简洁描述元素。
- 摘要与后续描述之间空一行；新段落用空行或显式 `<p>` 分隔。

### 1.3 块标签（§7.1.3 Block tags）

- 块标签固定顺序：`@param` → `@return` → `@throws` → `@deprecated`。
- 四种标签**不允许空描述**。
- 标签行放不下时，续行相对 `@` 符号缩进 ≥ 4 空格。

### 1.4 摘要片段的写法（§7.2，含官方 Tip）

- 常见错误：`/** @return the customer ID */` → 应为 `/** Returns the customer ID. */` 或（Java 16+ 支持）`/** {@return the customer ID} */`。
- 摘要用**陈述句**描述行为，不用"@return 引用式"写法。

### 1.5 何时必须写 Javadoc（§7.3 Where Javadoc is required）

- 必须：所有 public/protected 的类、接口、方法、构造器、字段，以及构成外部 API 的任何元素。
- **例外 7.3.1 — 自解释方法（self-explanatory）**：方法签名已充分自明、无更多值得说明的内容时，Javadoc 可省略（如简单的 `getFoo()`）。
- **例外 7.3.2 — 覆写（overrides）**：`@Override` 方法不要求重复父类 Javadoc（若确有补充，可写）。

### 1.6 块注释与行注释（§4.8 附近 Formatting 规则〔转述，含引用来源确认〕）

- 块注释：与周围代码同级缩进；可用 `/* … */` 或 `//` 风格；多行 `/*` 注释的后续行与起始 `*` 对齐；**禁止星号装饰边框**。
- 行注释：`//`，与所注释代码同级缩进。

### 1.7 特殊注释 TODO（§7.4 Special comments〔转述〕）

- 格式：`TODO:`（全大写）+ 冒号 + 链接（首选 bug 引用，因 bug 有跟踪与后续评论）+ 连字符 + 说明串。
- 用途定位：仅标记**临时的、不完美的代码**；**不是**清晰自解释代码或对实现透彻理解的替代品——克制性最强的一条。

## 二、Google Developer Documentation Style Guide 中可迁移到注释的原则

官方地址：https://developers.google.com/style（含子页 /style/voice 等）

- **主动语态优先**：明确谁执行动作；被动语态仅在特定场景可接受（如动作执行者无关紧要时）。
- **现在时**：描述代码行为用现在时（"The function returns a string"），不用将来时/过去时。
- **第二人称**：指令直接对读者（"you"），而非第三人称。
- **简洁**：删去不必要词汇（concision 条目）。
- **术语一致**：同一概念全库同一术语、同一拼写（如 "data center" 而非 "datacenter"）。
- **牛津逗号**：列举用序列逗号。
- **API 文档要求**：每个 public 类、方法、参数、返回值、异常都须用同样的主动、现在时风格清楚描述——与 Java Style §7.3 呼应。

〔转述〕优先级：项目自身风格 > 本指南 > 第三方参考（Merriam-Webster 等）——即项目级约定（如 SODA 的 STYLEGUIDE）优先于 Google 通则。

## 三、Google 面向 AI agent 的注释/文档指导

### 3.1 Google 官方仓库现实先例：GoogleChrome/modern-web-guidance-src 的 CONTEXT.md

官方仓库：https://github.com/GoogleChrome/modern-web-guidance-src/blob/main/CONTEXT.md

- 文档自述：**auto-maintained LLM context document**，向在本仓库工作的 AI agent 提供总体项目目标、架构、工作流细节；**明确声明"不打算替代给人类贡献者的 README"**。
- 启示：Google 自己在代码仓库里维护一份"给 agent 的上下文文档"，与给人类的文档**分置**；agent 上下文文档是活文档（auto-maintained），随代码演进。
- 这直接印证本 effort 的核心张力：agent 消费的文档 ≠ 人类 README，两者可以（且 Google 实践是）分开设计。

### 3.2 Google 工程师实践（Addy Osmani，Chrome 开发者体验负责人）

- 用注释引导 agent：在代码片段前给出意图与注意点（"当前实现是 X，要扩展为 Y，注意别破坏 Z"）——注释作为 agent 的上下文提示。
- 给 agent 的 spec：结构像严肃设计文档、意图清晰、让 agent 先展开计划。
- 〔转述〕要点：注释与文档对 agent 而言是**意图载体**，不是行为记录。

### 3.3 业界共识（跨来源归纳，供参考）

- 注释放置规则需显式化（方法注释写在前还是后、缩进规则、API 文档生成器是否启用、详略程度）——应写进版本控制的 agents.md/风格指南，作为 agent 系统提示的一部分。
- 给 agent 的文档与给人看的文档**工作方式不同**：不能指望内联注释让 agent 自己拼出全局，需要高层综合文档（progressive disclosure 的高层入口）。
- agent 上下文窗口是有限资源：高层综合文档优先于重复代码片段。

〔注意〕3.3 含非 Google 来源（Anthropic、Stack Overflow Blog、独立作者），仅作背景，不作为 Google 规范引用。

## 四、与"注释用中文"（项目裁定）的冲突分析

| Google 规则 | 与中文注释的冲突 | 结论 |
|---|---|---|
| §7 结构与标签顺序 | 无冲突（结构与语言无关） | 照搬 |
| §7.2 摘要用陈述句 | 无冲突；中文同样要求陈述句（"返回客户 ID。"） | 照搬，中文句号收尾 |
| 主动语态/现在时 | 中文天然弱时态；主动语态可迁移（"该方法返回…"） | 照搬原则，中文无时态问题 |
| 术语一致 | 无冲突——SODA 已有 CONTEXT.md 词汇表约束 | 照搬，对齐 CONTEXT.md 术语 |
| 牛津逗号 | 中文不使用逗号序列规则 | 不适用，跳过 |
| TODO 格式 | `TODO:` 保留英文大写前缀（工具/CI 可识别），说明串可用中文 | 部分照搬：前缀英文、说明中文 |

**结论：Google 注释规则的结构性部分与中文无冲突；语言相关部分（时态）中文天然满足或可等效迁移。唯一需要显式约定的是 TODO 前缀保留英文。**

## 五、决策支持表：Google 规则 → SODA 分层适用建议

| Google 规则 | SODA 适用建议（待 06 定案） |
|---|---|
| Javadoc 必须：public/protected 外部 API 元素 | domain 聚合/DP/接口、api 接口、adapter 端点 → 必写；infrastructure 实现类可降级 |
| 自解释方法例外 | `getFoo()` 式访问器 → 省略或一句话 |
| 覆写例外 | `@Override` 不重复父类文档 |
| 块标签顺序 + 非空 | 照搬；现有代码基本符合 |
| 摘要陈述句 | 照搬；修正现有 `@return` 引用式写法 |
| 克制原则（不注释显而易见代码 + TODO 定位） | 与现有"rich 中文 prose Javadoc"现实张力最大——06 需定迁移策略（保留意图型注释、删除行为复述型） |
| Developer Style：主动语态/现在时/简洁 | 作为注释写作风格条目并入 STYLEGUIDE |
| agent 上下文文档分置（modern-web-guidance 先例） | 与 03（matt 契约）交叉——agent 入口文档（AGENTS.md/CONTEXT.md）与人类文档分离设计 |
| agent 意图型注释（Osmani） | 注释写"为什么"，不写"做了什么"（后者是代码本身） |

## 引用清单

- Google Java Style Guide — https://google.github.io/styleguide/javaguide.html（§7.1.1/7.1.2/7.1.3/7.2/7.3/7.3.1/7.3.2/7.4、块/行注释、TODO）
- Google Developer Documentation Style Guide — https://developers.google.com/style；Active voice — https://developers.google.com/style/voice
- GoogleChrome/modern-web-guidance-src/CONTEXT.md — https://github.com/GoogleChrome/modern-web-guidance-src/blob/main/CONTEXT.md（auto-maintained LLM context 先例）
- Addy Osmani（Google Chrome）：LLM coding workflow（2026）、"How to write a good spec for AI agents"
- 背景参考（非 Google）：Anthropic effective context engineering；Stack Overflow Blog "Building shared coding guidelines for AI (and people too)"（2026-03-26）
