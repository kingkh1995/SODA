# Domain Primitive 编写规范

## 总则

所有 Domain Primitive（领域原语）必须满足：

- **不可变** — 所有字段不可变（record 隐式保证，class 显式 {@code private final}；枚举本身就是不可变的）
- **自校验** — 构造时验证业务约束，无效值不生成实例
- **可序列化** — 实现 `Serializable`（class/record 显式实现，枚举通过 Java 内置机制自动支持）
- **可比较** — 实现 `Comparable<Self>` 提供类型安全比较（class/record 各自实现 `compareTo(Self)`，枚举使用 Java 内置的枚举比较）

> 枚举 DP 遵循 {@link com.soda.component.domain.EnumType} 契约，由于 Java 枚举的特殊性（final class、编译器生成的方法），模板见下节。

## 标识符 vs 非标识符 DP

| 区别 | 标识符 DP | 非标识符 DP |
|---|---|---|
| 实现接口 | `Identifier<T>` + `Comparable<Self>` | `Type` + `Comparable<Self>` |
| `compareTo` | 自行实现 `compareTo(Self)` | 自行实现 `compareTo(Self)` |
| 领域方法 | 无（仅 `identifier()`） | 按领域提供（如 `Email.localPart()`） |
| 示例 | `LongId`, `UUId`, `UserId` | `Email`, `Money`, `Age` |


## `compareTo` 编写规范

所有 DP 的 `compareTo` 必须统一以下风格（参考 `Integer.compareTo`）：

| 场景 | 写法 | 示例 |
|---|---|---|
| 当前实例字段 | 一律加 `this.` | `this.value.compareTo(...)` |
| 基本类型（int/long/boolean） | 静态方法 | `Integer.compare(this.value, other.value)` |
| 对象类型（String/BigDecimal/Instant/Duration） | 实例方法 | `this.value.compareTo(other.value)` |
| 单字段 DP | 单行 `return` | `return this.value.compareTo(other.value);` |
| 多字段 DP | `var cmp` + `if (cmp != 0) { return cmp; }` | 花括号不可省略（参见 STYLEGUIDE） |

```java
// ✅ 单字段 — 对象类型
@Override
public int compareTo(Mobile other) {
    return this.value.compareTo(other.value);
}

// ✅ 单字段 — 基本类型
@Override
public int compareTo(CodeLength other) {
    return Integer.compare(this.value, other.value);
}

// ✅ 多字段
@Override
public int compareTo(EmailContent other) {
    var cmp = this.subject.compareTo(other.subject);
    if (cmp != 0) {
        return cmp;
    }
    return this.body.compareTo(other.body);
}
```
## 类结构模板（标识符 DP — record 风格）

JDK 16+ 起优先使用 `record` 实现标识符 DP。Record 自动提供不可变字段、`equals()`/`hashCode()`、`toString()` 和序列化合规。

```java
public record XxxId(@JsonValue T value) implements Identifier<T>, Comparable<XxxId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public XxxId {                                              // 紧凑构造：校验归一化
        // 校验 → 通过 ValidateUtils
    }

    /** 从字符串解析，null 或非法值时抛出 IllegalArgumentException。 */
    public static XxxId parse(String s) {
        return new XxxId(ParseUtils.parseXxx(s));
    }

    @Override
    public T identifier() {
        return value;
    }

    @Override
    public int compareTo(XxxId other) {
        return this.value.compareTo(other.value);                    // String / Long.compare 等，取决于 T 类型
    }
}
```

## 类结构模板（非标识符 DP — record 风格）

```java
public record Xxx(@JsonValue String value) implements Type, Comparable<Xxx> {

    @Serial
    private static final long serialVersionUID = 1L;

    public Xxx {                                                // 紧凑构造：校验归一化
        ValidateUtils.nonBlank(value);
        ValidateUtils.matches(PATTERN, value);
    }

    /** 从字符串解析，null 或非法值时抛出 IllegalArgumentException。 */
    public static Xxx parse(String s) {
        return new Xxx(ParseUtils.parseString(s));
    }

    @Override
    public int compareTo(Xxx other) {
        return this.value.compareTo(other.value);
    }
}
```

## 类结构模板（非标识符 DP — class + Lombok 风格）

当 record 无法满足需求时（如需要预计算派生字段、缓存实例），使用普通 class + Lombok 注解代替 record。
通过 `@Accessors(fluent = true)` 保持访问器命名与 record 一致（`value()` 而非 `getValue()`）。
`toString()` 手写为 record 方括号格式 `"Xxx[value=…]"`。
`@JsonCreator` 的位置取决于是否需要缓存实例：

