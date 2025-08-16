package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderDegradeFeignClient implements OrderFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {
        log.error("[订单模块]远程feign调用getOrderInfo失败");
        return Result.fail();
    }
}
