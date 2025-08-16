package com.atguigu;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.ServiceSearchApplication;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 张
 * @version 1.0
 */

@SpringBootTest(classes = ServiceSearchApplication.class)
public class SearchTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private SearchService service;

    @Test
    public void testUpper(){
        service.upperAlbum(1609L);
    }
}
