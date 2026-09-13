---
type: Convention
title: Domain Primitive 设计规范
description: 新增或修改 Domain Primitive（DP）时读——形态选择（归族/形态/继承判据）、行为契约、工厂命名、富血/缓存/SPI 与枚举 DP；含写·检视双清单。P3C【强制/推荐/参考】三级标定 + ❌/✅ 反例正例。校验与归一化单源在 conventions/dp-validation-conventions.md。
tags: [ convention, dp ]
status: stable
---

# Domain Primitive 设计规范

目标读者：coding agent。本规范覆盖 DP 的完整决策路径： **语义归族 → 实现形态 → 行为契约 → 工厂命名 → 校验（指针）→
JSON（指针）→ 富血 / 缓存 / SPI → 枚举 → 新建流程**。级别标定用 P3C 三级【强制】/【推荐】/【参考】。

> **单源指针**：[dp-validation-conventions](conventions/dp-validation-conventions.md)（校验与归一化）、
> [dp-json-conventions](conventions/dp-json-conventions.md)（Jackson 模式总表与序列化）、
> [dp-test-conventions](conventions/dp-test-conventions.md)（DP 测试）。
> **关联规范**：[framework-conventions](framework-conventions.md)（框架契约与模块治理）、根目录
> [STYLEGUIDE](../STYLEGUIDE.md)（编码与注释）。

## 写清单

新增 DP 时按动作序逐条执行（括号内为落点）：

