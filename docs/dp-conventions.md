# Domain Primitive 设计规范

目标读者：coding agent。本规范定义项目中所有 Domain Primitive（DP / 领域原语）的行为契约、实现形态、工厂命名、校验归一化、JSON/序列化及枚举 DP。

## Quick-Start Checklist

新增或修改 DP 时逐条核对：

- [ ] **不可变**：所有字段 `final`，不包裹 `byte[]`、`Date`、可变集合等类型；
- [ ] **自校验**：构造器通过 `ValidateUtils` 校验，非法值不可表示；
- [ ] **value-based identity**：`equals`/`hashCode` 基于规范值字段；
- [ ] **非空类型**：可选场景用 `Optional<DP>`，工厂方法对 `null` 零容忍；
- [ ] **不修理输入**：不自动 trim、不舍入、不修复非法输入；
- [ ] **实现形态**：简单 DP 用 record；需缓存/派生字段/私有构造器时用 `final class` + Lombok；
- [ ] **工厂命名**：`of`（参数即值）、`from`（跨类型转换）、`parse(String)`（字符串解析）符合 LocalDate 三分语义；
- [ ] **可比较性**：只在有自然顺序时实现 `Comparable<Self>`；
- [ ] **序列化**：默认不实现 `Serializable`，需要时显式实现；
- [ ] **JSON**：显式声明 `@JsonValue` + `@JsonCreator`，不依赖 Jackson 推断；`@JsonValue` 必须在 public 方法上（Jackson 3 忽略 private/字段级别）；Jackson 3 RecordDeserializer 自动处理 record 反序列化，`@JsonCreator(PROPERTIES)` + `@JsonProperty` 对 record 冗余；sealed class 无需 `@JsonSubTypes`（Jackson 3 从 `permits` 子句自动发现）；
- [ ] **富血方法**：自包含领域方法用 JDK 风格命名（`withXxx`、`plusXxx`、`isXxx`、`toXxx`、`next`），不调用 gateway/service；
- [ ] **缓存**：class DP、值域小、有明确性能收益时才做透明缓存，禁止依赖 `==`。

- [ ] **JSON**：显式声明 `@JsonValue` + `@JsonCreator`，不依赖 Jackson 推断；`@JsonValue` 必须在 public 方法上（Jackson 3 忽略 private 方法上的 `@JsonValue`，字段级别仍有效）；Jackson 3 RecordDeserializer 自动处理 record 反序列化，`@JsonCreator(PROPERTIES)` + `@JsonProperty` 对 record 冗余；sealed class 无需 `@JsonSubTypes`（Jackson 3 从 `permits` 子句自动发现）；

## 1. 核心行为契约

所有 DP 必须：

| 不可变 | 所有字段 `final` | record 隐式保证；class 显式 `private final`；禁止包裹可变类型 |
| 自校验 | 构造时验证业务约束 | 无效值不可表示；非法输入抛 `IllegalArgumentException` |
| value-based identity | `equals`/`hashCode` 只看规范值 | 不依赖对象身份；有派生字段时不纳入相等性 |
| class-aware identity | `equals`/`hashCode` 包含运行时类型检查 | 不同 DP 类型之间永不相等 |
| 非空类型 | DP 实例代表有效值 | 可选场景用 `Optional<DP>`；工厂对 `null` 零容忍 |
| 不修理输入 | 只归一化格式，不修复非法值 | 不自动 trim、不舍入、不把非法值改成合法值 |
| 可读 toString | `toString()` 输出调试字符串 | record 风格 `ClassName[field=value]`；Secret 脱敏为 `ClassName[***]` |

`Type` 是 DP 的根标记接口，**不继承 `Serializable`**。`Identifier<T>` 继承 `Type`，保留 `T extends Comparable<T>` 约束（标识符底层值经常需要排序），但 DP 本身是否实现 `Comparable<Self>` 是可选的。


### 1.1 equals/hashCode 规范

equals/hashCode 基于规范值 + class 类型（class-aware identity），派生/缓存值不参与。

