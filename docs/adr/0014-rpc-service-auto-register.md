# 0014 — RPC Service 注册策略：自动注册 + YAML 覆盖

使用 `DubboServiceAutoRegister` 在 adapter 层自动发现 api 模块的接口实现并注册为 Dubbo Service，通过 YAML 实现按服务覆盖配置，避免每个服务手写 wrapper 类。

**Status**: proposed

> **阻塞项：** Apache Dubbo 当前不支持 Spring Boot 4。等 Dubbo 发布兼容版本后可重新评估该提案并升级为 `accepted`。在此期间所有 RPC 相关的实施决策该提案不适用。
> 关联影响：Command 上的 JSR 380 校验注解也暂不加——当前仅在 HTTP 入口（Request 对象）做校验。

## Context

SODA 的写侧架构中，ApplicationService 作为业务用例的入口，同时暴露为 HTTP API 和 Dubbo RPC 服务。

初始设计有两条路：

- **方案 B：** 直接在 ApplicationService 实现类上加 `@DubboService`。代价是 application 模块耦合 Dubbo 框架，部署拓扑决策（是否发布为 RPC、超时、重试策略）写在业务代码里。
- **方案 C：** Adapter 层为每个 Service 手写一个 shell 类（`UserRpcAdapter implements UserService`），全部委托。代价是每个服务一个额外的 .java 文件，boilerplate 随服务数量线性增长。

两条路都不理想。需要第三方案：零 boilerplate 但保留按服务定制能力。

同时需要确定参数校验的归属：

- HTTP：Request 上 `@Valid` 在校验后 map 成 Command，Command 的验证注解不会被触发
- RPC：没有 Request 类，直接反序列化为 Command

验证必须在协议边界完成，不能在 AppService 内部重复。因此 Command 需要携带验证注解，Dubbo `validation="true"` 负责 RPC 入口的验证执行。

## Decision

### RPC 注册机制

Adapter 层部署一个 `DubboServiceAutoRegister` 组件（`ApplicationListener<ContextRefreshedEvent>`），在启动时扫描所有 Spring bean，识别 `com.soda.*.api.*` 包的接口实现，通过 `org.apache.dubbo.config.ServiceConfig` 注册为 Dubbo Service。

```yaml
# application.yml
soda:
  rpc:
    defaults:
      validation: true       # 全局开启参数校验
      timeout: 5000           # 毫秒
      retries: 2
    services:
      UserService:
        timeout: 3000         # 覆盖超时
        retries: 0            # 幂等方法可安全重试，非幂等方法设为 0
        validation: false     # 可单独关闭（如纯读接口）
      InternalWorkerService:
        exported: false       # 不发布为 RPC
```

处理规则：

1. 对每个 bean 实现的所有 `com.soda.*.api.*` 接口生成 `ServiceConfig`
2. 先 apply `defaults` 全局默认值
3. 再按接口名（`iface.getSimpleName()`）查找 `services` 中的覆盖配置
4. 覆盖配置中的字段只覆盖对应属性，不重置整个对象
5. `exported: false` 跳过注册

### 验证边界

| 入口 | 验证时机 | 验证对象 | 执行器 |
|---|---|---|---|
| HTTP | Controller 层 `@Valid` | `Request` 对象 | Spring Validation |
| RPC | Dubbo `ValidationFilter` | `Command` 对象 | Dubbo Filter + jakarta.validation |
| AppService | 不执行验证 | — | — |

两条路径各校验一次，零重叠。

### Command 上的验证注解

Command 在 `soda-xxx-api` 模块中携带格式级验证注解：

- `@NotNull` / `@NotBlank` — 字段存在性
- `@Size(min, max)` — 字段长度范围
- `@Email` — 字段格式
- `@Pattern(regexp)` — ❌ 禁止在此使用（属于 Value Object 的领域规则）

领域规则验证保留在 Domain 层的 Value Object 中，与 Command 无关。

## Considered Options

- **方案 A1（选中的方案）** — 自动注册 + YAML 覆盖。零 boilerplate，显式配置，可单独定制。代价：注册逻辑是隐式的，新团队成员需要了解这个约定。
- **方案 A2（集中注册）** — `@Configuration` 中每 Service 一行 `export(ctx, XxxService.class)`。显式但每新增一行操作，不支持复杂的 per-service 配置。
- **方案 B（务实）** — ApplicationService 加 `@DubboService`。零 boilerplate，但 application 模块耦合 Dubbo 框架且无法按部署拓扑调整发布策略。
- **方案 C（原始 wrapper）** — Adapter 层每个 Service 一个 shell 类。最纯的 DDD 但 boilerplate 随服务数线性增长。拒绝。

## Consequences

- 新增一个 Service：实现 api 层接口 → application 层实现 → 自动被注册为 Dubbo Service，零额外代码
- 需要定制服务配置：改 `application.yml`，不改代码
- API 模块需要引入 `jakarta.validation-api` 依赖用于 Command 上的注解
- Adapter 模块需要引入 Dubbo 编程式 API 依赖（`dubbo-config-api`）用于自动注册器
- 团队需知晓隐式注册约定——代码中没有 `@DubboService` 注解，全部集中在 `DubboServiceAutoRegister`
