package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TestLockService;
import com.atguigu.tingshu.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 张
 * @version 1.0
 */

@RestController
@RequestMapping("/api/testlock")
public class TestLockController {

//    @Autowired
//    private TestLockService testLockService;
//
//    /**
//     * 测试分布式锁
//     * @return
//     */
//    @GetMapping("/test1")
//    public Result test1(){
//        testLockService.test1();
//        return Result.ok();
//    }
}
