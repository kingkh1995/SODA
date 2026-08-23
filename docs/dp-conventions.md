# Domain Primitive 设计规范

目标读者：coding agent。本规范定义项目中所有 Domain Primitive（DP / 领域原语）的行为契约、实现形态、工厂命名、校验归一化、JSON/序列化及枚举 DP。

## Quick-Start Checklist

新增或修改 DP 时逐条核对：

- [ ] **不可变**：所有字段 `final`，不包裹 `byte[]`、`Date`、可变集合等类型；
- [ ] **自校验**：构造器通过 `ValidateUtils` 校验，非法值不可表示；
- [ ] **value-based identity**：`equals`/`hashCode` 基于规范值字段；
- [ ] **非空类型**：可选场景用 `Optional<DP>`，工厂方法对 `null` 零容忍；
- [ ] **不修理输入**：不自动 trim、不舍入、不修复非法输入；
- [ ] **实现形态**：简单 DP 用 record；需缓存/派生字段/私有构造器时用 `final class` + Lombok；**例外——敏感数据 DP 必须继承 `SensitiveValue` 基类（class），以获得编译期 toString 脱敏保证，见 ADR-0032**；
*15:- [ ] **工厂命名**：`of(T)`（@JsonCreator + 唯一公开字符串入口；参数即底层规范值）、`from(T)`／`fromXxx(T)`（跨类型转换，委托构造器）。`parse(String)` 仅在 `of`/构造器参数不是 `String` 时出现；
- [ ] **单一入口点**：所有业务校验集中在构造器（private 或 record 紧凑构造器）中。所有静态工厂方法只做类型转换/预处理后委托给构造器，不自含校验逻辑。`@JsonCreator` 方法也不例外——解析输入后调用构造器，不重复构造器已有的校验。
- [ ] **可比较性**：只在有自然顺序时实现 `Comparable<Self>`；
- [ ] **序列化**：默认不实现 `Serializable`，需要时显式实现；
- [ ] **JSON（字面量家族）**：单属性字面量 DP 实现字面量家族接口（`StringLiteralType`/`LongLiteralType`/`IntLiteralType`/`BooleanLiteralType`/`DoubleLiteralType`，见 ADR-0028）——`@JsonValue` 继承自家族接口的 `value()`，标量序列化与反序列化自动获得（Jackson 3.1.4 实证双向）：record 与单 public 构造器 class **零 Jackson 代码**；private 构造器 + 工厂（缓存/单例/解析）保留 `@JsonCreator(mode = DELEGATING)` 在 `of(T)` 上（构造入口不可继承）；枚举经 `EnumType` 继承（`value()` = `name()`）；多属性 DP 用 `@JsonCreator(PROPERTIES)` + `@JsonProperty`（record 可省略）；sealed class 无需 `@JsonSubTypes`（Jackson 3 从 `permits` 子句自动发现）；
- [ ] **富血方法**：自包含领域方法用 JDK 风格命名（`withXxx`、`plusXxx`、`isXxx`、`toXxx`、`next`），不调用 gateway/service；
- [ ] **缓存**：class DP、值域小、有明确性能收益时才做透明缓存，禁止依赖 `==`。

## 1. 核心行为契约

所有 DP 必须：

| 不可变 | 所有字段 `final` | record 隐式保证；class 显式 `private final`；禁止包裹可变类型 |
| 自校验 | 构造时验证业务约束 | 无效值不可表示；非法输入抛 `IllegalArgumentException` |
| value-based identity | `equals`/`hashCode` 只看规范值 | 不依赖对象身份；有派生字段时不纳入相等性 |
| class-aware identity | `equals`/`hashCode` 包含运行时类型检查 | 不同 DP 类型之间永不相等 |
| 非空类型 | DP 实例代表有效值 | 可选场景用 `Optional<DP>`；工厂对 `null` 零容忍 |
| 不修理输入 | 只归一化格式，不修复非法值 | 不自动 trim、不舍入、不把非法值改成合法值 |
| 可读 toString | `toString()` 输出调试字符串 | record 风格 `ClassName[field=value]`；敏感值脱敏为 `SecretValue[***]` / `ClassName[masked=…]` |

