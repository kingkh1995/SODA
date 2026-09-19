---
type: Convention
title: Spring Framework 使用规范
description: 写/评审 Spring Framework 用法（DI、组件与自动配置、事务、事件、Web MVC 扩展点、校验）时读——P3C 三级 + ❌/✅ + 双清单；编排 / 异常 / Logging 在 framework-crosscutting；版本基线与禁用清单见 §1。
tags: [ spring, convention ]
status: stable
---

# Spring Framework 使用规范

> **本篇**＝Spring Framework 用法的定案与落地：§1 版本基线 → §2–§7 规则章 → §8 偏离表。
> **官方事实全量、过时对照与版本锚点溯源**归
> [research/foundation-deps-official-practices.md § Spring Framework](../research/foundation-deps-official-practices.md#spring-framework)
> ——本篇不设来源章，`官方来源` 列只写一级链接。
> **关联规范**：[framework-crosscutting](framework-crosscutting.md) §5（编排 / 异常）/ §6（Logging）/ §8（Code
> Style）——编排与异常契约单源在该篇，本篇只指不述；
> [jspecify.md](jspecify.md)（空性标注；可选依赖的表达载体）；[lombok.md](lombok.md) §4/§6（构造器形态与 `@Slf4j`
> 准入）；[modulith.md](modulith.md) §2/§3（模块声明与依赖白名单）；
> [framework-type-contracts.md](framework-type-contracts.md)（Gateway / Versioned / VersionAwareCommand
> 类型契约）；[adapter.md](adapter.md) §1（`@Valid` 的协议边界职责）。
> **格式**：规则表列固定 `规则 | 级别 | 官方来源 | SODA 落地 | 关联 ADR`；级别三词 `【强制】/【推荐】/【参考】`，`↑`
> 表示由官方推荐升格为项目【强制】；
> 空值填 `—`；`官方来源` 只写一级 markdown 链接，版本锚点统一在 §1、不逐行重复；`SODA 落地` 列以「不在册」开头的行表示该规则经裁定不收进本仓约定（官方事实见
> research 篇）。

## 适用范围与管辖面

管 `src/main` 的 Spring Framework 用法：DI 与注入、组件与配置类、Boot 自动配置装载、事务、事件、Web MVC 扩展点、校验。

何时 **不**读：

- 写 `src/test` / `src/testFixtures` —— 测试侧用法归 [test-conventions](../test-conventions.md)，本篇不描述
- 编排（DomainService / ApplicationService）、异常类型族与 HTTP 映射、入口日志、Import
  规范 —— [framework-crosscutting](framework-crosscutting.md) §5 / §6 / §8
- 空性标注怎么写、可选依赖为什么用 JSpecify `@Nullable` —— [jspecify.md](jspecify.md)
- 构造器由 `@RequiredArgsConstructor` 生成还是手写 —— [lombok.md](lombok.md) §4/§6
- `@ApplicationModule` 与 `allowedDependencies` 白名单 —— [modulith.md](modulith.md) §2/§3
- 数据库开发约定（H2 / Flyway / 单 V1）—— [framework-crosscutting](framework-crosscutting.md)
  §7 → [ADR-0022](../adr/0022-dev-database-management.md)
- Gradle 依赖声明规则 —— 根 [STYLEGUIDE](../../STYLEGUIDE.md) §5
- 官方规则原文、过时对照全量（含本仓不使用的集合注入、缓存、Profile / Conditional 全谱、AOP
  代理细节）—— [research 篇](../research/foundation-deps-official-practices.md#spring-framework)

## 写清单

新增或修改 `src/main` 代码时按动作序逐条执行（括号内为落点）：

| #  | 动作（落点）                                                                                                              |
|----|---------------------------------------------------------------------------------------------------------------------------|
| 1  | §2：新 bean 一律构造器注入 + `final` 字段；单构造器免 `@Autowired`（参照 `UserServiceImpl.java:56`）                      |
| 2  | §2：可选依赖用 JSpecify `@Nullable`（指 [jspecify.md](jspecify.md)），不用 `@Autowired(required=false)` / `Optional<T>`   |
| 3  | §3：服务层 `@Service`、其余 `@Component`；新自动配置类登记进 `META-INF/spring/…AutoConfiguration.imports`                 |
| 4  | §3：自动配置类级 `@ConditionalOnClass` + 方法级 `@ConditionalOnMissingBean`；类不得落在任何 `scanBasePackages` 覆盖范围内 |
| 5  | §4：`@Transactional` 标具体类 / 方法；事务参与方不得声明 checked 异常（不得 `throws`）                                    |
| 6  | §4：不标 `readOnly` / `timeout` / `isolation`、不加 `rollbackFor` 与全局 `rollbackOn`（理由见 §8 E1/E5）                  |
| 7  | §5：`@TransactionalEventListener` 不设 `fallbackExecution`；`@Async` 必须伴随 `@EnableAsync`（本仓不启用）                |
| 8  | §6：错误响应走 `ProblemDetailAdvice` 单一出口；新增控制器保留类级 `@Validated`（§8 E6）                                   |
| 9  | §7：`@RequestBody` 用 `@Valid`；参数级直接约束靠类级 `@Validated`（二者配合，非二选一）                                   |
| 10 | §1：BOM / 直接依赖升级时逐项复核 1.2 禁用清单与版本表，改动就地更新                                                       |
| 11 | 完成判据：按文末检视清单逐条自检通过，且本篇零跨文档重复、来源链接为一级 markdown 链接                                    |

## 1. 版本基线

### 1.1 版本

| 项               | 值                                                          | 依据                                                                                                                                                                                                                                                                                   |
|------------------|-------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Spring Framework | **7.0.9**                                                   | [Boot 4.1.1 BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom) 的 `spring-framework.version`；与 [Framework 7 reference / javadoc](https://docs.spring.io/spring-framework/reference/) 稳定版一致，无漂移 |
| Spring Boot      | **4.1.1**                                                   | 根 `build.gradle:2`（plugin version）；[Boot 4.1 reference](https://docs.spring.io/spring-boot/reference/)                                                                                                                                                                             |
| JDK              | **25**                                                      | 根 `build.gradle` toolchain（`JavaLanguageVersion.of(25)`）                                                                                                                                                                                                                            |
| JSpecify         | **1.0.1**                                                   | [Boot 4.1.1 BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom) 的 `jspecify.version`（写法与落点见 [jspecify.md](jspecify.md) §1）                                                                        |
| 基线要求         | JDK 17+ / Jakarta EE 11 / Servlet 6.1 / Bean Validation 3.1 | [Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)；[Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)                                            |

### 1.2 禁用清单

| 本仓禁用项                                                                                                                                                                   | 替代                                           | 依据                                                                                                                                                                           | 影响面                                                                                                                           |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `javax.annotation.*` / `javax.inject.*` 注解                                                                                                                                 | `jakarta.annotation.*` / `jakarta.inject.*`    | [Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)（Framework 7 已移除 javax 支持）                    | 潜在                                                                                                                             |
| 字段注入 / setter 注入                                                                                                                                                       | 构造器注入 + `final` 字段                      | [factory-collaborators](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)（官方主张构造器注入）                            | 潜在（main 现状 0 处；§2 已升格为【强制】，理由同行）                                                                            |
| `spring-boot-starter-web`                                                                                                                                                    | `spring-boot-starter-webmvc`                   | [Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)（旧名 deprecated；`webmvc` = base starter + 同四项，严格增量） | **命中**（原 2 处，已清零：`soda-component-start-starter/build.gradle:3`、`soda-component-query-server-starter/build.gradle:5`） |
| 已移除的路径匹配选项（`suffixPatternMatch` / `trailingSlashMatch` / `favorPathExtension` / `matchOptionalTrailingSeparator`）与 `PathMatcher` / `HandlerMappingIntrospector` | `PathPatternParser` 体系                       | [Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)（已移除，引用即编译失败）                           | 潜在                                                                                                                             |
| `<mvc:*>` XML 命名空间                                                                                                                                                       | Java 配置（`WebMvcConfigurer`）                | [Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)                                                     | 潜在（本仓为纯 Java 配置）                                                                                                       |
| `org.springframework.lang.*`（JSR-305 空安全注解）                                                                                                                           | `org.jspecify.annotations.*`                   | [Framework · Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)（7 起 deprecated，让位于 JSpecify）                                         | 潜在（**单源在 [jspecify.md](jspecify.md) §5，本行只回指**）                                                                     |
| Boot 单体 jar 集中包（`org.springframework.boot.autoconfigure.*` 等）                                                                                                        | 模块化根包 `org.springframework.boot.<module>` | [Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)                                                                | 潜在（4 个 `@AutoConfiguration` 类自写 import 时按新根包选类）                                                                   |
| `RestTemplate`                                                                                                                                                               | `RestClient`                                   | [Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)（7.1 起正式 `@Deprecated`）                         | 潜在（main 命中 0）                                                                                                              |

**未收录（测试侧，归 [test-conventions](../test-conventions.md)）**：`@MockBean` / `@SpyBean`、JUnit 4 TestContext、
`@SpringBootTest` 隐式 MockMvc / TestRestTemplate、`MockitoTestExecutionListener`——均为 Boot 4 移除项，但命中面在
`src/test`，不在本篇管辖面。

维护：BOM / 直接依赖升级时逐项复核本表与 1.2 清单（依据＝官方 release notes / 迁移指南），改动就地更新。

## 2. DI 与注入

注入是模块接线的唯一入口：依赖在 **构造器**上声明，字段 `final`，对象交出时即完整初始化。

| 规则                                                                                                                  | 级别       | 官方来源                                                                                                                      | SODA 落地                                                                                                                                                                                                                                              | 关联 ADR |
|-----------------------------------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 单构造器免 `@Autowired`；多构造器且无默认构造器时至少一个必须标注（`required=true` 唯一）                             | 【强制】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                    | 守现状：main `@Autowired` 命中 0；所有托管类依赖均为唯一构造器注入（显式声明或 `@RequiredArgsConstructor` 生成），多构造器类 0 个（`UserController.java:45`、`UserServiceImpl.java:56`、`UserGatewayImpl.java:40`、`SpringDomainEventBus.java:14,17`） | —        |
| 注入注解不得在 BPP / BFPP 自身使用                                                                                    | 【强制】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                    | —（不在册：main 未实现任何 `BeanPostProcessor` / `BeanFactoryPostProcessor`，无落点）                                                                                                                                                                  | —        |
| 构造器注入主张（不可变 / 非 null / 完整初始化；大量构造参数是坏味道）                                                 | 【推荐】   | [factory-collaborators](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html) | 守现状：托管类依赖字段全 `private final`；最大构造器 6 参（`UserAuthServiceImpl.java:62-67`）                                                                                                                                                          | —        |
| 避免字段注入                                                                                                          | 【强制】 ↑ | [factory-collaborators](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html) | 守现状：main 零字段 / setter 注入。**升格理由**：官方只「主张构造器注入」、未禁止字段注入，本仓把它定为硬约定——字段注入破坏不可变与构造期完整性，且绕过 §2 的单构造器判据                                                                              | —        |
| 可选依赖的表达：官方给 `required=false` / `Optional<T>` / 参数级 `@Nullable` 三选一；多元素注入点无候选时解析为空实例 | 【参考】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                    | —（不在册：main 无 `Optional` / `required=false` / 参数级 `@Nullable` 注入点，依赖全为必需单例）；将来出现可选依赖时统一取**参数级 JSpecify `@Nullable`**，表达载体归 [jspecify.md](jspecify.md)、本章不重述                                           | —        |
| 自引用（self-injection）只作最后手段；`@Configuration` 类内互注 `@Bean` 结果同属自引用                                | 【推荐】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                    | 守现状：无自引用；4 个 `@AutoConfiguration` 的 2 个 `@Bean` 不互调                                                                                                                                                                                     | —        |
| 集合 / `Map` 注入的排序由 `Ordered` / `@Order` / `@Priority` 决定，否则按 bean 定义注册顺序                           | 【参考】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                    | —（不在册：main 无集合 / `Map` 注入点，`@Order` 命中 0——扩展点天然唯一）                                                                                                                                                                               | —        |
| `@Bean` 工厂方法应声明尽量具体的返回类型                                                                              | 【推荐】   | [bean-annotation](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html)                     | 守现状：两个 `@Bean` 均与注入点类型一致；端口型返回的本仓取舍见 §8 E3                                                                                                                                                                                  | —        |

### 2.1 构造器是全仓唯一注入点

字段注入在本仓零出现，不是因为没人图方便，而是因为它同时破坏两件事：字段无法 `final`（对象可在初始化未完成时被交出），以及依赖不进入构造器签名（§2
的单构造器判据失去意义）。构造器注入把「这个类需要什么」变成编译期可读的签名，注入点唯一。

构造器的 **写法**（`@RequiredArgsConstructor` 生成还是手写）不在本章——归 [lombok.md](lombok.md) §4/§6。本章只管注入机制与刚性。

```java
// ❌ 字段注入：依赖不可变性与构造期完整性同时丢失，容器反射写入绕过公开契约
@Component
public class UserGatewayImpl implements UserGateway {
    @Autowired
    private UserRepository userRepository;
}

// ✅ 构造器注入 + final 字段；单构造器无需 @Autowired
@Component
@RequiredArgsConstructor
public class UserGatewayImpl implements UserGateway {
    private final UserRepository userRepository;
}
```

### 2.2 前提是把 bean 组件扫描进来

构造器注入只在对象 **由容器创建**时才成立：全仓 bean 靠 `@ComponentScan` 发现（两个部署单元的
`@SpringBootApplication(scanBasePackages = "com.soda.user")` 即扫描入口，见 §3.6）。`new` 出来的对象不享受注入、也不接受容器对
`final` 字段的填充——这正是 §3.4「自动配置类不得落在扫描范围内」的另一面。

## 3. 组件·配置与自动配置

| 规则                                                                                                              | 级别       | 官方来源                                                                                                                                                                                                                                                                            | SODA 落地                                                                                                                                                                                                                                                                                                                  | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Component` 是通用 stereotype；服务层选 `@Service` 优于 `@Component`；`@Repository` 承载异常转换                 | 【强制】 ↑ | [classpath-scanning](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)                                                                                                                                                                          | 守现状：服务层 3 处全 `@Service`（`UserServiceImpl.java:42`、`UserAuthServiceImpl.java:51`、`CredentialChangeDomainService.java:30`）；非服务型 11 处全 `@Component`；`@Repository` 0 处（Spring Data 接口自动识别）。**升格理由**：官方为「推荐」，本仓定为分层硬约定——两词混用会让「这层是不是应用服务」在扫描层面不可读 | —        |
| `@Configuration` 声明 `@Bean`；类内 `@Bean` 互调只在 `@Configuration` 中成立，`@Component` 中不行                 | 【强制】   | [configuration-annotation](https://docs.spring.io/spring-framework/reference/core/beans/java/configuration-annotation.html)                                                                                                                                                         | 守现状：`@Configuration` 命中 0；两个 `@Bean` 均落 `@AutoConfiguration` 类内且无 inter-bean 互调                                                                                                                                                                                                                           | —        |
| `@Configuration` 默认 `proxyBeanMethods=true`，受此限制配置类不能 `final`；规避 CGLIB 用 `proxyBeanMethods=false` | 【强制】   | [configuration-annotation](https://docs.spring.io/spring-framework/reference/core/beans/java/configuration-annotation.html) ／ [`@Configuration` javadoc](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/context/annotation/Configuration.html) | 守现状：4 个自动配置类均非 `final`；`proxyBeanMethods` 显式取值 0 处（`@AutoConfiguration` 固定 false）                                                                                                                                                                                                                    | —        |
| `@Bean` 默认 bean 名为方法名，可用 `name` / `value` 指定                                                          | 【强制】   | [bean-annotation](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html)                                                                                                                                                                           | 守现状：两个 `@Bean` 未指定名，按方法名注册；无按名注入点                                                                                                                                                                                                                                                                  | —        |
| `@AutoConfiguration` 恒等于 `@Configuration(proxyBeanMethods=false)`，且是常规 `@Configuration` 的子集语义        | 【强制】   | [`@AutoConfiguration` javadoc](https://docs.spring.io/spring-boot/4.1/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html)                                                                                                                                       | 守现状：4 处 `@AutoConfiguration` 无一处叠加 `@Configuration` 或改写 `proxyBeanMethods`（§3.2）                                                                                                                                                                                                                            | —        |
| 可用元注解组合自定义 stereotype                                                                                   | 【推荐】   | [classpath-scanning](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)                                                                                                                                                                          | —（不在册：项目三个自有注解 `IfMatch` / `MobileNumber` / `EnumName` 分别是参数解析与校验用途，非 stereotype 组合）                                                                                                                                                                                                         | —        |

Boot 提供的 `JsonMapperBuilderCustomizer` 定制入口用法见 [jackson.md](jackson.md) §2。

### 3.1 stereotype 选择：`@Service` 与非服务 `@Component`

判据只有一条： **这个类是不是服务**。服务层（`application/service`、`domain/service`）用 `@Service`，其余托管类——gateway
实现、convertor、factory、事件监听器、sender 实现、job——用 `@Component`。`@Repository` 不手写：本仓持久化入口是 Spring Data
接口，异常转换由框架自动装配承担。

`CredentialChangeDomainService.java:30` 用 `@Service` 不是破例，它是领域服务，与两个应用服务同属服务层。

### 3.2 `@AutoConfiguration` 不是 `@Configuration` 的别名

`@AutoConfiguration` 已含 `@Configuration(proxyBeanMethods=false)`。再叠一个 `@Configuration` 是重复声明；叠上
`proxyBeanMethods=true` 更糟——把 CGLIB 子类化引回来，与自动配置「不依赖 inter-bean 互调」的前提相悖。`@Bean` 方法之间要互相引用时用
**方法参数注入**，不要直接调用同类里的另一个 `@Bean` 方法。

```java
// ❌ 叠加 @Configuration：语义重复，proxyBeanMethods 的实际取值变得不可读
@AutoConfiguration
@Configuration(proxyBeanMethods = true)
public class SpringDomainEventBusAutoConfiguration { …
}

// ✅ 只标 @AutoConfiguration；默认 bean 一律带退让条件
@AutoConfiguration
public class SpringDomainEventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventBus.class)
    public DomainEventBus springDomainEventBus(ApplicationEventPublisher publisher) { …}
}
```

### 3.3 自动配置：条件与顺序

| 规则                                                                                                                                              | 级别       | 官方来源                                                                                                                               | SODA 落地                                                                                                                                                                                                                                                                             | 关联 ADR            |
|---------------------------------------------------------------------------------------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| `@AutoConfiguration` 只能经 `AutoConfiguration.imports` 装载；须独立包空间，不得成为组件扫描目标，也不得自行启用组件扫描（用 `@Import` 代替）     | 【强制】   | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | 装载合规：2 份 imports 与 4 类一一对应、`@ComponentScan` 0 处；**独立包空间未满足** ⇒ 偏离 **E2**，代以项目加固（§3.4）                                                                                                                                                               | ADR-0008 / ADR-0019 |
| 自动配置类几乎总应带 `@Conditional`：常用 `@ConditionalOnClass` + `@ConditionalOnMissingBean`                                                     | 【推荐】   | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | 4 类齐备：`JpaAuditingAutoConfiguration.java:27` 类级 `@ConditionalOnClass` + `:32` 方法级 `@ConditionalOnMissingBean`；`SpringDomainEventBusAutoConfiguration.java:22`；`IfMatchAutoConfiguration.java:18` / `ProblemDetailAdvice.java:36` 的 `@ConditionalOnWebApplication`（§3.5） | —                   |
| `@ConditionalOnClass` 用 `value` 引用的类即使不在运行时 classpath 也可 ASM 解析，但该机制**不适用于 `@Bean` 方法**；作为元注解组合时必须用 `name` | 【强制】   | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | 唯一落点＝`JpaAuditingAutoConfiguration.java:27` 的**类级** `@ConditionalOnClass(EnableJpaAuditing.class)`，未加在任何 `@Bean` 方法上（§3.5）                                                                                                                                         | —                   |
| `@ConditionalOnBean` / `@ConditionalOnMissingBean` 只在自动配置类上使用（自动配置在用户 bean 定义之后加载）                                       | 【强制】 ↑ | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | 守现状：2 处全落自动配置类方法级；`@ConditionalOnBean` 0 处。**升格理由**：官方为「推荐」，但条件注解落在普通 bean 上因加载顺序而不确定成立——静默失效，须靠硬约定拦住（§3.5）                                                                                                         | —                   |
| 顺序用 `before` / `beforeName` / `after` / `afterName` 或 `@AutoConfigureOrder` 声明                                                              | 【推荐】   | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | —（不在册：4 类分两组互不依赖，无顺序约束）                                                                                                                                                                                                                                           | —                   |
| `@Bean` 返回类型要给出尽量多的类型信息（实现类优先于接口），否则 `@ConditionalOnMissingBean` 等按类型判断会失准                                   | 【推荐】   | [developing-auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html)              | **有据偏离** ⇒ §8 E3（`SpringDomainEventBusAutoConfiguration.java:23` 返回接口 `DomainEventBus`；条件显式指定类型故不失准）                                                                                                                                                           | ADR-0008            |
| 自动配置非侵入：用户自定义 bean 让其退让；排查生效项用 `--debug`（conditions report）；禁用用 `exclude` / `spring.autoconfigure.exclude`          | 【推荐】   | [using/auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)                                 | 守现状：`SpringDomainEventBusAutoConfiguration.java:22` 的退让行为有测试覆盖；4 类均可 exclude                                                                                                                                                                                        | —                   |
| 只应添加一个 `@SpringBootApplication` / `@EnableAutoConfiguration`，且建议只加在主 `@Configuration` 类上                                          | 【强制】 ↑ | [using/auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)                                 | 守现状：2 处**分属两个独立部署单元**（`SodaUserApplication.java:14`、`SodaUserQueryServerApplication.java:13`）各 1 个；独立使用的 `@EnableAutoConfiguration` 0 处。**升格理由**：同部署单元两个启动类会让自动配置出现两份装配来源，本仓以「一部署单元一启动类」定死（§3.6）          | ADR-0001            |
| Boot 推荐构造器注入 + `@ComponentScan` 发现 bean；bean 多于一个构造器时用 `@Autowired` 指定                                                       | 【推荐】   | [spring-beans-and-dependency-injection](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html) | 守现状：两启动类 `scanBasePackages = "com.soda.user"`；被发现的 bean 全为构造器注入                                                                                                                                                                                                   | —                   |

### 3.4 装载入口与包边界

自动配置类 **只能**经
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 装载——不靠组件扫描，也不自行
`@ComponentScan`（要引入别的类用 `@Import`）。官方另要求它拥有独立包空间；本仓未满足（4 类与普通类同包，位于
`com.soda.component.*`），以 **项目加固**替代：

- 【强制】自动配置类 **不得落在任何 `scanBasePackages` 覆盖范围内**——两个部署单元只扫 `com.soda.user`，四个自动配置类在
  `com.soda.component.*`，当前未被扫到；新增类的落点必须维持这一条，否则同一个类会被「扫描 + imports」双路径注册，条件求值时机错位
- 未迁独立包空间的原因与代价见 §8 E2

当前 2 份 imports 与 4 类一一对应：`soda-component-adapter-starter-web`（`IfMatchAutoConfiguration`、
`ProblemDetailAdvice`）、`soda-component-infrastructure-starter`（`SpringDomainEventBusAutoConfiguration`、
`JpaAuditingAutoConfiguration`）。新增自动配置类必须同步登记，否则类在包里但不生效。

### 3.5 条件注解的两级分工与退让

类级条件决定「这个自动配置要不要参与」，方法级条件决定「这个默认 bean 要不要退让」——判据不同，落点也不同：

| 落点   | 注解                           | 语义                                               | 本仓实例                                                                                                                          |
|--------|--------------------------------|----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| 类级   | `@ConditionalOnClass`          | 能力在类路径才装配（ASM 读元数据，类缺失也不报错） | `JpaAuditingAutoConfiguration.java:27`（`EnableJpaAuditing`）                                                                     |
| 类级   | `@ConditionalOnWebApplication` | 只在 servlet web 应用里装配                        | `IfMatchAutoConfiguration.java:18`、`ProblemDetailAdvice.java:36`                                                                 |
| 方法级 | `@ConditionalOnMissingBean`    | 消费方未提供该 bean 时才注册默认实现               | `JpaAuditingAutoConfiguration.java:32`（`DateTimeProvider`）、`SpringDomainEventBusAutoConfiguration.java:22`（`DomainEventBus`） |

两条边界：类级条件 **不得**下沉到 `@Bean` 方法（`@ConditionalOnClass` 的 ASM 机制对方法不适用）； **条件注解不写在业务 bean
上**——普通 bean 的加载早于自动配置，条件求值结果不确定，会出现「有时生效、有时静默不生效」的不可重现故障。

```java
// ❌ 类级 @ConditionalOnClass 下沉到 @Bean 方法：该机制只对配置类生效，方法上不成立
@Bean
@ConditionalOnClass(EnableJpaAuditing.class)
DateTimeProvider auditableDateTimeProvider() { …}

// ✅ 类级管能力、方法级管退让
@AutoConfiguration
@ConditionalOnClass(EnableJpaAuditing.class)
@EnableJpaAuditing
public class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DateTimeProvider.class)
    DateTimeProvider auditableDateTimeProvider() { …}
}
```

### 3.6 部署单元与启动类

一个部署单元一个 `@SpringBootApplication`，且只标在主配置类上：读写两侧各一个（`SodaUserApplication` /
`SodaUserQueryServerApplication`），各自 `scanBasePackages = "com.soda.user"`。不再额外声明裸 `@EnableAutoConfiguration`——
`@SpringBootApplication` 已含它。

【强制】启动入口 **不得**承载 `@EntityScan` / `@EnableJpaRepositories`：实体与仓储的扫描根由持久化模块自持（在其被扫到的
`@Configuration` 上声明），入口只做组件扫描与容器启动。这样入口的编译类路径不含任何 JPA 构件，两个部署单元也不会各存一份可能分歧的扫描包名。见
[ADR-0043](../adr/0043-persistence-scan-root-ownership.md)。

## 4. 事务

| 规则                                                                                                                                                        | 级别     | 官方来源                                                                                                                          | SODA 落地                                                                                                                                                                                                                                                  | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `@Transactional` 默认：propagation=`REQUIRED`、isolation=`ISOLATION_DEFAULT`、read-write、超时随底层系统；`RuntimeException` / `Error` 回滚，checked 不回滚 | 【强制】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | 守现状：main 5 处标注无一设 `isolation` / `timeout` / `readOnly`；无 checked 抛出面（main 内 9 处 `catch` 全为本方法内解析兜底，无一跨事务边界）                                                                                                           | ADR-0015 |
| `@Transactional` 标在具体类的方法上，而非接口方法（接口注解在 AspectJ 模式下不被识别，静默失效）                                                            | 【推荐】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | 守现状：标注全落具体类（`UserServiceImpl.java:41` 与 `UserAuthServiceImpl.java:50` 为类级、`UserGatewayImpl.java:49`、`VerificationGatewayImpl.java:43`、`VerificationCreatedEventHandler.java:57` 为方法级）；`UserService` / `UserAuthService` 接口 0 处 | —        |
| 代理模式只拦截经代理的外部调用；类内自调用不开事务；不在 `@PostConstruct` 等初始化代码里依赖代理行为                                                        | 【强制】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | 守现状：跨 bean 调用均经代理（`UserAuthServiceImpl.java:118-120`）；类内私有辅助由同类事务方法调用且外层事务在位；`@PostConstruct` 0 处                                                                                                                    | —        |
| 可见性：接口代理的方法必须 `public`；类代理自 6.0 起 `protected` / 包可见也可生效                                                                           | 【强制】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | 守现状：全部事务方法为 `public`                                                                                                                                                                                                                            | —        |
| `PROPAGATION_REQUIRED` 加入外层物理事务并忽略局部 isolation / timeout / read-only；内层置 rollback-only 后外层提交抛 `UnexpectedRollbackException`          | 【强制】 | [tx-propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)       | 机制行，支撑 E5 与 E1：嵌套调用均走 `REQUIRED`；9 处 `catch` 全为本方法内解析兜底（`IfMatchResolver`、`ParseUtils`），无一跨事务边界                                                                                                                       | —        |
| 抛异常触发回滚是推荐方式；用 `rollbackFor` / `noRollbackFor` 精确指定回滚规则                                                                               | 【强制】 | [rolling-back](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)           | 守现状：`rollbackFor` / `noRollbackFor` 显式 0 处，依赖默认「unchecked 回滚」——业务异常族全部 `extends RuntimeException`（见 [framework-crosscutting](framework-crosscutting.md) §5「Exception」），默认规则已覆盖                                         | ADR-0015 |
| 自 6.2 起可全局改为「所有异常回滚」：`@EnableTransactionManagement(rollbackOn = ALL_EXCEPTIONS)`                                                            | 【推荐】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | **保持框架默认**（不加全局开关），改以【强制】「事务参与方不得声明 checked 异常」加固 ⇒ 偏离 **E1**；`UserServiceImpl.java` / `UserAuthServiceImpl.java` 无 `throws`，全仓 `rollbackOn` / `@EnableTransactionManagement` 0 处                              | ADR-0015 |
| `readOnly` / `timeout` / `isolation` 只对 `REQUIRED` 或 `REQUIRES_NEW` 生效                                                                                 | 【参考】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | main 零 `readOnly` 落点 ⇒ 偏离 **E5**；本行保留为「为何本仓不用 `readOnly`」的依据（§4.2）                                                                                                                                                                 | —        |
| `PROPAGATION_REQUIRES_NEW` 独立物理事务、可独立提交 / 回滚；连接池需按「并发线程数 + 1」以上配置                                                            | 【参考】 | [tx-propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)       | 守现状 + 结论句：唯一落点 `VerificationCreatedEventHandler.java:57` 位于 AFTER_COMMIT、无并发驱动（H2 内存库）⇒ 容量不配，见 §8 E7                                                                                                                         | —        |
| `PROPAGATION_NESTED` 基于 JDBC savepoint，仅资源型事务支持                                                                                                  | 【推荐】 | [tx-propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)       | —（不在册：`Propagation` 显式取值仅 `REQUIRES_NEW` 1 处，`NESTED` 0 处）                                                                                                                                                                                   | —        |
| 重复的事务属性用自定义组合注解（meta-annotation）收敛，而非散落复制                                                                                         | 【推荐】 | [transaction/annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) | —（不在册：5 处标注的属性组合已互不相同——4 处无属性 + 1 处 `REQUIRES_NEW`，无重复可收敛）                                                                                                                                                                  | —        |

### 4.1 事务边界写在具体类上，且是 **服务层**的职责

事务边界由服务层承担：应用服务整类标 `@Transactional`（`UserServiceImpl.java:41`、`UserAuthServiceImpl.java:50`
），网关实现只在自己的写方法上标（`UserGatewayImpl.java:49`、`VerificationGatewayImpl.java:43`）。类级标注给出默认语义，方法级标注在需要时覆盖它。

边界 **不**写在接口上：接口注解在 AspectJ 模式下不被识别，会静默失效；而 Spring
默认代理模式下它虽生效，却让「哪个实现有事务」在类文件里读不出来。事务分层与编排职责的完整约定见 [framework-crosscutting](framework-crosscutting.md)
§5（本篇只指不述）。

两条机制约束必须在写代码时记住： **类内 `this.` 自调用不经代理**（事务不开启，且不报错）， **初始化钩子里的代理依赖不可靠**（
`@PostConstruct` 期间代理可能尚未就绪）。本仓两个陷阱都是 0 落点，但新增代码极易踩中。

### 4.2 精确属性一律不写

`readOnly` / `timeout` / `isolation` 写得出来不等于生效：它们 **只对 `REQUIRED` 或 `REQUIRES_NEW` 生效**
，最外层若是读写事务，局部声明被直接忽略。本仓的选择是 **全部不写**，属性面收敛到零：

- `readOnly`：两条查询路径最外层均为读写事务 ⇒ 网关类级的 `readOnly` 从未生效（已删除）；读路径的只读语义由 Spring Data
  Repository 层承担。见 §8 E5
- `timeout` / `isolation`：无场景需要非默认值
- `rollbackFor` / `noRollbackFor`：见 §4.3（回滚语义）

### 4.3 回滚语义：默认规则 + 「不得声明 checked 异常」

默认规则是 checked 不回滚、unchecked 回滚。官方自 6.2 起建议全局切成 `ALL_EXCEPTIONS`，本仓 **不采纳**（§8
E1），理由是这个全局开关要解决的问题在本仓不存在—— **main 里没有 checked 抛出面**。

守住这个前提靠一条硬约定：

【强制】 **事务参与方（sender / gateway 端口）不得声明 checked 异常（不得 `throws`）**。事务参与方一旦 `throws` checked
异常，回滚语义就取决于调用方是否捕获——调用方捕获后继续，外层提交时才发现内层已 mark rollback-only，最终抛
`UnexpectedRollbackException`（§4 机制行）。把 checked 堵在源头比引入全局开关便宜。

```java
// ❌ 事务参与方声明 checked 异常：回滚与否取决于调用方是否捕获，故障在提交时才浮现
public interface SmsSender {
    void send(SmsRecipient recipient, SmsContent content) throws IOException;
}

