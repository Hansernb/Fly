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
    /** 票种编码：adult/child/senior/family/night（前端用它做票种标识） */
    private String typeCode;
    /** 图标 emoji */
    private String emoji;
    /** 票种名称（如 成人票 / 亲子套票） */
    private String name;
    /** 价格（元） */
    private BigDecimal price;
    /** 原价（元）：用于划线展示优惠 */
    private BigDecimal originalPrice;
    /** 优惠标签，如 5 折 / 套票 */
    private String discountLabel;
    /** 角标文案：标准 / 儿童优惠 / 长者优惠 / 热门 / 夜场 */
    private String tagLabel;
    /** 一句话说明（订票流程用） */
    private String descText;
    /** 主题色 */
    private String color;
    /** 首页排序 */
    private Integer sortNo;
    /** 是否热门推荐 */
    private Boolean hot;
    /** 是否上架 */
    private Boolean enabled;
    /** 权益说明：数据库按行存储（\n 分隔），接口层与前端数组互转 */
    private String features;
    private LocalDateTime createdAt;

    /** 权益列表（非表字段，接口输出时填充） */
    @TableField(exist = false)
    private List<String> featureList;
}
