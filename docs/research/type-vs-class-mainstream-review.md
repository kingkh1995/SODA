---
type: Research
title: 多态实体 type-vs-class 设计结论审视（对照主流 Java/DDD 实践）
description: 锚点：审视或实现多态实体 type-vs-class 设计（sealed/枚举判据、网关 Class<T> 收参、@JsonTypeName 判别、铁律）时，按 OpenJDK/Jackson/DDD 主流来源逐条核对，结论收敛 ADR-0016。
tags: [sealed, enum, jackson, ddd]
status: stable
generated:
  by: wayfinder/01
  at: 2026-08-08T00:00:00Z
verified:
  - by: human:mm
    at: 2026-08-08T00:00:00Z
sources:
  - resource: https://openjdk.org/jeps/409
    id: jep-409
    author: OpenJDK
    title: OpenJDK JEP 409（Sealed Classes）
  - resource: https://openjdk.org/jeps/441
    id: jep-441
    author: OpenJDK
    title: OpenJDK JEP 441（Pattern Matching for switch）
  - resource: https://github.com/FasterXML/jackson-databind
    id: jackson-databind
    author: FasterXML
    title: Jackson Databind 官方仓库与发布说明
---
# 多态实体 type-vs-class 设计结论审视（对照主流 Java/DDD 实践）

> **定案状态（2026-08-05）**：审查结论已由团队采纳并收敛为 ADR——见 [docs/adr/0016-type-class-mapping-ownership.md](../adr/0016-type-class-mapping-ownership.md)。过渡态违例的处置：`VerificationChannel` 去 `Class` 引用与 `of(Class)`、gateway 收敛为 `Class<T>` 单轨、`VerificationQuery` 去 `channel`、反查归基础设施；争议点 2 的取舍已拍板（以类型安全换查询对象可读性，纯数据统计需求走基础设施查询模型）。

> **结论摘要**
> - 整体判定：7 条结论方向**全部符合**主流 Java/DDD 实践，无一条偏离；其中铁律 2（查询对象不含判别枚举）与 DDD"仓储查询用领域语言"存在张力，标注为**有争议点**（非偏离，属取舍）。
> - 最强支撑：JEP 409 官方明确区分"枚举 = 固定实例集 / sealed = 固定种类集"（判据 1 的语言级依据）；JEP 441 官方以模式匹配取代 instanceof+cast 链（铁律 1）；JLS §9.7.1 证明注解 String 元素值必须是编译期常量表达式，`Enum.name()` 无法进注解（铁律 5 的事实前提）。
> - 需注意：Jackson 3 并未"移除 @JsonSubTypes"（3.x 分支仍存在且未弃用），实际变化是 sealed 自动发现使其对 sealed 层次不再必要；当前代码 `VerificationChannel` 仍持 `Class` 引用 + `of(Class)` 反查（违反铁律 2/4），处于过渡态，需按结论收敛。
> - 方法说明：primary sources 优先（OpenJDK JEP/JLS、Fowler 官方站点、FasterXML 官方仓库源码与发布说明、Evans DDD Reference 官方 CC-BY 文档、Jakarta/Spring 官方文档）；无法直接核验的 claim 标注 [secondary] 或 [inference]。

---

## 评估对象与依据说明

- **项目现状**：`AuthAccount`（permits Password/Sms/Email/Social）与 `Verification`（permits Sms/Email）为 sealed 实体层次，`@JsonTypeInfo(use = Id.NAME)` + 子类 `@JsonTypeName` 短名（"P"/"S"/"E"/"O"），判别短名与枚举 `name()` 一致（`AuthAccountType` / `VerificationChannel`，均 `implements EnumType extends Type`）。gateway 为 domain 层防腐接口，infrastructure 尚为空壳。被审视的结论来自设计讨论（B 方案与决策矩阵），非当前代码全量现状。
- **Jackson 2/3 差异说明**：本项目用 Jackson 3（`tools.jackson`）。databind/core 包名 `com.fasterxml.*` → `tools.jackson.*`；**注解包未迁移**（3.x 仍为 `com.fasterxml.jackson.annotation`）；`TypeIdResolver` 接口方法签名在 3.x 增加 `DatabindContext` 参数；3.0.0-rc2 起支持从 Java 17 sealed 类型自动发现子类。下文对存在 2/3 差异处均标注。
- **引用规范**：正文用 `[来源: X](URL)`；书籍（Effective Java、Evans《Domain-Driven Design》原书）以"书名 + Item/章节"为 primary，网页核对链接一并给出；`martinfowler.com/bliki/TypeObject.html` 经核实当前返回 HTTP 404（2026-08-04），改引原始论文（Johnson & Woolf, PLoP 1997）。