// ✅ 参与方只抛 unchecked；调用方按契约处理失败（重发 / 补偿）
public interface SmsSender {
    void send(SmsRecipient recipient, SmsContent content);
}
```

## 5. 事件

| 规则                                                                                                                                                                                                              | 级别       | 官方来源                                                                                                                                                                                                                                                                               | SODA 落地                                                                                                                                                                                                                                            | 关联 ADR |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `ApplicationEventPublisher` 是 well-known resolvable dependency，可直接注入，无需额外配置                                                                                                                         | 【强制】   | [autowired](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)                                                                                                                                                                             | 守现状：`SpringDomainEventBus.java:17` 构造器注入；`SpringDomainEventBusAutoConfiguration.java:23` 方法参数注入                                                                                                                                      | —        |
| 自 4.2 起事件模型支持任意对象（不强制继承 `ApplicationEvent`）与 `@EventListener` 注解方法                                                                                                                        | 【推荐】   | [context-introduction](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html)                                                                                                                                                                         | 守现状：事件载荷为 record，不继承 `ApplicationEvent`；监听用 `@TransactionalEventListener` 特化                                                                                                                                                      | —        |
| 监听器 bean 不得声明为 lazy，否则容器不会注册其监听方法                                                                                                                                                           | 【强制】 ↑ | [context-introduction](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html)                                                                                                                                                                         | 守现状：`@Lazy` main 0 处；两个监听器 bean（`VerificationCreatedEventHandler.java:39`、`UserUpgradeConsumer.java:17`）均即时单例且在扫描范围内。**升格理由**：`@Lazy` 失效是**静默**的（方法照常存在、事件照常发出，只是没人接收），只能靠硬约定拦截 | —        |
| `@TransactionalEventListener` 默认绑定 `AFTER_COMMIT`；`phase` 可取 `BEFORE_COMMIT` / `AFTER_COMMIT` / `AFTER_ROLLBACK` / `AFTER_COMPLETION`；**无事务运行时监听器根本不会被调用**，除非 `fallbackExecution=true` | 【强制】   | [transaction/event](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)                                                                                                                                                                              | 守现状（已修正）：2 处均显式 `phase = AFTER_COMMIT`（`VerificationCreatedEventHandler.java:58`、`UserUpgradeConsumer.java:20`），均**不设** `fallbackExecution`（全仓命中 0）——恢复「无事务不执行」语义（§5.2）                                      | ADR-0011 |
| `@Async` 只有伴随 `@EnableAsync` 才产生代理、才真正生效；生效前提还包括方法 `public` 且经代理调用                                                                                                                 | 【强制】   | [`@EnableAsync`](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html) ／ [`@Async` javadoc](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/scheduling/annotation/Async.html) | 守现状（已修正）：全仓 `@EnableAsync` 0 处 ⇒ **本仓不启用异步执行**，`@Async` 命中 0（原唯一死代码已删）。将来新增必须同时引入 `@EnableAsync` 并说明线程池与连接池容量（§5.3）                                                                       | —        |
| 事务同步回调 `afterCompletion` 内抛出的异常被框架**记录但不传播**，调用方无法感知                                                                                                                                 | 【强制】   | [`TransactionSynchronization#afterCompletion` javadoc](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/transaction/support/TransactionSynchronization.html)                                                                                         | 守现状：`VerificationCreatedEventHandler` 的「弱保证语义」javadoc（`:31-33`、`:24`、`:50-51`）与该契约一致（§5.2）                                                                                                                                   | ADR-0011 |

