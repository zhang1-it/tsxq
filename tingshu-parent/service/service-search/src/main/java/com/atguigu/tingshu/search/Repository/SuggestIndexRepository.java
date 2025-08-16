package com.atguigu.tingshu.search.Repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author 张
 * @version 1.0
 */


public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
