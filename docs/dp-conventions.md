# Domain Primitive 编写规范

## 总则

所有 Domain Primitive（领域原语）必须满足：

- **不可变** — 所有字段 `private final`
- **自校验** — 构造时验证业务约束，无效值不生成实例
- **可序列化** — 实现 `Serializable`
- **可比较** — 实现 `Comparable`（ID 类型通过 `Identifier` 默认实现，非标识符 DP 需自行实现 `compareTo(Type)`）

## 标识符 vs 非标识符 DP

| 区别 | 标识符 DP | 非标识符 DP |
|---|---|---|
| 实现接口 | `Identifier<T>` | `Type` |
| `compareTo` | 默认实现来自 `Identifier` | 需自行实现 `compareTo(Type)` |
| 领域方法 | 无（仅 `identifier()`） | 按领域提供（如 `Email.localPart()`） |
| 示例 | `LongId`, `UUId`, `UserId` | `Email`, `Money`, `Age` |

## 类结构模板（标识符 DP — record 风格）

JDK 16+ 起优先使用 `record` 实现标识符 DP。Record 自动提供不可变字段、`equals()`/`hashCode()`、`toString()` 和序列化合规。

```java
public record XxxId(@JsonValue T value) implements Identifier<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    public XxxId {                                              // 紧凑构造：校验归一化
        // 校验 → 通过 ValidateUtils
    }

    /** 从不可靠输入构造，null 或非法值时抛出 IllegalArgumentException。 */
    public static XxxId valueOf(Object value) {
        return new XxxId(ParseUtils.parseXxx(value));           // ParseUtils 内部处理 null + 类型转换
    }
}
```

## 类结构模板（非标识符 DP — record 风格）

```java
public record Xxx(@JsonValue String value) implements Type {

    @Serial
    private static final long serialVersionUID = 1L;

    public Xxx {                                                // 紧凑构造：校验归一化
        ValidateUtils.nonBlank(value);
        ValidateUtils.matches(PATTERN, value);
    }

    /** 从不可靠输入构造，null 或非法值时抛出 IllegalArgumentException。 */
    public static Xxx valueOf(Object value) {
        return new Xxx(ParseUtils.parseString(value));          // ParseUtils 内部处理 null + 类型转换
    }

    @Override
    public int compareTo(Type other) {
        return value.compareTo(((Xxx) other).value);
    }
}
```

## 类结构模板（非标识符 DP — class + Lombok 风格）

当 record 无法满足需求时（如需要缓存实例），使用普通 class + Lombok 注解代替 record。
通过 `@Accessors(fluent = true)` 保持访问器命名与 record 一致（`value()` 而非 `getValue()`）。
`toString()` 手写为 record 方括号格式 `"Xxx[value=…]"`。

```java
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(fluent = true)
public final class Version implements Type {

    @Serial
    private static final long serialVersionUID = 1L;

    // 缓存示例（参考 Integer 缓存设计）
    private static final Version[] CACHE = new Version[100];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new Version(i);
        }
    }

    public static final Version PRIMARY = CACHE[0];

    @Getter
    @JsonValue
    private final int value;

    @JsonCreator                                                   // class 需要显式 @JsonCreator
    public static Version of(int value) {
        ValidateUtils.minValue(value, 0, true);
        if (value < CACHE.length) {
            return CACHE[value];
        }
        return new Version(value);
    }

    public static Version valueOf(Object value) {
        return of(ParseUtils.parseInt(value));
    }

    public Version next() {
        return of(value + 1);
    }

    @Override
    public int compareTo(Type other) {
        return Integer.compare(this.value, ((Version) other).value);
    }

    @Override
    public String toString() {                                     // 手写，对齐 record 方括号格式
        return "Version[value=" + value + "]";
    }
}
```
## 命名与包结构

| 元素 | 规范 | 示例 |
|---|---|---|
| 包 | `com.soda.component.support.types` | — |
| 类名 | 名词 + `Id`（标识符）或领域名词 | `LongId`, `UserId`, `Money`, `Email` |
| 工厂方法 | 单一 `valueOf(Object)` 委托 `ParseUtils` 转换 | — |
| Accessor | `value()`（record 自动，class 通过 `@Accessors(fluent = true)`） | — |

## 职责分层