### 5.1 载荷与 publisher

事件载荷是普通对象（本仓为 record），不必继承 `ApplicationEvent`；发布方注入 `ApplicationEventPublisher`（本仓经
`DomainEventBus` 端口二次封装，实现即构造器持有 publisher）。发布 **时机**
与「先落库后投递」的顺序属于编排约定，见 [framework-crosscutting](framework-crosscutting.md) §5 与 ADR-0011。

### 5.2 `AFTER_COMMIT` 的三条后果

把副作用挂在 `AFTER_COMMIT` 上换来「DB 事务不跨外部通道持有」，代价是三条必须记住的语义：

1. **无事务上下文时监听器不被调用**（不设 `fallbackExecution`）。这条是被主动选择的：`fallbackExecution=true` 会让监听器在
   **没有事务**时立即同步执行，把「提交后投递」变成「随时投递」，与「记录先于投递」的不变量冲突（本仓两处均不设该属性）
2. **回调内异常被吞**（官方 javadoc：logged but not propagated）。调用方感知不到投递失败，因此 `AFTER_COMMIT` 的副作用
   **必须在方法内自建补偿**——重试、状态对账，或让记录保持初始态等用户重发；不得依赖异常向上传播
3. **监听器如需写库要显式开新事务**。`VerificationCreatedEventHandler.java:57` 用 `REQUIRES_NEW`：提交后原事务已结束，普通
   `REQUIRED` 只会新开一个物理事务但语义不明；显式 `REQUIRES_NEW` 把「独立持久化 P 状态」写在签名上

