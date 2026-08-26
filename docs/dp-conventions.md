---
type: Convention
title: Domain Primitive 设计规范
description: 新增或修改 Domain Primitive（DP）时读——DP 行为契约、实现形态、工厂命名、JSON/序列化与枚举 DP 完整规则。校验与归一化细节单源在 conventions/dp-validation-conventions.md。
tags: [ convention, dp ]
status: stable
---

# Domain Primitive 设计规范

目标读者：coding agent。本规范定义项目中所有 Domain Primitive（DP / 领域原语）的行为契约、实现形态、工厂命名、JSON/序列化与枚举
DP 规则。DP 调用 ValidateUtils/ParseUtils 的 API
契约单源在 [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md)。

> 关联规范：框架契约与模块治理入口见 [framework-conventions](framework-conventions.md)
> ；跨层编码与注释规范见根目录 [STYLEGUIDE](../STYLEGUIDE.md)。本文只收 DP 专属规则。

## Quick-Start Checklist

新增或修改 DP 时逐条核对：

- [ ] **不可变**：所有字段 `final`，不包裹 `byte[]`、`Date`、可变集合等类型；
- [ ] **自校验**：构造器通过 `ValidateUtils` 校验，非法值不可表示；
- [ ] **value-based identity**：`equals`/`hashCode` 基于规范值字段；
- [ ] **非空类型**：可选场景用 `Optional<DP>`，工厂方法对 `null` 零容忍；
- [ ] **不修理输入**：不自动 trim、不舍入、不修复非法输入；
- [ ] **实现形态**：简单 DP 用 record；需缓存/派生字段/私有构造器时用 `final class` + Lombok； **例外——敏感数据 DP 必须继承
  `SensitiveValue` 基类（class），以获得编译期 toString 脱敏保证，见 ADR-0032 Masked Value DP 与脱敏命名**；
- [ ] **工厂命名**：`of(T)`（@JsonCreator + 唯一公开字符串入口；参数即底层规范值）、`from(T)`／`fromXxx(T)`（跨类型转换，委托构造器）。
  `parse(String)` 仅在 `of`/构造器参数不是 `String` 时出现；
- [ ] **单一入口点**：所有业务校验集中在构造器（private 或 record 紧凑构造器）中。所有静态工厂方法只做类型转换/预处理后委托给构造器，不自含校验逻辑。`@JsonCreator` 方法也不例外——解析输入后调用构造器，不重复构造器已有的校验。
- [ ] **可比较性**：只在有自然顺序时实现 `Comparable<Self>`；
- [ ] **序列化**：默认不实现 `Serializable`，需要时显式实现；
- [ ] **JSON**：序列化与反序列化（`@JsonValue` / `@JsonCreator` / Jackson 3
  模式表）见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1——本节不重述。
- [ ] **富血方法**：自包含领域方法用 JDK 风格命名（`withXxx`、`plusXxx`、`isXxx`、`toXxx`、`next`），不调用 gateway/service；
- [ ] **缓存**：class DP、值域小、有明确性能收益时才做透明缓存，禁止依赖 `==`。
- [ ] **record 形态 DP 不得重写 `toString()`**：record 自动生成 `ClassName[value=...]`，禁止手写覆盖；
  `should_haveCorrectToString` 测试断言 record 风格（见 §1.2）。

- [ ] **class toString 格式锁死**：class 形态 DP 必须重写 `toString()` 为 record 风格 `ClassName[value=...]`（或
  `ClassName[field1=...,field2=...]` 多字段），与 record 自动生成保持一致；必须有 `should_haveCorrectToString` 测试断言（见
  §1.2）。例外：`SensitiveValue` 子类由基类 `final` toString 输出 `ClassName[masked=...]`；`SecretValue` 独立 final 类输出
  `SecretValue[***]`。

## 1. 核心行为契约

所有 DP 必须：

