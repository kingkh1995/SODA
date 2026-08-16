package com.soda.user.domain.gateway;

import com.soda.component.domain.EntityGateway;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;

import java.util.Collection;
import java.util.Optional;

/**
 * 验证码实体的持久化契约（防腐层接口）。
 * <p>
 * 继承 {@link EntityGateway} 提供基础 CRUD（save、findById、findAllById），
 * 扩展按业务场景查询的方法。验证实体在 {@code soda-user-domain} 模块，
 * 本 gateway 提供领域层访问接口。
 * <p>
 * 查询契约遵循 existsBy 命名规范（framework-conventions）：存在性谓词 = 键存在性 +
 * 唯一性约定（受 {@code uk_active_key} 唯一索引兜底）；活跃（I/P）/过期判定与键组合
 * （{@code source.compositeKey()} = {@code scene:subject}）为基础设施实现细节（见 ADR-0026）。
 * <p>
 * 实现类位于基础设施层。
 *
 * @see EntityGateway
 * @see Verification
 * @see VerificationSource
 */
public interface VerificationGateway extends EntityGateway<Verification, UUId> {

    /**
     * 契约：该 source 是否存在占用活跃键的行（所有场景统一——source 即唯一索引的槽位身份：
     * UCC/ULG/UPR = 用户、URG = 投递端点，见 ADR-0026）。活跃（I/P）与未过期判定、
     * 键组合（{@code source.compositeKey()}）为基础设施实现细节。
     * <p>
     * 过期 I/P 行的惰性删除<b>收敛进 {@code save}</b>（基础设施实现细节，非领域契约——
     * 2026-08-16，见 ADR-0026）：INSERT 前同事务删除同活跃键过期行（腾槽），应用层无感。
     */
    boolean existsBySource(VerificationSource source);

    /**
     * 消费反查：按 source 键控、候选状态内、<b>最新一条</b>（expire_at 倒序首行）的验证
     * （2026-08-16，见 ADR-0026）。
     * <p>
     * <b>命名语义三维齐备</b>（2026-08-16 会话修订）：{@code latest}——只返回最新一条
     * （expire_at 倒序限 1，排序/限行是检索语义而非实现细节）；{@code source}——source 键控；
     * {@code stateIn}——候选状态集合。前身命名均不完整：{@code findFirstBySource} 缺 stateIn、
     * {@code findBySource} 缺 latest。注：{@code uk_active_key} 唯一索引保证每 source 至多一条
     * I/P 行，latest 语义在单行场景退化为取该行——契约仍显式表达，防未来多行场景退化。
     * <p>
     * 消费命令（changeMobile/changeEmail）不含 target——换绑的新联系方式隐含在验证记录中，
     * 以 {@code VerificationSource.of(UCC, userId)} 加载即主体匹配（持码人即收码人）；
     * 「按 source 加载」本身即守卫，source 匹配守卫在 {@code User.changeXxx} 保留为防御纵深。
     *
     * @param states 候选状态集合（消费流传 {@code {P}}——changeMobile 加载 PENDING）
     */
    Optional<Verification> findLatestBySourceAndStateIn(
            VerificationSource source, Collection<VerificationState> states);
}
