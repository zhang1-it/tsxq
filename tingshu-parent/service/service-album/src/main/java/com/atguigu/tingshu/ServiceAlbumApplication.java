package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }

    /**
     * 初始化布隆过滤器
     * CommandLineRunner是springboot提供方法该方法boot应用启动成功后，自动触发一次
     *
     * @param args
     * @throws Exception
     */
    @Autowired
    private RedissonClient redissonClient;
    @Override
    public void run(String... args) throws Exception {
        System.out.println("布隆过滤器初始化");
        //1.获取到布隆过滤器对象
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        //2.调用初始化方法 p1:数据规模  p2:误判率
        bloomFilter.tryInit(100000L, 0.03);
    }
}
