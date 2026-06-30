# Domain Primitive 测试规范

一页涵盖设计、风格、简化约定。

---

## 1. 测试用例设计：按 DP 形态

### 1.1 Record / Class DP（不含缓存）

| 分组 | 必须覆盖的测试 |
|---|---|
| **构造** | 合法值创建实例 |
| **校验** | null 拒绝、blank 拒绝（String）、范围/格式边界、`parse` 非法字符串（仅 DP 有 `parse` 时） |
| **相等** | 相同值相等、不同值不等、hashCode 一致 |
| **调试** | toString 格式 `ClassName[field=value]` |
| **序列化** | Jackson round-trip、非法 JSON 拒绝 |
| **比较** | `compareTo` 与 `equals` 一致（仅 `Comparable` DP） |
|
多字段 DP（`EmailContent`、`VerificationCode`、`VerificationCodePolicy`）使用 `@JsonCreator(mode = PROPERTIES)` + `@JsonProperty`，序列化为 JSON 对象 `{"f1":v, "f2":v}` 而非字符串。round-trip 测试方法相同，但 JSON 输入是对象格式。
Jackson 3 原生支持 `Instant` / `Duration` 等 JSR-310 类型，无需注册额外模块。
BigDecimal DP（`WanYuan`）规范值为 `String`（`toPlainString()` 格式），JSON 值为字符串而非数字。额外要求归一化幂等性测试（见 §3.6 示例）。

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
| **跨类型** | 不同子类之间不等（`Password("P:1")` ≠ `Sms("S:1")`） |
| **路由** | 基类 `of("P:42")` 返回 `PasswordAuthAccountId` 实例、反序列化 JSON 路由到正确子类 |
| **调试** | toString 格式由基类 `getSimpleName + [value=...]` 生成 |

### 1.4 Secret（RawCredential）

| 分组 | 必须覆盖的测试 |
|---|---|
| **身份** | 不同实例不等（identity-based） |
| **脱敏** | toString 输出 `ClassName[***]` |
| **序列化** | 序列化拒绝、反序列化拒绝 |

### 1.5 枚举 DP

| 分组 | 必须覆盖的测试 |
|---|---|
| **查找** | `of(String)` 合法值/空/非法名 |
| **显示** | `desc()` 返回描述、`toString()` 返回 `name()` |
| **序列化** | Jackson round-trip、非法名拒绝 |

### 1.6 跨 DP 不等式

每个模块一个 `CrossTypeEqualityTest`，覆盖：同模块不同 DP 类型不等、密封子类不等。

---

## 2. 测试代码风格

### 2.1 断言：AssertJ

| 场景 | AssertJ ✅ | JUnit ❌ |
|---|---|---|
| 相等 | `assertThat(a).isEqualTo(b)` | `assertEquals(a, b)` |
| 不等 | `assertThat(a).isNotEqualTo(b)` | `assertNotEquals(a, b)` |
| 异常类型+消息 | `assertThatThrownBy(() → f()).isInstanceOf(T.class).hasMessageContaining("…")` | `assertThrows(T.class, () → f())` |
| 无异常 | `assertThatCode(() → f()).doesNotThrowAnyException()` | — |
| hashCode | `assertThat(a).hasSameHashCodeAs(b)` | `assertEquals(a.hashCode(), b.hashCode())` |
| null | `assertThat(x).isNull()` | `assertNull(x)` |
| 为真 | `assertThat(x > 0).isTrue()` | `assertTrue(x > 0)` |

### 2.2 导入语句
```java
// AssertJ — 唯一断言方式
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 测试框架
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

// Jackson（序列化测试字段解析异常）
import tools.jackson.core.JacksonException;

// 禁止导入
// import static org.junit.jupiter.api.Assertions.*;     ❌
// import org.junit.jupiter.api.Assertions;               ❌
```

### 2.3 @DisplayName + @Nested

```java
@DisplayName("LongId 值对象")
class LongIdTest {

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法值创建实例")
        void should_create_when_validValue() {
            ...
        }
    }
}
```

简化规则：方法数 ≤6 时平铺即可，不需要 `@Nested`。

标准 `@Nested` 分组名：

| 分组名 | 适用范围 | 包含的测试 |
|---|---|---|
| `Constructor` | 所有 DP | 合法值创建、parse（如有） |
| `Validation` | 所有 DP | null/blank/边界/异常 |
| `Equality` | 所有 DP（除 Secret） | 相等、不等、hashCode |
| `Debug` | 所有 DP | toString 格式 |
| `Serialization` | 所有 DP | Jackson round-trip、非法 JSON 拒绝 |
| `ComparableTest` | 仅 `Comparable` DP | compareTo 与 equals 一致 |
| `Cache` | 仅含缓存 DP | 缓存实例相等性 |
| `CrossType` | 仅 AuthAccountId | 跨子类不等（1.3 表格已列） |

一个测试类按需选用以上分组，不必全部使用。

### 2.4 方法命名

```
should_expectedBehavior_when_condition
```

| 正向 | `should_create_when_validValue` |
| 异常 | `should_throw_when_valueIsNull` |
| 相等 | `should_beEqual_when_sameValue` |
| 不等 | `should_notBeEqual_when_differentValue` |

### 2.5 测试顺序

每分组：正向优先 → 边界 → 异常。

---

## 3. 简化约定