| #  | 动作                                                                                                                                                                                                                |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | 判语义归族：敏感 / 封闭集合 / 业务枚举 / BigDecimal 类 / 单属性字面量 / 多属性（§1.1）                                                                                                                              |
| 2  | 定实现形态：record 默认；需缓存·派生字段·私有构造器 → `final class` + Lombok（§1.2）                                                                                                                                |
| 3  | 有继承时按三形态判据选基类（§1.3）                                                                                                                                                                                  |
| 4  | 取模板起手（§1.4；枚举 §9、BigDecimal → [dp-json-conventions §3 BigDecimal DP 模式](conventions/dp-json-conventions.md#3-bigdecimal-dp-模式)）                                                                      |
| 5  | 不变式逐条落：不可变 / 自校验 / identity / 非空 / 不修理输入 / 可读 toString（§2.1）                                                                                                                                |
| 6  | 相等与调试按形态落注解（§2.2/§2.3；禁手写 `equals`；多态子类 `callSuper = true`）                                                                                                                                   |
| 7  | 敏感族按三层词汇选基类、实现 `maskedValue()`；`Masked*` 只走 `from`（§2.4）                                                                                                                                         |
| 8  | 入口：class 只给 `of(T)`；record 不给 `of`；跨类型用 `from`；`parse` 仅在需要时（§3.1）                                                                                                                             |
| 9  | 业务校验全部落构造器，收敛到 `ValidateUtils`（§4；[dp-validation-conventions §1 职责分层](conventions/dp-validation-conventions.md#1-职责分层)）                                                                    |
| 10 | 归一化只做不改语义的格式类；金额舍入用显式 `RoundingMode` 工厂（[dp-validation-conventions §4 归一化细则](conventions/dp-validation-conventions.md#4-归一化细则)）                                                  |
| 11 | JSON 入口按模式表取值；多字段 record 零注解；class 显式 creator；不实现 `Serializable`（[dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)）                                          |
| 12 | 需要自然顺序时实现 `Comparable<Self>`（附录 A）                                                                                                                                                                     |
| 13 | 富血方法 / 缓存 / SPI 按判据（§6/§7/§8；可配置常量入 SPI）                                                                                                                                                          |
| 14 | 建 UT：按形态覆盖必测分组、组名取单表；新增 DP 登记 `CrossTypeEqualityTest`（[dp-test-conventions](conventions/dp-test-conventions.md)）                                                                            |
| 15 | 在 [framework-type-contracts](conventions/framework-type-contracts.md)「DP 类型清单表」登记一行（表登记 `soda-component-domain-types` 公共 DP；业务域 DP 由样例覆盖矩阵举例，不逐行登记）；属样例的登记样例覆盖矩阵 |
| 16 | 完成判据：`build` 绿 + 本清单逐条 + 文末检视清单无未决项（§10）                                                                                                                                                     |

## 1. 形态选择

### 1.1 语义归族

先判语义族（决定基类 / 家族接口），再选实现形态（§1.2）：

- 【强制】敏感数据（PII 明文）→ 继承 `SensitiveValue` 基类（见 ADR-0030 数据保护 DP 四分类与 SensitiveValue 基类）；
  瞬态凭证 → `SecretValue`（§2.4）。
- 【强制】领域封闭的类型集合（取值集在领域内封闭、需穷尽分派）→ sealed 层级（形态判据见 §1.3）。
- 【强制】业务枚举 → 实现 `EnumType`（见 ADR-0005 枚举短名标识设计）；模板见 §9。
- 【强制】金额 / 百分比等小数类 → `final class` + Lombok，规范值 `String`、`BigDecimal` 作派生缓存（见 ADR-0031
  wire≠semantic
  字面量：小数与绝对时间点；模板见 [dp-json-conventions §3 BigDecimal DP 模式](conventions/dp-json-conventions.md#3-bigdecimal-dp-模式)）。
- 【强制】单属性字面量 → 实现对应字面量家族接口（`StringLiteralType` / `LongLiteralType` / `IntLiteralType` /
  `BooleanLiteralType` / `DoubleLiteralType`，见 ADR-0028 字面量类型家族）。
- 【强制】多属性 / 无标量语义 → 实现 `Type`（不实现家族接口）。
- 【强制】标识符 → 叠加 `Identifier<T>`（`T extends Comparable<T>` 约束底层值类型，与 DP 自身顺序无关；见 §2.1）。

❌ 敏感 PII 直接做成 record（脱敏依赖约定、泄露进日志与异常栈）：

```java
// ❌ record 自动生成的 toString 把明文 PII 写进日志/异常栈
public record Mobile(String value) implements StringLiteralType {
}

// ✅ 继承 SensitiveValue：toString 由基类 final 输出脱敏值，类型系统强制
public final class Mobile extends SensitiveValue { /* … */
}
```

### 1.2 实现形态

【强制】默认形态是 **record**；仅当需要 **缓存 / 派生字段 / 私有构造器**时才升为 `final class` + Lombok。

判据（依次回答）：

1. 是否需要缓存 / 派生字段 / 私有构造器？是 → `final class` + Lombok（模板见 §1.4）；否 → record（模板见 §1.4）。
2. 是否是标识符？是 → `implements Identifier<T>`；否 → `implements Type`（单属性字面量实现对应家族接口，见 §1.1）。
3. 是否有自然顺序？是 → 额外实现 `Comparable<Self>`（写法见附录 A）；否 → 不实现。
4. 【强制】不实现 `java.io.Serializable`——DP 禁止实现（口径与理由见 §5）。

【强制】class 形态 **构造器唯一**——第二入参形态一律以新工厂（`of` / `from` / `parse`）承载，不得新增构造器
（「单一构造路径」，§1.5 / §3.1；先例 `SoftwareVersion`：数值形态与线形态各自直达同一构造器）。唯一例外： **不可反序列化类型**
（§1.4）的唯一构造器可公开——其姿态以构造器为入口（`@JsonCreator(mode = DISABLED)`），无工厂可挂
（先例 `SecretValue`）。

【推荐】单属性字面量 DP 同时实现家族接口与 `Identifier<T>` 不互斥（先例 `UserId`）；多字段 DP 默认 record（class 多字段当前零实例，
其 creator 写法见 [dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)）。

❌ class 形态暴露 public 构造器（绕过缓存与单一构造路径）：

```java
// ❌ 缓存被绕过，任意实例可 new 出来
public ConcurrencyVersion(int value) { /* … */ }

// ✅ private 构造器 + static of(int) 作为唯一公开入口
private ConcurrencyVersion(int value) { /* … */ }
```

### 1.3 继承形态场景判据

【强制】先判类型集合是否 **领域封闭**（这是领域事实，不是技术偏好；判不准按开放处理），再选三形态之一：

- **封闭 + 穷尽分派 → sealed**：有共享存储状态 → `sealed abstract class`（先例 `AuthAccountId` 族）；无共享状态 →
  sealed interface + record（先例 `VerificationRecipient`）。
- **开放家族 + 共享不变量 → 非密封 abstract 基类**：`final` 方法锁不变量（先例 `SensitiveValue`、`DecimalLiteralType`）。
- 【强制】sealed 只承诺穷尽分派与编译期扩展守护， **不自动附带**前缀编码 / 集中反序列化路由（见 ADR-0007
  AuthAccountId 前缀编码）。
- 【强制】sealed 族的反序列化入口归属看 **边界声明类型**——wire 形态只解释其成因，不是判据。边界声明具体子类 → 入口下放各子类、
  基类零入口（先例 `AuthAccountId` 族：各边界经实体类型变量解析为具体子类，「声明类型为基类」的边界不存在，基类路由即零消费者
  维护面）；边界声明基类 → 基类承载入口（先例 `VerificationRecipient` 族：聚合字段即边界声明类型，同一入口兼两列 restore 工厂
  ——一途多界，不换 Jackson 原生 `@JsonTypeInfo`/`@JsonTypeName`）。见
  [dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)。

❌ 封闭集合用非密封基类 + `switch default` 抛 IAE（新增子类编译期不报错、运行期才落 default）：

```java
// ❌ 判别值 switch 用 default 兜底：新子类静默运行到 default 才炸
switch(recipient.channel()){
        case SMS -> …;
default ->throw new

IllegalArgumentException("unknown");
}

// ✅ sealed 层级 + 模式匹配：新增子类在编译期破坏分派，无 default
        switch(recipient){
        case
SmsRecipient r -> …;
        case
EmailRecipient r -> …;
        }
```

### 1.4 形态模板

**record 模板**（默认形态；标识符叠加 `Identifier<T>`）：

```java
package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.Identifier;      // 标识符 DP 叠加（可选，非互斥）
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

/**
 * Xxx DP —— 不可变、自校验。单属性字符串字面量，实现 {@link StringLiteralType}。
 * Jackson 集成零代码：@JsonValue 继承自家族接口（ADR-0028 字面量类型家族）。
 */
public record Xxx(String value) implements StringLiteralType {

    public Xxx {
      ValidateUtils.hasText(value);            // 校验唯一落点（§3.1）
      // 只做不改变值语义的归一化（如 Locale.ROOT 大小写）
    }

  /** 字符串解析入口；null 或非法值抛 IllegalArgumentException。仅当构造器参数不是 String 时需要。 */
  public static Xxx parse(String s) {
        return new Xxx(ParseUtils.parseXxx(s));
    }
}
```

标识符 DP：`implements` 追加 `Identifier<String>`，并加 `@Override public String identifier() { return value; }`（先例
`UserId`）。

非家族 record（多属性、无标量语义）不实现家族接口，序列化入口按
[dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)显式声明。

**class + Lombok 模板**（需要缓存 / 派生字段 / 私有构造器）：

```java
package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.IntLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Xxx DP —— 不可变、自校验、带缓存（可选）。单属性 int 字面量，实现 {@link IntLiteralType}。
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Xxx implements IntLiteralType {

  // ===== 可选：内联缓存（各 DP 自持，无缓存设施类，见 §7）=====
  private static final int CACHE_HIGH = Math.max(99, TypeConfig.PROVIDER.xxxCacheHigh());
  private static final Xxx[] CACHE = new Xxx[CACHE_HIGH + 1];
    static {
      for (int i = 0; i <= CACHE_HIGH; i++) {
            CACHE[i] = new Xxx(i);
        }
    }
    public static final Xxx INITIAL = CACHE[0];
  // =========================================================

    private final int value;

  private Xxx(int value) {                     // 有缓存/单例时 private，零注解推断不可用（§3.1）
    ValidateUtils.minValue(value, 0, true);
    this.value = value;
  }

  /** 规范值访问器 —— @JsonValue 继承自 {@link IntLiteralType}（ADR-0028）。 */
    public int value() {
        return value;
    }

  /** 主入口；**必须直达私有构造器**（缓存/单例分派是允许的中介，委托其他工厂禁止——§3.1）。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Xxx of(int value) {
      return 0 <= value && value <= CACHE_HIGH ? CACHE[value] : new Xxx(value);
    }

    /** 字符串解析入口。 */
    public static Xxx parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

  @Override
  public String toString() {
    return "Xxx[value=" + value + "]";       // class 形态格式锁死（§2.3）
  }
}
```

**密封继承模板**（单值字面量族：基类承载判别值与规范串渲染、不做反序列化入口；子类各自 `of(String)`；对象形态族见 §1.3；
先例 `AuthAccountId` 族）：

```java
/** 密封基类 —— 共享状态 value；相等性 callSuper 链保证 class-aware（§2.2）。 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public abstract sealed class XxxId implements Identifier<String>, StringLiteralType
        permits AId, BId {

  protected static final String DELIMITER = ":";   // 先例 AuthAccountId

  @EqualsAndHashCode.Include
  private final String value;

  /** payload 规范串由厂方法派生后传入——子类字段在 super 前未赋值，构造期不得读自身字段。 */
  protected XxxId(String payload) {
    ValidateUtils.hasText(payload);
    this.value = prefix(type()) + payload;         // 规范串唯一拼写点
  }

  /** 判别值的规范串前缀 —— 构造渲染与 of 的类型守卫同源。 */
  protected static String prefix(XxxType type) {
    return type.name() + DELIMITER;
  }

  /** 判别值（身份自描述；实体层判别不复用它，见先例 javadoc）。 */
  public abstract XxxType type();

  @Override
  public final String identifier() {
    return value;
  }
}

/** 子类 —— 反序列化入口下放至此；of 直达私有构造器，from 允许委托 of 或直达同一私有构造器。 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class AId extends XxxId implements Comparable<AId> {

  private final UserId userId;                     // payload：唯一事实源

  private AId(UserId userId) {
    super(String.valueOf(userId.value()));         // 派生串先于字段赋值
    this.userId = userId;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static AId of(String value) {
    var suffix = ParseUtils.cutPrefix(value, prefix(XxxType.A));   // 类型守卫
    return new AId(new UserId(ParseUtils.parseLong(suffix)));
  }

  public static AId from(UserId userId) {
    ValidateUtils.notNull(userId);
    return new AId(userId);
  }

  @Override
  public XxxType type() {
    return XxxType.A;
  }

  @Override
  public int compareTo(AId other) {
    return value().compareTo(other.value());
  }
}
```

**不可反序列化类型姿态**（判据：从 JSON 无法安全重建——秘密原值 / 瞬态输入载体）。唯一实例 `SecretValue`，要素：

- 【强制】不实现字面量家族接口（无可检测的序列化属性）；
- 【强制】无 `@JsonValue`；
- 【强制】构造器标 `@JsonCreator(mode = DISABLED)`——隐式 delegating 反序列化被显式关闭；
- 【强制】访问器命名避开 `value()`（防序列化框架自动发现，先例 `rawValue()`）；
- 【强制】序列化输出空对象 `{}`；`toString()` 全遮蔽属敏感族契约（§2.3 / §2.4）。

❌ record 补 `static of(...)`（零语义转发层，与「单一构造路径」相悖）：

```java
// ❌ of 与典范构造器完全同义，多一层转发即多一处漂移
public record VerificationSource(String scene, String subject) implements Type {
  public static VerificationSource of(String scene, String subject) {
    return new VerificationSource(scene, subject);
  }
}
// ✅ 典范构造器即唯一入口，调用方直接 new
new VerificationSource(scene, subject);
```

❌ 模板里顺手 `implements Serializable` + `@Serial`（JDK 序列化是第二契约，无消费方）：

```java
// ❌ 第二序列化契约 + serialVersionUID 维护面
public final class Xxx implements IntLiteralType, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
}

// ✅ 只保留 JSON 契约（@JsonValue / @JsonCreator），Serializable 相关全部不写
public final class Xxx implements IntLiteralType {
}
```

### 1.5 形态对比与设计理由

| 维度             | class 形态                                      | record 形态                                                        |
|------------------|-------------------------------------------------|--------------------------------------------------------------------|
| 构造器可见性     | `private`（封装缓存 / 单例 / 归一化）           | `public`（JDK 强制，典范构造器即公开入口）                         |
| 主入口           | `of(T)` 承载反序列化入口，可中介缓存 / 单例分派 | 典范构造器即主入口，零注解自动承载反序列化                         |
| `of(...)` 必要性 | 必须——构造器不可见，必须显式暴露入口            | 多余——典范构造器已是公开入口                                       |
| `from(...)` 角色 | 跨类型转换，委托 `of` 或直达构造器              | 跨类型转换，委托典范构造器                                         |
| 单一构造路径     | `of` 强制中介，防止绕过缓存 / 校验              | 典范构造器即单一路径；业务代码约定优先用 `from` / `parse` 语义入口 |

> **`SecretValue` 例外说明**：`SecretValue` 为 `final class` 但保留 public 构造器并显式关闭反序列化入口——这是防止凭证意外被
> 反序列化写入日志 / 存储的 **安全姿态**（§1.4），不属于「class 形态必须 private 构造器」规则的违例。

## 2. 核心行为契约

### 2.1 DP 不变式

下表各行均为【强制】：

| 维度                   | 规则                                   | 说明                                                                |
|------------------------|----------------------------------------|---------------------------------------------------------------------|
| 不可变                 | 所有字段 `final`                       | record 隐式保证；class 显式 `private final`；禁止包裹可变类型（§4） |
| 自校验                 | 构造时验证业务约束                     | 无效值不可表示；非法输入抛 `IllegalArgumentException`               |
| value-based identity   | `equals`/`hashCode` 只看规范值         | 不依赖对象身份；有派生字段时不纳入相等性（§2.2）                    |
| class-aware identity   | `equals`/`hashCode` 包含运行时类型检查 | 不同 DP 类型之间永不相等（§2.2）                                    |
| 非空类型               | DP 实例代表有效值                      | 可选场景用 `Optional<DP>`；工厂对 `null` 零容忍                     |
| 不修理输入             | 只归一化格式，不修复非法值             | 不自动 trim、不舍入、不把非法值改成合法值（§4）                     |
| 可读 toString          | `toString()` 输出调试字符串            | record 风格 `ClassName[field=value]`；敏感族按 §2.4 脱敏            |
| 单一构造路径           | 业务校验只落构造器                     | 工厂只做类型转换与预处理后委托构造器（§3.1）                        |
| 缓存不可观测（条件式） | 做缓存的 DP 只承诺值语义               | 调用方用 `equals`/`hashCode`/`compareTo`，禁止依赖 `==`（§7）       |

`Type` 是 DP 的根标记接口， **不继承 `Serializable`**。`Identifier<T>` 继承 `Type`，保留 `T extends Comparable<T>`
约束（标识符底层值
经常需要排序），但 DP 本身是否实现 `Comparable<Self>` 是可选的。【强制】DP 禁止实现 `java.io.Serializable`——单源口径见
[dp-json-conventions §5 JDK 序列化](conventions/dp-json-conventions.md#5-jdk-序列化)。

**派生字段（derived field）**：由唯一事实源按确定性函数得到的第二访问器—— **只读视图，不参与
`equals`/`hashCode`**，可即时计算或缓存备算（如 `WanYuan.decimalValue()`、`EpochMilli.toInstant()`；
字段级的口径见 §2.2「Class 含派生字段」行）。它带来的义务是 **唯一事实源必须真的是唯一的**：同一语义
若同时存在「字段」与「可推导该字段的输入」，两者必须由一个入口共同确定，不得各自可写（§3.1
「单一构造路径」在值内部的投影）；`AuthAccountId` 族即此形——`value` 由 payload 渲染而来，工厂永不以
入参原文充当规范值（见 ADR-0007 AuthAccountId 前缀编码）。

### 2.2 equals/hashCode

下表各行均为【强制】：

| 实现形态                            | 策略                                                                                                                                                                                                                    | 说明                                                                                                                        |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Record（单/多字段）                 | record 自动生成，所有 components 参与                                                                                                                                                                                   | 不需要手工编写；class 检查通过 `getClass()` 保证                                                                            |
| Class 单字段、无派生值              | `@EqualsAndHashCode`                                                                                                                                                                                                    | 隐式纳入唯一字段（即规范值）                                                                                                |
| Class 含派生/缓存字段               | `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` 在规范值上                                                                                                                           | 派生/缓存字段被排除                                                                                                         |
| 密封继承                            | 基类 `@EqualsAndHashCode`（比较 `value`），子类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)` 且不标注任何 `@Include`                                                                           | 仅比较基类 value + class 类型（`instanceof`），子类自身字段全部排除                                                         |
| 字面量敏感基类（SensitiveValue 族） | 基类 `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@Include` 在规范值上；具体子类**强制** `@EqualsAndHashCode(callSuper = true)`，canEqual 链逐层收窄即 class-aware；`SensitiveValueContractTest` 拦截子类漏标 | 子类漏标不报编译错，而是静默继承基类相等语义（跨类误等）——契约测试兜底；`toString`/`value()` 仍为手写 final（脱敏不可绕过） |
| Secret 子类                         | 引用相等（`Object.equals`/`Object.hashCode`），不暴露敏感值                                                                                                                                                             | 安全脱敏要求；不使用 Lombok 注解                                                                                            |
| Enum                                | JVM 枚举单例身份相等（`==` 等价）                                                                                                                                                                                       | 无需处理                                                                                                                    |

- 【强制】禁止手写 `equals`/`hashCode`——一律由 record 生成或 Lombok 注解生成；
  `CrossTypeEqualityTest` 充当「自定义 equals 漏 class 检查」的回归拦网。
- 【强制】多态（有子类的）DP 子类必须 `@EqualsAndHashCode(callSuper = true)`。
- 【强制】子类 `onlyExplicitlyIncluded = true` 的作用是 **排除子类自身所有字段**（配合 `callSuper = true` 只比较基类规范值）——该
  反直觉语义使子类派生字段（如 `userId`）不参与相等性计算。

❌ 手写 `equals`/`hashCode`（漏 class 检查 → 跨类型误等）：

```java
// ❌ 手写相等：漏 getClass() 检查，Mobile(1) 与 IdCard(1) 相等
@Override
public boolean equals(Object o) {
  return o instanceof Mobile m && m.value.equals(value);
}
// ✅ 注解生成（Lombok / record），class 检查由生成代码保证；禁止手写
@EqualsAndHashCode(callSuper = true)
```

❌ 多态子类漏 `callSuper`（退化为基类相等语义，跨子类误等）：

```java
// ❌ 漏 callSuper：只比较子类自身字段，且丢失基类 value 比较
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
// ✅ 继承基类比较链
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
```

❌ 含派生/缓存字段的 class 漏 `onlyExplicitlyIncluded`（派生值参与相等，破坏值语义）：

```java
// ❌ 派生缓存字段进入 equals
@EqualsAndHashCode
private final BigDecimal decimalValue;
// ✅ 显式标定参与字段，派生字段排除
@EqualsAndHashCode(onlyExplicitlyIncluded = true)   // 规范值字段上加 @Include
```

### 2.3 toString

下表各行均为【强制】：

| 实现形态                     | 规则                                                             | 示例                                      |
|------------------------------|------------------------------------------------------------------|-------------------------------------------|
| Record（所有字段）           | 不重写，使用 JDK 自动生成                                        | `LongId[value=42]`                        |
| Class 单字段                 | 手动：`"ClassName[value=" + value + "]"`                         | `ConcurrencyVersion[value=42]`            |
| Class 多字段                 | 手动：`"ClassName[field1=" + f1 + ", field2=" + f2 + "]"`        | `EmailContent[subject=Hello, body=World]` |
| 密封继承                     | 基类模板：`getClass().getSimpleName() + "[value=" + value + "]"` | `PasswordAuthAccountId[value=P:42]`       |
| SecretValue（独立 final 类） | 脱敏：`"SecretValue[***]"`，不得暴露内部值                       | `SecretValue[***]`                        |

- 【强制】 **class 形态格式锁死**：`ClassName[value=...]`（单字段）或 `ClassName[field1=..., field2=...]`（多字段），与 record
  自动生成严格
  一致；每个 class DP 必须有 `should_haveCorrectToString` 测试兜底（Locked）。
- 【强制】 **record 不得重写 `toString()`**：覆盖会偏离 record 风格基线并产生维护死角（删字段后覆盖方法不会自动跟上）；测试断言必须是
  `ClassName[value=...]`，不得断言 raw 值。
- 【强制】 **调试字符串不得被业务逻辑解析或比较**（全仓 `toString()` 消费点扫描可检视）。
- 【参考】密封层级统一在基类编写，子类不重写，借助 `getClass().getSimpleName()` 得到正确子类名。
- `SensitiveValue` 子类例外：`toString()` 由基类 `final` 化输出 `ClassName[masked=...]`（§2.4）；`SecretValue` 例外：输出
  `SecretValue[***]`。

❌ class 形态不重写 `toString()`（默认 `Object.toString` 无调试价值）：

```java
// ❌ 默认输出 ConcurrencyVersion@1a2b3c，字段值不可见
public final class ConcurrencyVersion implements IntLiteralType { /* 无 toString */
}

// ✅ 格式锁死为 record 风格
@Override
public String toString() {
  return "ConcurrencyVersion[value=" + value + "]";
}
```

❌ record 覆写 `toString()`（偏离自动格式、删字段后覆盖方法不跟上）：

```java
// ❌ 手写覆盖取代 JDK 自动格式
public record LongId(long value) implements LongLiteralType {
  @Override
  public String toString() {
    return "LongId(" + value + ")";
  }
}
// ✅ record 不写 toString，直接使用 JDK 生成的 LongId[value=42]
```

### 2.4 敏感数据族

三层词汇（见 ADR-0032 Masked Value DP 与脱敏命名）与四分类（见 ADR-0030 数据保护 DP 四分类与 SensitiveValue 基类）：

- 【强制】`SecretValue`（瞬态凭证，永不展示）→ `SensitiveValue`（长期 PII，脱敏后展示）→ `Masked*`（已脱敏落库形态）按泄漏面归层，
  三层不合并。

**`SensitiveValue` 基类**：

- 【强制】敏感数据 DP 必须继承 `SensitiveValue`（`domain.types`）：`value()` 与 `toString()` 为基类手写 `final`，脱敏输出不可被
  子类绕过；子类只实现 `maskedValue()`（基于 `value()` 的纯函数，无缓存）。
- 【强制】子类必须 `@EqualsAndHashCode(callSuper = true)`（§2.2）；`SensitiveValueContractTest` 拦截漏标。
- 【强制】脱敏不可绕过：子类不得覆写 `value()` / `toString()`。

**`Masked*` 记录家族**（`MaskedMobile` / `MaskedEmail` / `MaskedIdCard` / `MaskedBankCard` / `MaskedChineseName`）：

- 【强制】形态：record + `StringLiteralType`（脱敏值不敏感，不继承 `SensitiveValue`）；典范构造器以正则校验承重（落库值读回时
  不可篡改 / 非法）。
- 【强制】`from(原始值 DP)` 是唯一公开派生通道；`maskOf(String)` `private` 且与格式正则同址于各 `MaskedXxx` 内。这是
  **形态约定而非
  物理强制**（record 典范构造器 public，`new MaskedMobile(...)` 合法）——不新增机制。
- 【强制】每个 `MaskedXxx` 必须有成对原始值 DP（不接受孤儿掩码类）；原始值 DP 的 `maskedValue()` =
  `MaskedXxx.from(this).value()`。
- 【参考】原始值 DP 不缓存脱敏实例、不设 `redacted()`。

**哈希加密族**：

- `PasswordHash`（哈希族唯一 `SensitiveValue` 特例、有真实链路）与 `Ciphertext` / `Digest` + 端口 `Encryptor` /
  `Decryptor` /
  `Digester` 全部保留；无消费方的条目注记为「规范样例；无生产消费方」（见 ADR-0033 加密族与哈希族类型设计）。
- 【强制】算法词不出现在公共签名；端口入参必须为 DP（禁 `String` / 基本类型）。
- 【强制】 **零预建准入条件**——允许零消费类型当且仅当：①类型与端口成对；②判据已入 ADR；③落点具名；④位于 `component-types` 且承担
  某设计场景的规范样例（场景须登记于 [framework-type-contracts](conventions/framework-type-contracts.md) 样例覆盖矩阵）。

❌ `Masked*` 添 `of` 工厂或从脱敏串反推原值：

```java
// ❌ 派生通道多入口；❌ 从脱敏串反推原值（掩码不可逆是安全前提）
public static MaskedMobile of(Mobile mobile) { /* … */ }

public Mobile unmask() { /* … */ }

// ✅ 只留 from(原始值 DP)；反向不存在
public static MaskedMobile from(Mobile mobile) {
  return new MaskedMobile(maskOf(mobile.value()));
}
```

❌ `SecretValue` 开反序列化入口（凭证可被 JSON 注入、进入日志与存储）：

```java
// ❌ public creator：外部 JSON 可注入凭证
@JsonCreator
public SecretValue(String value) { /* … */ }

// ✅ 显式关闭：任何 JSON 绑定拒绝
@JsonCreator(mode = JsonCreator.Mode.DISABLED)
public SecretValue(String value) { /* … */ }
```

## 3. 构造与工厂命名

### 3.1 三层维护与两层入口

工厂方法按输入性质分三层，各层之间 **不可逆向委托**：

| 方法         | 语义                                              | 示例                                                                               |
|--------------|---------------------------------------------------|------------------------------------------------------------------------------------|
| `of(...)`    | 参数即底层规范值；反序列化入口类型 ≡ 序列化值类型 | `ConcurrencyVersion.of(1)`、`XxxEnum.of("A")`                                      |
| `from(...)`  | 参数 ≠ 底层值，跨类型转换                         | `PasswordAuthAccountId.from(UserId)`、`WanYuan.fromYuan(BigDecimal, RoundingMode)` |
| `parse(...)` | 不可靠输入（String / Object，外部来源）解析       | `UserId.parse("123")`、`UpdateMask.parse(raw, allowedFields)`                      |

| 层     | 方法                     | 输入                                              | 约束                                                                                                                                                                                                |
|--------|--------------------------|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 主入口 | `of(T)`                  | 规范值的精确表示（原语 / String）                 | 必须直达构造器（缓存 / 单例分派是允许的中介），不可委托其他工厂                                                                                                                                     |
| 转换层 | `from(T)` / `fromXxx(T)` | 可转换为规范值的其他类型（含包装 → 原语适配形态） | 类型转换 + 委托 `of` 或构造器；可保留前置守卫（避免拆箱 / 转换表达式 NPE），守卫与业务校验的边界单源见 [dp-validation-conventions §1 职责分层](conventions/dp-validation-conventions.md#1-职责分层) |

约束：

- 【强制】 **构造器＝唯一校验点**：所有业务校验只落构造器（record 紧凑构造器 / class private 构造器）；判据—— **去掉该检查，非法值
  还能不能构造出来？** 能 → 必须在构造器；不能（只是转换表达式会以错误类型爆炸）→ 允许留在转换入口。
- 【强制】`of(T)` **直达私有构造器**（缓存 / 单例分派是允许的中介）， **不得委托任何其他工厂**：反序列化行为不随转换层变更而漂移。
  `from` / `parse` 允许委托 `of` 或直达构造器（包装适配如 `from(Integer)` 优先委托 `of`，共享缓存分派；跨类型转换直达亦可）。
  `parse(Object)` 等上层入口委托 `of` 不在禁令内（它是「不可靠输入」的单一收敛点）。
- 【强制】 **原语优先**：DP 字段 / record component 与 `of`/`from`/`parse` 签名一律原语，禁包装类型（
  `ConcurrencyVersion.of(int)`）；
  调用方不得静默拆箱——实参静态类型是包装（`Integer`/`Long`/`Boolean`）时走 `from(包装)` 适配入口（`notNull` 守卫后委托
  `of`，按需补齐、不预建）。
- 【强制】`record` 不提供 `static of(...)`；`class` 不暴露 public 构造器（§1.2）。
- 【强制】`valueOf`（含编译器为枚举生成者）在生产代码不得暴露或调用；测试可将其作为 `of` 的对照。
- 【推荐】record 典范构造器 public 由 JDK 强制、无法物理阻止 `new`——业务代码优先用语义入口（`from` / `parse`），框架 / 序列化
  可直接走构造器。

❌ `of` 委托其他工厂（Jackson 反序列化路径不再单步、校验点漂移）：

```java
// ❌ of 转发 parse，反序列化路径多一跳
public static ConcurrencyVersion of(int value) {
  return parse(String.valueOf(value));
}

// ✅ of 直达构造器（缓存分派允许）
public static ConcurrencyVersion of(int value) {
  return cached(value);
}
```

❌ `@JsonCreator` 挂 `from` / `parse`（反序列化输入类型与规范值类型脱钩）：

```java
// ❌ creator 挂在转换层
@JsonCreator
public static PasswordAuthAccountId from(UserId userId) { /* … */ }

// ✅ creator 只挂主入口 of（class / enum）
@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
public static PasswordAuthAccountId of(String value) { /* … */ }
```

❌ `valueOf` 暴露 / 调用（第二套入口、绕过主入口语义）：

```java
// ❌ 生产代码用编译器生成的枚举 valueOf 当入口
UserState state = UserState.valueOf(raw);
// ✅ 统一走 of(String)（委托 ParseUtils.parseEnum）
UserState state = UserState.of(raw);
```

### 3.2 常量与便捷工厂

- 【参考】常量与便捷工厂按需暴露（`MIN` / `MAX` / `ZERO` / `ONE` / `now()` / `random()`），有无领域意义由设计者判断，规范不设闭集：
  数量 / 版本 / 金额型可给 `ZERO` / `ONE` / `MIN` / `MAX`；时间型按 `LocalDate` 惯例给 `now()` / `MIN` / `MAX`；随机生成型保留
  `random()` / `generate(...)`；纯标识符通常不需要 `UserId.ZERO`。
- 【参考】新增公共常量 / 便捷工厂 / 富血方法应有生产调用方或文档锚定（规范样例、ADR 语义）； **不追溯存量**（存量零调用成员保留）。
- 【强制】「无值」用 `Optional<DP>` 表达，不用 `Xxx.EMPTY` 单例——除非 `EMPTY` 本身是合法领域值。

❌ 用 `Xxx.EMPTY` 表达「无值」（把缺失值塞进类型系统，与 `Optional` 语义打架）：

```java
// ❌ EMPTY 不是合法领域值，却成为可传递的实例
public static final Username EMPTY = new Username("");
// ✅ 缺失用 Optional<Username> 表达
Optional<Username> username;
```

## 4. 校验与归一化

-

【强制】业务校验全部落构造器（唯一校验点），单源见 [dp-validation-conventions §1 职责分层](conventions/dp-validation-conventions.md#1-职责分层)。

- 【强制】DP 内单值校验禁止内联 `if/throw`，一律经 `ValidateUtils`；完整禁令与边界（含 DP 语义不变量）单源见
  [dp-validation-conventions §3 编排规则](conventions/dp-validation-conventions.md#3-编排规则)。
- 【强制】DP 字段必须不可变——下表各行均为【强制】：

| 禁止                                | 替代                                                   |
|-------------------------------------|--------------------------------------------------------|
| `byte[]`                            | 十六进制字符串 / Base64 `String` / 不可变 `ByteString` |
| `Date` / `Calendar`                 | `Instant` / `LocalDate` / `ZonedDateTime`              |
| `ArrayList` / `HashSet` / `HashMap` | `List.of` / `Set.copyOf` / `Map.copyOf`                |
| 可变领域对象                        | 提取为 DP 字段或设计成不可变                           |

❌ 字段用 `byte[]` / `Date` / 可变集合（外部可改内部状态，实例不再不可变）：

```java
// ❌ 数组引用可被调用方改写
public record Digest(byte[] value) implements Type {
}

// ✅ 不可变载体
public record Digest(String hex) implements StringLiteralType {
}
```

## 5. JSON 与序列化

- 取值原则：`value()` 即线上值（字面量家族继承 `@JsonValue`）、反序列化输入与规范值同型——round-trip 成立。
- 【强制】序列化 / 反序列化入口按 [dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)
  取值（行键＝构造器可见性）；
  不可反序列化类型姿态见 §1.4 / §2.4。
- 【强制】DP 禁止实现 `java.io.Serializable`
  ——口径与理由单源见 [dp-json-conventions §5 JDK 序列化](conventions/dp-json-conventions.md#5-jdk-序列化)。

## 6. 富血方法命名

命名口径均为【强制】；方法是否存在按需（§3.2）：

| 语义                  | 命名                    | 示例                                   |
|-----------------------|-------------------------|----------------------------------------|
| 修改字段返回新实例    | `withXxx(...)`          | `withYear(int)`                        |
| 加减 / 偏移           | `plusXxx` / `minusXxx`  | `plus(Duration)`、`minusDays(long)`    |
| 布尔查询              | `isXxx` / `hasXxx`      | `isAfter(EpochMilli)`、`isBefore(...)` |
| 转换（值 → JDK 类型） | `toXxx()`               | `toInstant()`、`toLongId()`            |
| 序列步进              | `next()` / `previous()` | `version.next()`                       |
| 派生键串              | `xxxKey()`              | `compositeKey()`                       |
| 展示串                | `toDisplayString()`     | `WanYuan.toDisplayString()`            |

- 【强制】值 → JDK 类型的转换一律 `toXxx()`，不得混用裸名词（先例 `EpochMilli.toInstant()`）。
- 【强制】单向派生的键串用名词性访问器 `xxxKey()`——非转换语义、 **禁止反解析**（先例 `VerificationSource.compositeKey()`，见
  ADR-0025 活跃验证唯一性：active_key 数据库硬保证）。
- 【强制】`toDisplayString()` 仅展示用途，禁止被业务逻辑解析（与 §2.3「调试串禁解析」同源）。
- 【强制】富血方法必须自包含、无副作用、不调用 gateway / service。
- 【参考】贫血包装器（如 `Avatar`、纯标识符 `UserId`）保持简洁。

## 7. 缓存

- 【强制】缓存不可观测：做缓存的 DP 只承诺 `equals` / `hashCode` / `compareTo`；调用方禁止依赖 `==`；
  **测试不得断言缓存单例身份**
  （`==` / `isSameAs`）。
- 【强制】仅 class DP 可做缓存——record 典范构造器 public，无法强制走缓存入口。
- 【强制】缓存上界经 SPI 暴露（§8），带下限保护。
- 【推荐】值域小且可预测、有明确性能收益时才做缓存。
- 缓存由各 DP **内联自持**（无缓存设施类），`of` 内越界回落 `new`（§1.4 模板）：

```java
private static final int CACHE_HIGH = Math.max(99, TypeConfig.PROVIDER.versionCacheHigh());
private static final ConcurrencyVersion[] CACHE = new ConcurrencyVersion[CACHE_HIGH + 1];

static {
  for (int i = 0; i <= CACHE_HIGH; i++) {
    CACHE[i] = new ConcurrencyVersion(i);
  }
}
```

❌ 测试断言缓存单例身份（把实现细节钉成契约，与「缓存不可观测」冲突）：

```java
// ❌ 钉死单例身份：缓存范围一改即测试碎
assertThat(ConcurrencyVersion.of(1)).isSameAs(ConcurrencyVersion.of(1));

// ✅ 只断言值语义
assertThat(ConcurrencyVersion.of(1)).isEqualTo(ConcurrencyVersion.of(1));
```

## 8. SPI 配置化常量

- 【强制】可配置常量一律经 `TypeConfigProvider` SPI 暴露，带默认值 + 下限 / 范围保护，DP 内以 `static final` 在类初始化期读取。
  可配置三类： **长度上限**（如 `smsContentMaxLength`）、 **精度 / 小数位**（如 `wanYuanScale`）、 **缓存上界**（如
  `versionCacheHigh`）。
- 【强制】以下 **不入 SPI**：领域不变量（值域 / 下限 / 最小熵，如 `PositiveInt ≥ 1`）、线协议格式常量（如 `SoftwareVersion` 段上界
  999，见 ADR-0020 SoftwareVersion 三段式软件版本号 DP）、正则 `Pattern`（编译成本）、协议字面量（如 `UpdateMask` 的通配符）。
- 【强制】SPI 是 component-types 公共 DP 常量的 **唯一拓展机制**：禁 `System.getProperty`、Spring 配置注入、静态
  setter（领域层零框架
  依赖）。
- 【强制】SPI 默认值不得低于原始设计下限——下限用 `Math.max` 保护，区间用 `Math.clamp` 保护。

```java
public interface TypeConfigProvider {
    default int versionCacheHigh() { return 99; }
}
```

```java
private static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());
```

❌ Spring 配置注入 / `System.getProperty` 替代 SPI（领域层沾框架、默认值无下限保护）：

```java
// ❌ 领域层依赖 Spring 配置
@Value("${soda.sms.max-length}")
private int maxLength;
// ✅ SPI + 下限保护（DP 内 static final 类初始化期读取）
private static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());
```

## 9. 枚举 DP

```java
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Xxx implements EnumType {

    A("Description A"),
    B("Description B");

    private final String desc;

  /** 反序列化入口 —— 校验与转换统一委托 ParseUtils.parseEnum。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Xxx of(String name) {
        return ParseUtils.parseEnum(Xxx.class, name);
    }
}
```

- 【强制】序列化用 Jackson 原生 `name()` 短名（与持久化短名一致），无需 `@JsonValue`；若显式声明，方法必须 public（Jackson 3
  忽略 private 方法上的 `@JsonValue`）。
- 【强制】反序列化入口 `of(String)` 一律委托 `ParseUtils.parseEnum`；编译器生成的 `valueOf(String)` 口径见 §3.1。
- 【强制】访问器 `desc()` 返回英文描述。
- 【强制】枚举 DP 与其它 DP **同包 `types`**（不设 `enums` 子包——`EnumType` 只是一族 DP）。
- 【强制】枚举常量名即持久化值，改名需 migration。

❌ 用编译器生成的 `valueOf(String)` 当公开入口、枚举放 `enums` 子包：

```java
// ❌ package …domain.enums;  + UserState.valueOf(raw) 作反序列化入口
// ✅ package …domain.types;  + UserState.of(raw) 委托 ParseUtils.parseEnum
```

## 10. 新建 DP 流程

形态决策见 §1（两段式：语义归族 → 实现形态）；动作逐条见文首[写清单](#写清单)。

**完成判据**（全部满足方算完工，任一不满足即未完工）：

1. `:soda-components:soda-component-domain-types:build` 通过（无编译 / 测试失败）；
2. 文首「写清单」逐条满足；
3. 文末「检视清单」无未决项；
4. [framework-type-contracts](conventions/framework-type-contracts.md)「DP 类型清单表」新增一行且 types 包内无遗漏
   DP（表登记 `soda-component-domain-types` 公共 DP；业务域 DP 由样例覆盖矩阵举例，不逐行登记）；属规范样例的登记
   样例覆盖矩阵。

## 附录 A：`compareTo` 编写规范

只在有自然顺序时实现（标识符、版本号、长度等）；不实现即不声明 `Comparable<Self>`。三种字段形态的写法：

```java
// 对象类型单字段（如 Uuid）
@Override
public int compareTo(Uuid other) {
    return this.value.compareTo(other.value);
}

// 基本类型单字段（如 ConcurrencyVersion）
@Override
public int compareTo(ConcurrencyVersion other) {
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

| DP                             | 实现                  | `Comparable`     | 缓存                                 |
|--------------------------------|-----------------------|------------------|--------------------------------------|
| `LongId`                       | record                | ✅               | 无                                   |
| `Uuid`                         | record                | ✅               | 无                                   |
| `Fen`                          | record                | ✅               | 无                                   |
| `Mobile` / `Email` / `WanYuan` | class / class / class | ❌（无领域顺序） | 无（WanYuan 缓存 BigDecimal 派生值） |
| `ConcurrencyVersion`           | class                 | ✅               | `[0, 99]`                            |
| `SoftwareVersion`              | class                 | ✅               | 无                                   |

## 检视清单

按分组逐条核对（每条可判定；对应规则见括号内落点）：

### 代码

1. 归族与形态一致（§1）：敏感值继承 `SensitiveValue`；封闭集合用 sealed；枚举实现 `EnumType`（§1.1/§1.3）
2. 无「record 提供 `of(...)`」「class 暴露 public 构造器」（唯一例外＝§1.4 不可反序列化类型的公开构造器；§1.2/§3.1）
3. 业务校验只在构造器；class 私有构造器唯一（§1.2）；构造器内零 `ParseUtils`；`of` 直达构造器、不委托其他工厂（§3.1；
   [dp-validation-conventions §1 职责分层](conventions/dp-validation-conventions.md#1-职责分层)）
4. 生产代码无 `valueOf` 暴露或调用（§3.1）
5. 无手写 `equals`/`hashCode`；多态子类 `@EqualsAndHashCode(callSuper = true)`（§2.2）
6. `toString` 为 record 风格；record 无覆写；敏感族输出 `masked`（§2.3）
7. `Masked*` 无 `of`；`SecretValue` 未开反序列化入口（§2.4；
   [dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)）
8. 无 `byte[]` / `Date` / 可变集合字段（§4）
9. 归一化无 trim / 隐式舍入；金额舍入走显式 `RoundingMode`
   工厂（[dp-validation-conventions §4 归一化细则](conventions/dp-validation-conventions.md#4-归一化细则)）
10. JSON 注解落位与 [dp-json-conventions §1 模式总表](conventions/dp-json-conventions.md#1-模式总表)逐行一致（含多字段
    record 零注解）
11. 未实现 `Serializable`（§5）
12. 可配置常量已入 SPI；无 Spring 配置 / 静态 setter / `System.getProperty`（§8）
13. 缓存透明、不可观测；无 `==` 语义泄漏（§7）
14. 枚举与其它 DP 同包 `types`；`of(String)` + `desc()`（§9）
15. javadoc 与实现一致（可比较性、脱敏承诺等）

### 测试

16. 必测分组按 [dp-test-conventions](conventions/dp-test-conventions.md)「必测分组」全覆盖（名 + 内容），组名取该篇单表（无自造、无适用形态外多建）
17. 无身份断言（`isSameAs` / `isNotSameAs`）；缓存组为边界三元组值等价
18. `Serialization` 三断言齐（形状 / round-trip / 非法拒绝）；共享 `MAPPER`，无 `new ObjectMapper()`
19. 无零断言 / 恒真 / STUB 回声；异常断言用 `hasMessageContaining`
20. 新增 DP 已登记 `CrossTypeEqualityTest` 清单 + 敏感契约套件同步
21. 标准形态 DP 的测试类声明 `Contract` 样例，不重复声明已折叠分组（`序列化` / `调试` / `相等性` / `比较` 由
    [dp-test-conventions §1.1](conventions/dp-test-conventions.md#11-record--class-dp不含缓存) 契约基类继承；重复声明 ＝
    双源 / 形式过标）

### 登记

22. [framework-type-contracts](conventions/framework-type-contracts.md)「DP 类型清单表」有本类型一行且契约句准确（表登记
    `soda-component-domain-types` 公共 DP；业务域 DP 由样例覆盖矩阵举例，不逐行登记）；样例已登记样例覆盖矩阵
23. ADR 锚点有效；无被删机制（缓存设施类、`VerificationSource.of` 等）的残留引用与文档专名

