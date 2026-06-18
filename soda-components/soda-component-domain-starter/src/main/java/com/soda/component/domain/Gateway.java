package com.soda.component.domain;

/**
 * 所有 Gateway 的标记接口。
 * <p>
 * 对应 Spring Data JPA 中 {@code Repository} 的定位：标记持久化契约，
 * 供 IOC 容器扫描和 AOP 切面识别。不带任何方法。
 * <p>
 * 业务模块中 {@code *Gateway} 的根接口，实现类位于基础设施层。
 *
 * @see EntityGateway
 */
public interface Gateway {
}
