package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    void updateTrackStat(Long trackId, String statType, int statNum);

    /**
     *  获取当前登录声音分页列表
     * @param pageInfo
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> getUserTrackByPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 修改用户专辑声音信息
     * @param id
     * @param trackInfoVo
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 根据id删除声音信息
     * @param id
     */
    void removeTrackInfo(Long id);

    /**
     * 查询专辑下声音列表分页集合
     * @param pageInfo
     * @param albumId
     * @return
     */
    Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId,Long userId);

    /**
     * 修改声音统计信息
     * @param trackStatMqVo
     */
    void updateTrackStat(TrackStatMqVo trackStatMqVo);

    /**
     * 根据声音ID，获取声音统计信息
     * @param trackId
     * @return
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 获取当前用户分集购买声音列表
     * @param trackId
     * @return
     */
    List<Map<String, Object>> getUserWaitBuyTrackPayList(Long trackId);

    /**
     * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
     * @param userId
     * @param trackId
     * @param trackCount
     * @return
     */
    List<TrackInfo> getWaitBuyTrackInfoList(Long userId, Long trackId, int trackCount);
}
