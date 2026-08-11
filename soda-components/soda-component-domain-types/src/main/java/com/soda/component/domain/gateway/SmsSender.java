package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;

/**
 * 短信发送器 Gateway — 发送文本短信。
 * <p>
 * <b>投递契约</b>：实现必须保证投递成功后才返回——返回即视为已确认投递（内部可自行
 * 重试/补偿）；抛异常视为未投递。该契约支撑验证码投递的事务语义：投递侧监听器仅在
 * {@code send} 返回后把验证实体状态置为 PENDING（PENDING 蕴含已送达，见 ADR-0011）。
 * <p>
 * 通用契约，实现层对接具体短信通道（如阿里云 SMS）。
 *
 * @see Gateway
 */
public interface SmsSender extends Gateway {

    /**
     * 向指定手机号发送短信内容。
     * <p>
     * 返回即已确认投递（见类级投递契约）；抛异常表示未投递，调用方按未投递处理。
     */
    void send(Mobile to, SmsContent content);
}
