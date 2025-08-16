package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {
    /**
     * 保存付款信息表
     * @param paymentType
     * @param orderNo
     * @param userId
     */
    PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId);

    /**
     * 修改付款状态为成功
     * @param transaction
     */
    void updatePaymentInfoSuccess(Transaction transaction);
}
