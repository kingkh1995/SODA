package com.soda.user.api;

import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeleteUserCommand;
import com.soda.user.api.command.UpdatePasswordCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.command.UpdateUserStatusCommand;


/**
 * 用户聚合根的 ApplicationService 接口。
 * 实现在 {@code soda-user-application} 模块。
 *
 * <p>Controller 只依赖此接口，不感知 application 模块的实现类。
 */
public interface UserService {

    Long createUser(CreateUserCommand command);

    void updateUser(UpdateUserCommand command);

    void deleteUser(DeleteUserCommand command);

    void updateStatus(UpdateUserStatusCommand command);

    void changePassword(UpdatePasswordCommand command);

    void changeUsername(ChangeUsernameCommand command);
}
