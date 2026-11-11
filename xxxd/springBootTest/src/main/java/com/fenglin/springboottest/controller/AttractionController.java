package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.service.AttractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AttractionController {

    @Autowired
    private AttractionService attractionService;

    /** 数据库未配风景图时的兜底图池（前端 public/img 已存在的资源，按项目 id 确定性取用） */
    private static final String[] FALLBACK_IMGS = {
            "/img/coaster.gif", "/img/ferris.jpeg", "/img/train.jpeg",
            "/img/rafting.jpeg", "/img/maze.jpeg", "/img/waterfall.jpg"
    };

    /** GET /api/attractions —— 返回全部游乐项目（含评价 + 综合评分 + 风景图），字段对齐前端 attractions.js */
    @GetMapping("/attractions")
    public Map<String, Object> attractions() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Attraction a : attractionService.listWithReviews()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("emoji", a.getEmoji());
            m.put("img", sceneryImg(a));
            m.put("name", a.getName());
            m.put("desc", a.getDescription());
            m.put("tag", a.getTag());
            m.put("wait", a.getWaitMinutes());
            m.put("color", a.getColor());
            m.put("rating", a.getRating());

            List<Map<String, Object>> rs = new ArrayList<>();
            for (Review r : a.getReviews()) {
                rs.add(toReviewMap(r, a.getName(), a.getId()));
            }
            m.put("reviews", rs);
            data.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("data", data);
        return out;
    }

    /** POST /api/reviews —— 新增评价 { attractionId, name?, avatar?, rating, content } */
    @PostMapping("/reviews")
    public Map<String, Object> createReview(@RequestBody Map<String, Object> body) {
        Object aid = body.get("attractionId");
        Object ratingObj = body.get("rating");
        String content = (String) body.get("content");
        if (aid == null || ratingObj == null || content == null || content.isBlank())
            throw new BizException(400, "attractionId、rating、content 均为必填");

        Long attractionId;
        BigDecimal rating;
        try {
            attractionId = Long.valueOf(aid.toString());
            rating = new BigDecimal(ratingObj.toString());
        } catch (NumberFormatException e) {
            throw new BizException(400, "attractionId / rating 必须为数字");
        }

        String name = (String) body.getOrDefault("name", "匿名游客");
        String avatar = sanitizeReviewAvatar((String) body.get("avatar"), name);

        Review r = attractionService.addReview(attractionId, name, avatar, rating, content);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("review", toReviewMap(r, null, null));
        return out;
    }

    /**
     * 评价头像只接受三种形态：
     *  - 站内图片路径（默认头像 /img/defaults/…、上传头像 /api/avatar/…、以及历史 /img/xxx.jpg）
     *  - 纯 emoji（历史数据，如 🐻）
     *  - 没传 / 传了无效值 → 按「评价人名字」确定性生成 emoji 头像（NameAvatar），
     *    同名同头像、不同名字头像不同，避免游客评论清一色同一只熊。
     */
    private String sanitizeReviewAvatar(String raw, String name) {
        if (raw != null) {
            String v = raw.trim();
            if (v.startsWith("/img/") || v.startsWith("/api/avatar/")) {
                // 站内路径：不允许穿越与反斜杠，长度受字段 varchar(255) 限制
                if (!v.contains("..") && !v.contains("\\") && v.length() <= 255) return v;
            } else if (v.length() <= 16 && !v.contains(":") && !v.contains("/")) {
                // 纯 emoji / 短文本
                return v;
            }
        }
        return attractionService.generateAvatar(name);
    }

    /** 风景图：优先取库里的 img 列；没有则按项目 id 从兜底图池确定性取一张，保证每个项目都有图 */
    private String sceneryImg(Attraction a) {
        String img = a.getImg();
        if (img != null && !img.isBlank()) return img;
        return FALLBACK_IMGS[(int) Math.floorMod(a.getId() == null ? 0L : a.getId(), FALLBACK_IMGS.length)];
    }

    private Map<String, Object> toReviewMap(Review r, String attractionName, Long attractionId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getReviewerName());
        m.put("avatar", r.getAvatar());
        m.put("rating", r.getRating());
        m.put("date", r.getReviewDate() != null ? r.getReviewDate().toString() : "");
        m.put("text", r.getContent());
        if (attractionName != null) m.put("attraction", attractionName);
        if (attractionId != null) m.put("attractionId", attractionId);
        return m;
    }
}