`Type` 是 DP 的根标记接口，**不继承 `Serializable`**。`Identifier<T>` 继承 `Type`，保留 `T extends Comparable<T>` 约束（标识符底层值经常需要排序），但 DP 本身是否实现 `Comparable<Self>` 是可选的。


### 1.1 equals/hashCode 规范

equals/hashCode 基于规范值 + class 类型（class-aware identity），派生/缓存值不参与。

| 实现形态 | 策略 | 说明 |
|---|---|---|
| Record（单/多字段） | record 自动生成，所有 components 参与 | 不需要手工编写；class 检查通过 `getClass()` 保证 |
| Class 单字段、无派生值 | `@EqualsAndHashCode` | 隐式纳入唯一字段（即规范值） |
| Class 含派生/缓存字段 | `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` 在规范值上 | 派生/缓存字段被排除 |
| 密封继承 | 基类 `@EqualsAndHashCode`（比较 `value`），子类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)` 且不标注任何 `@Include` | 仅比较基类 value + class 类型（`instanceof`），子类自身字段全部排除 |
| SecretValue（独立 final 类） | 引用相等（`Object.equals`/`Object.hashCode`），不暴露敏感值 | 安全脱敏要求；不使用 Lombok 注解 |
| Enum | JVM 枚举单例身份相等（`==` 等价） | 无需处理 |

> **子类 `onlyExplicitlyIncluded = true` 的作用**：密封子类上该标记的作用是**排除子类自身所有字段**。配合 `callSuper = true` 委托基类比较 value，子类的派生字段（如 `userId`、`mobile`）不参与相等性计算。

### 1.2 toString 规范

所有 DP 必须提供 `toString()` 用于调试，统一采用 record 风格格式：

```text
ClassName[field1=value1, field2=value2, ...]
```

| 实现形态 | 规则 | 示例 |
|---|---|---|
| Record（所有字段） | 不重写，使用 JDK 自动生成 | `Mobile[value=13800138000]` |
| Class 单字段 | 手动：`"ClassName[value=" + value + "]"` | `Version[value=42]` |
| Class 多字段 | 手动：`"ClassName[field1=" + f1 + ", field2=" + f2 + "]"` | `EmailContent[subject=Hello, body=World]` |
| 密封继承 | 基类模板：`getClass().getSimpleName() + "[value=" + value + "]"` | `PasswordAuthAccountId[value=P:42]` |
| SecretValue（独立 final 类） | 脱敏：`"SecretValue[***]"`，不得暴露内部值 | `SecretValue[***]` |

要点：

- 调试字符串**不应**被业务逻辑依赖解析或比较；
- class DP 的 `toString()` 必须手工编写与 record 一致的格式，不可省略；
- 密封层级统一在基类编写，子类不重写，借助 `getClass().getSimpleName()` 得到正确子类名；
- 敏感数据 DP（继承 `SensitiveValue` 基类，见 ADR-0032/0033）必须脱敏输出，`toString()` 在基类声明为
  `final` 防止子类泄露；脱敏派生经抽象 `maskedValue()` → `MaskedXxx.from(this).value()`（见 §1.3），
  每次计算、不缓存（缓存纯函数是纯开销，见 ADR-0032）。

> **record 构造器说明**：record 典范构造器必须是 public，无法物理阻止 `new Xxx(...)`。业务代码约定优先使用工厂方法；框架/序列化需要时可直接走构造器。

### 1.3 脱敏存储值（Masked* 记录家族）

已脱敏值（masked）是**落库存储形态**，用于展示场景（前端列表、日志归档、非敏感查询 API）——一经持久化，读回时必须格式校验承重。

- **形态**：`record MaskedMobile` / `MaskedEmail` / `MaskedIdCard` / `MaskedBankCard` / `MaskedRealName` `implements StringLiteralType`（非 `SensitiveValue` 子类——脱敏值不敏感，record 即满足 dp-conventions「简单 DP 用 record」）。
- **`value()`**：返回脱敏串本身（即展示值），`@JsonValue` 继承自 `StringLiteralType`。
- **`of(String)`**：`@JsonCreator` 入口，**格式正则校验脱敏串**（读回承重）；非法脱敏串抛 `IllegalArgumentException`。
- **`from(原始值 DP)`**：由原始值 DP 派生的唯一公开通道——`MaskedMobile.from(Mobile)` 内部经 `private static String maskOf(String)` 生成脱敏串后构造；原始值 DP 的 `maskedValue()` 即 `MaskedXxx.from(this).value()`。
- **`maskOf(String)`**：`private`，掩码算法的**单一事实源**，与格式正则同址于各 `MaskedXxx` 记录内，仅服务 `from(原始值 DP)`——外部不得直接调用。
- 设计依据见 ADR-0032（取代 ADR-0030 §4）；原始值 DP 不再缓存脱敏实例、`redacted()` 已删除。


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

record 是默认形态。单属性字面量 DP 实现字面量家族接口（`StringLiteralType` 等，见 ADR-0028）；
标识符 DP 叠加 `Identifier<T>`。字面量 DP **零 Jackson 代码**——`@JsonValue` 继承自家族接口，
标量序列化与反序列化自动获得（Jackson 3.1.4 实证双向，`{}`/对象形式仍被拒）。

```java
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

