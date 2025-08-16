package com.atguigu.tingshu.search.client.impl;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

/**
 * @author 张
 * @version 1.0
 */

@Component
@Slf4j
public class SearchDegradeFeignClient implements SearchFeignClient {
    /**
     * 更新排行榜
     * @return
     */
    @Override
    public Result updateLatelyAlbumRanking() {
        log.error("[搜索feign模块]远程调用updateLatelyAlbumRanking失败");
        return Result.fail();
    }

}
