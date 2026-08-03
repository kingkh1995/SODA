# Issue Tracker：本地 Markdown

本仓库的 Issue 与规格（PRD）以 Markdown 文件形式存放在 `.scratch/`。

## 约定

- 每个功能一个目录：`.scratch/<feature-slug>/`
- 规格文件：`.scratch/<feature-slug>/spec.md`
- 实现 Issue 每个 ticket 一个文件：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`，从 `01` 开始编号——绝不合并成单个文件
- Triage 状态记录在 Issue 文件顶部附近的 `Status:` 行（角色字符串见 `triage-labels.md`）
- 评论与对话历史追加在文件底部 `## Comments` 标题下

## 当技能说"发布到 issue tracker"时

在 `.scratch/<feature-slug>/` 下新建文件（必要时创建目录）。

## 当技能说"获取相关 ticket"时

读取引用路径对应的文件。用户通常会直接传入路径或 issue 编号。

## Wayfinding 操作

由 `/wayfinder` 使用。**map** 是一个文件，每个 ticket 对应一个**子**文件。

- **Map**：`.scratch/<effort>/map.md` — Notes / Decisions-so-far / Fog 正文
- **子 ticket**：`.scratch/<effort>/issues/NN-<slug>.md`，从 `01` 编号，问题写在正文中。`Type:` 行记录 ticket 类型（`research`/`prototype`/`grilling`/`task`）；`Status:` 行记录 `claimed`/`resolved`
- **阻塞**：文件顶部附近的 `Blocked by: NN, NN` 行。当列出的所有文件都 `resolved` 时 ticket 解除阻塞
- **Frontier**：扫描 `.scratch/<effort>/issues/` 中开放、未阻塞、未认领的文件；编号小的优先
- **认领**：开始工作前设置 `Status: claimed` 并保存
- **解决**：在 `## Answer` 标题下追加答案，设置 `Status: resolved`，然后在 `map.md` 的 Decisions-so-far 中追加上下文指针（要点 + 链接）