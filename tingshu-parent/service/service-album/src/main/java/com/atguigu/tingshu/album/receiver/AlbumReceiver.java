package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
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
public class AlbumReceiver {

    @Autowired
    private TrackInfoService trackInfoService;

    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateTrackStat(ConsumerRecord<String,String> consumerRecord) {
        try {
            String value = consumerRecord.value();
            TrackStatMqVo trackStatMqVo = JSON.parseObject(value, TrackStatMqVo.class);
            trackInfoService.updateTrackStat(trackStatMqVo);
            log.info("[专辑服务]，监听到更新声音统计消息：{}", value);
        } catch (Exception e) {
            log.error("[专辑服务]，监听更新声音统计消息失败：{}", e.getMessage());
            throw new GuiguException(400,"更新声音统计失败");
        }


    }

}
