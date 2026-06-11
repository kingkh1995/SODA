# 0001 — Module Architecture

**Status**: accepted

**Context**: 项目从零搭建 DDD 脚手架，需要确定模块拆分方案、命名约定和依赖方向。参考了 COLA（领域共享 + 业务 5 层）、yudao-cloud（基础设施 starter + 读服务简单混装）、kk-ddd（上一轮实践的教训）。

**Decision**:

- **领域共享**放 `soda-components/`，子模块按 `soda-component-xxx` 命名。纯 jar 或 starter 皆可，依赖原始 spring-core / lombok。参考 COLA 组件列表。
- **基础设施共享**放 `soda-supports/`，子模块按 `soda-support-starter-xxx` 命名。每个子模块是 Spring Boot starter（autoconfigure）。参考 yudao-framework。
- **业务模块**两层平铺：根模块 `soda-xxx/`，所有子模块平级放在根下，无中间 pom 模块。
- 写服务用 DDD（COLA 5 层：adapter + app + domain + infra），读服务用简单混装（yudao `-server` 风格）。
- 读写共用 `soda-xxx-api` 模块存放共享 DTO、Feign 接口。
- 写侧用 `soda-xxx-start` 模块作为启动入口，聚合 adapter + infra。

### Architecture diagram

```
soda/                                        ← rootProject
├── settings.gradle
├── build.gradle
│
├── soda-components/                ← [聚合 pom] 领域共享
│   ├── soda-component-dto/
│   ├── soda-component-exception/
│   ├── soda-component-domain-starter/
│   ├── soda-component-extension-starter/
│   ├── soda-component-statemachine/
│   └── soda-component-ruleengine/
│
├── soda-supports/                  ← [聚合 pom] 基础设施共享
│   ├── soda-support-starter-mybatis/
│   ├── soda-support-starter-redis/
│   ├── soda-support-starter-web/
│   ├── soda-support-starter-security/
│   └── soda-support-starter-xxx/
│
├── soda-user/                      ← [聚合 pom] 业务模块 example
│   ├── soda-user-api/              ← 共享 DTO/Feign 接口（读写共用）
│   ├── soda-user-start/            ← 写侧启动入口（@SpringBootApplication）
│   ├── soda-user-adapter/          ← COLA adapter — 写 Controller
│   ├── soda-user-app/              ← COLA app — 写 ApplicationService
│   ├── soda-user-domain/           ← COLA domain — 领域层
│   ├── soda-user-infrastructure/   ← COLA infra — 写 Repository 实现
│   └── soda-user-query-server/     ← 读 Controller + Service + DAO
│
├── soda-order/                     ← [聚合 pom]
│   ├── soda-order-api/
│   ├── soda-order-start/
│   ├── soda-order-adapter/
│   ├── soda-order-app/
│   ├── soda-order-domain/
│   ├── soda-order-infrastructure/
│   └── soda-order-query-server/
│
└── soda-bpm/                       ← [聚合 pom]
    ├── soda-bpm-api/
    ├── soda-bpm-start/
    ├── soda-bpm-adapter/
    ├── soda-bpm-app/
    ├── soda-bpm-domain/
    ├── soda-bpm-infrastructure/
    └── soda-bpm-query-server/


## Module details

### `soda-user-api` — 共享 API/DTO

**职责**: 读写两侧共用的接口和数据定义，外部系统通过此模块与服务交互。

**包含的类**:

| 类别 | 示例 |
|------|------|
| DTO | `UserDTO` |
| VO | `UserVO` |
| Command | `CreateUserCommand`、`UpdateUserCommand` |
| Query | `UserPageQuery`、`UserDetailQuery` |
| Feign 接口 | `UserApiFeign`、`UserQueryApi` |

**依赖方向**: 无外部依赖（纯 POJO）。

**被依赖**:
- `soda-user-app` — 直接依赖
- `soda-user-query-server` — 直接依赖
- `soda-user-adapter` — 间接依赖（通过 app）

### `soda-user-start` — 写侧启动入口

**职责**: 写服务的聚合部署单元。提供 Spring Boot 启动入口、配置文件和静态资源。

**包含的内容**:

| 类别 | 说明 |
|------|------|
| `@SpringBootApplication` | `UserCommandApplication.java`，启动类 |
| 配置文件 | `application.yml` |
| 日志配置 | `logback-spring.xml` |
| 静态资源 | banner、static 目录等 |

**依赖**:
- `soda-user-adapter` — 直接依赖（拉起整条写服务调用链）
- `soda-user-infrastructure` — 直接依赖（确保基础设施实现被 Spring 扫描到）

**被依赖**: 无（部署单元，不被其他模块引用）


### `soda-user-adapter` — 适配层

**职责**: 写服务的适配层。接收外部请求/消息/定时任务，调用应用层。

**包含的类**:

| 层次 | 示例 |
|------|------|
| Controller | `UserController`、`UserAdminController` |
| Consumer | `UserMessageConsumer`（MQ 消息监听） |
| Job/Scheduler | `UserSyncJob`（定时任务） |
| Assembler | `UserAssembler`（DTO ↔ VO 转换） |

**职责说明**:
- Controller 负责将 DTO（通用模型）转换为 VO（Controller 专属模型）
- `soda-user-api` 中的 DTO 是通用模型
- `soda-user-adapter` 中的 VO 是 Controller 专属的展现模型

**依赖**:
- `soda-user-app` — 调用 ApplicationService

**被依赖**:
- `soda-user-start` — 直接依赖


### `soda-user-app` — 应用层

**职责**: 业务编排。接收 Command/Query，构造或查询领域对象，调用领域方法，持久化，发布领域事件，管理事务。

**包含的类**:

| 类别 | 示例 | 说明 |
|------|------|------|
| ApplicationService | `UserCreateService` | 接受 Command → 返回 Void/Identifier |
| | `UserUpdateService` | 接受 Command → 返回 DTO |
| EventPublisher | `UserEventPublisher` | 发布领域事件 |
| DTOConverter | `UserDTOConverter` | DTO ↔ Domain 对象互转 |

**依赖**:
- `soda-user-domain` — 调用领域对象和 Repository 接口
- `soda-user-api` — 引用 Command/Query/DTO

### `soda-user-domain` — 领域层

**职责**: 核心业务逻辑。使用充血模型，包含所有业务规则和领域知识。

**包含的类**:

| 类别 | 示例 | 说明 |
|------|------|------|
| Aggregate Root | `User extends Aggregate<UserId>` | 聚合根，实体聚合入口 |
| Entity | `Account extends Entity<AccountId>` | 拥有标识和业务行为的对象 |
| Value Object | `UserId`、`AccountType`、`Username`、`Hash` | 不可变值对象，封装领域概念 |
| Domain Service 接口 | `UserService extends EntityService` | 跨实体的业务逻辑契约 |
| Domain Service 实现 | `UserServiceImpl`、`AccountServiceImpl` | 领域服务实现 |
| Repository 接口 | `UserRepository`、`AccountRepository` | 持久化契约（防腐层） |
| Gateway 接口 | 其他外部依赖的防腐层接口 | 屏蔽底层实现 |
| Factory | `UserFactory`（可选） | 复杂聚合的创建 |

**关键特征**（参考 kk-ddd）:
- Entity 拥有业务方法，如 `user.save(…)`、`account.unbind()`、`account.validate()`
- 业务方法内部完成所有校验和规则判断
- Repository = 防腐层接口的一种，名称不强制 `Gateway` 结尾
- 所有持久化和外部依赖都通过防腐层接口抽象

**依赖**:
- `soda-component-*` — Entity/Aggregate/Identifier 基类、领域异常、DTO 基类

**被依赖**:
- `soda-user-app` — 调用领域方法
- `soda-user-infrastructure` — 实现 Repository/Gateway 接口

### `soda-user-infrastructure` — 基础设施层

**职责**: 实现 domain 层定义的 Repository/Gateway 接口。对接数据库、外部服务、消息中间件等。

**包含的类**:

| 类别 | 示例 | 说明 |
|------|------|------|
| Repository 实现 | `UserRepositoryImpl`、`AccountRepositoryImpl` | 实现 domain 的 Repository 接口 |
| DAO / Mapper | `UserMapper`、`UserDOMapper` | 数据库访问 |
| PO / DataObject | `UserPO`、`UserDO` | 持久化对象 |
| Converter | `UserConverter` | PO ↔ Domain 互转 |
| 外部 RPC 实现 | `SmsClientImpl`、`PaymentClientImpl` | 实现 domain 的 Gateway 接口 |
| FeignClient 实现 | `ExternalUserClient` | 调用第三方系统 |
| Message Producer | `UserEventProducer` | 发送领域事件到 MQ |
| 配置类 | `MyBatisConfig`、`RedisConfig` | 技术组件配置 |

**依赖**:
- `soda-user-domain` — 实现其 Repository/Gateway 接口
- `soda-support-starter-*` — 按需引用（mybatis、redis、mq 等）

**被依赖**:
- `soda-user-start` — 直接依赖


### `soda-user-query-server` — 读服务

**职责**: 读服务的独立部署单元。yudao 风格混装，简单高效，无 DDD 分层。

**包含的类**:

| 类别 | 示例 |
|------|------|
| `@SpringBootApplication` | `UserQueryApplication.java` |
| Controller | `UserQueryController` |
| Service | `UserQueryService` |
| DAO / Mapper | `UserQueryMapper` |
| 配置文件 | `application.yml`、`application-dev.yml` |

**依赖**:
- `soda-user-api` — 使用 DTO
- `soda-support-starter-*` — 按需引用

**被依赖**: 无（部署单元）


### `soda-components` — 领域共享

**父模块**: `soda-components`（聚合 pom），子模块按 `soda-component-xxx` 命名。

**子模块及内容**:

| 子模块 | 内容 |
|--------|------|
| `soda-component-dto` | DTO/Command/Query 基类、`Page` 等通用数据容器 |
| `soda-component-exception` | `BizException`、`SysException` 异常体系 |
| `soda-component-domain-starter` | `Entity`/`Aggregate`/`Identifier` 注解和基类、`DomainFactory` |
| `soda-component-extension-starter` | 扩展点机制（可选） |
| `soda-component-statemachine` | 状态机引擎（可选） |
| `soda-component-ruleengine` | 规则引擎（可选） |

**依赖**: spring-core、lombok、validation-api 等基础库。

**被依赖**:
- `soda-user-domain` 等各业务模块的 domain 层


### `soda-supports` — 基础设施共享

**父模块**: `soda-supports`（聚合 pom），子模块按 `soda-support-starter-xxx` 命名。

**职责**: 基础设施公共封装，每个子模块是一个 Spring Boot starter（autoconfigure）。

**子模块举例**:

| 子模块 | 提供能力 |
|--------|----------|
| `soda-support-starter-mybatis` | MyBatis + 分页自动配置 |
| `soda-support-starter-redis` | Redis / Redisson 自动配置 |
| `soda-support-starter-web` | 通用 Web 配置（Jackson、CORS、异常处理） |
| `soda-support-starter-security` | Spring Security 集成 |
| `soda-support-starter-mq` | RocketMQ / Kafka 集成 |
| `soda-support-starter-xxx` | 按需追加 |

**每个 starter 的结构**:

| 包 | 内容 |
|----|------|
| `core/` | 核心封装（可选） |
| `config/` | `@Configuration` + `@ConditionalOnClass` |
| `META-INF/spring/` | `AutoConfiguration.imports` |

**被依赖**: `soda-user-infrastructure` / `soda-user-query-server` 按需引用

**Considered options**:
- **统一父模块（COLA 方式）**：所有组件放在一个父 POM 下，靠 `-starter` 后缀区分。放弃原因：我们同时有领域共享和基础设施共享两种依赖特性（jar vs starter），分开父模块职责更清晰。
- **分开父模块（选定）**：领域共享纯 jar/轻量 starter，基础设施共享 Spring Boot starter，依赖方向单向。
- **中间 pom 模块**：command 侧多加一层聚合。放弃原因：Gradle 不需要空气中间模块，增加复杂度无收益。

**Consequences**:
- 单个业务模块最多 7 个 Gradle 子模块（api + start + 4 写 + 1 读），拆得细但依赖路径清晰
- 领域共享 starter（如 `domain-starter`）轻量，不依赖 Spring Boot autoconfigure（待定，看具体实现）
- 读服务的 query-server 模块是 yudao 风格混装，没有 domain/app/infra 分层 — 读写两侧架构不一致，但这是有意的 CQRS 权衡
