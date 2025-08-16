package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author 张
 * @version 1.0
 */


public interface BaseCategoryViewService extends IService<BaseCategoryView> {
    List<JSONObject> getBaseCategoryList();
}
