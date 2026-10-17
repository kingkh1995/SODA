---
type: Research
title: 非 Java 注释载体的一手来源研究（SQL/Flyway · YAML · .properties · Gradle/Groovy · Markdown · lombok.config · .gitignore）
description: 锚点：新增非 Java 注释载体、核对 FORMAT-* 来源的版本与不可类推项、或复核其来源漂移时读——七个载体的一手来源事实、规范强度与来源边界；项目规则单源在 STYLEGUIDE §4.5。
tags: [comments, format-sources, non-java]
status: stable
generated:
  by: wayfinder/15
  at: 2026-09-25T00:00:00Z
stale_after: 2026-12-24T00:00:00Z
sources:
  - resource: https://www.postgresql.org/docs/18/sql-syntax-lexical.html#SQL-SYNTAX-COMMENTS
    id: FORMAT-SQL
    title: PostgreSQL 18.6 §4.1.5 Comments
    author: PostgreSQL Global Development Group
  - resource: https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-sql-migration-prefix-setting
    id: FORMAT-FLYWAY
    title: Flyway SQL migration prefix setting
    author: Redgate Flyway
  - resource: https://documentation.red-gate.com/flyway/reference/tutorials/tutorial-repeatable-migrations
    id: FORMAT-FLYWAY-REPEATABLE
    title: Flyway Repeatable Migrations
    author: Redgate Flyway
  - resource: https://yaml.org/spec/1.2.2/#3233-comments
    id: FORMAT-YAML
    title: YAML 1.2.2 §3.2.3.3 / §6.6 Comments
    author: YAML Team
  - resource: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Properties.html#load(java.io.Reader)
    id: FORMAT-PROPERTIES
    title: Java SE 25 java.util.Properties.load
    author: Oracle
  - resource: https://repo.maven.apache.org/maven2/org/apache/groovy/groovy/4.0.29/groovy-4.0.29-sources.jar
    id: FORMAT-GROOVY
    title: Apache Groovy 4.0.29 sources（GroovyLexer.g4）
    author: Apache Software Foundation
  - resource: https://docs.gradle.org/9.5.1/userguide/init_scripts.html
    id: FORMAT-GRADLE
    title: Gradle 9.5.1 Initialization Scripts
    author: Gradle
  - resource: https://spec.commonmark.org/0.31.2/
    id: FORMAT-MARKDOWN
    title: CommonMark 0.31.2 §6.6 HTML comments
    author: CommonMark
  - resource: https://projectlombok.org/features/configuration
    id: FORMAT-LOMBOK-CONFIG
    title: Lombok Configuration system
    author: Project Lombok
  - resource: https://git-scm.com/docs/gitignore
    id: FORMAT-GITIGNORE
    title: gitignore(5) PATTERN FORMAT
    author: Git Project
  - resource: https://github/github/gitignore
    id: FORMAT-GITIGNORE-TEMPLATE
    title: GitHub gitignore templates（Java.gitignore，示例非规范）
    author: GitHub
---
# 非 Java 注释载体的一手来源研究

> 事实层：来源事实、规范强度、核验记录与研究限制。项目规则（每条注释写什么、怎么写）单源在根文档 [STYLEGUIDE.md](../../STYLEGUIDE.md) §4.5 各 profile，本篇不复制其规则正文。

## 1. 来源层级与边界

| 表面 | 实际权威 | 来源能回答什么 | 不能由来源推出什么 |
|---|---|---|---|
| SQL migration | SQL 方言的词法规范；Flyway 的 migration / 命名 / 校验文档 | 注释在特定方言中如何被词法分析；Flyway 文件如何命名、何时重跑 | 方言差异不能合并成一个"SQL 注释标准"；Flyway 不定义注释内容质量、缩进或摘要句式 |
| YAML | YAML 1.2.2 规范 | `#` 的词法作用、空白分隔、注释是 presentation detail 且不承载数据 | 注释的价值、语气、是否解释配置意图 |
| `.properties` | `java.util.Properties` API | `#`/`!` 行注释、空白行、natural/logical line、字符编码 | 所有 properties reader 都兼容；措辞质量 |
| Gradle/Groovy | Groovy 语言词法参考；Gradle 官方脚本文档 | Groovy 注释标记；init/build script 是 Groovy/Kotlin 脚本 | Gradle 为构建脚本另造注释排版或内容政策；Kotlin DSL 规则 |
| Markdown | CommonMark 规范及其引用的 HTML 规范 | Markdown 没有独立 comment token；`<!-- … -->` 是 raw HTML 注释 | 该注释在所有渲染器中一致隐藏；文档的注释放置与内容政策 |
| `lombok.config` | Lombok 官方 Configuration 文档 | `#` 行注释的词法、`stopBubbling` 与 `flagUsage` 的合法值、自检命令形态 | 注释写什么、用什么语言、如何分节；退出码语义（文档未定义） |
| `.gitignore` | git `gitignore(5)`；GitHub 模板为示例 | `#` 注释与 `\#` 转义的词法 | 注释内容与风格；模板英文措辞的规范地位 |

