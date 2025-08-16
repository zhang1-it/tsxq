package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author 张
 * @version 1.0
 */

@Component
@Slf4j
public class OrderReceiver {
    @Autowired
    private OrderInfoService orderInfoService;
    @KafkaListener(topics = KafkaConstant.QUEUE_ORDER_PAY_SUCCESS)
    public void orderPaySuccess(ConsumerRecord<String,String> record, Consumer consumer) {
        try {
            String orderNo = record.value();
            orderInfoService.orderPaySuccess(orderNo);
            //手动提交，防止消费者可能在消息处理完成前就提交了offset，如果此时消费者崩溃，消息会丢失。
            consumer.commitAsync();
            log.info("[订单服务]监听到订单支付成功消息：{}", orderNo);
        } catch (Exception e) {
            log.error("[订单服务]，监听订单支付成功消息失败：{}", e.getMessage());
            throw new GuiguException(400,"订单支付成功消息处理失败");
        }
    }
}
