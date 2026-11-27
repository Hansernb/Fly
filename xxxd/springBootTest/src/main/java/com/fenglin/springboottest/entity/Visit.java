package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问记录（对应 boonie_visit 表）
 * 借鉴 java_travel 的 b_op_log：记录每次前台访问，看板据此计算真实 PV/UV。
 */
@Data
@TableName("boonie_visit")
public class Visit {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 访问者 IP */
    private String ip;
    /** 访问日期（便于按天聚合） */
    private LocalDate visitDate;
    /** 访问时间 */
    private LocalDateTime visitTime;
    /** 访问路径 */
    private String page;
    /** 浏览器标识（UV 辅助判断） */
    private String userAgent;
}
