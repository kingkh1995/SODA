# SoftwareVersion DP — 通用三段式软件版本号

新增通用软件版本号 DP（`com.soda.component.domain.types.SoftwareVersion`，位于 soda-components 的 domain-types 模块）：三段式纯数字（major.minor.patch）、带小写 `v` 前缀、前导 0 归一化（`v2.001.003` ≡ `v2.1.3`）、可逐段数值比较、支持 base-1000 打包 int 互转与 `nextPatch`/`nextMinor`/`nextMajor` 步进。既有 `Version` 是乐观锁版本号（单 int 计数器），语义完全不同，不能复用；软件版本号在应用升级、协议版本、依赖声明等场景都需要三段式比较与格式化，需要一个独立 DP。

## 决策

- **命名 `SoftwareVersion`**：与乐观锁 `Version` 长期共存于同一包，名字必须零歧义。`SemVer` 被否（本 DP 无 pre-release/build 后缀，不是严格 SemVer，叫这名字误导）；`VersionNumber`/`AppVersion` 区分度弱或不够通用。
- **严格三段**：`v2`、`v2.1`、`v2.1.3.4`、`v2..3`、`v2.1.3 ` 一律拒绝。不做"省略尾段补 0"的宽松解析——补 0 引入"`v2.1` 是不是 `v2.1.0`"的歧义，通用 DP 宁可严格。
- **前缀必选、大小写不敏感**：输入必须带 `v`/`V`（`2.1.3` 拒绝），规范值统一小写 `v2.1.3`。不悄悄补前缀——格式校验就是格式校验，符合"不修理输入、只归一化格式"纪律（dp-conventions §4.4）。
- **单段上限 999**：跟随主流三位段实践（每段 0-999）。正则 `[vV]\d{1,3}\.\d{1,3}\.\d{1,3}` 一步同时约束格式、段数与上限；段位到 999 的步进抛 IAE，不进位——`nextPatch` 语义必须只动 patch，进位会让调用方对方法语义产生疑惑。
- **前导 0 归一化 + value-equality**：`of(String)` 经 `ParseUtils.parseInt`（容忍前导 0）再以 `Integer.toString` 重组规范值，去 0 零成本；`equals`/`hashCode` 只基于规范值字符串（派生 major/minor/patch 字段不参与）。
- **base-1000 打包 int**：`toPackedInt()`（`v2.1.3` → `2001003`）/ `fromPackedInt(int)`（越界/负数拒绝）。999 上限使每段恰好 3 位十进制，打包无损；打包序与版本序单调一致，可直接用于 DB int 列存储、索引与范围查询。
- **最小面 + 显式步进**：`of(String)`（@JsonCreator DELEGATING，唯一主入口）、`from(int,int,int)`（三段 int 属转换层，不占用 `of`——遵守"每个 DP 只有一个主入口"）、`major()/minor()/patch()`、`compareTo`、`value()`、`toPackedInt()`、`fromPackedInt(int)`、`nextPatch()/nextMinor()/nextMajor()`。不提供 `ZERO`/`ONE` 常量与布尔比较糖（`Comparable` 已覆盖），不提供裸 `next()`（步进哪段有歧义）。

## Considered Options

| 方案 | 否定原因 |
|---|---|
| 复用/改造 `Version` | 乐观锁版本号（单 int、缓存 [0,99]、`next()`）语义不同，混用即事故 |
| `SemVer` 命名 | 无 pre-release/build 后缀，不是严格 SemVer |
| 单段上限 `Integer.MAX_VALUE`（SemVer 规范值） | 产品决策跟随主流三位段实践；999 使 base-1000 打包无损 |
| 前缀可选（`2.1.3` 也接受） | "悄悄补前缀"违背不修理输入纪律；宽松输入放行脏数据 |
| 省略尾段补 0（`v2` = `v2.0.0`） | 引入版本等价歧义，严格契约更可预测 |
| 段位到顶进位（`v2.1.999` → `v2.2.0`） | `nextPatch` 语义必须只动 patch，进位使方法名与行为脱节 |
| 三 int 字段直接相等性 | 与字符串规范值双事实源；打包/JSON 仍需字符串 |

## Consequences

- 规范值字符串与打包 int 是同一版本的两个视角；`equals`/`hashCode`/JSON 只走字符串，打包值不参与。
- 段上限 999 意味着 `v1000.0.0` 不可表示；未来若需放宽属 breaking change（规范值格式与打包编码同时受影响）。
- dp-conventions 附录 B 同步登记该 DP；CONTEXT.md Language 节新增词条，与 `Version`（乐观锁）在术语层明确区分。
