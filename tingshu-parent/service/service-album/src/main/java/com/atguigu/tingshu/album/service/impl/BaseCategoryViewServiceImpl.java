package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryViewService;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 张
 * @version 1.0
 */

@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    /**
     * 查询分类集合
     * @return
     */
    @Override
    public List<JSONObject> getBaseCategoryList() {
        JSONObject jsonObject = new JSONObject();
        //1.查询所有分类
        List<BaseCategoryView> allCategory = baseCategoryViewMapper.selectList(null);
        ArrayList<JSONObject> array1 = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(allCategory)) {
            //2.按一级分类进行分组
            Map<Long, List<BaseCategoryView>> category1List = allCategory.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

            for (Map.Entry<Long, List<BaseCategoryView>> category1 : category1List.entrySet()) {
                JSONObject obj1 = new JSONObject();
                obj1.put("categoryId", category1.getKey());
                obj1.put("categoryName", category1.getValue().get(0).getCategory1Name());

                //3.得到所有二级分类
                List<BaseCategoryView> category2ListByCategory1Id = category1.getValue();
                //4.按二级分类进行分组
                Map<Long, List<BaseCategoryView>> category2List = category2ListByCategory1Id.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
                ArrayList<JSONObject> array2 = new ArrayList<>();
                for (Map.Entry<Long, List<BaseCategoryView>> category2 : category2List.entrySet()) {
                    JSONObject obj2 = new JSONObject();
                    obj2.put("categoryId", category2.getKey());
                    obj2.put("categoryName", category2.getValue().get(0).getCategory2Name());

                    //5.得到所有三级分类
                    List<BaseCategoryView> category3ListByCategory2Id = category2.getValue();
//                    Map<Long, List<BaseCategoryView>> category3List = category3ListByCategory2Id.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
//                    ArrayList<JSONObject> array3 = new ArrayList<>();
//                    for (Map.Entry<Long, List<BaseCategoryView>> category3 : category3List.entrySet()) {
//                        JSONObject obj3 = new JSONObject();
//                        obj3.put("categoryId", category3.getKey());
//                        obj3.put("categoryName", category3.getValue().get(0).getCategory3Name());
//                        array3.add(obj3);
//                    }
                    //上面代码可优化成这样
                    List<JSONObject> array3 = category3ListByCategory2Id.stream().map(category3 -> {
                        JSONObject obj3 = new JSONObject();
                        obj3.put("categoryId", category3.getCategory3Id());
                        obj3.put("categoryName", category3.getCategory3Name());
                        return obj3;
                    }).collect(Collectors.toList());

                    obj2.put("categoryChild", array3);
                    array2.add(obj2);
                }
                obj1.put("categoryChild", array2);
                array1.add(obj1);
            }
        }
        return array1;
    }
}
