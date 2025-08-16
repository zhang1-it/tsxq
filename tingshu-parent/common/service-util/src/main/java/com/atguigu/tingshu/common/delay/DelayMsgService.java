package com.atguigu.tingshu.common.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 张
 * @version 1.0
 */

@Component
@Slf4j
public class DelayMsgService {
    @Autowired
    private RedissonClient redissonClient;
    public void sendDelayMessage(String queueName,String msg,Long delayTime){
        try {
            RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
            delayedQueue.offer(msg,delayTime, TimeUnit.SECONDS);
            log.info("发送延迟消息成功{}",msg);
        } catch (Exception e) {
            log.error("发送延迟消息失败{}",msg);
            throw new RuntimeException(e);
        }
    }
}
