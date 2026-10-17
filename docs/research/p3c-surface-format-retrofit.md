---
type: Research
title: P3C 表面格式借鉴调研（DP/Entity/AIP/注释规范）
description: 锚点：评估在 DP 设计规范、Entity/Aggregate 设计规范、Google AIP API 设计规范、Google 注释规范四个点借鉴 P3C《阿里巴巴 Java 开发手册》的【强制/推荐/参考】三级标定 + ❌/✅ 反例正例对照 + 表格列统一化的价值与采用边界。整改仅限表面格式，不动章节结构与规则内容。
tags: [ p3c, format, retrofit, dp, entity, aip, comment ]
status: stable
sources:
  - resource: https://github.com/alibaba/p3c
    id: alibaba-p3c
    title: Alibaba Java Coding Guidelines（P3C）
    author: Alibaba
  - resource: https://google.aip.dev
    id: google-aip
    author: Google
    title: Google API Improvement Proposals
  - resource: https://google.github.io/styleguide/javaguide.html
    id: google-java-style
    author: Google
    title: Google Java Style Guide（javaguide.html；当前 §7 Javadoc，implementation comments/TODO 在 §4.8.6）
generated:
  by: sub-agent/scout×4
  at: 2026-09-02T00:00:00Z
---

# P3C 表面格式借鉴调研

> **边界**：仅调研「表现形式」——【强制/推荐/参考】三级标定、❌/✅ 反例正例对照、表格列统一化；
> 不调研 P3C 规则内容本身，不动章节结构与规则语义，不引入工具链（checkstyle/Sonar）。
> **单源**：采用结论落盘 [../adr/0036-p3c-surface-format-retrofit.md](../adr/0036-p3c-surface-format-retrofit.md)；
> 规则正文单源在 `STYLEGUIDE.md` §4 与 `docs/doc-conventions.md`，本篇只留事实、差距与采用边界。

## 一、调研结论与采用边界

**值得局部借鉴，不值得全套整改。** 四个点收益不均，最终采用边界：

| 范围                                                    | 采用边界                                                                                    |
|---------------------------------------------------------|---------------------------------------------------------------------------------------------|
| DP 设计规范（`docs/dp-conventions.md` ＋ `docs/conventions/dp-*` 三篇） | **借鉴**——补统一的级别列与 ❌/✅ 对照                                                        |
| Entity / Aggregate 设计规范（类型契约与跨切面两篇）      | **借鉴**——四个点中收益最高：调研时是纯叙述体、无表格、无级别列                              |
| AIP（Research ＋ adapter）                               | **部分借鉴**——表面格式只落在 adapter 侧；Research 保持研究层，不升格为规范                   |
| Google 注释（`STYLEGUIDE.md` §4）                        | **不借鉴**——形式已等价，无需按 P3C 改造                                                     |

**已采用的形式**（规则正文不在本篇）：三级标定、❌/✅ 反例正例、统一规则表列已登记为形式先例——
`docs/doc-conventions.md` §4.1（级别）与 §4.3（规则表与示例的表达约定、形式先例）；注释侧由 `STYLEGUIDE.md` §4 承载
（§4.1 级别定义、§4.2 ❌/✅ 示例、§4.3 TODO 格式、§4.4 写/不写表与知识锚点）。

**明确不采用**：P3C 规则内容与章节结构；把 P3C 当外部权威（`docs/doc-conventions.md` §4.3：不把 P3C 当外部权威）；
全套手册化改造与工具链（见 §五）。

## 二、P3C 表面格式提供的形式要素

P3C《阿里巴巴 Java 开发手册》的可复用之处只在表现形式；三种要素构成本调研的全部借鉴范围：

1. **三级标定**：每条规则标注【强制】/【推荐】/【参考】——读者直接判断规则强度，规则表可按级别检索。
2. **❌/✅ 反例正例对照**：规则配对反例与正例，边界靠对比表达，不靠形容词描述。
3. **表格列统一化**：同一主题的规则用同一套表格列承载，跨文档列结构一致、可预测。

规则内容、章节组织与配套工具链不在借鉴范围。

## 三、四个点的对照事实

### 3.1 DP 设计规范

承载：`docs/dp-conventions.md` ＋ `docs/conventions/dp-json-conventions.md` / `dp-validation-conventions.md` /
`dp-test-conventions.md`。

调研时已优于 P3C 表层、不必被替换的机制：ADR 锚点（`见 ADR-0028`）、Locked 标记（toString 等关键约束的版本锁）、
测试分组注册表（跨类不等式与敏感值契约测试自动覆盖新增 DP）、校验职责分层与工具 API 镜像表、
形态差异矩阵附设计理由。

差距在级别标定与 ❌/✅ 覆盖的一致性——按 §一「借鉴」边界由各规范篇承接。

### 3.2 Entity / Aggregate 设计规范

承载：`docs/conventions/framework-type-contracts.md` §4（Entity / Aggregate / Identifiable 条目）、
`docs/conventions/framework-crosscutting.md`（应用编排与终态守卫）、`docs/dp-conventions.md`（等值与继承形态矩阵）；
`AGENTS.md` 的任务路由表是入口指针。

调研时已优于 P3C 表层的机制：跨切面的具名反例（AppService 反例）、ADR 单源引用链（ADR-0017 / ADR-0023 / ADR-0024）、
密封继承 equals/hashCode 矩阵。

