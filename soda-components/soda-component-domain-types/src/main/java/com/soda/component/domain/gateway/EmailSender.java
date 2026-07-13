package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.EmailContent;

/**
 * 邮件发送器 Gateway — 发送邮件。
 * <p>
 * 通用契约，实现层对接具体邮件通道（如阿里云邮件 / JavaMail）。
 *
 * @see Gateway
 */
public interface EmailSender extends Gateway {

    /**
     * 向指定邮箱发送邮件。
     */
    void send(Email to, EmailContent content);
}
