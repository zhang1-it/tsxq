package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 验证当前用户是否购买过专辑
     * @param albumId
     * @return
     */
    Boolean isPaidAlbum(Long albumId);
    /**
     * 微信登录验证
     * @param code
     * @return
     */
    Map<String, String> wxLogin(String code);

    /**
     * 根据userId获取用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfoVo(Long userId);

    /**
     * 获取登录用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 更新用户信息
     *
     * @param httpServletRequest
     * @param userInfoVo
     */
    void updateUser(HttpServletRequest httpServletRequest, UserInfoVo userInfoVo);

    /**
     * 获取用户信息通过id
     * @param id
     * @return
     */
    UserInfo getUserInfoById(Long id);

    /**
     * 获取用户声音列表付费情况
     * @param userId
     * @param albumId
     * @param needCheckTrackIdList
     * @return
     */
    Map<Long,Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckTrackIdList);

    /**
     * 提供给专辑服务调用，获取当前用户已购声音集合
     * @param userId
     * @param albumId
     * @return
     */
    List<Long> getUserPaidTrackIdList(Long userId, Long albumId);

    /**
     * 处理用户购买记录（虚拟物品发货）
     * @param userPaidRecordVo
     */
    void savePaidRecord(UserPaidRecordVo userPaidRecordVo);
}
