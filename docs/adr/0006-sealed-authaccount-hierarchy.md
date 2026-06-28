# 0006 — Sealed AuthAccount hierarchy

Use `sealed class AuthAccount permits PasswordAuthAccount, SmsAuthAccount, EmailAuthAccount, SocialAuthAccount` instead of an abstract class with type-discriminator field or a `Map<AuthAccountType, AuthAccount>`. This ensures the compiler enforces exhaustive pattern matching in `User.authenticate()` — adding a new Account type breaks the `permits` clause at compile time rather than silently missing a branch at runtime. The sealed shape mirrors the domain invariant: there are exactly 4 authentication modes, and the set cannot be extended at runtime.

**Status**: accepted

**Considered Options**:

- **Abstract class + `AuthAccountType` discriminator field** — runtime dispatch, error-prone when adding new types
- **Sealed class (chosen)** — compile-time exhaustiveness, idiomatic Java 17, matches the fixed-set domain invariant
