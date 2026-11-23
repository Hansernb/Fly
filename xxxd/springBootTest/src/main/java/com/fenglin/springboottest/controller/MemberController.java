package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Member;
import com.fenglin.springboottest.entity.PointLog;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员中心接口：
 *   GET  /api/member?phone=xxx                     —— 会员档案 + 积分明细（不存在返回 404）
 *   POST /api/member/profile {phone,nickname?,avatar?} —— 修改昵称 / 头像
 *   GET  /api/member/preview?phone=xxx&amount=yy   —— 订票页预览：会员 + 本单最多可抵扣积分
 *
 * 会员身份以「登录账号」为准：带 Authorization 时按账号查（账号买票积分就归该账号，与取票人手机号无关）；
 * 未登录（游客）时退回按手机号查，兼容老数据。
 */
@RestController
@RequestMapping("/api")
public class MemberController {

    private static final int LOG_LIMIT = 60;

    @Autowired
    private MemberService memberService;
    @Autowired
    private AuthService authService;

    /** 会员档案（登录态按账号；未登录按手机号。未开通 404 由前端提示「下单自动开通」） */
    @GetMapping("/member")
    public Map<String, Object> detail(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "phone", required = false) String phone) {

        User me = authService.verifyToken(bearer(auth));
        Member m;
        if (me != null) {
            m = memberService.findByUser(me.getId());
            if (m == null) throw new BizException(404, "该账号还没有会员档案，下单后自动开通");
        } else {
            m = memberService.findByPhone(normPhone(phone));
            if (m == null) throw new BizException(404, "该手机号还没有会员档案，下单后自动开通");
        }

        List<Map<String, Object>> logs = new ArrayList<>();
        for (PointLog log : memberService.logs(m.getId(), LOG_LIMIT)) {
            logs.add(memberService.logMap(log));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("member", memberService.toMap(m));
        data.put("logs", logs);
        return ok(data);
    }

    /** 修改会员资料：昵称 / 头像（emoji）。登录态改当前账号的会员卡，游客态按手机号 */
    @PostMapping("/member/profile")
    public Map<String, Object> profile(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {

        String nickname = str(body.get("nickname"));
        String avatar = str(body.get("avatar"));
        Member m = currentMember(auth, str(body.get("phone")), false);
        if (m == null) throw new BizException(404, "会员不存在，请先登录或下单开通");

        Member updated = memberService.updateProfileById(m.getId(), nickname, avatar);
        if (updated == null) throw new BizException(404, "会员不存在，请先登录或下单开通");
        return ok(memberService.toMap(updated));
    }

    /** 订票页预览：会员信息 + 本单可抵扣上限（会员不存在时 member 为 null，前端照常下单） */
    @GetMapping("/member/preview")
    public Map<String, Object> preview(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "amount", required = false) String amount) {

        Member m = currentMember(auth, phone, false);

        BigDecimal gross = decimalOf(amount);
        int maxUse = memberService.maxDeductible(m, gross);
        BigDecimal maxDiscount = memberService.discountOf(maxUse);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("member", memberService.toMap(m));
        data.put("maxPoints", maxUse);
        data.put("maxDiscount", maxDiscount);
        return ok(data);
    }

    /* ===================== 内部工具 ===================== */

    /** 登录态 → 账号绑定的会员；未登录 → 按手机号查（strictPhone=true 时手机号不合法报 400） */
    private Member currentMember(String auth, String phone, boolean strictPhone) {
        User me = authService.verifyToken(bearer(auth));
        if (me != null) return memberService.findByUser(me.getId());
        String p = str(phone);
        if (p.matches("^1\\d{10}$")) return memberService.findByPhone(p);
        if (strictPhone) throw new BizException(400, "请填写 11 位手机号");
        return null;
    }

    private String bearer(String auth) {
        if (auth == null) return null;
        String a = auth.trim();
        return a.startsWith("Bearer ") ? a.substring(7).trim() : a;
    }

    private String normPhone(String phone) {
        String p = str(phone);
        if (!p.matches("^1\\d{10}$")) throw new BizException(400, "请填写 11 位手机号");
        return p;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private BigDecimal decimalOf(String s) {
        try {
            return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }
}