| 不可变 | 所有字段 `final` | record 隐式保证；class 显式 `private final`；禁止包裹可变类型 |
| 自校验 | 构造时验证业务约束 | 无效值不可表示；非法输入抛 `IllegalArgumentException` |
| value-based identity | `equals`/`hashCode` 只看规范值 | 不依赖对象身份；有派生字段时不纳入相等性 |
| class-aware identity | `equals`/`hashCode` 包含运行时类型检查 | 不同 DP 类型之间永不相等 |
| 非空类型 | DP 实例代表有效值 | 可选场景用 `Optional<DP>`；工厂对 `null` 零容忍 | | 不修理输入 |
只归一化格式，不修复非法值 | 不自动 trim、不舍入、不把非法值改成合法值 | | 可读 toString | `toString()` 输出调试字符串 |
record 风格 `ClassName[field=value]`；`SensitiveValue` 子类脱敏为 `ClassName[masked=...]`；`SecretValue` 为
`SecretValue[***]`（见 §1.2） |

`Type` 是 DP 的根标记接口，**不继承 `Serializable`**。`Identifier<T>` 继承 `Type`，保留 `T extends Comparable<T>` 约束（标识符底层值经常需要排序），但 DP 本身是否实现 `Comparable<Self>` 是可选的。


### 1.1 equals/hashCode 规范

equals/hashCode 基于规范值 + class 类型（class-aware identity），派生/缓存值不参与。

| 实现形态                            | 策略                                                                                                                                                                                                                    | 说明                                                                                                                        |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Record（单/多字段）                 | record 自动生成，所有 components 参与                                                                                                                                                                                   | 不需要手工编写；class 检查通过 `getClass()` 保证                                                                            |
| Class 单字段、无派生值              | `@EqualsAndHashCode`                                                                                                                                                                                                    | 隐式纳入唯一字段（即规范值）                                                                                                |
| Class 含派生/缓存字段               | `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` 在规范值上                                                                                                                           | 派生/缓存字段被排除                                                                                                         |
| 密封继承                            | 基类 `@EqualsAndHashCode`（比较 `value`），子类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)` 且不标注任何 `@Include`                                                                           | 仅比较基类 value + class 类型（`instanceof`），子类自身字段全部排除                                                         |
| 字面量敏感基类（SensitiveValue 族） | 基类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@Include` 在规范值上；具体子类**强制** `@EqualsAndHashCode(callSuper = true)`，canEqual 链逐层收窄即 class-aware；`SensitiveValueContractTest` 拦截子类漏标 | 子类漏标不报编译错，而是静默继承基类相等语义（跨类误等）——契约测试兜底；`toString`/`value()` 仍为手写 final（脱敏不可绕过） |
| Secret 子类                         | 引用相等（`Object.equals`/`Object.hashCode`），不暴露敏感值                                                                                                                                                             | 安全脱敏要求；不使用 Lombok 注解                                                                                            |
| Enum                                | JVM 枚举单例身份相等（`==` 等价）                                                                                                                                                                                       | 无需处理                                                                                                                    |

> **子类 `onlyExplicitlyIncluded = true` 的作用**：密封子类上该标记的作用是**排除子类自身所有字段**。配合 `callSuper = true` 委托基类比较 value，子类的派生字段（如 `userId`、`mobile`）不参与相等性计算。

### 1.2 toString 规范

所有 DP 必须提供 `toString()` 用于调试，统一采用 record 风格格式：

```text
ClassName[field1=value1, field2=value2, ...]
```

| 实现形态                     | 规则                                                             | 示例                                      |
|------------------------------|------------------------------------------------------------------|-------------------------------------------|
| Record（所有字段）           | 不重写，使用 JDK 自动生成                                        | `LongId[value=42]`                        |
| Class 单字段                 | 手动：`"ClassName[value=" + value + "]"`                         | `Version[value=42]`                       |
| Class 多字段                 | 手动：`"ClassName[field1=" + f1 + ", field2=" + f2 + "]"`        | `EmailContent[subject=Hello, body=World]` |
| 密封继承                     | 基类模板：`getClass().getSimpleName() + "[value=" + value + "]"` | `PasswordAuthAccountId[value=P:42]`       |
| SecretValue（独立 final 类） | 脱敏：`"SecretValue[***]"`，不得暴露内部值                       | `SecretValue[***]`                        |

要点（强化约束 — class 形态 DP 格式锁死规则）：

