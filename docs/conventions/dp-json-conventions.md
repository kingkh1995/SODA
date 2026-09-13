---
type: Convention
title: DP JSON 与序列化规范
description: 设计或修改 DP 的 JSON/序列化行为时读——Jackson 3 模式总表（行键＝构造器可见性）、Java↔JSON 类型映射、BigDecimal DP 模式与禁止 Serializable 口径。P3C【强制/推荐/参考】三级标定 + ❌/✅ 反例正例。
tags: [ convention, dp, json, jackson ]
status: stable
---

# DP JSON 与序列化规范

本篇是 DP 序列化行为的单源：字面量家族 DP（`StringLiteralType` 等，见 ADR-0028 字面量类型家族）的序列化入口继承自家族接口
（`@JsonValue`）；反序列化按「构造器可见性」分型（§1）。 **取值原则**：`value()` 即线上值、`of` 入参类型 ≡ 序列化值类型（round-trip
成立）；不依赖全局 Jackson 配置或隐式推断。DP 设计总则见 [dp-conventions](../dp-conventions.md)；测试侧要求见
[dp-test-conventions](dp-test-conventions.md)。

## 1. 模式总表

下表各行均为【强制】； **行键＝构造器可见性**——形态决定 creator 能否被推断，缓存 / 单例只影响 `of` 内部实现，对 Jackson 不可观测。
密封两行的行键为 **边界声明类型**（判据见 [dp-conventions §1.3](../dp-conventions.md#13-继承形态场景判据)）——wire
形态只解释其成因，不作独立判据。

| 构造器可见性                                    | `@JsonValue`（序列化）                                         | `@JsonCreator`（反序列化）                                                | 说明                                                                                                                                     |
|-------------------------------------------------|----------------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Record + 单字段（字面量家族）                   | 继承自家族接口（record 隐式访问器）                            | 零注解（典范构造器）                                                      | 零 Jackson 代码（ADR-0028）；`@JsonValue` 标在 record component 上无效（Jackson 3.1.4 实测）                                             |
| Record + 单字段（非家族，如集合）               | 覆写访问器并标 `@JsonValue`（public 方法）                     | 零注解                                                                    | 先例 `UpdateMask`；无 `@JsonValue` 时退化为对象形态 `{"field": …}`                                                                       |
| Record + 多字段                                 | —                                                              | **零注解**（`@JsonProperty` 与 `mode = PROPERTIES` 均省略）               | Jackson 3 `RecordDeserializer` 按 component 名推断                                                                                       |
| Class + 单字段（private 构造器，含缓存 / 单例） | 继承自家族接口                                                 | `@JsonCreator(mode = DELEGATING)` 挂静态 `of(T)`                          | private 构造器 Jackson 不可见，creator 必须显式                                                                                          |
| Class + 多字段（private 构造器）                | —                                                              | `@JsonCreator(mode = PROPERTIES)` 挂静态 `of(...)` + 参数 `@JsonProperty` | 参数名不可推断 → 显式；**当前零存量样例**（覆盖缺口见 framework-type-contracts 样例覆盖矩阵）                                            |
| Enum                                            | 无（Jackson 原生输出 `name()` 短名）                           | `@JsonCreator(mode = DELEGATING)` 挂静态 `of(String)`                     | 序列化 `name()`（ADR-0005 枚举短名标识设计）                                                                                             |
| 密封继承 + 单值字面量族                         | 继承自家族接口（基类声明，子类复用）                           | **各子类** `of(String)`；基类不做反序列化入口                             | 边界声明具体子类（经实体类型变量解析）⇒ 基类路由零消费者；线形态自描述是解释（先例 `AuthAccountId`）                                     |
| 密封继承 + 对象形态族（判别字段 + 载荷）        | 属性承载（无家族接口）：判别属性在基类声明、载荷属性由子类提供 | **基类** `@JsonCreator(mode = PROPERTIES)` 对象入口，按判别字段路由子类   | 边界声明基类（聚合字段 / 两列持久化以其为界）⇒ 基类入口兼两列 restore 工厂（一途多界）；子类无自描述单值（先例 `VerificationRecipient`） |
| 不可反序列化类型                                | 无（不实现家族接口）                                           | `@JsonCreator(mode = DISABLED)` 构造器                                    | 判据「从 JSON 无法安全重建」；五要素见 [dp-conventions §1.4 形态模板](../dp-conventions.md#14-形态模板)                                  |

❌ `@JsonValue` 标在 record component 上（Jackson 3 不识别，静默退化为对象形态）：

```java
// ❌ 组件注解无效
public record UpdateMask(@JsonValue Set<String> fields) implements Type { }
// ✅ 覆写访问器（public 方法）并标注
@Override @JsonValue public Set<String> fields() { return fields; }
```

❌ 多字段 record 显式 `PROPERTIES` + `@JsonProperty`（冗余噪声，且会与 component 名漂移）：

```java
// ❌ 显式声明属性名
public record VerificationSource(@JsonProperty("scene") String scene, @JsonProperty("subject") String subject) { }
// ✅ 零注解，按 component 名推断
public record VerificationSource(String scene, String subject) implements Type { }
```

❌ private 构造器的 class 漏 creator（Jackson 不可见 → 反序列化失败）：

```java
// ❌ 无 creator，且构造器不可见
private ConcurrencyVersion(int value) { ValidateUtils.minValue(value, 0, true); this.value = value; }
// ✅ creator 显式挂主入口（校验仍在构造器）
@JsonCreator(mode = JsonCreator.Mode.DELEGATING) public static ConcurrencyVersion of(int value) { return cached(value); }
```

❌ 单值字面量族的密封基类做反序列化入口（该族边界全部声明具体子类，「声明类型为基类」的边界不存在，路由只是多一处维护面）：

```java
// ❌ 基类按前缀 switch 路由
@JsonCreator(mode = JsonCreator.Mode.DELEGATING) public static AuthAccountId of(String value) { /* 路由 */ }
// ✅ 子类各自声明入口（先例 PasswordAuthAccountId.of）
@JsonCreator(mode = JsonCreator.Mode.DELEGATING) public static PasswordAuthAccountId of(String value) { /* 解析 */ }
```

✅ 对象形态族（判别字段 + 载荷）反其道：基类 **就是**边界声明类型，故由基类承载对象入口、按判别字段路由子类——同一入口兼
两列 restore 工厂（一途多界，先例 `VerificationRecipient.of(channel, target)`；见 §1 模式总表密封行）。

❌ 枚举用 `@JsonValue` 输出 `desc`（破坏与持久化短名的一致性）：

```java
// ❌ 输出描述串（持久化短名与 JSON 值脱钩）
@JsonValue public String desc() { return desc; }
// ✅ 不声明 @JsonValue，Jackson 原生输出 name()
```

## 2. Java ↔ JSON 类型映射

| Java 类型      | JSON 类型                                                                                                                     | 说明                                                                                                                |
|----------------|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `long` / `int` | 数字                                                                                                                          | 标量                                                                                                                |
| `String`       | 字符串                                                                                                                        | 标量                                                                                                                |
| `boolean`      | 布尔                                                                                                                          | 标量                                                                                                                |
| `BigDecimal`   | 字符串                                                                                                                        | 经 `toPlainString()` 输出（规范值为 String、BigDecimal 为派生缓存；小数 DP `extends DecimalLiteralType`，ADR-0031） |
| `Instant`      | 领域时间点类型 DP 用 `EpochMilli`（long 毫秒；语义值经 `toInstant()` 派生）；多字段内嵌组件按 JSR-310 默认（ISO-8601 字符串） | `EpochMilli` 的 `@JsonValue long value()` 覆盖 Jackson 默认、不受全局 feature 影响（ADR-0031）                      |
| `Duration`     | ISO-8601 字符串                                                                                                               | Jackson 3 默认 ISO-8601（如 `PT5M`）；多字段 DP 内嵌组件按默认                                                      |
| enum           | 字符串                                                                                                                        | Jackson 3 默认用 `name()` 序列化；`@JsonValue` 若显式声明必须为 public 方法（private 被忽略）                       |

> **Jackson 3 JSR-310 说明**：Jackson 3 内建 `java.time` 序列化支持，无需注册 `JavaTimeModule`；`Instant` 默认 ISO-8601 字符串
> （`WRITE_DATES_AS_TIMESTAMPS` 默认 false——Jackson 2 为 true），`Duration` 默认 ISO-8601。 **界定（【强制】）**：仅「领域时间点类型
> DP」用
> `EpochMilli`（毫秒）显式覆盖，多字段 DP 的内嵌时间组件（如 `expiry`）按 JSR-310 默认，不外溢。与 Jackson 2 依赖
> `jackson-datatype-jsr310` 的配置不兼容。

## 3. BigDecimal DP 模式

BigDecimal 类 DP（`Percentage` / `WanYuan` 等）用 `class + Lombok`，规范值 `String`（`toPlainString()`），`BigDecimal` 为派生缓存，
不参与 `equals` / `hashCode` / `@JsonValue`。共享不变量收敛在 `DecimalLiteralType` 抽象基类——子类只提供 `SCALE` 与
`validate` 钩子。

```java
@EqualsAndHashCode(callSuper = true)
public final class WanYuan extends DecimalLiteralType implements Comparable<WanYuan> {

    private static final BigDecimal WAN = BigDecimal.valueOf(10000);
    private static final int SCALE = Math.clamp(TypeConfig.PROVIDER.wanYuanScale(), 0, 4);

    private WanYuan(BigDecimal raw) {
        super(raw, SCALE);          // 基类：notNull → maxScale → setScale(UNNECESSARY) → validate 钩子
    }

    /** 反序列化入口 —— 参数为 JSON String，内部解析 BigDecimal，不经 Jackson 类型强制。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WanYuan of(String value) {
        return new WanYuan(ParseUtils.parseBigDecimal(value));
    }

    /** 从 BigDecimal 构造（超精度即拒绝）。 */
    public static WanYuan from(BigDecimal value) {
        ValidateUtils.notNull(value);
        return new WanYuan(value);
    }

    /** 从元构造，显式舍入模式（转换层）。 */
    public static WanYuan fromYuan(BigDecimal yuan, RoundingMode roundingMode) {
        ValidateUtils.notNull(yuan);
        ValidateUtils.notNull(roundingMode);
        return new WanYuan(yuan.divide(WAN, SCALE, roundingMode));
    }
}
```

要点（各行为【强制】）：

- **规范值 = `String`**：`@JsonValue` 继承自家族接口（ADR-0028），输出 `toPlainString()`；禁科学计数法、禁
  `@JsonFormat(shape = STRING)`、禁自定义序列化器。
- **`BigDecimal` 为派生缓存**：不参与 `equals` / `hashCode` / `@JsonValue`（该不变量由 `DecimalLiteralType` 基类收敛）。
- **`@JsonCreator` 必须 `mode = DELEGATING`** 且参数为 `String`，内部走 `ParseUtils.parseBigDecimal`，不受 ObjectMapper
  配置影响。
- **构造链校验 + 归一化**：`ValidateUtils.maxScale(raw, scale)` 拒超精度、`setScale(UNNECESSARY)` 为防御断言（非舍入）；归一化幂等——
  `of(x).value()` 对同一数值 `equals` 恒成立。
- **舍入走显式转换层工厂**（`Percentage.from(BigDecimal, RoundingMode)` / `WanYuan.fromYuan(BigDecimal, RoundingMode)`
  ），构造器零舍入。

❌ BigDecimal DP 输出科学计数法 / 字段裸露 `BigDecimal`：

```java
// ❌ 序列化派生缓存：输出 1E+3 之类科学计数法，wire 不稳定
@JsonValue public BigDecimal value() { return decimalValue; }
// ✅ 规范值 String + toPlainString()（@JsonValue 继承自家族接口）
public String value() { return value; }
```

## 4. 示例

Record 多字段（零注解）：

```java
public record VerificationCodePolicy(
        PositiveInt codeLength,
        Duration expiry,
        Alphabet codeAlphabet
) implements Type {

    public VerificationCodePolicy {
        ValidateUtils.notNull(codeLength);
        ValidateUtils.range(codeLength.value(), 1, 20);
        ValidateUtils.minValue(expiry, Duration.ZERO, false);
        ValidateUtils.notNull(codeAlphabet);
    }
}
```

Enum（Jackson 原生 `name()`）：
```java
public enum AuthAccountType implements EnumType {
  P("password"), S("sms"), E("email"), O("oauth");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountType of(String name) {
        return ParseUtils.parseEnum(AuthAccountType.class, name);
    }
}
```

密封继承 · 单值字面量族（子类各自入口，基类不做反序列化入口；对象形态族由基类承载入口，见 §1 模式总表）：

```java
public abstract sealed class AuthAccountId implements Identifier<String>, StringLiteralType
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

  protected static final String DELIMITER = ":";

  private final String value;

  protected static String prefix(AuthAccountType type) {
    return type.name() + DELIMITER;
  }

  /** 规范串由基类从判别值 + payload 拼接，唯一拼写点。 */
  protected AuthAccountId(String payload) {
    ValidateUtils.hasText(payload);
    this.value = prefix(accountType()) + payload;
  }

  public abstract AuthAccountType accountType();
}

public final class PasswordAuthAccountId extends AuthAccountId implements Comparable<PasswordAuthAccountId> {

  private final UserId userId;

  private PasswordAuthAccountId(UserId userId) {
    super(String.valueOf(userId.value()));
    this.userId = userId;
  }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PasswordAuthAccountId of(String value) {
      var suffix = ParseUtils.cutPrefix(value, prefix(AuthAccountType.P));
      return new PasswordAuthAccountId(new UserId(ParseUtils.parseLong(suffix)));
    }

  public static PasswordAuthAccountId from(UserId userId) {
    ValidateUtils.notNull(userId);
    return new PasswordAuthAccountId(userId);
    }
}
```

> 声明类型为基类 `AuthAccountId` 的反序列化不再支持（零生产消费者）；`@JsonValue` 仍经基类继承输出前缀编码值（ADR-0007
> AuthAccountId 前缀编码）。

Record 单字段模板与 Class 单字段有缓存模板见 [dp-conventions §1.4 形态模板](../dp-conventions.md#14-形态模板)
——本节只列该节未覆盖的形态。

## 5. JDK 序列化

- 【强制】DP **禁止**实现 `java.io.Serializable`（含 `@Serial` / `serialVersionUID`）——JSON 是 DP 唯一的 wire 契约（见
  ADR-0031
  wire≠semantic 字面量：小数与绝对时间点）；JDK 序列化会引入第二契约，且本仓零消费者。

❌ DP 实现 `Serializable`（第二契约 + `serialVersionUID` 维护面）：

```java
// ❌ 显式 JDK 序列化
public final class LongId implements LongLiteralType, Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
// ✅ 不实现；JSON 契约由 @JsonValue / @JsonCreator 承载
public record LongId(long value) implements LongLiteralType { }
```
