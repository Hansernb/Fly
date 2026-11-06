package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.Habitat;
import com.fenglin.springboottest.entity.Show;
import com.fenglin.springboottest.entity.Ticket;
import com.fenglin.springboottest.mapper.HabitatMapper;
import com.fenglin.springboottest.mapper.ShowMapper;
import com.fenglin.springboottest.mapper.TicketMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页内容接口：森林栖息地 / 森林演出 / 门票
 * 这三个区块原先是前端写死的静态数据，现已入库，可由后台管理系统维护。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ContentController {

    @Autowired
    private HabitatMapper habitatMapper;
    @Autowired
    private ShowMapper showMapper;
    @Autowired
    private TicketMapper ticketMapper;

    /** GET /api/habitats —— 森林栖息地列表 */
    @GetMapping("/habitats")
    public Map<String, Object> habitats() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Habitat h : habitatMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("emoji", h.getEmoji());
            m.put("img", h.getImg());
            m.put("name", h.getName());
            m.put("desc", h.getDescription());          // 前端字段是 desc
            m.put("residents", h.getResidents());
            m.put("mood", h.getMood());
            m.put("color", h.getColor());
            m.put("rating", h.getRating());
            list.add(m);
        }
        return ok(list);
    }

    /** GET /api/shows —— 今日森林演出列表 */
    @GetMapping("/shows")
    public Map<String, Object> shows() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Show s : showMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("time", s.getShowTime());              // 前端字段是 time
            m.put("emoji", s.getEmoji());
            m.put("name", s.getName());
            m.put("place", s.getPlace());
            m.put("tag", s.getTag());
            list.add(m);
        }
        return ok(list);
    }

    /** GET /api/tickets —— 门票列表（features 由按行文本转成数组，按 sort_no 排序，仅上架） */
    @GetMapping("/tickets")
    public Map<String, Object> tickets() {
        QueryWrapper<Ticket> qw = new QueryWrapper<>();
        qw.eq("enabled", 1).orderByAsc("sort_no", "id");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Ticket t : ticketMapper.selectList(qw)) {
            list.add(ticketMap(t));
        }
        return ok(list);
    }

    /** 门票统一输出结构：字段对齐前端 src/data/tickets.js */
    private Map<String, Object> ticketMap(Ticket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        String code = (t.getTypeCode() == null || t.getTypeCode().isBlank())
                ? "ticket_" + t.getId() : t.getTypeCode();
        m.put("id", code);                                   // 前端以票种编码作 id
        m.put("dbId", t.getId());
        m.put("code", code);
        m.put("emoji", t.getEmoji());
        m.put("name", t.getName());
        m.put("price", t.getPrice());
        m.put("originalPrice", t.getOriginalPrice() == null ? t.getPrice() : t.getOriginalPrice());
        m.put("discount", t.getDiscountLabel() == null ? "" : t.getDiscountLabel());
        m.put("tag", t.getTagLabel() == null ? "标准" : t.getTagLabel());
        m.put("desc", t.getDescText() == null ? "" : t.getDescText());
        m.put("color", t.getColor() == null ? "#2f7d4f" : t.getColor());
        m.put("sortNo", t.getSortNo());
        m.put("hot", Boolean.TRUE.equals(t.getHot()));
        m.put("features", splitFeatures(t.getFeatures()));
        return m;
    }

    /** 按行文本 -> 列表 */
    static List<String> splitFeatures(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String line : raw.split("\\R")) {
            String s = line.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }
}
