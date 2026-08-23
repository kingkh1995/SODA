# UT（单元测试）规范参考调研（wayfinder 08）

> 研究日期：2026-08-16。来源：Google Testing Blog、Software Engineering at Google（abseil.io 在线全书）、JaCoCo 官方文档（jacoco.org）、SonarQube 官方文档（docs.sonarsource.com）、Martin Fowler bliki（martinfowler.com）、Spring Boot 官方参考文档（docs.spring.io）、Khorikov《Unit Testing Principles, Practices, and Patterns》（作者官网摘录 + 逐章摘要）、仓库内参考项目源码（`yudao-cloud/`、`kk-ddd/`、`COLA/`，只读）。
> 用途：为 SODA（Java 25 / Spring Boot 4 / Gradle / DDD 七子模块读写分离，V1 阶段，无 CI）确定 UT 规范参考基准，并把 `docs/dp-test-conventions.md`（仅领域层 DP）推广到 application / infrastructure / adapter / query-server 层。

---

## 一、候选参考调查

### 1.1 Google（官方测试实践）

#### 1.1.1 Testing Blog《Just Say No to More End-to-End Tests》（2015，Adam Bender）

**是什么**：Google Testing Blog 影响最大的文章之一，论证 E2E 测试应大幅收缩、以单元测试为底座，即经典测试金字塔的 Google 表述。

**规定内容**：主张约 70% 单元 / 20% 集成 / 10% E2E 的近似配比；命名了两个反模式——**ice-cream cone**（大量 E2E 堆在薄弱低层测试之上：慢、脆、难诊断）与 **hourglass**（大量 E2E + 大量单测、缺少中间集成层，导致 E2E 失败本可更早被中层测试捕获）；强调测试套件应快、稳、易诊断，flaky 测试须及时修。

**覆盖率数字**：无门禁数字，只有测试配比近似值（70/20/10）。

**适用性**：金字塔比例对 V1 无 CI 的 SODA 过于精确化，但其反模式识别与「E2E 只做守门员」的定位直接可用。SODA 现状恰好符合金字塔形状（领域/应用层单测为底座，start 层少量 E2E 为塔尖），无需改变形状，只需把各层该测什么写清楚。

> 验证说明：testing.googleblog.com 在本环境直连失败（详见 §三 引用处标注），本文内容由多来源搜索结果佐证，且与 1.1.3 Software Engineering at Google 第 11 章（同作者 Adam Bender）内容同源——该章完整复述了上述反模式与配比主张，可作为一级来源引用。

#### 1.1.2 TotT《Code Coverage Best Practices》（2020-08，Testing on the Toilet 第 113 期）

**是什么**：Google 内部 TotT 系列公开发布的一期，主题为代码覆盖率的正确用法。

**规定内容**：覆盖率应作为**发现工具**而非**目标**；区分 statement/branch/function/path 覆盖；不要追逐 100%（递减收益）；建议配合 mutation testing 识别假阳性；覆盖率报告应进 CI/CD 给实时反馈。

**覆盖率数字**：无门禁数字；明确反对「以 100% 为目标的覆盖率政策」。

**适用性**：与 1.1.3 的 coverage 专节、1.4 Fowler《Test Coverage》结论一致（覆盖率 = 找未测代码，不是质量数字）。此立场是 SODA 推荐基准中「V1 不做覆盖率门禁」的直接依据之一。

> 验证说明：同 1.1.1，直连失败，内容由搜索结果佐证；其论点与 1.1.3 SWE 书 coverage 专节一致。

#### 1.1.3 Software Engineering at Google（在线全书）第 11 章 Testing Overview / 第 12 章 Unit Testing —— Google 官方成文标准（主引用，一级来源）

**是什么**：Google 官方出版、免费公开的工程实践书，第 11 章（Adam Bender）定义测试规模/范围与覆盖率政策，第 12 章（Erik Kuefler）定义单元测试写法。

**规定内容**：

- **测试规模（size）**：small = 单进程、禁止 sleep/IO/阻塞调用（网络、磁盘都不行）；medium = 单机、可多进程、可连 localhost；large = 跨机器。规模由「怎么跑、允许做什么」决定，与 scope 正交。鼓励「永远写尽可能小的测试」。
- **测试范围（scope）配比**：rough guideline——约 80% 窄范围单元测试 / 15% 中范围集成测试 / 5% E2E（按测试用例数计；「every team's mix will be a little different」）。与 1.1.1 的 70/20/10 同源，SWE 书为成文版本。
- **反模式**：ice-cream cone、hourglass（同 1.1.1）。
- **Beyoncé Rule**：「If you liked it, then you shoulda put a test on it」——测试一切你不想坏掉的行为，包括失败路径、性能、安全等。
- **覆盖率专节（A Note on Code Coverage）**：明确反对把覆盖率当目标——「it quickly becomes a goal unto itself」；举 80% 门槛为例，团队会把 80% 当**天花板**而不是地板（80% ceiling effect）；只建议**从 small 测试测量覆盖率**，避免大测试造成的覆盖虚高；覆盖率的作用是「provide some insight into untested code, but it is not a substitute for thinking critically about how well your system is tested」。
- **第 12 章单元测试写法**：① *unchanging tests*——纯重构/加功能/修 bug 都不应改动既有测试，只有行为变更才应动测试；② *test via public APIs*——以用户使用方式调用被测系统，绝不测 private/实现细节（helper class 不要单独成单元，通过其使用方测）；③ *test state, not interactions*——状态测试优于交互测试，过度依赖 mock 框架是 brittle tests 的头号来源，能用真实对象就用真实对象；④ *test behaviors, not methods*——按行为（given/when/then）组织测试，不按方法一一对应；⑤ *clarity*——测试要 complete + concise，一个测试体包含理解它所需的全部信息且无冗余。

