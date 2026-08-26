package com.soda.user.consumer;

import com.soda.user.consumer.message.UserFirstOrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 跨服务消息消费者示例 — 监听 order 模块发布的首单消息，处理用户升级。
 * <p>
 * 监听器在发布方事务提交后异步执行（AFTER_COMMIT）。
 *
 * @see UserFirstOrderMessage
 */
@Slf4j
@Component
public class UserUpgradeConsumer {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFirstOrder(UserFirstOrderMessage message) {
        log.info("[onFirstOrder][userId={}, orderId={}]", message.userId(), message.orderId());
    }
}
