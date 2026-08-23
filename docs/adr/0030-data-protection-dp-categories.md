# Data Protection DP Categories: Raw, Encrypted, Hashed, Redacted

## Context

Business requirements demand four distinct data protection patterns:

1. **Raw + Maskable** — Store plaintext; mask only in `toString()`/logs (phone, email, ID card, bank card, real name)
2. **Encrypted** — Reversible: store ciphertext, decrypt at use-time (PII needing recovery)
3. **Hashed** — Irreversible: store hash, compare via hash (passwords, API keys)
4. **Redacted** — Display-only: store already-masked strings, cannot be reversed (frontend lists, log archives, non-sensitive query APIs)

Existing `CredentialHash` and `RawCredential`/`Secret` covered part of hashing and secret handling, but lacked:
- Unified base hierarchies for each category
- Explicit masking capability as a reusable trait
- Cross-category transformation contracts

## Decision

### 1. `SensitiveValue` (formerly `SensitiveLiteral`) — Abstract Base Class (domain-starter)

```java
public abstract class SensitiveLiteral implements StringLiteralType {
    protected SensitiveLiteral(String value) { /* blank 校验 */ }
    @JsonValue public final String value() { ... }
    public abstract String maskedValue();
    public final String toString() { return getClass().getSimpleName() + "[masked=" + maskedValue() + "]"; }
    // final class-aware equals/hashCode
}
```

All sensitive DPs **must extend this class** — toString masking is enforced by the type system at compile time, not by convention or tests. This is deliberately a base class rather than an interface: Java forbids default methods overriding `Object.toString`, and records cannot participate in class hierarchies — so all sensitive DPs are classes (see Consequences).

- `Maskable` interface was folded into this base and deleted.
- Equality is class-aware: distinct concrete subtypes are never equal even with identical stored values.

### 2. Encryption DP Hierarchy

```
EncryptedValue (sealed interface)
    └── permits AbstractEncryptedValue (abstract non-sealed class)
         ├── EncryptedString
         ├── EncryptedMobile
         ├── EncryptedEmail
         ├── EncryptedIdCard
         ├── EncryptedBankCard
         └── EncryptedRealName
```

- **Algorithm**: AES-GCM (authenticated encryption), random 12-byte IV per encryption
- **Key Management**: Single master key → HKDF derives per-field-type keys via `KeyRegistry`
- **Ciphertext format**: Base64(IV || ciphertext || tag)
- **JSON serialization**: `value()` returns Base64 ciphertext
- **Equality**: Class-aware (`getClass()` check) + ciphertext-based
- **Masking**: `maskedValue()` shows deterministic ciphertext prefix fallback (`AbCdEfGh...`); stateless and immutable — the earlier lazy-cache was removed once decryption moved out of the DP (its premise vanished; caching a pure function is pure overhead)
- **Base validation**: `AbstractEncryptedValue` constructor calls `ValidateUtils.isBase64()`
- **Decryption**: Through `Decryptor` gateway at application layer; DPs do not expose decrypt methods

### 3. Hashing DP Hierarchy

```
HashedValue (sealed interface)
    └── permits AbstractHashedValue, CredentialHash
         ├── AbstractHashedValue (abstract non-sealed class)
         │    ├── HashedPassword
         │    └── HashedApiKey
         └── CredentialHash (legacy record, direct permit)
```

- **Algorithm-agnostic**: Delegates to `Hasher` gateway (BCrypt, Argon2, SHA-256+Pepper etc.)
- **Salt embedded**: Hash string contains algorithm identifier, salt, iterations
- **Verification**: Through `Hasher.verify()` at application layer
- **Masking**: Algorithm prefix (first 7 chars) + `***`
- **`CredentialHash`**: Legacy record kept as direct sealed permit (records cannot extend classes); implements `HashedValue`, duplicating the prefix-masking logic inherent to record semantics

### 4. ~~Redacted DP Hierarchy~~ （由 ADR-0032 取代：Redacted*→Masked* 记录）
> **2026-08-23 修订**：本节（Redacted DP 层级）已被 **ADR-0032** 取代——`Redacted*`→`Masked*`（record，实现 `StringLiteralType`，非 `SensitiveValue` 子类）；`RedactedValue` 密封接口与 `AbstractRedactedValue` 抽象类删除；原始值 DP 移除缓存的脱敏实例与 `redacted()`，`maskedValue()` 委托 `MaskedXxx.maskOf`。下文保留为历史设计记录，现行形态以 ADR-0032 为准。

```
RedactedValue (sealed interface)
    └── permits AbstractRedactedValue (abstract non-sealed class)
         ├── RedactedMobile    (format: 138****8000)
         ├── RedactedEmail     (format: t***@example.com)
         ├── RedactedIdCard    (format: 110101********1234)
         ├── RedactedBankCard  (format: 622588******6789)
         └── RedactedRealName  (format: 张* / 欧阳*)
```

