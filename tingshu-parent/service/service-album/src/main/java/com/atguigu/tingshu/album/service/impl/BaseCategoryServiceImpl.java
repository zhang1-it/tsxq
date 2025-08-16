package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory2Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory3Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory2;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    /**
     * 根据一级分类Id查询三级分类列表
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
        LambdaQueryWrapper<BaseCategory2> bc2QueeryWrapper = new LambdaQueryWrapper<>();
        bc2QueeryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(bc2QueeryWrapper);
        List<Long> category2Ids = baseCategory2List.stream().map(item -> item.getId())
                .collect(Collectors.toList());
        LambdaQueryWrapper<BaseCategory3> bc3QueeryWrapper = new LambdaQueryWrapper<>();

        bc3QueeryWrapper.in(BaseCategory3::getCategory2Id, category2Ids);
        bc3QueeryWrapper.eq(BaseCategory3::getIsTop, 1);
        bc3QueeryWrapper.last(" limit 0,7 ");
        List<BaseCategory3> baseCategory3s = baseCategory3Mapper.selectList(bc3QueeryWrapper);
        return baseCategory3s;
    }

    /**
     * 根据1级分类对象查询包含二级分类（包含三级分类）
     *
     * @param category1Id
     * @return
     */
    @Override
    public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
        LambdaQueryWrapper<BaseCategoryView> bcvQueryWrapper = new LambdaQueryWrapper<>();
        bcvQueryWrapper.eq(BaseCategoryView::getCategory1Id, category1Id);
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(bcvQueryWrapper);

        if (CollectionUtil.isNotEmpty(baseCategoryViews)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("categoryName", baseCategoryViews.get(0).getCategory1Name());
            jsonObject.put("categoryId", category1Id);

            Map<Long, List<BaseCategoryView>> categoryListByC2Id = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            ArrayList<JSONObject> category2List = new ArrayList<>();
            System.out.println(categoryListByC2Id.entrySet());
            for (Map.Entry<Long, List<BaseCategoryView>> entry : categoryListByC2Id.entrySet()) {
                JSONObject obj2 = new JSONObject();
                obj2.put("categoryId", entry.getKey());
                obj2.put("categoryName", entry.getValue().get(0).getCategory2Name());
                ArrayList<JSONObject> category3List = new ArrayList<>();
                Map<Long, List<BaseCategoryView>> categoryListByC3Id = entry.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                categoryListByC3Id.entrySet().forEach(entry2 -> {
                    JSONObject obj3 = new JSONObject();
                    obj3.put("categoryId", entry2.getKey());
                    obj3.put("categoryName", entry2.getValue().get(0).getCategory3Name());
                    category3List.add(obj3);
                });
                obj2.put("categoryChild", category3List);
                category2List.add(obj2);
            }
            jsonObject.put("categoryChild", category2List);
            return jsonObject;
        }
        return null;
    }
}