- **class 形态 DP 的 `toString()` 格式锁死**：`ClassName[value=...]`（单字段）或 `ClassName[field1=..., field2=...]`（多字段），与
  record 自动生成严格一致；不得省略、不得使用其他格式。每个 class DP 必须有 `should_haveCorrectToString`
  测试兜底，违反即失败（Locked）。
- **record 形态 DP 不得重写 `toString()`**：record 已自动生成 `ClassName[value=...]` 风格 toString（Java 语言级保证），禁止手写
  `@Override toString()` 覆盖。覆盖将偏离 record 风格基线并产生维护死角（删字段后覆盖方法不会自动跟上）。测试断言必须是
  `ClassName[value=...]`（如 `MaskedMobile[value=138****8000]`），不得断言 raw 值（Locked）。

- `SensitiveValue` 子类例外：`toString()` 由基类 `final` 化输出 `ClassName[masked=...]`（脱敏约束，§1.2 表第 4 行 +
  ADR-0032），子类不重写。
- `SecretValue` 例外：独立 final 类，`toString()` 输出 `SecretValue[***]`（§1.2 表第 5 行）。
- 调试字符串**不应**被业务逻辑依赖解析或比较；
- 密封层级统一在基类编写，子类不重写，借助 `getClass().getSimpleName()` 得到正确子类名；
- `Secret` 及其子类与敏感数据 DP（继承 `SensitiveValue` 基类，见 ADR-0032 Masked Value DP 与脱敏命名）必须脱敏输出，
  `toString()` 在基类声明为 `final` 防止子类泄露；敏感 DP 实现抽象方法 `maskedValue()`
  （无缓存纯函数，可能被多次调用），派生经 `MaskedXxx.from(this).value()`（见 §1.3）。

> **record 构造器说明**：record 典范构造器必须是 public，无法物理阻止 `new Xxx(...)`。业务代码约定优先使用工厂方法；框架/序列化需要时可直接走构造器。

### 1.3 脱敏存储值（Masked* 记录家族）

已脱敏值（masked）是**落库存储形态**，用于展示场景（前端列表、日志归档、非敏感查询 API）——一经持久化，读回时必须格式校验承重。

- **形态**：`record MaskedMobile` / `MaskedEmail` / `MaskedIdCard` / `MaskedBankCard` / `MaskedChineseName`
  `implements StringLiteralType`（非 `SensitiveValue` 子类——脱敏值不敏感，record 即满足 dp-conventions「简单 DP 用
  record」）。
- **`value()`**：返回脱敏串本身（即展示值），`@JsonValue` 继承自 `StringLiteralType`。
- **典范构造器承重格式校验**：`new MaskedXxx(rawMasked)` 紧凑构造器做正则校验（读回承重）；非法脱敏串抛
  `IllegalArgumentException`。 record 形态无 `of(...)` 工厂（见 §2.4.2，Jackson 3 RecordDeserializer 从 record components
  自动推断反序列化路径）。
- **`from(原始值 DP)`**：由原始值 DP 派生的唯一公开通道——`MaskedMobile.from(Mobile)` 内部经 `private static String maskOf(String)` 生成脱敏串后构造；原始值 DP 的 `maskedValue()` 即 `MaskedXxx.from(this).value()`。
- **`maskOf(String)`**：`private`，掩码算法的**单一事实源**，与格式正则同址于各 `MaskedXxx` 记录内，仅服务 `from(原始值 DP)`——外部不得直接调用。
- 设计依据见 ADR-0032 Masked Value DP 与脱敏命名（ADR-0030 数据保护 DP 四分类与 SensitiveValue 基类将 Masked 族委派至此）；原始值
  DP 不再缓存脱敏实例、`redacted()` 已删除。

按以下顺序决策：

```
1. 是否需要缓存 / 派生字段 / 私有构造器？
   是 → class + Lombok（见 2.3）
   否 → record（见 2.2）

2. 是否是标识符？
   是 → implements Identifier<T>
   否 → implements Type

3. 是否有自然顺序？
   是 → 额外实现 Comparable<Self>
   否 → 不实现

4. 是否需要 JDK 序列化？
   是 → 显式 implements Serializable 并加 serialVersionUID
   否 → 不实现
```

## 2.2 Record 模板

