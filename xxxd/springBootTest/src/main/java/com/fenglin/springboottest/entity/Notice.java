package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首页顶部公告（对应 boonie_notice 表） */
@Data
@TableName("boonie_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公告内容 */
    private String content;
    /** 展示顺序 */
    private Integer sortNo;
    /** 是否上架：1 上架 0 下架 */
    private Boolean enabled;
    private LocalDateTime createdAt;
}