**覆盖率数字**：无门禁数字；明确批评把 80%（或任何数字）设为门槛的做法。

**适用性**：**最高**。这是唯一把「单元测试怎么写、测什么、不测什么、覆盖率怎么用」全部写成文的官方标准，且与 SODA 现有测试风格高度吻合（§二 详述）。第 12 章五条写法可直接搬进 SODA 分层规范作为通用原则；规模三分法为「哪些测试允许碰 DB/网络」提供了现成判据。

### 1.2 JaCoCo 官方文档

**是什么**：JaCoCo 是 Java 覆盖率工具的事实标准，官方文档定义计数器语义与规则机制。SODA `build.gradle` 当前**无 JaCoCo**（仅有 JUnit 5 / spring-boot-starter-test / spring-modulith-starter-test / mapstruct / Lombok test 依赖）。

**规定内容**（均为机制，**无推荐阈值**）：

- **计数器（counters）**：Instructions（C0）、Branches（C1，仅 if/switch 分支，**异常处理不算分支**）、Cyclomatic Complexity（v(G)=B−D+1）、Lines（需 debug 信息）、Methods、Classes。指令覆盖与分支覆盖在无 debug 信息时也可得；行覆盖需要 `-g` 编译。
- **check 规则（Maven check-mojo / Gradle jacocoTestCoverageVerification 同语义）**：规则元素 BUNDLE / PACKAGE / CLASS / SOURCEFILE / METHOD；限额按计数器（INSTRUCTION / LINE / BRANCH / COMPLEXITY / METHOD / CLASS）× 取值（TOTALCOUNT / COVEREDCOUNT / MISSEDCOUNT / COVEREDRATIO / MISSEDRATIO）；**默认值：element=BUNDLE、counter=INSTRUCTION、value=COVEREDRATIO**；`haltOnFailure` 默认 true；比率可用 0.80 或 80%。官方示例：整体指令覆盖 ≥80% 且无遗漏类；或每类行覆盖 ≥50%（排除 `*Test`）。
- **Gradle 插件要点**：`jacocoTestReport` 任务**不依赖 test**（需手动 `finalizedBy`/`dependsOn` 挂接）；`jacocoTestCoverageVerification` **默认不挂到 check**（文档明言原因：该任务非增量，挂上即任何违规自动失败 build，未必是所有人想要的行为）。

**覆盖率数字**：无。JaCoCo 提供的是「怎么测覆盖率」，阈值由使用者（如 SonarQube Sonar way 的 80%）决定。

**适用性**：工具层直接适用。SODA 要落地覆盖率只需 `plugins { id 'jacoco' }` + `test.finalizedBy jacocoTestReport`。若未来要门禁，规则语义（BUNDLE 粒度、COVEREDRATIO、`*Test` 排除、按层 include/exclude）可直接套到七子模块。注意 Gradle 下验证任务默认不参与 `check`——门禁需要显式接线，这反而给 V1「只出报告不拦截」提供了现成开关。

### 1.3 SonarQube 质量门（Sonar way）

**是什么**：SonarQube 内置默认质量门，Sonar 官方推荐；是业界事实上的「覆盖率阈值」来源。

**规定内容**（四条条件，全部针对 **new code**）：

1. 无新增 issue（或 Reliability/Security/Maintainability 评级均为 A）；
2. 新增 Security Hotspot 100% 已审阅；
3. **新增代码测试覆盖率 ≥ 80.0%**；
4. **新增代码重复率 ≤ 3.0%**。

另有 **fudge factor**（默认开启）：**新增可覆盖行数 < 20 时忽略覆盖率与重复率条件**，避免小改动被过度惩罚；PR 分析只应用 new code 条件。设计哲学明确：只卡新代码质量，不为存量代码的历史债买单。

**覆盖率数字**：**new code 80%**——这是所有候选中唯一成文的硬数字。

**适用性**：数字可直接引用，但**门禁形态对 SODA 不适用（现阶段）**：质量门依赖 SonarQube 实例 + CI 分析运行，SODA 无 CI 无 Sonar。推荐把「new code ≥80% + 20 行 fudge」作为**未来的参考线**（见 §2.3），而非今天的强制项；fudge factor 思想（小改动豁免）值得写进未来门禁设计。

### 1.4 Martin Fowler

#### 1.4.1《Test Pyramid》（2012）

**是什么**：测试金字塔的权威表述（概念源自 Mike Cohn 2009《Succeeding with Agile》）。

