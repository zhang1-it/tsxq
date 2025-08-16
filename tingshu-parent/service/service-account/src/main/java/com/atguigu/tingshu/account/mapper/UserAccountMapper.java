package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    /**
     * 扣减用户账户余额
     * @param userId
     * @param amount
     * @return
     */
    int checkAndDeduct(Long userId, BigDecimal amount);

    /**
     * 新增余额
     * @param userId
     * @param rechargeAmount
     * @return
     */
    @Update("\n" +
            "update user_account set total_amount=total_amount+#{rechargeAmount},\n" +
            "                        available_amount=available_amount+#{rechargeAmount},\n" +
            "                        total_income_amount=total_income_amount+#{rechargeAmount},\n" +
            "                        update_time=now()\n" +
            "where user_id=#{userId}\n" +
            "and is_deleted=0")
    int add(@Param("userId") Long userId,@Param("rechargeAmount") BigDecimal rechargeAmount);
}
