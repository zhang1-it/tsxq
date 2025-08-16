package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author 张
 * @version 1.0
 */

@Component
@Slf4j
public class DelayMsgConsumer {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderInfoService orderInfoService;

    public void orderCancal(){
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
        ExecutorService threadFactory = Executors.newSingleThreadExecutor();

        threadFactory.submit(()->{
            while (true){
                String orderNo = null;
                try {
                    orderNo = blockingQueue.take();
                } catch (InterruptedException e) {
                    log.error("接收延迟消息失败，消息{}", orderNo);
                    e.printStackTrace();
                }
                if (StringUtils.isNotBlank(orderNo)) {
                    log.info("监听到延迟关单消息：{}", orderNo);
                    //查询订单状态，关闭订单
                    orderInfoService.orderCanncal(orderNo);
                }
            }
        });

    }
}
