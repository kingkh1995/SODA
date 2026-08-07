package com.soda.user.domain.gateway;

import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

/**
 * 验证码实体的持久化契约（防腐层接口）。
 * <p>
 * 继承 {@link EntityGateway} 提供基础 CRUD（save、remove、findById、findAllById），
 * 扩展按业务场景查询的方法。验证实体在 {@code soda-user-domain} 模块，
 * 本 gateway 提供领域层访问接口。
 * <p>
 * 实现类位于基础设施层。
 *
 * @see EntityGateway
 * @see Verification
 */
public interface VerificationGateway extends EntityGateway<Verification<?>, UUId> {

    /**
     * 按用户查找最新一条验证（不限通道），结果按 {@link VerificationQuery} 过滤。
     * <p>
     * 「最新」定义为按 {@link VerificationCode#expireAt()} 倒序（expireAt 最晚者优先）——
     * 聚合无创建时间戳，策略差异（如 DEFAULT_SMS 5 分钟 vs DEFAULT_EMAIL 30 分钟）下
     * 创建顺序与过期顺序可能不一致；幂等发送保证 (userId, scene) 至多一条未过期 PENDING，
     * 因此调用方只需「未过期的待验证聚合」语义（见 ADR-0011）。
     * <p>
     * 过期过滤基于 {@link VerificationCode#expireAt()}：当 {@code query.validUntil()} 非空时，
     * 返回结果的 {@code expireAt} 严格晚于 {@code validUntil}（等价于 {@code !expiredAt(validUntil)}）；
     * 为空时不做过期过滤。
     *
     * @param userId 用户 ID（与实体属性同类型，{@code LongId}）
     * @param query  查询条件，可传 {@link VerificationQuery#all()} 仅按用户查询
     * @return 匹配的最新验证实体（如有）
     */
    Optional<Verification<?>> findLatestByUserId(LongId userId, VerificationQuery query);

    /**
     * {@link #findLatestByUserId(LongId, VerificationQuery)} 的类型收敛变体：
     * 结果类型由 {@code type} 决定——实现内部按 type 推导判别值过滤（ADR-0016），
     * 调用方无需知晓 channel↔class 映射、无需强转；无该类型的匹配时返回空。
     *
     * @param userId 用户 ID（与实体属性同类型，{@code LongId}）
     * @param query  查询条件，可传 {@link VerificationQuery#all()} 仅按用户查询
     * @param type   期望的验证类型（须为 {@link Verification} 的 permits 子类）
     * @return 匹配的最新且类型相符的验证实体（如有）
     */
    <T extends Verification<?>> Optional<T> findLatestByUserId(
            LongId userId, VerificationQuery query, Class<T> type);

    /**
     * 同一 (userId, scene) 当前是否存在未过期 PENDING 验证。
     * <p>
     * 基于 {@link #findLatestByUserId} 的便捷检查，判定时刻固定为
     * {@link Instant#now()}，供「同一时间同一场景至多一条未过期待验证码」的
     * 唯一性校验使用（见 ADR-0011）。
     *
     * @param userId 用户 ID（与实体属性同类型，{@code LongId}）
     * @param scene  验证场景
     * @return 当前存在未过期 PENDING 验证时为 {@code true}
     */
    default boolean hasUnexpiredPending(LongId userId, VerificationScene scene) {
        return findLatestByUserId(userId,
                new VerificationQuery(scene, VerificationStatus.P, Instant.now()))
                .isPresent();
    }

    /**
     * 最新验证查询条件 — 所有字段可选（{@code null} 表示不约束该条件）。
     * <ul>
     *   <li>{@code scene} — 验证场景过滤</li>
     *   <li>{@code status} — 验证状态过滤</li>
     *   <li>{@code validUntil} — 有效性判定时刻；非空时仅返回在此刻未过期的验证</li>
     * </ul>
     * <p>
     * 不含判别枚举（channel）——按通道过滤用类型收敛变体（ADR-0016 决策 4）。
     */
    record VerificationQuery(@Nullable VerificationScene scene,
                             @Nullable VerificationStatus status,
                             @Nullable Instant validUntil) {

        /**
         * 无任何过滤条件 — 仅按用户查询最新一条验证。
         */
        public static VerificationQuery all() {
            return new VerificationQuery(null, null, null);
        }
    }
}
