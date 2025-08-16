package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        log.error("[用户服务]提供远程调用方法savePaidRecord执行服务降级");
        return Result.fail();
    }

    @Override
    public Result<List<Long>> getUserPaidTrackIdList(Long albumId) {
        log.error("[用户服务]提供远程调用方法getUserPaidTrackIdList执行服务降级");
        return Result.fail();
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.error("[用户Feign服务]提供远程调用方法isPaidAlbum执行服务降级");
        return Result.fail();
    }

    @Override
    public Result<UserInfo> getUserInfoById(Long id) {
        return Result.fail();
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckTrackIdList) {
        log.error("[用户Feign服务] userIsPaidTrack失败 userId{}，albumId{}，needCheckTrackIdList{}",userId,albumId,needCheckTrackIdList);
        return Result.fail();
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long id) {
        log.error("[用户Feign服务] getVipServiceConfig失败 id{}",id);
        return Result.fail();
    }
}
