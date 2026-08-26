package com.soda.user.api;

import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.RequestChangeEmailCodeCommand;
import com.soda.user.api.command.RequestChangeMobileCodeCommand;

/**
 * 用户凭证相关的 ApplicationService 接口。
 * <p>
 * 管理手机号 / 邮箱 / 密码的修改。手机号 / 邮箱换绑走两步验证流程（发码与消费归位
 * User 侧，见 ADR-0026）：
 * <ol>
 *   <li>发送验证码 → {@link #requestChangeMobileCode(RequestChangeMobileCodeCommand)} /
 *       {@link #requestChangeEmailCode(RequestChangeEmailCodeCommand)}</li>
 *   <li>核验并修改 → {@link #changeMobile(ChangeMobileCommand)} / {@link #changeEmail(ChangeEmailCommand)}</li>
 * </ol>
 * 密码修改走单步流程 → {@link #changePassword(ChangePasswordCommand)}。
 * <p>
 * 实现在 {@code soda-user-application} 模块。
 *
 * <p>参数契约见 `docs/conventions/framework-crosscutting.md` §参数契约。
 */
public interface UserAuthService {

    /**
     * 请求发送换绑手机号验证码（两步验证第一步，scene=UCC、channel=S 由方法语义隐式）。
     */
    void requestChangeMobileCode(RequestChangeMobileCodeCommand command);

    /**
     * 请求发送换绑邮箱验证码（两步验证第一步，scene=UCC、channel=E 由方法语义隐式）。
     */
    void requestChangeEmailCode(RequestChangeEmailCodeCommand command);

    /**
     * 修改密码（单步，校验原密码）。
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 核验并修改手机号（两步验证第二步）。
     */
    void changeMobile(ChangeMobileCommand command);

    /**
     * 核验并修改邮箱（两步验证第二步）。
     */
    void changeEmail(ChangeEmailCommand command);
}
