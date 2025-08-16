package com.atguigu.tingshu.common.tread;

import com.atguigu.tingshu.common.zipkin.ZipkinHelper;
import com.atguigu.tingshu.common.zipkin.ZipkinTaskDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * @author 张
 * @version 1.0
 */

@Configuration
public class TreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        //1.动态得到线程数 IO密集型：CPU逻辑处理器个数*2
        int coreCount = Runtime.getRuntime().availableProcessors()*2;
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreCount,
                coreCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                (r, e) -> {
                    //r:被拒绝任务  e:线程池对象
                    //自定义拒绝策略：重试-将任务再次提交给线程执行
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    e.submit(r);
                }
        );

        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }

    @Autowired
    private ZipkinHelper zipkinHelper;

    /**
     * 项目中选择spring线程池
     * Spring提供线程池threadPoolTaskExecutor
     *
     * @return
     */
    @Bean
    public Executor threadPoolTaskExecutor() {
        int count = Runtime.getRuntime().availableProcessors();
        int threadCount = count*2+1;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 核心池大小
        taskExecutor.setCorePoolSize(threadCount);
        // 最大线程数
        taskExecutor.setMaxPoolSize(threadCount);
        // 队列程度
        taskExecutor.setQueueCapacity(300);
        // 线程空闲时间
        taskExecutor.setKeepAliveSeconds(0);
        // 线程前缀名称
        taskExecutor.setThreadNamePrefix("sync-tingshu-Executor--");
        // 该方法用来设置 线程池关闭 的时候 等待 所有任务都完成后，再继续 销毁 其他的 Bean，
        // 这样这些 异步任务 的 销毁 就会先于 数据库连接池对象 的销毁。
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 任务的等待时间 如果超过这个时间还没有销毁就 强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
        taskExecutor.setAwaitTerminationSeconds(300);
        //设置解决zipkin链路追踪不完整装饰器对象
        taskExecutor.setTaskDecorator(new ZipkinTaskDecorator(zipkinHelper));
        // 线程不够用时由调用的线程处理该任务
        taskExecutor.setRejectedExecutionHandler((task, executor)->{
            //将拒绝任务再次提交给线程池执行
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executor.submit(task);
        });
        return taskExecutor;
    }
}
