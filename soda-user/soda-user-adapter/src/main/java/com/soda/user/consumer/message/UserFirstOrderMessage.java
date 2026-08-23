package com.soda.user.consumer.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 此处仅为演示消费者结构。正式接入时，此消息类型由订单模块的 api 提供：
 * {@code com.soda.order.api.message.UserFirstOrderMessage}
 */
public record UserFirstOrderMessage(
        @JsonProperty("userId") Long userId,
        @JsonProperty("orderId") Long orderId) {
}
