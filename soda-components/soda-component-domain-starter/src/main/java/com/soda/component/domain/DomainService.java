package com.soda.component.domain;

/**
 * 所有领域服务的标记接口。
 * <p>
 * 对应 Spring 中 {@code @Service} 的定位：标记领域服务契约，
 * 供 IOC 容器扫描和 AOP 切面识别。不带任何方法。
 * <p>
 * 业务模块中 {@code XxxDomainService} 的根接口，实现类位于
 * domain 模块的 {@code service} 子包（跨聚合编排，见 docs/conventions/framework-crosscutting.md「DomainService」条目）。
 *
 * @see Gateway
 */
public interface DomainService {
}
