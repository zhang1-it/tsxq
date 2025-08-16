package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.cache.GuiguCache;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.user.strategy.StrategyFactory;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    /**
     * 验证当前用户是否购买过专辑
     *
     * @param albumId
     * @return
     */
    @Override
    public Boolean isPaidAlbum(Long albumId) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.构建查询条件：用户ID+专辑ID 查询专辑购买记录表
        LambdaQueryWrapper<UserPaidAlbum> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidAlbum::getUserId, userId);
        queryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        Long count = userPaidAlbumMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 微信登录验证
     *
     * @param code
     * @return
     */
    @Override
    public Map<String, String> wxLogin(String code) {
        //首先进行微信验证
        WxMaJscode2SessionResult sessionInfo = null;
        Map<String, String> map = new HashMap<>();
        try {
            sessionInfo = wxMaService.getUserService().getSessionInfo(code);

            if (sessionInfo != null) {
                String openid = sessionInfo.getOpenid();
                //查询数据库是否有该用户
                LambdaQueryWrapper<UserInfo> uiQueryWrapper = new LambdaQueryWrapper<>();
                uiQueryWrapper.eq(UserInfo::getWxOpenId, openid);
                UserInfo userInfo = userInfoMapper.selectOne(uiQueryWrapper);
                //判断是否有该用户，没有则创建一个新用户
                if (userInfo == null) {
                    userInfo = new UserInfo();
                    userInfo.setWxOpenId(openid);
                    userInfo.setNickname("听友:"+ IdUtil.fastUUID());
                    userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                    userInfo.setIsVip(0);
                    userInfoMapper.insert(userInfo);
                    //初始化用户账户信息
                    kafkaService.sendMessage(KafkaConstant.QUEUE_ACCOUNT_MINUS, userInfo.getId().toString());
                }
                //生成token并返回
                String token=IdUtil.getSnowflakeNextIdStr();
                map.put("token",token);
                //把用户信息存入redis中
                String redisKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

                UserInfoVo userInfoVo = new UserInfoVo();
                BeanUtils.copyProperties(userInfo, userInfoVo);
                redisTemplate.opsForValue().set(redisKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
            }
            return map;
        } catch (WxErrorException e) {
            log.error("[用户服务]微信登录失败：{}", e);
            throw new RuntimeException(e);
        }


    }

    /**
     * 根据userId获取用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfoVo getUserInfoVo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        return userInfoVo;
    }

    /**
     * 获取登录用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        return userInfoVo;
    }

    /**
     * 更新用户信息
     *
     * @param httpServletRequest
     * @param userInfoVo
     */
    @Override
    public void updateUser(HttpServletRequest httpServletRequest, UserInfoVo userInfoVo) {
        //TODO:redis中数据和mysql数据库同步问题
        UserInfo userInfo = BeanUtil.copyProperties(userInfoVo, UserInfo.class);
        userInfoMapper.updateById(userInfo);
        UserInfoVo user = this.getUserInfo(userInfo.getId());
        String token = httpServletRequest.getHeader("token");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, user);
    }

    /**
     * 获取用户信息通过id
     * @param id
     * @return
     */
    @Override
    @GuiguCache(prefix = "userInfo:")
    public UserInfo getUserInfoById(Long id) {
        UserInfo userInfo = userInfoMapper.selectById(id);
        return userInfo;
    }

    /**
     * 获取用户声音列表付费情况
     * @param userId
     * @param albumId
     * @param needCheekTrackList
     * @return
     */
    @Override
    public Map<Long,Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckTrackIdList) {
        //1.检查用户是否购买专辑
        LambdaQueryWrapper<UserPaidAlbum> upaLambdaQueryWrapper = new LambdaQueryWrapper<>();
        upaLambdaQueryWrapper.eq(UserPaidAlbum::getUserId, userId);
        upaLambdaQueryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        Long userPaidAlbumCount = userPaidAlbumMapper.selectCount(upaLambdaQueryWrapper);
        //如果用户购买了专辑就不需要购买声音
        if (userPaidAlbumCount>0){
            return needCheckTrackIdList.stream().map(trackId -> {
                return Map.entry(trackId, 1);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        //查询声音列表
        LambdaQueryWrapper<UserPaidTrack> uptLambdaQueryWrapper = new LambdaQueryWrapper<>();
        uptLambdaQueryWrapper.eq(UserPaidTrack::getUserId, userId);
        uptLambdaQueryWrapper.in(UserPaidTrack::getTrackId, needCheckTrackIdList);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(uptLambdaQueryWrapper);
        //如果用户没有购买声音
        if (CollectionUtil.isEmpty(userPaidTrackList)){
            return needCheckTrackIdList.stream().map(trackId -> {
                return Map.entry(trackId, 0);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        //获取用户已付费声音列表
        List<Long> collect = userPaidTrackList.stream().map(userPaidTrack -> {
            return userPaidTrack.getTrackId();
        }).collect(Collectors.toList());
        return needCheckTrackIdList.stream().map(trackId -> {
            return Map.entry(trackId, collect.contains(trackId) ? 1 : 0);
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    /**
     * 提供给专辑服务调用，获取当前用户已购声音集合
     * @param userId
     * @param albumId
     * @return
     */
    @Override
    public List<Long> getUserPaidTrackIdList(Long userId, Long albumId) {
        //1.根据用户ID+专辑ID查询已购声音集合
        LambdaQueryWrapper<UserPaidTrack> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPaidTrack::getUserId, userId);
        queryWrapper.eq(UserPaidTrack::getAlbumId, albumId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(queryWrapper);
        if (CollectionUtil.isNotEmpty(userPaidTrackList)) {
            //2.获取已购声音ID集合
            List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
            return userPaidTrackIdList;
        }
        return null;
    }

    @Autowired
    private StrategyFactory strategyFactory;
    /**
     * 处理用户购买记录（虚拟物品发货）
     * @param userPaidRecordVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //使用策略模式优化代码
        ItemTypeStrategy strategy = strategyFactory.getStrategy(userPaidRecordVo.getItemType());
        strategy.savePaidRecord(userPaidRecordVo);


//        //1.判断购买项目类型-处理专辑
//        if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(userPaidRecordVo.getItemType())) {
//            //1.1 根据订单编号查询专辑购买记录
//            //防止fegin因为某种原因导致重复调用
//            LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo());
//            Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
//            if (count > 0) {
//                return;
//            }
//            //1.2 查询到专辑购买记录为空则新增购买记录
//            UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
//            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
//            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
//            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
//            userPaidAlbumMapper.insert(userPaidAlbum);
//        } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) {
//            //2.判断购买项目类型-处理声音
//            //2.1 根据订单编号查询声音购买记录
//            LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
//            Long count = userPaidTrackMapper.selectCount(userPaidTrackLambdaQueryWrapper);
//            if (count > 0) {
//                return;
//            }
//            //2.2 查询到声音购买记录为空则新增购买记录（循环批量新增）
//            //2.2.1 远程调用专辑服务-根据声音ID查询声音对象-获取声音所属专辑ID
//            TrackInfo trackInfo = albumFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0)).getData();
//            Long albumId = trackInfo.getAlbumId();
//            //2.2.2 遍历购买项目ID集合批量新增声音购买记录
//            userPaidRecordVo.getItemIdList().forEach(trackId -> {
//                UserPaidTrack userPaidTrack = new UserPaidTrack();
//                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
//                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
//                userPaidTrack.setAlbumId(albumId);
//                userPaidTrack.setTrackId(trackId);
//                userPaidTrackMapper.insert(userPaidTrack);
//            });
//        } else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(userPaidRecordVo.getItemType())) {
//            //3.判断购买项目类型-处理VIP会员-允许多次购买
//            //3.1 新增VIP购买记录
//            UserVipService userVipService = new UserVipService();
//            //3.1.1 根据VIP套餐ID查询套餐信息-得到VIP会员服务月数
//            Long vipConfigId = userPaidRecordVo.getItemIdList().get(0);
//            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipConfigId);
//            Integer serviceMonth = vipServiceConfig.getServiceMonth();
//            //3.1.2 获取用户身份，如果是VIP会员，则续费
//            UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
//            Integer isVip = userInfo.getIsVip();
//            if (isVip.intValue() == 1 && userInfo.getVipExpireTime().after(new Date())) {
//                //如果是VIP会员，则续费
//                userVipService.setStartTime(userInfo.getVipExpireTime());
//                //续费会员过期时间=现有会员过期时间+套餐服务月数
//                userVipService.setExpireTime(DateUtil.offsetMonth(userInfo.getVipExpireTime(), serviceMonth));
//            } else {
//                //3.1.3 获取用户身份，如果是普通用户，则新开
//                userVipService.setStartTime(new Date());
//                //续费会员过期时间=现有会员过期时间+套餐服务月数
//                userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
//            }
//            //3.1.4 构建VIP购买记录对象保存
//            userVipService.setUserId(userPaidRecordVo.getUserId());
//            userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
//            userVipServiceMapper.insert(userVipService);
//
//            //3.2 更新用户表中VIP状态及会员过期时间
//            userInfo.setIsVip(1);
//            userInfo.setVipExpireTime(userVipService.getExpireTime());
//            userInfoMapper.updateById(userInfo);
//        }
    }


}
