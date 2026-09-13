---
type: Convention
title: Domain Primitive 测试规范
description: 写 DP 测试时按形态取必测分组（§1）、@Nested 分组名（§2）与模板（§4），并按 §5 检视清单核对覆盖。通用写法与分层映射单源在 test-conventions。P3C【强制/推荐/参考】三级标定 + ❌/✅ 反例正例。
tags: [ convention, testing, dp ]
status: stable
---

# Domain Primitive 测试规范

> **单源指针**：通用写法（断言 / 命名 / 分组 / 数据 / 参数化 / mock 边界）与分层映射单源在
> [test-conventions](../test-conventions.md)；DP 序列化模式（创建器注解 / 类型映射）单源在
> [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)——本文只指不述。本文只收 DP 必测分组、`@Nested` 分组名与
> DP 专属写法。

**管辖面**（【强制】）：实现 `com.soda.component.domain.Type` 的 DP。Entity / Aggregate（`AuthAccount` 族、`User`、`Verification`
）不在本文管辖
——按 [test-conventions](../test-conventions.md) 分层映射「聚合 / 领域服务按行为」分组，其组名不判过标。

## 1. 必测分组：按 DP 形态

### 1.0 适用范围与判定口径

- 【强制】 **名 + 内容双强制**：必测行 ＝「组名 + 该组必须覆盖的内容」；内容齐但并入他组 ＝ 缺项（不接受「校验并入
  `Constructor`」）。
- 【强制】组名一律取自 §2 单表；表外自造 ＝ 偏差（需新组名 ＝ 先改本规范）。
- 【强制】覆盖判据 ＝ §5 检视清单；终验由一次性脚本产出覆盖矩阵（不引入反射自检）。

❌ 把拒绝类用例塞进 `Constructor`（内容齐但组名错位，覆盖核对无法逐行落组）：

```java
// ❌ 拒绝用例占位「构造」组
@Nested @DisplayName("构造") class Constructor { /* 混入 null / 范围拒绝 */ }
// ✅ 按内容归组：拒绝类用例进「校验」
@Nested @DisplayName("校验") class Validation { /* null / blank / 边界 / 格式拒绝 */ }
```

### 1.1 Record / Class DP（不含缓存）

下表各行均为【强制】：

| 分组             | 必须覆盖                                                                                                      |
|------------------|---------------------------------------------------------------------------------------------------------------|
| `Constructor`    | 合法值创建（定义性路径：record 典范构造器 / class 主入口 `of` / `parse` 合法串 / 以原值或字段构成的 `from`）  |
| `Validation`     | 非法输入拒绝：null、blank（String）、范围 / 格式边界、`parse` 非法串（仅 DP 有 `parse` 时）                   |
| `Equality`       | 相同值相等、不同值不等、hashCode 一致                                                                         |
| `Debug`          | toString **精确全串**（`ClassName[field=value]`；嵌入 DP 按其自身 toString；多字段全字段）                    |
| `Serialization`  | ① 序列化形状（wire 形态：裸标量 / JSON 对象，精确串或键集）② round-trip 等价 ③ 非法 JSON 拒绝（三断言分方法） |
| `ComparableTest` | 仅 `Comparable` DP：`compareTo` 与 `equals` 一致                                                              |

- 多字段 DP：序列化为 JSON 对象形态 `{"f1":…}`；创建器注解与 class 多字段写法单源见
  [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)（class 多字段现零样例，待首个样例验证）。
- BigDecimal DP：规范值 `String`、JSON 值为字符串；归一化幂等用例归 `Normalization`。
- 【强制】`from` 落组判据： **建值**（由原值或字段构成自身，如 `EpochMilli.from(Instant)`、`AuthAccountId` 族的 `from(...)`）→
  `Constructor`； **转换**（`fromXxx` / `toXxx` / 包装适配 `from(包装类型)` / DP↔DP 伴生转换如 `Masked*.from(X)`）→
  `Conversion`。
- 【强制】 **契约套件承载**：`Equality` / `Debug` / `Serialization`（`Comparable` DP 另加 `ComparableTest`）由 test fixtures
  的契约
  基类统一实现，测试类只声明样例、不再复制分组骨架——`DomainPrimitiveContractTest` /
  `ComparableDomainPrimitiveContractTest`
  （枚举用 `EnumDomainPrimitiveContractTest`，见 §1.5）位于
  `soda-components/soda-component-domain-types/src/testFixtures/java/com/soda/component/domain/testutil/`；组件模块测试与下游模块
  （`soda-user-domain`）测试共用同一份（下游经 `testImplementation(testFixtures(...))` 取得）。组名、display name 与断言深度仍以本文
  §2 / §3 为准——契约基类是它们的 **实现载体**，不是例外。类型专属分组（`Constructor` / `Validation` / `Conversion` /
  `Masking` /
  `RichMethods` …）仍留在测试类内手写。

