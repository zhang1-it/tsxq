package com.atguigu.tingshu.user.repository;

import com.atguigu.tingshu.model.user.UserListenProcess;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author 张
 * @version 1.0
 */


public interface UserListenRepository extends MongoRepository<UserListenProcess,String> {
}
