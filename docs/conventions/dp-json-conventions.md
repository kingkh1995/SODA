---
type: Convention
title: DP JSON 与序列化规范
description: 设计或修改 DP 的 JSON/序列化行为时读——Jackson 3 模式总表、BigDecimal DP 模式、典型形态示例与 Serializable 约束。
tags: [ convention, dp, json, jackson ]
status: stable
---

# DP JSON 与序列化规范

字面量家族 DP（`StringLiteralType` 等，见 ADR-0028 字面量类型家族）的序列化入口继承自家族接口（`@JsonValue`）；反序列化：record
与单 public 构造器 class 零注解（Jackson 3.1.4 实证），带构造逻辑（缓存/单例/解析）的 class 显式 `@JsonCreator`。多属性 DP
独立声明序列化和反序列化入口（`@JsonCreator(PROPERTIES)` + `@JsonProperty`）。不依赖全局 Jackson 配置或隐式推断。

## 5.1 模式总表

| 实现形态                           | `@JsonValue`（序列化）                  | `@JsonCreator`（反序列化）                                         | Jackson 3 说明                                                                               |
|------------------------------------|-----------------------------------------|--------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| Record + 单字段（字面量家族）      | 继承自家族接口（record 隐式访问器即可） | —                                                                  | 零 Jackson 代码（ADR-0028；`@JsonValue` 不能在 record component 上，家族接口声明于抽象方法） |
| Record + 单字段（非家族，如集合）  | 显式覆写访问器并标注 `@JsonValue`       | —（典范构造器推断为 delegating）                                   | 标量/数组字面量（`UpdateMask`）；无 `@JsonValue` 时退化为对象形态 `{"field":…}`              |
| Record + 多字段                    | —                                       | `(mode = PROPERTIES)` 紧凑构造器或省略                             | Jackson 3 RecordDeserializer 能从典范构造器推断，`@JsonProperty` 冗余                        |
| Class + 单字段无缓存（字面量家族） | 继承自家族接口                          | —                                                                  | 零 Jackson 代码（单 public 构造器推断实证）                                                  |
| Class + 单字段有缓存（字面量家族） | 继承自家族接口                          | `(mode = DELEGATING)` 静态 `of(T)`                                 | private 构造器 Jackson 不可见，creator 必须显式                                              |
| Class + 多字段                     | —                                       | `(mode = PROPERTIES)` 静态 `of(...)`                               | 无变化                                                                                       |
| Enum                               | 无（Jackson 原生输出 `name()` 短名）    | `(mode = DELEGATING)` 静态 `of(String)`                            | 序列化输出为 `name()`（ADR-0005）；`@JsonCreator` 显式保留                                   |
| 密封继承基类（字面量家族）         | 继承自家族接口（基类声明，子类复用）    | 基类路由 `(mode = DELEGATING)` 静态 `of(String)`；子类也可各自声明 | sealed 类无需 `@JsonSubTypes`，Jackson 3 从 `permits` 子句自动发现                           |

| Java 类型      | JSON 类型                                         | 说明                                                                                                                                           |
|----------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `long` / `int` | 数字                                              | 标量                                                                                                                                           |
| `String`       | 字符串                                            | 标量                                                                                                                                           |
| `boolean`      | 布尔                                              | 标量                                                                                                                                           |
| `BigDecimal`   | 字符串                                            | 经 `toPlainString()` 输出，规范值为 String，BigDecimal 作为派生缓存；小数 DP `extends DecimalLiteralType`（ADR-0031）                          |
| `Instant`      | long（经 `EpochMilli` DP）/ ISO-8601 字符串（裸） | Jackson 3 默认 ISO-8601 字符串（`WRITE_DATES_AS_TIMESTAMPS` 默认 false）；`EpochMilli` DP 用 `@JsonValue long value()`（毫秒）覆盖（ADR-0031） |
| `Duration`     | ISO-8601 字符串                                   | Jackson 3 默认 ISO-8601（如 `PT5M`）                                                                                                           |
| enum           | 字符串                                            | Jackson 3 默认用 `name()` 序列化，`@JsonValue` 可选但必须在 public 方法上                                                                      |

> **Jackson 3 JSR-310 说明**：Jackson 3 内建 `java.time` 序列化支持，无需注册 `JavaTimeModule`。`Instant` 默认序列化为
> **ISO-8601 字符串**（`WRITE_DATES_AS_TIMESTAMPS` 默认 false；Jackson 2 为 true）；即便启用 timestamps，默认也是 epoch 秒 +
> 小数纳秒，非毫秒。`Duration` 默认 ISO-8601 字符串（如 `PT5M`）。绝对时间点领域字段用 `EpochMilli` DP（`@JsonValue long`
> 毫秒）显式覆盖，不受全局 feature 影响（ADR-0031）。
> 与 Jackson 2 中依赖 `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` 的配置不兼容。

## 5.2 BigDecimal DP 模式

BigDecimal 类型的 DP 使用 `class + Lombok`（非 record），规范值为 `String`（`toPlainString()` 格式）， BigDecimal 作为派生缓存字段，不参与
`equals`/`hashCode`/`@JsonValue`。

设计要点：

- **规范值 = String**：`@JsonValue` 挂在一个 `String` 返回方法上，输出 `toPlainString()`。 禁止科学计数法，禁止
  `@JsonFormat(shape = STRING)`、自定义序列化器、子 DP 等方式。
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

## 5.3 示例

Record 单字段模板见 [dp-conventions §2.2](../dp-conventions.md#22-record-模板)，Class
单字段有缓存模板见 [dp-conventions §2.3](../dp-conventions.md#23-class--lombok-模板)——本节只列 §2 未覆盖的形态。

Record 多字段：

```java
public record VerificationCodePolicy(
        @JsonProperty("codeLength") PositiveInt codeLength,
        @JsonProperty("expiry") Duration expiry,
        @JsonProperty("codeAlphabet") Alphabet codeAlphabet
) implements Type {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VerificationCodePolicy { …}
}
```

Enum（实现 EnumType：Jackson 原生序列化输出 name ()，与持久化短名一致）：

```java
public enum AuthAccountType implements EnumType {
    P("Password"), S("Sms"), E("Email"), O("OAuth");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountType of(String name) { …}
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
    }

    @Override
    public final String identifier() { …}

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

    public static PasswordAuthAccountId from(UserId userId) { …}

    @Override
    public AuthAccountType authAccountType() {
        return AuthAccountType.P;
    }

    @Override
    public int compareTo(PasswordAuthAccountId o) { …}
}
```

> **反序列化说明**：基类与子类各自有 `@JsonCreator`。Jackson 根据声明类型选择：
> 声明类型为基类 `AuthAccountId` 时走基类路由工厂按前缀分派；
> 声明类型为具体子类（如 `PasswordAuthAccountId`）时直接走子类入口。

## 5.4 `Serializable`

`Type` 接口不默认实现 `Serializable`。需要 JDK 序列化的 DP 显式 `implements Serializable` 并加
`@Serial serialVersionUID`。
