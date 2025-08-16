package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;
	@GuiGuLogin
	@GetMapping("/userAccount/findUserConsumePage/{page}/{limit}")
	public Result<Page<UserAccountDetail>> getUserConsumePage(@PathVariable int page, @PathVariable int limit) {
		Page<UserAccountDetail> pageInfo = new Page<>(page, limit);
		userAccountService.getUserAccountDetailPage(pageInfo, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
		return Result.ok(pageInfo);
	}

	/**
	 * 分页查询当前用户充值记录
	 *
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页查询当前用户充值记录")
	@GetMapping("/userAccount/findUserRechargePage/{page}/{limit}")
	public Result<Page<UserAccountDetail>> getUserRechargePage(@PathVariable int page, @PathVariable int limit) {
		Page<UserAccountDetail> pageInfo = new Page<>(page, limit);
		userAccountService.getUserAccountDetailPage(pageInfo, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
		return Result.ok(pageInfo);
	}
	/**
	 * 扣减用户账户余额
	 */
	@PostMapping("/userAccount/checkAndDeduct")
	@GuiGuLogin
	public Result checkAndDeduct(@RequestBody AccountLockVo accountLockVo){
		userAccountService.checkAndDeduct(accountLockVo);
		return Result.ok();
	}
	/**
	 * 获取用户账户可用余额
	 * /api/account/userAccount/getAvailableAmount
	 */
	@GetMapping("/userAccount/getAvailableAmount")
	@GuiGuLogin
	public Result<BigDecimal> getAvailableAmount(){
		Long userId = AuthContextHolder.getUserId();
		BigDecimal availableAmount =userAccountService.getAvailableAmount(userId);
		return Result.ok(availableAmount);
	}

}

