package com.atguigu.tingshu.account.impl;


import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result<RechargeInfo> getRechargeInfo(String orderNo) {
        log.error("[账户降级]远程方法getRechargeInfo调用失败");
        return Result.fail();
    }

    @Override
    public Result checkAndDeduct(AccountLockVo accountLockVo) {
        log.error("[账户降级]远程方法checkAndDeduct调用失败");
        return Result.fail();
    }
}
