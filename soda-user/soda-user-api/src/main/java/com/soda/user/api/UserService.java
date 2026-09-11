package com.soda.user.api;

import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeregisterUserCommand;
import com.soda.user.api.command.DisableUserCommand;
import com.soda.user.api.command.EnableUserCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.dto.UserDTO;

/**
 * 用户聚合根的 ApplicationService 接口。
 * 实现在 {@code soda-user-application} 模块。
 *
 * <p>Controller 只依赖此接口，不感知 application 模块的实现类。
 *
 * <p>参数契约见 `docs/conventions/framework-crosscutting.md` §参数契约。
 */
public interface UserService {

    /**
     * 创建用户 —— username/mobile/email 全局唯一前置检查，返回出站 DTO。
     */
    UserDTO createUser(CreateUserCommand command);

    /**
     * 更新用户资料 —— 掩码语态见 ADR-0038：省略掩码 = 仅写非 null 字段；命中掩码的字段传 null = 清空
     * （nickname 必填、不可清空）；`*` = 全量替换（必填字段不可缺）。
     */
    UserDTO updateUser(UpdateUserCommand command);

    /**
     * 注销用户 —— 终态迁移，前置必须禁用态 D（严格迁移）；R 为吸收态拒绝（见 ADR-0017）。
     * 键释放由基础设施表示决策负责，领域不感知（见 ADR-0023）。
     * 返回 R 态完整资源（AIP-164 软删除分支 should；内存快照，键为释放前原值，见实现注释）。
     */
    UserDTO deregisterUser(DeregisterUserCommand command);

    /**
     * 禁用用户 —— E→D 发状态事件；已是 D 则 no-op；R 拒绝（吸收态，见 ADR-0017）。
     */
    void disableUser(DisableUserCommand command);

    /**
     * 启用用户 —— D→E 发状态事件；已是 E 则 no-op；R 拒绝（吸收态，见 ADR-0017）。
     */
    void enableUser(EnableUserCommand command);

    /**
     * 修改账号 —— 新 Username 全局唯一前置检查（与 DB 唯一索引双重保证）。
     */
    void changeUsername(ChangeUsernameCommand command);
}
