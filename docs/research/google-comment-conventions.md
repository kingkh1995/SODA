---
type: Research
title: Java 注释来源研究：Google Java Style · JLS · Javadoc
description: 锚点：核对 Java 注释规则的来源章节归属、钉住版本或来源漂移时读——当前 §7 Javadoc 与 §4.8.6 的适用边界事实；SODA 注释规则单源在 STYLEGUIDE §4。
tags: [google-style, javadoc, comments]
status: stable
generated:
  by: wayfinder/02
  at: 2026-08-14T00:00:00Z
verified:
  - by: human:mm
    at: 2026-08-14T00:00:00Z
  - by: human:mm
    at: 2026-09-28T00:00:00Z
stale_after: 2026-12-27T00:00:00Z
sources:
  - resource: https://google.github.io/styleguide/javaguide.html
    id: google-java-style
    author: Google
    title: Google Java Style Guide（javaguide.html；钉住 google/styleguide@e4d272e）
  - resource: https://docs.oracle.com/en/java/javase/27/docs/specs/jls/jls-3.html#jls-3.7
    id: jls-3-7
    author: Oracle
    title: Java SE 27 Language Specification §3.7 Comments
  - resource: https://docs.oracle.com/en/java/javase/27/docs/specs/javadoc/doc-comment-spec.html
    id: javadoc-spec-jdk27
    author: Oracle
    title: JDK 27 Documentation Comment Specification（Standard Doclet）
---
# Java 注释来源研究：Google Java Style · JLS · Javadoc（wayfinder 02）

> 事实层：记录来源的章节归属、钉住版本、适用边界与比对结果。项目规则（写什么、怎么写）单源在根文档 [STYLEGUIDE.md](../../STYLEGUIDE.md) §4，本篇不复制其规则正文，也不承载项目建议。

## 1. 来源与钉住版本

| 来源 | 官方地址 | 版本 / 追踪点 | 规范强度 |
|---|---|---|---|
| Google Java Style Guide | [javaguide.html](https://google.github.io/styleguide/javaguide.html)（`google/styleguide` 仓 `gh-pages` 分支同名源文件） | 发布页无版本号；钉住 commit [`e4d272e`](https://github.com/google/styleguide/commit/e4d272eb151adfb5f382501265172a99c6a19671)（2026-09-14） | 自称 Google Java 标准的 complete definition；**示例代码 non-normative**，示例不是唯一合法写法 |
| Java Language Specification | [§3.7 Comments](https://docs.oracle.com/en/java/javase/27/docs/specs/jls/jls-3.html#jls-3.7) | Java SE 27（JSR 402，2026-09 发布） | 语言词法规范 |
| Javadoc（Standard Doclet） | [Documentation Comment Specification](https://docs.oracle.com/en/java/javase/27/docs/specs/javadoc/doc-comment-spec.html) | JDK 27 | doclet 识别与语法规范；不是内容质量政策 |

## 2. 章节归属（来源事实）

- 当前 Google 的 **§7 标题是 "7 Javadoc"**，不是 "Comments"。层级为 §7.1 Formatting（§7.1.1 general form、§7.1.2 paragraphs、§7.1.3 block tags）、§7.2 summary fragment、§7.3 where Javadoc is used（§7.3.1 self-explanatory 例外、§7.3.2 overrides 例外、§7.3.4 non-required）。
- **implementation comments 与 TODO 在 §4.8.6**（§4.8.6.1 block comment style、§4.8.6.2 TODO comments），不存在 "§7.4 Special comments"；§4.8.6 只处理 implementation comments，Javadoc 另按 §7。
- §7.1.1 同时接受 Traditional `/** … */` 与 Markdown `///` Javadoc；段落与续行规则按方言不同——Traditional 非首段前置 `<p>`、block tag 续行相对 `@` ≥ 4 格，Markdown 用空行且不加 `<p>`、续行恰 2 格（§7.1.2、§7.1.3）。
- §7.2 的 summary fragment 是 **noun phrase or verb phrase，不是完整句**，但按完整句做大小写与标点；官方反例是把 `@return the customer ID` 当作摘要。
- §7.3 的最低范围是每个 **visible class / member / record component**，按可见性定义；两个例外都带限定——self-explanatory 要求确实没有值得说的内容，overrides 只说明覆盖方法"不总是"有 Javadoc。
- JLS §3.7 定义 `/* … */` 与 `//` 两种注释且**不嵌套**；不规定必写范围、摘要措辞、TODO 格式或内容质量。
- Standard doclet 决定 documentation comment 的紧邻识别、main description 与 block tag 语法；**block tag 排序不是 doclet 规定**，是 Google 的项目风格规则。

## 3. 适用边界

| 表面 | Google / JLS / Javadoc 能否规范 |
|---|---|
| Java `/** … */` 与 `///` Javadoc | 能：§7 全部；JLS §3.7 词法、JDK 27 doclet 语法补足识别与结构 |
| Java `//`、`/* … */` 实现注释 | 部分能：§4.8.6（含 TODO）；JLS §3.7 词法 |
| Markdown、YAML、`.properties`、SQL/Flyway、Gradle/Groovy | **不能**：三份来源都不定义非 Java 文件的注释标记与解析行为 |

非 Java 载体的一手格式来源与不可类推项见 [non-java-comment-authorities.md](non-java-comment-authorities.md)；项目规则按载体分见 [STYLEGUIDE.md](../../STYLEGUIDE.md) §4.2–§4.5。任何把 §7 的摘要、block tag 顺序或 TODO 格式扩展到非 Java 文件的说法都不得归因 Google。

## 4. 来源比对记录

| 日期 | 比对 | 结果 |
|---|---|---|
| 2026-09-28 | `git ls-remote https://github.com/google/styleguide HEAD` → `543fe20`，对钉住 `e4d272e` | 24 commits / 34 files；`git diff e4d272e..543fe20` **未触及**被引文本 `javaguide.html` ⇒ **L1**：只记录，pin 沿用，规范零改动 |
| 2026-10-17 | 终检比对：`git ls-remote` HEAD → `b2a398d17f5ea890c6965b50e6c0c27a8c178f72`，≠ 钉住 `e4d272e`；compare（`e4d272e…HEAD`）64 commits / 55 files | `javaguide.html` **未命中**（仅 `javaguide.css` 与一张 png 变动，非被引规范文本）⇒ **L1**：只记录，pin 沿用，规范零改动 |
| 2026-10-17 | 终检比对（非 git 路径，同批两 Oracle 源）：JLS [§3.7](https://docs.oracle.com/en/java/javase/27/docs/specs/jls/jls-3.html#jls-3.7) 页、JDK 27 [doc-comment-spec](https://docs.oracle.com/en/java/javase/27/docs/specs/javadoc/doc-comment-spec.html) 页 | 两页仍声明 Java SE 27 / JDK 27，被引事实原样（§3.7 两种注释与不嵌套；doclet 识别与两方言语法）⇒ 已比对、影响面：无 |

`stale_after` = 2026-12-27（本次确认日 ＋ 90 天），到期即检；比对触发、分级判据与维护 owner 见 [doc-conventions.md](../doc-conventions.md) §9.2。

## 5. 与规范的关系

规则正文单源在 [STYLEGUIDE.md](../../STYLEGUIDE.md) §4（§4.1 来源登记与适用面矩阵、§4.2 Javadoc、§4.3 实现注释与 TODO、§4.4 中文内容政策、§4.5 非 Java profiles、§4.6/§4.7 写·检视双清单）。本篇只留来源事实与指针。
