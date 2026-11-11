package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 积分流水（对应 boonie_point_log 表） */
@Data
@TableName("boonie_point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    /** 获取 / 抵扣 / 退款 / 扣回 */
    private String logType;
    /** 积分变动（获取为正、抵扣为负） */
    private Integer points;
    /** 变动后余额 */
    private Integer balance;
    /** 关联订单号 */
    private String orderNo;
    private String remark;
    private LocalDateTime createdAt;
}