```java
// ❌ 设 fallbackExecution：无事务上下文时同步执行，投递时机不再是「提交后」
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
public void onVerificationCreated(VerificationCreatedEvent event) { …}

// ✅ 默认语义：无事务不执行，发布方负责在事务内发布
@Transactional(propagation = Propagation.REQUIRES_NEW)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onVerificationCreated(VerificationCreatedEvent event) { …}
```

### 5.3 异步：本仓不启用

`@Async` 是 **需要配套才生效**的注解：没有 `@EnableAsync` 就没有代理，方法就按同步执行——注解在、行为不在，是注释与代码互相说谎。本仓不启用异步执行（
`@EnableAsync` 0 处），因此也不写 `@Async`。

将来引入时三件事同批：`@EnableAsync` 与线程池 bean、方法为 `public` 且经代理调用、连接池按容量评估（`@Async` 与
`REQUIRES_NEW` 同属「并发 + 1」范畴，见 §4 与 §8 E7）。

## 6. Web MVC 扩展点

| 规则                                                                                                                                                                       | 级别       | 官方来源                                                                                                                                                                                                                                                                                                                                | SODA 落地                                                                                                                                                                                                                                                                                              | 关联 ADR |
|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 全局横切用 `@ControllerAdvice` / `@RestControllerAdvice`；全局 `@ExceptionHandler` 在局部之后应用                                                                          | 【强制】   | [ann-advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)                                                                                                                                                                                                                               | 守现状：2 处 `@ControllerAdvice`（`ProblemDetailAdvice.java:37`、`HttpValidatorHeadersAdvice.java:26`）均以 `basePackages = "com.soda"` 限定；零局部 handler，无顺序冲突                                                                                                                               | ADR-0013 |
| RFC 9457 错误响应：`@ExceptionHandler` 可返回 `ProblemDetail`；`instance` 未设置时由当前 URL 填充；要覆盖内置异常继承 `ResponseEntityExceptionHandler`                     | 【推荐】   | [mvc-ann-rest-exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)                                                                                                                                                                                                                    | 守现状：4 个 `@ExceptionHandler` 全返回 `ProblemDetail`（`ProblemDetailAdvice.java:44,53,62,72`）；`instance` 显式置请求 URI；继承 `ResponseEntityExceptionHandler` 一次性覆盖内置异常（§6.1）                                                                                                         | ADR-0013 |
| 自定义参数解析实现 `HandlerMethodArgumentResolver`，经 `WebMvcConfigurer#addArgumentResolvers` 注册                                                                        | 【强制】 ↑ | [WebMvcConfigurer](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/servlet/config/annotation/WebMvcConfigurer.html) ／ [HandlerMethodArgumentResolver](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/method/support/HandlerMethodArgumentResolver.html) | 守现状：`IfMatchResolver` 实现该接口，注册于 `IfMatchAutoConfiguration.java:23`。**升格理由**：官方为「推荐」，本仓把它定为扩展点唯一通路——另起 `HandlerMethodArgumentResolver` 注册途径会让参数解析的装配点分裂                                                                                       | ADR-0039 |
| `ResponseBodyAdvice` 在 `@ResponseBody` / `ResponseEntity` 方法执行后、body 写出前改写响应；标在 `@ControllerAdvice` 上会被框架自动发现                                    | 【推荐】   | [ResponseBodyAdvice](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyAdvice.html)                                                                                                                                                                      | 守现状：`HttpValidatorHeadersAdvice.java:26` 实现，**只写头、不改 body**（`:39-42`）；本仓经 `@Import` 显式纳入而非依赖扫描（`IfMatchAutoConfiguration.java:19`，见 §6.3）                                                                                                                             | ADR-0039 |
| 自定义 MVC 配置实现 `WebMvcConfigurer`（保留默认配置，可多个共存）；仅当需要覆盖基类 bean 声明时才改走高级模式（去 `@EnableWebMvc`、继承 `DelegatingWebMvcConfiguration`） | 【强制】 ↑ | [mvc-config/customize](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/customize.html) ／ [advanced-java](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/advanced-java.html)                                                                                                           | 守现状：`IfMatchAutoConfiguration.java:20` 实现 `WebMvcConfigurer`，只重写 `addArgumentResolvers`；`@EnableWebMvc` 0 处、`DelegatingWebMvcConfiguration` 0 处。**升格理由**：高级模式会**关闭 Boot 的 MVC 自动配置**（默认配置、错误处理、消息转换器全部失效），代价远超收益，其触发条件必须写成硬约定 | ADR-0019 |
| 控制器内计算 `lastModified` / ETag 后与条件请求头比较，返回 `304 NOT_MODIFIED` 空体；无法预计算用 `ShallowEtagHeaderFilter`                                                | 【推荐】   | [mvc-caching](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-caching.html)                                                                                                                                                                                                                                            | **有据偏离** ⇒ §8 E4：强验证器生成 + `If-Match` 412 已落地，无 304 / `Last-Modified`                                                                                                                                                                                                                   | ADR-0039 |
| Boot `spring.mvc.problemdetails.enabled` 自动配置 order 0 的 handler；只想接管单个内置异常时应另建 advice 排在其前，而不是继承该类                                         | 【推荐】   | [mvc-ann-rest-exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)                                                                                                                                                                                                                    | —（不在册：项目走全量接管路径——继承 `ResponseEntityExceptionHandler` 即抑制 Boot 的 `ProblemDetailsExceptionHandler`，无「只接管一个」需求；`spring.mvc.problemdetails.enabled` 已删）                                                                                                                 | ADR-0013 |
| 扩展非标准错误字段写 `ProblemDetail.properties`（Jackson 渲染时上提到顶层）；i18n 走 `MessageSource` 的 `problemDetail.*` message codes                                    | 【推荐】   | [mvc-ann-rest-exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)                                                                                                                                                                                                                    | —（不在册：ADR-0013 裁最小集 `{title, status, detail, instance}`、不设扩展字段，`detail` 面向开发者英文、不本地化）                                                                                                                                                                                    | ADR-0013 |
| RFC 9457 响应拦截通过 `WebMvcConfigurer#addErrorResponseInterceptors` 注册                                                                                                 | 【推荐】   | [mvc-ann-rest-exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)                                                                                                                                                                                                                    | —（不在册：错误加工收敛在唯一 advice 的单一 `problem()` 出口，无「对已生成的 `ErrorResponse` 再加工」需求；`addErrorResponseInterceptors` 0 处）                                                                                                                                                       | ADR-0013 |

