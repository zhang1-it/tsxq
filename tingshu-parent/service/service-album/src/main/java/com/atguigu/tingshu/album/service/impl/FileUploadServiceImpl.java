package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


/**
 * @author 张
 * @version 1.0
 */

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Override
    public String fileUpload(MultipartFile file) {

        try {
            //1.验证文件是否为图片
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read == null) {
                throw new GuiguException(400, "图片格式非法！");
            }
           String bucketName = minioConstantProperties.getBucketName();
           String endpointUrl = minioConstantProperties.getEndpointUrl();
            //2.判断桶是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists){
                //3.如果不存在在就创建桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            else {
                System.out.println("桶'"+bucketName+"'已经存在");
            }
            String folderName = "/" + DateUtil.today() + "/";
            String fileName = IdUtil.randomUUID();
            String extName = FileUtil.extName(file.getOriginalFilename());
            String objectName = folderName + fileName + "." + extName;
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName)
                                .object(objectName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                                );
            return endpointUrl+"/"+bucketName+objectName;
        } catch (Exception e) {
            log.error("上传文件异常,message:{}",e);
            throw new RuntimeException(e);
        }

    }
}