Google Java Style 的 §7 与 §4.8.6、JLS §3.7、Javadoc doclet 规范都只治理 Java 源文件（来源事实见 [google-comment-conventions.md](google-comment-conventions.md)）。**非 Java 载体不得归因 Google**：把 Javadoc 摘要、block tag 顺序、TODO 格式或内容质量推到 YAML / properties / SQL / Groovy / Markdown 的任何写法，都必须标为项目政策。

## 2. 来源事实表

| 载体 / 问题 | 一手来源（版本 / 日期） | 规范强度 | 来源事实 | 不可类推项 |
|---|---|---|---|---|
| SQL 注释词法（示例方言） | [PostgreSQL 18.6 §4.1.5 Comments](https://www.postgresql.org/docs/18/sql-syntax-lexical.html#SQL-SYNTAX-COMMENTS)（页面标注 18.6；访问 2026-09-25） | PostgreSQL 语言参考的规范性词法说明，**不是**跨数据库 SQL 标准 | `--` 到行尾；`/* … */` 到匹配的 `*/`；注释从输入流移除并替代空白；该方言的 block comment **可嵌套**并声明遵循 SQL 标准 | 嵌套不是所有目标数据库的保证；不得规定成 Flyway 跨数据库要求 |
| Flyway 文件与重跑 | [SQL migration prefix](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-sql-migration-prefix-setting)、[Repeatable migrations](https://documentation.red-gate.com/flyway/reference/tutorials/tutorial-repeatable-migrations)（页面更新 2026-09-24） | 官方产品文档：配置与工作流，不是 SQL 词法标准 | versioned 文件为 `V<version>__<description>.sql`（前缀可配置）；repeatable 用 `R` 前缀、checksum 变化时重新应用 | 官方无跨数据库"解释性注释内容/缩进"标准；不得把 checksum 语义写成"注释一定参与/不参与 checksum"（来源未作此断言） |
| YAML | [YAML 1.2.2 §3.2.3.3](https://yaml.org/spec/1.2.2/#3233-comments)、[§6.6](https://yaml.org/spec/1.2.2/#66-comments)（修订 2021-10-01；访问 2026-09-25） | YAML 1.2.2 规范，正式语法部分规范性 | 注释以 `#` 标记，是 presentation detail，不影响 representation；不能置于 scalar 内；必须与其它 token 以空白分隔（行首独立注释除外） | `#` 在 quoted scalar 中是字符；来源不规定句号、逐 key 解释或 Javadoc 句式 |
| Java `.properties` | Oracle Java SE 25 [`Properties.load`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Properties.html#load(java.io.Reader))（页面元数据 JDK 25，2026-07-15；访问 2026-09-25） | JDK API 规范：`load(Reader)` / `load(InputStream)` 的格式行为 | 行导向；注释行首个非空白字符必须是 ASCII `#` 或 `!`；注释行不编码 key/value、不能反斜杠续行；`load(InputStream)` 使用 ISO-8859-1 | API 不规定内容质量或首选 `#`/`!`；其它 reader 可能有扩展，不能无条件推广到所有配置文件 |
| Groovy / Gradle | Apache Groovy [4.0.29 `GroovyLexer.g4`](https://repo.maven.apache.org/maven2/org/apache/groovy/groovy/4.0.29/groovy-4.0.29-sources.jar)（附 [POM](https://repo.maven.apache.org/maven2/org/apache/groovy/groovy/4.0.29/groovy-4.0.29.pom)）；[Gradle 9.5.1 Initialization Scripts](https://docs.gradle.org/9.5.1/userguide/init_scripts.html)（项目 wrapper 版本；访问 2026-09-25） | Groovy grammar 是语言拥有方的 lexer 源码；Gradle 页面是官方用户手册；两者都不是注释风格规范 | `SL_COMMENT` 消费到行尾；`ML_COMMENT` 自 `/*` 到首个 `*/`（含 GroovyDoc）；`SH_COMMENT` shebang 仅首行；Gradle init script 是 Groovy/Kotlin 脚本 | Gradle 无独立注释排版/内容政策；Groovy grammar 无解释性散文风格；Kotlin DSL 未登记来源，须另行核验 |
| Markdown | [CommonMark 0.31.2](https://spec.commonmark.org/0.31.2/)（版本日期 2024-01-28；访问 2026-09-25），raw HTML 规则引用 [HTML Standard](https://html.spec.whatwg.org/multipage/parsing.html#markup-declaration-open-state) | CommonMark 规范（规范 Markdown 解析） | 没有原生 comment token；§6.6 定义 HTML comment 为 `<!-- … -->` 的 raw HTML 并按 raw HTML 保留/渲染 | 来源不规定注释放哪、是否应该存在、措辞与语言；raw HTML 注释不是跨 renderer 一致隐藏的通用机制 |
| `lombok.config` 注释 | Lombok [Configuration system](https://projectlombok.org/features/configuration)（页面无版本声明，功能级版本句至 v1.18.48；访问 2026-09-29） | 官方功能文档，说明配置系统，**不是**注释风格规范 | `Overview`：以 `#` 起始的行是注释；`config.stopBubbling = true` 声明根目录；`lombok.*.flagUsage` 合法值 `warning`/`error`；`java -jar lombok.jar config` 自检命令形态 | 不规定注释内容、语言、长度、分节与 `#` 转义；退出码 rc=0/rc=2 文档零表述，仅有官方源码与实测支持 |
| `.gitignore` 注释 | git [`gitignore(5)`](https://git-scm.com/docs/gitignore)（页面标注 last updated in 2.55.0，2026-06-29；访问 2026-09-29）；[GitHub `github/gitignore`](https://github/github/gitignore) `Java.gitignore`（末次变更 2021-11-14，CC0） | git 官方 man page 为格式所有者；GitHub 模板自述为 "a collection of useful .gitignore templates"，属**示例推荐清单**而非规范，规范指回 `gitignore(5)` | 以 `#` 起始的行为注释；模式以 `#` 开头时用 `\#` 转义；模板仓未 archived、活跃维护 | 不规定注释内容、语言与分节；已核验的格式所有者唯一为 `gitignore(5)`，未做穷举性否定；模板英文措辞不构成规范 |

**事实与项目建议的分界**：上表只在"来源事实"列断言；内容质量类要求（解释非显然意图、不复述字面代码）在这些格式标准中没有可转移的规范性依据，只能作为项目政策引用——落点是 [STYLEGUIDE.md](../../STYLEGUIDE.md) §4.5 各 profile 的 `SODA-POLICY` 行，本篇不重复。

## 3. 来源核验记录

- 2026-09-25 实际抓取并核验：YAML 1.2.2（yaml.org）、PostgreSQL 18.6（postgresql.org）、Oracle Java SE 25 `Properties` API（docs.oracle.com）、CommonMark 0.31.2（spec.commonmark.org）、Redgate Flyway 两个页面（documentation.red-gate.com）、Apache Groovy 4.0.29 sources jar 与 POM（repo.maven.apache.org）、Gradle 9.5.1 init scripts（docs.gradle.org）。
- 2026-09-29 实际抓取并核验：Lombok Configuration system（projectlombok.org）、git `gitignore(5)`（git-scm.com，标注 2.55.0 / 2026-06-29）、GitHub `github/gitignore` 模板仓现状（未 archived、CC0、`Java.gitignore` 末次变更 2021-11-14），并与本仓 `.gitignore` 逐行比对：行 1–24 与 `Java.gitignore` 零差异（模板段），行 26–66 为项目专属段。
- 退出码证据层级：Lombok 官方文档未定义 `config` 子命令的退出码，rc=0（通过）/rc=2（未知键或坏值）/rc=1（命令行或路径错误）来自官方源码与实测，属非文档证据；落规则时标 `SODA-POLICY`，不得升格为来源保证。
- Groovy 官方 `docs.groovy.org` 页面当次抓取不稳定，词法事实改用同一官方项目发布到 Maven Central 的 4.0.29 `GroovyLexer.g4` 源码；网页不可达不等于无来源。
- `stale_after` = 2026-12-24（首次核验日 ＋ 90 天，**不因追加登记而顺延**，故 2026-09-29 新增两载体沿用该更早到期日）。非 git 来源按**页面声明的规范版本/日期**比对，`ETag`/`Last-Modified` 不作判据；触发与分级见 [doc-conventions.md](../doc-conventions.md) §9.2。
- 2026-10-17 全量终检比对（逐源核对页面声明版本/日期）：FORMAT-FLYWAY 两页（SQL migration prefix、Repeatable Migrations）页面日期 2026-09-24 → 2026-10-15，**被引事实未变**（versioned `V<version>__<description>.sql` 命名、repeatable `R` 前缀与 checksum 重跑语义原样）⇒ **L1**：已比对、影响面：无，登记值与规则零改动；其余登记来源页面声明版本/日期与登记值一致。

## 4. 研究限制（来源不回答的问题）

- Flyway 官方资料覆盖命名、配置与 repeatable checksum 工作流，**没有**跨数据库通用的"解释性注释风格"页面；SQL 注释必须在「目标方言 + Flyway」组合上验证。
- CommonMark 保证 raw HTML 注释的解析，不保证 GitHub、IDE、静态站点等具体 renderer 的隐藏行为——属工具链验证。
- Gradle 9.5.1 与 Groovy 4.0.29 都不提供注释的内容或排版政策；Kotlin DSL 需按其语言来源另行核验（当前未登记）。
- Lombok 官方文档不定义 `config` 子命令的退出码，`.gitignore` 的"不存在其它格式所有者"未做穷举核验——两条均按限定措辞登记，不作来源保证。
- 各来源页面会继续更新；版本已记录，但未发布固定 commit 的页面在未来迁移前须重新核验（到期即检）。
