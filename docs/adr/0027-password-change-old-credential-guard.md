# 0027 — 自助改密增加原密码校验守卫（撤销 ADR-0010 的「不实现旧密码校验」决策）

**Status**: accepted（2026-08-16）

> 取代范围：ADR-0010（2026-08-11）「不实现旧密码校验——admin 重置场景无旧密码语义；用户自助改密时再评估」的决策项——仅限**自助改密路径**（`POST /users/{id}:changePassword`）。

## Context

- ADR-0010（2026-08-11）决策：不实现旧密码校验，理由为 admin 重置场景无旧密码语义，并注明「用户自助改密时再评估」。
- 现状（2026-08-16）：自助改密端点 `POST /users/{id}:changePassword` 已落地，但缺原密码比对——任何能触达该端点（会话 / 已授权身份）的调用无需知晓原密码即可改密，凭证修改无授权防线。
- 仓库尚无认证主体机制（ADR-0026 务实形态：userId 取自路径参数），原密码校验是自助改密场景下「凭证归属」的标准防线。

## Decision

1. **域守卫（不变量单一来源在聚合根）**：`User.changePassword(SecretValue oldCredential, SecretValue newCredential, PasswordHasher hasher)`——经 `PasswordAuthAccount.verify`（`hasher.verify`）比对旧凭证与当前哈希，不匹配抛 `IllegalArgumentException("Invalid old password")`，零变更零事件（不注册 `PasswordChangedEvent`）。守卫顺序：`mustEnable()` 在前（与既有聚合守卫同构，R/D 态用户得到状态语义异常而非密码语义）。
2. **链路透传**：`ChangePasswordCommand` / `ChangePasswordRequest` 增加 `oldPassword` 字段（校验落于 adapter 层请求对象：`@NotBlank @Size(min = 6, max = 32)`，与新密码同一策略；Command 仅透传不注解）；适配层 MapStruct 装配自动映射；应用服务构造 `SecretValue` 透传，不做业务校验（接口「参数契约」约定不变）。
3. **admin 重置（无旧密码语义）场景**：当前仓库无该流程。若未来引入，应走独立命令（如 `AdminResetPasswordCommand`）绕过本守卫，**不**改动自助改密路径的强制校验——两路径语义不同，不共享同一命令。

## 沿革

- 2026-08-11：ADR-0010 决策「不实现旧密码校验」（admin 重置场景无旧密码语义）。
- 2026-08-16：撤销（自助改密路径），实现原密码校验域守卫，见本 ADR。
