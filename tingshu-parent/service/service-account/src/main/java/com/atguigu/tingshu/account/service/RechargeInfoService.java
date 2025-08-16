package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {
    /**
     * 根据订单号获取充值信息
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);

    /**
     * 充值余额
     * @param rechargeInfoVo
     * @return
     */
    Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo);

    /**
     * 用户充值成功
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
