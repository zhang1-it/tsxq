package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import io.micrometer.common.util.StringUtils;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private VodUploadClient vodUploadClient;

    @Autowired
    private Credential credential;

    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        try {
            //先获得一个VodUploadClient对象，为了防止每次调用该方法都创建一个VodUploadClient对象，把这个对象放入ioc中
//        VodUploadClient client = new VodUploadClient("your secretId", "your secretKey");

            //把文件存入本地，并返回本地文件路径
            String filePath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            if (StringUtils.isBlank(filePath)) {
                throw new GuiguException(400, "当前声音上传为空");
            }
            //创建上传请求对象
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(filePath);
            //开始上传
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);
            HashMap<String, String> map = new HashMap<>();
            if (response!=null){
                map.put("mediaFileId",response.getFileId());
                map.put("mediaUrl",response.getMediaUrl());
            }
            return map;
        }  catch (Exception e) {
            log.error("[专辑服务]上传音频文件到点播平台异常：文件：{}，错误信息：{}", file, e);
            throw new RuntimeException(e);
          }
    }

    @Override
    public TrackMediaInfoVo getTrackMediaInfoVo(String mediaFileId) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性
            // 以下代码示例仅供参考，建议采用更安全的方式来使用密钥
            // 请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
//            Credential cred = new Credential("SecretId", "SecretKey"); //加入ioc容器
            // 使用临时密钥示例
            // Credential cred = new Credential("SecretId", "SecretKey", "Token");

            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(credential, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            String[] fileIds1 = {mediaFileId};
            req.setFileIds(fileIds1);

            req.setSubAppId(vodConstantProperties.getAppId().longValue());
            // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
            DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
            if (resp != null) {
                MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
                if (mediaInfoSet != null && mediaInfoSet.length > 0) {
                    TrackMediaInfoVo vo = new TrackMediaInfoVo();
                    MediaInfo mediaInfo = mediaInfoSet[0];
                    //5.1 获取媒体文件基本信息对象
                    MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                    vo.setType(basicInfo.getType());

                    //5.2 获取媒体文件元信息对象
                    MediaMetaData metaData = mediaInfo.getMetaData();
                    vo.setDuration(metaData.getAudioDuration());
                    vo.setSize(metaData.getSize());
                    return vo;
                }
            }
        } catch (TencentCloudSDKException e) {
            log.error("[专辑服务]获取点播平台文件：{}，详情异常：{}", mediaFileId, e);
            throw new GuiguException(400, "获取文件信息异常");
        }
        return null;
    }

    /**
     * 删除云点播中旧的声音文件
     * @param beforMediaFileId
     */
    @Override
    public void removeBeforMedia(String beforMediaFileId) {
        try{

            // 实例化要请求产品的client对象
            VodClient client = new VodClient(credential, vodConstantProperties.getRegion());
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(beforMediaFileId);
            req.setSubAppId(vodConstantProperties.getAppId().longValue());
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);

        } catch (TencentCloudSDKException e) {
            log.info("[专辑服务]删除云点播文件：{}，失败{}", beforMediaFileId, e);
            throw new GuiguException(400, "删除声音异常");
        }
    }
}