> **非字面量 DP**（多属性、无标量语义）：不实现家族接口，序列化入口按 §5 模式总表
> 显式声明（`@JsonValue` 必须在 public 方法上，record component / private 方法在 Jackson 3 中无效）。

## 2.3 Class + Lombok 模板

用于需要缓存、派生字段或私有构造器的场景。字面量 DP 的 `@JsonValue` 继承自家族接口（零注解）；
`@JsonCreator` 位置取决于构造器形态：无缓存时单 public 构造器零注解（Jackson 3.1.4 推断实证，
见 ADR-0028）；有缓存/单例/解析逻辑时 private 构造器 + 静态 `of(T)` 挂显式 `@JsonCreator`
（private 构造器 Jackson 不可见，构造入口不可继承——结构性必要）。

```java
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
…
}
```
    @Override
    public String toString() {
        return "Xxx[value=" + value + "]";
    }
}
```

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
2. **`from` 的前置守卫不是业务校验**。`from(BigDecimal)` 中的 `notNull(value)` 保护 `value.toPlainString()` 的 NPE，不是百分比范围/scale 校验——后者是构造器的唯一职责。守卫不会出现在构造器中。
3. **`from(BigDecimal, RoundingMode)` 的舍入属于类型适配**：调用方提供不兼容输入时预处理，不属于 DP 不变量修正。

> 旧版曾使用 `parse(String)` 作为单独的字符串解析入口。当前约定为 `of(String)` 直接承担字符串入口和 `@JsonCreator` 双重职责，不拆分 `parse` 层。理由：String → DP 的路径足够简单，拆分只会增加一个转发层。

`@JsonCreator` 位置（Jackson 3 说明列仅列出与 Jackson 2 有差异的行为）：

| 实现 | `@JsonCreator` 位置 | Jackson 3 说明 |
|---|---|---|
| record 单字段（字面量家族） | — | 不需要——继承 `@JsonValue` 即获标量反序列化（ADR-0028，Jackson 3.1.4 实证） |
| record 多字段 | `@JsonCreator(mode = PROPERTIES)` 在紧凑构造器上，或省略（Jackson 3 RecordDeserializer 自动推断） | 可选；省略时 Jackson 3 自动匹配典范构造器与 record component 名 |
| class 无缓存（字面量家族） | — | 不需要——单 public 构造器 + 继承 `@JsonValue`（Jackson 3.1.4 推断实证） |
| class 有缓存 | 静态 `of(T)` `@JsonCreator(mode = DELEGATING)` | 必须；private 构造器 Jackson 不可见 |
| class 多字段 | 静态 `of(...)` / 构造器 `@JsonCreator(mode = PROPERTIES)` | 无变化 |
| enum | 静态 `of(String)` `@JsonCreator(mode = DELEGATING)` | 无变化 |
| 密封基类（路由工厂） | 基类静态 `of(String)` `@JsonCreator(mode = DELEGATING)`，按前缀路由 | 无变化 |
| BigDecimal DP (class) | 静态 `of(String)` `@JsonCreator(mode = DELEGATING)`，内部解析 BigDecimal | 无变化 |

## 3.2 常量与便捷工厂

JDK 包装类常见 `MIN`/`MAX`/`ZERO`/`EPOCH`/`random()`/`now()` 等常量或便捷工厂。DP 中按需暴露，**不强制**：

- 数量/版本/金额型 DP（如 `Version`、`WanYuan`）可暴露 `ZERO`/`ONE`/`MIN`/`MAX`，如果有领域意义；
- 标识符 DP 通常不需要 `UserId.ZERO` 这类常量；
- 时间型 DP 可按 `LocalDate` 惯例暴露 `now()`、`MIN`/`MAX`；
- 随机生成型 DP（如 `Uuid`）保留 `random()` 或 `generate(...)`；
- “无值”用 `Optional<DP>` 表达，不用 `Xxx.EMPTY` 单例（除非 EMPTY 本身是合法领域值）。

## 4. 校验与归一化

### 4.1 职责分层

| 层次 | 职责 | 是否抛 IAE |
|---|---|---|
| `ParseUtils` | 不可靠 `Object` → 基础类型，含 null 检查 | 是 |
| `ValidateUtils` | 基础参数校验 | 是 |
| DP 构造器 | 业务校验 + 格式归一化，不做类型转换 | 否（由工具抛） |
| DP `parse` | 字符串入口，委托 `ParseUtils` → 构造器 | 否 |
| DP `from`/`fromXxx` | 前置条件守卫（仅 NPE 防护）+ 类型转换 → 构造器 | 是（仅守卫抛） |

> **前置守卫 vs 业务校验**：`from(BigDecimal)` 中的 `notNull(value)` 防止 `value.toPlainString()` NPE，不是 DP 的业务不变量。
> 守卫只保护到达构造器之前的代码，不会出现在构造器中。构造器中的 `notNull` 则是业务校验（值不允许为 null 是领域约束）。

### 4.2 校验收敛原则

所有校验必须通过 `ValidateUtils`，禁止内联：

  | 校验场景 | 正确做法 | 禁止替代 |
  | null 检查 | `ValidateUtils.notNull(value)` | `Objects.requireNonNull` |
  | blank | `ValidateUtils.hasText(value)` | `String.isBlank() + if/throw` |
  | 字符串长度 | `ValidateUtils.maxLength(value, max)` | 内联 `if/throw` |
  | 数值范围 | `ValidateUtils.minValue` / `range` | 内联比较 |
  | 正则格式 | `ValidateUtils.matches(value, pattern)` | 内联 `if(!pattern.matcher(...))` |
  | URI | `ValidateUtils.validUrl(uri)` | `try/catch URI` 校验 |
  | Base64/Base64url | `ValidateUtils.isBase64(value)` / `isBase64Url(value)` | try/catch 解码校验 |
  | 字节数组长度 | `ValidateUtils.byteLength(bytes, expected)` | 内联比较 |
  | 类型转换 | `ParseUtils.parseXxx(value)`（含 `parseBase64`/`parseBase64Url`） | 手动 cast / 类型推断 |

新增校验类型或转换方法的流程：

1. 在 `ValidateUtils` 中添加校验方法（英文消息，首字母小写，无句号）；
2. 如需类型转换，在 `ParseUtils` 中添加方法，可调用 `ValidateUtils.xxx(value, ...)`；
3. DP 构造器 / parse 方法调用对应工具方法；
4. 更新本规范 4.2 的表格。

### 4.3 编排规则

标准流程：**ParseUtils 做转换 → ValidateUtils 做校验 → DP 构造器/工厂编排**。

| 步骤 | 工具 | 职责 | 示例 |
|---|---|---|---|
| ① | `ParseUtils.parseXxx(Object)` | 不可靠输入 → 基础类型 | `ParseUtils.parseLong(raw)` |
| ② | `ValidateUtils.xxx(value, ...)` | 语义校验 | `ValidateUtils.minValue(value, 0, false)` |
| ③ | `new XxxDp(...)` | 编排前两步，可选归一化 | `new LongId(parsed)` |

**禁区**：DP 类中不得出现 `new IllegalArgumentException(...)`。所有非法输入必须通过工具类中的 `throw` 抛出。

| 违规写法 | 正确写法 |
|---|---|
| `if (value < 0) throw new IllegalArgumentException(...)` | `ValidateUtils.minValue(value, 0, true)` |
| `if (prefix == null) throw new IllegalArgumentException(...)` | `ValidateUtils.hasPrefix(value, prefix)` |
| 工厂方法中 `switch` 的 `default` 分支 | 密封类保证穷举；或调用 `ParseUtils.parseEnum` |

例外：工具类（`ArrayTypeCache` 等）不受此限，但仅限必要场景。

### 4.4 映射关系

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

DP 构造器是"守门员"不是"修理工"。只允许**不改变值语义**的格式归一化：

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

### 4.5 可变类型禁令
DP 字段必须不可变：

| 禁止 | 替代 |
|---|---|
| `byte[]` | 十六进制字符串 / Base64 `String` / 不可变 `ByteString` |
| `Date` / `Calendar` | `Instant` / `LocalDate` / `ZonedDateTime` |
| `ArrayList` / `HashSet` / `HashMap` | `List.of` / `Set.copyOf` / `Map.copyOf` |
| 可变领域对象 | 提取为 DP 字段或设计成不可变 |

## 5. JSON 与序列化

字面量家族 DP（`StringLiteralType` 等，见 ADR-0028）的序列化入口继承自家族接口（`@JsonValue`）；反序列化：record 与单 public 构造器 class 零注解（Jackson 3.1.4 实证），带构造逻辑（缓存/单例/解析）的 class 显式 `@JsonCreator`。多属性 DP 独立声明序列化和反序列化入口（`@JsonCreator(PROPERTIES)` + `@JsonProperty`）。不依赖全局 Jackson 配置或隐式推断。

### 5.1 模式总表

| 实现形态 | `@JsonValue`（序列化） | `@JsonCreator`（反序列化） | Jackson 3 说明 |
|---|---|---|---|
| Record + 单字段（字面量家族） | 继承自家族接口（record 隐式访问器即可） | — | 零 Jackson 代码（ADR-0028；`@JsonValue` 不能在 record component 上，家族接口声明于抽象方法） |
| Record + 多字段 | — | `(mode = PROPERTIES)` 紧凑构造器或省略 | Jackson 3 RecordDeserializer 能从典范构造器推断，`@JsonProperty` 冗余 |
| Class + 单字段无缓存（字面量家族） | 继承自家族接口 | — | 零 Jackson 代码（单 public 构造器推断实证） |
| Class + 单字段有缓存（字面量家族） | 继承自家族接口 | `(mode = DELEGATING)` 静态 `of(T)` | private 构造器 Jackson 不可见，creator 必须显式 |
| Class + 多字段 | — | `(mode = PROPERTIES)` 静态 `of(...)` | 无变化 |
| Enum | 无（Jackson 原生输出 `name()` 短名） | `(mode = DELEGATING)` 静态 `of(String)` | 序列化输出为 `name()`（ADR-0005）；`@JsonCreator` 显式保留 |
| 密封继承基类（字面量家族） | 继承自家族接口（基类声明，子类复用） | 基类路由 `(mode = DELEGATING)` 静态 `of(String)`；子类也可各自声明 | sealed 类无需 `@JsonSubTypes`，Jackson 3 从 `permits` 子句自动发现 |

| Java 类型 | JSON 类型 | 说明 |
|---|---|---|
| `long` / `int` | 数字 | 标量 |
| `String` | 字符串 | 标量 |
| `boolean` | 布尔 | 标量 |
| `BigDecimal` | 字符串 | 经 `toPlainString()` 输出，规范值为 String，BigDecimal 作为派生缓存；小数 DP `extends DecimalLiteralType`（ADR-0031） |
| `Instant` | long（经 `EpochMilli` DP）/ ISO-8601 字符串（裸） | Jackson 3 默认 ISO-8601 字符串（`WRITE_DATES_AS_TIMESTAMPS` 默认 false）；`EpochMilli` DP 用 `@JsonValue long value()`（毫秒）覆盖（ADR-0031） |
| `Duration` | ISO-8601 字符串 | Jackson 3 默认 ISO-8601（如 `PT5M`） |
| enum | 字符串 | Jackson 3 默认用 `name()` 序列化，`@JsonValue` 可选但必须在 public 方法上 |

> **Jackson 3 JSR-310 说明**：Jackson 3 内建 `java.time` 序列化支持，无需注册 `JavaTimeModule`。`Instant` 默认序列化为 **ISO-8601 字符串**（`WRITE_DATES_AS_TIMESTAMPS` 默认 false；Jackson 2 为 true）；即便启用 timestamps，默认也是 epoch 秒 + 小数纳秒，非毫秒。`Duration` 默认 ISO-8601 字符串（如 `PT5M`）。绝对时间点领域字段用 `EpochMilli` DP（`@JsonValue long` 毫秒）显式覆盖，不受全局 feature 影响（ADR-0031）。
> 与 Jackson 2 中依赖 `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` 的配置不兼容。

### 5.2 BigDecimal DP 模式

BigDecimal 类型的 DP 使用 `class + Lombok`（非 record），规范值为 `String`（`toPlainString()` 格式），
BigDecimal 作为派生缓存字段，不参与 `equals`/`hashCode`/`@JsonValue`。

设计要点：

- **规范值 = String**：`@JsonValue` 挂在一个 `String` 返回方法上，输出 `toPlainString()`。
  禁止科学计数法，禁止 `@JsonFormat(shape = STRING)`、自定义序列化器、子 DP 等方式。
- **BigDecimal 为派生缓存**：构造时解析 String 得到 BigDecimal 并缓存，不参与 `@EqualsAndHashCode.Include`。
- **`@JsonCreator` 参数为 String**：反序列化入口 `of(String)` 接收 JSON 字符串，内部调用 `ParseUtils.parseBigDecimal()`，
  不经 Jackson 类型强制转换，完全不受 ObjectMapper 配置影响。
- **构造器做校验 + 归一化**：校验 scale、精度等，用 `setScale` 归一化后再取 `toPlainString()` 作为规范值。

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class WanYuan implements Type {

    @EqualsAndHashCode.Include
    private final String value;                  // toPlainString() 格式

    @Getter    // fluent, 业务代码取值用
    private final BigDecimal bigDecimalValue;    // 派生缓存，不参与 equals/hashCode/@JsonValue

    private WanYuan(String value, BigDecimal bigDecimalValue) {
        this.value = value;
        this.bigDecimalValue = bigDecimalValue;
    }

    /** 序列化出口。显式方法级别，返回 toPlainString()。 */
    @JsonValue
    public String value() {
        return value;
    }

    /** 反序列化入口。参数为 JSON String，内部解析 BigDecimal。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WanYuan of(String jsonValue) {
        var bd = ParseUtils.parseBigDecimal(jsonValue);
        // 校验 + 归一化（如 scale 约束、setScale）
        ValidateUtils.maxScale(2, bd);
        bd = bd.setScale(2, java.math.RoundingMode.UNNECESSARY);
        return new WanYuan(bd.toPlainString(), bd);
    }

    /** BigDecimal 运算入口（如 fromYuan、toYuan 使用 bigDecimalValue()）。 */
    public BigDecimal bigDecimalValue() {
        return bigDecimalValue;
    }
}
```

