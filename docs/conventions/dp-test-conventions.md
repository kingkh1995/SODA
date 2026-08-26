---
type: Convention
title: Domain Primitive 测试规范
description: 写 DP 测试时按形态取必测分组（§1）、@Nested 分组名（§2）与完整模板（§4）；改 DP 时按同形态核对覆盖。通用写法与分层映射单源在 test-conventions，本文只收 DP 必测分组与 DP 专属约定。
tags: [ convention, testing, dp ]
status: stable
---

# Domain Primitive 测试规范

> 指针+单源：通用写法（断言/命名/分组/数据/参数化/mock 边界）与分层映射 **单源**
> 在 [test-conventions](../test-conventions.md)；本文只收 DP 必测分组与 DP 专属约定。

## 1. 必测分组：按 DP 形态

**完成判据**：按被测 DP 的形态，覆盖该形态下表每一行「必须覆盖的测试」；跨 DP 不等式另见 §1.6。每组测试归属见 §2 的标准
`@Nested` 分组名。

### 1.1 Record / Class DP（不含缓存）

| 分组 | 必须覆盖的测试 |
|---|---|
| **构造** | 合法值创建实例 |
| **校验** | null 拒绝、blank 拒绝（String）、范围/格式边界、`parse` 非法字符串（仅 DP 有 `parse` 时） |
| **相等** | 相同值相等、不同值不等、hashCode 一致 |
| **调试** | toString 格式 `ClassName[field=value]` |
| **序列化** | Jackson round-trip、非法 JSON 拒绝 |
| **比较** | `compareTo` 与 `equals` 一致（仅 `Comparable` DP） |

多字段 DP（`EmailContent`、`VerificationCode`、`VerificationCodePolicy`）使用 `@JsonCreator(mode = PROPERTIES)` +
`@JsonProperty`，序列化为 JSON 对象 `{"f1":v, "f2":v}` 而非字符串。round-trip 测试方法相同，但 JSON 输入是对象格式。Jackson
3 原生支持 `Instant` / `Duration` 等 JSR-310 类型，无需注册额外模块。 BigDecimal DP（`WanYuan`）规范值为 `String`（
`toPlainString()` 格式），JSON 值为字符串而非数字；额外要求归一化幂等性测试（≥4 组同构值时按 test-conventions §3「参数化」行写
`@ParameterizedTest`）。

### 1.2 Record / Class DP（含缓存）

同 1.1，额外：

| 分组 | 必须覆盖的测试 |
|---|---|
| **缓存** | 缓存实例 `equals` 一致、hashCode 一致、不依赖 `==` |

### 1.3 密封继承（AuthAccountId 子类）

同 1.1，额外：

| 分组 | 必须覆盖的测试 |
|---|---|
| **相等** | 相同子类等值相等（`Password("P:1")` = `Password("P:1")`）、hashCode 一致 |
| **路由** | 基类 `of("P:42")` 返回 `PasswordAuthAccountId` 实例、反序列化 JSON 路由到正确子类 |
| **调试** | toString 格式由基类 `getSimpleName + [value=...]` 生成 |

### 1.4 SecretValue

| 分组 | 必须覆盖的测试 |
|---|---|
| **身份** | 不同实例不等（identity-based） |
| **脱敏** | toString 输出 `SecretValue[***]` |
| **序列化** | 序列化输出 `{}`（无可检测属性）、反序列化拒绝 |

### 1.5 枚举 DP

| 分组 | 必须覆盖的测试 |
|---|---|
| **查找** | `of(String)` 合法值/空/非法名 |
| **显示** | `desc()` 返回描述、`toString()` 返回 `name()` |
| **序列化** | Jackson round-trip、非法名拒绝 |

### 1.6 跨 DP 不等式

每模块一个 `CrossTypeEqualityTest`——维护一份「DP 实例清单」（非枚举、非 SecretValue 的 record/class DP 各一规范实例），套件内两两断言跨类
`equals` 恒不等。新增 DP ＝ 清单登记一行，配对自动全量；密封子类不等由此统一承载。SensitiveValue 的 callSuper 泄漏守卫另归各模块
`SensitiveValueContractTest`（探针契约，互补不重叠）。

## 2. 标准 @Nested 分组名

**完成判据**：分组名严格取自下表，不新造；一个测试类按需选用，不必全部使用。

| 分组名           | 适用范围                  | 包含的测试                         |
|------------------|---------------------------|------------------------------------|
| `Constructor`    | 所有 DP                   | 合法值创建、parse（如有）          |
| `Validation`     | 所有 DP                   | null/blank/边界/异常               |
| `Equality`       | 所有 DP（除 SecretValue） | 相等、不等、hashCode               |
| `Debug`          | 所有 DP                   | toString 格式                      |
| `Serialization`  | 所有 DP                   | Jackson round-trip、非法 JSON 拒绝 |
| `ComparableTest` | 仅 `Comparable` DP        | compareTo 与 equals 一致           |
| `Cache`          | 仅含缓存 DP               | 缓存实例相等性                     |

