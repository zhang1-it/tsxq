package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private UserAccountMapper userAccountMapper;
	@Autowired
	private UserAccountService userAccountService;
	/**
	 * 用户充值成功
	 * @param orderNo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void orderPaySuccess(String orderNo) {
		Boolean flag = redisTemplate.opsForValue().setIfAbsent("orderNo:" + orderNo, orderNo, 1, TimeUnit.HOURS);
		//防重
		if (flag){
            try {
                LambdaQueryWrapper<RechargeInfo> rechargeInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
                rechargeInfoLambdaQueryWrapper.eq(RechargeInfo::getOrderNo,orderNo);
                RechargeInfo rechargeInfo = this.getOne(rechargeInfoLambdaQueryWrapper);
                if (rechargeInfo != null && SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeInfo.getRechargeStatus())){
                    rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
                    this.updateById(rechargeInfo);
                    //修改账户信息
                    //2.新增余额
                    int count = userAccountMapper.add(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
                    if (count == 0) {
                        throw new GuiguException(400, "充值异常");
                    }
                    //3.新增账户变动日志
                    userAccountService.saveUserAccountDetail(
                            rechargeInfo.getUserId(),
                            "充值：" + rechargeInfo.getRechargeAmount(),
                            SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,
                            rechargeInfo.getRechargeAmount(),
                            orderNo
                    );
                }
            } catch (GuiguException e) {
				redisTemplate.delete(orderNo);
                throw new RuntimeException(e);
            }
        }
		}

		/**
         * 根据订单号获取充值信息
         * @param orderNo
         * @return
         */
	@Override
	public RechargeInfo getRechargeInfo(String orderNo) {
		LambdaQueryWrapper<RechargeInfo> riLambdaQueryWrapper = new LambdaQueryWrapper<>();
		riLambdaQueryWrapper.eq(RechargeInfo::getOrderNo, orderNo);
		return rechargeInfoMapper.selectOne(riLambdaQueryWrapper);
	}

	/**
	 * 充值余额
	 * @param rechargeInfoVo
	 * @return
	 */
	@Override
	public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
		Long userId = AuthContextHolder.getUserId();
		RechargeInfo rechargeInfo = new RechargeInfo();
		rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
		rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
		rechargeInfo.setUserId(userId);
		rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
		//1.1 生成本次充值记录订单编号：CZ+日期+雪花算法ID
		String orderNo = "CZ" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
		rechargeInfo.setOrderNo(orderNo);
		rechargeInfoMapper.insert(rechargeInfo);
		Map<String, String> mapResult = new HashMap<>();
		mapResult.put("orderNo", orderNo);
		return mapResult;
	}
}
