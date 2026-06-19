## Parent

`.scratch/soda-user/PRD.md`

## What to build

初始化 soda-user 模块的 7 个 Gradle 子模块骨架。包括：
- `settings.gradle` 中添加 7 行 `include`
- 各子模块 `build.gradle`（最小依赖声明）
- 根包 `package-info.java`（`@ApplicationModule` + `@NullMarked`）
- 写侧 `soda-user-start` 的 `@SpringBootApplication` 启动类
- `ModulithTest`（含 `archunit.properties`）验证模块依赖白名单

不包含任何业务代码。

## Acceptance criteria

- [ ] `./gradlew :soda-user:soda-user-domain:build` 编译通过
- [ ] `./gradlew :soda-user:soda-user-start:build` 编译通过
- [ ] `ModulithTest.verifyModuleStructure()` 通过
- [ ] 所有 7 个子模块的 `build.gradle` 存在且依赖关系正确（domain 依赖 domain-starter，infra 依赖 mybatis starter，等）

## Blocked by

None — can start immediately.