### 非缓存模板（如 Email）

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Xxx implements Type, Comparable<Xxx> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @JsonValue
    @EqualsAndHashCode.Include
    private final String value;

    private final String derivedField;                       // 预计算派生字段（如 Email.localPart/domain）

    @JsonCreator
    public Xxx(String value) {                               // public 构造器 = Jackson 入口
        // 校验 + 归一化
        ValidateUtils.nonBlank(value);
        this.value = value;
        this.derivedField = compute(value);                  // 预计算
    }

    /** 从字符串解析，null 或非法值时抛出 IllegalArgumentException。 */
    public static Xxx parse(String s) {
        return new Xxx(ParseUtils.parseString(s));
    }

    public String derivedField() {
        return derivedField;
    }

    @Override
    public int compareTo(Xxx other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return "Xxx[value=" + value + "]";
    }
}
```

### 缓存模板（如 Version）

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Xxx implements Type, Comparable<Xxx> {

    @Serial
    private static final long serialVersionUID = 1L;

    // 缓存示例（参考 Integer 缓存设计）
    private static final Xxx[] CACHE = new Xxx[100];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new Xxx(i);
        }
    }

    public static final Xxx PRIMARY = CACHE[0];

    @Getter
    @JsonValue
    @EqualsAndHashCode.Include
    private final int value;

    /** 校验在构造器中完成 — 所有创建路径（{@code of}、{@code parse}、Jackson）最终都经过此构造器。 */
    private Xxx(int value) {
        ValidateUtils.minValue(0, true, value);
        this.value = value;
    }

    @JsonCreator                                                   // 有缓存 → @JsonCreator 在 of(T) 上
    public static Xxx of(int value) {
        if (0 <= value && value < CACHE.length) {
            return CACHE[value];
        }
        return new Xxx(value);
    }

    public static Xxx parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    public Xxx next() {
        return of(value + 1);
    }

    @Override
    public int compareTo(Xxx other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return "Xxx[value=" + value + "]";
    }
}
## 命名与包结构

| 元素 | 规范 | 示例 |
|---|---|---|
| 包 | `com.soda.component.support.types` | — |
| 类名 | 名词 + `Id`（标识符）或领域名词 | `LongId`, `UserId`, `Money`, `Email` |
| 工厂方法 | parse(String) 字符串解析入口（仅单字段 DP，构造器参数非 String 时） | — |
| Accessor | `value()`（record 自动，class 通过 `@Accessors(fluent = true)`） | — |

## 职责分层

| 层次 | 定位 | 规则 | 是否引用 `IllegalArgumentExceptions` |
|---|---|---|---|
| `ParseUtils` | 类型转换工具 | 将不可靠 `Object` 输入转为基础类型（long/String），含 null 检查 | 是（复用异常消息） |
| `ValidateUtils` | 校验工具 | 基础参数校验，失败抛出 IAE | 是（复用异常消息） |
| DP 构造器 | 业务校验 + 归一化 | 使用 `ValidateUtils` 校验，不做类型转换 | 否 |
| DP parse | 字符串解析入口 | 委托 ParseUtils → 委托构造器，不重复 null 检查 | 否 |

## 校验收敛原则

校验代码必须通过通用工具类，禁止内联：

| 校验类型 | 工具类 | 方法 | 示例用途 |
|---|---|---|---|
| Null 检查（类型转换侧） | `ParseUtils` | `parseXxx` 内部自带 | `ParseUtils.parseString(value)` 含 null→抛IAE |
| Null 检查（业务校验侧） | `ValidateUtils` | `notNull(value)` | `ValidateUtils.notNull(expiry)` |
| Blank 检查 | `ValidateUtils` | `nonBlank(value)` | `ValidateUtils.nonBlank(code)` |
| 数值下界（long） | `ValidateUtils` | `minValue(min, inclusive, value)` | ID 必须 ≥ 0 |
| 数值下界（BigDecimal） | `ValidateUtils` | `minValue(min, inclusive, value)` | 金额必须 ≥ 0 |
| 数值上界（int） | `ValidateUtils` | `maxValue(max, inclusive, value)` | codeLength 必须 ≤ 100 |
| 字符串长度上限 | `ValidateUtils` | `maxLength(max, value)` | 昵称最多 30 字符 |
| BigDecimal 精度 | `ValidateUtils` | `maxScale(max, value)` | WanYuan 最多 2 位小数 |
| 数值范围（int, min-max） | `ValidateUtils` | `range(min, max, value)` | codeLength 必须在 [1, 100] |
| 数值下界（Duration） | `ValidateUtils` | `minValue(min, inclusive, value)` | 过期时间必须 > 0 |
| 格式匹配（regex） | `ValidateUtils` | `matches(pattern, value)` | 邮箱/UUID/手机号格式 |
| URI 格式 + absolute + http/https | `ValidateUtils` | `validUri(value)` | 头像 URL 校验 |

## 禁止模式

以下代码模式**禁止**出现在任何 DP 的构造器、parse 方法或任何校验上下文中：

| 禁止模式 | 反例 | 正确做法 |
|---|---|---|
| `Objects.requireNonNull` | `Objects.requireNonNull(code, "msg")` | `ValidateUtils.notNull(code)` |
| 内联 `if/throw` | `if (value.length() > 30) throw …` | `ValidateUtils.maxLength(30, value)` |
| `try/catch` 校验 | `try { new URI(value); } catch …` | `ValidateUtils.validUri(value)` |
| `String.isBlank()` + `if/throw` | `if (code.isBlank()) throw …` | `ValidateUtils.nonBlank(code)` |
| 直接引用 `IllegalArgumentExceptions` | `throw IllegalArgumentExceptions.forIsNull()` | 由 `ValidateUtils` 统一委托 |
| 自定义异常消息字符串 | `new IllegalArgumentException("my msg")` | 在 `IllegalArgumentExceptions` 中添加工厂方法 |
| 类型转换（type cast） | `((Xxx) other).value` | 实现 `Comparable<Self>` + `compareTo(Self)` 消除强转 |

## 校验方法扩展规范

当现有 `ValidateUtils` 方法无法覆盖新的校验需求时，按以下流程新增：

### 步骤

1. **在 `IllegalArgumentExceptions` 中添加工厂方法** — 生成带有业务语义的错误消息，遵循「异常消息」规范
2. **在 `ValidateUtils` 中添加校验方法** — 委托步骤 1 的工厂方法，遵循通用命名原则
3. **在 DP 构造器中使用 `ValidateUtils.xxx()`** — 不直接引用 `IllegalArgumentExceptions`
4. **更新本规范的「校验收敛原则」校验类型表** — 记录新类型的签名和用途

### 命名规范

| 场景 | ValidateUtils 方法命名 | IllegalArgumentExceptions 工厂命名 |
|---|---|---|
| 字符串长度上限 | `maxLength(max, value)` | `forMaxLength(actual, max)` |
| 数值上界 | `maxValue(max, inclusive, value)` | `forMaxValue(value, max, inclusive)` |
| 数值下界 | `minValue(min, inclusive, value)` | `forMinValue(value, min, inclusive)` |
| 特定业务校验 | `validXxx(value)` | `forInvalidXxx(value)` / `forRelativeXxx(value)` |
| 数值范围 | `range(value, min, max)` | `forOutOfRange(value, min, max)` |
| 通用状态检查 | `nonBlank(value)` | `forIsBlank()` |
### 方法设计约束

- **ValidateUtils 方法签名**：辅助参数在前，被校验值（命名为 {@code value}）在最后，最后为 boolean flag（如有）。如 {@code minValue(0, false, value)}、{@code maxLength(30, value)}。格式匹配方法例外：{@code matches(Pattern, String)} 遵循 {@code Pattern.matcher} 的 Java 标准签名。方法名为 {@code void}，失败抛 {@code IllegalArgumentException}。
- **IllegalArgumentExceptions 工厂签名**：参数携带足够信息构造描述性消息，返回 `IllegalArgumentException` 实例。
- **消息模板**：英文、首字母小写、无句号。使用 `StringBuilder` 拼接复合消息（参考 `forMinValue`），避免字符串拼接性能开销。
- **不引入领域概念**：ValidateUtils 和 IllegalArgumentExceptions 属于 `soda-component-support` 通用模块，方法必须泛化，不能绑定特定领域名词（如不能写 `forNicknameTooLong`，应写 `forMaxLength`）。

## 异常消息

- 英文，首字母小写，无句号
- 异常消息统一通过 `IllegalArgumentExceptions` 工厂方法生成
- DP 类不直接引用 `IllegalArgumentExceptions`，消息由 `ValidateUtils` / `ParseUtils` 带入

## 类型分发策略

通过单一 `parse(String)` 工厂方法接受不可靠输入，内部流程如下：

```
parse(String)
  │ ParseUtils.parseXxx(value)          ← Object → 基础类型（含 null 检查 + 类型分发）
  ▼
