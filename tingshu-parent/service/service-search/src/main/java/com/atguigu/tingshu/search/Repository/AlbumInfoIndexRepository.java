package com.atguigu.tingshu.search.Repository;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author 张
 * @version 1.0
 */

public interface AlbumInfoIndexRepository extends ElasticsearchRepository<AlbumInfoIndex,Long> {
}