**不变约定**：

- 所有 BigDecimal DP 统一使用此模式。`value`（toPlainString）是规范值，`equals`/`hashCode` 基于它。
  `bigDecimalValue` 是派生缓存，运算时使用它。
- `@JsonCreator` 必须用 `mode = DELEGATING`，参数类型必须是 `String`，不依赖任何 Jackson 类型强制。
- 归一化幂等性要求：`new WanYuan(x).value()` 的 `equals` 对同一数值恒成立。

### 5.3 示例

Record 单字段（字面量家族，零 Jackson 代码——@JsonValue 继承自家族接口，见 ADR-0028）：
```java
public record Mobile(String value) implements StringLiteralType {
    public Mobile { … }   // 校验 + 归一化；无需 @JsonValue / @JsonCreator
}
```

Record 多字段：
```java
public record VerificationCodePolicy(
        @JsonProperty("codeLength") PositiveInt codeLength,
        @JsonProperty("expiry") Duration expiry,
        @JsonProperty("codeAlphabet") Alphabet codeAlphabet
) implements Type {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VerificationCodePolicy { … }
}
```

Class 单字段有缓存：
```java
@EqualsAndHashCode
…
public final class Version implements IntLiteralType {

    private final int value;

    public int value() {                              // @JsonValue 继承自 IntLiteralType（ADR-0028）
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)  // private 构造器不可见，必须显式
    public static Version of(int value) { … }
}
```

