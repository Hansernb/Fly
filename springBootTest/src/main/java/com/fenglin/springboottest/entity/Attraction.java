package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 游乐项目（对应 boonie_attraction 表） */
@Data
@TableName("boonie_attraction")
public class Attraction {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 项目名称 */
    private String name;
    /** 项目图标 emoji */
    private String emoji;
    /** 项目具体内容（简介 / 玩法说明） */
    private String description;
    /** 标签：刺激 / 浪漫 / 亲子 / 清凉 / 益智 */
    private String tag;
    /** 预计排队时间（分钟） */
    private Integer waitMinutes;
    /** 主题色 */
    private String color;
    private LocalDateTime createdAt;

    /** 游客评价列表（非表字段，查询时填充） */
    @TableField(exist = false)
    private List<Review> reviews;
    /** 综合评分（非表字段，由评价实时计算） */
    @TableField(exist = false)
    private BigDecimal rating;
}
