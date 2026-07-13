package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;

/**
 * 短信发送器 Gateway — 发送文本短信。
 * <p>
 * 通用契约，实现层对接具体短信通道（如阿里云 SMS）。
 *
 * @see Gateway
 */
public interface SmsSender extends Gateway {

    /**
     * 向指定手机号发送短信内容。
     */
    void send(Mobile to, SmsContent content);
}
