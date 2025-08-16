package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author 张
 * @version 1.0
 */


public interface BaseAttributeService extends IService<BaseAttribute> {
    List<BaseAttribute> findAttribute(Long category1Id);
}
