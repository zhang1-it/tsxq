package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {

    /**
     * 修改声音统计信息
     * @param trackId
     * @param statType
     * @param count
     */
    @Update("update track_stat\n" +
            "set stat_num=stat_num+#{count}, update_time=now()\n" +
            "where stat_type=#{statType} and track_id=#{trackId} and is_deleted=0")
    void updateStat(@Param("trackId") Long trackId, @Param("statType") String statType, @Param("count") Integer count);
}
