package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 游客评价（对应 boonie_review 表） */
@Data
@TableName("boonie_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属项目 ID */
    private Long attractionId;
    /** 评价人昵称 */
    private String reviewerName;
    /** 头像 emoji */
    private String avatar;
    /** 评分 0–5（可含一位小数） */
    private BigDecimal rating;
    /** 评价日期 */
    private LocalDate reviewDate;
    /** 评价内容 */
    private String content;
    private LocalDateTime createdAt;
}