| 实现形态 | 策略 | 说明 |
|---|---|---|
| Record（单/多字段） | record 自动生成，所有 components 参与 | 不需要手工编写；class 检查通过 `getClass()` 保证 |
| Class 单字段、无派生值 | `@EqualsAndHashCode` | 隐式纳入唯一字段（即规范值） |
| Class 含派生/缓存字段 | `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` 在规范值上 | 派生/缓存字段被排除 |
| 密封继承 | 基类 `@EqualsAndHashCode`（比较 `value`），子类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)` 且不标注任何 `@Include` | 仅比较基类 value + class 类型（`instanceof`），子类自身字段全部排除 |
| Secret 子类 | 引用相等（`Object.equals`/`Object.hashCode`），不暴露敏感值 | 安全脱敏要求；不使用 Lombok 注解 |
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
| Secret 子类 | 脱敏：`getClass().getSimpleName() + "[***]"`，不得暴露内部值 | `RawCredential[***]` |

要点：

- 调试字符串**不应**被业务逻辑依赖解析或比较；
- class DP 的 `toString()` 必须手工编写与 record 一致的格式，不可省略；
- 密封层级统一在基类编写，子类不重写，借助 `getClass().getSimpleName()` 得到正确子类名；
- `Secret` 及其子类是唯一例外，必须脱敏输出，`toString()` 在基类声明为 `final` 防止子类泄露。

> **record 构造器说明**：record 典范构造器必须是 public，无法物理阻止 `new Xxx(...)`。业务代码约定优先使用工厂方法；框架/序列化需要时可直接走构造器。


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

record 是默认形态。下面模板同时覆盖标识符与非标识符：差异仅在 `implements Identifier<T>` 还是 `implements Type`。

```java
package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;  // 标识符 DP
// import com.soda.component.domain.Type;     // 非标识符 DP
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

/**
 * Xxx DP — 不可变、自校验。
 * Jackson 3: record 上使用 {@code @JsonValue} 必须在 public 方法（component 上无效），
 * 此处显式声明 {@code value()} 方法。
 */
public record Xxx(T value) implements Identifier<T> {  // 或 implements Type

    /** Jackson 3 序列化出口 — 必须为 public 方法。 */
    @JsonValue
    @Override
    public T value() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)        // 紧凑构造：校验 + 格式归一化
    public Xxx {
        ValidateUtils.notNull(value);                            // 或其他 ValidateUtils 方法
        // 只做不改变值语义的归一化，如 Locale.ROOT 大小写
    }

    /** 从字符串解析；null 或非法值时抛出 IllegalArgumentException。 */
    public static Xxx parse(String s) {                          // 仅当构造器参数不是 String 时需要
        return new Xxx(ParseUtils.parseXxx(s));
    }

    @Override
    public T identifier() {                                      // 仅标识符 DP 需要
        return value;
    }

    // 只在有自然顺序时实现 Comparable<Self>
    // @Override
    // public int compareTo(Xxx other) { ... }
}
```

## 2.3 Class + Lombok 模板

用于需要缓存、派生字段或私有构造器的场景。无缓存与有缓存的差异仅在于构造器可见性和 `@JsonCreator` 位置。

```java
package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;  // 仅在需要 JDK 序列化时

