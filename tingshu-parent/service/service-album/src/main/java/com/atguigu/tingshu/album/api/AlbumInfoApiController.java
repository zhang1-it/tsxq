package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * 修改专辑购买数量
     * Long albumId, String statType, int statNum
     */
    @PutMapping("/albumInfo/updateAlbumStatBuyNum/{albumId}/{statType}/{statNum}")
    public Result updateAlbumStatBuyNum(@PathVariable Long albumId,
                                        @PathVariable String statType,
                                        @PathVariable Integer statNum
                                        ){
        albumInfoService.updateAlbumStatBuyNum(albumId,statType,statNum);
        return Result.ok();
    }


    /**
     * 根据专辑id查询专辑统计信息
     * /api/album/albumInfo/getAlbumStatVo/{albumId}
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        return Result.ok(albumStatVo);
    }

    /**
     * 查询添加声音专辑列表
     * api/album/albumInfo/findUserAllAlbumList
     */
    @GetMapping("/albumInfo/findUserAllAlbumList")
    @GuiGuLogin
    public Result<List<AlbumInfo>> findUserAllAlbumList(){
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList();
        return Result.ok(albumInfoList);
    }

    /**
     * 根据id修改专辑
     * /api/album/albumInfo/updateAlbumInfo/{id}
     */
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable Long id, @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.updateAlbumInfo(id, albumInfoVo);
        return Result.ok();
    }

    /**
     * 根据id查询专辑信息
     * /api/album/albumInfo/getAlbumInfo/{id}
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
        return Result.ok(albumInfo);
    }

    /**
     * 根据id删除专辑
     * /api/album/albumInfo/removeAlbumInfo/{id}
     */
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id) {
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }


    /**
     * /api/album/albumInfo/findUserAlbumPage/{page}/{limit}
     * 查询用户专辑分页列表
     */
    @GuiGuLogin
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    public Result<IPage<AlbumListVo>> findUserAlbumPage(@PathVariable Long page,
                                                        @PathVariable Long limit,
                                                        @RequestBody(required = false) AlbumInfoQuery albumInfoQuery) {


        Page<AlbumListVo> albumListVoPage = new Page<>(page, limit);
        albumListVoPage = albumInfoService.findUserAlbumPage(albumListVoPage, albumInfoQuery);
        return Result.ok(albumListVoPage);
    }


    /**
     * 保存专辑
     * /api/album/albumInfo/saveAlbumInfo
     */
    @PostMapping("/albumInfo/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }

}

