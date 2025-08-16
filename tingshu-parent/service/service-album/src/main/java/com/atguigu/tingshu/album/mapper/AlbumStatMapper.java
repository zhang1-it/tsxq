package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {


    /**
     * 根据专辑id查询专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo selectAlbumStatVo(Long albumId);
    /**
     * 更新专辑统计信息
     * @param albumId
     * @param albumStatPlay
     * @param count
     */
    @Update("update album_stat\n" +
            "set stat_num=stat_num+#{count}, update_time=now()\n" +
            "where stat_type=#{albumStat} and album_id=#{albumId} and is_deleted=0;")
    void updateStat(@Param("albumId") Long albumId,@Param("albumStat") String albumStat,@Param("count") Integer count);
}
