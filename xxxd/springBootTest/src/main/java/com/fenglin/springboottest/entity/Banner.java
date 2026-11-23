package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页轮播图（对应 boonie_banner 表） */
@Data
@TableName("boonie_banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 轮播标题 */
    private String title;
    /** 副标题 / 前台描述 */
    private String subtitle;
    /** 角标文案 */
    private String tag;
    /** 图片地址 */
    private String img;
    /** 跳转板块 */
    private String link;
    /** 主题色 */
    private String color;
    /** 展示顺序 */
    private Integer sortNo;
    /** 是否上架：1 上架 0 下架 */
    private Boolean enabled;
    private LocalDateTime createdAt;
}
