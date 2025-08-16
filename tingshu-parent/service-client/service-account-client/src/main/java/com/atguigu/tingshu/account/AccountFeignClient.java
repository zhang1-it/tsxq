package com.atguigu.tingshu.account;

import com.atguigu.tingshu.account.impl.AccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-account", path = "/api/account",fallback = AccountDegradeFeignClient.class)
public interface AccountFeignClient {
    /**
     * 根据订单号获取充值信息
     * /api/account/rechargeInfo/getRechargeInfo/{orderNo}
     */
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo);
    /**
     * 扣减用户账户余额
     */
    @PostMapping("/userAccount/checkAndDeduct")
    Result checkAndDeduct(@RequestBody AccountLockVo accountLockVo);
}