record 是默认形态。单属性字面量 DP 实现字面量家族接口（`StringLiteralType` 等，见 ADR-0028 字面量类型家族）； 标识符 DP 叠加
`Identifier<T>`。Jackson
序列化与反序列化的具体路径见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1 模式总表——本节不重述。
package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;  // 字面量家族；原语家族：LongLiteralType/IntLiteralType/BooleanLiteralType/DoubleLiteralType
import com.soda.component.domain.Identifier;  // 标识符 DP 叠加（可选，非互斥）
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

/**
 * Xxx DP — 不可变、自校验。单属性字符串字面量，实现 {@link StringLiteralType}。
 * Jackson 集成零代码：@JsonValue 继承自家族接口（ADR-0028），标量序列化与反序列化自动获得。
 */
public record Xxx(String value) implements StringLiteralType {  // 标识符：Identifier<String>, StringLiteralType

    public Xxx {
        ValidateUtils.hasText(value);                            // 或其他 ValidateUtils 方法
        // 只做不改变值语义的归一化，如 Locale.ROOT 大小写
    }

    /** 从字符串解析；null 或非法值时抛出 IllegalArgumentException。 */
    public static Xxx parse(String s) {                          // 仅当构造器参数不是 String 时需要
        return new Xxx(ParseUtils.parseXxx(s));
    }

    @Override
    public String identifier() {                                 // 仅标识符 DP 需要
        return value;
    }

