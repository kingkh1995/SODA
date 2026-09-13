---
type: Research
title: P3C 表面格式借鉴调研（DP/Entity/AIP/注释规范）
description: 锚点：评估在 DP 设计规范、Entity/Aggregate 设计规范、Google AIP API 设计规范、Google 注释规范四个点借鉴 P3C《阿里巴巴 Java 开发手册》的【强制/推荐/参考】三级标定 + ❌/✅ 反例正例对照 + 表格列统一化的改造价值与成本。整改仅限表面格式，不动章节结构与规则内容。
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
    title: Google Java Style Guide §7 Comments
generated:
  by: sub-agent/scout×4
  at: 2026-09-02T00:00:00Z
verified: [ ]
---

# P3C 表面格式借鉴调研

> 边界：仅调研「表现形式」（【强制/推荐/参考】三级 + ❌/✅ 反例正例对照 + 表格列统一化），不调研 P3C
> 规则内容本身。整改仅限表面格式——不动章节结构、不动规则语义、不重组文档、不引入工具链（checkstyle/Sonar）。
> 调研方法：四个 scout sub-agent 并行调研 dp-conventions/framework-type-contracts/google-aip-api-design-spec/STYLEGUIDE
> §4 四组文档。

## 一、调研结论先行

**值得局部借鉴，不值得全套整改。** 四个点改造收益/成本不均：

| 范围                         | 现有 P3C 化程度                      | 改造估算 | 推荐                                                     |
|------------------------------|--------------------------------------|----------|----------------------------------------------------------|
| DP 设计规范（4 文档）        | 16-70%（RFC 2119）/ 0-33%（❌/✅）   | ~15h     | **做**                                                   |
| Entity/Aggregate（散落）     | 最低（中文 必须/禁止 + ❌/✅ ~30%）  | ~6h      | **做**（四个点中最高 ROI）                               |
| AIP（research + adapter）    | Research 已 100% P3C 化 / adapter 0% | ~12-16h  | **半做**（adapter 条形化 + research 升格代价高，做减法） |
| Google 注释（STYLEGUIDE §4） | 已 90% P3C 化                        | ~1h      | **不做**（补少量三级标定即可）                           |

合计 ~34-38h，与用户预期中型 1-2 周一致。落地后预计四条核心规范有明确的强制/推荐强度 grep 轴，code-review 与 agent
阅读都有稳定抓手。

## 二、四个点现状数据

### 2.1 DP 设计规范（4 文档）

承载位置：

- `docs/dp-conventions.md`（492 行，主规范）
- `docs/conventions/dp-json-conventions.md`（~200 行）
- `docs/conventions/dp-validation-conventions.md`（~100 行）
- `docs/conventions/dp-test-conventions.md`（320 行）

RFC 2119 级别覆盖率（scout 测）：16-70%，不均衡——`dp-validation-conventions.md` 与 `dp-test-conventions.md` 已 67-70%，
`dp-json-conventions.md` 仅 16%，`dp-conventions.md` 58%。

❌/✅ 覆盖率：0-33%——`dp-conventions.md` 仅 9%（§2.4 形态对照 + §4 测试分组），其他多文档零覆盖。

表格列样式：22 张表格，三套列不统一。dp-conventions §1.1 用「实现形态/策略/说明」3 列；§1.2 用「实现形态/规则/示例」3 列；§2.4.3
用「维度/class 形态/record 形态」3 列。dp-validation §2 用「场景/唯一入口/禁止替代」3 列。

**已优于 P3C 表层处**（不必改造）：

1. ADR 锚点（`见 ADR-0028`）——单源原则的代码侧索引，P3C 无此机制。
2. Locked 标记（如 `dp-conventions.md` L36、L90）——关键约束的版本锁，优于 P3C 文字描述。
3. 测试分组注册表（`dp-test-conventions.md §1.6`）——`CrossTypeEqualityTest` + `SensitiveValueContractTest` 自动覆盖新增
   DP，P3C 无此机制。