- **Value is the redacted string**: `value()` returns the masked string directly
- **Irreversible**: No original data held, no keys, no decrypt/verify operations
- **Format validation**: Each concrete class regex-validates the redacted format on construction
- **`maskedValue()`**: Returns `value()` (already masked)
- **Cross-category factory**: Each Redacted DP provides `from(RawXxx)` delegating to the Raw DP's `maskedValue()` — single source of truth for masking logic:
  ```java
  RedactedMobile.from(Mobile)      // → Mobile.maskedValue()
  RedactedEmail.from(Email)
  RedactedIdCard.from(IdCard)
  RedactedBankCard.from(BankCard)
  RedactedRealName.from(RealName)
  ```

### 5. Gateway Interfaces

All gateways extend the `Gateway` marker interface for Spring scanning/AOP consistency.

#### Encryption

```java
public interface Encryptor extends Gateway {
    EncryptedMobile encrypt(Mobile mobile);
    EncryptedEmail encrypt(Email email);
    EncryptedIdCard encrypt(IdCard idCard);
    EncryptedBankCard encrypt(BankCard bankCard);
    EncryptedRealName encrypt(RealName realName);
    EncryptedString encryptGeneric(String plaintext);
}

public interface Decryptor extends Gateway {
    Mobile decrypt(EncryptedMobile encrypted);
    Email decrypt(EncryptedEmail encrypted);
    IdCard decrypt(EncryptedIdCard encrypted);
    BankCard decrypt(EncryptedBankCard encrypted);
    RealName decrypt(EncryptedRealName encrypted);
    String decryptGeneric(EncryptedString encrypted);
}
```

`Encryptor`/`Decryptor` are deliberately separate (principle of least privilege): an encrypt-only service must not hold decryption capability.

#### Hashing — merged hash + verify (PasswordEncoder pattern)

```java
public interface Hasher extends Gateway {
    HashedValue hash(String algorithm, String plaintext);
    boolean verify(String algorithm, String plaintext, HashedValue hash);
    default HashedPassword hashPassword(String plaintext) { ... }
    default boolean verifyPassword(String plaintext, HashedPassword hash) { ... }
    default HashedApiKey hashApiKey(String plaintext) { ... }
    default boolean verifyApiKey(String plaintext, HashedApiKey hash) { ... }
}
```

Follows Spring Security `PasswordEncoder` (`encode()` + `matches()`) and the existing `CredentialHasher` pattern. No separate `Verifier` interface. Builtin algorithm identifiers are centralized in the `HashAlgorithm` enum (`PASSWORD`/`API_KEY`/`CREDENTIAL`); `hash(String, ...)` stays open for custom infra-registered algorithms.

#### Legacy consolidation

```java
public interface CredentialHasher extends Hasher {
    default CredentialHash hash(RawCredential credential) {
        return (CredentialHash) hash(HashAlgorithm.CREDENTIAL.key(), credential.rawValue());
    }
    default boolean matches(RawCredential credential, CredentialHash hash) {
        return verify(HashAlgorithm.CREDENTIAL.key(), credential.rawValue(), hash);
    }
}
```

`CredentialHasher` IS-A `Hasher` — no parallel contracts. Implementations only implement `Hasher.hash()`/`Hasher.verify()`; legacy convenience methods delegate.

#### Key management

```java
public interface KeyRegistry extends Gateway {
    SecretKey getOrCreateKey(String fieldType);
}
```
> 注：2026-08-23 `KeyRegistry` 端口已删除——密钥注册/派生是基础设施内部件而非领域端口（`getOrCreateKey(String)` 亦违反 ADR-0033 签名纪律）；字段级 HKDF 派生改由未来加密基础设施实现内部持有。

### 6. Serialization Summary

| Category | `value()` (JSON/DB) | `toString()` (logs) | Reversible |
|----------|---------------------|---------------------|------------|
| Raw (`Mobile` etc.) | Plaintext | `Mobile[masked=138****8000]` | — |
| `EncryptedXxx` | Base64(ciphertext) | `EncryptedMobile[masked=AbCd...]` | ✅ via Decryptor |
| `HashedXxx` / `CredentialHash` | Hash string | `HashedPassword[masked=$2a$10$***]` | ❌ verify only |
| `RedactedXxx` | Masked string | `RedactedMobile[masked=138****8000]` | ❌ |
> 注：本表 `RedactedXxx` 行已被 ADR-0032 的 `MaskedXxx` 记录取代（脱敏串形态不变；存储形态与读回承重校验见 ADR-0032）。

## Consequences

