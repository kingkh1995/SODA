package com.soda.user.consumer;

import com.soda.user.consumer.message.UserFirstOrderMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 跨服务消息消费者示例 — 同步监听 order 模块发布的首单消息，处理用户升级。
 *
 * <p>普通 {@code @EventListener} 在发布方调用栈内同步执行；如发布方有事务，listener 共享该事务上下文。
 *
 * @see UserFirstOrderMessage
 */
@Slf4j
@Component
public class UserUpgradeConsumer {

    @EventListener
    public void onFirstOrder(UserFirstOrderMessage message) {
        log.info("[onFirstOrder][userId={}, orderId={}]", message.userId(), message.orderId());
    }
}