4. 校验单一权威镜像表（`dp-validation-conventions.md §2`）——DP 构造器/ParseUtils/ValidateUtils 职责分层 + 工具 API 镜像。
5. 完整代码模板 + 设计理由表（`dp-conventions.md §2.4.3`）——形态差异矩阵附理由。

### 2.2 Entity / Aggregate 设计规范

承载位置（scout 报告）：

- 主：`docs/conventions/framework-type-contracts.md` L24-44（Entity/Aggregate/Identifiable 条目）
- 散落：`docs/conventions/framework-crosscutting.md` L25-200（DomainService/AppService 编排，含终态守卫/异常约定）
- 关联：`docs/dp-conventions.md`（双 Builder / sealed 继承 / 等值矩阵）
- 指针：`AGENTS.md`（触发场景→文档表）

RFC 2119 英文级别词：零覆盖（仅中文「必须/禁止/不建议」），与 DP 的 16-70% 不一致。

❌/✅ 覆盖率：~30%（仅 DP 表格附带，Entity/Aggregate 正文无）。

表格列样式：framework-type-contracts.md Entity/Aggregate 条目 **无表格**——纯叙述体。crosscutting.md
编排两套表格列。dp-conventions.md 三套列。

**已优于 P3C 表层处**：

1. 跨切面"典型反例"模式（`framework-crosscutting.md L52` AppService 反例）——`verification.verify(code)` +
   `user.changeMobile(...)`，具名反例强于 P3C 抽象描述。
2. ADR 单源引用链（`ADR-0017` 聚合生命周期 + `ADR-0023` 注销终态键释放 + `ADR-0024` save 路由）——每条规则可追溯。
3. 模板附理由表（DP §2.4.3）。
4. 密封继承 equals/hashCode 矩阵（DP §1.1 第四行）——子类漏标风险显式化。

### 2.3 AIP API 设计规范

承载位置（scout 报告）：

- 主：`docs/research/google-aip-api-design-spec.md`（828 行， **Research 类型**）
- 入口：`docs/conventions/adapter.md`（MapStruct 协议转换）
- 决策：ADR-0012（禁 PUT / 冒号自定义方法）/ ADR-0013（ErrorInfo）/ ADR-0009（Request+Response+WebAssembler）/
  ADR-0021（Verification 拓扑）
- 注解：STYLEGUIDE.md §3.4（MapStruct 四条定案）

RFC 2119 级别覆盖：Research doc **100%**（110/110 条规则带 MUST/SHOULD/MAY），adapter.md **0%**。

❌/✅ 覆盖：Research doc 仅 1 处（L119 嵌套集合省略前缀），adapter.md 0。

表格列样式：Research doc 规则表统一 `规则 | 级别 | AIP`（100% 一致）；adapter.md 单表 `方向 | 转换 | 语义`（无级别列）。

**已优于 P3C 表层处**：

1. 规则表三列固定 + RFC 2119 英文——每条规则可溯源至 AIP 编号。
2. SODA 适配差异段（L242、L257、L292、L524）——明确项目偏离 AIP 的理由。
3. 兼容性对照表（L737-745）——SODA 实现 | AIP 来源 | 状态三列逐项核对。
4. 自动化检查清单（L772-776）——可 grep 的勾选项，可接入 CI。
5. Research 文档结构（§3 type=Research，sources/verified 元信息完整）。

### 2.4 Google 注释规范（STYLEGUIDE §4）

承载位置：

- 主：`STYLEGUIDE.md` §4（L129-191）——单源约定
- 调研：`docs/research/google-comment-conventions.md`（140 行）
- 指针：`docs/doc-conventions.md` §6

RFC 2119 显式级别标定：§4 **0%**（scout 测：实际用「必写/禁止/不强制」隐含 12 条规则，未标三级）。