**规定内容**：低层单元测试数量应远多于高层 GUI/E2E 测试；GUI E2E 测试 brittle、贵、慢，且易产生非确定性；金字塔中段是 **subcutaneous tests**（穿透服务层/API 层、避开 UI 的测试）；**高层测试是第二道防线**——高层测试失败说明「功能代码有 bug + 缺一个单元测试」，修 bug 前应先用单元测试复现（bug 才真正死透）。

**覆盖率数字**：无。

**适用性**：金字塔形状 = SODA 现状；「高层失败 → 先补单测复现」的流程值得写进规范；subcutaneous 概念对应 SODA 的 adapter/start 层测试定位。

#### 1.4.2《Test Coverage》（2012）

**是什么**：Fowler 对「覆盖率数字崇拜」的批判。

**规定内容**：

- 「Test coverage is a useful tool for finding untested parts of a codebase. Test coverage is of little use as a numeric statement of how good your tests are.」
- 引 Brian Marick：「I expect a high level of coverage. Sometimes managers require one. There's a subtle difference.」——把覆盖率设为 target 会诱导人凑数字（如 AssertionFreeTesting 式空断言测试）。
- 参考数字：认真测试的套件覆盖率应在 **upper 80s–90s**；**100% 可疑**（为数字而写测试）；**低于一半是危险信号**；但高数字本身不能证明测试足够。
- 「测够了」的判定不是数字，而是：**很少漏 bug 到生产 + 改代码时很少因怕回归而犹豫**。

**覆盖率数字**：无门禁；经验区间 upper 80s–90s，<50% 危险。

**适用性**：与 Google 立场完全一致，构成「覆盖率 = 发现工具」论点的第二个一级来源；「两个判据」（漏 bug 少、敢改代码）可作为 V1 不设门的理由写进规范。

### 1.5 Spring Boot 官方测试文档（Testing Spring Boot Applications）

**是什么**：Spring Boot 官方参考文档测试章，定义 `@SpringBootTest` 与全部测试切片注解。

**规定内容**：

- **`@SpringBootTest`**：默认 `MOCK`（不起真实服务器）；`RANDOM_PORT` / `DEFINED_PORT` / `NONE` 三种变体；测试上下文缓存（同配置只加载一次）；`@TestConfiguration` 嵌套类=追加配置、顶层类=不被扫描、必须显式 `@Import` 才生效。
- **测试切片（auto-configured tests）**：每个 `@…Test` 只加载受限自动配置 + 受限组件扫描。与 SODA 相关的主要有——`@JsonTest`（序列化/反序列化）、`@WebMvcTest`（只扫 `@Controller`/`@ControllerAdvice`/`@JacksonComponent`/`Converter`/`Filter`/`HandlerInterceptor`/`WebMvcConfigurer` 等，**普通 `@Component` 不扫**，常配 `@MockitoBean` 提供协作对象，自动配置 MockMvc / MockMvcTester）、`@DataJpaTest`（扫 `@Entity`，classpath 有内嵌库则自动配置，默认事务回滚，注入 TestEntityManager）、`@JdbcTest`/`@DataJdbcTest`/`@DataR2dbcTest`/`@JooqTest`/`@DataMongoTest`/`@DataRedisTest` 等。**一个测试类不能叠多个切片注解**——要多个切片就选一个主注解手动补 `@AutoConfigure…`。
- **Mock Bean**：Spring Framework 7.x 起为 `@MockitoBean` / `@MockitoSpyBean`（旧 `@MockBean`/`@SpyBean` 的替代），在 ApplicationContext 内定义 Mockito mock/spy。
- **结构建议**：测试类按包结构镜像生产代码；主配置自动发现（向上找 `@SpringBootApplication`）。

**覆盖率数字**：无。

**适用性**：切片是「medium-scope、仍在单机内、快且确定」的测试形态，正好落在 Google 规模三分法的 medium 档。对 SODA 的具体意义：① adapter 层若未来想验证 MVC 映射/参数校验/序列化，用 `@WebMvcTest` + `@MockitoBean` 而非全量 `@SpringBootTest`；② 数据访问层验证用 `@DataJpaTest`（SODA 用 Spring Data JPA + H2，见 §2.2 基础设施行）比全量上下文便宜；③ `@MockitoBean` 是 yudao 式「Service 测试 mock 其他模块」的官方现成机制；④ 多切片限制意味着 query-server 读写分离两侧若都要测，得分两个测试类。

### 1.6 yudao-cloud（仓库只读调查）

**是什么**：国内高星 Spring Boot 脚手架，`yudao-framework/yudao-spring-boot-starter-test` 是独立的测试支撑 starter。

**规定内容**（源码核实）：

