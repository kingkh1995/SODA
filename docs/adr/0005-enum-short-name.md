# 0005 — 枚举短名标识设计

**Status**: accepted

**Context**:

soda-user 模块需要一组领域枚举（Sex、UserStatus、AuthAccountType、SocialType）。初始设计参考 yudao-cloud 使用 `int code` + `@JsonValue`/`@JsonCreator` 序列化，枚举放在独立 `soda-user-common` 模块中供 domain 和 api 共享。

随着 DTO/VO 层确认不直接引用枚举类型（使用 `String` 传递），`soda-user-common` 模块失去存在意义，枚举需要重新设计。

**Decision**:

### 枚举设计规则

1. **短名标识**：Java 枚举标识符作为持久化短名字符串，长度 1-4 字符，可含 `_`。由 `name()` 直接提供。
2. **英文 desc 字段**：每个枚举常量带 `desc` 字段，用于辅助解释含义。通过 `desc()` 访问器读取。
3. **不暴露给 DTO/VO**：DTO/VO 使用 `String` 传递枚举值，不直接引用枚举类型。
4. **DB 存储**：使用 `name()` 值持久化到数据库 `CHAR(4)` 列。
5. **Lombok**：`@Getter` + `@Accessors(fluent = true)` 生成 `desc()` 访问器；`@RequiredArgsConstructor` 生成构造器。
6. **JSpecify**：包级 `@NullMarked`。
7. **Domain Primitive**：`EnumType extends Type`，所有业务枚举同时也是 Domain Primitive。
8. **反序列化入口**：每个 enum 提供 `@JsonCreator of(String)`，委托 `ParseUtils.parseEnum()`。组件自带的 `valueOf(String)` 作为外部不可靠输入入口。

### 枚举清单

| 枚举 | 常量（短名 → desc） |
|------|---------------------|
| `Sex` | `M`(Male), `F`(Female) |
| `UserStatus` | `E`(Enabled), `D`(Disabled) |
| `AuthAccountType` | `P`(Password), `S`(Sms), `E`(Email), `O`(OAuth) |
| `SocialType` | `GE`(Gitee), `DT`(DingTalk), `WENT`(WechatWork), `WMP`(WechatMp), `WOPN`(WechatOpen), `WMIN`(WechatMini), `ALIP`(AlipayMini) |

### 模块调整

- 移除 `soda-user-common` 模块（枚举迁回 `soda-user-domain`）
- `soda-user-api` 不再依赖 common（无外部枚举引用）
- `soda-user-domain` 不再依赖 common（枚举是领域层概念）

**Rationale**:

- **短名优于 int code**：`CHAR(4)` 存储可读性强，`"E"` 比 `1` 在日志/DB 中直观。无需 `fromCode()` 映射逻辑。
- **短名优于长名**：`"O"` vs `"SOCIAL"`，传输和存储更紧凑。
- **`EnumType` 接口**：统一所有业务枚举的 `desc()` 契约，同时通过 `extends Type` 纳入 DP 体系。
- **record 风格访问器**：`desc()` 而非 `getDesc()`，与项目中 `record` DP 的访问器风格一致（`value()`、`username()` 等）。
- **`@JsonCreator of(String)`**：Jackson 默认用 `valueOf(String)` 反序列化，但 `valueOf` 无法自定义异常。通过 `of(String)` + `@JsonCreator` 拦截，委托 `ParseUtils.parseEnum()` 提供一致的异常语义（`forUnknownEnum`）。

**Consequences**:

| Positive | Negative |
|----------|----------|
| 枚举不可变，desc 是枚举常量的固有属性 | 短标识符在 IDE 中可读性略低于长名（`Sex.M` vs `Sex.MALE`） |
| DB 直接看出含义（`"E"` 是 Enabled） | 枚举常量改名破坏 DB 数据（需 migration） |
| `soda-user-common` 模块移除，模块数从 8 → 7 | |
| `EnumType extends Type`，枚举统一为 Domain Primitive，复用 DP 校验/异常体系 | |
