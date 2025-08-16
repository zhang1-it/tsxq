package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {
    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 定时执行热门专辑更新
     */
    @XxlJob("updateHotAlbumJob")
    public void updateHotAlbumJob() {
        log.info("定时执行热门专辑更新");
        searchFeignClient.updateLatelyAlbumRanking();
    }
    //TODO：vip过期更新
}