### 6.1 错误响应：一个 advice，一个出口

`ProblemDetailAdvice` 是项目异常到 RFC 9457 的 **唯一**翻译层，它继承 `ResponseEntityExceptionHandler`
，因此框架异常（请求体校验、类型转换、媒体类型、方法不支持等）由继承的 handler 一并翻译——这同时抑制了 Boot 自带的
`ProblemDetailsExceptionHandler`（其条件是 `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`）。项目自己的映射四条：
`ProblemDetailException` 取类型自带状态码、`IllegalArgumentException` 与 `ConstraintViolationException` 译 400、
`OptimisticLockingFailureException` 译 409、其余译 500。

状态码分配、`detail` 文案契约与 AIP-193 不遵守的结论见 [ADR-0013](../adr/0013-error-response-structure.md)
；异常类型族与抛出点见 [ADR-0015](../adr/0015-exception-class-convention.md)
与 [framework-crosscutting](framework-crosscutting.md) §5「Exception」。本章只约束 advice 的 **结构**：

- 【强制】错误响应只有这一个出口——新增映射加 `@ExceptionHandler` 到本 advice，不再另立 advice（多出口会让「哪个 handler
  先匹配」变成顺序问题）
- advice 以 `basePackages` 限定作用域（`com.soda`），不写无参全域形态

