package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 门票订单（对应 boonie_ticket_order 表，在线订票流程落库） */
@Data
@TableName("boonie_ticket_order")
public class TicketOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单号 BK+时间+随机（对外唯一标识） */
    private String orderNo;
    private Long userId;
    private String userName;
    private Long ticketId;
    /** 票种编码：adult/child/senior/family/night（前端用它做票种标识） */
    private String ticketCode;
    private String ticketName;
    private String emoji;
    /** 成交单价 */
    private BigDecimal unitPrice;
    /** 原价单价（用于计算优惠） */
    private BigDecimal originalPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal savedAmount;
    private LocalDate visitDate;
    private String buyerName;
    private String buyerPhone;
    /** 待使用 / 已取消 / 已使用 */
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
