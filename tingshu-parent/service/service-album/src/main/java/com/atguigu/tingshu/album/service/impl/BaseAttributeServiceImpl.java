package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.BaseAttributeMapper;
import com.atguigu.tingshu.album.service.BaseAttributeService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 张
 * @version 1.0
 */

@Service
public class BaseAttributeServiceImpl extends ServiceImpl<BaseAttributeMapper,BaseAttribute> implements BaseAttributeService {
   @Autowired
   private BaseAttributeMapper baseAttributeMapper;
    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        return baseAttributeMapper.selectAttribute(category1Id);
    }
}
