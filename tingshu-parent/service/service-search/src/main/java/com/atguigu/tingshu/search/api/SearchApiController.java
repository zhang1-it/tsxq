package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 获取排行榜
     * /api/search/albumInfo/findRankingList/{category1Id}/{dimension}
     */
    @GetMapping("/albumInfo/findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndex>> findRankingList(@PathVariable String category1Id, @PathVariable String dimension) {
       List<AlbumInfoIndex> albumInfoIndexList= searchService.findRankingList(category1Id, dimension);
       return Result.ok(albumInfoIndexList);
    }

    /**
     * 为定时更新首页排行榜提供调用接口
     *
     * @return
     */
    @Operation(summary = "为定时更新首页排行榜提供调用接口")
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }

    /**
     * 专辑详情
     * /api/search/albumInfo/{albumId}
     */
    @GetMapping("/albumInfo/{albumId}")
    public Result<Map<String, Object>> albumInfo(@PathVariable Long albumId) {
        Map<String, Object> resultMap = searchService.albumInfo(albumId);
        return Result.ok(resultMap);
    }


    /**
     * 关键字补充
     * /api/search/albumInfo/completeSuggest/{keyword}
     */
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword) {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }

    /**
     * 查询指定一级分类下热门排行专辑
     * /api/search/albumInfo/channel/{category1Id}
     */
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> channel(@PathVariable Long category1Id) {
        List<Map<String, Object>> list = searchService.channel(category1Id);
        return Result.ok(list);
    }

    /**
     * 专辑检索
     * /api/search/albumInfo
     */
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }

}

