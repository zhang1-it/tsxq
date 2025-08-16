package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.cache.GuiguCache;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;
    @Autowired
    private AlbumStatMapper albumStatMapper;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class) //默认事务注解修饰方法捕获运行时异常或者Error
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo) {
        //1.保存专辑
        //1.1 将提交专辑VO拷贝到专辑PO对象中 仅限同名同类型属性才会被拷贝
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);

        //1.2 手动为用户id、包含声音数量（0）、是否完结、免费试听集数、审核状态（TODO：审核通过，实则有内容审核机制）
        albumInfo.setUserId(AuthContextHolder.getUserId());
        albumInfo.setIncludeTrackCount(0);
        albumInfo.setIsFinished("0");
        albumInfo.setTracksForFree(5);
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //1.3 保存专辑，得到专辑ID
        albumInfoMapper.insert(albumInfo);
        Long albumId = albumInfo.getId();

        //2.保存专辑标签关系
        //2.1 将提交专辑标签关系VO集合转为PO集合
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (CollectionUtil.isNotEmpty(albumAttributeValueVoList)) {
            //2.2 关联专辑ID批量保存专辑标签关系
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
                AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
                albumAttributeValue.setAlbumId(albumId);
                albumAttributeValueMapper.insert(albumAttributeValue);
            }

        }
        //3.保存专辑统计信息（4条）
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_PLAY, 0);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_SUBSCRIBE, 0);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_BUY, 0);
        this.saveAlbumStat(albumId, SystemConstant.ALBUM_STAT_COMMENT, 0);

        if ("1".equals(albumInfo.getIsOpen())){
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, albumId.toString());
        }


    }

    /**
     * 查询用户专辑分页列表
     *
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    @Override
    @GuiguCache(prefix = "userAlbumPage:")
    public Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        albumInfoQuery.setUserId(AuthContextHolder.getUserId());
        return albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    /**
     * 根据ID删除专辑信息
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long id) {
        //1.首先查看专辑下是否有声音
        AlbumInfo albumInfo = albumInfoMapper.selectById(id);
        if (albumInfo==null || albumInfo.getIncludeTrackCount()>0){
           throw new GuiguException(400,"专辑下有声音，请谨慎删除");
        }
        //2.删除专辑
        albumInfoMapper.deleteById(id);
        //3.删除专辑标签关系
        LambdaUpdateWrapper<AlbumAttributeValue> albumAttributeValueLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        albumAttributeValueLambdaUpdateWrapper.eq(AlbumAttributeValue::getAlbumId,id);
        albumAttributeValueMapper.delete(albumAttributeValueLambdaUpdateWrapper);
        //4.删除专辑统计信息
        LambdaUpdateWrapper<AlbumStat> albumStatLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        albumStatLambdaUpdateWrapper.eq(AlbumStat::getAlbumId,id);
        albumStatMapper.delete(albumStatLambdaUpdateWrapper);
        //TODO:这里可以删除minio中的图片信息

        kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, id.toString());

    }

    /**
     * 根据id查询专辑信息
     * @param id
     * @return
     */
    @Override
    @GuiguCache(prefix = "albumInfo:")
    public AlbumInfo getAlbumInfo(Long id) {
        AlbumInfo albumInfo = albumInfoMapper.selectById(id);

        if (albumInfo!= null) {
            LambdaQueryWrapper<AlbumAttributeValue> aavWrapper = new LambdaQueryWrapper<>();
            aavWrapper.eq(AlbumAttributeValue::getAlbumId,id);
            List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper.selectList(aavWrapper);
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
        }
        return albumInfo;
    }

    /**
     * 修改专辑信息
     * @param id
     * @param albumInfoVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
        AlbumInfo albumInfo = new AlbumInfo();
        albumInfo.setId(id);
        BeanUtil.copyProperties(albumInfoVo,albumInfo);
        albumInfoMapper.updateById(albumInfo);
        LambdaUpdateWrapper<AlbumAttributeValue> aavWrapper = new LambdaUpdateWrapper<>();
        aavWrapper.eq(AlbumAttributeValue::getAlbumId,id);
        albumAttributeValueMapper.delete(aavWrapper);
        if (CollectionUtil.isNotEmpty(albumInfoVo.getAlbumAttributeValueVoList())){
            for (AlbumAttributeValueVo albumAttributeValueVo : albumInfoVo.getAlbumAttributeValueVoList()) {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                BeanUtil.copyProperties(albumAttributeValueVo,albumAttributeValue);
                albumAttributeValue.setAlbumId(id);
                albumAttributeValueMapper.insert(albumAttributeValue);
            }
        }
        if ("1".equals(albumInfo.getIsOpen())){
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_UPPER, id.toString());
        }else {
            kafkaService.sendMessage(KafkaConstant.QUEUE_ALBUM_LOWER, id.toString());
        }

    }

    /**
     * 查询添加声音专辑列表
     * select id, album_title
     * from album_info
     * where user_id = 1 and status=0301 and is_deleted=0 order by id desc limit 10;
     */
    @Override
    @GuiguCache(prefix = "userAllAlbumList:")
    public List<AlbumInfo> findUserAllAlbumList() {

        LambdaQueryWrapper<AlbumInfo> aiWrapper = new LambdaQueryWrapper<>();
        aiWrapper.select(AlbumInfo::getId,AlbumInfo::getAlbumTitle);
        aiWrapper.eq(AlbumInfo::getUserId,AuthContextHolder.getUserId());
        aiWrapper.eq(AlbumInfo::getStatus,SystemConstant.ALBUM_STATUS_PASS);
        aiWrapper.orderByDesc(AlbumInfo::getId);
        aiWrapper.last(" limit 10 ");
        List<AlbumInfo> albumInfoList = albumInfoMapper.selectList(aiWrapper);
        return albumInfoList;
    }

    /**
     * 根据专辑id查询专辑统计信息
     * @param albumId
     * @return
     */
    @Override
    @GuiguCache(prefix = "albumStatVo:")
    public AlbumStatVo getAlbumStatVo(Long albumId) {
     return albumStatMapper.selectAlbumStatVo(albumId);
    }

    /**
     * 修改专辑统计信息
     * @param albumId
     */
    @Override
    @Transactional
    public void updateAlbumStatBuyNum(Long albumId, String statType, int statNum) {
        //update album_state set stat_type=stat_type+1 where album_id=? and stat_type=?
        LambdaUpdateWrapper<AlbumStat> albumStatLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        albumStatLambdaUpdateWrapper.eq(AlbumStat::getAlbumId,albumId);
        albumStatLambdaUpdateWrapper.eq(AlbumStat::getStatType,statType);
        albumStatLambdaUpdateWrapper.set(AlbumStat::getStatNum,"stat_type"+statNum);
        int count = albumStatMapper.update(null, albumStatLambdaUpdateWrapper);
        if (count==0){
            throw new GuiguException(400,"修改专辑统计信息失败");
        }
    }

    /**
     * 保存专辑统计信息
     *
     * @param albumId  专辑ID
     * @param statType 统计类型
     * @param statNum  统计数量
     */
    public void saveAlbumStat(Long albumId, String statType, int statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(statNum);
        albumStatMapper.insert(albumStat);
    }
}
