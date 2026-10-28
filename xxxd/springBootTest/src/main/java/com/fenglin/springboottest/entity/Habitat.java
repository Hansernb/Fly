package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 森林栖息地（对应 boonie_habitat 表，首页「🦉 森林栖息地」区块） */
@Data
@TableName("boonie_habitat")
public class Habitat {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 图标 emoji */
    private String emoji;
    /** 配图地址 */
    private String img;
    /** 名称 */
    private String name;
    /** 简介 */
    private String description;
    /** 居民（如「猫头鹰一家 · 夜间巡逻员」） */
    private String residents;
    /** 当前状态（如「😴 白天打盹中」） */
    private String mood;
    /** 主题色 */
    private String color;
    /** 评分 0-5（一位小数） */
    private BigDecimal rating;
    private LocalDateTime createdAt;
}