- **测试基类族**：`BaseMockitoUnitTest`（纯 Mockito，`@ExtendWith(MockitoExtension.class)`）、`BaseDbUnitTest`（`@SpringBootTest(NONE)` + `@ActiveProfiles("unit-test")` + `@Import` 数据源/MyBatis 自动配置 + `@Sql(clean.sql, AFTER_TEST_METHOD)` 每测试后清库）、`BaseRedisUnitTest`、`BaseDbAndRedisUnitTest`；工具类 `AssertUtils`（`assertPojoEquals`、`assertServiceException` 等）、`RandomUtils`。
- **Service 层约定**（BaseDbUnitTest 类注释原文）：**「对于 Service 层的单元测试，我们针对自己模块的 Mapper 走的是 H2 内存数据库，针对别的模块的 Service 走的是 Mock 方法」**——即 yudao 的 Service「单测」是 @SpringBootTest + H2 内存库的集成式测试；实测 `CodegenServiceImplTest` 即 `extends BaseDbUnitTest` + `@Import(CodegenServiceImpl.class)` + `@MockBean` 跨模块服务 + 真实 Mapper。
- 断言风格：JUnit `Assertions` 与自定义 `AssertUtils` 混用（与 SODA 的 AssertJ-only 不同）。

**覆盖率数字**：无门禁（starter 不含覆盖率配置）。

**适用性**：**中等，且是「反面参照」而非「正面参照」**——yudao 把 Service 层测试做成 @SpringBootTest 集成式，与 SODA 现有的纯 Mockito 风格（`UserServiceImplTest` 用 `@ExtendWith(MockitoExtension.class)` + `@Mock`，不起 Spring 上下文）直接冲突；且 H2 与生产 SQL 的方言差异是已知痛点。SODA 应保留纯 Mockito 单测为主体（更快、更符合 Google/Khorikov 的 public-API/不over-mock 原则），只在**确需验证 DB 行为**时用 H2（SODA 主数据源本就是 H2，见 §2.2，方言问题天然较小）。yudao 的 `@Sql` 后置清理、跨模块服务用 Mock 而非真实现这两点可直接借鉴。

### 1.7 kk-ddd（仓库只读调查）

**是什么**：DDD 分层参考项目（ddd-user/ddd-sales/ddd-support/ddd-job-center）。

**规定内容**（源码核实）：测试集中在两类——① 入口模块的上下文加载冒烟测试（`XxxApplicationTests`，仅验证 Spring 上下文能起）；② support 模块的少量纯单元测试（如 `ConsistentHashRingTest`：JUnit 5 `Assertions`、`should*` 命名、行为导向、无 Spring）。测试整体覆盖稀疏，无覆盖率配置、无分层测试规范文档。

**覆盖率数字**：无。

**适用性**：**低**。其「入口冒烟测试」形态 SODA 已有更完整的对应物（start 层 ModulithTest + E2E）；support 模块纯单测风格与 SODA 组件测试（`soda-component-domain-types` 的 DP 测试）同构但更简陋。无可借鉴的新规则。

### 1.8 COLA（仓库只读调查）

**是什么**：阿里 COLA v5，整洁分层架构参考实现。

**规定内容**（源码核实）：archetype（cola-archetype-light）的测试按**分层分包**：`application/`（Service 测试）、`domain/`（纯单测）、`infrastructure/`（Repo/Gateway/JSON/WireMock 测试）。其中 Service 测试形态为 `@SpringBootTest` + WireMock stub 外部依赖（`@WireMockTest`），场景驱动方法名（`test_session_create`、`test_remaining_insufficient`），JUnit `Assertions`；domain 测试为纯 JUnit。另有 `cola-component-test-container`、`cola-component-unittest` 测试支撑组件。**无覆盖率配置、无门禁文档**。

**覆盖率数字**：无。

**适用性**：**中等**。分层分包（domain 纯单测 / application 集成 / infrastructure 单独成册）与 SODA 七子模块天然对应，印证「按层规定测试形态」是 DDD 项目的通行做法；但其 Service 层一律 @SpringBootTest + WireMock 对无 CI 的 V1 过重（每测试起上下文 + 外部 stub），SODA 现有 Mockito 直测更轻。WireMock 思路在 SODA 未来接真实第三方（短信等）时可复用。

### 1.9 Khorikov《Unit Testing Principles, Practices, and Patterns》（2019）

**是什么**：单元测试领域公认的系统性专著（Manning），作者为 Microsoft MVP Vladimir Khorikov。

**规定内容**（作者官网摘录 + 逐章摘要核实）：

- **单元测试四支柱**：protection against regressions（防回归）、resistance to refactoring（抗重构，即测试不因实现细节变化而误报）、fast feedback（快速反馈）、maintainability（可维护）。前两者互相权衡，由「测试是否耦合实现细节」决定——**黑盒优先**：验证可观察行为（operation/state），不验证内部步骤；**测 unit of behavior 而非 unit of code**（几个类实现一个行为也可作为一个单元）。
- **mock 立场（经典学派）**：mock 只应用于**系统与外部应用之间的通信边界**；用 mock 验证系统内部类间通信 = 耦合实现细节，牺牲抗重构性；过度的 mock 依赖通常是设计问题（类图过大）的信号。
- **覆盖率立场（第 1 章）**：「Coverage metrics are a good negative indicator (low coverage means you're not testing enough) but a bad positive one (high coverage doesn't guarantee good testing quality). Targeting a specific coverage number creates a perverse incentive that goes against the goal of unit testing.」
- **第 7 章代码分类（按 复杂度/领域重要性 × 协作者数）**：**Trivial code**（低复杂度少协作者）→ 不测；**Domain model and algorithms**（高复杂度少协作者）→ 重点单测，高价值低成本；**Controllers**（低复杂度多协作者）→ 由集成测试顺带覆盖；**Overcomplicated code**（两者都高）→ 拆分为 domain + controller 再测。
- **第 8 章**：后端通常三层足够——domain model / application services（controllers）/ infrastructure；集成测试覆盖 happy path + 单测覆盖不到的边界，单测覆盖尽量多的业务边界。

