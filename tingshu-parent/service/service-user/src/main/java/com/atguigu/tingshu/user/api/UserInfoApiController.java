package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	/**
	 * 处理用户购买记录（虚拟物品发货）
	 * @param userPaidRecordVo
	 * @return
	 */
	@PostMapping("/userInfo/savePaidRecord")
	public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo){
		userInfoService.savePaidRecord(userPaidRecordVo);
		return Result.ok();
	}
	/**
	 * 提供给专辑服务调用，获取当前用户已购声音集合
	 *
	 * @param albumId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提供给专辑服务调用，获取当前用户已购声音集合")
	@GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
	public Result<List<Long>> getUserPaidTrackIdList(@PathVariable Long albumId) {
		Long userId = AuthContextHolder.getUserId();
		List<Long> userPaidTrackIdList = userInfoService.getUserPaidTrackIdList(userId, albumId);
		return Result.ok(userPaidTrackIdList);
	}

	/**
	 * 提供给订单服务调用，验证当前用户是否购买过专辑
	 * @param albumId
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "提供给订单服务调用，验证当前用户是否购买过专辑")
	@GetMapping("/userInfo/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId){
		Boolean isBuy = userInfoService.isPaidAlbum(albumId);
		return Result.ok(isBuy);
	}
	/**
	 * 获取用户声音列表付费情况
	 * /api/user/userInfo/userIsPaidTrack/{userId}/{albumId}
	 */
	@GetMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long,Integer>> userIsPaidTrack(@PathVariable Long userId,
										   @PathVariable Long albumId,
										   @RequestParam List<Long> needCheckTrackIdList){
		Map<Long,Integer> trackList = userInfoService.userIsPaidTrack(userId, albumId,needCheckTrackIdList);
		return Result.ok(trackList);
	}

	/**
	 * 获取用户信息通过id
	 */
	@GetMapping("/userInfo/getUserInfoById/{id}")
	Result<UserInfo> getUserInfoById(@PathVariable Long id){
		UserInfo userInfo = userInfoService.getUserInfoById(id);
		return Result.ok(userInfo);
	}
	/**
	 * 更新用户信息
	 * /api/user/wxLogin/updateUser
	 */
	@PostMapping("/wxLogin/updateUser")
	@GuiGuLogin
	public Result updateUser(@RequestBody UserInfoVo userInfoVo, HttpServletRequest httpServletRequest){
		Long userId = AuthContextHolder.getUserId();
		userInfoVo.setId(userId);
		userInfoService.updateUser(httpServletRequest, userInfoVo);
		return Result.ok();
	}


	/**
	 * 根据userId获取用户信息
	 * /api/user/userInfo/getUserInfoVo/{userId}
	 */
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	@GuiGuLogin
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
		UserInfoVo userInfoVo = userInfoService.getUserInfoVo(userId);
		return Result.ok(userInfoVo);
	}
}

