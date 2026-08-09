# 0019 — adapter starter 按入站通道拆分为子模块

**Status**: accepted

**Context**:

`soda-component-adapter-starter` 是单个 Gradle 模块，内部实际承载三个 Spring Modulith 模块——`web`（`com.soda.component.web`：Result 信封、ErrorInfo、校验注解）、`job`（`com.soda.component.job`：JobContext）、`consumer`（`com.soda.component.consumer`：空占位，仅 package-info），三者均为 `@ApplicationModule(CLOSED, allowedDependencies = {"api"})`。整个模块仅依赖 `spring-boot-starter-validation`，无 auto-config、无 resources——它是基类库，不是 yudao 意义上的 Spring Boot starter。

现状与文档、直觉存在三处偏离：

- **文档幽灵**：CONTEXT.md 记载的 `com.soda.component.adapter` 模块（"Controller/Assembler 基类"）不存在——全仓库 grep 零匹配；任何 `com.soda.component.adapter` 包都不存在。start-starter 的 ModulithTest javadoc 表早已按 web/consumer/job 记录真实模块。
- **"adapter 属基础设施层"不成立**：通道模块 allowedDeps={api}，基础设施 {domain}，两者并列不相交——adapter 家族是独立第三族，不是基础设施的一部分。
- **Gradle 粒度滞后于 Modulith 边界**：Modulith 已拆出 web/job/consumer 三模块，Gradle 仍是单壳。业务侧 GRADLE 拆分的收益今天为零（唯一消费者 soda-user-adapter 同时使用 web+job），拆分是打包对齐，不是新建结构。

消费方：`soda-user-adapter`（web+job 类型：Result/EnumName/Mobile/JobContext）；`soda-component-start-starter`（testRuntimeOnly，其 ModulithTest 扫描全部 `com.soda.component` 模块）。

参考系：yudao-framework 按框架/通道切 17 个 `yudao-spring-boot-starter-*`（BOM 管版本、无聚合 starter、`-rpc` 为纯依赖聚合空壳——先有 14 个消费者后才有壳）；kk-ddd 按层切 `ddd-support-*`（Result/BaseController 等入站件归入"基础设施 Bean 集"——"adapter 属基础设施"直觉的出处，但切分轴与本次决策不同）。

**决策**:

1. **按通道拆分**：`soda-component-adapter-starter` 拆为三个 Gradle 子模块 `soda-component-adapter-starter-{web,job,consumer}`；原模块**删除**，不留聚合壳（yudao 无聚合 starter 先例；空聚合壳最终无人使用）。
2. **包名不变**：`com.soda.component.web|job|consumer`——Modulith 模块名保持通道精确（web/job/consumer）；"adapter" 仅作为构建级家族标签，不进入包名。
3. **consumer 保留空壳**（用户决策，覆盖 YAGNI 建议）：模式连续性优先于"重建成本≈1 个文件"。代价入档：空 Modulith 模块 + settings 条目 + 文档腐化风险（注释须写明存在理由）。ModulithTest 不独立配置——由家族级测试（start-starter 与 domain-types 的 `of("com.soda.component")`）经 testRuntimeOnly 覆盖（见 Consequences）。
4. **不建 rpc starter**：等第一个 Dubbo/Feign 消费者（yudao 先例：壳先于消费者则无人消费）。
5. **不建 BOM/platform**：当前 10 个 starter 由根 build.gradle 统一协调，版本冲突出现时再建。
6. **术语修正**：adapter 家族 = 可选入站通道基类，与基础设施并列、不隶属；放弃"adapter 属基础设施层"表述；CONTEXT.md 幽灵条目删除、词汇表补家族术语。

**Considered Options**:

| 方案 | 结论 |
|---|---|
| 保持单 Gradle 模块现状 | 否定：Modulith 边界已拆而 Gradle 打包滞后；拆分是打包对齐 |
| 迁入 soda-supports（yudao-framework 对应物） | 否定：命名决策落在 soda-components（`soda-component-adapter-starter-XXX`）；soda-supports 保持现状不动 |
| 删除 consumer 空壳（YAGNI） | 用户否决：保留，理由与代价见决策 3 |
| 建 rpc 空壳 | 否定：无消费者 |
| 建 soda-dependencies platform | 否定（暂缓）：版本冲突未出现 |
| 保留原模块作聚合壳 | 否定：yudao 无聚合 starter 先例；消费者已按需声明 |

**Consequences**:

- settings.gradle：+3 子模块、-1（adapter-starter 删除）。
- 三个新子模块不配独立 ModulithTest（仓库实际模式：家族级测试，与 api/application/infrastructure/query-server 等 starter 一致）；start-starter 与 domain-types 的家族级 ModulithTest（`of("com.soda.component")`）经 testRuntimeOnly 覆盖三个新模块。
- 消费方：`soda-user-adapter` 改依赖 web+job+consumer 三个通道 starter；`soda-component-start-starter` testRuntimeOnly 改三个通道 starter。
- 模块内容归属：web 携带 Result/ErrorInfo/validation（含既有测试 MobileTest/EnumNameTest）；job 携带 JobContext；consumer 仅 package-info。
- 文档：CONTEXT.md（幽灵 adapter 条目、mermaid 与模块表、业务模块依赖表、Result/ErrorInfo 归属、词汇表新增 Adapter 家族词条、start 行 allowedDeps 修正为 none）、framework-conventions.md（starter 职责示例行）。ADR-0008/0009 为历史记录不改写，本 ADR 在 adapter starter 打包事项上取代其描述。
