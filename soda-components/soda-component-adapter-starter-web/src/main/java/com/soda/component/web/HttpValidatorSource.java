package com.soda.component.web;

/**
 * HTTP 验证器源 —— 响应体携带条件请求强验证器原料（RFC 7232 §2；AIP-154；见 ADR-0039），
 * {@code HttpValidatorHeadersAdvice} 统一派生 {@code ETag} 头。
 * <p>
 * {@code version} 是强验证器源（乐观锁版本，派生 {@code ETag: "n"}，含引号）。
 * 弱验证器（{@code Last-Modified}）不提供：审计时刻属基础设施表示、不入领域与出站模型
 * （ADR-0031），无常驻可派生源——记为 AIP-154 双发首选行为的偏离（见
 * {@code docs/conventions/aip-api-conventions.md} §9.2）。
 * <p>
 * {@code Http} 前缀消歧同包 {@code validation} 的 Bean Validation 语义；record 直接 {@code implements}
 * （全仓惯例，如 {@code UserId implements Identifier}）；无需改 class。不引 domain-types，
 * 避免 starter-web 反向依赖 domain。
 */
public interface HttpValidatorSource {

    /**
     * 乐观锁版本号 —— 强验证器源，派生 {@code ETag: "n"}（引号是值的一部分，RFC 7232 §2.3）。
     */
    int version();
}