| 层次 | 定位 | 规则 | 是否引用 `IllegalArgumentExceptions` |
|---|---|---|---|
| `ParseUtils` | 类型转换工具 | 将不可靠 `Object` 输入转为基础类型（long/String），含 null 检查 | 是（复用异常消息） |
| `ValidateUtils` | 校验工具 | 基础参数校验，失败抛出 IAE | 是（复用异常消息） |
| DP 构造器 | 业务校验 + 归一化 | 使用 `ValidateUtils` 校验，不做类型转换 | 否 |
| DP `valueOf` | 外来输入入口 | 委托 `ParseUtils` → 委托构造器，不重复 null 检查 | 否 |

## 校验收敛原则

校验代码必须通过通用工具类，禁止内联：

| 校验类型 | 工具类 | 方法 |
|---|---|---|
| Null 检查 | `ParseUtils` | `parseXxx` 内部自带 |
| Blank 检查 | `ValidateUtils` | `nonBlank(value)` |
| 数值范围 | `ValidateUtils` | `minValue(value, min, inclusive)` |
| 格式匹配 | `ValidateUtils` | `matches(pattern, value)` |

## 异常消息

- 英文，首字母小写，无句号
- 异常消息统一通过 `IllegalArgumentExceptions` 工厂方法生成
- DP 类不直接引用 `IllegalArgumentExceptions`，消息由 `ValidateUtils` / `ParseUtils` 带入

## 类型分发策略

通过单一 `valueOf(Object)` 工厂方法接受不可靠输入，内部流程如下：

```
valueOf(Object)
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
| 实现接口 | `Identifier<Long>` | `Identifier<String>` | `Type` | `Type` |
| 构造校验 | `minValue(…, 0, false)` | `nonBlank(…)` + `matches(uuidPattern, …)` | `notNull(…)` + `minValue(…, 0, true)` + `maxScale(…, 2)` | `minValue(…, 0, true)` |
| `valueOf` 入口 | `ParseUtils.parseLong(value)` | `ParseUtils.parseString(value)` | `ParseUtils.parseBigDecimal(value)` | `ParseUtils.parseInt(value)` |
| `@JsonValue` | record 组件 `value` | record 组件 `value` | record 组件 `value` | 字段 `value` |
| `@JsonCreator` | 无需 | 无需 | 无需 | `of(T)` 显式 |
| 引用 `IllegalArgumentExceptions` | 否 | 否 | 否 | 否 |
| `toString()` 格式 | `"LongId[value=…]"` | `"UUId[value=…]"` | `"WanYuan[value=…]"` | `"Version[value=…]"` 手写对齐方括号 |
| 访问器来源 | record 自动 | record 自动 | record 自动 | `@Accessors(fluent = true)` |
| 缓存 | 无 | 无 | 无 | [0, 99] 缓存实例 |
| 随机生成 | 无 | `random()` | 无 | 无 |

## Jackson 集成

Record 的典范构造器被 Jackson 自动识别（Jackson 2.12+ 原生支持），不需要额外注解标注反序列化入口。
Class + Lombok 风格的 DP 需显式标注 `@JsonCreator` 在 factory method 上。

| 实现方式 | 序列化 | 反序列化 |
|---|---|---|
| record | `@JsonValue` 在 record 组件上 | 自动（典范构造器） |
| class + Lombok | `@JsonValue` 在字段上 | `@JsonCreator` 在 `of(T)` 上 |

## 创建新 DP 的步骤

### record 风格（默认）

1. 在 `com.soda.component.support.types` 包下创建 record
2. 选模板：标识符 → `implements Identifier<T>`；非标识符 → `implements Type`
3. record 紧凑构造中通过 `ValidateUtils` 做输入校验，领域格式校验内联
4. 添加单一 `valueOf(Object)` 工厂方法，委托 `ParseUtils.parseXxx(value)` 做类型转换
5. 如需要新校验类型，先在 `ValidateUtils` 中添加通用方法
6. 如需要新类型转换，先在 `ParseUtils` 中添加 `parseXxx(Object)` 方法
7. 运行 `:soda-components:soda-component-support:build` 验证

### class + Lombok 风格（record 不适用时）

当需要缓存实例或无法使用 record 时，采用 class + Lombok 风格。替代步骤：

1. `@EqualsAndHashCode` + `@RequiredArgsConstructor(access = AccessLevel.PRIVATE)` + `@Accessors(fluent = true)`
2. `@Getter` 在 `value` 字段上，`@JsonValue` 也在字段上
3. `@JsonCreator` 在静态 `of(T)` 工厂方法上（class 不能自动识别）
4. `toString()` 手写为方括号格式（如 `"Version[value=5]"`）
5. 其余步骤同 record 风格
