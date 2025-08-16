package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {

    /**
     * 保存专辑信息
     * @param albumInfoVo
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    /**
     * 查询用户分页专辑列表
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery);

    /**
     * 根据id删除专辑信息
     * @param id
     */
    void removeAlbumInfo(Long id);

    /**
     * 根据id查询专辑信息
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 根据id修改专辑
     * @param id
     * @param albumInfoVo
     */
    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);
    /**
     * 查询添加声音专辑列表
     */
    List<AlbumInfo> findUserAllAlbumList();

    /**
     * 根据专辑id查询专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);

    /**
     * 修改专辑购买数量
     * @param albumId
     */
    void updateAlbumStatBuyNum(Long albumId, String statType, int statNum);
}
