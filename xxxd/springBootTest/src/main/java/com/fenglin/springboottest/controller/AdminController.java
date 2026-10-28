package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Attraction;
import com.fenglin.springboottest.entity.Review;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AttractionMapper;
import com.fenglin.springboottest.mapper.ReviewMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统接口（/api/admin/**）
 * 全部接口都会校验：1) 令牌有效  2) 该用户 permission == 0（管理员）
 * 普通用户（permission == 1）一律 403，无法增删改查任何数据
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private AuthService authService;
    @Autowired
    private AttractionMapper attractionMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private UserMapper userMapper;

    /** 统一鉴权：必须是管理员 */
    private User requireAdmin(String authHeader) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : "";
        User u = authService.verifyToken(token);
        if (u == null)
            throw new BizException(401, "未登录或登录已过期");
        if (!authService.isAdmin(u))
            throw new BizException(403, "权限不足：仅管理员可操作后台数据");
        return u;
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("data", data);
        return m;
    }

    private Map<String, Object> attractionMap(Attraction a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("emoji", a.getEmoji());
        m.put("description", a.getDescription());
        m.put("tag", a.getTag());
        m.put("waitMinutes", a.getWaitMinutes());
        m.put("color", a.getColor());
        return m;
    }

    // ==================== 游乐项目管理 ====================

    /** GET /api/admin/attractions —— 项目列表（后台用） */
    @GetMapping("/attractions")
    public Map<String, Object> listAttractions(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Attraction a : attractionMapper.selectList(null)) {
            list.add(attractionMap(a));
        }
        return ok(list);
    }

    /** POST /api/admin/attractions —— 新增项目 */
    @PostMapping("/attractions")
    public Map<String, Object> createAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);

        String name = str(body.get("name"));
        if (name == null || name.isBlank())
            throw new BizException(400, "项目名称不能为空");

        Attraction a = new Attraction();
        a.setName(name);
        a.setEmoji(def(body.get("emoji"), "🎡"));
        a.setDescription(def(body.get("description"), ""));
        a.setTag(def(body.get("tag"), "亲子"));
        a.setWaitMinutes(intVal(body.get("waitMinutes"), 0));
        a.setColor(def(body.get("color"), "#2f7d4f"));
        a.setCreatedAt(LocalDateTime.now());
        attractionMapper.insert(a);
        return ok(attractionMap(a));
    }

    /** PUT /api/admin/attractions/{id} —— 修改项目 */
    @PutMapping("/attractions/{id}")
    public Map<String, Object> updateAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(auth);

        Attraction a = attractionMapper.selectById(id);
        if (a == null)
            throw new BizException(404, "项目不存在");

        if (body.containsKey("name")) a.setName(str(body.get("name")));
        if (body.containsKey("emoji")) a.setEmoji(str(body.get("emoji")));
        if (body.containsKey("description")) a.setDescription(str(body.get("description")));
        if (body.containsKey("tag")) a.setTag(str(body.get("tag")));
        if (body.containsKey("waitMinutes")) a.setWaitMinutes(intVal(body.get("waitMinutes"), a.getWaitMinutes()));
        if (body.containsKey("color")) a.setColor(str(body.get("color")));
        attractionMapper.updateById(a);
        return ok(attractionMap(a));
    }

    /** DELETE /api/admin/attractions/{id} —— 删除项目（连带删除其评价） */
    @DeleteMapping("/attractions/{id}")
    public Map<String, Object> deleteAttraction(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (attractionMapper.selectById(id) == null)
            throw new BizException(404, "项目不存在");
        reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getAttractionId, id));
        attractionMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 游客评价管理 ====================

    /** GET /api/admin/reviews —— 全部评价（带所属项目名） */
    @GetMapping("/reviews")
    public Map<String, Object> listReviews(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        Map<Long, String> names = new LinkedHashMap<>();
        for (Attraction a : attractionMapper.selectList(null)) {
            names.put(a.getId(), a.getName());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Review r : reviewMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("attractionId", r.getAttractionId());
            m.put("attraction", names.getOrDefault(r.getAttractionId(), "（已删除项目）"));
            m.put("name", r.getReviewerName());
            m.put("avatar", r.getAvatar());
            m.put("rating", r.getRating());
            m.put("date", r.getReviewDate() != null ? r.getReviewDate().toString() : "");
            m.put("text", r.getContent());
            list.add(m);
        }
        return ok(list);
    }

    /** DELETE /api/admin/reviews/{id} —— 删除评价 */
    @DeleteMapping("/reviews/{id}")
    public Map<String, Object> deleteReview(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        requireAdmin(auth);
        if (reviewMapper.selectById(id) == null)
            throw new BizException(404, "评价不存在");
        reviewMapper.deleteById(id);
        return ok(id);
    }

    // ==================== 用户与权限管理 ====================

    /** GET /api/admin/users —— 用户列表（不返回密码） */
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        requireAdmin(auth);
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("permission", u.getPermission() == null ? 1 : u.getPermission());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            list.add(m);
        }
        return ok(list);
    }

    /** PUT /api/admin/users/{id} —— 修改权限 { permission } 或重置密码 { password } */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        User admin = requireAdmin(auth);
        User u = userMapper.selectById(id);
        if (u == null)
            throw new BizException(404, "用户不存在");

        if (body.containsKey("permission")) {
            Integer p = intVal(body.get("permission"), 1);
            if (p != 0 && p != 1)
                throw new BizException(400, "权限值只能是 0（管理员）或 1（默认）");
            // 不允许把自己降级，避免出现无管理员的死锁
            if (u.getId().equals(admin.getId()) && p != 0)
                throw new BizException(400, "不能取消自己的管理员权限");
            u.setPermission(p);
        }
        if (body.containsKey("password")) {
            String np = str(body.get("password"));
            if (np == null || np.length() < 6)
                throw new BizException(400, "新密码至少 6 位");
            u.setPassword(np);   // 明文
        }
        userMapper.updateById(u);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("permission", u.getPermission());
        return ok(m);
    }

    // ==================== 小工具 ====================

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String def(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private Integer intVal(Object o, Integer fallback) {
        if (o == null) return fallback;
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