### 3.1 共享资源，不重复配置

```java
// ✅ 用户模块：使用共享 MAPPER
import static com.soda.user.domain.DomainTestUtil.MAPPER;

// ✅ 支持模块：使用 JacksonTestUtil（或共享 SupportTestUtil.MAPPER）
import static com.soda.component.support.testutil.JacksonTestUtil.assertRoundTrip;

// ❌ 禁止：每个文件 new ObjectMapper()
private static final ObjectMapper MAPPER = new ObjectMapper();  // ❌
```

### 3.2 `var` 取代显式类型

```java
// ✅ 右侧 new 已说明类型
var id = new LongId(42);

// ❌ 冗余
LongId id = new LongId(42);  // ❌
```

### 3.3 测试数据内联，不引入 fixture 层

```java
// ✅ 直接使用字面值
@Test
void should_notBeEqual_when_differentValue() {
    assertThat(new LongId(1)).isNotEqualTo(new LongId(2));
}

// ✅ 允许类内常量（同一值在 ≥3 个方法中复用时提取）
private static final long VALID_ID = 42L;
private static final long INVALID_ID = -1L;

// ❌ 禁止独立 fixture 工具类或数据工厂
// ❌ 禁止跨测试类共享常量的工具类
```

例外：常数列举测试集的枚举或 WanYuan 幂等测试可接受小数组（≤5 元素）。

### 3.4 每个方法一个逻辑断言

```java
// ✅ 一个场景一个断言
@Test
void should_beEqual_when_sameValue() {
    assertThat(new LongId(42)).isEqualTo(new LongId(42));
}

// ❌ 不要在一个方法中塞入多个无关场景
@Test
void testLongId() {
    assertThat(new LongId(42).value()).isEqualTo(42);
    assertThat(new LongId(42)).isEqualTo(new LongId(42));   // ❌ 多余的 equals 验证
    assertThat(MAPPER.readValue(MAPPER.writeValueAsString(new LongId(42)), LongId.class))
            .isEqualTo(new LongId(42));                     // ❌ 序列化应独立成方法
}
```

### 3.5 不要为了 DRY 写测试基类

```java
// ❌ 禁止：抽象测试基类
abstract class BaseDpTest<T> {
    abstract T createValue();
    abstract T createDifferentValue();
    @Test void equal() {
        assertThat(createValue()).isEqualTo(createValue());
    }
}

// ✅ 直接在每个测试类中写重复断言
// 几行重复比间接继承容易读
```

### 3.6 `@ParameterizedTest` 节制使用

仅当测试数据集 ≥4 组且逻辑完全相同时使用。≤3 组直接平铺。

```java
// ✅ 可用参数化：5 组幂等值
@ParameterizedTest
@ValueSource(strings = {"1.50", "0.00", "111.11", "-50.00", "99999999.99"})
void normalization_idempotent(String s) {
    assertThat(WanYuan.of(WanYuan.of(s).value()).value()).isEqualTo(WanYuan.of(s).value());
}

// ❌ 不需要参数化：仅 2 组
@ParameterizedTest                                                 // ❌
@CsvSource({"true, Active[value=true]", "false, Active[value=false]"})
void toString(String value, String expected) { ... }

// ✅ 直接写 2 个方法
@Test void toString_true() { assertThat(Active.TRUE).hasToString("Active[value=true]"); }
@Test void toString_false() { assertThat(Active.FALSE).hasToString("Active[value=false]"); }
```
数据 ≥10 组时优先使用 `@MethodSource` 将测试数据分离到独立工厂方法。

---

## 4. 完整示例

### 4.1 单字段 DP

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;   // 用户模块共享 Mapper
// 支持模块：import static com.soda.component.support.testutil.JacksonTestUtil.MAPPER;
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
            var policy = new VerificationCodePolicy(6, Duration.ofMinutes(5));
            assertThat(policy.codeLength()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确，含所有字段")
        void should_haveCorrectToString() {
            var policy = new VerificationCodePolicy(6, Duration.ofMinutes(5));
            assertThat(policy).hasToString("VerificationCodePolicy[codeLength=6, expiry=PT5M]");
        }
    }
    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson round-trip 一致（JSON 对象格式）")
        void should_roundTrip() throws Exception {
            var original = new VerificationCodePolicy(6, Duration.ofMinutes(5));
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).contains("codeLength").contains("expiry");
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

### 4.3 Secret（RawCredential）

Secret 使用 identity-based 相等、toString 脱敏、序列化/反序列化拒绝。

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RawCredential 敏感值对象")
class RawCredentialTest {

    @Test
    @DisplayName("不同实例不等（identity-based）")
    void should_notBeEqual_when_differentInstance() {
        assertThat(new RawCredential("secret")).isNotEqualTo(new RawCredential("secret"));
    }

    @Test
    @DisplayName("toString 脱敏")
    void should_maskToString() {
        assertThat(new RawCredential("my-secret")).hasToString("RawCredential[***]");
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("序列化拒绝")
        void should_throw_when_serialize() {
            assertThatThrownBy(() -> MAPPER.writeValueAsString(new RawCredential("s")))
                    .isInstanceOf(JacksonException.class);
        }
        @Test
        @DisplayName("反序列化拒绝")
        void should_throw_when_deserialize() {
            assertThatThrownBy(() -> MAPPER.readValue("\"secret\"", RawCredential.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
```