### 1.2 Record / Class DP（含缓存）

同 §1.1，追加：

| 分组    | 必须覆盖                                                                                                                                                                                                                                                                                 |
|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Cache` | 缓存透明性：边界三元组（下界 / 上限 / 上限+1）值等价（`equals` / `hashCode` / 序列化一致）；上限仅对声明实例缓存的 DP 适用，经 `TypeConfig.PROVIDER.<类型>CacheHigh()` 读取（当前仅 `ConcurrencyVersion` / `versionCacheHigh()`）；**禁止**身份断言（`==` / `isSameAs` / `isNotSameAs`） 

【参考】无边界常量的 DP（如 `Active` 的 TRUE/FALSE）不建 `Cache` 组。

❌ `Cache` 组断言缓存单例身份（把实现细节钉成契约）：

```java
// ❌ 钉死单例身份：缓存范围一改即测试碎
assertThat(ConcurrencyVersion.of(1)).isSameAs(ConcurrencyVersion.of(1));
// ✅ 边界三元组值等价
assertThat(ConcurrencyVersion.of(1)).isEqualTo(ConcurrencyVersion.of(1));
```

### 1.3 密封族

同 §1.1，追加：

| 分组       | 必须覆盖                                                                                                                                                                                   |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Routing`  | 以本族**边界声明类型**为入口断言路由：单值字面量族＝子类（`XxxId.of("<前缀>:<键>")` 返回本子类；JSON 以子类类型反序列化）；对象形态族＝基类（两参工厂返回子类实例；JSON 以基类型反序列化） |
| `Equality` | 子类等值相等 + hashCode 一致（跨子类不等归 §1.6）                                                                                                                                          |

对象形态族（`VerificationRecipient`）的边界声明类型是基类：入口在基类（判别字段路由），组内断言以基类型反序列化并路由到子类。

### 1.4 SecretValue

下表各行均为【强制】：

| 分组            | 必须覆盖                                      |
|-----------------|-----------------------------------------------|
| `Equality`      | 不同实例不等（identity-based）                |
| `Masking`       | toString 全遮蔽 `SecretValue[***]`            |
| `Serialization` | 序列化输出 `{}`（无可检测属性）；反序列化拒绝 |

### 1.5 枚举 DP

下表各行均为【强制】：

| 分组            | 必须覆盖                                                        |
|-----------------|-----------------------------------------------------------------|
| `Lookup`        | `of` 合法名（逐一）；`of(null)` / 空名 / 未知名拒绝；常量集快照 |
| `Display`       | `desc()` 契约（逐常量）；`toString()` = `name()`                |
| `Serialization` | round-trip + 非法名拒绝                                         |

- 【强制】`Serialization` 由 `EnumDomainPrimitiveContractTest` 承载：形状 / round-trip 逐 `values()` 常量断言、非法名拒绝；测试类只声明
  `EnumContract`（声明类型 + 非法名 JSON），`Lookup` / `Display` 仍手写。
- 【参考】枚举 `equals` 不可覆写（身份即等值），§1.5 不列 `Equality`；`toString()` = `name()` 已由 `Display` 覆盖，不另建
  `Debug`。

### 1.6 跨 DP 不等式与家族契约

- 【强制】每模块一个 `CrossTypeEqualityTest`——维护「DP 实例清单」（非枚举、非 SecretValue 的 record / class DP 各一规范实例），套件内
  两两断言跨类 `equals` 恒不等；新增 DP ＝ 清单登记一行，配对自动全量。
- 【强制】`SensitiveValueContractTest`（探针契约）与清单法互补不重叠（子类漏标 `callSuper` 的跨类误等由探针拦截）。
- 【强制】强制力 ＝ **检视项**：日常靠「新增 DP 必须登记清单一行」+ §5 检视清单；终验一次性脚本出矩阵，不引入反射自检。

❌ 新增 DP 未登记跨类不等式清单（跨类误等无人拦网）：