### 6.2 扩展点：参数解析器与响应体 advice

| 扩展点         | 接口                            | 注册方式                                | 本仓实例                                                  |
|----------------|---------------------------------|-----------------------------------------|-----------------------------------------------------------|
| 自定义方法参数 | `HandlerMethodArgumentResolver` | `WebMvcConfigurer#addArgumentResolvers` | `IfMatchResolver`（`@IfMatch Integer` 参数注入）          |
| 响应体写前改写 | `ResponseBodyAdvice`            | 标 `@ControllerAdvice` 或直接注册       | `HttpValidatorHeadersAdvice`（只补 `ETag` 头，不改 body） |
| MVC 配置增量   | `WebMvcConfigurer`              | 实现接口 + `@AutoConfiguration`         | `IfMatchAutoConfiguration`                                |

```java
// ❌ 高级模式：@EnableWebMvc 关闭 Boot 的 MVC 自动配置，默认消息转换器 / 错误处理一并失效
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer { …
}

// ✅ 增量实现 WebMvcConfigurer，只重写需要的方法，Boot 默认配置保持生效
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IfMatchAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new IfMatchResolver());
    }
}
```

### 6.3 条件请求：只做 AIP-154 里做得到的那半

`HttpValidatorHeadersAdvice` 的职责边界是 **只写头、不改 body、不求值**：出站由响应体实现 `HttpValidatorSource` 暴露强验证器原料（
`version()`），advice 统一补 `ETag`；入站由 `IfMatchResolver` 把 `If-Match` 归一化为 `@IfMatch Integer`（缺失 / 空 / `*` →
null 放行；弱标签 `W/` → 412；非数字 → 400），版本比对留应用层同事务守卫。两端业务模块都不感知 HTTP
头——契约单源见 [ADR-0039](../adr/0039-conditional-request-transport-contract.md) / [ADR-0037](../adr/0037-optimistic-lock-version-guard.md)
，类型契约见 [framework-type-contracts.md](framework-type-contracts.md)。

