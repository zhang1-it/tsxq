package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        //1.构建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        //2.执行查询播放进度
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess != null) {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal("0.00");

    }

    /**
     * 更新当前用户收听声音播放进度
     * @param userId
     * @param userListenProcessVo
     */
    @Override
    public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
        //1.构建查询条件
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        //1.2 设置查询第一条记录(避免小程序暂停后恢复播放将积压更新进度请求并发发起，导致新增多条播放进度)
        query.limit(1);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess==null){
            userListenProcess = new UserListenProcess();
            userListenProcess.setUserId(userId);
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
        }else {
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
        }
        mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

        //4.采用Redis提供set k v nx ex 确保在规定时间内（24小时/当日内）播放进度统计更新1次
        String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + userId + ":" + userListenProcessVo.getTrackId();
        long ttl = DateUtil.endOfDay(new Date()).getTime() - System.currentTimeMillis();
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, userListenProcess.getTrackId(), ttl, TimeUnit.MILLISECONDS);
        //如过是用户今天第一次点击
        if (flag){
            //更新声音播放量
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(IdUtil.randomUUID());
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());//声音id
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());//专辑id
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);//统计类型
            trackStatMqVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
        }

    }

    /**
     * 获取当前用户上次播放专辑声音记录
     * @param userId
     * @return
     */
    @Override
    public Map<String, Long> getLatelyTrack(Long userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        query.limit(1);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess!=null) {
            Map<String, Long> map = new HashMap<>();
            map.put("albumId", userListenProcess.getAlbumId());
            map.put("trackId", userListenProcess.getTrackId());
            return map;
        }
        return null;
    }
}