```java
// ❌ 新 DP 上线但清单无本行：配对不会被覆盖
private static final List<Object> SAMPLES = List.of(new LongId(1), new Uuid(uuid));
// ✅ 新增一行，配对自动全量
private static final List<Object> SAMPLES = List.of(new LongId(1), new Uuid(uuid), SoftwareVersion.of("v1.0.0"));
```

## 2. @Nested 分组名

**完成判据**：分组名严格取自下表，不新造；一个测试类按需选用，不必全部使用。

| 分组名                 | 适用形态                                                                            | 必测内容                                                   |
|------------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------|
| `Constructor`          | 所有 DP                                                                             | 合法值创建（定义性路径）                                   |
| `Validation`           | 所有 DP                                                                             | null / blank / 范围 / 格式 / `parse` 非法串                |
| `Equality`             | 所有 DP（SecretValue 为 identity 语义）                                             | 同值相等、异值不等、hashCode 一致                          |
| `Debug`                | 所有 DP                                                                             | toString 精确全串                                          |
| `Serialization`        | 所有 DP                                                                             | 形状 + round-trip + 非法输入拒绝                           |
| `ComparableTest`       | 仅 `Comparable` DP                                                                  | `compareTo` 与 `equals` 一致                               |
| `Cache`                | 含缓存且带边界值的 DP                                                               | 边界三元组值等价（禁身份断言）                             |
| `Conversion`           | 含 `fromXxx` / `toXxx` / 包装适配 `from(包装类型)` / DP↔DP 转换的 DP                | 转换往返与规范形态（含 `toDisplayString` / `toInstant`）   |
| `Masking`              | 脱敏输出族（`SensitiveValue` 子类 / `Masked*` 族 / `SecretValue` / `PasswordHash`） | 脱敏串形态与不可逆推                                       |
| `Identity`             | 实现 `Identifier<T>` 的 DP                                                          | `identifier()` 返回类型化规范值                            |
| `Normalization`        | 构造期归一化的 DP                                                                   | 归一形态 + 幂等（含边界维度）                              |
| `RichMethods`          | 含非 getter 业务方法的 DP                                                           | 派生业务方法契约（`compositeKey` / `matches` / `size` 等） |
| `Arithmetic`           | 含数值运算方法的 DP                                                                 | 运算契约（`plus` / `minus` / `multiply` …）                |
| `Inverse`              | 含互逆转换对的 DP                                                                   | `value()` ↔ `from(value())` 互逆                           |
| `PackedInt`            | 含位段编解码入口的 DP                                                               | 位段编码 / 解码契约                                        |
| `Next`                 | 含步进语义的 DP                                                                     | `next()` / `previous()` 单步推进                           |
| `DerivedFields`        | 含派生字段（缓存或即时计算）的 DP                                                   | 派生字段与原始值一致                                       |
| `JsonValueInheritance` | 字面量家族 DP                                                                       | `@JsonValue` 继承自家族接口、序列化单源                    |
| `Routing`              | 密封族（含 sealed interface + record）                                              | 入口路由（入口类型＝边界声明类型：子类或基类）             |
| `Lookup`               | 枚举 DP                                                                             | `of` 全路径 + 常量集                                       |
| `Display`              | 枚举 DP                                                                             | `desc()` 契约 + `toString()` = `name()`                    |
| `CrossTypeEquality`    | 模块级契约套件                                                                      | 清单两两跨类 `equals` 恒不等                               |

表规则（【强制】）：①组名只取本表，需新组名 ＝ 先改本规范；②「适用形态」满足才建组，「适用形态」之外多建 ＝ 形式过标；③一个 DP
可多组并存，组名与内容一一对应。

【强制】`Masking` 建组判据：§1 列出 `Masking` 的形态（§1.4 `SecretValue`
）必建；未列的形态不设为必建——本表「适用形态」命中时按需建组，缺组不判缺项、建组不判形式过标（「适用形态」外的多建方为形式过标）。

❌ 自造组名（表外名称，覆盖核对无法机械落组）：

```java
// ❌ 表外自造
@Nested @DisplayName("索引") class Indexing { }
// ✅ 取单表名（派生业务方法归 RichMethods）
@Nested @DisplayName("业务方法") class RichMethods { }
```

## 3. DP 专属写法

- 【强制】 **共享 Mapper（组件模块）**：`import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER`——定义在
  `soda-component-domain-types/src/testFixtures/java/`（随 test fixtures 工件发布，DP 契约基类即用它）；下游模块经
  `testImplementation(testFixtures(project(':soda-components:soda-component-domain-types')))` 取得，不复制配置。
