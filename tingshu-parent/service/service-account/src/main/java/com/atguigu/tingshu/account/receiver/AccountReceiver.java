package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import io.micrometer.common.util.StringUtils;
import io.netty.util.internal.StringUtil;
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
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private RechargeInfoService rechargeInfoService;

    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_MINUS)
    public void userRegister(ConsumerRecord<String,String> consumerRecord){
        String value = consumerRecord.value();
        try {
            log.info("[账户服务]监听用户注册成功消息：{}", value);
            Long userId = Long.valueOf(value);
            userAccountService.saveUserAccount(userId);
        } catch (NumberFormatException e) {
            log.info("[账户服务]监听用户注册失败消息：{}", value);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS)
    public void rechargePaySuccess(ConsumerRecord<String,String> consumerRecord, Consumer consumer){
        String orderNo = consumerRecord.value();
        try {
            log.info("[账户服务]监听充值成功消息：{}", orderNo);
            rechargeInfoService.orderPaySuccess(orderNo);
        } catch (Exception e) {
            log.error("[账户服务]监听充值成功消息失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
