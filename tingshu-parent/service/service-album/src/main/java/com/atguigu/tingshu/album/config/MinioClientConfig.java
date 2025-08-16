package com.atguigu.tingshu.album.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 张
 * @version 1.0
 */

@Configuration
public class MinioClientConfig {
    @Autowired
    private MinioConstantProperties minioConstantProperties;
    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(minioConstantProperties.getEndpointUrl())
                .credentials(minioConstantProperties.getAccessKey(),minioConstantProperties.getSecreKey())
                .build();
    }
}