    // 只在有自然顺序时实现 Comparable<Self>
    // @Override
    // public int compareTo(Xxx other) { ... }
}
```

> **非字面量 DP**
> （多属性、无标量语义）：不实现家族接口，序列化入口按 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md)
> §5.1 模式总表
> 显式声明（`@JsonValue` 必须在 public 方法上，record component / private 方法在 Jackson 3 中无效）。

用于需要缓存、派生字段或私有构造器的场景。Jackson 入口（`@JsonValue` / `@JsonCreator`）按实现形态枚举的完整对照表见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1——本节不重述。
package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.IntLiteralType;  // 字面量家族；字符串用 StringLiteralType
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;  // 仅在需要 JDK 序列化时

/**
 * Xxx DP — 不可变、自校验、带缓存（可选）。单属性 int 字面量，实现 {@link IntLiteralType}。
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Xxx implements IntLiteralType, Serializable {  // Serializable 按需

    @Serial
    private static final long serialVersionUID = 1L;      // 仅在实现 Serializable 时需要

    // ===== 可选：缓存 =====
    private static final int CACHE_SIZE = 100;
    private static final Xxx[] CACHE = new Xxx[CACHE_SIZE];
    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new Xxx(i);
        }
    }
    public static final Xxx INITIAL = CACHE[0];
    // ======================

    private final int value;

    /** 规范值访问器 — @JsonValue 继承自 {@link IntLiteralType}（ADR-0028）。 */
    public int value() {
        return value;
    }

    /** 构造器只含校验和格式归一化，不修理输入。 */
    private Xxx(int value) {                                 // 有缓存时私有；无缓存时 public（零注解，推断可用）
        ValidateUtils.minValue(0, true, value);
        this.value = value;
    }

    /** 主入口。有缓存时 @JsonCreator 挂这里（private 构造器不可见，必须显式）。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Xxx of(int value) {
        if (0 <= value && value < CACHE_SIZE) {
            return CACHE[value];
        }
        return new Xxx(value);
    }

    /** 字符串解析入口。 */
    public static Xxx parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    // 只在有自然顺序时实现 Comparable<Self>
    // @Override
    // public int compareTo(Xxx other) { ... }

    @Override
    public String toString() {
        return "Xxx[value=" + value + "]";
    }
}

## 2.4 Class 与 Record 形态构造规则

形态选 record 还是 class + Lombok，**只决定构造器可见性**与主入口形态；`@JsonValue` / `@JsonCreator` 的位置按实现形态枚举的完整对照见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1——本节不重述。

### 2.4.1 class 形态 DP

- **必须**声明 `private` 构造器，**不得**暴露 public 构造器。
- **必须**提供 `static Xxx of(T)` 静态工厂方法作为唯一公开构造入口。
- 所有业务校验集中在 `private` 构造器中；`of(T)` 只做类型转换/预处理后委托构造器，**不自含校验逻辑**。

### 2.4.2 record 形态 DP

- 直接使用 public 典范构造器，**不提供** `static of(...)` 工厂方法。
- 跨类型转换（如 `EpochMilli.from(Instant)`）使用 `from(...)` 命名，见 §3.1「两层入口语义」——`from` 是转换层，非主入口 `of`。

### 2.4.3 设计理由

| 维度 | class 形态 | record 形态 |
|---|---|---|
| 构造器可见性 | `private`（封装缓存/单例/归一化） | `public`（JDK 强制，典范构造器即公开入口） |
| 主入口 | `of(T)` 承载序列化入口、中介缓存/归一化 | 典范构造器即主入口，零注解自动承载反序列化 |
| `of(...)` 必要性 | 必须——构造器不可见，必须显式暴露入口 | 多余——典范构造器已是公开入口 |
| `from(...)` 角色 | 跨类型转换，委托 `of` 或构造器 | 跨类型转换，委托典范构造器 |
| 单一构造路径 | `of` 强制中介，防止绕过缓存/校验 | 典范构造器即单一路径；业务代码约定优先用 `from` / `parse` 语义入口 |

> **SecretValue 例外说明**：`SecretValue` 为 `final class` 但保留 public 构造器并显式关闭反序列化入口——这是为了防止凭证意外被反序列化写入日志/存储的**安全姿态**，不属于上述「class 形态必须 private 构造器」规则的违例。`@JsonCreator` 关闭位置见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1。


## 3. 工厂方法命名

对齐 `LocalDate` 三分语义：

| 方法 | 语义 | `@JsonCreator` | 示例 |
|---|---|---|---|
| `of(...)` | 参数即底层值 | class/enum 挂这里 | `Version.of(1)`、`XxxEnum.of("A")` |
| `from(...)` | 参数 ≠ 底层值，跨类型转换 | ❌ | `PasswordAuthAccountId.from(UserId)` |
| `parse(String)` | 字符串输入需解析/转换 | ❌ | `UserId.parse("123")` |

不变量：

- 每个 DP 只有一个主入口（`of` 或紧凑构造器）；
- `from(...)` 仅在跨类型转换时存在，使用 overload 同名即可；特例需后缀区分（如 `WanYuan.fromYuan(BigDecimal)`）；
- `parse(String)` 仅当 `of`/构造器参数不是 `String` 时才需要；
- `valueOf(Object)` 已从 DP 上移除（枚举编译器生成的 `valueOf(String)` 除外）。

### 3.1 两层入口语义

工厂方法按输入类型分两个语义层，各层之间**不可逆向委托**：

| 层 | 方法 | 输入 | 约束 | @JsonCreator |
|---|---|---|---|---|
| 主入口 | `of(T)` | 规范值的精确表示 | 必须直接调用构造器，不可委托其他工厂。T 一般是 String（@JsonCreator 需要），也可以是原生类型 | ✅ |
| 转换层 | `from(T)` / `fromXxx(T)` | 可转换为规范值的其他类型 | 类型转换 + 委托构造器。可保留 `notNull` 等前置条件守卫（避免转换操作 NPE），但**不自含业务校验** | ❌ |

关键约束：

1. **`of(T)` 是唯一的公开字符串入口**，也是 `@JsonCreator` 方法。必须直接调用构造器，不可委托其他工厂。Jackson 反序列化路径必须是单步的。
        2.**`from`
的前置守卫 vs
业务校验**——边界与范例的完整解释（`

