package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private VodService vodService;

    /**
     * 提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表
     *
     * @param trackId    声音ID
     * @param trackCount 数量
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "提供给订单服务渲染购买商品（声音）列表-查询当前用户待购买声音列表")
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> getWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable int trackCount) {
        Long userId = AuthContextHolder.getUserId();
        List<TrackInfo> trackInfoList = trackInfoService.getWaitBuyTrackInfoList(userId, trackId, trackCount);
        return Result.ok(trackInfoList);
    }

    /**
     * 获取当前用户分集购买声音列表
     *
     * @param trackId 声音ID
     * @return [{name:"本集", price:0.2, trackCount:1},{name:"后10集", price:2, trackCount:10},...,{name:"全集", price:*, trackCount:*}]
     */
    @GuiGuLogin
    @Operation(summary = "获取当前用户分集购买声音列表")
    @GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
    public Result<List<Map<String, Object>>> getUserWaitBuyTrackPayList(@PathVariable Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        List<Map<String, Object>> list = trackInfoService.getUserWaitBuyTrackPayList(trackId);
        return Result.ok(list);
    }

    /**
     * 根据声音ID，获取声音统计信息
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音ID，获取声音统计信息")
    @GetMapping("/trackInfo/getTrackStatVo/{trackId}")
    public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId) {
        return Result.ok(trackInfoService.getTrackStatVo(trackId));
    }

    /**
     * 查询专辑下声音列表分页集合
     * /api/album/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}
     */
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    @GuiGuLogin(required = false)
    public Result<Page<AlbumTrackListVo>> findAlbumTrackPage(@PathVariable Long albumId, @PathVariable int page, @PathVariable int limit) {
        Page<AlbumTrackListVo> PageInfo = new Page<>(page, limit);
        Long userId = AuthContextHolder.getUserId();
        PageInfo = trackInfoService.findAlbumTrackPage(PageInfo, albumId, userId);
        return Result.ok(PageInfo);
    }

    /**
     * 根据id删除声音信息
     * /api/album/trackInfo/removeTrackInfo/{id}
     */
    public Result removeTrackInfo(@PathVariable Long id) {
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }

    /**
     * 修改用户专辑声音信息
     * /api/album/trackInfo/updateTrackInfo/{id}
     */
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfoVo trackInfoVo) {
        trackInfoService.updateTrackInfo(id, trackInfoVo);
        return Result.ok();
    }

    /**
     * 回显声音信息
     * api/album/trackInfo/getTrackInfo/51948
     */
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }

    /**
     * 获取当前登录声音分页列表
     *
     * @param page           页码
     * @param limit          页大小
     * @param trackInfoQuery 查询条件
     * @return
     */
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    @GuiGuLogin
    public Result<Page<TrackListVo>> getUserTrackByPage(@PathVariable int page, @PathVariable int limit, @RequestBody TrackInfoQuery trackInfoQuery) {
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.封装分页查询对象
        trackInfoQuery.setUserId(userId);
        //3.调用业务层进行分页
        Page<TrackListVo> PageInfo = new Page<>(page, limit);
        PageInfo = trackInfoService.getUserTrackByPage(PageInfo, trackInfoQuery);
        return Result.ok(PageInfo);
    }

    /**
     * 保存声音
     * /api/album/trackInfo/saveTrackInfo
     */
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        trackInfoService.saveTrackInfo(userId, trackInfoVo);
        return Result.ok();
    }

    /**
     * 上传声音
     * /api/album/trackInfo/uploadTrack
     */
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestBody MultipartFile file) {
        Map<String, String> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }
}

