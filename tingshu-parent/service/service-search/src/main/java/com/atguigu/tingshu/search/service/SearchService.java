package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 专辑上架
     */
    public void upperAlbum(Long albumId);

    /**
     * 下架
     */
    public void lower(Long albumId);

    /**
     * 专辑检索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 查询指定一级分类下热门排行专辑
     * @param category1Id
     * @return
     */
    List<Map<String,Object>> channel(Long category1Id);

    /**
     * 转换结果AlbumSearchResponseVo
     * @param search
     * @param albumIndexQuery
     * @return
     */
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> search, AlbumIndexQuery albumIndexQuery);

    /**
     * 构建条件查询es语句
     * @param albumIndexQuery
     * @return
     */
    public SearchRequest bulidDSL(AlbumIndexQuery albumIndexQuery);


    void saveSuggestIndex(AlbumInfoIndex albumInfoIndex);

    /**
     * 关键字补充
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 专辑详情
     * @param albumId
     * @return
     */
    Map<String, Object> albumInfo(Long albumId);
    /**
     * 获取不同分类下不同排序方式榜单专辑列表
     */
    void updateLatelyAlbumRanking();

    /**
     * 获取排行榜
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndex> findRankingList(String category1Id, String dimension);
}
