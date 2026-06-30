# 0008 — Layered Component Starters

**Status**: accepted

**Context**:

Cola-style DDD splits a business module into 6 sub-modules: api / domain / app / adapter / infrastructure / start (plus query-server for reads). Initially each business module declared its own Spring dependencies directly — e.g. `soda-user-adapter` had `implementation 'org.springframework:spring-webmvc'`, `soda-user-domain` declared `spring-boot-starter-json` on its own.

As we add more business modules (`soda-order`, `soda-system`, …), each would repeat the same dependency declarations. Worse, nothing enforces the dependency direction between layers — adapter could accidentally depend on infrastructure, app could bypass api and reach into domain internals.

We needed a central place to:
1. Define the base classes (`Entity`, `Aggregate`, `Identifier`, Application Service skeleton, Controller skeleton) — one per layer
2. Enforce dependency direction via Spring Modulith `allowedDependencies`
3. Provide a single dependency for each layer so business modules don't repeat boilerplate

**Decision**:

Create 7 Gradle sub-modules under `soda-components/`, one per architecture layer, with a rigid dependency chain:

```
start → adapter + infrastructure
adapter → app
app → domain + api
infrastructure → domain
query-server → api + infrastructure
domain → (none)
api → (none)
```

Each sub-module:
- Is a `java-library` Gradle project (uses `api` configuration so transitive deps propagate)
- Carries `@ApplicationModule(type = OPEN, allowedDependencies = …)` to enforce dependency direction at test-time
- Declares `@NullMarked` on the package for JSpecify nullness
- Contains the base classes / skeletons for that layer (currently minimal — e.g. `Entity` in domain-starter, `package-info.java` in others)

Business modules replace their raw dependency declarations with `implementation project(':soda-components:soda-component-xxx-starter')`. Example:

```groovy
// soda-user-adapter/build.gradle — before
dependencies {
    implementation 'org.springframework:spring-webmvc'
}
// — after
dependencies {
    implementation project(':soda-components:soda-component-adapter-starter')
}
```

**Rationale**:

- **Physical dependency enforcement**: `allowedDependencies` catches violations at test-time via `ModulithTest`. Adapter cannot depend on infrastructure, domain cannot depend on app — the graph is DAG by construction.
- **DRY across business modules**: `soda-order-adapter`, `soda-system-adapter`, … all use the same starter, getting all base types and transitive deps in one line.
- **Tree-shaking by Gradle**: Since starters use `api` config and business modules use `implementation`, unused starters are not transitively pulled into other layers.
- **Read service remains lightweight**: query-server starter depends on `api` + `infrastructure` only — no app/adapter layer, preserving the yudao-style mixed architecture for reads.

**Considered options**:

- **One `soda-component-all-starter`** that bundles everything. Rejected: defeats dependency enforcement — any module depending on "all" can reach into any layer.
- **No starters, continue with raw per-module declarations**. Rejected: duplicated boilerplate, no enforcement, inconsistent across business modules.
- **BOM-only approach** (one `soda-component-bom` with managed versions, each module still declares its own deps). Rejected: loses the `allowedDependencies` enforcement and transitive `api` propagation.
- **Separate parent POM for each layer group** (e.g. `soda-components-write`). Rejected: Gradle aggregation modules add no value; flat 7-submodule list is simpler.

**Consequences**:

Positive:
- New business modules add one line per layer instead of 3-5 deps
- Dependency direction violations are caught by CI (`ModulithTest`)
- `soda-components` becomes the single source of truth for architecture base types

Negative:
- 7 sub-modules in `settings.gradle` + 7 `build.gradle` — maintenance surface
- If a starter has no base types yet (only a `package-info.java`), it still requires a Gradle sub-module entry
