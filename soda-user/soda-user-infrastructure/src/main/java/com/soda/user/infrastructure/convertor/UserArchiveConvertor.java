package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.User;
import com.soda.user.infrastructure.persistence.UserArchivePO;

/**
 * {@link User} → {@link UserArchivePO} 单向转换（COLA 惯例：infrastructure 独立 convertor）。
 * <p>
 * 注销迁移归档快照（ADR-0023）— {@code UserGatewayImpl.save} 在 D→R 时调用：
 * 捕获释放前的原键值（username/mobile/email）落 {@code user_archive} 审计表。
 * 归档表纯写无读回领域路径，故仅 {@link #toPersistence} 单向。
 * <p>
 * 领域对象在注销事务内仍持原键（键释放是基础设施表示决策，领域不感知），
 * 故此处从领域值取原始数据；此后 R 行三键置空，原值仅存于归档表。
 * 命名：一持久化形状一 convertor——归档快照独立成类，不塞进 {@link UserConvertor}。
 */
public final class UserArchiveConvertor {

    private UserArchiveConvertor() {
        // 工具类
    }

    /**
     * 领域聚合 → 归档快照行。
     * <p>
     * 主键 = 原用户 ID（D→R 分支中 id 恒非空，见 {@code UserGatewayImpl.save}）；
     * 三键为释放前快照。id 恒有 → isNew=false → save 恒走 merge（insert 多一次 PK SELECT，
     * 1:1 行低频可接受，ADR-0024）。
     */
    public static UserArchivePO toPersistence(User user) {
        var archive = new UserArchivePO();
        archive.setId(user.getId().value());
        archive.setUsername(user.getUsername().value());
        archive.setMobile(user.getMobile().map(Mobile::value).orElse(null));
        archive.setEmail(user.getEmail().map(Email::value).orElse(null));
        return archive;
    }
}
