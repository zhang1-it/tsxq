package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> uploadTrack(MultipartFile file);

    TrackMediaInfoVo getTrackMediaInfoVo(String mediaFileId);

    /**
     * 删除云点播中旧的声音文件
     * @param beforMediaFileId
     */
    void removeBeforMedia(String beforMediaFileId);
}
