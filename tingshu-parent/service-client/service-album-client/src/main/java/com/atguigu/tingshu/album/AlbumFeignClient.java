package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * <p>
 * 专辑模块远程调用Feign接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album", path = "api/album", fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {
    /**
     * 回显声音信息
     * api/album/trackInfo/getTrackInfo/51948
     */
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id);
    /**
     * 修改专辑购买数量
     * Long albumId, String statType, int statNum
     */
    @PutMapping("/albumInfo/updateAlbumStatBuyNum/{albumId}/{statType}/{statNum}")
    public Result updateAlbumStatBuyNum(@PathVariable Long albumId,
                                        @PathVariable String statType,
                                        @PathVariable Integer statNum
    );

    /**
     * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
     *
     * @param trackId    声音ID
     * @param trackCount 数量
     * @return
     */
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> getWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable int trackCount);
    /**
     * 查询所有一级分类列表
     * @return
     */
    @GetMapping("/category/findAllCategory1")
     Result<List<BaseCategory1>> getAllCategory1();
    /**
     * 根据id查询专辑信息
     * /api/album/albumInfo/getAlbumInfo/{id}
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
     Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);
    @GetMapping("/category/getbaseCategoryViewById/{id}")
    Result<BaseCategoryView> getbaseCategoryViewById(@PathVariable Long id);

    /**
     * 根据一级分类Id查询三级分类列表
     * /api/album/category/findTopBaseCategory3/{category1Id}
     */
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id);

    /**
     * 根据专辑id查询专辑统计信息
     * /api/album/albumInfo/getAlbumStatVo/{albumId}
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);
}
