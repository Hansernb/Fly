package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.Visit;
import com.fenglin.springboottest.mapper.VisitMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 访问埋点（借鉴 java_travel 的 OpLog）
 * POST /api/visit —— 前端每次会话上报一次，写入 boonie_visit；
 * 看板 dashboard 据此计算真实 PV（记录数）/ UV（去重 IP 数）。
 */
@RestController
@RequestMapping("/api")
public class VisitController {

    @Autowired
    private VisitMapper visitMapper;

    /** 上报一次访问（失败不影响前端，接口始终返回 ok） */
    @PostMapping("/visit")
    public Map<String, Object> report(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Visit v = new Visit();
            v.setIp(clientIp(request));
            v.setVisitDate(LocalDate.now());
            v.setVisitTime(LocalDateTime.now());
            v.setPage(body == null || body.get("page") == null ? "/" : String.valueOf(body.get("page")));
            String ua = request.getHeader("User-Agent");
            v.setUserAgent(ua == null ? "" : ua.length() > 255 ? ua.substring(0, 255) : ua);
            visitMapper.insert(v);
        } catch (Exception ignored) {
            // 埋点失败不影响业务
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }

    /** 取真实客户端 IP：优先代理头，兜底 RemoteAddr */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return (comma > 0 ? ip.substring(0, comma) : ip).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}