Enum（实现 EnumType：Jackson 原生序列化输出 name()，与持久化短名一致）：
```java
public enum AuthAccountType implements EnumType {
    P("Password"), S("Sms"), E("Email"), O("OAuth");
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountType of(String name) { … }
}
```

密封继承（AuthAccountId 模式）：
```java
@EqualsAndHashCode
@Getter
@Accessors(fluent = true)
public abstract sealed class AuthAccountId implements Identifier<String>, StringLiteralType
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

    private final String value;                       // @JsonValue 继承自 StringLiteralType（ADR-0028）；@Getter 生成 value() 访问器

    protected AuthAccountId(String value) {
        ValidateUtils.nonBlank(value);
        this.value = value;
…

    @Override
    public final String identifier() { … }
    public abstract AuthAccountType authAccountType();

    /** 统一反序列化入口。根据前缀路由到对应子类。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountId of(String value) {
        var prefix = value.substring(0, value.indexOf(':'));
        return switch (prefix) {
            case "P" -> PasswordAuthAccountId.of(value);
            case "S" -> SmsAuthAccountId.of(value);
            case "E" -> EmailAuthAccountId.of(value);
            case "O" -> SocialAuthAccountId.of(value);
            default -> throw new IllegalArgumentException("unknown prefix: " + prefix);
        };
    }
}

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Accessors(fluent = true)
public final class PasswordAuthAccountId extends AuthAccountId
        implements Comparable<PasswordAuthAccountId> {

    private final UserId userId;

    private PasswordAuthAccountId(String value, UserId userId) {
        super(value);
        this.userId = userId;
    }

    /** 子类反序列化入口。格式 {@code "P:{userId}"}。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PasswordAuthAccountId of(String value) {
        ValidateUtils.hasPrefix("P:", value);
        var suffix = value.substring(2);
        return new PasswordAuthAccountId(value, new UserId(ParseUtils.parseLong(suffix)));
    }

    public static PasswordAuthAccountId from(UserId userId) { … }

    @Override public AuthAccountType authAccountType() { return AuthAccountType.P; }
    @Override public int compareTo(PasswordAuthAccountId o) { … }
}
```

