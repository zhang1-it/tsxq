package com.atguigu.tingshu.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {
    /**
     * 微信确认下单
     * @param paymentType
     * @param orderNo
     * @return
     */
    Map<String, String> createJsapi(String paymentType, String orderNo);

    /**
     * 小程序轮询查询支付结果-根据商户订单编号查询交易状态
     * @param orderNo
     * @return
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 微信异步回调
     * @param request
     * @return
     */
    Map<String, String> wxNotify(HttpServletRequest request);
}
