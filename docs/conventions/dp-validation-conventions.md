---
type: Convention
title: DP 校验与归一化规范
description: 写或测 DP 的校验/归一化（ParseUtils/ValidateUtils 职责、校验收敛、编排规则、归一化细则）时读。
tags: [ convention, dp, testing ]
status: stable
---

### 4.1 职责分层

| 层次                | 职责                                           | 是否抛 IAE     |
|---------------------|------------------------------------------------|----------------|
| `ParseUtils`        | 不可靠 `Object` → 基础类型，含 null 检查       | 是             |
| `ValidateUtils`     | 基础参数校验                                   | 是             |
| DP 构造器           | 业务校验 + 格式归一化，不做类型转换            | 否（由工具抛） |
| DP `parse`          | 字符串入口，委托 `ParseUtils` → 构造器         | 否             |
| DP `from`/`fromXxx` | 前置条件守卫（仅 NPE 防护）+ 类型转换 → 构造器 | 是（仅守卫抛） |

> **前置守卫 vs 业务校验**：`from(BigDecimal)` 中的 `notNull(value)` 防止 `value.toPlainString()` NPE，不是 DP 的业务不变量。
> 守卫只保护到达构造器之前的代码，不会出现在构造器中。构造器中的 `notNull` 则是业务校验（值不允许为 null 是领域约束）。

### 4.2 校验收敛原则

所有校验必须通过 `ValidateUtils`，禁止内联：

| 校验场景 | 正确做法 | 禁止替代 | | null 检查 | `ValidateUtils.notNull(value)` | `Objects.requireNonNull` | | blank |
`ValidateUtils.hasText(value)` | `String.isBlank() + if/throw` | | 字符串长度 | `ValidateUtils.maxLength(value, max)` |
内联 `if/throw` | | 数值范围 | `ValidateUtils.minValue` / `range` | 内联比较 | | 正则格式 |
`ValidateUtils.matches(value, pattern)` | 内联 `if(!pattern.matcher(...))` | | URI | `ValidateUtils.validUrl(uri)` |
`try/catch URI` 校验 | | Base64/Base64url | `ValidateUtils.isBase64(value)` / `isBase64Url(value)` | try/catch
解码校验 | | 字节数组长度 | `ValidateUtils.byteLength(bytes, expected)` | 内联比较 | | 类型转换 |
`ParseUtils.parseXxx(value)`（含 `parseBase64`/`parseBase64Url`） | 手动 cast / 类型推断 |

> 「正确做法」列是 `ValidateUtils` / `ParseUtils` API 的镜像（事实源在 utils 类，方法名 / 签名以源码为准）；本表的承载是「禁止替代」列——违规与允许的边界。

新增校验类型或转换方法的流程：

1. 在 `ValidateUtils` 中添加校验方法（英文消息，首字母小写，无句号）；
2. 如需类型转换，在 `ParseUtils` 中添加方法，可调用 `ValidateUtils.xxx(value, ...)`；
3. DP 构造器 / parse 方法调用对应工具方法；
4. 更新本规范 4.2 的表格。 完成判据：新方法已在 ValidateUtils/ParseUtils 落地且本表已登记对应行；DP 构造器/parse
   已调用对应工具方法；CI 绿。 判空角色（DP 类 `ValidateUtils.notNull`
   固定消息）单一权威见 [STYLEGUIDE §2.3 判空与控制流](../../STYLEGUIDE.md#23-空值校验角色)。

### 4.3 编排规则

标准流程： **ParseUtils 做转换 → ValidateUtils 做校验 → DP 构造器/工厂编排**。

| 步骤 | 工具                            | 职责                   | 示例                                      |
|------|---------------------------------|------------------------|-------------------------------------------|
| ①    | `ParseUtils.parseXxx(Object)`   | 不可靠输入 → 基础类型  | `ParseUtils.parseLong(raw)`               |
| ②    | `ValidateUtils.xxx(value, ...)` | 语义校验               | `ValidateUtils.minValue(value, 0, false)` |
| ③    | `new XxxDp(...)`                | 编排前两步，可选归一化 | `new LongId(parsed)`                      |

> DP 类禁止内联 `new IllegalArgumentException`、非法输入统一经 `ValidateUtils`
> 抛出——约定单一权威见 [STYLEGUIDE §2.3 判空与控制流](../../STYLEGUIDE.md#23-空值校验角色)。

| 违规写法（依据 [STYLEGUIDE §2.3 判空与控制流](../../STYLEGUIDE.md#23-空值校验角色)） | 正确写法                                      |
|--------------------------------------------------------------------------------------|-----------------------------------------------|
| `if (value < 0) throw new IllegalArgumentException(...)`                             | `ValidateUtils.minValue(value, 0, true)`      |
| `if (prefix == null) throw new IllegalArgumentException(...)`                        | `ValidateUtils.hasPrefix(value, prefix)`      |
| 工厂方法中 `switch` 的 `default` 分支                                                | 密封类保证穷举；或调用 `ParseUtils.parseEnum` |

例外：工具类（`ArrayTypeCache` 等）不受此限，但仅限必要场景。

### 4.4 归一化细则

```
输入 Object
    ↓
ParseUtils.parseXxx(o)     ← 类型转换，含 null 检查
    │                         内部委托 ValidateUtils.notNull
    ↓
ValidateUtils.xxx(value)   ← 语义校验
    ↓
DP 构造器
    ├── 赋值
    └── 格式归一化（不改变语义）
```

DP 构造器是"守门员"不是"修理工"。只允许 **不改变值语义**的格式归一化：

- ✅ `Locale.ROOT` 大小写、`Unicode NFC`；
- ❌ 自动 `trim`、隐式舍入、修复非法输入。

数值 DP（如金额）如需舍入，必须提供显式带 `RoundingMode` 的工厂方法，不能在构造器里隐式舍入：

```java
// ✅ 显式舍入工厂
public static WanYuan fromYuan(BigDecimal yuan, RoundingMode mode) {
    ValidateUtils.notNull(yuan);
    ValidateUtils.notNull(mode);
    var result = yuan.divide(WAN, 2, mode);
    return of(result.toPlainString());
}
// ❌ 构造器里隐式 setScale(2)
```
