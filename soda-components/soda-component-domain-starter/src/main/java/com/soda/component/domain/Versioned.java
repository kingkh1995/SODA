package com.soda.component.domain;

/**
 * 版本化对象契约接口 — 声明持有乐观锁版本号的领域对象。
 * <p>
 * 实现类声明「我有版本号」：暴露乐观锁版本（{@link #getVersion()}，<b>非空契约</b>——
 * 版本化实体恒有版本，创建路径为初始版本）；{@link #ifMatch} 回答「当前版本是否为给定期望版本」，
 * 应用层经 {@code AbstractAppService.requireIfMatch} 统一守卫（见 ADR-0037）；{@link #assignVersion}
 * 供基础设施在写库 flush 后回填真实落库版本（递增归基础设施层，同 {@code Entity.assignId} 回填语义）。
 * <p>
 * 返回 {@link IntLiteralType} 而非具体 DP：application-starter 对 domain-types
 * 零依赖，具体聚合凭协变返回满足契约（如 {@code User.getVersion(): Version}），
 * 零额外样板。无乐观锁版本列的实体不实现本契约。
 *
 * @see IntLiteralType
 */
public interface Versioned {

    /**
     * 当前乐观锁版本（恒非 null）。
     *
     * @return 版本字面量，恒非 null
     */
    IntLiteralType getVersion();

    /**
     * 当前版本是否为给定期望版本（值相等）—— 判定原语，不加载、不抛异常，失配的处置归调用方
     * （应用层守卫）。
     *
     * @param version 客户端期望版本，非 null
     * @return 相等时 true
     */
    boolean ifMatch(IntLiteralType version);

    /**
     * 回填权威版本 —— 由基础设施在写库 flush 后调用，把 {@code @Version} 实际落库的版本同步回聚合。
     * <p>
     * 递增由基础设施层负责（{@code WHERE version = ?} 校验后落 {@code version + 1}），故聚合侧只接受
     * 落库结果、不自增：是否发 UPDATE、从而是否递增，由持久化层按脏字段与审计列决定，自增会与行版本漂移。
     * <p>
     * 实现契约：落库版本单调不减——同值 = 本次未发 UPDATE 的等价写（接受），回退 = 基础设施 bug，
     * 实现方抛裸 {@code IllegalStateException}（防御编程不携消息，见 ADR-0015）。
     *
     * @param version 落库后的真实版本，非 null 且不低于当前值
     */
    void assignVersion(IntLiteralType version);
}