### 3.3 AIP API 设计规范

承载：`docs/research/google-aip-api-design-spec.md`（Research）、`docs/conventions/adapter.md`、
`docs/conventions/mapstruct.md`（MapStruct 定案）；相关决策 ADR-0009 / ADR-0012 / ADR-0013 / ADR-0021。

已优于 P3C 表层的机制：规则表三列固定 ＋ RFC 2119 级别词、SODA 适配差异段、兼容性对照表——Research 结构本身已优于
P3C 形式，这是「Research 不升格、表面格式只落 adapter」这一采用边界的依据。

### 3.4 Google 注释规范（`STYLEGUIDE.md` §4）

承载：`STYLEGUIDE.md` §4 单源；调研材料 `docs/research/google-comment-conventions.md`；
路由：`docs/doc-conventions.md` §1.1（写随代码提交的注释 → `STYLEGUIDE.md` §4）。

已优于 P3C 表层的机制：§4.2 的 ❌/✅ 代码示例、§4.4 的写/不写对比表与「见 ADR-NNNN」锚点约定、
§4.3 的 `TODO: <资源链接> - <说明>` 双语格式（前缀英文、说明中文）。本调研据此判定该点无需按 P3C 改造。

## 四、不需要改造的部分

借鉴 P3C 不是「全替换」，是「补缺口」。以下 **保留不动**：

| 部分                                            | 不动理由                                                                                 |
|-------------------------------------------------|------------------------------------------------------------------------------------------|
| AIP Research doc 规则表                         | 三列固定 ＋ RFC 2119 ＋ SODA 适配差异段 ＋ 兼容性对照表，已优于 P3C                       |
| DP 的 toString Locked 标记                      | 锁死式约束，胜于 P3C 文字描述                                                            |
| DP 测试分组注册表                               | 跨类不等式 ＋ 敏感值契约测试兜底，自动覆盖新增 DP                                        |
| DP 校验职责分层与 API 镜像表                    | ParseUtils / ValidateUtils / DP 构造器职责分层 ＋ 工具 API 镜像                          |
| 跨切面「典型反例」模式                          | 具名反例强于 P3C 抽象描述                                                                |
| ADR 单源引用链                                  | 每条规则可溯源至 ADR 编号                                                                |
| 密封继承 equals/hashCode 矩阵                   | 子类漏标风险显式化                                                                       |
| `STYLEGUIDE.md` §4.2 ❌/✅ 代码示例              | 已胜于 P3C 文字描述                                                                       |
| `STYLEGUIDE.md` §4.4 写/不写对比表              | 反例体成组（不变量 / 规则为什么 / 知识锚点 / 签名外契约 / 反直觉语义 / 不写决策历史）     |
| `STYLEGUIDE.md` §4.4 「见 ADR-NNNN」锚点         | P3C 无此机制                                                                              |
| `STYLEGUIDE.md` §4.3 TODO 双语格式              | 工具可识别 ＋ 中文友好                                                                    |

## 五、明确不采用的项

- **不引入构建门禁**：本项目不引入 checkstyle / Sonar / markdownlint / CI 等自动门禁；机械可查项由 agent 审计逐项承载——
  `STYLEGUIDE.md` §4.7 code-review 清单 ＋ `docs/doc-conventions.md` §8.3（不新增构建门禁）与 §8.4（机械项由 agent 审计承载）。
- **不把 P3C 当外部权威**：三级标定等形式的权威登记在 `docs/doc-conventions.md` §4.3，规则正文在 `STYLEGUIDE.md` §4 与各规范篇。
- **不全套手册化**：单源原则不主张「手册化」——不新建第二份规范文档，只在现有规范内做条形化。

## 六、边界

- **OKF v0.2 边界**：本调研不动章节结构、不动 ADR 极简格式（`docs/doc-conventions.md` §5.3：正文 1–3 句）、
  不把 Research 升格为规范；符合 V1 阶段「决策变更就地更新」原则（同篇 §9.1）。
- **agent 阅读稳定性**：条形化后 `规则 | 级别 | 来源` 表格结构稳定可检索——description 决定何时触达，正文结构稳定便于解析。

## 七、关联文档

- 决策：`docs/adr/0036-p3c-surface-format-retrofit.md`
- 调研输入：`docs/research/google-aip-api-design-spec.md`、`docs/research/google-comment-conventions.md`
- 调研结论引用：`docs/research/ut-testing-standards.md`、`docs/research/okf-code-repo-adaptation.md`
- 规范承载：`STYLEGUIDE.md`、`docs/dp-conventions.md`、`docs/conventions/framework-type-contracts.md`、
  `docs/conventions/adapter.md`

## 八、引用清单

- Alibaba P3C — https://github.com/alibaba/p3c（表现形式：三级标定 + ❌/✅ + 表格列；规则内容 **不在调研范围**）
- Google API Improvement Proposals — https://google.aip.dev（§3 type=Research 文档承载）
- Google Java Style Guide（当前 §7 Javadoc；implementation comments 与 TODO 在 §4.8.6）— https://google.github.io/styleguide/javaguide.html（注释规范源头）
- SODA 内文引用：`STYLEGUIDE.md` §4、`docs/dp-conventions.md` §1、`docs/conventions/framework-type-contracts.md` §4、
  `docs/doc-conventions.md` §1.1/§3.1/§5/§9
