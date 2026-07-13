package com.soda.user.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 跨服务消息消费者示例 — 监听 order 模块发布的消息，处理用户升级。
 * <p>
 * 消息类型 {@code UserFirstOrderMessage} 由 order 模块定义在其 api 包中，
 * 本模块依赖 order-api 后即可接收。
 *
 * @see UserFirstOrderMessage
 */
@Slf4j
@Component
public class UserUpgradeConsumer {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Async
    public void onFirstOrder(UserFirstOrderMessage message) {
        log.info("[onFirstOrder][userId={}, orderId={}]", message.userId(), message.orderId());
    }

    /**
     * 此处仅为演示消费者结构。正式接入时，此消息类型由订单模块的 api 提供：
     * {@code com.soda.order.api.message.UserFirstOrderMessage}
     */
    public record UserFirstOrderMessage(Long userId, Long orderId) {}
}