from(BigDecimal)`中 `notNull`与构造器中 `notNull`的区分）见 [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md) §4.1，本节不重述。
3. **`from(BigDecimal, RoundingMode)` 的舍入属于类型适配**：调用方提供不兼容输入时预处理，不属于 DP 不变量修正。

> `of(String)` 直接承担字符串入口和 `@JsonCreator` 双重职责，不拆分单独的 `parse(String)` 层——String → DP
> 的路径足够简单，拆分只会增加一个转发层。
        `@JsonCreator`
位置与 Jackson 3行为细节（按实现形态枚举的完整对照表）见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.1。

## 3.2 常量与便捷工厂

JDK 包装类常见 `MIN`/`MAX`/`ZERO`/`EPOCH`/`random()`/`now()` 等常量或便捷工厂。DP 中按需暴露，**不强制**：

- 数量/版本/金额型 DP（如 `Version`、`WanYuan`）可暴露 `ZERO`/`ONE`/`MIN`/`MAX`，如果有领域意义；
- 标识符 DP 通常不需要 `UserId.ZERO` 这类常量；
- 时间型 DP 可按 `LocalDate` 惯例暴露 `now()`、`MIN`/`MAX`；
- 随机生成型 DP（如 `Uuid`）保留 `random()` 或 `generate(...)`；
- “无值”用 `Optional<DP>` 表达，不用 `Xxx.EMPTY` 单例（除非 EMPTY 本身是合法领域值）。

## 4. 校验与归一化

>
DP 校验与归一化规范（校验职责分层、校验收敛、编排规则、归一化细则）见 [conventions/dp-validation-conventions.md](conventions/dp-validation-conventions.md)。

        ### 4.2可变类型禁令
DP 字段必须不可变：

| 禁止 | 替代 |
|---|---|
| `byte[]` | 十六进制字符串 / Base64 `String` / 不可变 `ByteString` |
| `Date` / `Calendar` | `Instant` / `LocalDate` / `ZonedDateTime` |
| `ArrayList` / `HashSet` / `HashMap` | `List.of` / `Set.copyOf` / `Map.copyOf` |
| 可变领域对象 | 提取为 DP 字段或设计成不可变 |

## 5. JSON 与序列化

DP 的
JSON 与序列化（Jackson 3模式总表、
BigDecimal DP
模式、典型形态示例、`Serializable`约束）见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md)——本节不重述，仅指。Quick-
Start 涉及
Jackson 形态的条目在原 §5。

## 6. 富血值对象方法命名

鼓励 DP 承载自包含、无副作用、不调用外部 gateway/service 的领域方法：

| 语义 | 命名 | 示例 |
|---|---|---|
| 修改字段返回新实例 | `withXxx(...)` | `withYear(int)` |
| 加减/偏移 | `plusXxx` / `minusXxx` | `plusDays(long)` |
| 布尔查询 | `isXxx` / `hasXxx` | `isAfter(Instant)` |
| 转换 | `toXxx()` | `toLongId()` |
| 序列步进 | `next()` / `previous()` | `version.next()` |

贫血包装器（如 `Avatar`、纯标识符 `UserId`）保持简洁即可。

## 7. 缓存

缓存是透明性能优化，不是行为契约：

- 仅 class DP 可做（record 构造器 public，无法强制走缓存入口）；
- 值域小且可预测（如版本号 `[0, 99]`）；
- 有明确性能收益；
- 不可观测：调用方仍用 `equals`/`hashCode`/`compareTo`，禁止依赖 `==`。

## 8. SPI 配置化常量

数值常量（长度上限、范围边界等）可通过 `TypeConfigProvider` SPI 自定义：

```java
public interface TypeConfigProvider {
    default int versionCacheHigh() { return 99; }
}
```

DP 内引用：

```java
private static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());
```

注意：

- SPI 默认值不能低于原始设计下限，用 `Math.max` 保护；
- 正则 Pattern 不适合 SPI（编译成本高）。

## 9. 枚举 DP

```java
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Xxx implements EnumType {

    A("Description A"),
    B("Description B");

    private final String desc;

    // Jackson 3 默认用 name() 序列化枚举，无需 @JsonValue。
    // Jackson 3 忽略 @JsonValue private，如需显式声明须为 public 方法。

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Xxx of(String name) {
        return ParseUtils.parseEnum(Xxx.class, name);
    }
}
```

规则：

- 序列化：Jackson 3 默认使用 `name()` 作为枚举 JSON 值，无需额外注解；`@JsonValue private` 被忽略，若需显式声明应使用 public 方法；
- 反序列化：`@JsonCreator of(String)` 委托 `ParseUtils.parseEnum()`；
- 访问器：`desc()`；
- 包：所在模块的 `enums` 子包（如 `com.soda.user.domain.enums`）。

限制：

- 用 `of(String)` 作为公开反序列化入口，编译器生成的 `valueOf(String)` 仅内部使用；
- 枚举常量名即持久化值，改名需 migration。

## 10. 创建新 DP 的精简流程

1. **判断形态**：
   - 简单 → record（见 2.2）；
   - 需缓存/派生字段/私有构造器 → `final class` + Lombok（见 2.3）；
   - BigDecimal DP → `final class` + Lombok，规范值用 `String`，BigDecimal
     作派生缓存（见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.2）；
   - 密封继承层次 → 基类 `abstract sealed class` + 子类 `final class`
     （见 [conventions/dp-json-conventions.md](conventions/dp-json-conventions.md) §5.3 密封继承示例）；
2. 选择接口：单属性字面量 → 字面量家族（`StringLiteralType`/`LongLiteralType`/`IntLiteralType`/`BooleanLiteralType`/`DoubleLiteralType`，枚举用 `EnumType`）；标识符叠加 `Identifier<T>`；多属性/无标量语义 → `Type`；
3. 字段类型必须不可变；
4. 紧凑构造器/私有构造器内用 `ValidateUtils` 校验，只做格式归一化；
5. 按对应模板实现 `value()`（字面量家族：注解继承自家族接口，零 Jackson 代码；多属性：`@JsonCreator(PROPERTIES)` + `@JsonProperty`）；带构造逻辑（缓存/单例/解析）的 class 保留 `@JsonCreator(DELEGATING)` 在 `of(T)` 上；补 `parse`、`toString` 等；
6. 需要自然顺序时实现 `Comparable<Self>`；
7. 需要 JDK 序列化时显式实现 `Serializable`；
8. 多字段 DP 必须用 `@JsonProperty` + `@JsonCreator(mode = PROPERTIES)`；
9. 运行 `:soda-components:soda-component-domain-types:build` 验证。
10. 在 conventions/framework-type-contracts.md「DP 类型清单表」登记一行（缓存设施类不入表）——该表是 types 包全部 DP 的覆盖清单。
11. **class 形态 DP**：补充 `should_haveCorrectToString` 测试断言 `ClassName[value=...]` 格式（§1.2 锁定规则）。
12. **record 形态 DP**：补充 `should_haveCorrectToString` 测试断言 record 自动生成的 `ClassName[value=...]` 格式（§1.2
    锁定规则）——不得手写 `@Override toString()` 覆盖。

**完成判据**：下列条件全部满足方算完工，任一不满足即未完工——

- `:soda-components:soda-component-domain-types:build` 通过（无编译/测试失败）；
- 文首 Quick-Start Checklist 逐条勾选满足；
- conventions/framework-type-contracts.md「DP 类型清单表」新增一行，且 types 包内无遗漏 DP（缓存设施类 `ArrayTypeCache`/
  `MapTypeCache` 不入表）。

## 附录 A：`compareTo` 编写规范

只在有自然顺序时实现（标识符、版本号、长度等）。三种字段形态的写法：

```java
// 对象类型单字段（如 Uuid）
@Override
public int compareTo(Uuid other) {
    return this.value.compareTo(other.value);
}

// 基本类型单字段（如 Version）
@Override
public int compareTo(Version other) {
    return Integer.compare(this.value, other.value);
}

// 多字段（多字段 DP 通常无自然顺序；如某复合 DP 确有顺序，则逐字段比较）
@Override
public int compareTo(Xxx other) {
    var cmp = this.first.compareTo(other.first);
    if (cmp != 0) {
        return cmp;
    }
    return this.second.compareTo(other.second);
}
```

## 附录 B：对比一致性示例

| DP | 实现 | `Comparable` | 缓存 | `Serializable` |
|---|---|---|---|---|
| `LongId` | record | ✅ | 无 | 不显式 |
| `Uuid` | record | ✅ | 无 | 不显式 |
| `Fen` | record | ✅ | 无 | 不显式 |
| `Mobile` / `Email` / `WanYuan` | record / class / class | ❌（无领域顺序） | 无（WanYuan 缓存 BigDecimal 派生值） | 不显式 |
| `Version` | class | ✅ | `[0, 99]` | ✅ 显式 |
| `SoftwareVersion` | class | ✅ | 无 | 不显式 |
