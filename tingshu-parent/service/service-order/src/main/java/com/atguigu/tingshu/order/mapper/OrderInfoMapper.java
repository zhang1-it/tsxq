package com.atguigu.tingshu.order.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 获取用户订单分页列表
     * @param iPage
     * @param userId
     * @return
     */
    Page<OrderInfo> selectUserPage(IPage<OrderInfo> iPage, Long userId);
}
