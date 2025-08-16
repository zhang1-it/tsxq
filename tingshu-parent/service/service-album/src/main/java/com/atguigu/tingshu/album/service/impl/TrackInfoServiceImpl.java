package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;
	@Autowired
	private AlbumInfoMapper albumInfoMapper;
	@Autowired
	private VodService vodService;
	@Autowired
	private TrackStatMapper trackStatMapper;
	@Autowired
	private AlbumInfoService albumInfoService;
	@Autowired
	private UserFeignClient userFeignClient;
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private AlbumStatMapper albumStatMapper;
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo) {
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		trackInfo.setUserId(userId);
		//设置声音在专辑中的排序
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount()+1);
		//设置声音的时长，文件大小
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfoVo(trackInfo.getMediaFileId());
		trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
		trackInfo.setMediaSize(trackMediaInfoVo.getSize());
		trackInfo.setMediaType(trackMediaInfoVo.getType());
		//设置声音来源
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_UPLOAD);
		//TODO:这里在实际开发中应该等到后台管理审核
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
		//保存声音
		trackInfoMapper.insert(trackInfo);
		//修改专辑信息，把专辑信息中的声音数量加1
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()+1);
		albumInfoMapper.updateById(albumInfo);
		//修改声音统计表
		this.updateTrackStat(trackInfo.getId(),SystemConstant.TRACK_STAT_PLAY,0);
		this.updateTrackStat(trackInfo.getId(),SystemConstant.TRACK_STAT_COLLECT,0);
		this.updateTrackStat(trackInfo.getId(),SystemConstant.TRACK_STAT_PRAISE,0);
		this.updateTrackStat(trackInfo.getId(),SystemConstant.TRACK_STAT_COMMENT,0);
	}

	@Override
	public void updateTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		trackStat.setStatNum(statNum);
		trackStatMapper.insert(trackStat);
	}

	/**
	 *  获取当前登录声音分页列表
	 * @param pageInfo
	 * @param trackInfoQuery
	 * @return
	 */
	@Override
	public Page<TrackListVo> getUserTrackByPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.selectUserTrackByPage(pageInfo,trackInfoQuery);
	}

	/**
	 * 修改用户专辑声音信息
	 * @param id
	 * @param trackInfoVo
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		String beforMediaFileId = trackInfo.getMediaFileId();
		String afterMediaFileId = trackInfoVo.getMediaFileId();
		//判断有没有修改声音
		if (!StringUtils.equals(beforMediaFileId,afterMediaFileId)){
			//删除云点播中旧的声音
			vodService.removeBeforMedia(beforMediaFileId);
			//获取云点播中新的声音时长，大小，类型
			TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfoVo(trackInfoVo.getMediaFileId());
			if (trackMediaInfoVo != null){
				trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
				trackInfo.setMediaSize(trackMediaInfoVo.getSize());
				trackInfo.setMediaType(trackMediaInfoVo.getType());
			}
		}
		//修改声音信息
		BeanUtil.copyProperties(trackInfoVo,trackInfo);
		trackInfoMapper.updateById(trackInfo);

	}

	/**
	 * 根据id删除声音信息
	 * @param id
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		//删除声音信息
		trackInfoMapper.deleteById(id);
		//修改同专辑下的声音排序
		trackInfoMapper.updateOrderNumDecrease(trackInfo.getAlbumId(),trackInfo.getOrderNum());
		//修改专辑包含声音总数信息
		AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount()-1);
		albumInfoMapper.updateById(albumInfo);
		//删除声音统计信息
		LambdaUpdateWrapper<TrackStat> tsUpdateWrapper = new LambdaUpdateWrapper<>();
		tsUpdateWrapper.eq(TrackStat::getTrackId,id);
		trackStatMapper.delete(tsUpdateWrapper);
		//删除云点播中的声音
		vodService.removeBeforMedia(trackInfo.getMediaFileId());
	}

	/**
	 * 查询专辑下声音列表分页集合
	 * @param pageInfo
	 * @param albumId
	 * @return
	 */
	@Override
	public Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId,Long userId) {
		Page<AlbumTrackListVo> page = trackInfoMapper.selectAlbumTrackPage(pageInfo,albumId);
		AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
		Assert.notNull(albumInfo,"[专辑声音]专辑不存在{}",albumId);
		String payType = albumInfo.getPayType();
		Integer secondsForFree = albumInfo.getTracksForFree();
		//如果用户未登录
		if (userId==null){
			//如果该专辑是收费的
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE) || payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)){
				List<AlbumTrackListVo> records = page.getRecords();
				records.stream().filter(new Predicate<AlbumTrackListVo>() {
					@Override
					public boolean test(AlbumTrackListVo albumTrackListVo) {
						return albumTrackListVo.getOrderNum()>secondsForFree;
					}
				}).forEach(albumTrackListVo -> albumTrackListVo.setIsShowPaidMark(true));
			}
		}
		else {
			//获取用户信息
			UserInfo userInfo = userFeignClient.getUserInfoById(userId).getData();
			Assert.notNull(userInfo,"[专辑声音]用户id不存在:{}",userId);
			Integer isVip = userInfo.getIsVip();
			Date vipExpireTime = userInfo.getVipExpireTime();
			//是否需要检查是购买了多少声音
			boolean needCheckPaidTrack=false;
			//如果该专辑是vip免费
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE)){
				//如果用户不是vip
				if (isVip==0){
					needCheckPaidTrack=true;
				}
				//如果vip过期了的话
				if (isVip==1 && new Date().after(vipExpireTime)){
					needCheckPaidTrack=true;
				}
			}

			//如果该专辑是收费的
			if (payType.equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)){
				needCheckPaidTrack=true;
			}
			//检查声音付费情况
			if (needCheckPaidTrack){
				//获取用户需要购买的声音集合
				List<AlbumTrackListVo> albumTrackList= page.getRecords().stream().filter(albumTrackListVo -> {
					return albumTrackListVo.getOrderNum() > secondsForFree;
				}).collect(Collectors.toList());
				//获取用户需要购买的声音id集合
				List<Long> albumTrackIdList = albumTrackList.stream().map(albumTrackListVo ->
						albumTrackListVo.getTrackId()).collect(Collectors.toList());

				Map<Long, Integer> userPaidTrackMap = userFeignClient.userIsPaidTrack(userId, albumId, albumTrackIdList).getData();
				for (AlbumTrackListVo albumTrackListVo : albumTrackList) {
					if (userPaidTrackMap.get(albumTrackListVo.getTrackId())==0){
						albumTrackListVo.setIsShowPaidMark(true);
					}
				}

			}


		}
		return page;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateTrackStat(TrackStatMqVo trackStatMqVo) {
		String key= "mq:"+trackStatMqVo.getBusinessNo();
        try {
            //这里的过期时间要设置为Kafka重试结束的时间
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, trackStatMqVo.getBusinessNo(), 1, TimeUnit.HOURS);
            //redisTemplate.boundValueOps().setIfAbsent()  redis的第二种使用方式
            if (!flag){
                return;
            }
            //2.更新声音统计信息
            trackStatMapper.updateStat(trackStatMqVo.getTrackId(), trackStatMqVo.getStatType(), trackStatMqVo.getCount());

            //3.更新专辑统计信息(播放量、评论量只要声音+1，对应专辑也得+1)
            if (SystemConstant.TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())) {
                albumStatMapper.updateStat(trackStatMqVo.getAlbumId(), SystemConstant.ALBUM_STAT_PLAY, trackStatMqVo.getCount());
            }
            if (SystemConstant.TRACK_STAT_COMMENT.equals(trackStatMqVo.getStatType())) {
                albumStatMapper.updateStat(trackStatMqVo.getAlbumId(), SystemConstant.ALBUM_STAT_COMMENT, trackStatMqVo.getCount());
            }
        } catch (Exception e) {
			//如果更新数据库发送异常，事务会进行回滚，下次再次投递消息允许继续处理统一个消息
			redisTemplate.delete(key);
            throw new RuntimeException(e);
        }
    }

	/**
	 * 根据声音ID，获取声音统计信息
	 * @param trackId
	 * @return
	 */
	@Override
	public TrackStatVo getTrackStatVo(Long trackId) {
		return trackInfoMapper.selectTrackStatVo(trackId);
	}

	@Override
	public List<Map<String, Object>> getUserWaitBuyTrackPayList(Long trackId) {
		//1.根据声音ID查询声音对象-得到专辑ID跟声音序号
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
		//2.根据专辑ID+当前声音序号查询大于当前声音待购买声音列表
		LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
		queryWrapper.ge(TrackInfo::getOrderNum, trackInfo.getOrderNum());

		List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(queryWrapper);
		if (CollectionUtil.isEmpty(waitBuyTrackList)) {
			throw new GuiguException(400, "该专辑下没有符合购买要求声音");
		}
		//3.远程调用"用户服务"获取用户已购买声音ID集合
		List<Long> userPaidTrackIdList = userFeignClient.getUserPaidTrackIdList(trackInfo.getAlbumId()).getData();

		//4.将待购买声音列表中用户已购买声音排除掉-得到实际代购买声音列表
		if (CollectionUtil.isNotEmpty(userPaidTrackIdList)) {
			waitBuyTrackList = waitBuyTrackList.stream()
					.filter(waitTrackInfo -> !userPaidTrackIdList.contains(waitTrackInfo.getId())) //排除掉已购声音ID
					.collect(Collectors.toList());
		}
		//5.基于实际购买声音列表长度，动态构建分集购买对象
		List<Map<String, Object>> mapList = new ArrayList<>();
		if (CollectionUtil.isNotEmpty(waitBuyTrackList)) {
			//5.1 根据专辑ID查询专辑得到单集价格
			AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
			BigDecimal price = albumInfo.getPrice();
			//5.1 构建本集购买对象
			Map<String, Object> currMap = new HashMap<>();
			currMap.put("name", "本集");
			currMap.put("price", price);
			currMap.put("trackCount", 1);
			mapList.add(currMap);
			//5.2 判断待购买声音数量 数量<10 动态展示后count集合 价格=count*price 数量=count
			int count = waitBuyTrackList.size();

			//5.3 数量>=10 固定显示后10集 价格=10*price 数量=10
			//if (count >= 10) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后10集");
			//    map.put("price", price.multiply(new BigDecimal("10")));
			//    map.put("trackCount", 10);
			//    mapList.add(map);
			//}
			////5.3 数量>10 and 数量<20 动态展示：后count集合（全集） 价格=count*price 数量=count  相当于全集
			//if (count > 10 && count < 20) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后"+count+"集(全集)");
			//    map.put("price", price.multiply(new BigDecimal(count)));
			//    map.put("trackCount", count);
			//    mapList.add(map);
			//}
			//
			////5.4 数量>=20 固定显示后20集 价格=20*price 数量=20
			//if (count >= 20) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后20集");
			//    map.put("price", price.multiply(new BigDecimal("20")));
			//    map.put("trackCount", 20);
			//    mapList.add(map);
			//}
			////5.4 数量>20 and 数量<30 动态展示：后count集合（全集） 价格=count*price 数量=count  相当于全集
			//if (count > 20 && count < 30) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后"+count+"集(全集)");
			//    map.put("price", price.multiply(new BigDecimal(count)));
			//    map.put("trackCount", count);
			//    mapList.add(map);
			//}
			////5.5 数量>=30 固定显示后30集 价格=30*price 数量=30
			//if (count >= 30) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后30集");
			//    map.put("price", price.multiply(new BigDecimal("30")));
			//    map.put("trackCount", 30);
			//    mapList.add(map);
			//}
			////5.5 数量>30 and 数量<40 动态展示：后count集合（全集） 价格=count*price 数量=count  相当于全集
			//if (count > 30 && count < 40) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后"+count+"集(全集)");
			//    map.put("price", price.multiply(new BigDecimal(count)));
			//    map.put("trackCount", count);
			//    mapList.add(map);
			//}
			//
			////5.5 数量>=40 固定显示后40集 价格=40*price 数量=40
			//if (count >= 40) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后40集");
			//    map.put("price", price.multiply(new BigDecimal("40")));
			//    map.put("trackCount", 40);
			//    mapList.add(map);
			//}
			////5.5 数量>40 and 数量<50 动态展示：后count集合（全集） 价格=count*price 数量=count  相当于全集
			//if (count > 40 && count < 50) {
			//    Map<String, Object> map = new HashMap<>();
			//    map.put("name", "后"+count+"集(全集)");
			//    map.put("price", price.multiply(new BigDecimal(count)));
			//    map.put("trackCount", count);
			//    mapList.add(map);
			//}

			// 18
			for (int i = 10; i <= 50; i += 10) {
				//判断数量>i 固定显示后i集
				if (count > i) {
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + i + "集");
					map.put("price", price.multiply(new BigDecimal(i)));
					map.put("trackCount", i);
					mapList.add(map);
				} else {
					//反之全集（动态构建后count集合）
					Map<String, Object> map = new HashMap<>();
					map.put("name", "后" + count + "集");
					map.put("price", price.multiply(new BigDecimal(count)));
					map.put("trackCount", count);
					mapList.add(map);
					break;
				}
			}
		}
		return mapList;
	}

	/**
	 * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
	 * @param userId
	 * @param trackId
	 * @param trackCount
	 * @return
	 */
	@Override
	public List<TrackInfo> getWaitBuyTrackInfoList(Long userId, Long trackId, int trackCount) {
		//1.根据声音ID查询声音对象-得到专辑ID跟声音序号
		TrackInfo trackInfo = trackInfoMapper.selectById(trackId);

		//2.远程调用"用户服务"获取用户已购买声音ID集合
		List<Long> userPaidTrackIdList = userFeignClient.getUserPaidTrackIdList(trackInfo.getAlbumId()).getData();

		//3.根据专辑ID+当前声音序号查询大于当前声音待购买声音列表
		LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
		queryWrapper.ge(TrackInfo::getOrderNum, trackInfo.getOrderNum());
		//3.1 去掉已购买过声音
		if(CollectionUtil.isNotEmpty(userPaidTrackIdList)){
			queryWrapper.notIn(TrackInfo::getId, userPaidTrackIdList);
		}
		//3.2 限制购买数量(用户选择购买数量)
		queryWrapper.last("limit "+trackCount);
		//3.3 只查询指定列：封面图片、声音名称、声音ID、所属专辑ID
		queryWrapper.select(TrackInfo::getId, TrackInfo::getTrackTitle, TrackInfo::getCoverUrl, TrackInfo::getAlbumId);
		//3.4 对声音进行排序：按照序号升序
		queryWrapper.orderByAsc(TrackInfo::getOrderNum);
		List<TrackInfo> waitBuyTrackList = trackInfoMapper.selectList(queryWrapper);
		if (CollectionUtil.isEmpty(waitBuyTrackList)) {
			throw new GuiguException(400, "该专辑下没有符合购买要求声音");
		}
		return waitBuyTrackList;
	}
}