> **反序列化说明**：基类与子类各自有 `@JsonCreator`。Jackson 根据声明类型选择：
> 声明类型为基类 `AuthAccountId` 时走基类路由工厂按前缀分派；
> 声明类型为具体子类（如 `PasswordAuthAccountId`）时直接走子类入口。

### 5.4 `Serializable`

`Type` 接口不默认实现 `Serializable`。需要 JDK 序列化的 DP 显式 `implements Serializable` 并加 `@Serial serialVersionUID`。

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
   - BigDecimal DP → `final class` + Lombok，规范值用 `String`，BigDecimal 作派生缓存（见 5.2）；
   - 密封继承层次 → 基类 `abstract sealed class` + 子类 `final class`（见 5.3 密封继承示例）；
   - 枚举 DP → enum + `EnumType`（见 9）；
2. 选择接口：单属性字面量 → 字面量家族（`StringLiteralType`/`LongLiteralType`/`IntLiteralType`/`BooleanLiteralType`/`DoubleLiteralType`，枚举用 `EnumType`）；标识符叠加 `Identifier<T>`；多属性/无标量语义 → `Type`；
3. 字段类型必须不可变；
4. 紧凑构造器/私有构造器内用 `ValidateUtils` 校验，只做格式归一化；
5. 按对应模板实现 `value()`（字面量家族：注解继承自家族接口，零 Jackson 代码；多属性：`@JsonCreator(PROPERTIES)` + `@JsonProperty`）；带构造逻辑（缓存/单例/解析）的 class 保留 `@JsonCreator(DELEGATING)` 在 `of(T)` 上；补 `parse`、`toString` 等；
6. 需要自然顺序时实现 `Comparable<Self>`；
7. 需要 JDK 序列化时显式实现 `Serializable`；
8. 多字段 DP 必须用 `@JsonProperty` + `@JsonCreator(mode = PROPERTIES)`；
9. 运行 `:soda-components:soda-component-domain-types:build` 验证。

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