**报备族谱**（专用 `@Nested` 分组名；首次出现于 soda-components / soda-user 16/15 票存量，已在现库使用 — 报备不视为偏差，未来新
DP 沿用）：

| 分组名                  | 适用范围                                           | 包含的测试                                             |
|-------------------------|----------------------------------------------------|--------------------------------------------------------|
| `Conversion`            | 含派生转换的 DP（`Fen`/`WanYuan`/`EpochMilli` 等） | `fromYuan()` / `toYuan()` / `instant()` 等派生方法往返 |
| `Masking`               | Masked 五族（ADR-0032）                            | `maskOf` 输出匹配 PATTERN、原值不可逆推                |
| `Identity`              | Identifier 系 DP                                   | `identifier()` 返回类型化值                            |
| `Normalization`         | 含归一化的 DP                                      | 大小写/格式归一化幂等性                                |
| `BoundaryNormalization` | 含边界归一化的 DP                                  | 边界值的归一化形态（如 `BankCard` 13–19 位映射）       |
| `RichMethods`           | 含额外方法的 DP                                    | 非 getter 的业务方法（`KeyUtils` 派生、状态查询等）    |
| `Arithmetic`            | 含数值运算的 DP                                    | `add` / `subtract` / `multiply` 等运算契约             |
| `Inverse`               | 含互逆转换的 DP                                    | `value()` 与 `from(value())` 互逆                      |
| `Indexing`              | 含索引字段的 DP                                    | 索引与原始值映射、盲索引列契约                         |
| `PackedInt`             | 含位运算的 DP                                      | 位段编码/解码（如 `MaskedBankCard` 星号段）            |
| `Next`                  | 含步进语义的 DP                                    | `next()` / `previous()` 等单步推进                     |
| `DerivedFields`         | 含派生缓存的 DP                                    | 派生字段与原始值一致性（如 `DecimalLiteralType` 缓存） |
| `CrossTypeEquality`     | 跨类不等式 DP                                      | 跨类 `equals` 恒不等（已收 `CrossTypeEqualityTest`）   |
| `CacheInvariant`        | 含缓存 DP                                          | 缓存实例引用相等、不可变 Map 行为等                    |
| `JsonValueInheritance`  | 字面量家族 DP                                      | `@JsonValue` 继承自家族接口、序列化单源                |
| `Routing`               | 密封基类（§1.3）                                   | `of(String)` 按短名前缀路由到子类、反序列化路由        |
| `Parse`                 | 含 `parse(String)` 工厂的 DP                       | `parse` 合法/非法/边界输入                             |

## 3. DP 专属写法

- **共享 Mapper**（序列化/反序列化测试用；「共享基础设施允许、数据 fixture 禁止」规则 **单源**在 test-conventions
  §3「复用」行）：用户模块 `import static com.soda.user.domain.DomainTestUtil.MAPPER`；组件模块
  `import static com.soda.component.domain.testutil.JacksonTestUtil`（或共享 `SupportTestUtil.MAPPER`）；禁止每个测试文件
  `new ObjectMapper()`
- **小数组例外**：常数列举测试集的枚举或归一化幂等测试可接受 ≤5 元素的小数组（如 WanYuan 幂等值集）

---

## 4. 完整示例（按形态取用）

按被测 DP 形态取对应模板，其余形态按需跳过：单字段（§4.1）、多字段带 `Instant`/`Duration`（§4.2）、SecretValue（§4.3）。

### 4.1 单字段 DP

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;   // 用户模块共享 Mapper
// 支持模块：import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
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
    @DisplayName("校验与异常")
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
    @DisplayName("相等性与 hashCode")
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
        @DisplayName("Jackson round-trip 一致")
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

多字段 DP 使用 `@JsonCreator(mode = PROPERTIES)` + `@JsonProperty`，序列化为 JSON 对象。
Jackson 3 原生支持 `Instant` / `Duration` 等 JSR-310 类型，无需注册额外模块。

```java
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import tools.jackson.core.JacksonException;

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
        @DisplayName("Jackson round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("codeLength").contains("expiry").contains("codeAlphabet");
            assertThat(MAPPER.readValue(json, VerificationCodePolicy.class)).isEqualTo(original);
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

SecretValue 使用 identity-based 相等、toString 脱敏、序列化输出 `{}`、反序列化拒绝。

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecretValue 敏感值对象")
class SecretValueTest {

    @Test
    @DisplayName("不同实例不等（identity-based）")
    void should_notBeEqual_when_differentInstance() {
        assertThat(new SecretValue("secret")).isNotEqualTo(new SecretValue("secret"));
    }

    @Test
    @DisplayName("toString 脱敏")
    void should_maskToString() {
        assertThat(new SecretValue("my-secret")).hasToString("SecretValue[***]");
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化无可检测属性，输出空对象")
        void should_serializeToEmpty_when_serialize() throws Exception {
            var json = MAPPER.writeValueAsString(new SecretValue("s"));
            assertThat(json).isEqualTo("{}");
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
