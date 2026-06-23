# 0006 — DP 工厂方法命名约定

**Status**: accepted

参考 LocalDate 的三分语义（`of` / `from` / `parse`），统一项目中所有 Domain Primitive（class、record、enum）的静态工厂方法命名。

**Context**: 项目中 DP 的静态工厂命名不统一：`valueOf(Object)`、`of(Type)`、`from(Type)`、`fromYuan(BigDecimal)` 混用。枚举的 `of(String)` 和 AuthAccountId 子类的 `from(String)` 语义相同但命名不同。需要统一规则。

## 规则

|命名|语义|`@JsonCreator`|适用条件|
|---|---|---|---|
|`of(...)`|参数就是底层值或其组成部分|✅ 挂这里 (class/enum)||
|`from(...)`|参数 ≠ 底层值，跨类型转换|❌||
|`parse(String)`|字符串输入需类型转换或格式解析（如 `"5"` → `int` → `Version`）|❌|仅当 `of`/构造器参数不是 `String` 时|

### 各实现类型的 `@JsonCreator` 位置

|实现|`@JsonCreator` 在|
|---|---|
|`record`|紧凑构造器（编译器自动，无需 `@JsonCreator` 注解，除非显式标注，但标注在构造器上也无害）|
|`class`|`of(T)` 静态工厂方法|
|`enum`|`of(String)` 静态工厂方法|

### 校验与缓存职责

- **校验全部在构造器内**（record 紧凑构造器 / class 私有构造器）
- **缓存逻辑在 `of` 内**（class DP），构造器不参与

## 映射

|DP|实现|`of`/构造器 @JsonCreator|`from`|`parse(String)`|
|---|---|---|---|---|
|Email|record|`Email(String)`|—|—|
|Mobile|record|`Mobile(String)`|—|—|
|LongId|record|`LongId(long)`|—|`parse(String)`|
|UUId|record|`UUId(String)`|—|—|
|UserId|record|`UserId(long)`|—|`parse(String)`|
|CodeValue|record|`CodeValue(String)`|—|—|
|CodeLength|record|`CodeLength(int)`|—|`parse(String)`|
|SmsContent|record|`SmsContent(String)`|—|—|
|EmailContent|record|`EmailContent(String,String)`|—|—|
|RawPassword|record|`RawPassword(String)`|—|—|
|PasswordHash|record|`PasswordHash(String)`|—|—|
|Username|record|`Username(String)`|—|—|
|Avatar|record|`Avatar(String)`|—|—|
|WanYuan|record|`WanYuan(BigDecimal)`|`fromYuan(BigDecimal)`|`parse(String)`|
|Version|class|`of(int)`|—|`parse(String)`|
|Active|class|`of(boolean)`|—|`parse(String)`|
|枚举 *4|enum|`of(String)`|—|—|
|PasswordAuthAccountId|class|`of(String)`|`from(UserId)`|—|
|SmsAuthAccountId|class|`of(String)`|`from(Mobile)`|—|
|EmailAuthAccountId|class|`of(String)`|`from(Email)`|—|
|SocialAuthAccountId|class|`of(String)`|`from(SocialType, String)`|—|

## 不变量

- 每个 DP 只有一个主入口（`of` 或紧凑构造器），不做 `of` + 构造器两个入口
- `from` 仅在跨类型转换时存在，使用时 overload 同名即可；特例需后缀区分（`fromYuan`）
- `valueOf(Object)` 从所有 DP 上移除（组件自带的编译器生成 `valueOf(String)` 除外）

## Rationale

- **`of` vs `from`**：`of` 语义"参数即值"，`from` 语义"参数≠值，需转换"——与 LocalDate 一致
- **`parse` 仅用于 `String`→X 且需类型转换**：当 `of`/构造器参数已是 String 时，`parse` = `of`，无存在意义
- **record 用构造器而非 `of`**：record 紧凑构造器自带的 `@JsonCreator` 功能已完整，加 `of` 多一层委托无价值
- **移除 `valueOf(Object)`**：枚举编译器已生成 `valueOf(String)`，其他 DP 不需要第二个不可靠输入入口
