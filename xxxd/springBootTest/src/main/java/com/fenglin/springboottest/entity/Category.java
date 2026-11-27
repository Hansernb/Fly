package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目分类字典（对应 boonie_category 表）
 * 借鉴 java_travel 的 b_classification：把写死在前端的「刺激/亲子/观光」等标签收进字典表，
 * 前台读库展示，后台可增删改，避免改一个标签要动前端代码。
 */
@Data
@TableName("boonie_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 分类名称 */
    private String name;
    /** 分类图标 */
    private String emoji;
    /** 主题色 */
    private String color;
    /** 展示顺序 */
    private Integer sortNo;
    /** 是否启用 */
    private Boolean enabled;
    private LocalDateTime createdAt;
}