---

## 逐条评估

### 结论 1：判据 1/2 — 类型间有行为或字段差异 → sealed class 层次；纯标签/元数据 → 枚举

**主张**：有行为/字段差异的"种类"用 sealed 类层次；纯标签/元数据用枚举。

**主流做法**：

1. **JEP 409（Sealed Classes）官方把两种建模方式并列为互补**：枚举建模"固定数量的实例"，sealed 类层次建模"固定数量的种类"（kinds）——
   > "Java supports *enum classes* to model the situation where a given class has only a fixed number of instances... you can switch over them exhaustively — without having to write a `default` clause"；"sometimes we want to model a fixed set of *kinds* of values... restricting the set of subclasses or subinterfaces can streamline the modeling."
   [来源: JEP 409 Motivation](https://openjdk.org/jeps/409)
2. **Effective Java Item 23「Prefer class hierarchies to tagged classes」**：单一类 + 判别字段（tag）+ switch 是"tagged class"反模式（样板代码、易漏分支、类型不安全、低效），应按行为差异建模为类层次。[来源: 《Effective Java》第 3 版（Addison-Wesley, 2018）Item 23](https://www.informit.com/store/effective-java-9780134685991)（在线核对 [secondary]: https://ahdak.github.io/blog/effective-java-part-3）
3. **Fowler Refactoring 目录**：`Replace Type Code with Subclasses`（type code 影响行为时用子类）与 `Replace Primitive with Object`（别名 `Replace Type Code with Class`，type code 只是数据时用类/枚举）并列给出，判据即"是否影响行为"。 [来源: https://refactoring.com/catalog/replaceTypeCodeWithSubclasses.html](https://refactoring.com/catalog/replaceTypeCodeWithSubclasses.html)、[https://refactoring.com/catalog/replacePrimitiveWithObject.html](https://refactoring.com/catalog/replacePrimitiveWithObject.html)
4. **Type Object 模式（Johnson & Woolf, PLoP 1997）**：适用条件是"子类数量大且/或种类总数未知、需在运行时创建新类型"；其 Consequences 明确写道 *"Use inheritance; it's easier."*——即类型集固定已知时直接用继承（本项目 P/S/E/O 为固定已知集，不适用 Type Object，反证判据 1）。[来源: http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf](http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf)（注：Fowler bliki 同名页 404，该模式另见 Fowler《Analysis Patterns》(1997)，论文参考文献已列 [Fowler97]）

**判定**：**符合**。判据 1 的二分与 JEP 409 官方表述逐字对应（enum=固定实例集，sealed=固定种类集），且 sealed 让 Item 23 的经典建议更进一步——编译器强制子类完备性，为后续穷举分析提供基础（JEP 409 Goals 第 3 条）。

---

### 结论 2：判据 3 — 跨领域边界（持久化列、JSON 判别符、RPC、事件）的值用 type（枚举短名），领域内部流动用 class

**主张**：跨边界传输/存储用稳定的短名枚举值，领域内部用 sealed 类对象。

**主流做法**：

1. **DDD：边界是翻译点，内部是模型**。Evans DDD Reference：
   - Layered Architecture："Isolate the expression of the domain model... eliminate any dependency on infrastructure"、依赖"only on the layers below"——领域层对象不携带基础设施形态，存储/传输形态是边界上的表达。
   - Domain Events："a domain event typically contains a timestamp for the time the event occurred and **the identity of entities involved** in the event"——事件携带实体**身份标识**而非内部对象图；本项目的判别短名恰好编码在 `AuthAccountId` 前缀（`"P:42"`）中，即短名随身份在事件/持久化中流动。
   [来源: Evans, *Domain-Driven Design Reference* (2015, 官方 CC-BY 文档), §Layered Architecture / §Domain Events / §Repositories](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)
2. **Hexagonal（Ports & Adapters）**："The rule to obey is that code pertaining to the *inside* part should not leak into the *outside* part"；adapter 负责把外部信号转换为领域 API——判别值落在存储列/JSON 字段上属 adapter 侧的技术形态。[来源: https://alistair.cockburn.us/hexagonal-architecture/](https://alistair.cockburn.us/hexagonal-architecture/)
3. **边界值应稳定**：AIP-126 规定枚举只用于"变化不频繁的值集合"，频繁变化的值用 string——短名一旦落库/进 JSON 即成对外合同（本项目
   ADR-0005 已承认"枚举常量改名破坏 DB
   数据"）。[来源: https://google.aip.dev/126](https://google.aip.dev/126)；[来源: docs/adr/0005-enum-short-name.md](../adr/0005-enum-short-name.md)
4. **领域内部用 class**：即结论 1/3 的 sealed + 模式匹配（JEP 409/441），见下条。

**判定**：**符合**（附争议点）。方向完全主流："内部对象、边界稳定标识"是 DDD 边界翻译（Evans §Anticorruption Layer："create an isolating layer to provide your system with functionality of the upstream system **in terms of your own domain model**"）与 Hexagonal 的共识。争议点：判别短名既是"技术边界值"，同时又是**领域语言的一部分**（`channel`/`authAccountType` 对领域专家有意义，DDD Reference 明确要求查询条件"meaningful to domain experts"）——如何划界见结论 4 的争议点。

---

### 结论 3：判据 4 + 铁律 1 — 领域内行为分派只用 class + pattern matching，禁止 "type 枚举判断 + class 强转" 成对出现

**主张**：领域内分派只准用 sealed 类 + instanceof/switch 模式匹配；"按枚举判断再强转"是反模式。

**主流做法**：

1. **Effective Java Item 23** 正是对"tag 字段 + switch 分派"的判词：tagged class 样板代码多、分支易漏、把类型安全交给运行时的临时判断。[来源: 《Effective Java》第 3 版 Item 23](https://www.informit.com/store/effective-java-9780134685991)（[secondary] https://ahdak.github.io/blog/effective-java-part-3）
2. **Fowler `Replace Conditional with Polymorphism`**：`switch (bird.type)` 用子类各自实现 `plumage()` 取代。[来源: https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html](https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html)
3. **JEP 441（Pattern Matching for switch）官方把"instanceof + 强转成对出现"列为旧惯用法并给出替代**：
   > "Prior to Java 16" 的 `if (obj instanceof String) { String s = (String)obj; ... }` 被 `if (obj instanceof String s)` 取代；"extending pattern matching to switch... allows complex data-oriented queries to be expressed concisely and safely"。
   Goal："requiring that pattern `switch` statements cover all possible input values"——配合 sealed（JEP 409 "foundation for the exhaustive analysis of patterns"），**漏分支是编译错误**，这正是禁止手写 type-code 判断的根本理由。
   [来源: JEP 441 Motivation/Goals](https://openjdk.org/jeps/441)；[来源: JEP 409 Goals](https://openjdk.org/jeps/409)

**判定**：**符合**。铁律 1 禁止的"枚举判断 + 强转成对"正是 JEP 441 开篇展示的 Java 16 前旧写法；模式匹配 switch（而非手写 if/else instanceof 链）是官方推荐的替代。注意：模式匹配本质仍是"按运行时类型分派"，但与手写 type-code 判断的关键差异是**编译器完备性检查 + 无显式强转**——判据 4 的表述与主流一致。

---

### 结论 4：铁律 2 — 静态 class→type 反查不进领域/应用层；仓储/网关按 Class<T> 收参、返回泛型 Optional<T>，查询对象不含判别枚举

**主张**：`class → VerificationChannel` 之类的静态反查不下放给领域/应用层；判别值仅由基础设施（gateway 实现）在持久化查询需要时自行推导；网关接口用 `Class<T>` 参数 + 泛型返回表达"要哪种子类型"。

**主流做法**：

1. **类型安全仓储是标准做法**：
   - Spring Data：`Repository<T, ID>` 以"领域类 + 标识符类型"为类型参数，`CrudRepository<T, ID>.findById(ID)` 返回 `Optional<T>`——仓储接口的类型参数就是领域类。[来源: https://docs.spring.io/spring-data/data-commons/reference/repositories/core-concepts.html](https://docs.spring.io/spring-data/data-commons/reference/repositories/core-concepts.html)
   - Jakarta Persistence：`EntityManager.<T> T find(Class<T> entityClass, Object primaryKey)`（运行时类令牌 + 泛型返回）；`TypedQuery<X>` "Interface used to control the execution of typed queries"，`getSingleResult()` 返回 `X`。[来源: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/entitymanager](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/entitymanager)、[https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/typedquery](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/typedquery)
   - 运行时类令牌（runtime type token）是官方记载的惯用法："This technique of using class literals as run time type tokens is a very useful trick to know"，示例即 `select(EmpInfo.class, "select * from emps")` 返回 `Collection<T>`。[来源: Oracle Java 教程（Gilad Bracha）](https://docs.oracle.com/javase/tutorial/extra/generics/literals.html)
2. **映射/推导归基础设施**：Evans Repository——查询条件应对领域专家有意义、仓储封装存储与查询技术 [来源: DDD Reference §Repositories](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)；Hexagonal——技术形态转换是 adapter 职责，判别列值推导属于 adapter 内部细节 [来源: https://alistair.cockburn.us/hexagonal-architecture/](https://alistair.cockburn.us/hexagonal-architecture/)。
3. **依赖方向**：领域层不反向依赖基础设施（Evans Layered Architecture / Hexagonal），`Class<T>` 参数让"要哪种子类"留在领域语言内，判别值推导留在基础设施内——方向上与 Fowler EAA Repository"one-way dependency between the domain and data mapping layers"一致。[来源: https://martinfowler.com/eaaCatalog/repository.html](https://martinfowler.com/eaaCatalog/repository.html)

**判定**：**符合**（附争议点）。"按 Class<T> 收参 + Optional<T>/T 返回"有 Spring Data、JPA、官方教程三重佐证，属主流；"判别值推导在基础设施"符合 Hexagonal adapter 职责划分。争议点：
- "查询对象不含判别枚举"是强约束。`VerificationChannel` 本身是**领域枚举**（领域概念，非纯技术值），DDD Reference 要求查询条件"meaningful to domain experts"；从 gateway 查询对象剔除 `channel` 后，领域层只能靠 `Class<T>` 表达"通道"——这是类型安全等价物，但损失了查询对象的可读性/自描述性。若未来出现"按通道统计"等纯数据需求，应在基础设施侧建查询模型，而非在 gateway 接口放回判别枚举。
- `Class<T>` 参数经泛型擦除后是裸 `Class`，gateway 实现须校验调用方传入的是 permits 子类（否则运行时才出错）——JPA `find` 同样在运行时解析，这是该模式的固有代价。
- **现状对照**：`VerificationGateway` 当前同时存在 `VerificationQuery(channel)` 与 `Class<T>` 变体，双轨并存，收敛前存在语义重复（见文末附录）。

---

### 结论 5：铁律 3 — 实例侧 class→type 编码由子类实现（基类抽象方法，如 getChannel()/getAccountType()），编译器强制多态

**主张**：判别值（channel/type）由各子类以抽象方法覆盖实现，编译器保证每个子类都有实现。

**主流做法**：

1. **Replace Conditional with Polymorphism 的落点**：行为（含标识/元数据访问器）下沉到各子类实现。[来源: https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html](https://refactoring.com/catalog/replaceConditionalWithPolymorphism.html)
2. **Effective Java Item 34 的 constant-specific method implementation** 精神一致：每个枚举常量/子类型自带其特有实现，编译器强制每个常量提供。[来源: 《Effective Java》第 3 版 Item 34](https://www.informit.com/store/effective-java-9780134685991)（[secondary] https://ahdak.github.io/blog/effective-java-part-3）
3. **与 Type Object 论文相反路线**：Type Object 把"type 请求"委托给运行时 TypeObject 实例（设计复杂度高，论文警告 "Use inheritance; it's easier"）；本项目类型集固定已知，选择编译期多态（基类抽象方法）是论文推荐的更简路线。[来源: http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf](http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf)
4. **Jackson 侧同构**：`@JsonTypeName` 同样放在每个子类上（"Annotation used for binding logical name that the annotated class has"）——"标识随子类声明"是 Jackson 官方模型。[来源: jackson-annotations @JsonTypeName javadoc](https://github.com/FasterXML/jackson-annotations/blob/3.x/src/main/java/com/fasterxml/jackson/annotation/JsonTypeName.java)

**判定**：**符合**。基类抽象方法强制子类实现是标准多态；与 `@JsonTypeName` 放子类同构。注意：铁律 3 的 `getChannel()`（Java 代码）与 `@JsonTypeName("S")`（注解字符串）是**同一信息的两个副本**，编译器不检查二者一致——这正是铁律 5 测试的必要性来源；也可用自定义 `TypeIdResolver` 收敛（见结论 7）。

---

### 结论 6：铁律 4 — 枚举不持 class 引用（types 包不反向依赖 domain 包，避免包级循环）

**主张**：`VerificationChannel`/`AuthAccountType` 等枚举不持有 `Class<? extends ...>` 字段，`types` 包不反向依赖 `domain` 包。

**主流做法**：

1. **依赖方向一般原则**：Evans Layered Architecture——"depends only on the layers below"；Hexagonal——内部不依赖外部。包级"无环、单向"是这两条在 Java 包结构上的直接应用 [inference]。[来源: DDD Reference §Layered Architecture](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)、[https://alistair.cockburn.us/hexagonal-architecture/](https://alistair.cockburn.us/hexagonal-architecture/)
2. **行为与类型关联的主流形态是"接口 + 枚举实现"而非"枚举持有 Class 引用"**：Effective Java Item 38「Emulate extensible enums with interfaces」——定义行为接口（如 `Operation`），枚举实现之，行为放枚举常量专用实现；类型关联靠接口多态而非 Class 引用。[来源: 《Effective Java》第 3 版 Item 38](https://www.informit.com/store/effective-java-9780134685991)（[secondary] 内容核对见搜索引擎快照：`interface Operation { double apply(double x, double y); }` + `enum BasicOperation implements Operation`）
3. **Type Object 论文对"对象持 type 引用"的警告**：type→class 反向注册表属于该模式的设计复杂度来源；类型集固定时无必要。[来源: http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf](http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf)

**判定**：**符合**（作为项目规范）。方向与主流一致（依赖单向无环、关联靠接口多态）。**但需如实标注**：没有任何 primary source 直接规定"枚举不得持有 Class 引用"——它是"依赖单向 + 单一职责 + 单一事实源"原则的**派生规范** [inference]；社区中 enum 常量持 Class 引用的注册表式写法并非罕见。**现状对照**：`VerificationChannel` 当前仍持 `Class<? extends Verification<?>> type` 字段并提供 `of(Class<T>)` 反查（违反铁律 2/4 与结论 5 的"实例侧编码"路线），`AuthAccountType` 已无 class 引用——代码处于过渡态，需把该映射下沉到 gateway 实现或基础设施映射器（与结论 4 一致）。

---

### 结论 7：铁律 5 — @JsonTypeName 判别串与枚举 name 无法编译期绑定，用反射测试（permits 完备 + 判别值唯一 + 判别值 == 枚举 name）锁定

**主张**：Java 注解值限制使判别串无法在编译期引用枚举 name；用测试兜底是主流做法。

**主流做法与事实核对**：

1. **事实前提（语言层硬约束）**：JLS §9.7.1——"If T is a primitive type or String, then **v is a constant expression** (§15.29)"。`VerificationChannel.S.name()` 是方法调用，不属于 §15.29 常量表达式（常量表达式仅含引用"常量变量"的简单名等；枚举常量非常量变量，`name()` 更不是）——`@JsonTypeName(VerificationChannel.S.name())` **无法编译**。这是 Java 语言层面的事实，与 Jackson 无关。[来源: JLS 21 §9.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.1)、[§15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29)
2. **Jackson 官方模型就是"手工维护字符串映射"**：
   - `@JsonTypeName` 的 `value()` 就是 `String`："Logical type name for annotated type. If missing... defaults to using non-qualified class name as the type."——Jackson 对判别串与枚举的关系一无所知。[来源: jackson-annotations @JsonTypeName javadoc（2.x 与 3.x 一致）](https://github.com/FasterXML/jackson-annotations/blob/3.x/src/main/java/com/fasterxml/jackson/annotation/JsonTypeName.java)
   - 官方自定义扩展点是 `TypeIdResolver`（`idFromValue`/`typeFromId`，建议继承 `TypeIdResolverBase`），`@JsonTypeIdResolver`
     注解 javadoc 明言："In simplest cases this can be a simple class with **static mapping between type names and
     matching classes**"——官方设想即"开发者集中维护一张映射表"。来源: jackson-databind
     TypeIdResolver（[2.x](https://github.com/FasterXML/jackson-databind/blob/2.x/src/main/java/com/fasterxml/jackson/databind/jsontype/TypeIdResolver.java)/[3.x](https://github.com/FasterXML/jackson-databind/blob/3.x/src/main/java/tools/jackson/databind/jsontype/TypeIdResolver.java)）、[@JsonTypeIdResolver javadoc](https://github.com/FasterXML/jackson-databind/blob/2.x/src/main/java/com/fasterxml/jackson/databind/annotation/JsonTypeIdResolver.java)
   - **Jackson 3 变化**：3.0.0-rc2（2025-03-28）"#5025: Add support for automatic detection of subtypes (like `@JsonSubTypes`) **from Java 17 sealed types**"——sealed `permits` 子句替代 `@JsonSubTypes` 注册，编译器已强制"有哪些子类"，但 `@JsonTypeName` 的**值**仍无编译期约束，判别串↔枚举一致性问题原样保留。[来源: jackson-databind 3.x release-notes/VERSION](https://github.com/FasterXML/jackson-databind/blob/3.x/release-notes/VERSION)
3. **Jackson 自身不做判别值唯一性校验**：2.x `TypeNameIdResolver` 源码对重复 id 直接 `idToType.put(id, ...)`（后者覆盖，无 duplicate/conflict 检查）——项目的 `jsonTypeNamesAreUnique` 测试填补了真实空白（[inference: 由源码阅读得出，无 duplicate 检测逻辑]）。[来源: jackson-databind 2.x TypeNameIdResolver](https://github.com/FasterXML/jackson-databind/blob/2.x/src/main/java/com/fasterxml/jackson/databind/jsontype/impl/TypeNameIdResolver.java)
4. **结构性/架构测试是主流**：用测试锁定"编译器管不到的结构不变量"是 Java 社区成熟实践，代表作是 ArchUnit（官方定位即架构/结构测试库，校验包依赖等规则）。[来源: https://www.archunit.org/](https://www.archunit.org/)

**判定**：**符合**。"无法编译期绑定"是 JLS 与 Jackson 双重确认的事实；反射测试（permits 完备性、唯一性、与枚举 name 一致——对应项目 `VerificationSubtypesMatchesPermitsTest`/`AuthAccountSubtypesMatchesPermitsTest`）是务实且主流的兜底。可选的更进一步：用自定义 `TypeIdResolver`（`idFromValueAndType` 返回 `getChannel().name()`）把"单一事实源"从注解移到铁律 3 的实例方法——但 TypeIdResolver 通常落在基础设施层，恰好符合铁律 2 的"映射不进领域层"；在 sealed 自动发现（#5025）下 `@JsonTypeName` 仍是判别串的唯一声明处，测试兜底依然必要。

---

## 争议点 / 需注意清单

1. **"@JsonSubTypes 已移除"表述不准确**：jackson-annotations 3.x 分支仍存在 `JsonSubTypes.java` 且无弃用标注（2026-08-04 核验）；实际变化是 3.0.0-rc2 起 sealed 类型自动发现使其对 sealed 层次不再必要。ADR-0004 中"@JsonSubTypes 已移除"的表述建议修正为"对 sealed 层次不再需要"。[来源: jackson-annotations 3.x 目录/源码](https://github.com/FasterXML/jackson-annotations/tree/3.x)、[jackson-databind 3.x release-notes](https://github.com/FasterXML/jackson-databind/blob/3.x/release-notes/VERSION)
2. **铁律 2 的"查询对象不含判别枚举"与 DDD"查询用领域语言"的张力**：`VerificationChannel` 是领域概念；剔除后靠 `Class<T>` 等价表达（类型安全换可读性），需团队显式确认该取舍；纯数据类统计需求应放基础设施查询模型。
3. **当前代码与结论的差距（过渡态）**：`VerificationChannel` 持 `Class` 引用 + `of(Class)` 反查（违反铁律 2/4）；`VerificationGateway` 同时有 `VerificationQuery(channel)` 与 `Class<T>` 变体（双轨语义重复）——按结论收敛时应一并清理。
4. **判别短名是持久化/传输合同**：改名破坏 DB 数据（ADR-0005 已承认）；新增子类型同时变更 DB 数据与 JSON 判别串。AIP-126
   建议频繁变化的值集用 string；本项目用枚举锁定 1-4
   字符短名，需接受"变更=迁移"的成本。[来源: https://google.aip.dev/126](https://google.aip.dev/126)、[docs/adr/0005](../adr/0005-enum-short-name.md)
5. **铁律 3 与 @JsonTypeName 的双副本问题**：`getChannel()` 与 `@JsonTypeName("S")` 各存一份映射，仅靠测试（铁律 5）与 TypeIdResolver 收敛；不存在编译期手段。
6. **`Class<T>` 参数的运行时校验缺口**：泛型擦除后 gateway 实现需校验 `Class` 是 permits 子类，否则错误静默到运行时；这是 JPA `find` 同款代价。
7. **Fowler bliki TypeObject.html 404**（2026-08-04 实测）：引用 Type Object 请用 Johnson & Woolf PLoP 1997 论文（cs.ox.ac.uk 镜像可访问）+ Fowler《Analysis Patterns》(1997)。
8. **Effective Java 第 3 版出版于 2017（覆盖到 Java 9）**，无 sealed 内容；其 Item 23/34/38 是精神先导，sealed 相关判据以 JEP 409/441 为准。[来源: InformIT 出版社页（"coverage is through Java 9"）](https://www.informit.com/store/effective-java-9780134685991)
9. **铁律 4 无直接 primary 来源**：属"依赖单向无环 + 单一事实源"的派生规范，方向主流但非强制惯例。
10. **Jackson 2/3 差异提示**：判别相关注解包未迁移（仍 `com.fasterxml.jackson.annotation`）；`TypeIdResolver` 3.x 方法签名带 `DatabindContext`；本项目用 Jackson 3 时查阅 3.x 分支文档，2.x 结论（如 TypeNameIdResolver 无重复校验）如需复验应在 3.x 源码上重复。

---

## 来源汇总

| 主题 | 来源 | URL | 类型 |
|---|---|---|---|
| Sealed 类 / enum vs sealed 定位 | JEP 409 (JDK 17) | https://openjdk.org/jeps/409 | primary |
| 模式匹配 switch / instanceof+cast 替代 | JEP 441 (JDK 21) | https://openjdk.org/jeps/441 | primary |
| 注解元素值必须常量表达式 | JLS 21 §9.7.1 / §15.29 | https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.1 | primary |
| tagged class 反模式 / 枚举与接口 | Effective Java 3rd, Item 23/34/38 | https://www.informit.com/store/effective-java-9780134685991（[secondary] https://ahdak.github.io/blog/effective-java-part-3） | 书（primary）/ 网页（secondary） |
| Type Code 重构 | Fowler Refactoring 目录 | https://refactoring.com/catalog/replaceTypeCodeWithSubclasses.html / replaceConditionalWithPolymorphism.html / replacePrimitiveWithObject.html | primary |
| Type Object 模式适用条件 | Johnson & Woolf, PLoP 1997 | http://www.cs.ox.ac.uk/jeremy.gibbons/dpa/typeobject.pdf | primary |
| Repository 模式 / 单向依赖 | Fowler P of EAA Catalog | https://martinfowler.com/eaaCatalog/repository.html | primary |
| 分层依赖 / 仓储语言 / 领域事件 | Evans, DDD Reference 2015 (CC-BY) | https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf | primary |
| 内部不外泄 / adapter 职责 | Cockburn, Hexagonal Architecture (2005) | https://alistair.cockburn.us/hexagonal-architecture/ | primary |
| 枚举稳定值 / 变化频繁用 string | Google AIP-126 | https://google.aip.dev/126 | primary |
| 类型安全仓储 | Spring Data Commons 核心概念 | https://docs.spring.io/spring-data/data-commons/reference/repositories/core-concepts.html | primary |
| 类型化查询 / Class 令牌 | Jakarta Persistence TypedQuery / EntityManager | https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/typedquery 、.../entitymanager | primary |
| 运行时类令牌惯用法 | Oracle Java 教程 (Generics) | https://docs.oracle.com/javase/tutorial/extra/generics/literals.html | primary |
| @JsonTypeInfo / @JsonTypeName | FasterXML jackson-annotations（2.x/3.x javadoc） | https://github.com/FasterXML/jackson-annotations/blob/3.x/src/main/java/com/fasterxml/jackson/annotation/JsonTypeName.java | primary |
| TypeIdResolver / @JsonTypeIdResolver 契约 | FasterXML jackson-databind（2.x/3.x 源码） | https://github.com/FasterXML/jackson-databind/blob/2.x/src/main/java/com/fasterxml/jackson/databind/jsontype/TypeIdResolver.java | primary |
| sealed 自动发现（Jackson 3） | jackson-databind 3.x release-notes（#5025, 3.0.0-rc2） | https://github.com/FasterXML/jackson-databind/blob/3.x/release-notes/VERSION | primary |
| 结构测试主流化 | ArchUnit 官网 | https://www.archunit.org/ | primary |
| 判别值唯一性无内置校验 | jackson-databind 2.x TypeNameIdResolver 源码 | https://github.com/FasterXML/jackson-databind/blob/2.x/src/main/java/com/fasterxml/jackson/databind/jsontype/impl/TypeNameIdResolver.java | primary（源码阅读） |

---

## 附录：项目现状对照（代码快照，2026-08-04）

| 现状 | 对应结论 | 状态 |
|---|---|---|
| `AuthAccount`/`Verification` sealed + `@JsonTypeInfo(use=Id.NAME)`，子类 `@JsonTypeName` 短名 | 结论 1/3 | 符合 |
| `AuthAccount.getAccountType()` 抽象方法由子类实现 | 结论 5（铁律 3） | 符合 |
| `VerificationChannel` 枚举持 `Class<? extends Verification<?>> type` 字段 + `of(Class<T>)` 反查 | 结论 4/6（铁律 2/4） | **违反，待收敛** |
| `AuthAccountType` 无 class 引用 | 结论 6（铁律 4） | 符合 |
| `VerificationGateway.VerificationQuery` 含 `VerificationChannel channel` + 另存 `<T extends Verification<?>> Optional<T> findLatestByUserId(..., Class<T>)` 变体 | 结论 4（铁律 2） | 双轨并存，需收敛 |
| `VerificationSubtypesMatchesPermitsTest` / `AuthAccountSubtypesMatchesPermitsTest`（permits 完备 + 唯一 + 枚举 name 一致） | 结论 7（铁律 5） | 符合 |
| `EnumType extends Type` 枚举 DP 体系（`@JsonCreator of(String)`） | 结论 1/2 | 符合 |
