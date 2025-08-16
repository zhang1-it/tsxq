package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {

    /**
     * 根据一级分类Id查询三级分类列表
     * @param category1Id
     * @return
     */
    List<BaseCategory3> findTopBaseCategory3(Long category1Id);

    /**
     * 根据1级分类对象查询包含二级分类（包含三级分类）
     * @param category1Id
     * @return
     */
    JSONObject getBaseCategoryListByCategory1Id(Long category1Id);
}
