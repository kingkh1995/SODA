package com.soda.user.infrastructure.gateway.persistence;

import com.soda.component.domain.types.ConcurrencyVersion;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.User;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.infrastructure.convertor.UserArchiveConvertor;
import com.soda.user.infrastructure.convertor.UserConvertor;
import com.soda.user.infrastructure.repository.UserArchiveRepository;
import com.soda.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * {@link UserGateway} 的 JPA 实现 — 单表 {@code user} + 派生账户组装。
 * <p>
 * 编排：查询 → {@link UserConvertor} 双向转换。领域↔持久化映射逻辑见 convertor。
 * <p>
 * 乐观锁：JPA {@code @Version} 自动校验 {@code WHERE version = ?} 并递增（领域层
 * 「递增由基础设施层负责」契约，IDDD ConcurrencySafeEntity 同款），flush 后经
 * {@code Versioned.assignVersion} 把落库版本回填聚合（聚合令牌与行版本恒一致——响应
 * {@code ETag} / 下次 {@code If-Match} 自洽的前提）；冲突抛
 * {@code ObjectOptimisticLockingFailureException}。
 * <p>
 * 注销终态（ADR-0017/0023）：无删除契约，终态 R 由 {@code save} 持久化 state 列；
 * D→R 迁移在 save 内走特殊分支——归档原键（{@code user_archive} 审计表，同事务）
 * 后三键（username/mobile/email）置空落库（键释放，领域不感知）；
 * 已终态行（persisted state=R）任何写入被拒绝（基础设施兜底守卫）。
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGatewayImpl implements UserGateway {

    private final UserRepository userRepository;
    private final UserArchiveRepository userArchiveRepository;

    // ─── EntityGateway：save / findById / findAllById ───

    @Override
    @Transactional
    public UserId save(User user) {
        if (!user.isIdentified()) {
            // 创建路径（服务端生成 id）：id==null → isNew=true → persist（IDENTITY 立即生成 id，INSERT 后回填）
            var saved = userRepository.saveAndFlush(UserConvertor.toPersistence(user));
            var generatedId = new UserId(saved.getId());
            user.assignId(generatedId);
            user.assignVersion(ConcurrencyVersion.from(saved.getVersion()));
            return generatedId;
        }
        // 更新路径：findById 仅为终态守卫读取持久化态（merge 不需要既有行基线——
        // save(toPersistence(user)) 对 id 有值走 em.merge，路由与乐观锁由框架内建；
        // 审计列由 auditing + updatable=false 自动处理，见 UserConvertor javadoc；
        // 不原地改托管实例——会绕过乐观锁快照）
        // 防御编程：行不存在 → orElseThrow NSE（框架无删除契约，id 有效则行必在，缺失即调用方 bug）
        var persisted = userRepository.findById(user.getId().value()).orElseThrow();
        // 基础设施兜底：终态（吸收态）行不可写——领域守卫之外的任何写路径在此被拒（ADR-0023）。
        // 检查持久化行状态（非聚合状态）：D→R 迁移合法地把聚合写成终态 R，行状态才是权威——
        // 拦截「域状态与行状态不一致」的绕过路径（陈旧聚合/双重注销）——防御编程兜底：
        // 调用方按契约调用，异常类型 + 栈帧即语义，不携消息
        if (UserState.of(persisted.getState()).terminal()) {
            throw new IllegalStateException();
        }
        if (UserState.R.equals(user.getState())) {
            // D→R 注销迁移（领域 deregister 前置保证 persisted 为 D）：归档原键 → 键置空 → 落库。
            // 同事务性保证并发安全：双注销后写者版本校验失败，归档行随事务一并回滚。
            userArchiveRepository.save(UserArchiveConvertor.toPersistence(user));
            var entity = UserConvertor.toPersistence(user);
            entity.setUsername(null);
            entity.setMobile(null);
            entity.setEmail(null);
            var merged = userRepository.saveAndFlush(entity);
            user.assignVersion(ConcurrencyVersion.from(merged.getVersion()));
            return user.getId();
        }
        // 常规 update：id 有值 → merge（@Version 校验自动）；flush 后把落库的真实版本回填聚合。
        // 回填而非自增：是否发 UPDATE（从而递增）由持久化层按脏字段 / 审计列决定，聚合自增会与行版本漂移
        // （响应 ETag 随之陈旧或超前，客户端回带 If-Match 必失配）。
        var merged = userRepository.saveAndFlush(UserConvertor.toPersistence(user));
        user.assignVersion(ConcurrencyVersion.from(merged.getVersion()));
        return user.getId();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userRepository.findById(id.value()).map(UserConvertor::toDomain);
    }

    @Override
    public List<User> findAllById(Iterable<UserId> ids) {
        var values = StreamSupport.stream(ids.spliterator(), false)
                .map(UserId::value)
                .toList();
        return userRepository.findAllById(values).stream().map(UserConvertor::toDomain).toList();
    }

    // ─── UserGateway 专用查询 ───

    @Override
    public Optional<User> findByUsername(Username username) {
        return userRepository.findByUsername(username.value()).map(UserConvertor::toDomain);
    }

    @Override
    public Optional<User> findByMobile(Mobile mobile) {
        return userRepository.findByMobile(mobile.value()).map(UserConvertor::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userRepository.findByEmail(email.value()).map(UserConvertor::toDomain);
    }
}
