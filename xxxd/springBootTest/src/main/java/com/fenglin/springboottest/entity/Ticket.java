package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 门票（对应 boonie_ticket 表，首页「🎫 门票速览」区块） */
@Data
@TableName("boonie_ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 图标 emoji */
    private String emoji;
    /** 票种名称（如 成人票 / 亲子套票） */
    private String name;
    /** 价格（元） */
    private BigDecimal price;
    /** 是否热门推荐 */
    private Boolean hot;
    /** 权益说明：数据库按行存储（\n 分隔），接口层与前端数组互转 */
    private String features;
    private LocalDateTime createdAt;

    /** 权益列表（非表字段，接口输出时填充） */
    @TableField(exist = false)
    private List<String> featureList;
}
