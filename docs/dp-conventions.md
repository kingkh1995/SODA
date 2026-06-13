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
| 示例 | `LongId`, `StringId`, `UserId` | `Email`, `Money`, `Age` |

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

## 命名与包结构

| 元素 | 规范 | 示例 |
|---|---|---|
| 包 | `com.soda.component.support.types` | — |
| 类名 | 名词 + `Id`（标识符）或领域名词 | `LongId`, `UserId`, `Money`, `Email` |
| 工厂方法 | 单一 `valueOf(Object)` 委托 `ParseUtils` 转换 | — |
| Accessor | record 组件（如 `value()`），被 `@JsonValue` 标注 | — |

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

类型匹配发生在 `ParseUtils` 内部，DP 不关心分发细节。DP 的 `valueOf` 只做两件事：
委托转换、委托构造。

针对数值型转换，`ParseUtils.parseLong(Object)` 使用 `case Number n` 统一处理所有数字类型（Integer、Long、Double、BigDecimal 等），不需要为每个数字子类型写单独 case：

```java
public static long parseLong(Object o) {
    ...
    return switch (o) {
        case Number n -> n.longValue();   // 所有数字类型
        case String s -> Long.parseLong(s.trim());
        default -> throw ...;
    };
}
```

针对字符串型转换，`ParseUtils.parseString(Object)` 直接调用 `toString()`：

```java
public static String parseString(Object o) {
    ...
    return o.toString();  // 任意 Object → String
}
```

优势：一个入口，调用方不必关心类型转换；DP 本身不操心 null 检查，全部委托给工具类。

## Jackson 集成

Record 的典范构造器被 Jackson 自动识别（Jackson 2.12+ 原生支持），不需要额外注解标注反序列化入口。

| 用途 | 注解 | 位置 |
|---|---|---|
| 序列化 | `@JsonValue` | record 组件（如 `@JsonValue long value`）或对应访问器方法 |
| 反序列化 | 无需（Jackson 自动识别 record 典范构造器） | — |

## 对比一致性

LongId 与 StringId 对比如下，创建新 DP 时应保持同一种风格：
| 检查项 | LongId | StringId |
|---|---|---|
| 构造校验 | `ValidateUtils.minValue(…)` | `ValidateUtils.nonBlank(…)` |
| `valueOf` 入口 | `new LongId(ParseUtils.parseLong(value))` | `new StringId(ParseUtils.parseString(value))` |
| `@JsonValue` | record 组件 `value` | record 组件 `value` |
| 引用 `IllegalArgumentExceptions` | 否 | 否 |
| `toString()` 格式 | `"LongId[value=…]"` (record 默认) | `"StringId[value=…]"` (record 默认) |

## 创建新 DP 的步骤

1. 在 `com.soda.component.support.types` 包下创建 record
2. 选模板：标识符 → `implements Identifier<T>`；非标识符 → `implements Type`
3. record 紧凑构造中通过 `ValidateUtils` 做输入校验，领域格式校验内联
4. 添加单一 `valueOf(Object)` 工厂方法，委托 `ParseUtils.parseXxx(value)` 做类型转换
5. 如需要新校验类型，先在 `ValidateUtils` 中添加通用方法
6. 如需要新类型转换，先在 `ParseUtils` 中添加 `parseXxx(Object)` 方法
7. 运行 `:soda-components:soda-component-support:build` 验证
