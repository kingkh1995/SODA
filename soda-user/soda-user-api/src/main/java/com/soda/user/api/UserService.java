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
 * <p><b>参数契约</b>：实现不校验入参，调用方必须保证参数合法（存在性 / 格式 / 业务约束）。
 * 方法入参默认非空；仅在可为 {@code null} 时标注 {@code @Nullable}（JSpecify）。
 * Command 对象属性遵循同一约定：未标注 {@code @Nullable} 的属性默认非空。
 */
public interface UserService {
    UserDTO createUser(CreateUserCommand command);

    void updateUser(UpdateUserCommand command);

    void deregisterUser(DeregisterUserCommand command);

    void disableUser(DisableUserCommand command);

    void enableUser(EnableUserCommand command);

    void changeUsername(ChangeUsernameCommand command);
}
