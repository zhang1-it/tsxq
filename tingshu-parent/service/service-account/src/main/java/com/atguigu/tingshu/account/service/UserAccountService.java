package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {

    /**
     * 初始化用户账户
     * @param userId
     */
    void saveUserAccount(Long userId);

    /**
     * 账户变动日志
     * @param userId
     * @param title
     * @param tradeType
     * @param amount
     * @param orderNo
     */
    void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);

    /**
     * 扣减用户账户余额
     * @param accountLockVo
     */
    void checkAndDeduct(AccountLockVo accountLockVo);

    /**
     * 获取用户账户可用余额
     *
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 分页查询当前用户充值记录
     * @param pageInfo
     * @param accountTradeTypeDeposit
     */
    void getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, String accountTradeTypeDeposit);
}
