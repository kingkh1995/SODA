# 编码规范

## JDK 特性优先

优先使用 JDK 最新特性，不重复造轮子：

| 场景 | 优先使用 | 禁止 |
| 不可变值对象（DP、DTO） | `record`（JDK 16+） | 手写 class + `@EqualsAndHashCode` + `toString()` |
| 空值校验 + 值归一化 | record 紧凑构造 | 在工厂方法中校验 |
| 工厂方法（DP） | 单一 `valueOf(Object)` 委托 `ParseUtils`（含 null 检查）| 多个重载或内联类型分发 |
| 局部变量类型 | `var`（JDK 10+） | 显式写冗长类型 |
| 序列化相关成员 | `@Serial`（JDK 14+） | 不加注解 |
| 异常消息拼接 | `var sb = new StringBuilder(...)` | 省略 `var` |
```java
// ✅ 推荐
public record LongId(@JsonValue long value) implements Identifier<Long>, Comparable<LongId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public LongId {                                          // 紧凑构造：校验
        ValidateUtils.minValue(0, false, value);
    }

    /** 从不可靠输入构造，null 时抛出 IllegalArgumentException。 */
    public static LongId valueOf(Object value) {
        return new LongId(ParseUtils.parseLong(value));      // ParseUtils 处理 null + 类型转换
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
```
### 校验方法参数顺序

`ValidateUtils` 中所有校验方法的参数遵循：辅助参数在前，被校验值（命名 `value`）在最后。
只有被校验值标注 `@Nullable` 并校验，辅助参数视为可信。

```java
minValue(0, true, value)   // long min, boolean inclusive, long value
maxLength(30, value)       // int max, String value
hasPrefix("P:", value)     // String prefix, String value

## 注解驱动

能使用注解声明语义的地方，**禁止用手写代码替代**。

### Jackson 注解

| 用途 | 注解 | 位置 |
|---|---|---|
| 序列化 | `@JsonValue` | record 组件（如 `@JsonValue long value`）或访问器方法 |
| 反序列化 | 无需（Jackson 2.12+ 原生识别 record 典范构造器） | — |

### JSpecify 注解

- 每个包的 `package-info.java` 必须标注 `@NullMarked`，使包内所有类型默认为 `@NonNull`
- 仅在确实可为空的参数/返回值处使用 `@Nullable` 覆盖默认

```java
@ApplicationModule(allowedDependencies = {})
@NullMarked                                 // 包内默认非空
package com.soda.component.support.util;
```

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
- 工具类统一通过 `IllegalArgumentExceptions` 工厂方法生成异常消息

## 依赖管理
- `implementation` 用于运行时需要的依赖（Jackson 注解、spring-modulith 等）
- `compileOnly` 用于仅编译期需要的依赖（Lombok）
- 不在基础模块用 `api` 暴露传递依赖，除非被依赖的是核心框架能力
- `testImplementation` 用于测试需要的依赖（如 `jackson-databind` 用于对象序列化验证）