new XxxId(convertedValue)
  │ ValidateUtils.xxx(...)              ← 业务校验
  │ normalization (trim, toLowerCase…)
  ▼
XxxId (valid, normalized)
```

## 对比一致性

| 检查项 | LongId (record) | UUId (record) | WanYuan (record) | Version (class+Lombok) |
|---|---|---|---|---|
| 实现接口 | `Identifier<Long>, Comparable<LongId>` | `Identifier<String>, Comparable<UUId>` | `Type, Comparable<WanYuan>` | `Type, Comparable<Version>` |
| 构造校验 | `minValue(…, 0, false)` | `nonBlank(…)` + `matches(uuidPattern, …)` | `notNull(…)` + `minValue(…, 0, true)` + `maxScale(…, 2)` | `minValue(…, 0, true)` |
| `parse` 入口 | `ParseUtils.parseLong(value)` | `ParseUtils.parseString(value)` | `ParseUtils.parseBigDecimal(value)` | `ParseUtils.parseInt(value)` |
| `@JsonValue` | record 组件 `value` | record 组件 `value` | record 组件 `value` | 字段 `value` |
| `@JsonCreator` | 无需 | 无需 | 无需 | `of(T)` 显式 |
| 引用 `IllegalArgumentExceptions` | 否 | 否 | 否 | 否 |
| `toString()` 格式 | `"LongId[value=…]"` | `"UUId[value=…]"` | `"WanYuan[value=…]"` | `"Version[value=…]"` 手写对齐方括号 |
| 访问器来源 | record 自动 | record 自动 | record 自动 | `@Accessors(fluent = true)` |
| 缓存 | 无 | 无 | 无 | [0, 99] 缓存实例 |
| 随机生成 | 无 | `random()` | 无 | 无 |

## Jackson 集成

Record 的典范构造器被 Jackson 自动识别（Jackson 2.12+ 原生支持），不需要额外注解标注反序列化入口。
Class + Lombok 风格的 DP 需显式标注 `@JsonCreator`，位置取决于是否需要缓存实例：

| 实现方式 | 序列化 | 反序列化 |
|---|---|---|
| record | `@JsonValue` 在 record 组件上 | 自动（典范构造器） |
| class + Lombok（无缓存） | `@JsonValue` 在字段上 | `@JsonCreator` 在 public 构造器上 |
| class + Lombok（有缓存） | `@JsonValue` 在字段上 | `@JsonCreator` 在 `of(T)` 上 |


### 多字段 DP 的序列化

上述模板适用于**单字段 DP**（只有一个业务值，由 `@JsonValue` 标记）。当 DP 包含多个业务字段时（如 `VerificationCodePolicy`：codeLength + expiry），应序列化为 JSON 对象而非单值：

| 策略 | 注解 | 适用场景 |
|---|---|---|
| 单值 | `@JsonValue` 在字段/组件上 | 只有一个业务字段 |
| 对象 | `@JsonCreator(PROPERTIES)` + 每个参数 `@JsonProperty` | 多个业务字段，需完整反序列化 |

多字段 DP 的编写要点：

- **无 `@JsonValue`** — 序列化为对象 `{"field1":v1, "field2":v2}`
- **`@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)`** 在构造器上 — Jackson 通过参数名匹配 JSON 属性
- **每个构造器参数加 `@JsonProperty`** — 确保参数名映射不受编译期名称混淆影响
- **Record 自动处理** — record 典范构造器被 Jackson 原生支持，无需额外注解
- **无需 parse(String)** — 多字段 DP 没有单值入口，故省略此工厂方法

```java
// ✅ 多字段 DP — record 风格，自动对象序列化
public record VerificationCodePolicy(
        @JsonProperty("codeLength") int codeLength,
        @JsonProperty("expiry") Duration expiry
) implements Type, Comparable<VerificationCodePolicy> { … }
```
## 创建新 DP 的步骤

### record 风格（默认）

1. 在 `com.soda.component.support.types` 包下创建 record
2. 选模板：标识符 → `implements Identifier<T>, Comparable<XxxId>`；非标识符 → `implements Type, Comparable<Xxx>`
3. record 紧凑构造中全部通过 `ValidateUtils` 做输入校验，禁止内联任何 `if/throw`、`Objects.requireNonNull`、`try/catch` 等校验代码
4. 添加 `parse(String)` 工厂方法（仅单字段 DP 且构造器参数非 String 时需要，如 LongId、UserId）
5. 如需要新校验类型，先在 `ValidateUtils` 中添加通用方法
6. 如需要新类型转换，先在 `ParseUtils` 中添加 `parseXxx(Object)` 方法
7. 运行 `:soda-components:soda-component-support:build` 验证

### class + Lombok 风格（record 不适用时）

当无法使用 record 时（如需要预计算派生字段、缓存实例），采用 class + Lombok 风格。
`@JsonCreator` 的位置取决于是否需要缓存实例：

#### 无缓存（如 Email）

1. `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@Accessors(fluent = true)`（无 `@RequiredArgsConstructor`）
2. 添加 `Comparable<Self>` 到 `implements` 子句
3. `@Getter` 在 `value` 字段上，`@JsonValue` 也在字段上；`@EqualsAndHashCode.Include` 在 `value` 字段上
4. **`@JsonCreator` 在 public 构造器上** — Jackson 直接通过构造器反序列化
5. 添加 `parse(String)` 工厂方法，委托 `ParseUtils.parseString(value)` → 构造器
6. `toString()` 手写为方括号格式
7. 自行实现 `compareTo(Self other)`，无强转
8. 其余步骤同 record 风格

#### 有缓存（如 Version）

1. `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@Accessors(fluent = true)`（用显式构造器代替 `@RequiredArgsConstructor`）
2. 添加 `Comparable<Self>` 到 `implements` 子句
3. `@Getter` 在 `value` 字段上，`@JsonValue` 也在字段上；`@EqualsAndHashCode.Include` 在 `value` 字段上
4. 构造器为 `private`，内含校验
5. **`@JsonCreator` 在静态 `of(T)` 工厂方法上** — Jackson 通过 `of(T)` 反序列化以利用缓存
6. `parse(String)` 委托到 `of(ParseUtils.parseXxx(value))` 而非直接 `new Xxx(…)`
7. `toString()` 手写为方括号格式
8. 自行实现 `compareTo(Self other)`，无强转
9. 其余步骤同 record 风格

## SPI 配置化常量

DP 中的数值常量（长度上限、范围边界等）可通过 SPI 机制支持业务模块自定义。
正则 Pattern 等不适合 SPI（编译成本高，很少需要自定义）。

### SPI 接口

在 `TypeConfigProvider` 中添加默认方法，提供默认值及 Javadoc 说明下限：

```java
public interface TypeConfigProvider {

