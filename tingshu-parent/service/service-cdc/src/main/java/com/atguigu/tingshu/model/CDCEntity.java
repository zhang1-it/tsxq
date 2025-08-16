package com.atguigu.tingshu.model;

import lombok.Data;

import javax.persistence.Column;

/**
 *
 * @author: atguigu
 * @create: 2023-11-01 16:22
 */
@Data
public class CDCEntity {
    //这里对应的是数据库中的字段名
    @Column(name = "id")
    private Long id;
}