❌/✅ 覆盖：§4 仅 1 组（L149-152 `@return` 引用式对比）。

表格列：§4 写/不写对比表 2 列（写/不写），列一致但未统一（含级别列）。

**已优于 P3C 表层处**：

1. §4.2 ❌/✅ 代码示例（L149-152）——胜于 P3C 文字描述。
2. §4.5 写/不写对比表（L170-177）——6 行反例体（不变量/规则为什么/CONTEXT 锚点/签名外契约/反直觉语义/不写决策历史）。
3. §4.5 「见 ADR-NNNN」锚点约定——P3C 无此机制。
4. §4.7 TODO 格式约束（`TODO: <链接> - <说明串>`，前缀英文 + 说明中文）——双语兼容。

## 三、共性结论

1. **改造收益集中在 DP + Entity/Aggregate**：这两个点是当前四级标定最不均衡、❌/✅ 覆盖率最低的，AIP 与注释规范已经接近 P3C
   表层。
2. **AIP Research 升格代价不匹配收益**：scout 估算 12-16h 主要花在从 Research 抽取 60 对 ❌/✅ + 新建
   conventions/aip-api-conventions.md。考虑到 SODA V1 阶段「research 不当 spec 用」原则（doc-conventions §3.1 type 词表研究用
   OKF §5.1 provenance 而非 §4.1 决策），且 Research doc 本身结构已优， **建议保留 research/google-aip-api-design-spec.md
   不动**，仅对 adapter.md 加级别列与 ❌/✅。
3. **Entity/Aggregate 是 ROI 最高的改造点**：当前纯叙述体、无表格、无级别列；改造后可达 P3C 化基线。
4. **注释规范是 ROI 最低的改造点**：§4 已 90% P3C 化，剩 1h 即可收口。
5. **不需要的工具链**：checkstyle/Sonar 适合「机械且高收益」规则（块标签顺序、装饰性星号边框等）。STYLEGUIDE §4 明确「机械格式可加
   checkstyle，漂移真实出现时再评估启用」（现行无需）。本次调研不引入工具链。

## 四、改造路径与成本（不含工具链）

| 序号 | 改造                        | 内容                                                                       | 估算 | 优先级           |
|------|-----------------------------|----------------------------------------------------------------------------|------|------------------|
| 1    | DP 全文档加【级别】列       | 22 张表格统一为 `规则                                                      | 级别 | 来源`            | 6h | P0 |
| 2    | DP 必测规则补 ❌/✅         | 约 60 对（按 `dp-conventions §1` Quick-Start Checklist 优先）              | 6h   | P0               |
| 3    | Entity/Aggregate 表化       | `framework-type-contracts.md` L24-44 散落规则制表                          | 2h   | P0               |
| 4    | Entity/Aggregate 补 ❌/✅   | 跨聚合编排/终态守卫等典型反例                                              | 3h   | P0               |
| 5    | Entity/Aggregate 指针单源   | 散落规则指针化到 `framework-type-contracts.md`（如终态守卫 ADR-0017/0023） | 1h   | P1               |
| 6    | adapter.md 加级别列 + ❌/✅ | 单表改 `规则                                                               | 级别 | 来源` + 补 ~5 对 | 2h | P1 |
| 7    | AIP Research 不动           | 仅在 AGENTS.md 加指针指向 `research/google-aip-api-design-spec.md`         | 0.5h | P2               |
| 8    | STYLEGUIDE §4 加三级标定    | §4.1/§4.3/§4.5 隐含级别显式化                                              | 1h   | P2               |

合计 ~21.5h（不含 AIP 升格 12h）。如采纳 AIP 升格（scout 建议的完整路径），合计 ~34h。

## 五、不需要改造的部分

借鉴 P3C 不是「全替换」，是「补缺口」。以下 **保留不动**：

