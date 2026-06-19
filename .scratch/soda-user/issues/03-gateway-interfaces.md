## Parent

`.scratch/soda-user/PRD.md`

## What to build

Domain 层的所有 Gateway 接口。每个接口继承 `Gateway`（复合标记接口，不含泛型）。

**接口清单：**

```java
interface UserGateway extends EntityGateway<User, UserId> {
    Optional<User> findByUsername(Username username);
    Optional<User> findByMobile(Mobile mobile);
    boolean existsByUsername(Username username);
    boolean existsByMobile(Mobile mobile);
    boolean existsByEmail(Email email);
}

interface PasswordEncoder extends Gateway {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}

interface CodeGenerator extends Gateway {
    String generate(int length);
}

interface SmsSender extends Gateway {
    void send(Mobile mobile, String code);
}

interface EmailSender extends Gateway {
    void send(Email email, String code);
}
```

`UserGateway` 放在 `soda-user-domain` 的 `gateway` 子包下。其他 Gateway 放 domain 根包或 `gateway` 子包均可，需保持风格一致。

## Acceptance criteria

- [ ] 所有 Gateway 接口编译通过
- [ ] 接口继承 `Gateway`，可被 Spring 组件扫描
- [ ] 不包含任何实现代码（实现在 Phase 3）

## Blocked by

`02-domain-primitives.md`
