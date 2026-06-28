# 0007 — AuthAccountId centralized `@JsonCreator` on sealed base

Centralize JSON deserialization of all `AuthAccountId` subtypes on the sealed base class via `@JsonCreator(mode = Mode.DELEGATING) AuthAccountId.of(String)`, which routes to the correct subclass by extracting the prefix (`P:`, `S:`, `E:`, `O:`). Each subclass provides its own `of(String)` for internal use; the base class dispatches to them.

**Status**: accepted

**Context**: Without a centralized deserializer, Jackson would need either (a) `@JsonTypeInfo` with a type ID field in every serialized form, or (b) per-subtype deserializers registered in the ObjectMapper. Both add coupling to the serialization format that complicates non-Jackson use cases.

**Decision**: The prefix-embedded format (`"P:42"`, `"S:13800138000"`, etc.) makes the type self-describing in a single string — no extra discriminator field needed. The sealed `permits` clause gives a closed set of subtypes for the routing switch, so adding a new subtype breaks the dispatch at compile time.

**Considered Options**:

- **`@JsonTypeInfo(use = Id.DEDUCTION)`** — Jackson deduces subtype from field presence, but requires all subtypes to have disjoint fields, which isn't the case here
- **Wrapper object with type field** — `{ "type": "P", "value": "42" }`, adds noise, breaks the simplicity of `AuthAccountId` as a value
- **Prefix-embedded string (chosen)** — single-string format, self-describing, Jackson-native