**覆盖率数字**：无门禁；与 Google/Fowler 同一立场（负向指标有效、正向指标无效、设数字门槛产生反激励）。

**适用性**：**最高之一**。第 7 章分类可直接映射 SODA 七子模块（domain=Domain model；application/adapter=Controllers；infrastructure= 混合：convertor/gateway 逻辑属 domain-like、repository 属 infrastructure 边界）；「mock 只在外部边界」与 SODA 现有测试的 mock 用法完全一致（application 层 mock gateway/eventBus，adapter 层 mock service——都是跨边界）；四支柱可作为测试评审标准（code-review 技能可据此判「这个测试值不值得存在」）。

---

## 二、推荐基准（SODA）

### 2.1 组合与理由

**推荐：以 Google SWE 书单元测试学说（1.1.3）+ Fowler 金字塔（1.4）+ Khorikov 四支柱与代码分类（1.9）为「设计哲学」；JaCoCo（1.2）为「测量工具（只出报告、不设门）」；Sonar way new-code 80%（1.3）为「未来参考线（明确不强制）」；Spring Boot 切片（1.5）为「需要上下文时的工具形态」；yudao/COLA 为「特定做法借鉴」而非整体范式。**

理由（对应本节约束）：

1. **与现状一致**：SODA 现有 63 个测试文件已天然呈现该组合——domain 层按 `dp-test-conventions.md` 测 DP；application 层（`UserServiceImplTest` 等）为 `@ExtendWith(MockitoExtension.class)` + `@Mock` 边界的纯 Mockito 单测；adapter 层（`UserControllerTest`）直接 new 控制器 + mock 服务 + MapStruct `Mappers.getMapper`；infrastructure 层（`VerificationGatewayImplTest`）mock repository 测 gateway 逻辑；start 层为 ModulithTest + 少量 @SpringBootTest E2E。推荐基准 = **把既有好实践显式化**，而非引入新范式。
2. **覆盖率为发现工具而非门禁**（Google SWE 书 / TotT / Fowler / Khorikov 四方一致）：V1 无 CI、无 JaCoCo，设数字门既无执行载体，又会诱发「凑覆盖率」行为（80% 天花板效应）。这与 `dp-test-conventions.md` 现状一致——该文档通篇无覆盖率要求。
3. **Khorikov 第 7 章分类解决「各层测什么」**：这正是 `dp-test-conventions.md` 要推广到其他层时最缺的一环。

### 2.2 分层映射（七子模块 × 测什么 / 怎么测 / 工具）

| 子模块 | Khorikov 分类 | 测什么 | 测试形态（与现有文件对应） | 工具 |
|---|---|---|---|---|
| api | Controllers（薄） | 命令/查询/DTO 契约 | 基本不直接测，由 application/adapter 测试覆盖（Google：helper 不单独成单元） | — |
| domain | **Domain model and algorithms（重点）** | DP 按 `dp-test-conventions.md`；聚合/领域服务按行为测（given/when/then），聚合边界内不 mock（经典学派） | 纯 JUnit + AssertJ；`soda-user-domain` 现有 29 个 DP 测试为模板 | JUnit 5, AssertJ, JacksonTester |
| application | Controllers（用例编排） | 用例服务的成功/失败路径、领域事件发布、跨模块调用 | **Mockito 单测**：`@Mock` gateway/eventBus/外部服务，验证状态与外部边界交互（`UserServiceImplTest` 为模板）；`*WiringTest` 类保持最少 | Mockito, AssertJ |
| adapter（web/job/consumer） | Controllers（薄） | 请求→命令映射、DTO 组装、状态码/响应结构 | **Mockito 单测**：直接实例化 controller/assembler（`UserControllerTest` 为模板）；仅当需验证 MVC 映射/参数校验/序列化时用 `@WebMvcTest` + `@MockitoBean`（多切片不叠加） | Mockito, MapStruct `Mappers`, 可选 @WebMvcTest |
| infrastructure | 混合：convertor/gateway 逻辑=domain-like；repository=边界 | convertor 双向映射 round-trip；gateway 状态机/守卫逻辑（mock repository）；repository 真库行为 | convertor/gateway 用 Mockito 单测（`UserConvertorTest`、`VerificationGatewayImplTest` 为模板）；repository 级验证用 `@DataJpaTest`（内嵌 H2，默认事务回滚，SODA 主数据源即 H2，见 ADR-0022） | Mockito, @DataJpaTest, TestEntityManager |
| query-server | Controllers（读侧编排） | 读模型 DTO 映射、投影/查询组装逻辑 | **当前 `src/test` 为空——新增范围**：读侧查询组装用 Mockito 单测；读模型序列化用 `@JsonTest` | Mockito, @JsonTest |
| start | —（组装） | 模块依赖（ModulithTest）、上下文可启动、关键端到端路径 | 保留现有 3 个测试；**数量受控**——E2E 是第二道防线（Fowler），新行为先在低层补单测再考虑 E2E | spring-modulith-starter-test, @SpringBootTest |

