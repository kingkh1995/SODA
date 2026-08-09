# 领域文档

工程技能在探索代码库时应如何消费本仓库的领域文档。

## 探索前先读

- 根目录 **`CONTEXT.md`**，或
- 根目录若存在 **`CONTEXT-MAP.md`** — 指向每个上下文各自的 `CONTEXT.md`。阅读与当前主题相关的每一个
- **`docs/adr/`** — 阅读与你即将工作的区域相关的 ADR。多上下文仓库还需检查 `src/<context>/docs/adr/` 中的上下文级决策

如果这些文件不存在，**静默继续**。不要标记缺失，也不要建议预先创建。`/domain-modeling` 技能（经 `/grill-with-docs` 与 `/improve-codebase-architecture` 触达）会在术语或决策真正落地时惰性创建它们。

## 文件结构

单上下文仓库（大多数仓库）：

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `CONTEXT-MAP.md`）：

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← 上下文级决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## 使用术语表的词汇

当输出命名领域概念时（issue 标题、重构提案、假设、测试名），使用 `CONTEXT.md` 中定义的术语。不要漂移到术语表明确回避的同义词。

如果所需概念尚未收录，这是一个信号——要么你在发明项目未使用的语言（重新考虑），要么存在真实缺口（记下来交给 `/domain-modeling`）。

## 标记 ADR 冲突

如果输出与现有 ADR 矛盾，显式标出而不是静默覆盖：

> _与 ADR-0007（event-sourced orders）矛盾——但值得重新审视，因为…_

## 维护 ADR 修订

- 修订段只记录**已落库的设计**（已提交或随同一补丁落库）；未落库的中间设计不写入修订段
- 设计被后续 ADR 取代时，在原修订段加「已被 ADR-00XX 取代」标记并保留原文（沿革），不静默改写历史内容
- 修订段按时间序追加（新段放在既有段之后），不插入中间——避免时间序错乱