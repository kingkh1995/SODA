---
type: Convention
title: Soda 编码规范
description: 写 Java / 加注解 / 写注释 / 改依赖前读——编码与注释的仓库单一标准
tags: [convention, styleguide, code]
status: stable
---

# 编码规范

> 编码风格与注释的仓库单源；框架层有什么归 `docs/framework-conventions.md`（类型契约见
> conventions/framework-type-contracts.md，跨切面见 conventions/framework-crosscutting.md）。测试规范见
> `docs/test-conventions.md`；DP 必测分组见 `docs/conventions/dp-test-conventions.md`。

## 1. 语言与术语

- 代码注释与文档用中文（项目裁定）；术语以 `CONTEXT.md` 为 **单源**——保留英文原词、对齐其词表，本文件不重定义
- 异常消息用英文

## 2. 代码风格

### 2.1 JDK 特性优先

优先使用 JDK 最新特性，不重复造轮子：

| 场景                    | 优先使用                                                                                                                                              | 禁止                                                         |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| 不可变值对象（DP、DTO） | `record`（JDK 16+）                                                                                                                                   | 手写 class + `@EqualsAndHashCode` + `toString()`             |
| 封闭集合建模            | `sealed` + `permits`，switch 分派依赖编译期穷举                                                                                                       | sealed 层次的 switch 写 `default` 兜底；把开放继承当封闭集用 |
| 空值校验 + 值归一化     | record 紧凑构造                                                                                                                                       | 在工厂方法中校验                                             |
| 工厂方法（DP）          | `of(T)`（参数即底层规范值）、`from`/`fromXxx`（跨类型转换）；`parse(String)` 仅当 `of`/构造器参数非 String 时出现                                     | 禁止 `valueOf(Object)`                                       |
| 局部变量类型            | `var`（JDK 10+）                                                                                                                                      | 显式冗长类型                                                 |
| DP 序列化               | JSON 契约（`@JsonValue` / `@JsonCreator`，单源 [conventions/dp-json-conventions.md §1 模式总表](docs/conventions/dp-json-conventions.md#1-模式总表)） | `Serializable` / `@Serial` / `serialVersionUID`              |

`var` 仅限局部变量——字段、方法参数、返回值不用。

### 2.2 校验参数顺序

`ValidateUtils` 校验方法参数遵循 Spring `Assert` 风格：被校验值在前，辅助参数在后（`minValue(value, 0, false)`）。辅助参数视为可信，仅被校验值允许 `@Nullable`。

### 2.3 空值校验角色

| 角色 | 判空工具 | 消息 |
|---|---|---|
| DP 类 | `ValidateUtils.notNull(value)` | 固定默认消息 |
| Entity 类 | `Assert.notNull(value, "field must not be null")` | 每处自解释 |
| 工具类 | `ValidateUtils.notNull(value)` | 固定默认消息 |

Entity 承载业务状态，判空应明确哪个字段为 null；DP 类型简单，默认消息足够。

### 2.4 控制流

- `if` / `for` / `while` 必须带 `{}`，禁止省略单行体
- 非必要不嵌套；优先卫语句（guard clause）快速失败；避免 `else` / `else if`
- 禁止显式类型转换，用泛型 / `Comparable<Self>` / 模式匹配消除强转

### 2.5 类内成员访问（`this`）

「写显式、读裸、调用裸」：赋值与集合增删必须 `this.`；字段读取、本类方法调用、委托协作调用禁止 `this.`。

### 2.6 异常

优先 `IllegalArgumentException` / `IllegalStateException`（不使用已废弃异常）；不在非异常路径上构造异常；工具类内联 IAE
消息，不引入消息工厂类。 **需要被 HTTP 状态码细分的业务拒绝**走 `ProblemDetailException` 族（静态工厂按场景创建，见
ADR-0015），其余拒绝维持 IAE / ISE。

### 2.7 成员顺序

**静态初始化依赖优先于可见性排序**：`static final` 常量若在构造/初始化表达式中引用另一静态字段，被引用者必须先声明——静态初始化按文本顺序执行，倒序即
`NullPointerException` → `ExceptionInInitializerError`（类首次使用即崩，编译期不报）。 **禁止无功能理由的成员重排**
：提交中的成员移位必须由本规范条款或可观察行为驱动，不得混入顺手的「整理」——无规则的排序审美正是上条事故的成因。

## 3. 注解

本节仅作索引——注解与库用法规则的单源在各规范篇，规则正文不在本文件。

| 库               | 规范篇                                                         |
|------------------|----------------------------------------------------------------|
| Jackson          | [docs/conventions/jackson.md](docs/conventions/jackson.md)     |
| JSpecify         | [docs/conventions/jspecify.md](docs/conventions/jspecify.md)   |
| Lombok           | [docs/conventions/lombok.md](docs/conventions/lombok.md)       |
| MapStruct        | [docs/conventions/mapstruct.md](docs/conventions/mapstruct.md) |
| Spring Framework | [docs/conventions/spring.md](docs/conventions/spring.md)       |
| Spring Modulith  | [docs/conventions/modulith.md](docs/conventions/modulith.md)   |

## 4. 注释规范

适用面：一切随代码提交的解释性文本——Java Javadoc 与实现注释、Flyway 迁移 SQL 与 DDL `COMMENT`、YAML / `.properties` / Gradle 构建脚本注释、Markdown HTML 注释、`lombok.config` 与 `.gitignore` 注释。

执行：code-review 以本节为核查基准（§4.7 清单），语义项人工判定；本项目不引入 Checkstyle、DocLint、markdownlint、CI 等自动门禁——机械可查项由 §4.7 清单逐项承载，漂移在审计中暴露、在迁移中修复。

### 4.1 范围、来源层级与适用面矩阵

**来源登记**（规则行的「来源」列只写下列 ID；钉住版本集中在本表，不在每行重复）

| 来源 ID | 权威与钉住版本                                                                                                                                  | 适用边界                                                                     |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `GOOGLE-JAVA` | Google Java Style，[`google/styleguide@e4d272e`](https://github.com/google/styleguide/commit/e4d272eb151adfb5f382501265172a99c6a19671)（2026-09-14），发布页 [javaguide.html](https://google.github.io/styleguide/javaguide.html) | 仅 Java：Javadoc（其 §7）；implementation comments 与 TODO（其 §4.8.6）      |
| `JLS` | Java SE 27 语言规范 [§3.7 Comments](https://docs.oracle.com/en/java/javase/27/docs/specs/jls/jls-3.html#jls-3.7)                                 | 仅 Java 注释词法（`//`、`/* … */`、不嵌套）；不提供分工或内容政策            |
| `JAVADOC` | JDK 27 [Documentation Comment Specification](https://docs.oracle.com/en/java/javase/27/docs/specs/javadoc/doc-comment-spec.html)                   | documentation comment 的识别与邻接、两种方言语法、main description / block tags |
| `SODA-POLICY` | 本项目政策（本节正文）                                                                                                                       | 中文、覆盖增量、override、no-op、意图与知识锚点；违反即审计缺陷              |
| `FORMAT-*` | 各非 Java 载体的一手格式来源，逐个登记在 §4.5 各 profile                                                                                      | 只为对应 profile 提供词法、缩进与示例事实                                    |
| `WFA` | `writing-for-agents`：`mattpocock/skills@c55ee46`，`SKILL.md` SHA-256 `1c0c4ebf…`                                                              | 只治理本节作为 agent 文档的写法（分组、单源、no-op、双清单）；不是注释内容政策来源 |

**来源维护**：比对触发、漂移分级（L1–L3）与维护 owner 见 [docs/doc-conventions.md](docs/doc-conventions.md) §9.2（唯一流程正文），本表只登记来源与钉住版本。

**级别**：**【强制】** 违反即审计缺陷，只能由 `GOOGLE-JAVA` / `JLS` / `JAVADOC` 的适用规则、`FORMAT-*` 的词法事实或 `SODA-POLICY` 硬门支撑；**【推荐】** 建议，偏离记录理由即可；**【参考】** 解释与示例，不产生通过/不通过结论。工具事实不记为风格缺陷，`FORMAT-*` 与 `WFA` 的建议不升格为 Google 要求，非 Java 规则一律标「项目扩展」。

**适用面矩阵**

| 载体                                                | 本节管辖                | 不管辖                         | 必要性 / 覆盖                                                                                       |
|-----------------------------------------------------|-------------------------|--------------------------------|------------------------------------------------------------------------------------------------------|
| Java `/** … */` 与 `///` Javadoc                    | §4.2 全部规则           | Java 代码行为本身              | visible class / member / record component 按 Google 底线；窄幅加严见 §4.2                            |
| Java `//`、`/* … */`                                | §4.3、§4.4              | 格式化器职责、构建门禁         | 只写签名与上下文表达不了的意图、约束、理由或非显然算法；TODO 另用固定格式                              |
| main Java                                           | §4.2–§4.4               | 按目录/文件数量补注释          | 先判可见性与 no-op，不设模块覆盖率                                                                   |
| test / testFixtures Java                            | §4.2–§4.4               | 为测试可见性另立规矩           | visible 声明仍受 Google 底线；包私有测试只在场景、fixture 契约或反直觉意图值得保留时写               |
| `package-info.java` / `package.html`                | §4.2–§4.4               | 包内每个声明的机械覆盖率       | 已有且仍有导航/契约价值的包文档保留；失真、重复、空洞则删/改                                         |
| Flyway/SQL 的 `--`、`/* … */`、`COMMENT ON`          | §4.5.1                  | SQL 命名、索引设计、迁移执行器行为 | 只保留迁移原因、兼容性与数据风险等代码本身表达不了的说明                                             |
| YAML / `.properties` 注释                           | §4.5.2、§4.5.3          | key 命名、schema、业务配置本身 | 只在配置意图、单位、取值约束、环境差异或危险默认值不能从结构看出时写                                 |
| Gradle/Groovy 构建脚本注释                          | §4.5.4                  | 构建逻辑与依赖治理             | 只写任务/变体/绕过原因、顺序契约与易错环境约束，不复述 DSL                                            |
| Markdown HTML 注释                                  | §4.5.5                  | 普通 Markdown 正文             | 只治理 `<!-- … -->`；正文质量与文档体系规则归 [docs/doc-conventions.md](docs/doc-conventions.md)      |
| `lombok.config` 的 `#` 注释                         | §4.5.6                  | 配置键与禁用清单的取舍         | 只治理注释与自检；清单内容单源 `docs/conventions/lombok.md` §1.2                                     |
| `.gitignore` 的 `#` 注释                            | §4.5.7                  | 忽略模式本身与模式正确性       | 只治理注释；上游模板段沿用不动、不审内容                                                             |

表外载体不预留空 profile：审计真实命中尚未列出的解释性文本载体时，先按 §4.5 的取源流程核验一手来源，再新建 profile 或决策票；命中确认前不给未出现的载体留规则位。已按命中建立的 profile 是 §4.5.6 与 §4.5.7；`gradlew*`、`LICENSE` 等 vendored 上游样板所有权在上游，只核验不动内容。

### 4.2 Javadoc

| 规则                                                                                                                            | 级别       | 来源                      | SODA 落地                                                                                                             |
|---------------------------------------------------------------------------------------------------------------------------------|------------|---------------------------|------------------------------------------------------------------------------------------------------------------------|
| 每个 visible class、member、record component 写 Javadoc                                                                        | 【强制】   | `GOOGLE-JAVA` §7.3        | 顶层类仅 public 时 visible；member 还需容器 visible 且成员为 public/protected；record component 随 visible record 可见  |
| 只有在签名真正自解释、且没有任何比名称字面含义更多信息时才可省略                                                               | 【强制】   | `GOOGLE-JAVA` §7.3.1      | `getXxx()`、纯状态谓词只是通常候选，不是自动豁免；典型读者需要的 null、单位、状态范围、副作用不能省                    |
| `@Override` 成员默认不写 Javadoc                                                                                                | 【强制】   | `SODA-POLICY`             | 实现新增、收紧、改变或解释上层契约时才写，且只写相对上层契约的增量；不复制父文档，允许 `{@inheritDoc}` 但不强制       |
| 非 visible 的聚合、子实体、领域服务、领域事件、DP、Identifier、enum 在类型名与结构表达不全领域角色或不变量时写类级 Javadoc       | 【强制】   | `SODA-POLICY`             | 不是「所有领域类型一行」的数量门                                                                                       |
| 非平凡领域操作与应用编排在签名表达不了前置/后置条件、跨聚合语义、事务/副作用边界时写 Javadoc                                   | 【强制】   | `SODA-POLICY`             | DTO 字段、query accessor、infrastructure override、测试与 testFixtures 不按类别机械补文档                              |
| Traditional `/** … */` 与 Markdown `///` Javadoc 均合法；新增跟随文件既有方言，文件无先例时用 Traditional                       | 【强制】   | `GOOGLE-JAVA` §7.1.1 ＋ `SODA-POLICY` | 存量不因方言本身迁移或改写；两类注释各按自己的段落与续行规则，不互套                        |
| Traditional：多行每行以与 `/**` 对齐的 `*` 起始；段落之间与 block tag 组之前放只含对齐 `*` 的空行；除首段外每段首词前紧接 `<p>` 且其后不留空格，其它 block-level 元素前不加 `<p>`；单行 `/** … */` 只在整块（含标记）放得下且没有 block tag 时可用 | 【强制】   | `GOOGLE-JAVA` §7.1.1、§7.1.2 | —                                                                                                                  |
| Markdown：连续 `///` 行紧邻对应声明，段落之间用空的 `///` 行分隔，不写 `<p>`                                                     | 【强制】   | `GOOGLE-JAVA` §7.1.1、§7.1.2 ＋ `JAVADOC` | doclet 只识别紧邻声明的一个 documentation comment，多取最靠近声明者                                          |
| 每个 Javadoc 以 summary fragment 开头：名词短语或动词短语，不是完整陈述句，不用 `@return` 当唯一摘要                            | 【强制】   | `GOOGLE-JAVA` §7.2        | 按完整句的大小写与句末标点书写；中文无大小写、句号保留；不得以「此方法/该方法/本类……」开头               |
| 用到的 block tag 顺序固定 `@param` → `@return` → `@throws` → `@deprecated`，描述不得为空                                        | 【强制】   | `GOOGLE-JAVA` §7.1.3      | 只约束实际用到的标签，不要求凑齐四类                                                                                   |
| block tag 不能放一行时的续行缩进：Traditional 相对 `@` 至少四格，Markdown 恰好两格                                               | 【强制】   | `GOOGLE-JAVA` §7.1.3      | Markdown 缩进四格在部分语境会被解析成缩进代码块                                                                        |
| visible 之外的 Javadoc 按需存在，建议沿用同一格式与摘要规则                                                                     | 【推荐】   | `GOOGLE-JAVA` §7.3.4      | —                                                                                                                      |

**summary fragment 的判据**：它是 class/method index 里唯一被取用的文本，因此先写它，再写需要的描述段与 block tags。

```java
// ✅ Traditional：动词短语 + 句号
/** 返回客户 ID。 */

// ❌ 用 @return 当摘要
/** @return 客户 ID */

// ❌ 完整陈述句 + 「此方法」开头
/** 此方法返回客户 ID。 */

// ✅ Markdown：连续 /// 行，段落用空 /// 行分隔，不写 <p>
/// 更新客户名称。
///
/// 名称冲突时抛出 {@link DuplicateNameException}。
```

**block tag 续行**：

```java
// ✅ Traditional：续行相对 @ 缩进四格
/**
 * @param rawName 原始名称；
 *     空白已在紧凑构造中归一化
 */

/// ✅ Markdown：续行相对 block tag 恰好两格
/// @param rawName 原始名称；
///   空白已在紧凑构造中归一化
```

**两个省略情形的判据**：self-explanatory 要求「确实没有比名称字面含义更多的内容」——`canonicalName()` 若读者无从判断其含义、单位或可空性，就不满足；override 的省略是本项目的 `SODA-POLICY`，Google 只说明覆盖方法「不总是有」Javadoc，并未明令重复或禁止重复。

### 4.3 Implementation comments 与 TODO

| 规则                                                                                          | 级别       | 来源                         | SODA 落地                                                                                          |
|-----------------------------------------------------------------------------------------------|------------|------------------------------|-----------------------------------------------------------------------------------------------------|
| `//` 与 `/* … */` 只承担实现注释，Javadoc 另按 §4.2                                            | 【强制】   | `GOOGLE-JAVA` §4.8.6         | 分工来源是 Google §4.8.6；`JLS` §3.7 只定义词法，不作分工依据                                       |
| 实现注释与周围代码同层缩进；多行 `/* … */` 的续行以与上一行 `*` 对齐的 `*` 起始                | 【强制】   | `GOOGLE-JAVA` §4.8.6.1       | `//` 注释保持同层缩进即可，不强制多行说明改用块注释                                                |
| 禁止用星号或其它字符画盒子边框                                                                | 【强制】   | `GOOGLE-JAVA` §4.8.6.1       | 只禁装饰形状：正常的 `*` 续行对齐不受影响，手工围成边框的多行注释一律不合规                          |
| 需要 formatter 重排的多行说明用 `/* … */`                                                     | 【推荐】   | `GOOGLE-JAVA` §4.8.6.1       | 这是选择依据，不是「所有多行说明一律块注释」的要求                                                 |
| TODO 只标 temporary、short-term 或 good-enough-but-not-perfect 的代码                          | 【强制】   | `GOOGLE-JAVA` §4.8.6.2       | 不是清晰代码或透彻理解的替代品                                                                     |
| TODO 格式固定 `TODO: <资源链接> - <说明>`                                                      | 【强制】   | `GOOGLE-JAVA` §4.8.6.2       | `TODO` 全大写带冒号；资源链接提供上下文（优先 bug 引用）；连字符引出说明；无资源链接即不合规         |
| TODO 的资源链接与说明内容                                                                     | 【强制】   | `GOOGLE-JAVA` §4.8.6.2 ＋ `SODA-POLICY` | 不以人名/团队当上下文；「未来某日期」必须写具体日期或事件；说明用中文（§4.4），链接用项目内可达地址 |
| TODO 示例只写语法，不放真实外链                                                               | 【强制】   | `SODA-POLICY`                 | 临时实施细节随 TODO 闭环删除，不转成长久决策历史；不写 `.scratch/` 或外部参考仓路径                 |

```java
// ✅ 语法（资源链接占位，按实际可达地址替换）
// TODO: <issue-url> - 边界确认后移除 fail-fast

// ❌ 无资源链接
// TODO: 移除 fail-fast
```

### 4.4 中文内容政策与知识锚点

本节全部条目是 **【强制】 · `SODA-POLICY`**：它们是项目内容政策，不得归因给 Google 或任何格式来源。

1. 注释用中文，保留代码标识符、协议字段、标准 tag 与不可直译的领域术语（语言政策见 §1）。
2. 行为描述用主动语态与现在时，句子简洁；避免「被……」「将会……」这类无信息句式。
3. 只写签名、类型、上下文和代码本身表达不了的契约、不变量、意图、原因、副作用或反直觉语义；不逐步复述实现。
4. 逐句过 **reader-relative no-op 测试**：删掉这句，典型读者是否失去信息？失去则留，否则删整句。该测试是本项目的代码注释内容政策，与 `WFA` 对 agent 指令的 model-relative no-op 不是同一件事。
5. 需要领域词义时引用 `CONTEXT.md` 的规范术语；需要稳定决策理由时写「见 ADR-NNNN」。只有引用能替代重复正文时才链接，不要求每条注释附链接。
6. 决策历史、日期、「原为/现为」、被否方案归 git；需求/实施票据与 `.scratch/` 路径不进入正式源码、配置或迁移注释（适用面按 §4.1 矩阵：Java 注释由本节直接管辖，非 Java 载体的同款禁令由其 §4.5 profile 的内容条款承接）。

注释是**意图载体**，不是行为记录：

| 写                                                                 | 不写                                                                                       |
|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| 不变量、前置/后置条件、吸收态语义                                  | 复述方法名的单句（"修改用户名。"）                                                         |
| 规则**为什么**存在（最终态理由）                                   | 逐步描述实现过程                                                                           |
| CONTEXT.md 术语原词、「见 ADR-NNNN」锚点（编号即定位、标题即命名） | 复述 CONTEXT.md 词条定义；inline 展开 ADR 决策内容                                         |
| 签名之外的契约（可空、时钟注入、副作用、事务边界）                 | 显而易见的直译                                                                             |
| 反直觉语义（如 `isVerified()` 含已使用态）                         | 环境可见事实（序列化细节、框架声明）                                                       |
| —                                                                  | **决策历史**：日期、变更沿革、"原为…现为…"、被否方案——演进叙事归 git 历史，不归注释        |
| —                                                                  | **需求票据引用**：`.scratch/` 票据（编号 / 路径）——闭环即归档删除；需锚定用「见 ADR-NNNN」 |

### 4.5 非 Java 注释 profiles（项目扩展）

本节以下全部是**项目扩展**：继续同处 §4 作为唯一注释规范正文，但每个载体独立成 profile，只引用各自格式来源，禁止从 Java 规则或 Google Java Style 类推。内容质量条款一律标 `SODA-POLICY`——格式来源只回答「怎么写得被解析」，不回答「写什么才值得写」。

取源流程：新载体先找维护方或格式所有者的一手资料，登记 canonical 地址、版本/日期、规范强度、可采纳规则与不可类推项，再落规则；来源缺失时不写规则、不类推 Google。

非 Java 载体的一手来源研究记录、来源事实与验证状态汇总在 [docs/research/non-java-comment-authorities.md](docs/research/non-java-comment-authorities.md)；本节各 profile 的规则以该记录登记的来源层级为准，两者冲突时先修来源登记再改规则。

#### 4.5.1 SQL / Flyway

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-SQL`：[PostgreSQL 18.6 §4.1.5 Comments](https://www.postgresql.org/docs/18/sql-syntax-lexical.html#SQL-SYNTAX-COMMENTS)（示例方言，访问 2026-09-25）；`FORMAT-FLYWAY`：[SQL migration prefix](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-sql-migration-prefix-setting)、[Repeatable migrations](https://documentation.red-gate.com/flyway/reference/tutorials/tutorial-repeatable-migrations)（页面更新 2026-09-24）；目标方言的一手文档优先于示例方言 |
| 不可类推 | PostgreSQL 的可嵌套 block comment 不是所有数据库的保证；Flyway 官方不提供跨数据库的注释风格；「注释是否参与 checksum」未经来源证实，不得断言 |

- **【强制】· `SODA-POLICY`** 单行注释用 `--`，多行说明用 `/* … */`；两者在目标方言中的终止、嵌套与字符串交互必须以该方言一手文档为准（本项目开发/测试库按 [ADR-0022](docs/adr/0022-dev-database-management.md)）。
- **【强制】· `SODA-POLICY`** 迁移脚本随本次迁移用 Flyway 实际执行/验证确认注释未破坏脚本。
- **【强制】· `SODA-POLICY`** 注释只写迁移目的、数据兼容性、回滚风险等代码本身表达不了的事实；不把注释当 DDL 语义。DDL `COMMENT ON` 属 schema 说明，同样受本条治理。
- **【参考】** SQL 命名、索引设计、迁移执行器行为不在本节——归对应数据库与迁移规范。

#### 4.5.2 YAML

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-YAML`：[YAML 1.2.2 §3.2.3.3 / §6.6](https://yaml.org/spec/1.2.2/#3233-comments)（修订 2021-10-01，访问 2026-09-25） |
| 不可类推 | 注释是 presentation detail、不能承载数据；`#` 在 quoted scalar 中是字符而非注释；来源不规定措辞、句号或「每个 key 都要解释」 |

- **【强制】· `FORMAT-YAML`** 注释用 `#`；必须与其它 token 以空白分隔（独立成行的首列注释不需要前置空白）；不放进 quoted scalar 或 block scalar。
- **【推荐】· `SODA-POLICY`** 独立成行，紧邻被说明的节点；行内注释的 `#` 前保留分隔空白。
- **【强制】· `SODA-POLICY`** 只在配置意图、单位、取值约束、环境差异或危险默认值不能从结构看出时写；不把配置值移进注释。
- **【参考】** key 命名、schema 与业务配置治理不在本节。

#### 4.5.3 `.properties`

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-PROPERTIES`：Oracle Java SE 25 [`Properties.load`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Properties.html#load(java.io.Reader))（页面元数据 JDK 25，2026-07-15；访问 2026-09-25） |
| 不可类推 | API 只定义行词法与编码，不规定解释性内容质量；其它 reader 可能有扩展，不能把 `Properties` 细节无条件推广到所有配置文件 |

- **【强制】· `SODA-POLICY`** 只用整行 `#` 注释（API 允许 `!`，本项目不作首选）；每行自带标记，不用反斜杠续行。
- **【强制】· `SODA-POLICY`** 改动后用实际 `java.util.Properties` loader 做 smoke test，确认编码与续行边界未被破坏。
- **【强制】· `SODA-POLICY`** 只写 override 前提、安全影响、环境差异、单位或取值范围等不能从 key/value 看出的事实。

#### 4.5.4 Gradle / Groovy 构建脚本

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-GROOVY`：Apache Groovy [4.0.29 `GroovyLexer.g4`](https://repo.maven.apache.org/maven2/org/apache/groovy/groovy/4.0.29/groovy-4.0.29-sources.jar)（`SL_COMMENT` / `ML_COMMENT` / `SH_COMMENT`）；`FORMAT-GRADLE`：[Gradle 9.5.1 Initialization Scripts](https://docs.gradle.org/9.5.1/userguide/init_scripts.html)（项目 wrapper 版本，访问 2026-09-25） |
| 不可类推 | Gradle 官方文档没有独立的注释排版或内容政策；Groovy grammar 不提供解释性散文风格；Kotlin DSL 未登记来源，需另行核验 |

- **【强制】· `FORMAT-GROOVY`** 用 parser 合法的 `//` 或非嵌套 `/* … */`；shebang 只出现在首行；只有需要 GroovyDoc 语义时才写 `/** … */`。
- **【强制】· `SODA-POLICY`** 只写任务/变体/绕过原因、顺序契约与易错环境约束，写在相关 task/plugin/config 附近；不复述 DSL 已经说明的事情。
- **【参考】** 构建逻辑、任务语义与依赖治理不在本节——归 Gradle 与依赖规范（§5）。

#### 4.5.5 Markdown HTML 注释

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-MARKDOWN`：[CommonMark 0.31.2 §6.6 HTML comments](https://spec.commonmark.org/0.31.2/)（2024-01-28，访问 2026-09-25），其 raw HTML 规则引用 HTML Standard |
| 不可类推 | CommonMark 没有原生 comment token；`<!-- … -->` 是 raw HTML，不是跨 renderer 一致隐藏的通用注释；来源不规定放置位置、措辞或是否应该存在 |

- **【强制】· `FORMAT-MARKDOWN`** 用闭合明确的 `<!-- … -->`，内容控制在单行并避免出现 `-->`。
- **【强制】· `SODA-POLICY`** 用仓库实际 renderer 验证渲染结果与源码可读性；注释不得成为面向读者的唯一说明。
- **【参考】** Markdown 正文质量、frontmatter 与文档体系规则归 [docs/doc-conventions.md](docs/doc-conventions.md)——本 profile 只管 `<!-- … -->` 本身。

#### 4.5.6 `lombok.config`

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-LOMBOK-CONFIG`：Lombok [Configuration system](https://projectlombok.org/features/configuration)（页面无版本声明，功能级版本句至 v1.18.48；访问 2026-09-29）——`Overview` 规定以 `#` 起始的行为注释、`config.stopBubbling`、`lombok.*.flagUsage` 合法值 `warning`/`error`，并给出 `java -jar lombok.jar config` 命令形态 |
| 不可类推 | 官方不规定注释的内容、语言、长度、分节，也未规定 `#` 转义；rc=0 / rc=2 的语义文档零表述（证据为官方源码与实测），不得当作文档契约 |

- **【强制】· `FORMAT-LOMBOK-CONFIG`** 注释独占整行并以 `#` 起始；来源只支持这一种形态，行内注释与 `#` 转义没有来源，不使用。
- **【强制】· `SODA-POLICY`** 只写配置意图、禁用前提、维护与自检方式这类结构看不出的内容；逐句 no-op，不复述逐键含义，不写决策历史、日期与票据指针（§4.4 通用禁令）。
- **【强制】· `SODA-POLICY`** 改动后执行 `java -jar <lombok.jar> config <仓库根>` 自检并以 rc=0 为通过：rc=2 表示未知键或坏值、rc=1 表示命令行或路径错误——该 rc 语义来自 Lombok 官方源码与实测，不是文档规定。
- **【参考】** 禁用清单本身（禁哪些键、为何禁）单源 `docs/conventions/lombok.md` §1.2；本 profile 只管注释与自检，不管配置取舍。

#### 4.5.7 `.gitignore`

| 项 | 内容 |
|---|---|
| 来源 | `FORMAT-GITIGNORE`：git [`gitignore(5)`](https://git-scm.com/docs/gitignore)（页面标注 last updated in 2.55.0，2026-06-29；访问 2026-09-29）`PATTERN FORMAT`——以 `#` 起始的行为注释、模式以 `#` 开头时用 `\#` 转义；GitHub [`github/gitignore`](https://github/github/gitignore) 的 `Java.gitignore`（末次变更 2021-11-14，CC0）是示例模板而非规范 |
| 不可类推 | 官方不规定注释的内容、语言、分节与排版；已核验的格式所有者唯一为 `gitignore(5)`，未做穷举性否定，新来源出现时按 [docs/doc-conventions.md](docs/doc-conventions.md) §9.2 比对；模板的英文措辞不构成规范，沿用也不成为本项目要求 |

- **【强制】· `FORMAT-GITIGNORE`** 注释独占整行并以 `#` 起始；模式本身以 `#` 开头时写成 `\#`。
- **【强制】· `SODA-POLICY`** 项目专属段的注释只写分组意图与忽略理由这类结构看不出的内容；逐句 no-op，不写决策历史、日期与票据指针（§4.4 通用禁令）。
- **【强制】· `SODA-POLICY`** 上游模板段沿用不动：不审内容、不改写、不补条目；模板段与项目专属段的分界以与 `Java.gitignore` 的逐行比对结果为准，一致的行为模板段。

### 4.6 写清单

服务新增或修改注释，按序执行，完成判据在末条：

| # | 动作 |
|---|---|
| 1 | 判定载体：Java Javadoc、Java 实现注释、SQL/Flyway、配置、构建脚本、Markdown HTML 注释、`lombok.config`、`.gitignore` 或审计新增载体 |
| 2 | 先做 no-op：没有独立信息就删除整句/整条，不创建 no-op 注释 |
| 3 | 读取该载体的登记来源（§4.1、§4.5）；非 Java 来源缺失时停止规则化，不类推 Google |
| 4 | Java 先判 visibility、self-explanatory/override 例外与 SODA 领域加严，再选文件局部方言 |
| 5 | 按方言写 Javadoc 格式、summary fragment 与非空 block tags；实现注释与 TODO 分流 |
| 6 | 用中文写契约/不变量/意图/原因；需要时链接 CONTEXT/ADR，删除历史、票据与复述 |
| 7 | 非 Java 文本只应用本 profile 的词法、缩进与内容政策 |
| 8 | 完成判据：规则有正确来源与级别、格式可被对应工具识别、无 no-op/失真/重复/过时内容、无 `.scratch` 指针 |

### 4.7 code-review 清单与完成判据

服务审计与迁移复核；逐项给出通过/不通过条件，不引入新的构建门禁。

| # | 检视项 | 通过条件 |
|---|--------|----------|
| 1 | 载体与来源 | 文件/符号/载体、规则来源、级别与适用 profile 正确；非 Java 未误引 Google |
| 2 | 覆盖与省略 | visible class/member/record component 已覆盖；self-explanatory 与 override 省略有可判定理由；未把 SODA 窄幅加严扩成机械覆盖率 |
| 3 | 方言格式 | Traditional/Markdown 的段落、`<p>`、block-tag 顺序、非空描述与续行缩进各自合规 |
| 4 | 摘要与分工 | summary 是中文名词/动词片段而非「此方法……」完整陈述句；实现注释未被当作 Javadoc |
| 5 | TODO | 满足 `TODO: <资源链接> - <说明>`，且只描述临时、不完美的工作 |
| 6 | 内容质量 | 只保留契约、不变量、意图、原因与反直觉语义；逐句 no-op；主动语态、现在时、简洁与中文政策均通过 |
| 7 | 锚点与残留 | CONTEXT/ADR 引用稳定且必要；无决策历史、外部参考仓或 `.scratch` 票据残留 |
| 8 | 分载体边界 | main/test/testFixtures、SQL/Flyway、配置、构建与 Markdown 按各自边界审计，未对无注释载体制造数量指标 |
| 9 | 单源与处置 | 单源、链接、版本锚点与迁移处置记录可达；每个发现都有修/删/移/保留及复核结果 |
| 10 | 完成判据 | Java 适用规则全部通过、项目扩展来源诚实、所有发现逐项复核、无新增构建门禁 |

## 5. 依赖管理

- `implementation`：运行时需要的依赖（Jackson 注解、spring-modulith 等）
- `compileOnly`：仅编译期需要（Lombok）
- 基础模块不用 `api` 暴露传递依赖，除非被依赖的是核心框架能力
- `testImplementation`：测试需要的依赖
