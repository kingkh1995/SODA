package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.Type;

/**
 * 投递端点多态 DP（2026-08-16，见 ADR-0026）— 验证码的投递目标（channel + 地址）。
 * <p>
 * <b>泛型多态</b>：类型参数 {@code T extends StringLiteralType}（组件层字符串字面量契约，见 ADR-0028）
 * 由子类型以各自地址 DP 特化——{@code SmsRecipient} → {@code Mobile}、{@code EmailRecipient} → {@code Email}；
 * {@link #target()} 返回类型化地址（record 组件即 target，隐式访问器满足接口方法），投递侧直接
 * {@code sms.target()} 取 {@code Mobile}，基础设施可直接 {@code target().value()} 落裸串（免判别）。
 * <p>
 * <b>多属性输出</b>：JSON 对象形式（{@code {"channel":"S","target":"13800138000"}}——{@code channel()}
 * 根访问器 {@code @JsonProperty}、组件 target 序列化为地址裸串），反序列化经双参
 * {@link #of(String, String)} 工厂（2026-08-16 修订三/四/五：弃规范字符串/前缀路由，
 * 见 ADR-0026 注记）。
 * <p>
 * <b>形态差异</b>：AuthAccountId 是 {@link com.soda.component.domain.Identifier}（规范值即身份、存储于根），
 * 故用抽象基类；本 DP 无共享存储状态（channel 派生、target 为类型化组件），故用密封接口 + record——
 * 子类零样板且支持 record pattern 解构（{@code User#changeMobile}、{@code VerificationCreatedEventHandler}）。
 * <p>
 * <b>channel 只在 VerificationRecipient</b>——subject 不携带通道语义（判别源从 subject 前缀迁至
 * recipient，见 ADR-0026）。
 * 消费：投递侧监听器按 recipient 类型匹配 sender（{@code SmsRecipient}→SmsSender、
 * {@code EmailRecipient}→EmailSender）；码形策略经 {@link #channel()} 由调用方工厂选择
 * （见 {@link VerificationCodePolicy}）。
 *
 * @param <T> 类型化投递地址（{@code Mobile}/{@code Email}）
 * @see SmsRecipient
 * @see EmailRecipient
 */
public sealed interface VerificationRecipient<T extends StringLiteralType> extends Type
        permits SmsRecipient, EmailRecipient {

    /**
     * 反序列化（{@code @JsonCreator}）— JSON 对象 {@code {"channel","target"}} 按通道枚举分派：
     * {@code channel} 为通道名裸串（{@code "S"}/{@code "E"}），枚举解析收敛进工厂内部单点
     * （未知通道：{@code VerificationChannel.of} 抛 IAE 快速失败——脏数据/未来通道）。
     */
    @JsonCreator
    static VerificationRecipient<?> of(@JsonProperty("channel") String channel,
                                       @JsonProperty("target") String target) {
        return switch (VerificationChannel.of(channel)) {
            case S -> SmsRecipient.of(target);
            case E -> EmailRecipient.of(target);
        };
    }

    /**
     * 投递通道（{@code S}/{@code E}）——按子类型派生，构造无冗余参数。
     */
    @JsonProperty("channel")
    VerificationChannel channel();

    /**
     * 类型化投递地址（泛型多态）——子类型以各自地址 DP 特化：{@code SmsRecipient.target()} = {@code Mobile}、
     * {@code EmailRecipient.target()} = {@code Email}；record 组件即 target，隐式访问器满足本方法。
     */
    T target();
}
