package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;
	/**
	 * 获取用户订单分页列表
	 * api/order/orderInfo/findUserPage/{page}/{limit}
	 */
	@GetMapping("orderInfo/findUserPage/{page}/{limit}")
	@GuiGuLogin
	public Result<IPage<OrderInfo>> findUserPage(@PathVariable Long page, @PathVariable Long limit){
		Long userId = AuthContextHolder.getUserId();
		IPage<OrderInfo> iPage=new Page<>(page,limit);
		iPage = orderInfoService.findUserPage(iPage,userId);
		return Result.ok(iPage);
	}

	/**
	 * 根据订单号获取订单信息
	 * /api/order/orderInfo/getOrderInfo/{orderNo}
	 */
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	@GuiGuLogin
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
		//订单号是唯一的不同通过用户id进行获取
		OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
		return Result.ok(orderInfo);
	}

	/**
	 * 订单确认
	 * /api/order/orderInfo/trade
	 */
	@PostMapping("orderInfo/trade")
	@GuiGuLogin
	public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo){
		Long userId = AuthContextHolder.getUserId();
		OrderInfoVo orderInfoVo = orderInfoService.trade(userId,tradeVo);
		return Result.ok(orderInfoVo);
	}
	/**
	 * 提交订单
	 * /api/order/orderInfo/submitOrder
	 */
	@PostMapping("orderInfo/submitOrder")
	@GuiGuLogin
	public Result<Map<String,String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo){
		Long userId = AuthContextHolder.getUserId();
		Map<String,String> map = orderInfoService.submitOrder(userId,orderInfoVo);
		return Result.ok(map);
	}


}

