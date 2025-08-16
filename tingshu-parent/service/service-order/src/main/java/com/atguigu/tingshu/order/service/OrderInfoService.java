package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 订单确认
     *
     * @param userId
     * @param tradeVo
     * @return
     */
    OrderInfoVo trade(Long userId, TradeVo tradeVo);

    /**
     * 提交订单
     *
     * @param userId
     * @param orderInfoVo
     * @return
     */
    Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 根据订单号获取订单信息
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 获取用户订单分页列表
     * @param iPage
     * @param userId
     * @return
     */
    IPage<OrderInfo> findUserPage(IPage<OrderInfo> iPage, Long userId);

    /**
     * 查询订单状态，关闭订单
     * @param orderNo
     */
    void orderCanncal(String orderNo);

    /**
     * 修改订单支付成功
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