- **Positive**: toString masking enforced at compile time for every sensitive DP (base class, no exceptions); `SensitiveLiteral`/`Maskable`/types-root collapsed into one base class (net -2 types); three abstract bases eliminate equals/hashCode/toString duplication; Raw DPs cache the corresponding **Redacted DP** (not a raw String) as their masked value — mask production algorithms and format regexes live authoritatively in the Redacted family (`maskOf`), eliminating the dual-maintenance point; cross-category factories give a single masking truth source; gateway consolidation removes parallel contracts; algorithm-agnostic domain layer
- **Negative**: All sensitive DPs are classes — record ergonomics lost (deconstruction patterns, compact constructors); boilerplate mitigated by Lombok where needed; `dp-conventions` "简单 DP 用 record" amended with a sensitive-DP exemption
- **Removed**: the volatile `cachedMaskedValue` in `AbstractEncryptedValue` — its premise (decrypt-then-mask inside the DP) vanished when decryption moved out; caching a pure function is pure overhead. Encrypted/Hashed/Redacted masked renderings remain uncached pure functions

## Implementation Notes

1. `SensitiveLiteral` (abstract class) lives in domain-starter; `Maskable` was folded into it and deleted
2. Sealed interfaces permit only direct subtypes (the abstract bases; plus `CredentialHash` for `HashedValue`); the sealed interfaces are standalone — a Java interface cannot extend a class, so they no longer reference `SensitiveLiteral` in their hierarchy
3. Concrete DPs contain only: constructor validation + factory methods (+ `from(RawXxx)` for Redacted). The five Raw masking DPs additionally cache their Redacted DP (`private final RedactedXxx masked`) and expose it via `redacted()`; `maskedValue()` bridges as `masked.value()` (base contract returns String). Mask production algorithms live as package-private `maskOf(String)` in each Redacted class — co-located with the format regex, single maintenance point. Lombok is used only where it removes real boilerplate (Email's derived-field getters, field-level `@Getter` + `@Accessors(fluent = true)`; class-level `@Getter` is avoided so it never collides with the base's final `value()`)
4. `Comparable` intentionally omitted from all hierarchies — no natural ordering exists for ciphertext/hashes/redacted strings
5. The three category bases declare `extends SensitiveLiteral implements XxxValue` explicitly — the sealed interface must remain a *direct* superinterface for the `permits` clause to hold
6. Superclass abstract methods take precedence over interface default methods (JLS class-wins rule): `AbstractRedactedValue` must override `maskedValue()` even though `RedactedValue` provides a default — omitting it breaks compilation of all Redacted concretes
7. 2026-08-22：原始五 DP 迁移为 `implements SensitiveLiteral`（历史记录，已被本版基类方案取代）
8. **SecretValue 与 SensitiveLiteral 的边界**（不合并）：`Secret` 抽象基类与唯一子类 `RawCredential` 已合并为独立 final 类 `SecretValue`（domain.types）。其安全姿态与 `SensitiveLiteral` 刻意相反——不实现 `StringLiteralType`（无 `@JsonValue value()`），访问器命名 `rawValue()` 以避开 Jackson 自动发现，相等性为引用级，toString 输出 `[***]`。若并入本基类，继承的 `@JsonValue value()` 会使序列化输出明文密码/Token。二者是同一防御栈的两个层级：SecretValue 永不展示（瞬态凭证载体），SensitiveLiteral 脱敏后展示（长期 PII 状态）——互补而非重复

9. 2026-08-23（ADR-0032）：**§4 Redacted DP 层级被取代**——`RedactedValue` 接口与 `AbstractRedactedValue` 抽象类删除，5 个 `RedactedXxx` 类重构为 `MaskedXxx` 记录（`implements StringLiteralType`），原始值 DP 移除缓存脱敏实例与 `redacted()`、`maskedValue()` 委托 `maskOf`；全文 `SensitiveLiteral` 命名按 ADR-0032 更名为 `SensitiveValue`（类名变更，语义/契约不变，见 §1 标题）。ADR-0030 其余（Encrypted / Hashed 层级、Gateway 契约）不变。
10. 2026-08-23（ADR-0033）：**§2 Encrypted 层级与 §3 Hashing 层级被取代**——类型擦除模型下
   `EncryptedValue`/`HashedValue` 密封根、`AbstractEncryptedValue`/`AbstractHashedValue`
   基类及全部派生空壳删除；加密族唯一代表 `Ciphertext`（JWE compact 自验证，普通
   StringLiteralType），哈希族 `PasswordHash`（PHC 白名单，唯一 SensitiveValue 特例）与
   `Digest`（32 字节，普通 StringLiteralType）；`Hasher`/`CredentialHasher`/
   `HashAlgorithm` 通用算法串分发模型废弃，改场景端口 `Encryptor`/`Decryptor`/
   `PasswordHasher`/`Digester`。完整决策、场景全景与域界判据见 ADR-0033。
   本 ADR 仍有效的部分：Raw 层级与 SensitiveValue 基类、Masked 族（ADR-0032）、SecretValue 边界（注 8）。