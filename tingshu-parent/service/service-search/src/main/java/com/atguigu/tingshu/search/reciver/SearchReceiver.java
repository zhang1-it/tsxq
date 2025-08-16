package com.atguigu.tingshu.search.reciver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author 张
 * @version 1.0
 */

@Component
@Slf4j
public class SearchReceiver {
    @Autowired
    private SearchService searchService;
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void upperAlbum(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isBlank(value)){
            log.info("[搜索服务]监听到专辑上架消息：{}", value);
            searchService.upperAlbum(Long.valueOf(value));
        }
    }

    /**
     * 监听专辑下架消息，完成索引库删除
     * 考虑：1.要不要进行幂等性处理  2.是否需要进行事务管理（不需要）
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (StringUtils.isNotBlank(value)) {
            log.info("[搜索服务]监听到专辑下架消息：{}", value);
            searchService.lower(Long.valueOf(value));
        }
    }
}
