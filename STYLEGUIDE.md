# 编码规范

## JDK 特性优先

优先使用 JDK 最新特性，不重复造轮子：

| 场景 | 优先使用 | 禁止 |
| 不可变值对象（DP、DTO） | `record`（JDK 16+） | 手写 class + `@EqualsAndHashCode` + `toString()` |
| 空值校验 + 值归一化 | record 紧凑构造 | 在工厂方法中校验 |
| 工厂方法（DP） | `of`（参数即值）、`from`（跨类型转换）、`parse(String)`（字符串解析） | `valueOf(Object)`（已移除） |
| 局部变量类型 | `var`（JDK 10+） | 显式写冗长类型 |
| DP 序列化 | `Serializable` + `@Serial` + `serialVersionUID`（仅在明确需要时） | 无 `implements Serializable` 时留 `@Serial serialVersionUID` |
```java
// ✅ 推荐 — record DP，不显式实现 Serializable
public record LongId(@JsonValue long value) implements Identifier<Long>, Comparable<LongId> {

    public LongId {                                          // 紧凑构造：校验
        ValidateUtils.minValue(0, false, value);
    }

    /** 从字符串解析构造，格式同 {@link ParseUtils#parseLong}。 */
    public static LongId parse(String s) {
        return new LongId(ParseUtils.parseLong(s));
    }

    @Override
    public Long identifier() {
        return value;
    }

    @Override
    public int compareTo(LongId other) {
        return Long.compare(this.value, other.value);
    }
}

// ✅ 推荐 — class DP，显式实现 Serializable（仅在必要时）
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Version implements Type, Comparable<Version>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @JsonValue
    @EqualsAndHashCode.Include
    private final int value;

    public static Version of(int value) { ... }
}
```
### 校验方法参数顺序

`ValidateUtils` 中所有校验方法的参数遵循 Spring {@code Assert} 风格：被校验值在前，辅助参数在后。
只有被校验值标注 `@Nullable` 并校验，辅助参数视为可信。

```java
minValue(value, 0, false)     // long value, long min, boolean inclusive
maxLength(value, 30)          // String value, int max
hasPrefix(value, "P:")        // String value, String prefix
matches(value, pattern)       // String value, Pattern pattern
```

### Entity 空值校验

Entity 类的空值校验使用 Spring `Assert.notNull`，携带自解释的错误消息：

```java
Assert.notNull(username, "username must not be null");
Assert.notNull(status, "status must not be null");
```

| 角色 | 判空工具 | 消息 |
|---|---|---|
| DP 类 | `ValidateUtils.notNull(value)` | 固定默认消息 |
| Entity 类 | `Assert.notNull(value, "field must not be null")` | 每处自解释 |
| 工具类 | `ValidateUtils.notNull(value)` | 固定默认消息 |

原则：Entity 承载业务状态，判空时应当明确哪个字段为 null，便于故障排查。DP 类型简单，默认消息足够。

## 注解驱动

能使用注解声明语义的地方，**禁止用手写代码替代**。

### Jackson 注解

| 用途 | 注解 | 位置 |
|---|---|---|
| 序列化 | `@JsonValue` | record 组件（如 `@JsonValue long value`）或访问器方法 |
| 反序列化 | 无需（Jackson 2.12+ 原生识别 record 典范构造器） | — |

### JSpecify 注解

项目使用 JSpecify 1.0 作为空性标注标准，覆盖 `org.jspecify.annotations` 包中全部 4 个注解。
Nullness Specification（空性标注）原则：通过 JSpecify 注解在编译期明确每个类型的空性，杜绝 NullPointerException。

#### 注解速览

| 注解 | 目标 | 语义 | 本项目使用 |
|---|---|---|---|
| `@NullMarked` | MODULE, PACKAGE, TYPE, METHOD, CONSTRUCTOR | 作用域内类型默认 `@NonNull` | ✅ 必须 — 仅包级 |
| `@Nullable` | TYPE_USE | 该类型可以包含 `null` | ✅ 按场景使用 |
| `@NonNull` | TYPE_USE | 该类型排除 `null`（在 `@NullMarked` 下极少需要） | ⚠️ 仅非空投影 |
| `@NullUnmarked` | PACKAGE, TYPE, METHOD, CONSTRUCTOR | 退出 `@NullMarked`，回到未指定空性 | ❌ 禁止使用 |

---

#### `@NullMarked` — 包级默认非空

每个包须在 `package-info.java` 第一行标注 `@NullMarked`：

```java
@NullMarked
@ApplicationModule(allowedDependencies = {})
package com.soda.component.support.util;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
```

- 位置：`@NullMarked` 在前，`@ApplicationModule` 在后（纯约定，语义等价；安全契约优先于模块契约）

- 仅限包级（本项目约定；JSpecify 同时支持 MODULE/TYPE/METHOD/CONSTRUCTOR 级别）
- 未加 `@NullMarked` 的包视为不合规
- `@NullMarked` 不级联子包，每个包独立标注

---

#### `@Nullable` — 按场景使用

**必须加 `@Nullable` 的场景：**