本仓无 GET 端点（11 个端点全为写操作，读侧走独立查询服务），因此 **没有** 304 / `If-None-Match` 路径——这一半的偏离与理由见
§8 E4。

```java
// ❌ advice 内求值：把条件请求的语义判断混进传输层，业务模块随后无法解释自己的响应
@Override
public @Nullable Object beforeBodyWrite(…) {
    if (body instanceof HttpValidatorSource source && source.version() == requestVersion) {
        response.setStatusCode(HttpStatus.NOT_MODIFIED);
    }
    return body;
}

// ✅ 只写头、不求值：比对留在应用层同一事务内
@Override
public @Nullable Object beforeBodyWrite(@Nullable Object body, …) {
    if (body instanceof HttpValidatorSource validators) {
        response.getHeaders().setETag(Integer.toString(validators.version()));
    }
    return body;
}
```

## 7. 校验

| 规则                                                                                                                                                     | 级别       | 官方来源                                                                                                                                                                                                                                                   | SODA 落地                                                                                                                                                                                                                                                                                      | 关联 ADR |
|----------------------------------------------------------------------------------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| 本仓选定 AOP 单通道：控制器**必须保留类级 `@Validated`**；参数级（`@RequestBody` 上的 `@Valid`）与类级方法校验的表达分工明确，两类失败都应被处理         | 【强制】 ↑ | [ann-validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)                                                                                                                                          | **规则文本按本仓实测改写**：官方「移除类级 `@Validated` 以启用 MVC 内建方法校验」在本仓**不采纳** ⇒ 偏离 **E6**；落点 `UserController.java:46`（§7.1）。**升格理由**：官方为「推荐」且方向相反（要求移除类级注解），本仓反向定为【强制】——删掉它 `detail` 即退化为固定文案，属可判定的契约破坏 | ADR-0013 |
| `@Valid` 本身不是约束注解（只表达嵌套约束）；`@Validated` 支持 validation groups，并可用于类级方法校验（经 `MethodValidationPostProcessor` 的 AOP 代理） | 【强制】   | [beanvalidation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html) ／ [`@Validated` javadoc](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/validation/annotation/Validated.html) | 守现状：`@Valid` 只用在 `@RequestBody` 参数边界（8 处，`UserController.java:60,73,116,126,136,146,156,166`）；类级 `@Validated` 1 处；`groups` 未使用                                                                                                                                          | —        |
| 类级 `@Validated` 的方法校验依赖 AOP 代理，必须通过代理访问，直接字段访问无效                                                                            | 【强制】   | [beanvalidation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)                                                                                                                                                    | 守现状：MVC 经 bean 引用（代理）分发；无字段直访、无 `this.` 自调用校验                                                                                                                                                                                                                        | —        |
| Bean Validation 在 classpath 时 `LocalValidatorFactoryBean` 被注册为全局 `Validator`；另行注入该 bean 需 `@Primary`（或给 MVC 配置的那个标 `@Fallback`） | 【强制】 ↑ | [mvc-config/validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/validation.html)                                                                                                                                           | 守现状：starter 在编译 / 运行类路径；无自定义 `Validator` bean、无注入点、无冲突（`@Primary` / `@Fallback` 命中 0）。**升格理由**：官方为「推荐」，但本仓把「只有一个 `Validator`」定为前提——两个候选时注入点会直接启动失败，属装配期硬约束                                                    | —        |
| 校验错误消息经 `MessageSource` 定制（`ConstraintViolation` → `FieldError` 的 error codes）                                                               | 【推荐】   | [beanvalidation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)                                                                                                                                                    | —（不在册：项目错误文案不做 i18n，main 无 `MessageSource`；`detail` 面向开发者英文，见 ADR-0013）                                                                                                                                                                                              | ADR-0013 |

### 7.1 单通道：为什么保留类级 `@Validated`

Spring Framework 6.1 起 MVC 内置了方法级校验，官方文档写得很清楚：要启用它， **需要移除类级 `@Validated`**——两者是 **互斥**
的，不是并行的两条通道。本仓选择保留类级 `@Validated`，走 AOP 通道，理由是契约而非偏好：

| 通道                | 触发方式                                            | 失败异常                           | `detail` 内容                                                                                                |
|---------------------|-----------------------------------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **AOP（本仓选用）** | 类级 `@Validated` + `MethodValidationPostProcessor` | `ConstraintViolationException`     | 字段级反馈（`mobile: must be …`），经 [ADR-0013](../adr/0013-error-response-structure.md) 的 detail 契约透出 |
| MVC 内建（未选用）  | 移除类级 `@Validated`                               | `HandlerMethodValidationException` | 退化为 `"Validation failure"`——丢失字段级反馈                                                                |

隔离探针（删除 `UserController.java:46` 的 `@Validated`、跑 `ProblemDetailMvcTest`，已还原）实证了第二行的退化：内建通道下
`MethodValidationException` 无 message，`ErrorResponse.getDetailMessageCode()` 落到 `ErrorResponse.DEFAULT`。类级
`@Validated` 因此是 ADR-0013 `detail` 契约的 **使能条件**，不可删。完整理由见 §8 E6。

### 7.2 两层校验各管什么

`@Valid` 与 `@Validated` 不是二选一，各管一段边界：

- **`@Valid`（参数级）**：`@RequestBody` 上的请求体校验——Request 承载 JSR 380 约束注解，失败抛
  `MethodArgumentNotValidException`，由 `ProblemDetailAdvice`
  继承的基类翻译。协议边界的职责归属见 [adapter.md](adapter.md) §1
