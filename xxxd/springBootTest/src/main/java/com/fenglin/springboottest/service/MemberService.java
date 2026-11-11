package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.Member;
import com.fenglin.springboottest.entity.PointLog;
import com.fenglin.springboottest.entity.TicketOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 会员积分服务。规则与前端 boonie-auth-pro/src/data/member.js 保持一致：
 *   获取：实付 1 元 = 1 积分（向下取整）
 *   抵扣：1 积分 = 0.3 元，单笔最多抵扣订单金额的 50%
 *   等级：累计积分 &lt;500 普通会员 / 500–1999 白银会员 / ≥2000 黄金会员
 */
public interface MemberService {

    /** 按手机号查会员档案，没有返回 null */
    Member findByPhone(String phone);

    /** 按登录账号查会员档案（账号绑定会员），没有返回 null */
    Member findByUser(Long userId);

    /** 按手机号取会员，不存在则自动建档（游客下单兜底） */
    Member getOrCreate(String phone, String nickname);

    /**
     * 按登录账号取会员，不存在则自动建档（账号绑定）。
     * 若该手机号此前以游客身份建过档且未绑定账号，会自动归并到当前账号名下。
     */
    Member getOrCreateForUser(Long userId, String phone, String nickname);

    /** 修改昵称 / 头像，返回更新后的会员 */
    Member updateProfile(String phone, String nickname, String avatar);

    /** 按会员 id 修改昵称 / 头像（账号绑定场景） */
    Member updateProfileById(Long memberId, String nickname, String avatar);

    /** 本单最多可抵扣多少积分：余额 与「金额 × 50% ÷ 0.3」取小 */
    int maxDeductible(Member member, BigDecimal amount);

    /** 积分折算金额（1 积分 = 0.3 元） */
    BigDecimal discountOf(int points);

    /** 实付金额可得积分（1 元 = 1 积分，向下取整） */
    int earnedOf(BigDecimal paid);

    /** 记一笔积分变动（正数获取、负数抵扣），同时写流水并刷新等级 */
    Member record(Member member, int delta, String logType, String orderNo, String remark);

    /** 订单取消：退回已抵扣积分（只回余额）、扣回本单赠送积分（余额与累计积分一起回退，等级回落） */
    void rollback(TicketOrder order);

    /** 积分明细（按时间倒序） */
    List<PointLog> logs(Long memberId, int limit);

    /** 按累计积分判定等级名 */
    String levelOf(int cumPoints);

    /** 会员对外字段（前端 decorate() 直接使用） */
    Map<String, Object> toMap(Member member);

    /** 积分流水对外字段（前端 l.type / l.points / l.balance ...） */
    Map<String, Object> logMap(PointLog log);
}