| 场景 | 字段标注 | getter 返回值 | 示例 |
| 实体的可选字段 | `@Nullable` | `Optional<T>` | `private @Nullable Mobile mobile;` → `public Optional<Mobile> getMobile()` |
| 未持久化的标识符 | `@Nullable` | `@Nullable`（基类特例） | `private @Nullable ID id;` → `public final @Nullable ID getId()` |
| 工厂/构造方法中与可选字段对应的参数 | `@Nullable` | — | `create(@Nullable Email email)` |
| 工具方法接受 null 并立即校验拒绝的参数 | `@Nullable`（参数） | — | `notNull(@Nullable Object value)` |

**禁止加 `@Nullable` 的场景：**

| public/protected 方法返回值 | `Optional<T>` 或空集合/空数组 | 返回值永远不允许 null（项目约定；JSpecify 允许返回值 `@Nullable`） |
> 注：`Optional<T>` 返回值每次调用分配对象。领域层不属于热路径，接受此开销。
> 如有性能敏感路径需豁免，应在代码评审中逐案评估。

**不受限的场景：**

- `Entity.getId()` 和 `Identifiable.getId()`：实体在服务端生成 ID 模式下，持久化前 ID 为 null，getter 须保留 `@Nullable`（领域模型约定，不可用 `Optional` 替代）


---

#### `@NonNull` — 仅语义需要

在 `@NullMarked` 全覆盖的项目中，`@NonNull` 的唯一合理用途是**非空投影（non-null projection）**：

```java
// 当 E 有 @Nullable 上界时，强制某个使用点非空
interface List<E extends @Nullable Object> {
    Optional<@NonNull E> findFirst();
}
```

- 禁止为"文档强调"目的冗余标注
- 触发条件：泛型类的类型参数声明为 `<E extends @Nullable Object>` 时，若某个使用点需要强制非空（如 `Optional<@NonNull E>`），应标注 `@NonNull E`
- 当前项目没有非空投影需求，如引入需评审

---

#### `@NullUnmarked` — 禁止使用

仅用于遗留代码渐进迁移，本项目为新项目，不适用。

---

#### 外部库互操作

外部库未标注 `@NullMarked` 时，其返回值具有未指定空性（unspecified nullness）。
Java 编译器不会因此产生警告，但静态分析工具可能报错。
建议在调用点显式 null 检查或使用 `Optional.ofNullable()` 包装，避免传播空性不确定性。

---

#### 泛型空性

```java
// 声明侧：默认非空上界
public class MyList<E> { ... }                // E 不可为 null

// 声明侧：可 null 上界（当前项目无此模式）
public class MyList<E extends @Nullable Object> { ... }  // E 可 null

// 使用侧：
E                 // parametric — E 可 null 则 null，否则非空
@Nullable E       // nullable projection — 始终可 null
@NonNull E        // non-null projection — 始终非空
```

---

#### 数组空性

```java
@Nullable String[]   // 元素可 null，数组本身不可 null
String @Nullable []  // 数组本身可 null，元素不可 null
```

---

#### 局部变量

局部变量的根类型**不标注**空性注解，由赋值推断。

### Lombok 注解

Lombok 用于减少实体/基类的 boilerplate，不用于替代 record：

| 注解 | 使用场景 |
|---|---|
| `@Getter` | 字段上的值 getter，替代手写 `getXxx()`；Entity 基类不适用（需 final 语义）|
| `@Accessors(fluent = true)` | 与 `@Getter` 配合，使访问器名为 `value()` 而非 `getValue()`，对齐 record 风格|
| `@RequiredArgsConstructor` | 基类构造函数（如 `Entity(access = PROTECTED)`） |
| `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` | 用于**非 record 的 class DP**，配合 `@EqualsAndHashCode.Include` 显式标记参与比较的字段|
| `@EqualsAndHashCode.Include` | 标记参与 `equals`/`hashCode` 的字段|

> **注意**：`@UtilityClass` 不用于有 `import static` 交叉引用的工具类（因编译期符号解析先于 Lombok 处理）。此类场景保持显式 `final class` + 私有构造器 + `static` 方法。

## 控制流

- `if` / `for` / `while` 必须带 `{}`，禁止省略单行体
- 非必要不嵌套。优先卫语句（guard clause）快速失败
- 避免 `else` / `else if`，优先卫语句 + 提前 `return`
- 禁止类型转换（type cast），用泛型 / {@code Comparable<Self>} / 模式匹配消除强转

```java
// ❌ 嵌套 + 无括号
if (value != null)
    for (var item : value)
        if (item.isActive())
            return item;

// ✅ 卫语句 + 花括号
if (value == null) {
    return null;
}
if (!value.isEmpty()) {
    return null;
}
for (var item : value) {
    if (item.isActive()) {
        return item;
    }
}
```

## 异常

- 异常消息使用英文
- 优先 `IllegalArgumentException` / `IllegalStateException`，不使用已废弃异常
- 不在非异常路径上构造异常（性能考虑）
- 工具类通过内联 `IllegalArgumentException` 消息文本校验，不引入工厂类

## 依赖管理
- `implementation` 用于运行时需要的依赖（Jackson 注解、spring-modulith 等）
- `compileOnly` 用于仅编译期需要的依赖（Lombok）
- 不在基础模块用 `api` 暴露传递依赖，除非被依赖的是核心框架能力
- `testImplementation` 用于测试需要的依赖
