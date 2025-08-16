package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;
	/**
	 * 充值余额
	 * /api/account/rechargeInfo/submitRecharge
	 */
	@PostMapping("/rechargeInfo/submitRecharge")
	@GuiGuLogin
	public Result<Map<String,String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo){
		Map<String,String> resultMap = rechargeInfoService.submitRecharge(rechargeInfoVo);
		return Result.ok(resultMap);
	}
	/**
	 * 根据订单号获取充值信息
	 * /api/account/rechargeInfo/getRechargeInfo/{orderNo}
	 */
	@GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
	@GuiGuLogin
	public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo) {
		RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
		return Result.ok(rechargeInfo);
	}
}

