package com.soda.user.api;

import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.VerifyEmailCommand;
import com.soda.user.api.command.VerifyMobileCommand;

/**
 * 用户凭证相关的 ApplicationService 接口。
 * <p>
 * 管理手机号 / 邮箱 / 密码的修改与验证，每个修改走两步验证流程：
 * <ol>
 *   <li>发送验证码 → {@link #verifyMobile(VerifyMobileCommand)} / {@link #verifyEmail(VerifyEmailCommand)}</li>
 *   <li>核验并修改 → {@link #changeMobile(ChangeMobileCommand)} / {@link #changeEmail(ChangeEmailCommand)}</li>
 * </ol>
 * 密码修改走单步流程 → {@link #changePassword(ChangePasswordCommand)}。
 * <p>
 * 实现在 {@code soda-user-application} 模块。
 *
 * <p><b>参数契约</b>：实现不校验入参，调用方必须保证参数合法（存在性 / 格式 / 业务约束）。
 * 方法入参默认非空；仅在可为 {@code null} 时标注 {@code @Nullable}（JSpecify）。
 * Command 对象属性遵循同一约定：未标注 {@code @Nullable} 的属性默认非空。
 */
public interface UserAuthService {

    /**
     * 修改密码（单步）。
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 发送手机验证码（两步验证第一步）。
     */
    void verifyMobile(VerifyMobileCommand command);

    /**
     * 核验并修改手机号（两步验证第二步）。
     */
    void changeMobile(ChangeMobileCommand command);

    /**
     * 发送邮箱验证码（两步验证第一步）。
     */
    void verifyEmail(VerifyEmailCommand command);

    /**
     * 核验并修改邮箱（两步验证第二步）。
     */
    void changeEmail(ChangeEmailCommand command);
}
