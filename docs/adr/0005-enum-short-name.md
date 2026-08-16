# 0005 — 枚举短名标识设计

**Status**: accepted

**Context**:

soda-user 模块需要一组领域枚举（Sex、UserState、AuthAccountType、SocialType）。初始设计参考 yudao-cloud 使用 `int code` + `@JsonValue`/`@JsonCreator` 序列化，枚举放在独立 `soda-user-common` 模块中供 domain 和 api 共享。

> 现状修订：`Sex` 作为通用枚举已下沉到 `soda-components` 的 `com.soda.component.domain.types`（web 层 `@EnumName` 引用它不越模块边界）；`UserState`/`AuthAccountType`/`SocialType` 仍留在 `soda-user-domain`。
>
> 后续修订：`VerificationStatus.X` 已删除（过期是派生判断，不落状态）——见 ADR-0011。
>
> 再修订（2026-08-03）：`VerificationChannel` 归属 `soda-user-domain`（`com.soda.user.domain.types.VerificationChannel`，取值 `S`/`E`）——验证通道作为 `Verification` 子类型的判别值（与 `@JsonTypeName` 同构，见 ADR-0011），也被 gateway 查询过滤使用；因与 `Verification` 子类型一一对应、映射需业务侧维护，故不放 `soda-components`（`Sex` 因 web 层跨模块引用才下沉组件层）；上一版"已删除"注记作废。
>
> 再修订（2026-08-05）：「映射需业务侧维护」作废——`class→VerificationChannel` 反查下沉基础设施（ADR-0016）；枚举仅作为领域词汇与判别值锚点保留在 user-domain（取值与短名规则不变）。
>
> 再修订（2026-08-13）：`VerificationStatus` 改名 `VerificationState`（`soda-user-domain.types`）——修复本 ADR「state vs status 命名规则」的既有违规（I→P→V→U 是状态机，按规则应 `XxxState`）；验证状态属性/列/JSON 判别值随之统一 `state`（`Verification` 实体字段、`user_verification.state` 列、`VerificationQuery`），见 ADR-0011 修订注记。
>
> 再修订（2026-08-15）：`VerificationScene` 取值改为**扁平助记码** `UCC/UPR/ULG/URG`（领域前缀，跨领域共享枚举时隔离，SocialType 先例；列宽 2→4）；`VerificationChannel` 定位修订——**仅保留命令输入判别**（2026-08-15 多态塌缩后无持久化列、无聚合访问器、无查询参数；通道判别单一源栖身 `Subject` 密封类型），`channel` 列与 class↔channel 映射删除——见 ADR-0025。
>
> 再修订（2026-08-16）：`VerificationScene` → **`UserVerificationScene`**（调用方词汇，Verification 只存场景字符串；`code()` 返回助记码串 = `VerificationSource.scene` 值）；`VerificationChannel` 角色扩为三项（① 预认证命令输入、② `Recipient` 判别/序列化属性、③ 工厂策略选择）——**channel 只在 Recipient**（subject 不携带、无独立 channel 列），`Subject` 密封层级删除——见 ADR-0026。

随着 DTO/VO 层确认不直接引用枚举类型（使用 `String` 传递），`soda-user-common` 模块失去存在意义，枚举需要重新设计。

**Decision**:

### 枚举设计规则

1. **短名标识**：Java 枚举标识符作为持久化短名字符串，长度 1-4 字符，可含 `_`。由 `name()` 直接提供。
2. **英文 desc 字段**：每个枚举常量带 `desc` 字段，用于辅助解释含义。通过 `desc()` 访问器读取。**desc 格式为英文 i18n key**（如 `"enabled"`、`"disabled"`），为后续国际化能力保留扩展性。
3. **不暴露给 DTO/VO**：DTO/VO 使用 `String` 传递枚举值，不直接引用枚举类型。
4. **DB 存储**：使用 `name()` 值持久化到数据库 `CHAR(4)` 列。
5. **Lombok**：`@Getter` + `@Accessors(fluent = true)` 生成 `desc()` 访问器；`@RequiredArgsConstructor` 生成构造器。
6. **JSpecify**：包级 `@NullMarked`。
7. **Domain Primitive**：`EnumType extends Type`，所有业务枚举同时也是 Domain Primitive。
8. **反序列化入口**：每个 enum 提供 `@JsonCreator of(String)`，委托 `ParseUtils.parseEnum()`。组件自带的 `valueOf(String)` 作为外部不可靠输入入口。

### 枚举清单

| 枚举 | 常量（短名 → desc） |
|------|---------------------|
| `Sex` | `M`(male), `F`(female) |
| `UserState` | `E`(enabled), `D`(disabled), `R`(removed) |
| `AuthAccountType` | `P`(password), `S`(sms), `E`(email), `O`(oauth) |
| `SocialType` | `GE`(gitee), `DT`(ding-talk), `WENT`(wechat-work), `WMP`(wechat-mp), `WOPN`(wechat-open), `WMIN`(wechat-mini), `ALIP`(alipay-mini) |
| `UserVerificationScene` | `UCC`(user-credential-change), `UPR`(user-password-reset), `ULG`(user-login), `URG`(user-register) |
| `VerificationState` | `I`(initialized), `P`(pending), `V`(verified), `U`(used) |

### state vs status 命名规则

| 术语 | 含义 | 示例 |
|---|---|---|
| `state` | 状态集合/状态机，表示实体的生命周期阶段 | `UserState`（用户生命周期：E/D） |
| `status` | 具体某个状态值，通常是外部可观测的 | HTTP status（200, 404） |

枚举命名时，表示状态机的用 `XxxState`（如 `UserState`），表示具体状态值的用 `XxxStatus`（如 `HttpStatus`）。

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

### API 层使用规范

枚举在 RPC/HTTP 接口中的使用遵循以下规则：

| 层级 | 使用方式 | 说明 |
|---|---|---|
| Domain 层 | 枚举类型（`UserState.E`） | 短名存储，高效 |
| Application 层 | 枚举 → String 转换 | `state.name()` → `"E"` |
| DTO/Response | String 类型 | 短名（`"E"`/`"D"`） |
| JSON 序列化 | 短名（`"E"`/`"D"`） | Jackson 默认 `name()` 序列化 |
| API 响应 | 短名（`"E"`/`"D"`） | 直接返回，无需转换 |

**字段命名**：

| 场景 | 命名 | 说明 |
|---|---|---|
| 状态机字段 | `state` | 表示实体生命周期阶段（如 `UserState`） |
| 独立状态值 | `status` | 表示具体状态值（如 `HttpStatus`） |

**国际化扩展**：

desc 字段为英文 i18n key，后续国际化时可通过以下方式扩展：

```java
// 扩展方式：增加 i18n 字段
public enum UserState implements EnumType {
    E("enabled", "启用"),      // name, desc_en, desc_zh
    D("disabled", "禁用");

    private final String desc;
    private final String descZh;  // 新增中文描述
}
```

参考 ADR-0012（URL 命名规范）和 ADR-0013（错误响应结构）。