**通用规则（从 `dp-test-conventions.md` 直接推广，全层适用）**：AssertJ-only 断言；`@DisplayName` + `@Nested`（方法数 ≤6 平铺）；`should_expectedBehavior_when_condition` 命名；测试数据内联、不建 fixture 层；不为 DRY 写测试基类；`@ParameterizedTest` 仅 ≥4 组数据时用；每个方法一个逻辑断言；黑盒优先（只走公开 API，不测私有/实现细节——Google ch12）；状态断言优先于交互断言（Google ch12）；修 bug 先写复现单测再改码（Fowler）。

### 2.3 覆盖率政策（V1）

- **现状**：`build.gradle` 无 JaCoCo；`dp-test-conventions.md` 无覆盖率要求。**保持。**
- **推荐**：V1 不引入覆盖率门禁，可选地接入 JaCoCo **只出 HTML 报告**（`plugins { id 'jacoco' }` + `test.finalizedBy jacocoTestReport`），把报告当「找未测代码」的发现工具（Google/Fowler/Khorikov 共识），人工按 Khorikov 第 7 章分类判断未覆盖代码是否该测——**不设自动阈值**。
- **未来（CI 落地后）**：参考 **Sonar way：new-code 覆盖率 ≥80% + 新增可覆盖行 <20 豁免（fudge factor）+ 新代码重复 ≤3%**；门禁只卡新代码，不背存量债。若用 Gradle `jacocoTestCoverageVerification` 做门禁，注意其**默认不挂 `check`**，需显式接线；规则粒度建议 BUNDLE（模块级）起步，逐层 include/exclude（排除 `*Test`）。
- **为什么 V1 不设门**：① 无 CI 无执行载体；② 四方来源一致警告数字门槛的反激励（80% 天花板效应、为数字写测试）；③ Fowler 的充分性判据（漏 bug 少 + 敢改代码）比任何数字更符合 V1 目标。

### 2.4 可直接采纳 vs 需适配

**可直接采纳（as-is）**：

- Google ch12 五条写法（unchanging tests / public API / state-not-interactions / behaviors-not-methods / clarity）——通用原则，与现有风格零冲突；
- Google 测试规模三分法（small 单进程无 IO / medium 单机 localhost / large 跨机）——用于判定「这测试能不能这么写」；
- Fowler 金字塔 + 高层失败先补单测的流程；
- Khorikov 四支柱 + 第 7 章代码分类 + mock 仅外部边界；
- JaCoCo 计数器与规则语义、Sonar way 的 80% + fudge factor 数字（作为参考线）；
- `dp-test-conventions.md` 的全部风格条款（推广到各层）；
- yudao 的「测试后 `@Sql` 清理」「跨模块服务用 mock」、COLA 的「按层分包测试」布局。

**需适配（needs adaptation）**：

- **配比数字**：Google 80/15/5（或博客 70/20/10）是「rough guideline」，V1 无 CI 小团队照抄无意义——适配为**原则**（单测为底座、集成/切片居中、E2E 最少）而非比例目标；
- **Spring Boot 切片**：切片是 medium 档工具，**按需取用**——SODA 现有 adapter 直测（不碰 MockMvc）更快更不易碎，切片只在确需 MVC 容器行为时上；多切片不叠加的限制写进规范；
- **yudao Service 测试范式**：其 @SpringBootTest+H2 的「单测」与 SODA 纯 Mockito 风格冲突，**不采纳为 Service 层默认**；H2 仅用于 repository 级验证（SODA 主数据源本就是 H2，方言风险低于 yudao 场景）；
- **Sonar 质量门形态**：需 SonarQube 实例 + CI，V1 不适用，只取数字与 fudge 思想；
- **query-server 层**：现有 `src/test` 为空，规范需**新增**该层条目（§2.2 已给）。

### 2.5 权衡（tradeoffs）

1. **数字门禁 vs 覆盖率为发现工具**：门禁（Sonar way 80%）给出明确契约、可自动化执行，但三方一致警告天花板效应与凑数字行为；发现工具（报告 + 人工判断）质量更高但依赖纪律，无 CI 时形同虚设。**选择：V1 发现工具，门禁留给 CI 时代，且门禁只卡 new code。**
2. **纯 Mockito 单测 vs H2 集成式「单测」（yudao 范式）**：前者快、稳、无方言问题，但验证不到真实 SQL/映射；后者覆盖真实持久化路径，但慢、需清理、H2 与生产方言有漂移风险。**选择：单测为主（现有风格），H2 只在 repository 层（`@DataJpaTest`）——SODA 生产即 H2，风险最小化。**
3. **直测控制器 vs `@WebMvcTest` 切片**：直测快且不受 Spring MVC 内部变化影响，但覆盖不到 MVC 绑定/校验/序列化；切片覆盖更全但每次改动上下文更重。**选择：默认直测，切片按需。**
4. **E2E 数量**：E2E 价值高但脆、慢、非确定（Google/Fowler 共识）；start 层测试是「守门员」不是「主力」。**选择：严格控制增长，新行为先在低层补单测（Fowler 流程）。**
5. **「测 unit of behavior」vs「按方法一一对应」**：前者抗重构但需要设计判断，后者机械但易写。Khorikov/Google 都选前者；`dp-test-conventions.md` 已按 DP 形态（behavior 的雏形）组织，推广时保持此取向。