/**
 * Xxx DP — 不可变、自校验、带缓存（可选）。
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Xxx implements Type, Serializable {  // Serializable 按需

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
    public static final Xxx PRIMARY = CACHE[0];
    // ======================

    private final int value;

    /** 序列化出口。显式方法级别，不受 ObjectMapper visibility 配置影响。 */
    @JsonValue
    public int value() {
        return value;
    }

    /** 构造器只含校验和格式归一化，不修理输入。 */
    private Xxx(int value) {                                 // 有缓存时私有；无缓存时 public 并加 @JsonCreator
        ValidateUtils.minValue(0, true, value);
        this.value = value;
    }

    /** 主入口。有缓存时 @JsonCreator 挂这里。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)       // 有缓存时必需；无缓存时直接标在 public 构造器
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

`@JsonCreator` 位置（Jackson 3 说明列仅列出与 Jackson 2 有差异的行为）：

| 实现 | `@JsonCreator` 位置 | Jackson 3 说明 |
|---|---|---|
| record 单字段 | 紧凑构造器 `@JsonCreator(mode = DELEGATING)` | 必须；单字段反序列化需 delegating |
| record 多字段 | `@JsonCreator(mode = PROPERTIES)` 在紧凑构造器上，或省略（Jackson 3 RecordDeserializer 自动推断） | 可选；省略时 Jackson 3 自动匹配典范构造器与 record component 名 |
| class 无缓存 | public 构造器 `@JsonCreator(mode = DELEGATING)` | 无变化 |
| class 有缓存 | 静态 `of(T)` `@JsonCreator(mode = DELEGATING)` | 无变化 |
| class 多字段 | 静态 `of(...)` / 构造器 `@JsonCreator(mode = PROPERTIES)` | 无变化 |
| enum | 静态 `of(String)` `@JsonCreator(mode = DELEGATING)` | 无变化 |
| 密封基类（路由工厂） | 基类静态 `of(String)` `@JsonCreator(mode = DELEGATING)`，按前缀路由 | 无变化 |
| BigDecimal DP (class) | 静态 `of(String)` `@JsonCreator(mode = DELEGATING)`，内部解析 BigDecimal | 无变化 |

## 3.2 常量与便捷工厂

JDK 包装类常见 `MIN`/`MAX`/`ZERO`/`EPOCH`/`random()`/`now()` 等常量或便捷工厂。DP 中按需暴露，**不强制**：

- 数量/版本/金额型 DP（如 `Version`、`WanYuan`）可暴露 `ZERO`/`ONE`/`MIN`/`MAX`，如果有领域意义；
- 标识符 DP 通常不需要 `UserId.ZERO` 这类常量；
- 时间型 DP 可按 `LocalDate` 惯例暴露 `now()`、`MIN`/`MAX`；
- 随机生成型 DP（如 `UUId`）保留 `random()` 或 `generate(...)`；
- “无值”用 `Optional<DP>` 表达，不用 `Xxx.EMPTY` 单例（除非 EMPTY 本身是合法领域值）。

## 4. 校验与归一化

### 4.1 职责分层

| 层次 | 职责 | 是否抛 IAE |
|---|---|---|
| `ParseUtils` | 不可靠 `Object` → 基础类型，含 null 检查 | 是 |
| `ValidateUtils` | 基础参数校验 | 是 |
| DP 构造器 | 业务校验 + 格式归一化，不做类型转换 | 否（由工具抛） |
| DP `parse` | 字符串入口，委托 `ParseUtils` → 构造器 | 否 |

### 4.2 校验收敛原则

所有校验必须通过 `ValidateUtils`，禁止内联：

  | 校验场景 | 正确做法 | 禁止替代 |
  | null 检查 | `ValidateUtils.notNull(value)` | `Objects.requireNonNull` |
  | blank | `ValidateUtils.hasText(value)` | `String.isBlank() + if/throw` |
  | 字符串长度 | `ValidateUtils.maxLength(value, max)` | 内联 `if/throw` |
  | 数值范围 | `ValidateUtils.minValue` / `range` | 内联比较 |
  | 正则格式 | `ValidateUtils.matches(value, pattern)` | 内联 `if(!pattern.matcher(...))` |
  | URI | `ValidateUtils.validUrl(uri)` | `try/catch URI` 校验 |
  | 类型转换 | `ParseUtils.parseXxx(value)` | 手动 cast / 类型推断 |

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

每个 DP 独立声明序列化和反序列化入口，不依赖 Jackson 配置或隐式推断。

### 5.1 模式总表

| 实现形态 | `@JsonValue`（序列化） | `@JsonCreator`（反序列化） | Jackson 3 说明 |
|---|---|---|---|
| Record + 单字段 | public 方法返回 component 值 | `(mode = DELEGATING)` 紧凑构造器 | `@JsonValue` 不能在 record component 上，必须在 public 方法 |
| Record + 多字段 | — | `(mode = PROPERTIES)` 紧凑构造器或省略 | Jackson 3 RecordDeserializer 能从典范构造器推断，`@JsonProperty` 冗余 |
| Class + 单字段无缓存 | public 方法 | `(mode = DELEGATING)` public 构造器 | 无变化 |
| Class + 单字段有缓存 | public 方法 | `(mode = DELEGATING)` 静态 `of(T)` | 无变化 |
| Class + 多字段 | — | `(mode = PROPERTIES)` 静态 `of(...)` | 无变化 |
| Enum | `name()` 默认；如需显式可加 public 方法（Jackson 3 忽略 `@JsonValue private`） | `(mode = DELEGATING)` 静态 `of(String)` | Jackson 3 默认用 `.name()` 序列化枚举；`@JsonValue private` 被忽略，可省略 |
| 密封继承基类 | public 方法（子类复用） | 基类路由 `(mode = DELEGATING)` 静态 `of(String)`；子类也可各自声明 | sealed 类无需 `@JsonSubTypes`，Jackson 3 从 `permits` 子句自动发现 |

| Java 类型 | JSON 类型 | 说明 |
|---|---|---|
| `long` / `int` | 数字 | 标量 |
| `String` | 字符串 | 标量 |
| `boolean` | 布尔 | 标量 |
| `BigDecimal` | 字符串 | 经 `toPlainString()` 输出，规范值为 String，BigDecimal 作为派生缓存 |
| `Instant` / `Duration` / 其他 JSR-310 | 数字/字符串 | Jackson 3 原生支持 JSR-310 类型，无需额外模块注册 |
| enum | 字符串 | Jackson 3 默认用 `name()` 序列化，`@JsonValue` 可选但必须在 public 方法上 |

> **Jackson 3 JSR-310 说明**：Jackson 3 包含对 `java.time` 类型的原生序列化支持，
> 无需注册 `JavaTimeModule`。`Instant` 输出数字时间戳（epoch seconds），`Duration` 输出 ISO-8601 格式字符串。
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

Record 单字段（Jackson 3: @JsonValue 在 public 方法上，不在 record component）：
```java
public record Mobile(String value) implements Type {
    @JsonValue
    @Override
    public String value() {
        return value;
    }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Mobile { … }
}
```

Record 多字段：
```java
public record VerificationCodePolicy(
        @JsonProperty("codeLength") int codeLength,
        @JsonProperty("expiry") Duration expiry
) implements Type {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VerificationCodePolicy { … }
}
```

Class 单字段有缓存：
```java
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Version implements Type {

    private final int value;

    @JsonValue                                    // 方法级别，不依赖字段可见性
    public int value() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Version of(int value) { … }
}
```

Enum（Jackson 3 默认用 name() 序列化，无需 @JsonValue；@JsonValue private 被忽略）：
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
public abstract sealed class AuthAccountId implements Identifier<String>
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

    @JsonValue
    private final String value;                       // @Getter 生成 value() 访问器

    protected AuthAccountId(String value) {
        ValidateUtils.nonBlank(value);
        this.value = value;
    }

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
2. 选择接口：标识符 → `Identifier<T>`；其他 → `Type`（枚举用 `EnumType`）；
3. 字段类型必须不可变；
4. 紧凑构造器/私有构造器内用 `ValidateUtils` 校验，只做格式归一化；
5. 按对应模板添加 `@JsonValue`（必须为 public 方法，record component / private 方法在 Jackson 3 中无效）、`@JsonCreator`（必须显式 `mode`）、`parse`、`toString` 等；
6. 需要自然顺序时实现 `Comparable<Self>`；
7. 需要 JDK 序列化时显式实现 `Serializable`；
8. 多字段 DP 必须用 `@JsonProperty` + `@JsonCreator(mode = PROPERTIES)`；
9. 运行 `:soda-components:soda-component-support:build` 验证。

## 附录 A：`compareTo` 编写规范

只在有自然顺序时实现（标识符、版本号、长度等）。三种字段形态的写法：

```java
// 对象类型单字段（如 UUId）
@Override
public int compareTo(UUId other) {
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
| `UUId` | record | ✅ | 无 | 不显式 |
| `Mobile` / `Email` / `WanYuan` | record / class / class | ❌（无领域顺序） | 无（WanYuan 缓存 BigDecimal 派生值） | 不显式 |
| `Version` | class | ✅ | `[0, 99]` | ✅ 显式 |