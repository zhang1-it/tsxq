package com.atguigu.tingshu.common.cache;

import cn.hutool.core.util.ArrayUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 张
 * @version 1.0
 * 缓存切面
 */

@Aspect
@Component
@Slf4j
public class GuiguCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(guiguCache)")
    public Object cache(ProceedingJoinPoint joinPoint, GuiguCache guiguCache) throws Throwable {
        try {
            String prefix = guiguCache.prefix();
            Object[] args = joinPoint.getArgs();
            //构建redis的key
            String paramVal = "none";
            if (ArrayUtil.isNotEmpty(args)) {
                paramVal = Arrays.stream(args).map(Object::toString).collect(Collectors.joining(":"));
            }
            String key = prefix + paramVal;
            //查询redis中是否存在改值
            Object resultObject = redisTemplate.opsForValue().get(key);
            if (resultObject != null) {
                return resultObject;
            }
            //加分布式锁避免缓存击穿
            String lockKey = key + RedisConstant.CACHE_LOCK_SUFFIX;
            RLock lock = redissonClient.getLock(lockKey);
            try {

                lock.tryLock(RedisConstant.CACHE_LOCK_EXPIRE_PX1, RedisConstant.CACHE_LOCK_EXPIRE_PX2, TimeUnit.SECONDS);

                //3.1 再次查询一次缓存:处于阻塞等待获取线程（终将获取锁成功）避免获取锁线程再次查库，这里再查一次缓存
                resultObject = redisTemplate.opsForValue().get(key);
                if (resultObject != null) {
                    return resultObject;
                }

                //redis中不存在该值，则调用方法获取数据
                resultObject = joinPoint.proceed();
                //获取返回值的类型
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                Class returnType = methodSignature.getReturnType();
                //如果查询的数据为null，需要做缓存穿透处理
                if (resultObject == null) {
                    resultObject = returnType.newInstance();
                    //存储零时值，避免重复查询数据库
                    redisTemplate.opsForValue().set(key, resultObject, RedisConstant.CACHE_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                    return resultObject;
                } else {
                    redisTemplate.opsForValue().set(key, resultObject, RedisConstant.CACHE_TIMEOUT, TimeUnit.SECONDS);
                    return resultObject;
                }
            } finally {
                //释放锁
                lock.unlock();
            }
        } catch (Throwable e) {
            log.info("自定义缓存切面异常：{}", e);
            //5.兜底处理方案：如果redis服务不可用，则执行查询数据库方法
           return joinPoint.proceed();
        }
    }
}
