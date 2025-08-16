package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jodd.time.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    /**
     * 初始化用户账户
     *
     * @param userId
     */
    @Override
    public void saveUserAccount(Long userId) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(BigDecimal.valueOf(100));
        userAccount.setAvailableAmount(BigDecimal.valueOf(100));
        userAccount.setLockAmount(BigDecimal.valueOf(100));
        userAccountMapper.insert(userAccount);
        //2.新增账户变动历史（首次注册赠送100点）
        if (userAccount.getAvailableAmount().intValue() > 0) {
            this.saveUserAccountDetail(userId, "赠送", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, userAccount.getAvailableAmount(), null);
        }
    }
	@Override
    public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    /**
     * 扣减用户账户余额
     * @param accountLockVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndDeduct(AccountLockVo accountDeductVo) {
        //1.检查及扣减余额
        int count = userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(), accountDeductVo.getAmount());
        if (count == 0) {
            throw new GuiguException(400, "账户余额不足！");
        }
        //2.新增账户变动日志
        this.saveUserAccountDetail(
                accountDeductVo.getUserId(),
                accountDeductVo.getContent(),
                SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
                accountDeductVo.getAmount(),
                accountDeductVo.getOrderNo()
        );
    }

    /**
     * 获取用户账户可用余额
     *
     * @param userId
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        QueryWrapper<UserAccount> userAccountQueryWrapper = new QueryWrapper<>();
        userAccountQueryWrapper.select("available_amount");
        userAccountQueryWrapper.eq("user_id", userId);
        UserAccount userAccount = userAccountMapper.selectOne(userAccountQueryWrapper);
        return userAccount.getAvailableAmount();
    }

    /**
     * 分页查询当前用户充值记录
     * @param pageInfo
     * @param accountTradeTypeDeposit
     */
    @Override
    public void getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, String accountTradeTypeDeposit) {
        Long userId = AuthContextHolder.getUserId();
        LambdaQueryWrapper<UserAccountDetail> userAccountDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userAccountDetailLambdaQueryWrapper.eq(UserAccountDetail::getUserId, userId);
        userAccountDetailLambdaQueryWrapper.eq(UserAccountDetail::getTradeType, accountTradeTypeDeposit);
        userAccountDetailLambdaQueryWrapper.orderByDesc(UserAccountDetail::getId);
        Page<UserAccountDetail> userAccountDetailPage = userAccountDetailMapper.selectPage(pageInfo, userAccountDetailLambdaQueryWrapper);

    }


}