### 2.6 落地顺序（与 V1 无 CI 现状匹配）

1. **立即（纯文档）**：把 §2.2 分层映射 + §2.4 通用规则并入（或作为）`dp-test-conventions.md` 的推广版（另立文档或扩展现文档，由文档体系定案决定）；`docs/research` 本期只出本调研。
2. **可选（零风险）**：根 `build.gradle` 挂 JaCoCo 出报告（V1 只读不拦）。
3. **CI 落地后**：SonarQube（或 Gradle 验证任务）接 new-code 80% 门禁 + fudge factor；仓库级覆盖趋势入日常评审。

---

## 三、引用

> 验证状态图例：**[直接]** = 本环境直读原文/源码；**[搜索佐证]** = 目标站点直连失败，内容由多来源搜索结果交叉佐证（要点已与同源一级来源核对）。

### 3.1 Google

| 引用 | URL | 状态 |
|---|---|---|
| Testing Blog《Just Say No to More End-to-End Tests》(2015, A. Bender)：70/20/10 配比、ice-cream cone/hourglass 反模式、E2E 守门员定位 | https://testing.googleblog.com/2015/04/just-say-no-to-more-end-to-end-tests.html | [搜索佐证]；内容与 SWE 书 ch11 同作者同源 |
| TotT《Code Coverage Best Practices》(2020-08, 第 113 期)：覆盖率=发现工具、不追 100%、mutation testing、覆盖率报告进 CI | https://testing.googleblog.com/2020/08/code-coverage-best-practices.html | [搜索佐证]；论点与 SWE 书 coverage 专节一致 |
| Software Engineering at Google 第 11 章 Testing Overview（A. Bender）：测试规模 small/medium/large 定义、80/15/5 配比、Beyoncé Rule、反模式、coverage 专节（80% 天花板效应、只从 small 测试测覆盖率） | https://abseil.io/resources/swe-book/html/ch11.html | [直接] |
| Software Engineering at Google 第 12 章 Unit Testing（E. Kuefler）：unchanging tests、test via public APIs、test state not interactions、test behaviors not methods、clarity | https://abseil.io/resources/swe-book/html/ch12.html | [直接] |
| Fowler《Test Pyramid》对 Google 博客的引用（2015 版 URL） | https://martinfowler.com/bliki/TestPyramid.html | [直接，见 3.4] |

### 3.2 JaCoCo / SonarQube

| 引用 | URL | 状态 |
|---|---|---|
| JaCoCo Coverage Counters：C0/C1 定义、异常处理不算分支、复杂度公式 v(G)=B−D+1、行覆盖需 debug 信息 | https://www.jacoco.org/jacoco/trunk/doc/counters.html | [直接] |
| JaCoCo check-mojo：规则元素/计数器/取值、默认 BUNDLE+INSTRUCTION+COVEREDRATIO、haltOnFailure、官方示例（BUNDLE 80% / CLASS LINE 50% 排除 *Test） | https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html | [直接] |
| Gradle JaCoCo Plugin：jacocoTestReport 不依赖 test；jacocoTestCoverageVerification 默认不挂 check（非增量、避免自动失败 build） | https://docs.gradle.org/current/userguide/jacoco_plugin.html | [直接] |
| SonarQube《Understanding quality gates》：Sonar way 四条件（new code 覆盖率 ≥80.0%、重复 ≤3.0%、无新 issue、hotspot 全审）、fudge factor（新增可覆盖行 <20 豁免覆盖/重复条件）、PR 只应用 new code 条件 | https://docs.sonarsource.com/sonarqube-server/quality-standards-administration/managing-quality-gates/introduction-to-quality-gates | [直接] |

### 3.3 Spring Boot

| 引用 | URL | 状态 |
|---|---|---|
| Testing Spring Boot Applications：@SpringBootTest 四种 webEnvironment、上下文缓存、@TestConfiguration 嵌套/顶层差异、@MockitoBean、切片限制组件扫描、多切片不叠加、@JsonTest/@WebMvcTest/@DataJpaTest/@JdbcTest 等切片语义、MockMvcTester、事务回滚 | https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html | [直接]（Boot 4.1.0 文档） |

### 3.4 Martin Fowler

