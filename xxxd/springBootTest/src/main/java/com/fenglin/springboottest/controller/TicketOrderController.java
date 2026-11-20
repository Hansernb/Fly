package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Member;
import com.fenglin.springboottest.entity.Ticket;
import com.fenglin.springboottest.entity.TicketOrder;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.TicketMapper;
import com.fenglin.springboottest.mapper.TicketOrderMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 门票订单接口：在线订票流程落库（boonie_ticket_order）
 *   POST /api/orders                  —— 提交订单（价格由后端按票库重算，前端传的价格不采信）
 *   GET  /api/orders                  —— 我的订单列表（按登录用户）
 *   POST /api/orders/{orderNo}/cancel —— 取消订单（仅本人 + 待使用）
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://192.168.68.47:5173")
public class TicketOrderController {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TicketOrderMapper orderMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private MemberService memberService;

    /** 提交订单：{ ticketId(票种编码或数据库 id), quantity, visitDate, buyerName, buyerPhone } */
    @PostMapping("/orders")
    public Map<String, Object> create(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {

        User me = authService.verifyToken(bearer(auth));

        String ticketKey = str(body.get("ticketId"));
        if (ticketKey.isEmpty()) throw new BizException(400, "请选择票种");
        Ticket ticket = findTicket(ticketKey);
        if (ticket == null || Boolean.FALSE.equals(ticket.getEnabled()))
            throw new BizException(404, "票种不存在或已下架");

        int qty = intOf(body.get("quantity"), 1);
        if (qty < 1 || qty > 10) throw new BizException(400, "单次购买数量需在 1–10 张之间");

        LocalDate visitDate = parseDate(str(body.get("visitDate")));
        if (visitDate == null) throw new BizException(400, "请选择游玩日期");
        if (visitDate.isBefore(LocalDate.now())) throw new BizException(400, "游玩日期不能早于今天");

        String buyerName = str(body.get("buyerName"));
        String buyerPhone = str(body.get("buyerPhone"));
        if (buyerName.isEmpty()) throw new BizException(400, "请填写取票人姓名");
        if (!buyerPhone.matches("^1\\d{10}$")) throw new BizException(400, "请填写 11 位手机号");

        BigDecimal unit = ticket.getPrice() == null ? BigDecimal.ZERO : ticket.getPrice();
        BigDecimal origin = ticket.getOriginalPrice() == null ? unit : ticket.getOriginalPrice();
        if (origin.compareTo(unit) < 0) origin = unit;
        BigDecimal qtyDec = BigDecimal.valueOf(qty);
        BigDecimal gross = unit.multiply(qtyDec);

        // ===== 会员：积分跟「登录账号」走；游客（未登录）才按取票人手机号兜底建档 =====
        Member member;
        boolean isNewMember;
        if (me != null) {
            isNewMember = memberService.findByUser(me.getId()) == null;
            member = memberService.getOrCreateForUser(me.getId(), buyerPhone, buyerName);
        } else {
            Member bound = memberService.findByPhone(buyerPhone);
            // 该手机号已绑定会员账号：游客不得冒用（防止别人填你的手机号花你的积分）
            if (bound != null && bound.getUserId() != null) {
                throw new BizException(400, "该手机号已绑定会员账号，请先登录再下单，积分会记到账号上");
            }
            isNewMember = bound == null;
            member = memberService.getOrCreate(buyerPhone, buyerName);
        }

        // ===== 积分抵扣：1 积分 = 0.3 元，单笔最多抵扣金额的 50% =====
        int maxUse = memberService.maxDeductible(member, gross);
        int wantUse = Math.max(0, intOf(body.get("usePoints"), 0));
        int usePoints = Math.min(wantUse, maxUse);
        BigDecimal discount = memberService.discountOf(usePoints);
        BigDecimal paid = gross.subtract(discount);
        if (paid.compareTo(new BigDecimal("0.01")) < 0) paid = new BigDecimal("0.01");
        paid = paid.setScale(2, RoundingMode.HALF_UP);
        // 实付 1 元 = 1 积分（向下取整）
        int earned = paid.setScale(0, RoundingMode.FLOOR).intValue();

        TicketOrder o = new TicketOrder();
        o.setOrderNo(nextOrderNo());
        o.setUserId(me == null ? null : me.getId());
        o.setUserName(me == null ? "游客" : me.getName());
        o.setTicketId(ticket.getId());
        o.setTicketCode(ticket.getTypeCode());
        o.setTicketName(ticket.getName());
        o.setEmoji(ticket.getEmoji() == null ? "🎫" : ticket.getEmoji());
        o.setUnitPrice(unit);
        o.setOriginalPrice(origin);
        o.setQuantity(qty);
        o.setGrossAmount(gross);
        o.setTotalAmount(paid);
        o.setPointsUsed(usePoints);
        o.setPointsDiscount(discount);
        o.setPointsEarned(earned);
        o.setMemberId(member.getId());
        o.setSavedAmount(origin.subtract(unit).multiply(qtyDec));
        o.setVisitDate(visitDate);
        o.setBuyerName(buyerName);
        o.setBuyerPhone(buyerPhone);
        o.setStatus("待使用");
        o.setCreatedAt(LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(o);

        // ===== 积分流水：先抵扣、再按实付金额赠送 =====
        if (usePoints > 0) {
            memberService.record(member, -usePoints, "抵扣", o.getOrderNo(),
                    "订单抵扣 ¥" + discount.toPlainString());
        }
        if (earned > 0) {
            memberService.record(member, earned, "获取", o.getOrderNo(),
                    "购票实付 ¥" + paid.toPlainString() + " 获得积分");
        }
        Member latest = member;   // record() 已把最新余额/累计/等级回写到该对象

        Map<String, Object> out = ok(orderMap(o));
        out.put("member", memberService.toMap(latest));
        out.put("pointsEarned", earned);
        out.put("pointsUsed", usePoints);
        out.put("pointsDiscount", discount);
        out.put("isNewMember", isNewMember);
        out.put("message", isNewMember
                ? "下单成功，已自动开通会员，获得 " + earned + " 积分"
                : "下单成功，获得 " + earned + " 积分");
        return out;
    }

    /** 我的订单（未登录返回 401，前端会降级为本地缓存） */
    @GetMapping("/orders")
    public Map<String, Object> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = authService.verifyToken(bearer(auth));
        if (me == null) throw new BizException(401, "未登录或登录已过期");

        QueryWrapper<TicketOrder> qw = new QueryWrapper<>();
        qw.eq("user_id", me.getId()).orderByDesc("id");
        List<Map<String, Object>> list = new ArrayList<>();
        for (TicketOrder o : orderMapper.selectList(qw)) list.add(orderMap(o));

        Map<String, Object> out = ok(list);
        out.put("total", list.size());
        return out;
    }

    /** 取消订单 */
    @PostMapping("/orders/{orderNo}/cancel")
    public Map<String, Object> cancel(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable String orderNo) {

        User me = authService.verifyToken(bearer(auth));
        if (me == null) throw new BizException(401, "未登录或登录已过期");

        QueryWrapper<TicketOrder> qw = new QueryWrapper<>();
        qw.eq("order_no", orderNo).eq("user_id", me.getId());
        TicketOrder o = orderMapper.selectOne(qw);
        if (o == null) throw new BizException(404, "订单不存在");
        if ("已取消".equals(o.getStatus())) throw new BizException(400, "订单已取消");
        if ("已使用".equals(o.getStatus())) throw new BizException(400, "订单已核销，不能取消");

        o.setStatus("已取消");
        o.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(o);

        // 积分回滚：退回已抵扣积分、扣回本单赠送积分
        memberService.rollback(o);

        Map<String, Object> out = ok(orderMap(o));
        out.put("message", "订单已取消，积分已同步回滚");
        return out;
    }

    /* ===================== 内部工具 ===================== */

    /** 支持前端票种编码（adult/child...）或数据库主键 */
    private Ticket findTicket(String key) {
        QueryWrapper<Ticket> qw = new QueryWrapper<>();
        qw.eq("type_code", key);
        Ticket t = ticketMapper.selectOne(qw);
        if (t != null) return t;
        try {
            return ticketMapper.selectById(Long.parseLong(key));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nextOrderNo() {
        for (int i = 0; i < 5; i++) {
            String no = "BK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            QueryWrapper<TicketOrder> qw = new QueryWrapper<>();
            qw.eq("order_no", no);
            if (orderMapper.selectCount(qw) == 0) return no;
        }
        return "BK" + System.currentTimeMillis();
    }

    private Map<String, Object> orderMap(TicketOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getOrderNo());                 // 前端用订单号作唯一标识
        m.put("orderId", o.getId());
        m.put("ticketId", o.getTicketCode());         // 前端票种 id 即编码
        m.put("ticketName", o.getTicketName());
        m.put("emoji", o.getEmoji());
        m.put("price", o.getUnitPrice());
        m.put("originalPrice", o.getOriginalPrice());
        m.put("date", o.getVisitDate() == null ? "" : o.getVisitDate().toString());
        m.put("quantity", o.getQuantity());
        m.put("total", o.getTotalAmount());
        m.put("grossAmount", o.getGrossAmount() == null ? o.getTotalAmount() : o.getGrossAmount());
        m.put("pointsUsed", o.getPointsUsed() == null ? 0 : o.getPointsUsed());
        m.put("pointsDiscount", o.getPointsDiscount() == null ? BigDecimal.ZERO : o.getPointsDiscount());
        m.put("pointsEarned", o.getPointsEarned() == null ? 0 : o.getPointsEarned());
        m.put("saved", o.getSavedAmount());
        m.put("buyerName", o.getBuyerName());
        m.put("buyerPhone", o.getBuyerPhone());
        m.put("status", o.getStatus());
        m.put("createdAt", o.getCreatedAt() == null ? "" : o.getCreatedAt().format(STAMP));
        m.put("user", o.getUserName());
        return m;
    }

    private String bearer(String auth) {
        if (auth == null) return null;
        String a = auth.trim();
        return a.startsWith("Bearer ") ? a.substring(7).trim() : a;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private int intOf(Object v, int def) {
        try {
            return Integer.parseInt(str(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }
}