- 【强制】 **共享 Mapper（用户模块）**：`import static com.soda.user.domain.DomainTestUtil.MAPPER`——仍供手写分组（`Routing`
  等）与非
  DP 测试（`UserTest` / `*AuthAccountTest` / `VerificationTest`）使用；该模块 DP 测试被折叠的三组由契约基类承载，间接走组件
  Mapper。
  全库仅此两处定义；禁止每个测试文件 `new ObjectMapper()`
  （复用规则单源见 [test-conventions §3 通用写法](../test-conventions.md#3-通用写法全层适用)「复用」行）。
- 【强制】 **断言深度**：每方法一个场景；`Serialization` 三断言 **分方法**（形状 / round-trip / 非法拒绝）；`Debug` 精确全串；组规模
  ≤6
  （超 6 拆分或参数化，「分组」行单源见 [test-conventions §3 通用写法](../test-conventions.md#3-通用写法全层适用)）。
- 【强制】 **命名**：`should_<行为>` 为基，条件不足以消歧时补 `_when_<条件>`；`@DisplayName` 中文说明（「命名」行单源见
  [test-conventions §3 通用写法](../test-conventions.md#3-通用写法全层适用)）。
- 【参考】数据与参数化（内联字面值 / `@ParameterizedTest` ≥4
  组阈值等）单源在 [test-conventions §3 通用写法](../test-conventions.md#3-通用写法全层适用)——本文不另设例外。

❌ 每个测试文件 `new ObjectMapper()`（重复配置、易漂移）：

```java
// ❌ 就地新建
var mapper = new ObjectMapper();
// ✅ 共享 Mapper
import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
```

❌ 精确异常消息 / 零断言恒真：

```java
// ❌ 消息逐字耦合 + 恒真断言
assertThatThrownBy(() -> new LongId(-1)).hasMessage("must be >= 0, got: -1");
assertThat(new LongId(1)).isNotNull();
// ✅ 消息包含断言 + 有信息量的断言
assertThatThrownBy(() -> new LongId(-1)).hasMessageContaining("must be >= 0");
assertThat(new LongId(1).value()).isEqualTo(1);
```

## 4. 完整示例（按形态取用）

按被测 DP 形态取对应模板，其余形态按需跳过：单字段（§4.1）、多字段带 `Instant` / `Duration`（§4.2）、SecretValue（§4.3）、枚举（§4.4）。
模板只给骨架与命名，必测内容以 §1 为准（不复制清单，防双源）。

### 4.1 单字段 DP

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;   // 用户模块共享 Mapper
// 组件模块：import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LongId 值对象")
class LongIdTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            assertThat(new LongId(42).value()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {
        @Test
        @DisplayName("负值拒绝")
        void should_throw_when_valueIsNegative() {
            assertThatThrownBy(() -> new LongId(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be >= 0");
        }
        @Test
        @DisplayName("parse 非法字符串拒绝")
        void should_throw_when_parseInvalidString() {
            assertThatThrownBy(() -> LongId.parse("not-a-number"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new LongId(42)).isEqualTo(new LongId(42));
        }
        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new LongId(1)).isNotEqualTo(new LongId(2));
        }
        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new LongId(42)).hasSameHashCodeAs(new LongId(42));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化形状为裸标量")
        void should_haveScalarShape() throws Exception {
            assertThat(MAPPER.writeValueAsString(new LongId(42))).isEqualTo("42");
        }
        @Test
        @DisplayName("round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = new LongId(42);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, LongId.class)).isEqualTo(original);
        }
        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-number\"", LongId.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new LongId(42);
            var same = new LongId(42);
            assertThat(a.compareTo(same) == 0).isTrue();
            assertThat(a).isEqualTo(same);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new LongId(42)).hasToString("LongId[value=42]");
        }
    }

}
```

### 4.2 多字段 DP（带 `Instant` / `Duration`）

多字段 DP 序列化为 JSON 对象（创建器注解单源见 [dp-json-conventions §1 模式总表](dp-json-conventions.md#1-模式总表)）；
Jackson 3 原生支持 `Instant` / `Duration` 等 JSR-310 类型，无需注册额外模块。

```java
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import java.time.Duration;
import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VerificationCodePolicy 值对象")
class VerificationCodePolicyTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            var policy = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            assertThat(policy.codeLength()).isEqualTo(PositiveInt.of(6));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确，含所有字段")
        void should_haveCorrectToString() {
            var policy = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            assertThat(policy).hasToString(
                    "VerificationCodePolicy[codeLength=PositiveInt[value=6], expiry=PT5M, codeAlphabet=Alphabet[value=0123456789]]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化形状为 JSON 对象，键集完整")
        void should_haveObjectShape() throws Exception {
            var policy = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            var json = MAPPER.writeValueAsString(policy);
            assertThat(json).contains("codeLength").contains("expiry").contains("codeAlphabet");
        }
        @Test
        @DisplayName("round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            assertThat(MAPPER.readValue(MAPPER.writeValueAsString(original), VerificationCodePolicy.class))
                    .isEqualTo(original);
        }
        @Test
        @DisplayName("非法 JSON 拒绝（空对象）")
        void should_throw_when_emptyJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationCodePolicy.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
```

### 4.3 SecretValue

SecretValue 使用 identity-based 相等、toString 脱敏、序列化输出 `{}`、反序列化拒绝：

```java
import com.soda.component.domain.types.SecretValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecretValue 敏感值对象")
class SecretValueTest {

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("不同实例不等（identity-based）")
        void should_notBeEqual_when_differentInstance() {
            assertThat(new SecretValue("secret")).isNotEqualTo(new SecretValue("secret"));
        }
    }

    @Nested
    @DisplayName("脱敏")
    class Masking {
        @Test
        @DisplayName("toString 全遮蔽")
        void should_maskToString() {
            assertThat(new SecretValue("my-secret")).hasToString("SecretValue[***]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化无可检测属性，输出空对象")
        void should_serializeToEmpty_when_serialize() throws Exception {
            assertThat(MAPPER.writeValueAsString(new SecretValue("s"))).isEqualTo("{}");
        }
        @Test
        @DisplayName("反序列化拒绝")
        void should_throw_when_deserialize() {
            assertThatThrownBy(() -> MAPPER.readValue("\"secret\"", SecretValue.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
```

### 4.4 枚举 DP

```java
import com.soda.component.domain.types.Sex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Sex 枚举 DP")
class SexTest {

    @Nested
    @DisplayName("查找")
    class Lookup {
        @Test
        @DisplayName("合法名逐一解析")
        void should_lookup_when_validName() {
            assertThat(Sex.of("M")).isEqualTo(Sex.M);
            assertThat(Sex.of("F")).isEqualTo(Sex.F);
        }
        @Test
        @DisplayName("空 / 未知 / null 名拒绝")
        void should_throw_when_invalidName() {
            assertThatThrownBy(() -> Sex.of("")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Sex.of("X")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Sex.of(null)).isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        @DisplayName("常量集快照")
        void should_exposeExactConstants() {
            assertThat(Sex.values()).containsExactly(Sex.M, Sex.F);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {
        @Test
        @DisplayName("desc 逐常量")
        void should_exposeDesc() {
            assertThat(Sex.M.desc()).isEqualTo("male");
            assertThat(Sex.F.desc()).isEqualTo("female");
        }
        @Test
        @DisplayName("toString 为 name()")
        void should_haveNameToString() {
            assertThat(Sex.M).hasToString("M");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("round-trip 一致")
        void should_roundTrip() throws Exception {
            assertThat(MAPPER.readValue(MAPPER.writeValueAsString(Sex.M), Sex.class)).isEqualTo(Sex.M);
        }
        @Test
        @DisplayName("非法名拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"X\"", Sex.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
```

## 5. 测试检视清单

按形态逐类核对（每条可判定）：

1. 形态判定 → §1 逐行核对：每行落到具体 `@Nested` 组（名 + 内容双强制）；对标准形态 DP，`Equality` / `Debug` /
   `Serialization`
   （及 `ComparableTest`）由契约基类继承而来——核对该类 **已声明 `Contract` 样例且未重复声明这四组**，不逐条抄写断言
2. 组名取自 §2 单表——无表外自造、无适用形态外的多建（手写分组同样适用）
3. 缓存 DP 无身份断言（`==` / `isSameAs` / `isNotSameAs`）
4. 新增 DP 已登记 `CrossTypeEqualityTest` 清单 + 敏感契约套件同步（§1.6）
5. 序列化测试用共享 `MAPPER`，无 `new ObjectMapper()`；组件 Mapper 唯一定义在 `src/testFixtures`，下游模块经 `testFixtures`
   依赖取得
