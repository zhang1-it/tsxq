package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AlbumDegradeFeignClient implements AlbumFeignClient {

    @Override
    public Result<TrackInfo> getTrackInfo(Long id) {
        log.error("[专辑模块]提供远程调用方法getTrackInfo服务降级");
        return Result.fail();
    }

    @Override
    public Result updateAlbumStatBuyNum(Long albumId, String statType, Integer statNum) {
        log.error("[专辑模块]提供远程调用方法updateAlbumStatBuyNum服务降级");
        return Result.fail();
    }

    @Override
    public Result<List<TrackInfo>> getWaitBuyTrackInfoList(Long trackId, int trackCount) {
        log.error("[专辑模块]提供远程调用方法getWaitBuyTrackInfoList服务降级");
        return Result.fail();
    }

    @Override
    public Result<List<BaseCategory1>> getAllCategory1() {
        log.error("[专辑模块Feign调用]getAllCategory1异常");
        return Result.fail();
    }
    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("AlbumFeignClient调用getAlbumInfo失败，参数{}",id);
        return Result.fail();
    }

    @Override
    public Result<BaseCategoryView> getbaseCategoryViewById(Long id) {
        log.error("AlbumFeignClient调用getbaseCategoryViewById失败，参数{}",id);
        return Result.fail();
    }

    @Override
    public Result<List<BaseCategory3>> findTopBaseCategory3(Long category1Id) {
        log.error("AlbumFeignClient调用findTopBaseCategory3失败，参数{}",category1Id);
        return Result.fail();
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("AlbumFeignClient调用getAlbumStatVo失败，参数{}",albumId);
        return Result.fail();
    }
}
