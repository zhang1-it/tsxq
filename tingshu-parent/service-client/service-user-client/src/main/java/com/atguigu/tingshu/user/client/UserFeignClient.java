package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user", path = "api/user", fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {
    /**
     * 处理用户购买记录（虚拟物品发货）
     *
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);

    /**
     * 提供给专辑服务调用，获取当前用户已购声音集合
     *
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> getUserPaidTrackIdList(@PathVariable Long albumId);

    /**
     * 提供给订单服务调用，验证当前用户是否购买过专辑
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);

    @GetMapping("/userInfo/getUserInfoById/{id}")
    Result<UserInfo> getUserInfoById(@PathVariable Long id);

    /**
     * 获取用户声音列表付费情况
     * /api/user/userInfo/userIsPaidTrack/{userId}/{albumId}
     */
    @GetMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long userId,
                                                      @PathVariable Long albumId,
                                                      @RequestParam List<Long> needCheckTrackIdList);
    /**
     * 根据套餐ID查询套餐信息
     *
     * @param id
     * @return
     */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);
}