| 部分                              | 不动理由                                                                                  |
|-----------------------------------|-------------------------------------------------------------------------------------------|
| AIP Research doc 规则表           | 已 100% 优于 P3C（三列固定 + RFC 2119 + SODA 适配差异段 + 兼容性对照表 + 自动化检查清单） |
| DP §1.2 toString 规范 Locked 标记 | 锁死式约束，胜于 P3C 描述                                                                 |
| DP 测试分组注册表                 | 跨类不等式 + 敏感值契约测试兜底，自动覆盖新增 DP                                          |
| DP 校验单一权威镜像表             | ParseUtils/ValidateUtils/DP 构造器职责分层 + API 镜像                                     |
| 跨切面"典型反例"模式              | 具名反例强于 P3C 抽象                                                                     |
| ADR 单源引用链                    | 每条规则可溯源至 ADR 编号                                                                 |
| 密封继承 equals/hashCode 矩阵     | 子类漏标风险显式化                                                                        |
| 注释 §4.2 ❌/✅ 代码示例          | 已胜于 P3C                                                                                |
| 注释 §4.5 写/不写对比表           | 6 行反例体                                                                                |
| 注释 §4.5 「见 ADR-NNNN」锚点     | P3C 无此机制                                                                              |
| 注释 §4.7 TODO 双语格式           | 工具可识别 + 中文友好                                                                     |

## 六、决策路径

| 路径                       | 内容                                                 | 工作量 | 推荐                                     |
|----------------------------|------------------------------------------------------|--------|------------------------------------------|
| A. 不改造                  | 现状已接近 P3C 表层，且 OKF 单源原则不主张「手册化」 | 0h     | 不推荐（DP + Entity/Aggregate 仍有缺口） |
| B. 局部改造（推荐）        | §四序号 1-6 + 8（不含 AIP 升格）                     | ~21.5h | **推荐**                                 |
| C. 全套改造（含 AIP 升格） | §四序号 1-8 + AIP 升格 + 升格 ADR-0036               | ~34h   | 视项目意愿                               |

## 七、风险与边界

- **OKF v0.2 边界**：本调研不动章节结构、不动 ADR 极简格式（doc-conventions §4 极简三句）、不升格 Research 为 spec；符合 V1
  阶段「决策变更就地更新」原则（§9）。
- **单源原则**：所有改造路径不引入第二份规范文档（如不新建 `docs/conventions/dp-design-conventions.md`），仅条形化现有文档。
- **`.scratch` 不入库**：本调研以正式 docs/research/ 落地（已调研完成），符合 issue-tracker 工作流（
  `docs/agents/issue-tracker.md`）。
- **agent 阅读稳定性**：条形化后 agent 可按 `规则 | 级别 | 来源` 表格结构稳定检索，与 writing-for-agents 原则一致（description
  决定何时触达，正文结构稳定便于解析）。

## 八、关联文档

- 决策：`docs/adr/0036-p3c-surface-format-retrofit.md`
- 调研输入：`docs/research/google-aip-api-design-spec.md`、`docs/research/google-comment-conventions.md`
- 调研结论引用：`docs/research/ut-testing-standards.md`、`docs/research/okf-code-repo-adaptation.md`
- 规范承载：`STYLEGUIDE.md`、`docs/dp-conventions.md`、`docs/conventions/framework-type-contracts.md`、
  `docs/conventions/adapter.md`

## 九、引用清单

- Alibaba P3C — https://github.com/alibaba/p3c（表现形式：三级标定 + ❌/✅ + 表格列；规则内容 **不在调研范围**）
- Google API Improvement Proposals — https://google.aip.dev（§3 type=Research 文档承载）
- Google Java Style Guide §7 Comments — https://google.github.io/styleguide/javaguide.html（注释规范源头）
- SODA 内文引用：`STYLEGUIDE.md §4`、`docs/dp-conventions.md §1`、`docs/conventions/framework-type-contracts.md §4`、
  `docs/doc-conventions.md §4/§6/§9`
