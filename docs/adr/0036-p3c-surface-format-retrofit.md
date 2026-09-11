---
type: Decision Record
title: P3C 表面格式借鉴（DP/Entity/AIP/注释规范）
description: 决定是否在 DP 设计规范、Entity/Aggregate 设计规范、Google AIP API 设计规范、Google 注释规范四处借鉴 P3C 表面格式（【强制/推荐/参考】三级 + ❌/✅ 反例正例 + 表格列统一化）时读——调研结论与采纳路径。
tags: [format, p3c, dp, entity, aip, comment]
status: stable
---

# 0036 — P3C 表面格式借鉴

借鉴 P3C《阿里巴巴 Java 开发手册》的表面格式——【强制/推荐/参考】三级标定 + ❌/✅ 反例正例对照 +
表格列统一化——按局部改造路径落在四处（DP / Entity/Aggregate / AIP /
注释规范），不动章节结构、不动规则语义、不重组文档、不引入工具链（checkstyle/Sonar），总估算 ~21.5h。AIP Research 文档 **不升格**
为 spec（V1 阶段 OKF §3.1 type=Research 与项目 research 不当 spec 用原则保留）；现有优于 P3C 表层的部分（DP Locked 标记 /
测试分组注册表 / 校验单一权威镜像表 / 跨切面典型反例 / ADR 单源引用链 / AIP Research 规则表三列固定 / 注释 §4.2 ❌/✅
代码示例 + §4.5 写不写对比表 + §4.7 TODO 双语格式）保留作为单源。完整调研、改造路径、ROI
表与必要性论证单源在 [docs/research/p3c-surface-format-retrofit.md](../research/p3c-surface-format-retrofit.md)——本 ADR
只承载为什么这么定。

## Consequences

- 改造动四处文档共 ~21.5h（DP 22 表级别列 + 60 对 ❌/✅ ~15h；Entity/Aggregate 表化 + ❌/✅ ~6h；adapter.md 级别列 + ❌/✅ ~
  2h；STYLEGUIDE §4 三级标定 ~1h），按 §四序号 1-6 + 8 落地（调研报告 §四）。AIP Research doc 不动、ADR 不动、根目录
  STYLEGUIDE/CONTEXT/AGENTS/README 不动
- 改造 **不引入** checkstyle/Sonar/ArchUnit——STYLEGUIDE §4 明确「机械格式可加
  checkstyle，漂移真实出现时再评估启用」（现行无需）；doc-conventions §7 合规基准明确「机械项脚本检查不做，由 agent 审计按基准逐项承载」
- 条形化后 agent / code-review 按 `规则 | 级别 | 来源` 三列稳定检索，与 writing-for-agents 原则一致（description
  决定何时触达、正文结构稳定便于解析）；不破坏 OKF 单源原则（不引入第二份规范文档）
- 不替代现有 ADR 极简三句格式（doc-conventions §4）——本 ADR 自身即是极简三句（背景 + 决策 + 为什么）；不替代 doc-conventions
  §9 V1 阶段「决策变更就地更新」原则
- 不改 docs/research/google-aip-api-design-spec.md 状态（仍为 Research，§3 type 词表）——research 文档的 provenance
  与「单源决策 + 单源规则」分离，符合 OKF §5.1；SODA 适配差异段（L242/L257/L292/L524）已优于 P3C 表层
- 改造完成后 **不**立即开启下一轮改造——P3C 化是终态，引入第二轮会引发「形式反复」（V1 阶段反漂移）