| 引用 | URL | 状态 |
|---|---|---|
| 《Test Pyramid》(2012)：金字塔形状、GUI E2E 的 brittle/慢/贵、subcutaneous tests、高层=第二道防线、修 bug 先复现单测 | https://martinfowler.com/bliki/TestPyramid.html | [直接] |
| 《Test Coverage》(2012)：覆盖率=找未测代码而非质量数字、Marick 引文、upper 80s–90s、100% 可疑、<50% 危险、充分性两判据 | https://martinfowler.com/bliki/TestCoverage.html | [直接] |

### 3.5 Khorikov《Unit Testing Principles, Practices, and Patterns》（Manning, 2019）

| 引用 | 来源 | 状态 |
|---|---|---|
| 四支柱（protection against regressions / resistance to refactoring / fast feedback / maintainability）；黑盒优先；unit of behavior；mock 仅外部边界；覆盖率=负向指标有效/正向无效、设数字门槛产生反激励（第 1 章） | 作者官网第 1 章摘录：https://enterprisecraftsmanship.com/files/Unit-Testing-Chapter-1-Excerpt.pdf | [搜索佐证：摘录 PDF 标题与作者官网互证] |
| 第 7 章代码分类（trivial 不测 / domain+algorithms 重点单测 / controllers 集成测 / overcomplicated 拆分）；第 8 章三层结构与集成测试定位；四支柱细目 | 逐章摘要：https://olano.dev/blog/unit-testing-principles | [直接]（摘要文；书本体为印刷版，未全文直读——引文以作者官网摘录 + 摘要双重核对） |

### 3.6 仓库参考项目（只读，源码核实）

| 引用 | 路径 | 核实内容 |
|---|---|---|
| yudao-cloud 测试支撑 starter | `yudao-cloud/yudao-framework/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseMockitoUnitTest.java`、`BaseDbUnitTest.java`（H2 + @Sql 清理 + @ActiveProfiles("unit-test") + 「Service 层走 H2、跨模块 Service 走 Mock」类注释） | [直接] |
| yudao Service 测试示例 | `yudao-cloud/yudao-module-infra/yudao-module-infra-server/src/test/java/cn/iocoder/yudao/module/infra/service/codegen/CodegenServiceImplTest.java`（extends BaseDbUnitTest + @Import + @MockBean） | [直接] |
| kk-ddd 测试 | `kk-ddd/ddd-support/ddd-support-dependencies/src/test/java/com/kk/ddd/support/grl/ConsistentHashRingTest.java`（纯 JUnit5、should* 命名）；各 web 模块 `XxxApplicationTests`（上下文冒烟） | [直接] |
| COLA 测试 | `COLA/cola-archetypes/cola-archetype-light/src/main/resources/archetype-resources/src/test/java/`（application/domain/infrastructure 分层；`ChargeServiceTest` 为 @SpringBootTest + WireMock） | [直接] |

### 3.7 本仓库现状（核实基准）

| 引用 | 路径 | 内容 |
|---|---|---|
| DP 测试规范 | `docs/dp-test-conventions.md` | AssertJ-only、@DisplayName/@Nested、should_*_when_*、无覆盖率要求、仅覆盖领域层 DP |
| 现有测试分布 | `soda-user/**/src/test`、`soda-components/**/src/test` 共 63 个测试文件 | domain 29（DP）、application 5（Mockito）、adapter 2（直测）、infrastructure 4、start 3（Modulith/E2E）、components 20 |
| 测试依赖 | 根 `build.gradle`（已核实事实，未重查） | JUnit 5（junit-platform-launcher）、spring-boot-starter-test、spring-modulith-starter-test、mapstruct test、Lombok testAnnotationProcessor；**无 JaCoCo** |
| 数据层 | `soda-user/soda-user-infrastructure/build.gradle` | Spring Data JPA（Auditable 基类）+ Flyway + **H2 为主数据源（ADR-0022，MODE=MySQL），运行时含 h2 驱动** |
| 参考风格文档 | `docs/research/okf-code-repo-adaptation.md` | 本文档风格参照（研究日期/来源/用途头注 + 表格 + 逐条归因） |

### 3.8 未验证缺口（明确声明）

1. **Google Testing Blog 两篇文章未能直连**（testing.googleblog.com / blogspot 在本环境网络不可达，含 wayback 镜像）：《Just Say No to More End-to-End Tests》与 TotT《Code Coverage Best Practices》的内容按 [搜索佐证] 标注引用，其核心主张已与 SWE 书第 11 章（同作者/同主题的成文标准）核对一致。**如需逐字引文，请在可访问网络下重读原文。**
2. **Khorikov 书本体为印刷版**：本环境无法直读全文；四支柱与覆盖率立场经作者官网第 1 章摘录 + 公开逐章摘要双重核对，其余章节（如第 8 章集成测试细节）为摘要转述。**关键决策引用（四支柱、代码分类、mock 边界、覆盖率立场）已落在两个独立来源交叉点内。**
3. SonarQube 文档为 Server 版路径（docs.sonarsource.com 自动重定向）；Sonar way 的四条件数字（80%/3%/20 行 fudge）在 Server 与 Cloud 版间一致，但如未来对接具体版本请以该版本页面复核。

---

*（完）—— 本调研仅新增 `docs/research/ut-testing-standards.md` 一个文件；未修改任何代码、构建文件或其他文档。*
