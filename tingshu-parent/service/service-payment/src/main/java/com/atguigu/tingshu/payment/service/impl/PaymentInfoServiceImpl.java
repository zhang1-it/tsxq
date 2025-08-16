package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private KafkaService kafkaService;
    /**
     * 保存付款信息表
     * @param paymentType
     * @param orderNo
     * @param userId
     */
    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo, Long userId) {
        //防止重复保存
        LambdaQueryWrapper<PaymentInfo> piLambdaQueryWrapper = new LambdaQueryWrapper<>();
        piLambdaQueryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(piLambdaQueryWrapper);
        if (paymentInfo!=null){
            return paymentInfo;
        }
        //保存付款信息
        paymentInfo = new PaymentInfo();
        paymentInfo.setUserId(userId);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setOutTradeNo(orderNo);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            //支付订单
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull(orderInfo,"订单不存在，订单编号{}",orderInfo);
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        }
        else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)){
            //充值
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo,"充值信息不存在，充值订单编号{}",orderNo);
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent("充值："+rechargeInfo.getRechargeAmount());
        }
        paymentInfoMapper.insert(paymentInfo);
        return paymentInfo;
    }

    /**
     * 修改付款状态为成功
     * @param transaction
     */
    @Override
    @Transactional
    public void updatePaymentInfoSuccess(Transaction transaction) {
        String outTradeNo = transaction.getOutTradeNo();
        //防止重复修改
        LambdaQueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoQueryWrapper.eq(PaymentInfo::getOrderNo, outTradeNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (!SystemConstant.PAYMENT_STATUS_UNPAID.equals(paymentInfo.getPaymentStatus())){
            return;
        }
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        paymentInfo.setCallbackContent(transaction.toString());
        paymentInfo.setCallbackTime(new Date());
        paymentInfoMapper.updateById(paymentInfo);
        //判断支付类型
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentInfo.getPaymentType())){
            //订单支付
            kafkaService.sendMessage(KafkaConstant.QUEUE_ORDER_PAY_SUCCESS, paymentInfo.getOrderNo());
        }
        else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentInfo.getPaymentType())){
            //充值
            kafkaService.sendMessage(KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS, paymentInfo.getOrderNo());
        }
    }
}
