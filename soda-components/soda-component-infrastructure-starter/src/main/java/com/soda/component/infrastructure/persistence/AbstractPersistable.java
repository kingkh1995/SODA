package com.soda.component.infrastructure.persistence;

import jakarta.persistence.MappedSuperclass;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

/**
 * 持久化状态检测（{@code isNew}）基类 — 所有 PO 的强制统一基类。
 * <p>
 * {@code isNew()} 为 {@code id == null} 判定（Spring Data 自身 AbstractPersistable 的语义，
 * ADR-0024 merge 全权委托）：服务端生成 id 的实体（UserPO）创建路径 id==null → persist；
 * 客户端分配 id 的实体（VerificationPO/UserArchivePO）恒已标识 → merge——insert/update 由
 * merge 按行存在性统一路由。有 {@code @Version} 的实体（UserPO）isNew 判定由 version 值
 * 决定（version 属性存在时优先于 Persistable，Spring Data {@code JpaMetamodelEntityInformation}）
 * ——创建路径 version=0 亦走 merge（transient → INSERT），与无本基类时行为一致。
 * <p>
 * 早期形态（transient isNew 标志 + {@code markNotNew()} + {@code @PostLoad}/{@code @PostPersist}
 * 翻转）已随 merge 全权委托移除（ADR-0024）——本类仅保留 {@link Persistable} 契约与
 * {@code id == null} 判定，作为所有 PO 的统一基类（ADR-0023 命名惯例：
 * AbstractXxx implements Xxx）。
 * <p>
 * 不声明 {@code @Id} 字段（PO 主键策略异构：服务端自增/客户端 UUId/分配式），由各 PO
 * 自行声明；{@code Persistable.getId()} 由 PO 的 {@code @Getter} 生成（字段名与列名一致，
 * 仅下划线/驼峰形式差异）。
 * 不实现 {@code toString()}/{@code equals()}/{@code hashCode()}——PO 不参与业务相等性
 * （领域相等性由领域对象承担），保持最小面。
 *
 * @param <ID> 实体标识符类型（{@link Serializable}，与 Spring 基类同界）
 * @see AbstractAuditable
 */
@MappedSuperclass
public abstract class AbstractPersistable<ID extends Serializable> implements Persistable<ID> {

    @Override
    public boolean isNew() {
        return getId() == null;
    }
}
