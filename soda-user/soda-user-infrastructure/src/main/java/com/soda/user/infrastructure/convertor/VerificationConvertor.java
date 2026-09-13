package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Uuid;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationRecipient;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import com.soda.user.infrastructure.persistence.VerificationPO;

/**
 * {@link Verification} ↔ {@link VerificationPO} 双向转换（COLA 惯例：infrastructure 独立 convertor）。
 * <p>
 * <b>source/recipient 双概念</b>（见 ADR-0026）：{@code scene}/{@code subject} 两列
 * 重组为 {@link VerificationSource}（subject 裸键，无类型前缀）；{@code channel} + {@code target}
 * 两列承载 {@link VerificationRecipient}，restore 经 {@link VerificationRecipient#of(String, String)}
 * 双参工厂。
 * <p>
 * <b>{@link VerificationSource#compositeKey()} 为活跃键组合的单一来源</b>（convertor 设值、
 * gateway 预检/惰性 DELETE 共用，见 ADR-0026）。
 * <p>
 * {@code expire_at} 为绝对时间点：Hibernate {@code TIMESTAMP_UTC} 自动 UTC 规范化读写。
 */
public final class VerificationConvertor {

    private VerificationConvertor() {
        // 工具类
    }

    /**
     * 持久化行 → 领域验证实体。
     */
    public static Verification toDomain(VerificationPO e) {
        return Verification.builder()
                .id(new Uuid(e.getId()))
                .source(new VerificationSource(e.getScene(), e.getSubject()))
                .state(VerificationState.of(e.getState()))
                .code(new VerificationCode(e.getCode(), e.getExpireAt()))
                .recipient(VerificationRecipient.of(e.getChannel(), e.getTarget()))
                .build();
    }

    /**
     * 领域验证实体 → 持久化行（全量构造，创建与更新路径共用——id 恒有 → merge 按行存在性
     * 统一路由（无行 INSERT、有行 UPDATE），见 ADR-0024）。
     * <p>
     * 全量构造：领域对象是行的唯一事实源，逐列赋值（{@code active_key} 恒设
     * {@code source.compositeKey()}——终态（U）清 NULL 收敛进 {@code VerificationGatewayImpl.save}
     * （V 内存瞬态不落库，不涉槽位，见 ADR-0026）。
     * <p>
     * 审计列（{@code created_date}/{@code last_modified_date}）不在此构造（null 即可）——
     * 由 Spring Data auditing + {@code updatable=false} 自动处理（见 ADR-0024）。
     * User 版本兜底，见 ADR-0024）。
     */
    public static VerificationPO toPersistence(Verification verification) {
        var entity = new VerificationPO();
        entity.setId(verification.getId().value());
        entity.setScene(verification.getSource().scene());
        entity.setSubject(verification.getSource().subject());
        entity.setState(verification.getState().name());
        entity.setCode(verification.getCode().code());
        entity.setExpireAt(verification.getCode().expireAt());
        entity.setChannel(verification.getRecipient().channel().name());
        // 泛型捕获自带 value()（T extends StringLiteralType，见 ADR-0028）——免密封 switch 判别
        entity.setTarget(verification.getRecipient().target().value());
        // 活跃键恒设 source.compositeKey()——终态（U）清 NULL 收敛进 gateway.save（状态语义归网关，
        // convertor 纯字段映射；V 内存瞬态不落库不涉槽位，见 ADR-0026）
        entity.setActiveKey(verification.getSource().compositeKey());
        return entity;
    }
}
