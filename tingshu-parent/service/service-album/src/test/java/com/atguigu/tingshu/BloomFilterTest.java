package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 张
 * @version 1.0
 */

@SpringBootTest
public class BloomFilterTest {
    @Autowired
    private RedissonClient redissonClient;
    @Test
    public void addBloomFilter(){
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        for (int i = 0; i < 1602; i++) {
            bloomFilter.add(Long.valueOf(i));
        }
    }
}