- **类级 `@Validated`（方法级）**：参数上直接写约束（`@Min` / `@NotBlank` 等）或方法 / 返回值约束——失败抛
  `ConstraintViolationException`，由 advice 的自有映射译 400

校验的 **业务**部分不在这里：DP 构造器校验与 `ValidateUtils` / `Assert`
的分工见 [dp-validation-conventions.md](dp-validation-conventions.md)
与 [framework-crosscutting](framework-crosscutting.md) §5「Exception」；判空工具的参数顺序（`ValidateUtils` 仿 Spring
`Assert`：被校验值在前、辅助参数视为可信）见根 [STYLEGUIDE](../../STYLEGUIDE.md) §2.2；应用层入参契约（AppService 不校验入参）见
framework-crosscutting 同节「参数契约」。

```java
// ❌ 只标 @Valid、丢掉类级 @Validated：参数上的直接约束不再被 AOP 通道拦截，detail 退化为固定文案
@RestController
public class UserController {
    @PatchMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid UpdateUserRequest request,
                                   @IfMatch Integer expectedVersion) { …}
}

// ✅ 类级 @Validated（AOP 通道，拦参数上的 @Positive）+ 参数边界 @Valid（协议边界，拦 Request 约束）
@Validated
@RestController
public class UserController {
    @PatchMapping("/{id}")
    public UserResponse updateUser(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid UpdateUserRequest request,
                                   @IfMatch Integer expectedVersion) { …}
}
```

## 8. 偏离表

| 偏离项                       | 官方做法                                                                            | SODA 做法                                                                                      | 理由                                                                                                                                                                                                                                          | 关联 ADR                                                                                                         |
|------------------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| **E1** 全局回滚语义          | 自 6.2 起建议 `@EnableTransactionManagement(rollbackOn = ALL_EXCEPTIONS)`（§4）     | 保持框架默认（仅 unchecked 回滚），改以【强制】「事务参与方不得声明 checked 异常」加固         | 官方切换的前提是存在 checked 抛出面，本仓该面为零（main 内 9 处 `catch` 全为本方法内解析兜底）；`ALL_EXCEPTIONS` 的已知代价是内层捕获 checked 后继续 ⇒ 外层提交抛 `UnexpectedRollbackException`。为一个当前不存在的问题引入全局语义漂移不划算 | [ADR-0015](../adr/0015-exception-class-convention.md)                                                            |
| **E2** 自动配置独立包空间    | 4 个 `@AutoConfiguration` 类应迁至专用包（§3.3）                                    | 保持现状（与普通类同包），改以【强制】「自动配置类不得落在任何 `scanBasePackages` 覆盖范围内」 | 两部署单元只扫 `com.soda.user`，`com.soda.component.*` 不在扫描面 ⇒ 未实际命中；重排包会牵动 Modulith 模块声明与 imports 文件，收益为负                                                                                                       | [ADR-0008](../adr/0008-layered-component-starters.md) / [ADR-0019](../adr/0019-adapter-starter-channel-split.md) |
| **E3** `@Bean` 返回类型      | 返回实现类优先于接口，否则按类型判断的 `@ConditionalOnMissingBean` 会失准（§3.3）   | `SpringDomainEventBusAutoConfiguration.java:23` 返回接口 `DomainEventBus`                      | 端口封装意图（按端口退让、模块可提供自定义实现覆盖）；`@ConditionalOnMissingBean(DomainEventBus.class)` 显式指定类型 ⇒ 条件判定不失准                                                                                                         | [ADR-0008](../adr/0008-layered-component-starters.md)                                                            |
| **E4** ETag 条件请求无 304   | 控制器内比较后返回 `304 NOT_MODIFIED`；无法预计算用 `ShallowEtagHeaderFilter`（§6） | 仅强验证器生成 + `If-Match` 412，无 304 / `Last-Modified`                                      | 项目无 GET 端点（11 个端点全为写操作，读侧走独立查询服务）⇒ 304 不可实现                                                                                                                                                                      | [ADR-0039](../adr/0039-conditional-request-transport-contract.md)                                                |
| **E5** 不用 `readOnly`       | `readOnly = true` 标在查询方法上（§4）                                              | main 零 `readOnly`；只读语义由 Spring Data Repository 层承担                                   | 两条查询路径最外层均为读写事务 ⇒ 网关类级 `readOnly` 按 `REQUIRED` 语义被忽略、从未生效；保留声明反而误导读者期待只读事务优化                                                                                                                 | —                                                                                                                |
| **E6** 保留类级 `@Validated` | 移除类级 `@Validated` 以启用 MVC 内建方法校验（§7）                                 | 控制器保留类级 `@Validated`，走 AOP 通道                                                       | 内建通道下 `ConstraintViolation` 无 message ⇒ `detail` 退化为 `"Validation failure"`（隔离探针实证），丢失字段级反馈，违反 `detail` 契约                                                                                                      | [ADR-0013](../adr/0013-error-response-structure.md)                                                              |
| **E7** 连接池容量未配置      | `REQUIRES_NEW` 需按「并发 + 1」配置连接池（§4）                                     | 不配（Hikari 默认）                                                                            | 唯一 `REQUIRES_NEW` 位于 AFTER_COMMIT 且无并发驱动（H2 内存库）⇒ 无证据表明不足；加启动断言属过度工程                                                                                                                                         | —                                                                                                                |

## 检视清单

code-review 标准轴按本清单逐条核对（每组逐条可判定，括号内为落点）：

### 代码

1. `@Autowired` 零出现（含字段 / setter）；所有托管类依赖为 `final` 构造器参数（§2）
2. 单构造器无 `@Autowired`；多构造器恰有一个标 `@Autowired`（§2）
3. 服务层用 `@Service`，非服务 bean 用 `@Component`；`@Repository` 不手写（§3.1）
4. `@AutoConfiguration` 类未叠加 `@Configuration`、未改写 `proxyBeanMethods`（§3.2）
5. 自动配置类带 `@Conditional`；默认 bean 带 `@ConditionalOnMissingBean`；条件注解只出现在自动配置类（§3.5）
6. `@Transactional` 标在具体类 / 方法而非接口；事务方法为 `public`（§4.1）
7. 无 `readOnly` / `timeout` / `isolation`；无 `rollbackFor` / `rollbackOn`；事务参与方不声明 checked 异常（§4.2/§4.3）
8. 无类内自调用事务；无 `@PostConstruct` 依赖代理行为（§4.1）
9. `@TransactionalEventListener` 无 `fallbackExecution`；监听器 bean 非 `@Lazy`（§5.2）
10. 无 `@Async`（若新增：伴随 `@EnableAsync`、方法 `public`、经代理调用）（§5.3）
11. 事件载荷为普通对象（不继承 `ApplicationEvent`）；`ApplicationEventPublisher` 直接注入（§5.1）
12. `@ExceptionHandler` 返回 `ProblemDetail`；advice 为 `@ControllerAdvice` 且限定包（§6.1）
13. 控制器保留类级 `@Validated`；`@Valid` 只出现在参数边界（§7.1/§7.2）
14. 自定义 MVC 扩展实现 `WebMvcConfigurer`；无 `@EnableWebMvc`、无 `DelegatingWebMvcConfiguration`（§6.2）

### 依赖与构建声明

15. starter 用 `spring-boot-starter-webmvc`，无 `spring-boot-starter-web`（§1.2）
16. 无 `javax.*` 注解 import、无 `org.springframework.lang.*`（§1.2）
17. 无 `RestTemplate`、无 `<mvc:*>` XML、无已移除的路径匹配选项（§1.2）

### 库专属

18. 单一 `@SpringBootApplication` per 部署单元，且只标在主 `@Configuration` 类上（§3.6）
19. 启动入口不含 `@EntityScan` / `@EnableJpaRepositories`；扫描根声明在持久化模块内（§3.6）