    /** 版本号缓存上限（含）。默认 99，至少 99。 */
    default int versionCacheHigh() {
        return 99;
    }

    /** 短信内容最大长度。默认 70，至少 70。 */
    default int smsContentMaxLength() {
        return 70;
    }
}
```

### DP 内引用

通过 `TypeConfig.PROVIDER.xxx()` 获取，用 `Math.max(下限, …)` 保证合法性：

```java
// 下限 = 默认值（SPI 只能往上调，不能低于原始设计值）
private static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());
```

### 依赖方向

`support.types` → `support.util`（`TypeConfig`）→ `support.spi`（`TypeConfigProvider`）。
与模块白名单一致：`support.types` 的 `allowedDependencies` 含 `support.util`、`support.spi`。

## 类结构模板（枚举 DP — Java enum + Lombok 风格）

枚举 DP 实现 {@link com.soda.component.domain.EnumType}（继承 {@link com.soda.component.domain.Type}），
使用 Java {@code enum} + Lombok，不转为 class/record。

### 模板

```java
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Xxx implements EnumType {

    A("Description A"),
    B("Description B");

    private final String desc;

    @JsonCreator
    public static Xxx of(String name) {
        return ParseUtils.parseEnum(Xxx.class, name);
    }
}
```

### 规则

| 元素 | 规范 |
|------|------|
| 接口 | `EnumType`（继承 `Type`，自动满足 DP 契约） |
| 序列化 | Jackson 默认 {@code name()} — 枚举常量名即为持久化值 |
| 反序列化 | `@JsonCreator of(String)` — 委托 `ParseUtils.parseEnum()`，提供一致的异常语义 |
| 不可靠输入 | 编译器生成的 `valueOf(String)` 可直接使用（也抛 IAE） |
| 字段 | 仅 `private final String desc`，由 `@RequiredArgsConstructor` 生成构造器 |
| 访问器 | `desc()` — 通过 `@Getter @Accessors(fluent = true)` 生成 |
| `compareTo` | 使用 Java 内置的枚举比较（按声明顺序） |
| 包 | 所在模块的 `enums` 子包下（如 `com.soda.user.domain.enums`） |

### 限制

- 不能隐藏编译器生成的 `valueOf(String)` — 用 `of(String)` 作为 `@JsonCreator` 入口
- 枚举常量名即是持久化值，改名破坏 DB 数据（需 migration）
- 不可与普通 DP 共用 `parse(String)` 模式（枚举不能覆盖 `valueOf(String)`）
