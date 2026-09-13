---
type: Convention
title: DP 校验与归一化规范
description: 写或测 DP 的校验/归一化时读——三入口职责（构造器＝唯一校验点）、校验收敛、编排规则与归一化细则。工具 API 以源码为准，判空角色单源在 STYLEGUIDE §2.3。P3C【强制/推荐/参考】三级标定 + ❌/✅ 反例正例。
tags: [ convention, dp, testing ]
status: stable
---

# DP 校验与归一化规范

适用范围： **DP 类型与 DP 侧工具**（`ParseUtils` / `ValidateUtils` / DP 构造器与工厂）。工具 API
以源码为唯一事实源——本文只给场景 →
入口的收敛边界，不承诺穷举 API 面；判空角色（DP / Entity / 工具类）单源在
[STYLEGUIDE §2.3 空值校验角色](../../STYLEGUIDE.md#23-空值校验角色)。DP
的结构与契约见 [dp-conventions §2 核心行为契约](../dp-conventions.md#2-核心行为契约) /
[§3 构造与工厂命名](../dp-conventions.md#3-构造与工厂命名)；
Entity / Aggregate 的校验不归本文裁判。

## 1. 职责分层

下表各行均为【强制】：

| 入口                                               | 职责                                                                                                | 校验位置                             | 示例                                             |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------|--------------------------------------|--------------------------------------------------|
| 构造器（record 公共紧凑构造器 / class 私有构造器） | **唯一校验点**；格式归一化（不改语义）                                                              | `ValidateUtils`（禁 `ParseUtils`）  | `new LongId(long)`、`record Fen(int)` 紧凑构造器 |
| `of(T)`                                            | 反序列化入口；**T ≡ 序列化值类型**（int DP → `of(int)`）；直达构造器（缓存 / 单例分派是允许的中介） | 无业务校验（线形态格式守卫见下）     | `ConcurrencyVersion.of(int)`                     |
| `parse(Object/String)`                             | 不可靠输入入口                                                                                      | `ParseUtils`（含 `notNull`）→ 构造器 | `PositiveInt.parse(String)`                      |
| `from(...)` / `fromXxx(...)`                       | 跨类型转换（含转换前置校验，允许与构造器重复；守卫保护转换表达式本身）                              | `ParseUtils` + `ValidateUtils` 守卫  | `WanYuan.fromYuan(BigDecimal, RoundingMode)`     |

**判据句**：去掉该检查，非法值还能不能构造出来？能 → 必须在构造器；不能（只是转换表达式会以错误类型爆炸）→ 允许留在转换入口，但与构造器
重复时 **不得成为不变量的唯一校验点**。

**两条入口/构造器边界**（【强制】，2026-09-13 裁定）：

- **class 形态私有构造器只接收语义形态**：构造器内只允许 `ValidateUtils` 与不改变语义的格式归一化，**不得调用
  `ParseUtils`**——「不可靠输入 → 基础类型」的转换一律留在 `of` / `from` / `parse` 工厂。构造器内出现 `ParseUtils` ＝
  解析层与不变量层混层：该形态下数值入口只能把已校验分量重新线形态化、再交构造器反解（反模式见 §1 末）。record 形态不受本条限制
  ——典范构造器即入口层（`Avatar` 的 `ParseUtils.parseUri` + `ValidateUtils.validUrl`、`Ciphertext` 的 `decodeHeader`
  即该形态的正确写法）。
- **线形态入口的格式守卫属转换契约**：构造器入参为语义分量时，线形态（String）的格式规则只能在线形态入口守——去掉它，
  `v0001.2.3` 一类非法线格式会被静默归一化接受。该检查落在判据句「非法值构造不出来」分支，允许留在入口；它不与构造器重复
  （构造器看不见线形态），不构成「不变量的唯一校验点」违规。线格式是**输入契约**，实例不变量（如 `SoftwareVersion`
  的段范围 ＋ 规范串渲染）仍由唯一构造器承担。

**入口矩阵**（按输入性质选入口）：

| 输入性质                                                        | 入口                                                       | 守卫              | null 语义         |
|-----------------------------------------------------------------|------------------------------------------------------------|-------------------|-------------------|
| 规范值类型的原语，调用方保证非空                                | `of(int)` / record 紧凑构造器                              | 无（类型保证）    | 不可表示          |
| 规范值类型的包装形态（静态类型 `Integer` / `Long` / `Boolean`） | `from(Integer)` → `ValidateUtils.notNull` → 委托 `of(int)` | 必要              | 错误（IAE）       |
| 领域其他类型（`BigDecimal` / `Instant` / 其它 DP）              | `from(T)`                                                  | 转换前置守卫      | 错误（IAE）       |
| 不可靠输入（String / Object，外部来源）                         | `parse(...)`（`ParseUtils`）                               | `ParseUtils` 内建 | 错误（IAE）       |
| 「无值」                                                        | 不构造                                                     | —                 | 用 `Optional<DP>` |

- 【强制】调用方 **不得静默拆箱**——实参静态类型为包装类型时一律走 `from(包装)`；`of` / 构造器只接受原语。
- 【强制】枚举的编译器生成入口 `valueOf(String)` 在生产代码不得暴露或调用——公开入口一律 `of(String)`（委托
  `ParseUtils.parseEnum`）；测试可用 `valueOf` 作为 `of` 的对照。
- 【推荐】`from(包装)` 按需补齐（出现包装入参调用点时新增，不预建）；`from` 无 `@JsonCreator`，不影响 round-trip 与 JSON 入口。

**前置守卫 vs 业务校验**（本文单源）：`from(BigDecimal)` 中的 `notNull(value)` 防止 `value.toPlainString()` NPE，不是 DP
的业务不变量——
守卫只保护到达构造器之前的代码（转换表达式与拆箱），不会出现在构造器中。构造器中的 `notNull` 则是业务校验（值不允许为 null
是领域约束）。

❌ 用 `Objects.requireNonNull` 顶替 DP 侧判空（语义与消息不统一）：

```java
// ❌ NPE 语义 + 每处自解释消息
Objects.requireNonNull(value, "value");
// ✅ DP / 工具类统一 IAE + 固定默认消息
ValidateUtils.notNull(value);
```

❌ 入口把已校验的语义分量重新序列化成规范表示，交构造器反解（同一组不变量在一次调用里「拆—校—拼—再拆—再校」）：

```java
// ❌ 段范围查 6 次、线格式正则 1 次、字符串回炉 1 次
public static SoftwareVersion from(int major, int minor, int patch) {
    ValidateUtils.range(major, 0, MAX_SEGMENT);
    ValidateUtils.range(minor, 0, MAX_SEGMENT);
    ValidateUtils.range(patch, 0, MAX_SEGMENT);
    return new SoftwareVersion("v" + major + "." + minor + "." + patch);   // 构造器再拆、再 parse、再校
}

// ✅ 直达唯一构造器：校验只发生一次，规范串由构造器渲染
public static SoftwareVersion from(int major, int minor, int patch) {
    return new SoftwareVersion(major, minor, patch);
}
```

**不计入本反模式的三类边界**（防误改）：① 入口持有的是另一种表示、构造器那次检查**承重**——`Digest.fromBase64` 的 `HEX_64`
是 32 字节长度的唯一检查，删掉即放行任意长度摘要；② 入口里命中的是**分支**不是校验——`UpdateMask.parse` 的
`contains(WILDCARD)` 命中即 `return new UpdateMask(allowedFields)`；③ **转换前置守卫与构造器防御断言**——`Percentage` /
`WanYuan` 的 `notNull`、`DecimalLiteralType` 的 `setScale(UNNECESSARY)` 已由本文 §1 / §4 明文允许。

## 2. 校验收敛

下表各行均为【强制】—— **场景 → 唯一入口 → 禁止替代**的收敛边界（不承诺穷举 API；方法名 / 签名以源码为准）：

| 场景               | 唯一入口                                                             | 禁止替代                          |
|--------------------|----------------------------------------------------------------------|-----------------------------------|
| null 检查          | `ValidateUtils.notNull(value)`                                       | `Objects.requireNonNull`          |
| blank              | `ValidateUtils.hasText(value)`                                       | `String.isBlank() + if/throw`     |
| 字符串长度         | `ValidateUtils.maxLength(value, max)`                                | 内联 `if/throw`                   |
| 数值范围           | `ValidateUtils.minValue` / `range`                                   | 内联比较                          |
| 正则格式           | `ValidateUtils.matches(value, pattern)`                              | 内联 `if (!pattern.matcher(...))` |
| 前缀校验           | `ValidateUtils.hasPrefix(value, prefix)`                             | 内联 `startsWith` + `if/throw`    |
| URI                | `ValidateUtils.validUrl(uri)`                                        | `try/catch URI` 校验              |
| Base64 / Base64url | `ParseUtils.parseBase64` / `parseBase64Url`（解码校验归 ParseUtils） | try/catch 解码校验                |
| 类型转换           | `ParseUtils.parseXxx(value)`                                         | 手动 cast / 类型推断              |

适用范围：本表只约束 DP 类型与 DP 侧工具；域对象基类（Entity / Aggregate）的判空与校验不在本表裁判（角色划分见
[STYLEGUIDE §2.3 空值校验角色](../../STYLEGUIDE.md#23-空值校验角色)）。

【强制】新增校验场景时四步：①在 `ValidateUtils` 添加校验方法（英文消息、首字母小写、无句号）；②如需类型转换，在 `ParseUtils` 添加方法
（内部可调 `ValidateUtils`）；③DP 构造器 / `parse` 调用对应工具方法；④ **若引入新场景**，在本表登记一行（不穷举
API）。完成判据：新方法已落地且
本表已登记对应行；DP 构造器 / `parse` 已调用；`build` 绿。

❌ 内联 `if/throw` 替代既有校验方法：

```java
// ❌ 手写长度 / 格式校验（消息自成一套）
if (value == null || value.length() > 70) throw new IllegalArgumentException("too long");
// ✅ 工具方法（统一消息）；方法名以源码为准
ValidateUtils.maxLength(value, 70);
```

❌ try/catch 顶替解码校验（重复实现 + 吞异常）：

```java
// ❌ 自己解码再包装异常
try { Base64.getDecoder().decode(raw); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("bad"); }
// ✅ 解码即校验，归 ParseUtils
byte[] bytes = ParseUtils.parseBase64(raw);
```

## 3. 编排规则

标准流程： **`ParseUtils` 做转换 → `ValidateUtils` 做校验 → DP 构造器 / 工厂编排**。

| 步骤 | 工具                            | 职责                   | 示例                                      |
|------|---------------------------------|------------------------|-------------------------------------------|
| ①    | `ParseUtils.parseXxx(Object)`   | 不可靠输入 → 基础类型  | `ParseUtils.parseLong(raw)`               |
| ②    | `ValidateUtils.xxx(value, ...)` | 语义校验               | `ValidateUtils.minValue(value, 0, false)` |
| ③    | `new XxxDp(...)`                | 编排前两步，可选归一化 | `new LongId(parsed)`                      |

校验分两分（判据：单值 vs DP 语义）：

- 【强制】 **单值校验**（null / 空 / 长度 / 范围 / 格式 / 相等）必须经 `ValidateUtils`，禁止内联 `if/throw` 与内联
  `new IllegalArgumentException`。
- 【强制】 **DP 语义不变量**（跨字段 / 集合语义 / 请求级语义）允许直接抛 `IllegalArgumentException`，消息遵循同一约定（英文、首字母小写、无句号）。

成文样例（DP 语义不变量，允许内联 IAE）：

- `UpdateMask` 构造器拒绝未展开的 `*`——集合语义不变量（`*` 是请求级指令，非可存储掩码值）；
- `UpdateMask.parse` 拒绝白名单外字段名（消息 `unknown update_mask fields`）——请求级语义，白名单属资源上下文。

> 本条的适用范围不含 `ValidateUtils` / `ParseUtils` 自身——它们是约定的实现。

❌ 工厂用 `switch default` 兜底（穷尽性交给运行期）：

```java
// ❌ 新增分支编译期不报错，运行期才落 default
default -> throw new IllegalArgumentException("unknown type");
// ✅ sealed 层级 + 模式匹配：无 default，新增子类编译期破坏分派
switch (recipient) { case SmsRecipient r -> …; case EmailRecipient r -> …; }
```

## 4. 归一化细则

```text
输入 Object
    ↓
ParseUtils.parseXxx(o)     ← 类型转换 + 宽松裁边（解析层），含 notNull
    ↓
ValidateUtils.xxx(value)   ← 语义校验
    ↓
DP 构造器
    ├── 赋值
    └── 格式归一化（不改变语义）
```

- 【强制】构造器是「守门员」不是「修理工」：只允许 **不改变值语义**的格式归一化——`Locale.ROOT` 大小写、Unicode NFC；禁止自动
  `trim`、隐式舍入、修复非法输入。
- 【强制】宽松解析（自动 trim、分隔裁边等）只允许收敛在 **解析层与入口工厂**（`ParseUtils`、`UpdateMask.parse` 的请求串分隔裁边）；
  **构造器与规范值路径零 trim**。
- 【强制】金额等需要舍入时提供 **显式 `RoundingMode` 的转换层工厂**（`fromXxx(T, mode)`），构造器零舍入。
- 【参考】`DecimalLiteralType` 构造链的 `setScale(UNNECESSARY)` 是防御断言（拒绝超精度输入），不是舍入——合规。

```java
// ✅ 显式舍入工厂（转换层）
public static WanYuan fromYuan(BigDecimal yuan, RoundingMode roundingMode) {
    ValidateUtils.notNull(yuan);
    ValidateUtils.notNull(roundingMode);
    return new WanYuan(yuan.divide(WAN, SCALE, roundingMode));
}
```

❌ 构造器里 `trim()`（改写用户输入、隐藏非法值）：

```java
// ❌ 静默修值：空白输入被改成合法值
public Xxx { value = value.trim(); ValidateUtils.hasText(value); }
// ✅ 构造器零 trim；宽松裁边只留在解析层
public Xxx { ValidateUtils.hasText(value); }
```

❌ 构造器里隐式 `setScale(2)`（吞掉精度信息、调用方无从选择模式）：

```java
// ❌ 隐式舍入
this.value = raw.setScale(2, RoundingMode.HALF_UP);
// ✅ 超精度即拒绝（UNNECESSARY 防御断言）；要舍入走显式工厂
ValidateUtils.maxScale(raw, 2);
this.value = raw.setScale(2, RoundingMode.UNNECESSARY);
```
