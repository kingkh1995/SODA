package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.Type;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;

/**
 * 投递端点多态 DP（见 ADR-0026）— 验证码的投递目标（channel + 地址）。
 * <p>
 * <b>泛型多态</b>：类型参数 {@code T extends StringLiteralType}（组件层字符串字面量契约，见 ADR-0028）
 * 由子类型以各自地址 DP 特化——{@code SmsRecipient} → {@link Mobile}、{@code EmailRecipient} → {@link Email}；
 * {@link #target()} 返回类型化地址（record 组件即 target，隐式访问器满足接口方法），投递侧直接
 * {@code sms.target()} 取 {@link Mobile}，基础设施可直接 {@code target().value()} 落裸串（免判别）。
 * <p>
 * <b>多属性输出</b>（channel + target，反序列化契约见 {@link #of(String, String)}）。
 * <p>
 * <b>形态差异</b>：AuthAccountId 是 {@link com.soda.component.domain.Identifier}（规范值即身份、存储于根），
 * 故用抽象基类；本 DP 无共享存储状态（channel 派生、target 为类型化组件），故用密封接口 + record——
 * 子类零样板且支持 record pattern 解构（{@code User#changeMobile}、{@code VerificationCreatedEventHandler}）。
 * <p>
 * <b>channel 只在 VerificationRecipient</b>——subject 不携带通道语义（见 ADR-0026）。
 * 消费：投递侧监听器按 recipient 类型匹配 sender（{@code SmsRecipient}→SmsSender、
 * {@code EmailRecipient}→EmailSender）；码形策略由调用方工厂按场景选择（见 {@link VerificationCodePolicy}）。
 *
 * @param <T> 类型化投递地址（{@link Mobile}/{@link Email}）
 * @see SmsRecipient
 * @see EmailRecipient
 */
public sealed interface VerificationRecipient<T extends StringLiteralType> extends Type
        permits SmsRecipient, EmailRecipient {

    /**
     * 反序列化（{@code @JsonCreator}）— JSON 对象 {@code {"channel","target"}} 按通道枚举分派：
     * {@code channel} 为通道名裸串（{@code "S"}/{@code "E"}），枚举解析收敛进工厂内部单点
     * （未知通道：{@link VerificationChannel#of} 抛 IAE 快速失败——脏数据/未来通道）。
     */
    @JsonCreator
    static VerificationRecipient<?> of(@JsonProperty("channel") String channel,
                                       @JsonProperty("target") String target) {
        return switch (VerificationChannel.of(channel)) {
            case S -> new SmsRecipient(Mobile.of(target));
            case E -> new EmailRecipient(Email.of(target));
        };
    }

    /**
     * 投递通道（{@code S}/{@code E}）——按子类型派生，构造无冗余参数。
     */
    @JsonProperty("channel")
    VerificationChannel channel();

    /**
     * 类型化投递地址——由子类型特化为 {@link Mobile} / {@link Email}。
     */
    T target();
}
