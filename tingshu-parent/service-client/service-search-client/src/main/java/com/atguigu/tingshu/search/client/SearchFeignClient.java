package com.atguigu.tingshu.search.client;

import com.atguigu.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import com.atguigu.tingshu.common.result.Result;

/**
 * @author 张
 * @version 1.0
 */
@FeignClient(value = "service-search",path = "api/search",fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {
    /**
     * 更新排行榜
     * @return
     */
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    Result updateLatelyAlbumRanking();
}
